package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class Terminal : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.terminal)

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
            showExitDialog()
        }
    }

    // --- Підтвердження виходу з терміналу ---
    private fun showExitDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Вихід із терміналу")
            .setMessage("Для виходу потрібно ввести пароль адміністратора.")
            .setPositiveButton("Ввести пароль") { _, _ ->
                val intent = Intent(this, MainMenu::class.java)

                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
            }
            .setNegativeButton("Скасувати", null)
            .create()
        dialog.show()
    }

    // --- Блокуємо кнопку "Назад" ---
    override fun onBackPressed() {
        Toast.makeText(this, "Повернення заблоковано", Toast.LENGTH_SHORT).show()
    }

}