package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException


class MainActivity : AppCompatActivity() {
    @SuppressLint("SetTextI18n")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val editTextLogin: EditText = findViewById(R.id.editTextLogin)
        val editTextPassword: EditText = findViewById(R.id.editTextPassword)
        val httpClient = HttpClient()

        val buttonLogin : Button = findViewById(R.id.btnLogin)
        buttonLogin .setOnClickListener {

//            val intent = Intent(this, MainMenu::class.java)
//            intent.putExtra("librarianId", "227124f4-fa38-4d89-a271-251ee8a92e21")
//            intent.putExtra("libraryId", "e5589965-f655-4919-ae58-5c8b39eadd39")
//            this.startActivity(intent)

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
                                Toast.makeText(this@MainActivity, "Помилка: Перевірте з'єднання з інтернетом або повторіть спробу пізніше.", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onResponse(call: Call, response: Response) {
                            if (!response.isSuccessful) {
                                runOnUiThread {
                                    Toast.makeText(this@MainActivity, "Помилка на сервері, вибачте за незручності.", Toast.LENGTH_SHORT).show()
                                }
                                return
                            }

                            try {
                                runOnUiThread {
                                    val resultValue =
                                        JSONObject(response.body?.use { it?.string() })["result"]
                                    if (resultValue is JSONArray) {
                                        val intent = Intent(this@MainActivity, MainMenu::class.java)
                                        intent.putExtra("librarianId", resultValue.getString(0))
                                        intent.putExtra("libraryId", resultValue.getString(1))
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Вхід в систему успішний",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        this@MainActivity.startActivity(intent)
                                    } else if (resultValue == -1) {
                                        Toast.makeText(this@MainActivity, "Помилка: неправильний логін або пароль", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(this@MainActivity, "Помилка в запиті до серверу", Toast.LENGTH_SHORT).show()
                                    }
                                }

                            } catch (e: Exception) {
                                runOnUiThread {
                                    Toast.makeText(this@MainActivity, "Помилка при обробці відповіді сервера.", Toast.LENGTH_SHORT).show()
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
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}