package com.example.myapplication

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class UserLibraryPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> UserRegistrationFragment()
            1 -> LibraryRegistrationFragment()
            else -> UserRegistrationFragment()
        }
    }

    override fun getItemCount(): Int {
        return 2
    }
}
