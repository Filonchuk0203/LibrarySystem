package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class AddReader : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_reader)

        val editTextLastName: EditText = findViewById(R.id.editTextLastName)
        val editTextFirstName: EditText = findViewById(R.id.editTextFirstName)
        val editTextMiddleName: EditText = findViewById(R.id.editTextMiddleName)
        val editTextAddress: EditText = findViewById(R.id.editTextAddress)
        val editTextPhone: EditText = findViewById(R.id.editTextPhone)
        val editTextEmail: EditText = findViewById(R.id.editTextEmail)

        val buttonBack: Button = findViewById(R.id.buttonBack)
        buttonBack.setOnClickListener {
            finish()
        }

        val httpClient = HttpClient()
        val url = getString(R.string.server_url)

        val btnRegisterReader: Button = findViewById(R.id.btnRegisterReader)
        val btnDeleteReader: Button = findViewById(R.id.btnDeleteReader)

        val type = intent.getStringExtra("type") ?: "add_reader"
        when (type) {
            "add_reader" -> {
                val libraryId = intent.getStringExtra("libraryId")
                if (libraryId == null)  {
                    Toast.makeText(this, "Помилка: Такої бібліотеки не існує", Toast.LENGTH_SHORT).show()
                    finish()
                }
                btnRegisterReader.text = "Реєстрація читача"
                btnDeleteReader.visibility = Button.GONE
                btnRegisterReader.setOnClickListener {

                    if (areFieldsEmpty()) {
                        Toast.makeText(this, "Помилка: Будь ласка, заповніть всі поля", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val lastName = editTextLastName.text.toString()
                    val firstName = editTextFirstName.text.toString()
                    val middleName = editTextMiddleName.text.toString()
                    val address = editTextAddress.text.toString()
                    val phone = editTextPhone.text.toString()
                    val email = editTextEmail.text.toString()

                    val json = """{
                            "function_name": "add_reader",
                            "param_dict": {
                                "last_name": "$lastName",
                                "first_name": "$firstName",
                                "middle_name": "$middleName",
                                "address": "$address",
                                "phone": "$phone",
                                "email": "$email",
                                "library_id": "$libraryId"
                            }
                        }"""

                    httpClient.postRequest(url, json, object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            runOnUiThread {
                                Toast.makeText(this@AddReader, "Помилка: Перевірте з'єднання з інтернетом або повторіть спробу пізніше.", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onResponse(call: Call, response: Response) {
                            if (!response.isSuccessful) {
                                runOnUiThread {
                                    Toast.makeText(this@AddReader, "Помилка на сервері, вибачте за незручності.", Toast.LENGTH_SHORT).show()
                                }
                                return
                            }

                            try {
                                runOnUiThread {
                                    val resultValue = JSONObject(response.body?.use { it?.string() }).getInt("result")
                                    if (resultValue > 0) {
                                        Toast.makeText(
                                            this@AddReader,
                                            "Читач успішно зареєстрований",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        this@AddReader.finish()
                                    } else if (resultValue == -1) {
                                        Toast.makeText(this@AddReader, "Помилка: Не вдалося зареєструвати читача", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(this@AddReader, "Помилка в запиті до серверу", Toast.LENGTH_SHORT).show()
                                    }
                                }

                            } catch (e: Exception) {
                                runOnUiThread {
                                    Toast.makeText(this@AddReader, "Помилка при обробці відповіді сервера.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    })
                }
            }
            "update_reader" -> {
                btnRegisterReader.text = "Оновити дані"
                btnDeleteReader.visibility = Button.VISIBLE

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

                var idReader = dataMap["ID"]
                if (idReader != null) {
                    editTextLastName.setText(dataMap["Last Name"])
                    editTextFirstName.setText(dataMap["First Name"])
                    editTextMiddleName.setText(dataMap["Middle Name"])
                    editTextAddress.setText(dataMap["Address"])
                    editTextPhone.setText(dataMap["Phone"])
                    editTextEmail.setText(dataMap["Email"])

                    btnDeleteReader.setOnClickListener {
                        val builder = AlertDialog.Builder(this)
                        builder.setTitle("Підтвердження видалення")
                        builder.setMessage("Ви впевнені, що хочете видалити цього читача?")
                        builder.setPositiveButton("Так") { dialog, which ->
                            val json = """{
                                "function_name": "delete_reader",
                                "param_dict": {
                                    "reader_id": "$idReader"
                                }
                            }"""
                            httpClient.postRequest(url, json, object : Callback {
                                override fun onFailure(call: Call, e: IOException) {
                                    runOnUiThread {
                                        Toast.makeText(this@AddReader, "Помилка: Перевірте з'єднання з інтернетом або повторіть спробу пізніше.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                override fun onResponse(call: Call, response: Response) {
                                    if (!response.isSuccessful) {
                                        runOnUiThread {
                                            Toast.makeText(this@AddReader, "Помилка на сервері, вибачте за незручності.", Toast.LENGTH_SHORT).show()
                                        }
                                        return
                                    }
                                    try {
                                        runOnUiThread {
                                            val resultValue = JSONObject(response.body?.use { it?.string() }).getInt("result")
                                            if (resultValue > 0) {
                                                Toast.makeText(
                                                    this@AddReader,
                                                    "Читач видалений",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                this@AddReader.finish()
                                            } else if (resultValue == -1) {
                                                Toast.makeText(this@AddReader, "Помилка: Такого ID читача не існує", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(this@AddReader, "Помилка в запиті до серверу", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        runOnUiThread {
                                            Toast.makeText(this@AddReader, "Помилка при обробці відповіді сервера.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            })
                        }
                        builder.setNegativeButton("Ні") { dialog, which ->
                            dialog.dismiss()
                        }
                        builder.show()
                    }


                    btnRegisterReader.setOnClickListener {

                        if (areFieldsEmpty()) {
                            Toast.makeText(
                                this,
                                "Помилка: Будь ласка, заповніть всі поля",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@setOnClickListener
                        }

                        val lastName = editTextLastName.text.toString()
                        val firstName = editTextFirstName.text.toString()
                        val middleName = editTextMiddleName.text.toString()
                        val address = editTextAddress.text.toString()
                        val phone = editTextPhone.text.toString()
                        val email = editTextEmail.text.toString()

                        val json = """{
                            "function_name": "update_reader",
                            "param_dict": {
                                "last_name": "$lastName",
                                "first_name": "$firstName",
                                "middle_name": "$middleName",
                                "address": "$address",
                                "phone": "$phone",
                                "email": "$email",
                                "reader_id": "$idReader"
                            }
                        }"""

                        httpClient.postRequest(url, json, object : Callback {
                            override fun onFailure(call: Call, e: IOException) {
                                runOnUiThread {
                                    Toast.makeText(this@AddReader, "Помилка: Перевірте з'єднання з інтернетом або повторіть спробу пізніше.", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onResponse(call: Call, response: Response) {
                                if (!response.isSuccessful) {
                                    runOnUiThread {
                                        Toast.makeText(this@AddReader, "Помилка на сервері, вибачте за незручності.", Toast.LENGTH_SHORT).show()
                                    }
                                    return
                                }

                                try {
                                    runOnUiThread {
                                        val resultValue = JSONObject(response.body?.use { it?.string() }).getInt("result")
                                        if (resultValue > 0) {
                                            Toast.makeText(
                                                this@AddReader,
                                                "Читач успішно оновлений",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            this@AddReader.finish()
                                        } else if (resultValue == -1) {
                                            Toast.makeText(this@AddReader, "Помилка: Такого ID читача не існує", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(this@AddReader, "Помилка в запиті до серверу", Toast.LENGTH_SHORT).show()
                                        }
                                    }

                                } catch (e: Exception) {
                                    runOnUiThread {
                                        Toast.makeText(this@AddReader, "Помилка при обробці відповіді сервера.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        })
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
            findViewById<EditText>(R.id.editTextLastName),
            findViewById<EditText>(R.id.editTextFirstName),
            findViewById<EditText>(R.id.editTextPhone),
        )

        for (editText in editTexts) {
            if (editText.text.toString().isEmpty()) {
                return true
            }
        }

        return false
    }
}