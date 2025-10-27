package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class TableViewer : AppCompatActivity(){
    @SuppressLint("SetTextI18n", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.table_viewer)

        val type = intent.getStringExtra("type")
        val libraryId = intent.getStringExtra("libraryId")
        val listViewTable: ListView = findViewById(R.id.listView)
        var resultList = ArrayList<String>()
        val httpClient = HttpClient()
        val url = getString(R.string.server_url)
        var json = String()

        when (type) {
            "book" -> json = """{
                            "function_name": "get_all_books",
                            "param_dict": {
                                "library_id": "$libraryId"
                            }
                        }"""
            "reader" -> json = """{
                            "function_name": "get_all_readers",
                            "param_dict": {
                                "library_id": "$libraryId"
                            }
                        }"""
            "issue" -> json = """{
                            "function_name": "get_all_issues",
                            "param_dict": {
                                "library_id": "$libraryId"
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
                    Toast.makeText(this@TableViewer, "Помилка: Перевірте з'єднання з інтернетом або повторіть спробу пізніше.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@TableViewer, "Помилка на сервері, вибачте за незручності.", Toast.LENGTH_SHORT).show()
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
                            val resultAdapter = object : ArrayAdapter<String>(
                                this@TableViewer,
                                android.R.layout.simple_list_item_1,
                                resultList
                            ) {
                                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                                    val view = super.getView(position, convertView, parent) as TextView
                                    view.setTextColor(Color.WHITE)  // задаємо білий текст
                                    return view
                                }
                            }
                            listViewTable.adapter = resultAdapter
                            listViewTable.adapter = resultAdapter
                        } else {
                            Toast.makeText(this@TableViewer, "Помилка в запиті до серверу", Toast.LENGTH_SHORT).show()
                        }
                    }

                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@TableViewer, "Помилка при обробці відповіді сервера.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })

        listViewTable.setOnItemClickListener { parent, view, position, id ->
            if (type != "issue"){
                val selectedItem = resultList[position]
                when (type) {
                    "book" -> {
                        val intent = Intent(this, AddBook::class.java)
                        intent.putExtra("type", "update_book")
                        intent.putExtra("data", selectedItem)
                        startActivity(intent)
                        finish()
                    }
                    "reader" -> {
                        val intent = Intent(this, AddReader::class.java)
                        intent.putExtra("type", "update_reader")
                        intent.putExtra("data", selectedItem)
                        startActivity(intent)
                        finish()
                    }
                }
            }
        }

        val buttonBack: Button = findViewById(R.id.buttonBack)
        buttonBack.setOnClickListener {
            finish()
        }
    }
}