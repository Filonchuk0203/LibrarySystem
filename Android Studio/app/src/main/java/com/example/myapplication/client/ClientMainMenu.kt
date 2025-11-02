package com.example.myapplication.client

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.myapplication.R
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter

class ClientMainMenu : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.client_main_menu)

        val ClientID = intent.getStringExtra("ClientID")
        val password = intent.getStringExtra("password")

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        val btnMenu = findViewById<ImageButton>(R.id.btnMenu)
        val btnQR = findViewById<LinearLayout>(R.id.btnQR)
        val btnLibraries = findViewById<LinearLayout>(R.id.btnLibraries)
        val btnBooks = findViewById<LinearLayout>(R.id.btnBooks)
        val btnMyBooks = findViewById<LinearLayout>(R.id.btnMyBooks)

        val btnEditData = findViewById<Button>(R.id.btnEditData)
        val btnChangePassword = findViewById<Button>(R.id.btnChangePassword)
        val btnChangeQR = findViewById<Button>(R.id.btnChangeQR)

        // Меню
        btnMenu.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        // --- QR-код ---
        btnQR.setOnClickListener {
            val dataToEncode = "ClientID:$ClientID;Password:$password"
            try {
                val qrCodeBitmap = generateQRCode(dataToEncode)
                showQRCodeDialog(qrCodeBitmap)
            } catch (e: Exception) {
                Toast.makeText(this, "Помилка створення QR-коду", Toast.LENGTH_SHORT).show()
            }
        }

        // --- Кнопки ---
        btnLibraries.setOnClickListener {
            val intent = Intent(this, ClientFilter::class.java)
            intent.putExtra("type", "Library")
            startActivity(intent)
        }

        btnBooks.setOnClickListener {
            val intent = Intent(this, ClientFilter::class.java)
            intent.putExtra("type", "Book")
            startActivity(intent)
        }

        btnMyBooks.setOnClickListener {
            val intent = Intent(this, ClientFilter::class.java)
            intent.putExtra("type", "MyBook")
            intent.putExtra("ClientID", ClientID)
            startActivity(intent)
        }
    }

    // ----------------- Функція для генерації QR -----------------
    private fun generateQRCode(text: String): Bitmap {
        val writer = QRCodeWriter()
        try {
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            return bitmap
        } catch (e: WriterException) {
            throw e
        }
    }

    // ----------------- Діалог із QR -----------------
    private fun showQRCodeDialog(bitmap: Bitmap) {
        val imageView = ImageView(this)
        imageView.setImageBitmap(bitmap)
        imageView.setPadding(60, 40, 60, 40)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Ваш QR-код")
            .setView(imageView)
            .setPositiveButton("Закрити") { dialogInterface, _ -> dialogInterface.dismiss() }
            .create()
        dialog.show()
    }
}
