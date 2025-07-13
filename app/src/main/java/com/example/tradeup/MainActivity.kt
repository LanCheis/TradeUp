package com.example.tradeup

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.tradeup.home.HomeFragment
import com.example.tradeup.profile.ProfileFragment
import com.example.tradeup.listing.ListingsFragment
import com.example.tradeup.chat.ChatFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val homeFragment = HomeFragment()
    private val profileFragment = ProfileFragment()
    private val listingsFragment = ListingsFragment()
    private val chatFragment = ChatFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadFragment(homeFragment)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.menu_home -> loadFragment(homeFragment)
                R.id.menu_profile -> loadFragment(profileFragment)
                R.id.menu_listings -> loadFragment(listingsFragment)
                R.id.menu_chat -> loadFragment(chatFragment)
            }
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    fun navigateTo(menuItemId: Int) {
        findViewById<BottomNavigationView>(R.id.bottomNavigationView).selectedItemId = menuItemId
    }
}
