package com.example.myapplication.client

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.HttpClient
import com.example.myapplication.R
import org.json.JSONArray
import org.json.JSONObject

class ClientFilter : AppCompatActivity() {

    private lateinit var httpClient: HttpClient
    private lateinit var url: String
    private lateinit var autoCompleteTextViewFind: AutoCompleteTextView
    private lateinit var spinnerFilter: Spinner
    private lateinit var btnFind: Button
    private lateinit var listView: ListView

    private var type: String? = null
    private var currentParam: String = ""
    private var currentListItems: List<String> = emptyList()
    private var clientId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.client_filter)

        autoCompleteTextViewFind = findViewById(R.id.autoCompleteTextViewFind)
        spinnerFilter = findViewById(R.id.spinnerFilter)
        btnFind = findViewById(R.id.btnFind)
        listView = findViewById(R.id.listView)

        type = intent.getStringExtra("type")
        clientId = intent.getStringExtra("ClientID")

        if (type == null) {
            Toast.makeText(this, "Помилка: невідомий тип.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        httpClient = HttpClient()
        url = getString(R.string.server_url)

        // Spinner params
        val filterParams = when (type) {
            "Library" -> listOf("Назва", "Адреса")
            "Book", "MyBook" -> listOf("Назва", "Автор", "Видавництво", "Рік", "ISBN")
            else -> emptyList()
        }
        if (filterParams.isEmpty()) return

        val spinnerAdapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            filterParams
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.setTextColor(Color.WHITE)
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.setTextColor(Color.WHITE)
                view.setPadding(view.paddingLeft, 20, view.paddingRight, 20)
                return view
            }
        }
        spinnerFilter.adapter = spinnerAdapter
        spinnerFilter.setSelection(0)
        currentParam = filterParams[0]

        loadAllData(type!!)
        loadAutoCompleteValues(type!!, currentParam)

        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                currentParam = filterParams[position]
                loadAutoCompleteValues(type!!, currentParam)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        btnFind.setOnClickListener {
            val value = autoCompleteTextViewFind.text.toString()
            if (value.isEmpty()) {
                Toast.makeText(this, "Введіть значення для пошуку", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            searchByParam(type!!, currentParam, value)
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedItem = currentListItems.getOrNull(position) ?: return@setOnItemClickListener
            val parts = selectedItem.split("\n")
            val dataMap = mutableMapOf<String, String>()
            for (part in parts) {
                val keyValue = part.split(": ")
                if (keyValue.size == 2) dataMap[keyValue[0].trim()] = keyValue[1].trim()
            }

            when (type) {
                "Book" -> {
                    val bookTitle = dataMap["Title"] ?: selectedItem
                    val json = """{
                        "function_name": "get_libraries_with_book",
                        "param_dict": {
                            "book_title": "$bookTitle"
                        }
                    }"""
                    sendLibraryRequest(json, "Бібліотеки з цією книгою")
                }

                "MyBook" -> {
                    val bookTitle = dataMap["Title"] ?: selectedItem
                    val json = """{
                        "function_name": "get_libraries_with_mybook",
                        "param_dict": {
                            "book_title": "$bookTitle",
                            "client_id": "$clientId"
                        }
                    }"""
                    sendLibraryRequest(json, "Бібліотеки, де ви брали цю книгу")
                }

                "Library" -> {
                    val libraryId = dataMap["ID"] ?: return@setOnItemClickListener
                    val json = """{
                        "function_name": "get_books_in_library",
                        "param_dict": {
                            "library_id": "$libraryId"
                        }
                    }"""
                    httpClient.safePostRequest(this, url, json) { jsonResponse ->
                        val result = jsonResponse["result"]
                        if (result is JSONArray) {
                            val booksList = mutableListOf<String>()
                            for (i in 0 until result.length()) {
                                val book = result.getJSONArray(i)
                                booksList.add("Назва: ${book.getString(0)}\nАвтор: ${book.getString(1)}")
                            }
                            runOnUiThread {
                                showScrollableDialog("Книги в цій бібліотеці", booksList)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun sendLibraryRequest(json: String, dialogTitle: String) {
        httpClient.safePostRequest(this, url, json) { jsonResponse ->
            val result = jsonResponse["result"]
            if (result is JSONArray) {
                val libraryList = mutableListOf<String>()
                for (i in 0 until result.length()) {
                    val lib = result.getJSONArray(i)
                    libraryList.add("Назва: ${lib.getString(0)}\nАдреса: ${lib.getString(1)}")
                }
                runOnUiThread { showScrollableDialog(dialogTitle, libraryList) }
            }
        }
    }

    private fun showScrollableDialog(title: String, items: List<String>) {
        val dialogBuilder = android.app.AlertDialog.Builder(this)
        dialogBuilder.setTitle(title)

        val listView = ListView(this)
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.setPadding(40, 30, 40, 30)
                view.textSize = 16f
                return view
            }
        }
        listView.adapter = adapter
        dialogBuilder.setView(listView)
        dialogBuilder.setPositiveButton("Закрити") { dialog, _ -> dialog.dismiss() }
        dialogBuilder.show()
    }

    private fun loadAllData(type: String) {
        val functionName = when (type) {
            "Library" -> "get_all_libraries"
            "Book" -> "get_all_books"
            "MyBook" -> "get_all_mybooks"
            else -> return
        }

        val paramBlock = if (type == "MyBook") """"client_id": "$clientId"""" else ""
        val json = """{
            "function_name": "$functionName",
            "param_dict": { $paramBlock }
        }"""

        httpClient.safePostRequest(this, url, json) { jsonResponse ->
            val result = jsonResponse["result"]
            if (result is JSONArray) {
                val list = mutableListOf<String>()
                for (i in 0 until result.length()) list.add(result.getString(i))
                runOnUiThread {
                    currentListItems = list
                    val adapter = object : ArrayAdapter<String>(this@ClientFilter, android.R.layout.simple_list_item_1, list) {
                        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                            val view = super.getView(position, convertView, parent) as TextView
                            view.setTextColor(Color.WHITE)
                            return view
                        }
                    }
                    listView.adapter = adapter
                }
            }
        }
    }

    private fun loadAutoCompleteValues(type: String, param: String) {
        val columnName = when (param) {
            "Назва" -> if (type == "Book" || type == "MyBook") "title" else "name"
            "Адреса" -> "address"
            "Автор" -> "author"
            "Видавництво" -> "publisher"
            "Рік" -> "publicationYear"
            "ISBN" -> "isbn"
            else -> "name"
        }

        val functionName = when (type) {
            "Book" -> "filter_books"
            "MyBook" -> "filter_mybooks"
            "Library" -> "filter_libraries"
            else -> return
        }

        val clientPart = if (type == "MyBook") ""","client_id": "$clientId"""" else ""
        val json = """{
            "function_name": "$functionName",
            "param_dict": {
                "column_name": "$columnName"$clientPart
            }
        }"""

        httpClient.safePostRequest(this, url, json) { jsonResponse ->
            val result = jsonResponse["result"]
            if (result is JSONArray) {
                val options = mutableListOf<String>()
                for (i in 0 until result.length()) options.add(result.getString(i))
                runOnUiThread {
                    val adapter = ArrayAdapter(this@ClientFilter, android.R.layout.simple_dropdown_item_1line, options)
                    autoCompleteTextViewFind.setAdapter(adapter)
                    autoCompleteTextViewFind.threshold = 1
                }
            }
        }
    }

    private fun searchByParam(type: String, param: String, value: String) {
        val columnName = when (param) {
            "Назва" -> if (type == "Book" || type == "MyBook") "title" else "name"
            "Адреса" -> "address"
            "Автор" -> "author"
            "Видавництво" -> "publisher"
            "Рік" -> "publicationYear"
            "ISBN" -> "isbn"
            else -> "name"
        }

        val functionName = when (type) {
            "Book" -> "get_books_from_param"
            "MyBook" -> "get_mybooks_from_param"
            "Library" -> "get_libraries_from_param"
            else -> return
        }

        val clientPart = if (type == "MyBook") ""","client_id": "$clientId"""" else ""
        val json = """{
            "function_name": "$functionName",
            "param_dict": {
                "param": "$columnName",
                "value": "$value"$clientPart
            }
        }"""

        httpClient.safePostRequest(this, url, json) { jsonResponse ->
            val result = jsonResponse["result"]
            if (result is JSONArray) {
                val list = mutableListOf<String>()
                for (i in 0 until result.length()) list.add(result.getString(i))
                runOnUiThread {
                    currentListItems = list
                    val adapter = object : ArrayAdapter<String>(this@ClientFilter, android.R.layout.simple_list_item_1, list) {
                        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                            val view = super.getView(position, convertView, parent) as TextView
                            view.setTextColor(Color.WHITE)
                            return view
                        }
                    }
                    listView.adapter = adapter
                }
            }
        }
    }
}
