package com.example.tradeup

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.tradeup.home.HomeFragment
import com.example.tradeup.profile.ProfileFragment
import com.example.tradeup.search.SearchFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val homeFragment = HomeFragment()
    private val profileFragment = ProfileFragment()
    private val searchFragment = SearchFragment()

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        loadFragment(homeFragment)
        setupBottomNavigation()
    }

    private fun initializeViews() {
        bottomNav = findViewById(R.id.bottomNavigationView)
    }

    private fun setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.fragment_home -> {
                    loadFragment(homeFragment)
                    true
                }
                R.id.menu_profile -> {
                    loadFragment(profileFragment)
                    true
                }
                R.id.menu_library -> {
                    loadFragment(searchFragment)
                    true
                }
                R.id.menu_chat -> {
                    // Show "Coming Soon" message instead of loading chat
                    showComingSoonMessage("Chat feature")
                    true
                }
                else -> false
            }
        }
    }

    private fun showComingSoonMessage(featureName: String) {
        Toast.makeText(
            this,
            "$featureName is coming soon! 🚀\nStay tuned for updates.",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun loadFragment(fragment: Fragment) {
        try {
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .commit()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun navigateTo(menuItemId: Int) {
        bottomNav.selectedItemId = menuItemId
    }
}