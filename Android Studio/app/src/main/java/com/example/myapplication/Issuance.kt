package com.example.myapplication

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class Issuance : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.issuance)

        val librarianId = intent.getStringExtra("librarianId")
        val type = intent.getStringExtra("type")
        val libraryId = intent.getStringExtra("libraryId")
        if (libraryId == null || librarianId == null)  {
            Toast.makeText(this, "Помилка: Такої бібліотеки чи бібліотекаря не існує", Toast.LENGTH_SHORT).show()
            finish()
        }
        val autoCompleteTextViewBooks: AutoCompleteTextView = findViewById(R.id.autoCompleteTextViewBooks)
        val autoCompleteTextViewReader: AutoCompleteTextView = findViewById(R.id.autoCompleteTextViewReader)
        val btnIssuance: Button = findViewById(R.id.btnIssuance)

        val buttonBack: Button = findViewById(R.id.buttonBack)
        buttonBack.setOnClickListener {
            finish()
        }

        val httpClient = HttpClient()
        val url = getString(R.string.server_url)

        val json = """{
            "function_name": "get_books_and_readers",
            "param_dict": {
                "library_id": "$libraryId"
            }
        }"""

        httpClient.safePostRequest(this, url, json) { jsonResponse ->
            val resultValue = jsonResponse["result"]
            if (resultValue is JSONArray) {
                val booksArray = resultValue[0]
                val readersArray = resultValue[1]
                if (booksArray is JSONArray && readersArray is JSONArray) {
                    val allBooks = mutableListOf<String>()
                    val allReaders = mutableListOf<String>()
                    for (i in 0 until booksArray.length()) {
                        val bookName = booksArray.getString(i)
                        allBooks.add(bookName)
                    }
                    for (i in 0 until readersArray.length()) {
                        val readerName = readersArray.getString(i)
                        allReaders.add(readerName)
                    }

                    val booksAdapter = ArrayAdapter(
                        this@Issuance,
                        android.R.layout.simple_dropdown_item_1line,
                        allBooks
                    )
                    val readersAdapter = ArrayAdapter(
                        this@Issuance,
                        android.R.layout.simple_dropdown_item_1line,
                        allReaders
                    )
                    autoCompleteTextViewBooks.setAdapter(booksAdapter)
                    autoCompleteTextViewReader.setAdapter(readersAdapter)
                } else {
                    Toast.makeText(this@Issuance, "Помилка в запиті до серверу", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this@Issuance, "Помилка в запиті до серверу", Toast.LENGTH_SHORT).show()
            }
        }

        autoCompleteTextViewBooks.setOnItemClickListener { _, _, _, _ ->
            val selectedBook = autoCompleteTextViewBooks.text.toString()
            autoCompleteTextViewBooks.setText(selectedBook)
        }

        autoCompleteTextViewReader.setOnItemClickListener { _, _, _, _ ->
            val selectedReader = autoCompleteTextViewReader.text.toString()
            autoCompleteTextViewReader.setText(selectedReader)
        }

        when (type) {
            "issue" -> {
                btnIssuance.text = "Видати книгу"
                btnIssuance.setOnClickListener {
                    val selectedBook = autoCompleteTextViewBooks.text.toString()
                    val selectedReader = autoCompleteTextViewReader.text.toString()

                    if (selectedBook.isNotEmpty() && selectedReader.isNotEmpty()) {

                        val json = """{
                            "function_name": "insert_issue",
                            "param_dict": {
                                "librarian_id": "$librarianId",
                                "library_id": "$libraryId",
                                "selected_book": "$selectedBook",
                                "selected_reader": "$selectedReader"
                            }
                        }"""

                        httpClient.safePostRequest(this, url, json) { jsonResponse ->
                            val resultValue = jsonResponse["result"]
                            if (resultValue is String) {
                                Toast.makeText(this@Issuance, resultValue, Toast.LENGTH_SHORT).show()
                                if (resultValue == "Запис успішний") {
                                    finish()
                                }
                            } else {
                                Toast.makeText(this@Issuance, "Помилка в запиті до серверу", Toast.LENGTH_SHORT).show()
                            }
                        }

                    } else {
                        Toast.makeText(this, "Заповніть всі поля", Toast.LENGTH_SHORT).show()
                    }

                }
            }

            "return" -> {
                btnIssuance.text = "Повернути книгу"
                btnIssuance.setOnClickListener {
                    val selectedBook = autoCompleteTextViewBooks.text.toString()
                    val selectedReader = autoCompleteTextViewReader.text.toString()

                    if (selectedBook.isNotEmpty() && selectedReader.isNotEmpty()) {

                        val json = """{
                            "function_name": "return_issue",
                            "param_dict": {
                                "librarian_id": "$librarianId",
                                "library_id": "$libraryId",
                                "selected_book": "$selectedBook",
                                "selected_reader": "$selectedReader"
                            }
                        }"""

                        httpClient.safePostRequest(this, url, json) { jsonResponse ->
                            val resultValue = jsonResponse["result"]
                            if (resultValue is String) {
                                Toast.makeText(this@Issuance, resultValue, Toast.LENGTH_SHORT).show()
                                if (resultValue == "Книга успішно повернена") {
                                    finish()
                                }
                            } else {
                                Toast.makeText(this@Issuance, "Помилка в запиті до серверу", Toast.LENGTH_SHORT).show()
                            }
                        }

                    } else {
                        Toast.makeText(this, "Заповніть всі поля", Toast.LENGTH_SHORT).show()
                    }

                }
            }

            else -> {
                Toast.makeText(this, "Помилка: Такої дії не існує", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
