package com.example.myapplication.terminal

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.example.myapplication.R
import com.google.android.material.tabs.TabLayout

class BooksTabsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_books_tabs)

        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = findViewById<ViewPager>(R.id.viewPager)

        val libraryId = intent.getStringExtra("libraryId")
        val clientId = intent.getStringExtra("ClientID")
        val password = intent.getStringExtra("password")
        val librarianId = intent.getStringExtra("librarianId")

        val tabTitles: List<String>
        val tabTypes: List<String>

        if (!clientId.isNullOrEmpty() && !password.isNullOrEmpty() && !libraryId.isNullOrEmpty() && !librarianId.isNullOrEmpty()) {
            tabTitles = listOf("Купівля", "Видача", "Повернення")
            tabTypes = listOf("purchase", "issue", "return")
        } else {
            tabTitles = listOf("Купівля")
            tabTypes = listOf("purchase")
        }

        val adapter = BooksPagerAdapter(
            supportFragmentManager,
            tabTitles,
            tabTypes,
            libraryId,
            clientId,
            password,
            librarianId
        )

        viewPager.adapter = adapter
        tabLayout.setupWithViewPager(viewPager)

        // 🔹 Завантажуємо дані лише коли користувач переходить на вкладку
        viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                adapter.getFragment(position)?.onTabVisible()
            }
        })

        // ✅ Одразу завантажуємо дані для першої вкладки ("Купівля")
        viewPager.post {
            adapter.getFragment(0)?.onTabVisible()
        }
    }
}

class BooksPagerAdapter(
    fm: androidx.fragment.app.FragmentManager,
    private val tabTitles: List<String>,
    private val tabTypes: List<String>,
    private val libraryId: String?,
    private val clientId: String?,
    private val password: String?,
    private val librarianId: String?
) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val fragments = mutableListOf<BooksTabFragment?>()

    init {
        repeat(tabTitles.size) {
            fragments.add(null)
        }
    }

    override fun getCount(): Int = tabTitles.size

    override fun getItem(position: Int): Fragment {
        val fragment = BooksTabFragment.newInstance(
            tabTypes[position],
            libraryId,
            clientId,
            password,
            librarianId
        )
        fragments[position] = fragment
        return fragment
    }

    override fun getPageTitle(position: Int): CharSequence = tabTitles[position]

    fun getFragment(position: Int): BooksTabFragment? = fragments.getOrNull(position)
}