package com.example.myapplication.client

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.HttpClient
import com.example.myapplication.R
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class ClientRegistration : AppCompatActivity() {

    private lateinit var editTextName: EditText
    private lateinit var editTextLastName: EditText
    private lateinit var editTextSurname: EditText
    private lateinit var editTextPhone: EditText
    private lateinit var editTextAddress: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextLogin: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var editTextRepeatPassword: EditText
    private lateinit var buttonRegister: Button
    private lateinit var httpClient: HttpClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.client_register)

        editTextName = findViewById(R.id.editTextName)
        editTextLastName = findViewById(R.id.editTextLastName)
        editTextSurname = findViewById(R.id.editTextSurname)
        editTextPhone = findViewById(R.id.editTextPhone)
        editTextAddress = findViewById(R.id.editTextAddress)
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextLogin = findViewById(R.id.editTextLogin)
        editTextPassword = findViewById(R.id.editTextPassword)
        editTextRepeatPassword = findViewById(R.id.editTextRepeatPassword)
        buttonRegister = findViewById(R.id.btnRegister)
        httpClient = HttpClient()

        val url = getString(R.string.server_url)

        buttonRegister.setOnClickListener {
            val password = editTextPassword.text.toString()
            val repeatPassword = editTextRepeatPassword.text.toString()

            if (password == repeatPassword && password.length > 7) {
                if (areClientFieldsEmpty(
                        editTextName,
                        editTextLastName,
                        editTextPhone,
                        editTextAddress,
                        editTextLogin
                    )
                ) {
                    Toast.makeText(
                        this,
                        "Помилка: Будь ласка, заповніть всі поля",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                val name = editTextName.text.toString()
                val lastName = editTextLastName.text.toString()
                val surName = editTextSurname.text.toString()
                val phone = editTextPhone.text.toString()
                val address = editTextAddress.text.toString()
                val email = editTextEmail.text.toString()
                val login = editTextLogin.text.toString()

                val json = """{
                    "function_name": "add_client",
                    "param_dict": {
                        "last_name": "$lastName",
                        "user_name": "$name",
                        "sur_name": "$surName",
                        "address": "$address",
                        "phone": "$phone",
                        "email": "$email",
                        "login": "$login",
                        "password": "$password"
                    }
                }"""

                httpClient.postRequest(url, json, object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        runOnUiThread {
                            Toast.makeText(
                                this@ClientRegistration,
                                "Помилка: Перевірте з'єднання з інтернетом або повторіть спробу пізніше.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (!response.isSuccessful) {
                            runOnUiThread {
                                Toast.makeText(
                                    this@ClientRegistration,
                                    "Помилка на сервері, вибачте за незручності.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            return
                        }

                        try {
                            val resultValue =
                                JSONObject(response.body?.use { it.string() })["result"]
                            runOnUiThread {
                                when (resultValue) {
                                    is JSONArray -> {
                                        Toast.makeText(
                                            this@ClientRegistration,
                                            "Реєстрація успішна!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        val intent = Intent(
                                            this@ClientRegistration,
                                            ClientMainMenu::class.java
                                        )
                                        intent.putExtra("clientId", resultValue.getString(0))
                                        intent.putExtra("password", password)
                                        startActivity(intent)
                                        finish()
                                    }

                                    -1 -> Toast.makeText(
                                        this@ClientRegistration,
                                        "Даний логін вже існує. Введіть інший.",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    else -> Toast.makeText(
                                        this@ClientRegistration,
                                        "Помилка в запиті до серверу",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        } catch (e: Exception) {
                            runOnUiThread {
                                Toast.makeText(
                                    this@ClientRegistration,
                                    "Помилка при обробці відповіді сервера.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                })
            } else {
                Toast.makeText(
                    this,
                    "Помилка: Пароль замалий (менше 8 символів) або повторений неправильно",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun areClientFieldsEmpty(
        nameEditText: EditText,
        lastNameEditText: EditText,
        phoneEditText: EditText,
        addressEditText: EditText,
        loginEditText: EditText
    ): Boolean {
        val editTexts = arrayOf(
            nameEditText,
            lastNameEditText,
            phoneEditText,
            addressEditText,
            loginEditText
        )
        for (editText in editTexts) {
            if (editText.text.toString().isEmpty()) {
                return true
            }
        }
        return false
    }
}
