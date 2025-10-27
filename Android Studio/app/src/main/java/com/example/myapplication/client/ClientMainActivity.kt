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
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException


class ClientMainActivity : AppCompatActivity() {
    @SuppressLint("SetTextI18n")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val editTextLogin: EditText = findViewById(R.id.editTextLogin)
        val editTextPassword: EditText = findViewById(R.id.editTextPassword)
        val httpClient = HttpClient()

        val buttonLogin : Button = findViewById(R.id.btnLogin)
        buttonLogin .setOnClickListener {

            //val login = editTextLogin.text.toString()
            //val password = editTextPassword.text.toString()
            val login = "djoker0203"
            val password = "12345678"
            if (login.isNotEmpty() && password.isNotEmpty()) {
                if (password.length > 7) {
                    val url = getString(R.string.server_url)
                    val json = """{
                            "function_name": "check_librarian_credentials",
                            "param_dict": {
                                "login": "$login",
                                "password": "$password"
                            }
                        }"""

                    httpClient.postRequest(url, json, object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            runOnUiThread {
                                Toast.makeText(this@ClientMainActivity, "Помилка: Перевірте з'єднання з інтернетом або повторіть спробу пізніше.", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onResponse(call: Call, response: Response) {
                            if (!response.isSuccessful) {
                                runOnUiThread {
                                    Toast.makeText(this@ClientMainActivity, "Помилка на сервері, вибачте за незручності.", Toast.LENGTH_SHORT).show()
                                }
                                return
                            }

                            try {
                                runOnUiThread {
                                    val resultValue =
                                        JSONObject(response.body?.use { it?.string() })["result"]
                                    if (resultValue is JSONArray) {
                                        val intent = Intent(this@ClientMainActivity, ClientMainMenu::class.java)
                                        intent.putExtra("librarianId", resultValue.getString(0))
                                        intent.putExtra("libraryId", resultValue.getString(1))
                                        intent.putExtra("password", password)
                                        Toast.makeText(
                                            this@ClientMainActivity,
                                            "Вхід в систему успішний",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                        this@ClientMainActivity.startActivity(intent)
                                    } else if (resultValue == -1) {
                                        Toast.makeText(this@ClientMainActivity, "Помилка: неправильний логін або пароль", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(this@ClientMainActivity, "Помилка в запиті до серверу", Toast.LENGTH_SHORT).show()
                                    }
                                }

                            } catch (e: Exception) {
                                runOnUiThread {
                                    Toast.makeText(this@ClientMainActivity, "Помилка при обробці відповіді сервера.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    })
                } else {
                    Toast.makeText(this, "Помилка: Пароль має бути 8 або біільше символів.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Помилка: Введіть всі поля.", Toast.LENGTH_SHORT).show()
            }
        }

        val buttonRegister : Button = findViewById(R.id.btnRegister)
        buttonRegister .setOnClickListener {
            // startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}