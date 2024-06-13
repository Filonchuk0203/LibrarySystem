package com.example.myapplication

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Table
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.Manifest
import androidx.core.app.ActivityCompat
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.layout.Style
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.properties.UnitValue
import android.app.DatePickerDialog
import android.widget.DatePicker
import java.util.Calendar

class MainMenu : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_menu)

        val librarianId = intent.getStringExtra("librarianId")
        val libraryId = intent.getStringExtra("libraryId")
        if (librarianId == null || libraryId == null)  {
            Toast.makeText(this, "Помилка: Такого бібліотекаря або бібліотеки не існує", Toast.LENGTH_SHORT).show()
            finish()
        }
        val btnAddBook: Button = findViewById(R.id.btnAddBook)
        val btnViewBooks: Button = findViewById(R.id.btnViewBooks)
        val btnDeleteBook: Button = findViewById(R.id.btnDeleteBook)
        val httpClient = HttpClient()
        val url = getString(R.string.server_url)
        var json = String()


        val btnBook = findViewById<Button>(R.id.btnBook)
        val subButtonBook = findViewById<LinearLayout>(R.id.subButtonBook)
        val btnReader = findViewById<Button>(R.id.btnReader)
        val subButtonReader = findViewById<LinearLayout>(R.id.subButtonReader)
        val btnIssuanceAndReturn = findViewById<Button>(R.id.btnIssuanceAndReturn)
        val subButtonIssuanceAndReturn = findViewById<LinearLayout>(R.id.subButtonIssuanceAndReturn)

        btnBook.setOnClickListener {
            if (subButtonBook.visibility == View.VISIBLE) {
                subButtonBook.visibility = View.GONE
            } else {
                subButtonBook.visibility = View.VISIBLE
            }
        }

        btnReader.setOnClickListener {
            if (subButtonReader.visibility == View.VISIBLE) {
                subButtonReader.visibility = View.GONE
            } else {
                subButtonReader.visibility = View.VISIBLE
            }
        }

        btnIssuanceAndReturn.setOnClickListener {
            if (subButtonIssuanceAndReturn.visibility == View.VISIBLE) {
                subButtonIssuanceAndReturn.visibility = View.GONE
            } else {
                subButtonIssuanceAndReturn.visibility = View.VISIBLE
            }
        }

        btnAddBook.setOnClickListener(View.OnClickListener {
            val intent = Intent(this, AddBook::class.java)
            intent.putExtra("libraryId", libraryId)
            this.startActivity(intent)
        })

        btnViewBooks.setOnClickListener(View.OnClickListener {
            val intent = Intent(this, TableViewer::class.java)
            intent.putExtra("type", "book")
            intent.putExtra("libraryId", libraryId)
            startActivity(intent)
        })

        btnDeleteBook.setOnClickListener {
            val bookCharacteristics = arrayOf("Назва", "Автор", "Рік", "ISBN", "Видавництво")
            val dialogParameter = AlertDialog.Builder(this)
            val inflater = layoutInflater
            val dialogLayout = inflater.inflate(R.layout.dialog_layout, null)
            val autoCompleteTextView: AutoCompleteTextView = dialogLayout.findViewById(R.id.autoCompleteTextView)
            val textViewHint: TextView = dialogLayout.findViewById(R.id.textViewHint)

            dialogParameter.setTitle("Оберіть параметр")
                .setItems(bookCharacteristics) { _, characteristicIndex ->
                    val selectedCharacteristic = bookCharacteristics[characteristicIndex]
                    var param = ""
                    when (selectedCharacteristic) {
                        "Назва" -> param = "title"
                        "Автор" -> param = "author"
                        "Рік" -> param = "publicationYear"
                        "ISBN" -> param = "isbn"
                        "Видавництво" -> param = "publisher"
                    }
                    textViewHint.text = "Введіть значення параметра '$selectedCharacteristic':"
                    val allParams = mutableListOf<String>()
                    json = """{
                            "function_name": "filter_books_readers",
                            "param_dict": {
                                "library_id": "$libraryId",
                                "column_name": "$param",
                                "table_name": "Book"
                            }
                        }"""
                    httpClient.postRequest(url, json, object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            runOnUiThread {
                                Toast.makeText(this@MainMenu, "Помилка: Перевірте з'єднання з інтернетом або повторіть спробу пізніше.", Toast.LENGTH_SHORT).show()
                            }
                        }
                        override fun onResponse(call: Call, response: Response) {
                            if (!response.isSuccessful) {
                                runOnUiThread {
                                    Toast.makeText(this@MainMenu, "Помилка на сервері, вибачте за незручності.", Toast.LENGTH_SHORT).show()
                                }
                                return
                            }

                            try {
                                runOnUiThread {
                                    val resultValue = JSONObject(response.body?.use { it?.string() })["result"]
                                    if (resultValue is JSONArray) {
                                        for (i in 0 until resultValue.length()) {
                                            val resultText = resultValue.getString(i)
                                            allParams.add(resultText)
                                        }
                                        val paramAdapter = ArrayAdapter(
                                            this@MainMenu,
                                            android.R.layout.simple_list_item_1,
                                            allParams
                                        )
                                        autoCompleteTextView.setAdapter(paramAdapter)

                                    } else {
                                        Toast.makeText(this@MainMenu, "Помилка в запиті до серверу", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                runOnUiThread {
                                    Toast.makeText(this@MainMenu, "Помилка при обробці відповіді сервера.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    })
                    val dialogValue = AlertDialog.Builder(this)
                    dialogValue.setView(dialogLayout)
                        .setPositiveButton("OK") { dialogInterface, _ ->
                            val enteredText = autoCompleteTextView.text.toString()
                            if (allParams.contains(enteredText)) {
                                val confirmDialogBuilder = AlertDialog.Builder(this)
                                confirmDialogBuilder.setTitle("Підтвердження видалення")
                                    .setMessage("Бажаєте видалити всі книжки з параметром '$selectedCharacteristic', значенням якого є '$enteredText'?")
                                    .setPositiveButton("Так") { dialogInterface, _ ->

                                        json = """{
                                            "function_name": "delete_books_readers_for_param",
                                            "param_dict": {
                                                "library_id": "$libraryId",
                                                "column_name": "$param",
                                                "table_name": "Book",
                                                "value": "$enteredText"
                                            }
                                        }"""

                                        httpClient.postRequest(url, json, object : Callback {
                                            override fun onFailure(call: Call, e: IOException) {
                                                runOnUiThread {
                                                    Toast.makeText(this@MainMenu, "Помилка: Перевірте з'єднання з інтернетом або повторіть спробу пізніше.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            override fun onResponse(call: Call, response: Response) {
                                                if (!response.isSuccessful) {
                                                    runOnUiThread {
                                                        Toast.makeText(this@MainMenu, "Помилка на сервері, вибачте за незручності.", Toast.LENGTH_SHORT).show()
                                                    }
                                                    return
                                                }
                                                try {
                                                    runOnUiThread {
                                                        val resultValue = JSONObject(response.body?.use { it?.string() }).getInt("result")
                                                        if (resultValue > 0) {
                                                            Toast.makeText(
                                                                this@MainMenu,
                                                                "Видалені книги: $resultValue",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        } else if (resultValue == -1) {
                                                            Toast.makeText(this@MainMenu, "Книг з таким значенням параметра не існує", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            Toast.makeText(this@MainMenu, "Помилка в запиті до серверу", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    runOnUiThread {
                                                        Toast.makeText(this@MainMenu, "Помилка при обробці відповіді сервера.", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        })
                                        dialogInterface.dismiss()
                                    }
                                    .setNegativeButton("Ні") { dialogInterface, _ ->
                                        Toast.makeText(this, "Видалення відмінено", Toast.LENGTH_SHORT).show()
                                        dialogInterface.dismiss()
                                    }
                                    .show()
                            } else {
                                Toast.makeText(this, "Введеного значення для обраного параметра не існує", Toast.LENGTH_SHORT).show()
                            }
                            dialogInterface.dismiss()
                        }
                        .setNegativeButton("Скасувати") { dialogInterface, _ ->
                            dialogInterface.dismiss()
                        }
                        .show()
                }
                .setNegativeButton("Скасувати") { dialogInterface, _ ->
                    dialogInterface.dismiss()
                }
                .show()
        }

        val btnAddReader: Button = findViewById(R.id.btnAddReader)
        val btnViewReaders: Button = findViewById(R.id.btnViewReaders)
        val btnDeleteReader: Button = findViewById(R.id.btnDeleteReader)

        btnAddReader.setOnClickListener(View.OnClickListener {
            val intent = Intent(this, AddReader::class.java)
            intent.putExtra("libraryId", libraryId)
            this.startActivity(intent)
        })

        btnViewReaders.setOnClickListener(View.OnClickListener {
            val intent = Intent(this, TableViewer::class.java)
            intent.putExtra("type", "reader")
            intent.putExtra("libraryId", libraryId)
            startActivity(intent)
        })

        btnDeleteReader.setOnClickListener {
            val readerCharacteristics = arrayOf("Прізвище", "Ім'я", "По-батькові", "Номер телефону", "Адреса", "Емайл")
            val dialogParameter = AlertDialog.Builder(this)
            val inflater = layoutInflater
            val dialogLayout = inflater.inflate(R.layout.dialog_layout, null)
            val autoCompleteTextView: AutoCompleteTextView = dialogLayout.findViewById(R.id.autoCompleteTextView)
            val textViewHint: TextView = dialogLayout.findViewById(R.id.textViewHint)

            dialogParameter.setTitle("Оберіть параметр")
                .setItems(readerCharacteristics) { _, characteristicIndex ->
                    val selectedCharacteristic = readerCharacteristics[characteristicIndex]
                    var param = ""
                    when (selectedCharacteristic){
                        "Прізвище" -> param = "lastName"
                        "Ім'я" -> param = "firstName"
                        "По-батькові" -> param = "middleName"
                        "Номер телефону" -> param = "phone"
                        "Адреса" -> param = "address"
                        "Емайл" -> param = "email"
                    }
                    textViewHint.text = "Введіть значення параметра '$selectedCharacteristic':"
                    val allParams = mutableListOf<String>()
                    json = """{
                            "function_name": "filter_books_readers",
                            "param_dict": {
                                "library_id": "$libraryId",
                                "column_name": "$param",
                                "table_name": "Reader"
                            }
                        }"""
                    httpClient.postRequest(url, json, object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            runOnUiThread {
                                Toast.makeText(this@MainMenu, "Помилка: Перевірте з'єднання з інтернетом або повторіть спробу пізніше.", Toast.LENGTH_SHORT).show()
                            }
                        }
                        override fun onResponse(call: Call, response: Response) {
                            if (!response.isSuccessful) {
                                runOnUiThread {
                                    Toast.makeText(this@MainMenu, "Помилка на сервері, вибачте за незручності.", Toast.LENGTH_SHORT).show()
                                }
                                return
                            }

                            try {
                                runOnUiThread {
                                    val resultValue = JSONObject(response.body?.use { it?.string() })["result"]
                                    if (resultValue is JSONArray) {
                                        for (i in 0 until resultValue.length()) {
                                            val resultText = resultValue.getString(i)
                                            allParams.add(resultText)
                                        }
                                        val paramAdapter = ArrayAdapter(
                                            this@MainMenu,
                                            android.R.layout.simple_list_item_1,
                                            allParams
                                        )
                                        autoCompleteTextView.setAdapter(paramAdapter)

                                    } else {
                                        Toast.makeText(this@MainMenu, "Помилка в запиті до серверу", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                runOnUiThread {
                                    Toast.makeText(this@MainMenu, "Помилка при обробці відповіді сервера.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    })
                    val dialogValue = AlertDialog.Builder(this)
                    dialogValue.setView(dialogLayout)
                        .setPositiveButton("OK") { dialogInterface, _ ->
                            val enteredText = autoCompleteTextView.text.toString()
                            if (allParams.contains(enteredText)) {
                                val confirmDialogBuilder = AlertDialog.Builder(this)
                                confirmDialogBuilder.setTitle("Підтвердження видалення")
                                    .setMessage("Бажаєте видалити всіх читачів з параметром '$selectedCharacteristic', значенням якого є '$enteredText'?")
                                    .setPositiveButton("Так") { dialogInterface, _ ->

                                        json = """{
                                            "function_name": "delete_books_readers_for_param",
                                            "param_dict": {
                                                "library_id": "$libraryId",
                                                "column_name": "$param",
                                                "table_name": "Reader",
                                                "value": "$enteredText"
                                            }
                                        }"""

                                        httpClient.postRequest(url, json, object : Callback {
                                            override fun onFailure(call: Call, e: IOException) {
                                                runOnUiThread {
                                                    Toast.makeText(this@MainMenu, "Помилка: Перевірте з'єднання з інтернетом або повторіть спробу пізніше.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            override fun onResponse(call: Call, response: Response) {
                                                if (!response.isSuccessful) {
                                                    runOnUiThread {
                                                        Toast.makeText(this@MainMenu, "Помилка на сервері, вибачте за незручності.", Toast.LENGTH_SHORT).show()
                                                    }
                                                    return
                                                }
                                                try {
                                                    runOnUiThread {
                                                        val resultValue = JSONObject(response.body?.use { it?.string() }).getInt("result")
                                                        if (resultValue > 0) {
                                                            Toast.makeText(
                                                                this@MainMenu,
                                                                "Видалені читачі: $resultValue",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        } else if (resultValue == -1) {
                                                            Toast.makeText(this@MainMenu, "Читачів з таким значенням параметра не існує", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            Toast.makeText(this@MainMenu, "Помилка в запиті до серверу", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    runOnUiThread {
                                                        Toast.makeText(this@MainMenu, "Помилка при обробці відповіді сервера.", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        })
                                        dialogInterface.dismiss()
                                    }
                                    .setNegativeButton("Ні") { dialogInterface, _ ->
                                        Toast.makeText(this, "Видалення відмінено", Toast.LENGTH_SHORT).show()
                                        dialogInterface.dismiss()
                                    }
                                    .show()
                            } else {
                                Toast.makeText(this, "Введеного значення для обраного параметра не існує", Toast.LENGTH_SHORT).show()
                            }
                            dialogInterface.dismiss()
                        }
                        .setNegativeButton("Скасувати") { dialogInterface, _ ->
                            dialogInterface.dismiss()
                        }
                        .show()
                }
                .setNegativeButton("Скасувати") { dialogInterface, _ ->
                    dialogInterface.dismiss()
                }
                .show()
        }

        val btnIssuance: Button = findViewById(R.id.btnIssuance)
        val btnReturn: Button = findViewById(R.id.btnReturn)
        val btnІssuedBooks: Button = findViewById(R.id.btnІssuedBooks)


        btnIssuance.setOnClickListener(View.OnClickListener {
            val intent = Intent(this, Issuance::class.java)
            intent.putExtra("librarianId", librarianId)
            intent.putExtra("libraryId", libraryId)
            intent.putExtra("type", "issue")
            startActivity(intent)
        })

        btnReturn.setOnClickListener(View.OnClickListener {
            val intent = Intent(this, Issuance::class.java)
            intent.putExtra("librarianId", librarianId)
            intent.putExtra("libraryId", libraryId)
            intent.putExtra("type", "return")
            startActivity(intent)
        })

        btnІssuedBooks.setOnClickListener(View.OnClickListener {
            val intent = Intent(this, TableViewer::class.java)
            intent.putExtra("type", "issue")
            intent.putExtra("libraryId", libraryId)
            startActivity(intent)
        })

        val btnFilter: Button = findViewById(R.id.btnFilter)

        btnFilter.setOnClickListener(View.OnClickListener {
            val tableOptions = arrayOf("Книги", "Читачі", "Видача")

            val tableDialog = AlertDialog.Builder(this)
                .setTitle("Виберіть таблицю для фільтрів:")
                .setItems(tableOptions) { _, tableIndex ->
                    // Отримання вибраної таблиці
                    val selectedTable = tableOptions[tableIndex]

                    // Створення діалогового вікна для вибору характеристики
                    val characteristicsDialog = AlertDialog.Builder(this)
                        .setTitle("Виберіть характеристику:")

                    when (selectedTable) {
                        "Книги" -> {
                            val bookCharacteristics = arrayOf("Назва", "Автор", "Рік", "ISBN", "Видавництво")
                            characteristicsDialog.setItems(bookCharacteristics) { _, characteristicIndex ->
                                val selectedCharacteristic = bookCharacteristics[characteristicIndex]
                                var param = ""
                                when (selectedCharacteristic){
                                    "Назва" -> param = "title"
                                    "Автор" -> param = "author"
                                    "Рік" -> param = "publicationYear"
                                    "ISBN" -> param = "isbn"
                                    "Видавництво" -> param = "publisher"
                                }

                                val intent = Intent(this, Filter::class.java)
                                intent.putExtra("table", "Book")
                                intent.putExtra("param", param)
                                intent.putExtra("libraryId", libraryId)
                                startActivity(intent)
                            }
                        }
                        "Читачі" -> {
                            val readerCharacteristics = arrayOf("Прізвище", "Ім'я", "По-батькові", "Номер телефону", "Адреса", "Емайл")
                            characteristicsDialog.setItems(readerCharacteristics) { _, characteristicIndex ->
                                val selectedCharacteristic = readerCharacteristics[characteristicIndex]
                                var param = ""
                                when (selectedCharacteristic){
                                    "Прізвище" -> param = "lastName"
                                    "Ім'я" -> param = "firstName"
                                    "По-батькові" -> param = "middleName"
                                    "Номер телефону" -> param = "phone"
                                    "Адреса" -> param = "address"
                                    "Емайл" -> param = "email"
                                }

                                val intent = Intent(this, Filter::class.java)
                                intent.putExtra("table", "Reader")
                                intent.putExtra("param", param)
                                intent.putExtra("libraryId", libraryId)
                                startActivity(intent)
                            }
                        }
                        "Видача" -> {
                            val issueCharacteristics = arrayOf("Читач", "Книга", "Статус", "Дата видачі", "Дата повернення")
                            characteristicsDialog.setItems(issueCharacteristics) { _, characteristicIndex ->
                                val selectedCharacteristic = issueCharacteristics[characteristicIndex]
                                var param = ""
                                when (selectedCharacteristic){
                                    "Читач" -> param = "lastName"
                                    "Книга" -> param = "title"
                                    "Статус" -> param = "status"
                                    "Дата видачі" -> param = "issueDate"
                                    "Дата повернення" -> param = "returnDate"
                                }

                                val intent = Intent(this, Filter::class.java)
                                intent.putExtra("table", "Issue")
                                intent.putExtra("param", param)
                                intent.putExtra("libraryId", libraryId)
                                startActivity(intent)
                            }
                        }
                    }

                    characteristicsDialog.show()
                }
                .setNegativeButton("Відмінити", null)

            tableDialog.show()
        })


        val btnReport: Button = findViewById(R.id.btnReport)
        btnReport.setOnClickListener{
            if (ContextCompat.checkSelfPermission(this@MainMenu, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this@MainMenu, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            } else {
                var dateFrom: String? = null
                var dateTo: String? = null

                val calendar = Calendar.getInstance()
                val dateFromPicker = DatePickerDialog(
                    this,
                    { _: DatePicker, yearFrom: Int, monthFrom: Int, dayFrom: Int ->
                        dateFrom = "$yearFrom-${monthFrom + 1}-$dayFrom"
                        val dateToPicker = DatePickerDialog(
                            this,
                            { _: DatePicker, yearTo: Int, monthTo: Int, dayTo: Int ->
                                dateTo = "$yearTo-${monthTo + 1}-$dayTo"

                                val json = """{
                                "function_name": "get_report_data",
                                "param_dict": {
                                    "library_id": "$libraryId",
                                    "date_from": "$dateFrom",
                                    "date_to": "$dateTo"
                                }
                            }"""

                                httpClient.postRequest(url, json, object : Callback {
                                    override fun onFailure(call: Call, e: IOException) {
                                        runOnUiThread {
                                            Toast.makeText(
                                                this@MainMenu,
                                                "Помилка: Перевірте з'єднання з інтернетом або повторіть спробу пізніше.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }

                                    override fun onResponse(call: Call, response: Response) {
                                        if (!response.isSuccessful) {
                                            runOnUiThread {
                                                Toast.makeText(
                                                    this@MainMenu,
                                                    "Помилка на сервері, вибачте за незручності.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            return
                                        }

                                        try {
                                            runOnUiThread {
                                                val resultValue =
                                                    JSONObject(response.body?.use { it?.string() })["result"]
                                                if (resultValue is JSONArray) {
                                                    val allParams = mutableListOf<List<String>>()
                                                    for (i in 0 until resultValue.length()) {
                                                        val item = resultValue.getJSONArray(i)
                                                        val params = mutableListOf<String>()
                                                        params.add((i + 1).toString())
                                                        for (j in 0 until item.length()) {
                                                            params.add(item.getString(j))
                                                        }
                                                        for (row in params) {
                                                        }
                                                        allParams.add(params)
                                                    }
                                                    Toast.makeText(
                                                        this@MainMenu,
                                                        "Зачекайте, створюється звіт",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    val directory = createPdfReport(allParams)
                                                    if (directory != null) {
                                                        Toast.makeText(
                                                            this@MainMenu,
                                                            "Звіт успішно створений в ${directory.absolutePath}",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    } else {
                                                        Toast.makeText(
                                                            this@MainMenu,
                                                            "Виникла помилка під час створення звіту",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                } else {
                                                    Toast.makeText(
                                                        this@MainMenu,
                                                        "Помилка в запиті до серверу",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        } catch (e: Exception) {
                                            runOnUiThread {
                                                Toast.makeText(
                                                    this@MainMenu,
                                                    "Помилка при обробці відповіді сервера.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                })
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        )
                        dateToPicker.setTitle("Оберіть дату закінчення звіту")
                        dateToPicker.show()
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                )
                dateFromPicker.setTitle("Оберіть дату початку звіту")
                dateFromPicker.show()
            }
        }
    }

    private fun createPdfReport(data: List<List<String>>): File? {
        return try {
            val directory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            val document = Document(PdfDocument(PdfWriter(FileOutputStream(File(directory, "report.pdf")))))
            val font: PdfFont = PdfFontFactory.createFont(
                "assets/timesnewromanpsmt.ttf",
                PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED
            )
            val table = Table(UnitValue.createPercentArray(floatArrayOf(8f, 30f, 30f, 16f, 16f)))
            table.setWidth(UnitValue.createPercentValue(100f))
            val boldStyle = Style().setBold()

            table.addHeaderCell(Cell().add(Paragraph("№").addStyle(boldStyle).setFont(font)))
            table.addHeaderCell(Cell().add(Paragraph("Читач").addStyle(boldStyle).setFont(font)))
            table.addHeaderCell(Cell().add(Paragraph("Книга").addStyle(boldStyle).setFont(font)))
            table.addHeaderCell(Cell().add(Paragraph("Дата").addStyle(boldStyle).setFont(font)))
            table.addHeaderCell(Cell().add(Paragraph("Статус").addStyle(boldStyle).setFont(font)))

            for (row in data) {
                for (cell in row) {
                    table.addCell(Cell().add(Paragraph(cell).setFont(font)))
                }
            }

            val margin = 36f
            document.setMargins(margin, margin, margin, margin)
            document.add(table)
            document.close()
            directory
        } catch (e: Exception) {
            System.out.println("Error: $e")
            null
        }
    }

}