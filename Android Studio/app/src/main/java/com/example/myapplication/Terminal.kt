package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class Terminal : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.terminal)

        val librarianId = intent.getStringExtra("librarianId")
        val libraryId = intent.getStringExtra("libraryId")
        val password = intent.getStringExtra("password")
        val btnQR = findViewById<Button>(R.id.btnQR)
        val btnLoginManually = findViewById<Button>(R.id.btnLoginManually)
        val btnGuestPurchase = findViewById<Button>(R.id.btnGuestPurchase)
        val btnClose = findViewById<ImageButton>(R.id.btnClose)

        // --- Авторизація через QR ---
        btnQR.setOnClickListener {

        }

        // --- Авторизація через логін ---
        btnLoginManually.setOnClickListener {

        }

        // --- Купівля без авторизації ---
        btnGuestPurchase.setOnClickListener {

        }

        // --- Кнопка-хрестик ---
        btnClose.setOnClickListener {
            // Створюємо поле для введення пароля
            val input = EditText(this)
            input.hint = "Введіть пароль"
            input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            input.setPadding(32, 40, 32, 40)

            val dialog = AlertDialog.Builder(this)
                .setTitle("Вихід із терміналу")
                .setMessage("Для виходу потрібно ввести пароль адміністратора.")
                .setView(input)
                .setPositiveButton("Підтвердити") { _, _ ->
                    val enteredPassword = input.text.toString()

                    if (enteredPassword == password) {
                        try {
                            stopLockTask()  // знімає блокування Home/Overview
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        // Якщо пароль правильний — вихід до MainMenu
                        Handler(Looper.getMainLooper()).postDelayed({
                            val intent = Intent(this, MainMenu::class.java)
                            intent.putExtra("librarianId", librarianId)
                            intent.putExtra("libraryId", libraryId)
                            intent.putExtra("password", password)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            startActivity(intent)
                        }, 1000)
                    } else {
                        // Якщо пароль неправильний — показуємо повідомлення
                        Toast.makeText(this, "Невірний пароль!", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Скасувати", null)
                .create()

            dialog.show()
        }
        // --- Активуємо kiosk mode ---
        startLockTask()
    }

    // --- Блокуємо кнопку "Назад" ---
    override fun onBackPressed() {
        Toast.makeText(this, "Повернення заблоковано", Toast.LENGTH_SHORT).show()
    }

}