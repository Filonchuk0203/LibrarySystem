package com.example.myapplication.client

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.myapplication.R

class ClientMainMenu : AppCompatActivity() {

    private var isMenuVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.client_main_menu)

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        val btnMenu = findViewById<ImageButton>(R.id.btnMenu)

        btnMenu.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        // Решта кнопок поки порожні
        // findViewById<LinearLayout>(R.id.btnLibraries).setOnClickListener { ... }
        // findViewById<LinearLayout>(R.id.btnBooks).setOnClickListener { ... }
        // ...
    }
}
