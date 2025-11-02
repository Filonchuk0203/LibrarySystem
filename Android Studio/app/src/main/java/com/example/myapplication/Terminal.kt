package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.myapplication.client.ClientMainMenu
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                startQRScanner()
            } else {
                cameraPermissionRequest.launch(Manifest.permission.CAMERA)
            }
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

    private val cameraPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startQRScanner()
            else Toast.makeText(this, "Доступ до камери заборонений", Toast.LENGTH_SHORT).show()
        }

    private val qrScanLauncher =
        registerForActivityResult(ScanContract()) { result ->
            if (result.contents != null) {
                handleQRCode(result.contents)
            } else {
                Toast.makeText(this, "Сканування скасовано", Toast.LENGTH_SHORT).show()
            }
        }

    // --- Блокуємо кнопку "Назад" ---
    override fun onBackPressed() {
        Toast.makeText(this, "Повернення заблоковано", Toast.LENGTH_SHORT).show()
    }

    private fun startQRScanner() {
        val options = ScanOptions()
        options.setPrompt("Наведи камеру на QR-код клієнта")
        options.setBeepEnabled(true)
        options.setOrientationLocked(false)
        qrScanLauncher.launch(options)
    }

    private fun handleQRCode(qrData: String) {
        try {
            // Наприклад, QR містить рядок типу: "ClientID:xxx;Password:yyy"
            val parts = qrData.split(";")
            val clientId = parts.find { it.startsWith("ClientID:") }?.substringAfter("ClientID:")?.trim()
            val password = parts.find { it.startsWith("Password:") }?.substringAfter("Password:")?.trim()

            if (clientId != null && password != null) {
                Toast.makeText(this, "Успішно: $clientId", Toast.LENGTH_SHORT).show()

                // 🔹 Переходимо в TerminalMainMenu
                val intent = Intent(this, ClientMainMenu::class.java)
                intent.putExtra("ClientID", clientId)
                intent.putExtra("password", password)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Некоректний QR-код", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Помилка при зчитуванні коду", Toast.LENGTH_SHORT).show()
        }
    }

}