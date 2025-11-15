package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class AddBook : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_book)

        val editTextTitle: EditText = findViewById(R.id.editTextTitle)
        val editTextAuthor: EditText = findViewById(R.id.editTextAuthor)
        val editTextPublicationYear: EditText = findViewById(R.id.editTextPublicationYear)
        val editTextISBN: EditText = findViewById(R.id.editTextISBN)
        val editTextGenre: EditText = findViewById(R.id.editTextGenre)
        val editTextPageCount: EditText = findViewById(R.id.editTextPageCount)
        val editTextAvailableCopies: EditText = findViewById(R.id.editTextAvailableCopies)
        val editTextPublisher: EditText = findViewById(R.id.editTextPublisher)

        val buttonBack: Button = findViewById(R.id.buttonBack)
        buttonBack.setOnClickListener {
            finish()
        }
        val httpClient = HttpClient()
        val url = getString(R.string.server_url)

        val btnRegisterBook: Button = findViewById(R.id.btnRegisterBook)
        val btnDeleteBook: Button = findViewById(R.id.btnDeleteBook)

        val type = intent.getStringExtra("type") ?: "add_book"
        when (type) {
            "add_book" -> {
                val libraryId = intent.getStringExtra("libraryId")
                if (libraryId == null)  {
                    Toast.makeText(this, "Помилка: Такої бібліотеки не існує", Toast.LENGTH_SHORT).show()
                    finish()
                }
                btnRegisterBook.text = "Реєстрація книги"
                btnDeleteBook.visibility = Button.GONE
                btnRegisterBook.setOnClickListener {

                    if (areFieldsEmpty()) {
                        Toast.makeText(this, "Помилка: Будь ласка, заповніть всі поля", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val title = editTextTitle.text.toString()
                    val author = editTextAuthor.text.toString()
                    val publicationYear = editTextPublicationYear.text.toString().toIntOrNull() ?: 0
                    val isbn = editTextISBN.text.toString()
                    val genre = editTextGenre.text.toString()
                    val pageCount = editTextPageCount.text.toString().toIntOrNull() ?: 0
                    val availableCopies = editTextAvailableCopies.text.toString().toIntOrNull() ?: 0
                    val publisher = editTextPublisher.text.toString()

                    val json = """{
                            "function_name": "add_book",
                            "param_dict": {
                                "title": "$title",
                                "author": "$author",
                                "publication_year": "$publicationYear",
                                "isbn": "$isbn",
                                "genre": "$genre",
                                "page_count": "$pageCount",
                                "available_copies": "$availableCopies",
                                "publisher": "$publisher",
                                "library_id": "$libraryId"
                            }
                        }"""

                    httpClient.safePostRequest(this, url, json) { jsonResponse ->
                        val resultValue = jsonResponse.getInt("result")
                        when {
                            resultValue > 0 -> {
                                Toast.makeText(this, "Книга успішно зареєстрована", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            resultValue == -1 -> {
                                Toast.makeText(this, "Помилка: ISBN книги вже існує в даній бібліотеці", Toast.LENGTH_SHORT).show()
                            }
                            else -> {
                                Toast.makeText(this, "Помилка в запиті до серверу", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            "update_book" -> {
                btnRegisterBook.text = "Оновити дані"
                btnDeleteBook.visibility = Button.VISIBLE

                val data = intent.getStringExtra("data") ?: ""

                val parts = data.split("\n")
                val dataMap = mutableMapOf<String, String>()
                for (part in parts) {
                    val keyValue = part.split(": ")
                    if (keyValue.size == 2) {
                        val key = keyValue[0].trim()
                        val value = keyValue[1].trim()
                        dataMap[key] = value
                    }
                }
                var idBook = dataMap["ID"]
                if (idBook != null) {
                    editTextTitle.setText(dataMap["Title"])
                    editTextAuthor.setText(dataMap["Author"])
                    editTextPublicationYear.setText(dataMap["Publication Year"])
                    editTextISBN.setText(dataMap["ISBN"])
                    editTextGenre.setText(dataMap["Genre"])
                    editTextPageCount.setText(dataMap["Page Count"])
                    editTextAvailableCopies.setText(dataMap["Available Copies"])
                    editTextPublisher.setText(dataMap["Publisher"])

                    btnDeleteBook.setOnClickListener {
                        val builder = AlertDialog.Builder(this)
                        builder.setTitle("Підтвердження видалення")
                        builder.setMessage("Ви впевнені, що хочете видалити цю книгу?")
                        builder.setPositiveButton("Так") { dialog, which ->
                            val json = """{
                                "function_name": "delete_book",
                                "param_dict": {
                                    "book_id": "$idBook"
                                }
                            }"""

                            httpClient.safePostRequest(this, url, json) { jsonResponse ->
                                val resultValue = jsonResponse.getInt("result")
                                when {
                                    resultValue > 0 -> {
                                        Toast.makeText(this, "Книга видалена", Toast.LENGTH_SHORT).show()
                                        finish()
                                    }
                                    resultValue == -1 -> {
                                        Toast.makeText(this, "Помилка: Такого ID книги не існує", Toast.LENGTH_SHORT).show()
                                    }
                                    else -> {
                                        Toast.makeText(this, "Помилка в запиті до серверу", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                        builder.setNegativeButton("Ні") { dialog, which ->
                            dialog.dismiss()
                        }
                        builder.show()
                    }

                    btnRegisterBook.setOnClickListener {

                        if (areFieldsEmpty()) {
                            Toast.makeText(
                                this,
                                "Помилка: Будь ласка, заповніть всі поля",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@setOnClickListener
                        }

                        val title = editTextTitle.text.toString()
                        val author = editTextAuthor.text.toString()
                        val publicationYear =
                            editTextPublicationYear.text.toString().toIntOrNull() ?: 0
                        val isbn = editTextISBN.text.toString()
                        val genre = editTextGenre.text.toString()
                        val pageCount = editTextPageCount.text.toString().toIntOrNull() ?: 0
                        val availableCopies =
                            editTextAvailableCopies.text.toString().toIntOrNull() ?: 0
                        val publisher = editTextPublisher.text.toString()

                        val json = """{
                            "function_name": "update_book",
                            "param_dict": {
                                "book_id": "$idBook",
                                "title": "$title",
                                "author": "$author",
                                "publication_year": "$publicationYear",
                                "isbn": "$isbn",
                                "genre": "$genre",
                                "page_count": "$pageCount",
                                "available_copies": "$availableCopies",
                                "publisher": "$publisher"
                            }
                        }"""

                        httpClient.safePostRequest(this, url, json) { jsonResponse ->
                            val resultValue = jsonResponse.getInt("result")
                            when {
                                resultValue > 0 -> {
                                    Toast.makeText(this, "Книга успішно оновлена", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                                resultValue == -1 -> {
                                    Toast.makeText(this, "Помилка: Такого ID книги не існує", Toast.LENGTH_SHORT).show()
                                }
                                else -> {
                                    Toast.makeText(this, "Помилка в запиті до серверу", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
                else {
                    Toast.makeText(this, "Помилка: id не існує", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            else -> {
                Toast.makeText(this, "Помилка: Такої дії не існує", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun areFieldsEmpty(): Boolean {
        val editTexts = arrayOf(
            findViewById<EditText>(R.id.editTextTitle),
            findViewById<EditText>(R.id.editTextAuthor),
            findViewById<EditText>(R.id.editTextISBN),
            findViewById<EditText>(R.id.editTextGenre),
            findViewById<EditText>(R.id.editTextAvailableCopies)
        )

        for (editText in editTexts) {
            if (editText.text.toString().isEmpty()) {
                return true
            }
        }
        return false
    }
}