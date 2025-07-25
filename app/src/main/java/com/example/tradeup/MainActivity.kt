package com.example.tradeup

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.tradeup.auth.LoginActivity
import com.example.tradeup.data.remote.UserRepository
import com.example.tradeup.listing.AddListingActivity
import com.example.tradeup.listing.MyListingsFragment
import com.example.tradeup.profile.ProfileFragment
import com.example.tradeup.ui.browse.BrowseFragment
import com.example.tradeup.utils.CloudinaryHelper
import com.example.tradeup.utils.ProfileValidator
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var userRepository: UserRepository
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var fabCreateListing: FloatingActionButton

    // Updated Fragments
    private val browseFragment = BrowseFragment() // FR-3.1.1: Browse all listings
    private val myListingsFragment = MyListingsFragment() // FR-2.2.1: User's own listings
    private val profileFragment = ProfileFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("MainActivity", "🚀 App starting...")

        // Initialize Firebase Auth first
        auth = FirebaseAuth.getInstance()

        // Test Firebase connection and auth status
        testFirebaseConnection()

        // Initialize Cloudinary
        CloudinaryHelper.initialize(this)

        userRepository = UserRepository()

        initViews()
        setupBottomNavigation()
        setupFloatingActionButton()
        checkAuthenticationAndProfile()
    }

    private fun initViews() {
        bottomNav = findViewById(R.id.bottomNav)
        fabCreateListing = findViewById(R.id.fabCreateListing)
        Log.d("MainActivity", "✅ Views initialized")
    }

    // Enhanced Firebase connection test
    private fun testFirebaseConnection() {
        val currentUser = auth.currentUser
        Log.d("MainActivity", "👤 Current user: ${currentUser?.email ?: "Not logged in"}")
        Log.d("MainActivity", "🔐 User ID: ${currentUser?.uid ?: "None"}")
        Log.d("MainActivity", "✅ Email verified: ${currentUser?.isEmailVerified ?: false}")

        lifecycleScope.launch {
            try {
                Log.d("MainActivity", "🧪 Testing Firebase Firestore connection...")

                val db = FirebaseFirestore.getInstance()

                // Test connection with a simple read
                val snapshot = db.collection("listings")
                    .limit(10) // Limit for testing
                    .get()
                    .await()

                Log.d("MainActivity", "🔥 Firebase connection successful!")
                Log.d("MainActivity", "📊 Found ${snapshot.documents.size} total documents in listings collection")

                if (snapshot.documents.isNotEmpty()) {
                    snapshot.documents.forEachIndexed { index, doc ->
                        val data = doc.data
                        Log.d("MainActivity", "📄 Document $index:")
                        Log.d("MainActivity", "   ID: ${doc.id}")
                        Log.d("MainActivity", "   Title: ${data?.get("title")}")
                        Log.d("MainActivity", "   Status: ${data?.get("status")}")
                        Log.d("MainActivity", "   Seller: ${data?.get("sellerName")}")
                    }
                } else {
                    Log.w("MainActivity", "⚠️ No documents found in listings collection")
                    Log.w("MainActivity", "💡 Try creating a listing first using the + button")
                }

            } catch (e: Exception) {
                Log.e("MainActivity", "💥 Firebase connection test failed: ${e.message}")
                Log.e("MainActivity", "🔍 Error details: ${e.javaClass.simpleName}")
                e.printStackTrace()

                // Show user-friendly error
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Firebase connection issue. Check your internet connection.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener { item ->
            Log.d("MainActivity", "🧭 Navigation item selected: ${item.title}")

            when (item.itemId) {
                R.id.nav_home -> {
                    Log.d("MainActivity", "🏠 Loading browse fragment (Home)")
                    loadFragment(browseFragment)
                    updateToolbarTitle("Browse Items")
                    true
                }
                R.id.nav_search -> {
                    Log.d("MainActivity", "🔍 Loading browse fragment (Search)")
                    loadFragment(browseFragment)
                    updateToolbarTitle("Search Items")
                    // Focus search bar when fragment loads
                    browseFragment.focusSearchBar()
                    true
                }
                R.id.nav_sell -> {
                    Log.d("MainActivity", "💼 Loading my listings fragment")
                    loadFragment(myListingsFragment)
                    updateToolbarTitle("My Listings")
                    true
                }
                R.id.nav_chat -> {
                    Log.d("MainActivity", "💬 Chat not implemented yet")
                    Toast.makeText(this, "Chat coming soon", Toast.LENGTH_SHORT).show()
                    false
                }
                R.id.nav_profile -> {
                    Log.d("MainActivity", "👤 Loading profile fragment")
                    loadFragment(profileFragment)
                    updateToolbarTitle("Profile")
                    true
                }
                else -> {
                    Log.w("MainActivity", "❓ Unknown navigation item: ${item.itemId}")
                    false
                }
            }
        }

        // Set default selection to browse
        bottomNav.selectedItemId = R.id.nav_home
        Log.d("MainActivity", "🏠 Default navigation set to Home")
    }

    private fun setupFloatingActionButton() {
        fabCreateListing.setOnClickListener {
            Log.d("MainActivity", "➕ FAB clicked - opening AddListingActivity")
            startActivity(Intent(this, AddListingActivity::class.java))
        }
    }

    private fun updateToolbarTitle(title: String) {
        supportActionBar?.title = title
        Log.d("MainActivity", "📝 Toolbar title updated to: $title")
    }

    private fun loadFragment(fragment: Fragment) {
        Log.d("MainActivity", "🔄 Loading fragment: ${fragment.javaClass.simpleName}")

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()

        Log.d("MainActivity", "✅ Fragment loaded successfully")
    }

    private fun checkAuthenticationAndProfile() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Log.w("MainActivity", "❌ User not authenticated - redirecting to login")
            redirectToLogin()
            return
        }

        Log.d("MainActivity", "✅ User authenticated: ${currentUser.email}")
        Log.d("MainActivity", "🔐 Checking user profile completeness...")

        lifecycleScope.launch {
            try {
                val userProfile = userRepository.getUserProfile(currentUser.uid)
                Log.d("MainActivity", "📋 User profile loaded: ${userProfile?.displayName ?: "No name"}")

                if (ProfileValidator.isProfileComplete(userProfile)) {
                    Log.d("MainActivity", "✅ Profile is complete - loading browse fragment")
                    loadFragment(browseFragment)
                    updateToolbarTitle("Browse Items")
                } else {
                    val missingFields = ProfileValidator.getMissingFields(userProfile)
                    Log.w("MainActivity", "⚠️ Profile incomplete - missing: ${missingFields.joinToString(", ")}")

                    Toast.makeText(
                        this@MainActivity,
                        "Please complete your profile: ${missingFields.joinToString(", ")}",
                        Toast.LENGTH_LONG
                    ).show()

                    loadFragment(profileFragment)
                    bottomNav.selectedItemId = R.id.nav_profile
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "💥 Error loading profile: ${e.message}")
                e.printStackTrace()

                Toast.makeText(this@MainActivity, "Error loading profile", Toast.LENGTH_SHORT).show()
                // Load browse fragment as fallback
                loadFragment(browseFragment)
            }
        }
    }

    private fun redirectToLogin() {
        Log.d("MainActivity", "🔄 Redirecting to LoginActivity")
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    // Options menu for additional actions
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_my_listings -> {
                Log.d("MainActivity", "📋 Menu: My Listings selected")
                loadFragment(myListingsFragment)
                bottomNav.selectedItemId = R.id.nav_sell
                updateToolbarTitle("My Listings")
                true
            }
            R.id.action_add_listing -> {
                Log.d("MainActivity", "➕ Menu: Add Listing selected")
                startActivity(Intent(this, AddListingActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "🔄 MainActivity resumed")

        // Refresh current fragment when returning from other activities
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (currentFragment is BrowseFragment) {
            Log.d("MainActivity", "🔄 Refreshing browse fragment data")
            // Fragment will automatically refresh when resumed
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "💀 MainActivity destroyed")
    }
}