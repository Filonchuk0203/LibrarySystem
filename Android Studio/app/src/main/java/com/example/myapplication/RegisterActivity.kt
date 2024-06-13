package com.example.myapplication

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import androidx.viewpager2.widget.ViewPager2

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register)

        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)

        val adapter = UserLibraryPagerAdapter(this)
        viewPager.adapter = adapter

        val colors = intArrayOf(
            Color.parseColor("#FFFFFF"),
            Color.parseColor("#a0a0a0")
        )
        val states = arrayOf(
            intArrayOf(android.R.attr.state_selected),
            intArrayOf(-android.R.attr.state_selected)
        )

        val colorList = ColorStateList(states, colors)
        tabLayout.tabTextColors = colorList


        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "Реєстрація користувача"
                1 -> tab.text = "Реєстрація бібліотеки"
            }
        }.attach()

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                tab.view.setBackgroundColor(Color.parseColor("#4f81e5"))
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                tab.view.setBackgroundColor(Color.parseColor("#036EE0"))
            }

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        tabLayout.getTabAt(0)?.view?.setBackgroundColor(Color.parseColor("#4f81e5"))
    }
}