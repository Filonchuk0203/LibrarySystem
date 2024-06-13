package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class Register : AppCompatActivity(){
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register)

        val editTextName: EditText = findViewById(R.id.editTextName)
        val editTextLastName: EditText = findViewById(R.id.editTextLastName)
        val editTextSurname: EditText = findViewById(R.id.editTextSurname)
        val editTextPhone: EditText = findViewById(R.id.editTextPhone)
        val editTextAddress: EditText = findViewById(R.id.editTextAddress)
        val editTextEmail: EditText = findViewById(R.id.editTextEmail)
        val editTextLogin: EditText = findViewById(R.id.editTextLogin)
        val editTextPassword: EditText = findViewById(R.id.editTextPassword)
        val editTextRepeatPassword: EditText = findViewById(R.id.editTextRepeatPassword)
        val editTextSystemPassword: EditText = findViewById(R.id.editTextSystemPassword)
        val databaseHelper = DatabaseHelper(this)

        val buttonRegister : Button = findViewById(R.id.btnRegister)
        buttonRegister .setOnClickListener {
            val systemPassword = editTextSystemPassword.text.toString()

            if (systemPassword == "new_world"){
                val password = editTextRepeatPassword.text.toString()
                val repeatPassword = editTextPassword.text.toString()
                if (password == repeatPassword && password.length > 7){
                    if (areLibrarianFieldsEmpty()) {
                        Toast.makeText(this, "Помилка: Будь ласка, заповніть всі поля", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    val name = editTextName.text.toString()
                    val lastName = editTextLastName.text.toString()
                    val surName = editTextSurname.text.toString()
                    val phone = editTextPhone.text.toString()
                    val address = editTextAddress.text.toString()
                    val email = editTextEmail.text.toString()
                    val login = editTextLogin.text.toString()
                    val librarianId = databaseHelper.addLibrarian(name, lastName, surName, phone ,address, email, login, password)
                    if (librarianId != -1){
                        val intent = Intent(this, MainMenu::class.java)
                        intent.putExtra("librarianId", librarianId)
                        startActivity(intent)
                        finish()
                    }
                    else {
                        Toast.makeText(this, "Помилка: Користувач з даним логіном вже існує", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Помилка: Пароль замалий (Менше 8 символів) або повторений неправильно", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Помилка: Невірний системний пароль", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun areLibrarianFieldsEmpty(): Boolean {
        val editTexts = arrayOf(
            findViewById<EditText>(R.id.editTextName),
            findViewById<EditText>(R.id.editTextLastName),
            findViewById<EditText>(R.id.editTextPhone),
            findViewById<EditText>(R.id.editTextAddress),
            findViewById<EditText>(R.id.editTextLogin)
        )

        for (editText in editTexts) {
            if (editText.text.toString().isEmpty()) {
                return true
            }
        }

        return false
    }
}