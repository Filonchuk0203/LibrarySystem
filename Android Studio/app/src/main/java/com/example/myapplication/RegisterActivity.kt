package com.example.myapplication

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register)

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val adapter = UserLibraryPagerAdapter(this)
        viewPager.adapter = adapter

        val tabLibrarian = findViewById<TextView>(R.id.tabLibrarian)
        val tabLibrary = findViewById<TextView>(R.id.tabLibrary)

        val selectedTextColor = Color.WHITE
        val unselectedTextColor = Color.parseColor("#9ca3af")

        fun selectTab(index: Int) {
            tabLibrarian.isSelected = index == 0
            tabLibrary.isSelected = index == 1

            tabLibrarian.setTextColor(if (index == 0) selectedTextColor else unselectedTextColor)
            tabLibrary.setTextColor(if (index == 1) selectedTextColor else unselectedTextColor)

            viewPager.currentItem = index
        }

        tabLibrarian.setOnClickListener { selectTab(0) }
        tabLibrary.setOnClickListener { selectTab(1) }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                selectTab(position)
            }
        })

        // Вибір першої вкладки спочатку
        selectTab(0)
    }
}