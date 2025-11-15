package com.example.myapplication.terminal

import android.Manifest
import android.content.Intent
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
import com.example.myapplication.HttpClient
import com.example.myapplication.MainMenu
import com.example.myapplication.R
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import org.json.JSONArray

class Terminal : AppCompatActivity() {

    private var libraryId: String? = null
    private var librarianId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.terminal)

        libraryId = intent.getStringExtra("libraryId")
        librarianId = intent.getStringExtra("librarianId")
        val password = intent.getStringExtra("password")
        val btnQR = findViewById<Button>(R.id.btnQR)
        val btnLoginManually = findViewById<Button>(R.id.btnLoginManually)
        val btnGuestPurchase = findViewById<Button>(R.id.btnGuestPurchase)
        val btnClose = findViewById<ImageButton>(R.id.btnClose)
        val httpClient = HttpClient()
        val url = getString(R.string.server_url)

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
            // Створюємо кастомний Layout для діалогу
            val dialogView = layoutInflater.inflate(R.layout.dialog_login, null)
            val editTextLogin = dialogView.findViewById<EditText>(R.id.editTextDialogLogin)
            val editTextPassword = dialogView.findViewById<EditText>(R.id.editTextDialogPassword)
            val btnDialogLogin = dialogView.findViewById<Button>(R.id.btnDialogLogin)

            val dialog = AlertDialog.Builder(this, R.style.CustomDialogTheme)
                .setView(dialogView)
                .setCancelable(true)
                .create()

            dialog.show()

            btnDialogLogin.setOnClickListener {
                val login = editTextLogin.text.toString().trim()
                val password = editTextPassword.text.toString().trim()

                if (login.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "Помилка: Введіть усі поля.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (password.length < 8) {
                    Toast.makeText(this, "Пароль має бути 8 або більше символів.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

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
                                val intent = Intent(this, BooksTabsActivity::class.java)
                                intent.putExtra("libraryId", libraryId)
                                intent.putExtra("ClientID", resultValue.getString(0))
                                intent.putExtra("password", password)
                                intent.putExtra("librarianId", librarianId)
                                Toast.makeText(this, "Вхід успішний", Toast.LENGTH_SHORT).show()
                                startActivity(intent)
                                dialog.dismiss()
                            }
                            resultValue == -1 -> {
                                Toast.makeText(this, "Неправильний логін або пароль", Toast.LENGTH_SHORT).show()
                            }
                            else -> {
                                Toast.makeText(this, "Помилка запиту до сервера", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }

        // --- Купівля без авторизації ---
        btnGuestPurchase.setOnClickListener {
            val intent = Intent(this, BooksTabsActivity::class.java)
            intent.putExtra("libraryId", libraryId)
            startActivity(intent)
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
        options.setPrompt("Наведіть камеру на QR-код")
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
                val intent = Intent(this, BooksTabsActivity::class.java)
                intent.putExtra("libraryId", libraryId)
                intent.putExtra("ClientID", clientId)
                intent.putExtra("password", password)
                intent.putExtra("librarianId", librarianId)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Некоректний QR-код", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Помилка при зчитуванні коду", Toast.LENGTH_SHORT).show()
        }
    }

}