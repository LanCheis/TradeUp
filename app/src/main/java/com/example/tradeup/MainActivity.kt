package com.example.tradeup

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.tradeup.auth.LoginActivity
import com.example.tradeup.data.remote.UserRepository
import com.example.tradeup.listing.AddListingActivity
import com.example.tradeup.listing.ListingsFragment
import com.example.tradeup.listing.UserListingsActivity
import com.example.tradeup.profile.ProfileFragment
import com.example.tradeup.utils.CloudinaryHelper
import com.example.tradeup.utils.ProfileValidator
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var userRepository: UserRepository
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var fabCreateListing: FloatingActionButton

    // Fragments
    private val profileFragment = ProfileFragment()
    private val listingsFragment = ListingsFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Cloudinary
        CloudinaryHelper.initialize(this)

        auth = FirebaseAuth.getInstance()
        userRepository = UserRepository()

        initViews()
        setupBottomNavigation()
        setupFloatingActionButton()
        checkAuthenticationAndProfile()
    }

    private fun initViews() {
        bottomNav = findViewById(R.id.bottomNav)
        fabCreateListing = findViewById(R.id.fabCreateListing)
    }

    private fun setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(listingsFragment)
                    true
                }
                R.id.nav_search -> {
                    Toast.makeText(this, "Search coming soon", Toast.LENGTH_SHORT).show()
                    false
                }
                R.id.nav_sell -> {
                    // Navigate to My Listings
                    startActivity(Intent(this, UserListingsActivity::class.java))
                    false // Don't change bottom nav selection
                }
                R.id.nav_chat -> {
                    Toast.makeText(this, "Chat coming soon", Toast.LENGTH_SHORT).show()
                    false
                }
                R.id.nav_profile -> {
                    loadFragment(profileFragment)
                    true
                }
                else -> false
            }
        }

        // Set default selection
        bottomNav.selectedItemId = R.id.nav_home
    }

    private fun setupFloatingActionButton() {
        fabCreateListing.setOnClickListener {
            startActivity(Intent(this, AddListingActivity::class.java))
        }
    }

    // Options menu for additional actions
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_my_listings -> {
                startActivity(Intent(this, UserListingsActivity::class.java))
                true
            }
            R.id.action_add_listing -> {
                startActivity(Intent(this, AddListingActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun checkAuthenticationAndProfile() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            redirectToLogin()
            return
        }

        lifecycleScope.launch {
            try {
                val userProfile = userRepository.getUserProfile(currentUser.uid)

                if (ProfileValidator.isProfileComplete(userProfile)) {
                    loadFragment(listingsFragment)
                } else {
                    val missingFields = ProfileValidator.getMissingFields(userProfile)
                    val message = "Complete your profile to continue:\n• ${missingFields.joinToString("\n• ")}"

                    Toast.makeText(this@MainActivity, "Profile setup required", Toast.LENGTH_LONG).show()
                    redirectToProfileSetup(message)
                }

            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
                redirectToLogin()
            }
        }
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    private fun redirectToProfileSetup(message: String = "") {
        try {
            val setupClass = Class.forName("com.example.tradeup.onboarding.SetupProfileActivity")
            val intent = Intent(this, setupClass)
            if (message.isNotEmpty()) {
                intent.putExtra("incomplete_message", message)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        } catch (e: ClassNotFoundException) {
            Toast.makeText(this, "Please complete your profile", Toast.LENGTH_LONG).show()
            loadFragment(listingsFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        if (bottomNav.selectedItemId == R.id.nav_home) {
            loadFragment(listingsFragment)
        }
    }
}