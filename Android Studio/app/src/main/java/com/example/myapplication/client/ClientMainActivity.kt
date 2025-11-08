package com.example.myapplication.client

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.myapplication.HttpClient
import com.example.myapplication.R
import org.json.JSONArray
import org.json.JSONObject

class ClientMainActivity : AppCompatActivity() {
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val editTextLogin: EditText = findViewById(R.id.editTextLogin)
        val editTextPassword: EditText = findViewById(R.id.editTextPassword)
        val httpClient = HttpClient()

        val buttonLogin: Button = findViewById(R.id.btnLogin)
        buttonLogin.setOnClickListener {

            val login = editTextLogin.text.toString()
            val password = editTextPassword.text.toString()

            if (login.isNotEmpty() && password.isNotEmpty()) {
                if (password.length > 7) {
                    val url = getString(R.string.server_url)
                    val json = """{
                        "function_name": "check_client_credentials",
                        "param_dict": {
                            "login": "$login",
                            "password": "$password"
                        }
                    }"""

                    httpClient.safePostRequest(this, url, json) { jsonResponse ->
                        val resultValue = jsonResponse["result"]
                        runOnUiThread {
                            when {
                                resultValue is JSONArray -> {
                                    val intent = Intent(this@ClientMainActivity, ClientMainMenu::class.java)
                                    intent.putExtra("ClientID", resultValue.getString(0))
                                    intent.putExtra("password", password)
                                    Toast.makeText(this@ClientMainActivity, "Вхід в систему успішний", Toast.LENGTH_SHORT).show()
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    startActivity(intent)
                                }
                                resultValue == -1 -> {
                                    Toast.makeText(this@ClientMainActivity, "Помилка: неправильний логін або пароль", Toast.LENGTH_SHORT).show()
                                }
                                else -> {
                                    Toast.makeText(this@ClientMainActivity, "Помилка в запиті до серверу", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                } else {
                    Toast.makeText(this, "Помилка: Пароль має бути 8 або більше символів.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Помилка: Введіть всі поля.", Toast.LENGTH_SHORT).show()
            }
        }

        val buttonRegister: Button = findViewById(R.id.btnRegister)
        buttonRegister.setOnClickListener {
            startActivity(Intent(this@ClientMainActivity, ClientRegistration::class.java))
        }
    }
}
