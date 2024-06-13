package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class Filter : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.filter)

        val tableName = intent.getStringExtra("table")
        val param = intent.getStringExtra("param")
        val libraryId = intent.getStringExtra("libraryId")

        val autoCompleteTextViewFind: AutoCompleteTextView = findViewById(R.id.autoCompleteTextViewFind)
        val btnFind: Button = findViewById(R.id.btnFind)
        val listViewTable: ListView = findViewById(R.id.listView)
        val httpClient = HttpClient()
        val url = getString(R.string.server_url)
        var json = String()

        when (tableName) {
            "Book", "Reader" -> json = """{
                            "function_name": "filter_books_readers",
                            "param_dict": {
                                "library_id": "$libraryId",
                                "column_name": "$param",
                                "table_name": "$tableName"
                            }
                        }"""
            "Issue" -> json = """{
                            "function_name": "filter_issue",
                            "param_dict": {
                                "library_id": "$libraryId",
                                "column_name": "$param",
                                "table_name": "$tableName"
                            }
                        }"""
            else -> {
                Toast.makeText(this, "Помилка: Такої таблиці не існує", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        httpClient.postRequest(url, json, object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@Filter, "Помилка: Перевірте з'єднання з інтернетом або повторіть спробу пізніше.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@Filter, "Помилка на сервері, вибачте за незручності.", Toast.LENGTH_SHORT).show()
                    }
                    return
                }

                try {
                    runOnUiThread {
                        val resultValue = JSONObject(response.body?.use { it?.string() })["result"]
                        if (resultValue is JSONArray) {
                            val allParams = mutableListOf<String>()
                            for (i in 0 until resultValue.length()) {
                                val resultText = resultValue.getString(i)
                                allParams.add(resultText)
                            }
                            val paramAdapter = ArrayAdapter(
                                this@Filter,
                                android.R.layout.simple_list_item_1,
                                allParams
                            )
                            autoCompleteTextViewFind.setAdapter(paramAdapter)

                        } else {
                            Toast.makeText(this@Filter, "Помилка в запиті до серверу", Toast.LENGTH_SHORT).show()
                        }
                    }

                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@Filter, "Помилка при обробці відповіді сервера.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })

        autoCompleteTextViewFind.setOnItemClickListener { _, _, _, _ ->
            val selectedParams = autoCompleteTextViewFind.text.toString()
            autoCompleteTextViewFind.setText(selectedParams)
        }

        btnFind.setOnClickListener(View.OnClickListener {
            var resultList = ArrayList<String>()
            var valueParam = autoCompleteTextViewFind.text.toString()
            when (tableName) {
                "Book" -> json = """{
                            "function_name": "get_books_from_param",
                            "param_dict": {
                                "library_id": "$libraryId",
                                "param": "$param",
                                "value": "$valueParam"
                            }
                        }"""
                "Reader" -> json = """{
                            "function_name": "get_readers_from_param",
                            "param_dict": {
                                "library_id": "$libraryId",
                                "param": "$param",
                                "value": "$valueParam"
                            }
                        }"""
                "Issue" -> json = """{
                            "function_name": "get_issue_from_param",
                            "param_dict": {
                                "library_id": "$libraryId",
                                "param": "$param",
                                "value": "$valueParam"
                            }
                        }"""
                else -> {
                    Toast.makeText(this, "Помилка: Такої таблиці не існує", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            httpClient.postRequest(url, json, object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@Filter, "Помилка: Перевірте з'єднання з інтернетом або повторіть спробу пізніше.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        runOnUiThread {
                            Toast.makeText(this@Filter, "Помилка на сервері, вибачте за незручності.", Toast.LENGTH_SHORT).show()
                        }
                        return
                    }

                    try {
                        runOnUiThread {
                            val resultValue = JSONObject(response.body?.use { it?.string() })["result"]
                            if (resultValue is JSONArray) {
                                for (i in 0 until resultValue.length()) {
                                    val resultText = resultValue.getString(i)
                                    resultList.add(resultText)
                                }
                                val resultAdapter = ArrayAdapter(
                                    this@Filter,
                                    android.R.layout.simple_list_item_1,
                                    resultList
                                )
                                listViewTable.adapter = resultAdapter
                            } else {
                                Toast.makeText(this@Filter, "Помилка в запиті до серверу", Toast.LENGTH_SHORT).show()
                            }
                        }

                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@Filter, "Помилка при обробці відповіді сервера.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })

            listViewTable.setOnItemClickListener { parent, view, position, id ->
                if (tableName != "Issue"){
                    val selectedItem = resultList[position]
                    when (tableName) {
                        "Book" -> {
                            val intent = Intent(this, AddBook::class.java)
                            intent.putExtra("type", "update_book")
                            intent.putExtra("data", selectedItem)
                            startActivity(intent)
                            finish()
                        }
                        "Reader" -> {
                            val intent = Intent(this, AddReader::class.java)
                            intent.putExtra("type", "update_reader")
                            intent.putExtra("data", selectedItem)
                            startActivity(intent)
                            finish()
                        }
                    }
                }
            }
        })

    }
}