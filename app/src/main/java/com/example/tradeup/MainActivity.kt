package com.example.tradeup

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.tradeup.auth.LoginActivity
import com.example.tradeup.chat.ChatFragment
import com.example.tradeup.data.remote.UserRepository
import com.example.tradeup.home.HomeFragment
import com.example.tradeup.listing.ListingsFragment
import com.example.tradeup.onboarding.SetupProfileActivity
import com.example.tradeup.profile.ProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private val homeFragment = HomeFragment()
    private val profileFragment = ProfileFragment()
    private val listingsFragment = ListingsFragment()
    private val chatFragment = ChatFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Check if user is logged in
        if (!isUserLoggedIn()) {
            redirectToLogin()
            return
        }

        // ✅ Check if user needs to complete profile setup
        checkProfileAndRedirectIfNeeded()

        setContentView(R.layout.activity_main)

        loadFragment(homeFragment)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.fragment_home -> loadFragment(homeFragment)
                R.id.menu_profile -> loadFragment(profileFragment)
                R.id.menu_library -> loadFragment(listingsFragment)
                R.id.menu_chat -> loadFragment(chatFragment)
            }
            true
        }
    }

    private fun isUserLoggedIn(): Boolean {
        return FirebaseAuth.getInstance().currentUser != null
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    // ✅ NEW METHOD - Check if profile setup is needed
    private fun checkProfileAndRedirectIfNeeded() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            UserRepository.getUserProfile(currentUser.uid) { user ->
                if (user == null || !isProfileComplete(user)) {
                    // Profile incomplete, redirect to setup
                    val intent = Intent(this, SetupProfileActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                }
                // If profile is complete, continue with MainActivity
            }
        }
    }

    // ✅ NEW METHOD - Check if essential profile fields are filled
    private fun isProfileComplete(user: com.example.tradeup.data.model.User): Boolean {
        return user.name.isNotEmpty() &&
                user.phone.isNotEmpty() &&
                user.bio.isNotEmpty() &&
                user.gender.isNotEmpty()
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