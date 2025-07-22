package com.example.tradeup

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.tradeup.chat.ChatFragment
import com.example.tradeup.home.HomeFragment
import com.example.tradeup.listing.ListingsFragment
import com.example.tradeup.profile.ProfileFragment
import com.example.tradeup.search.SearchFragment // Add this import
import com.example.tradeup.utils.CloudinaryHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.tradeup.ImageUrl

class MainActivity : AppCompatActivity() {

    private val homeFragment = HomeFragment()
    private val profileFragment = ProfileFragment()
    private val listingsFragment = ListingsFragment()
    private val chatFragment = ChatFragment()
    private val searchFragment = SearchFragment() // Add this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        testCloudinarySetup()
        loadFragment(homeFragment)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.fragment_home -> loadFragment(homeFragment)
                R.id.menu_profile -> loadFragment(profileFragment)
                R.id.menu_library -> loadFragment(searchFragment) // Use search instead of listings
                R.id.menu_chat -> loadFragment(chatFragment)
            }
            true
        }
    }

    private fun testCloudinarySetup() {
        if (CloudinaryHelper.isInitialized()) {
            Log.d("MainActivity", "✅ Cloudinary is working")
            Toast.makeText(this, "✅ Cloudinary Ready", Toast.LENGTH_SHORT).show()
        } else {
            Log.e("MainActivity", "❌ Cloudinary not working")
            Toast.makeText(this, "❌ Cloudinary Error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }

    fun navigateTo(menuItemId: Int) {
        findViewById<BottomNavigationView>(R.id.bottomNavigationView).selectedItemId = menuItemId
    }
}