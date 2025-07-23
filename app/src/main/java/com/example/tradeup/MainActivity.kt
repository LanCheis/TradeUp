// app/src/main/java/com/example/tradeup/MainActivity.kt
package com.example.tradeup

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.tradeup.chat.ChatFragment
import com.example.tradeup.home.HomeFragment
import com.example.tradeup.listing.ListingDetailActivity
import com.example.tradeup.offers.OffersActivity
import com.example.tradeup.profile.ProfileFragment
import com.example.tradeup.search.SearchFragment
import com.example.tradeup.utils.CloudinaryHelper
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val homeFragment = HomeFragment()
    private val profileFragment = ProfileFragment()
    private val searchFragment = SearchFragment()
    private val chatFragment = ChatFragment()

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        testCloudinarySetup()
        loadFragment(homeFragment)
        setupBottomNavigation()
        handleNotificationIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Update the intent
        handleNotificationIntent(intent)
    }

    private fun initializeViews() {
        bottomNav = findViewById(R.id.bottomNavigationView)
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
                    loadFragment(chatFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return

        val navigateTo = intent.getStringExtra("navigate_to")
        val listingId = intent.getStringExtra("listing_id")
        val chatId = intent.getStringExtra("chat_id")

        when (navigateTo) {
            "chat" -> {
                // Navigate to chat
                loadFragment(chatFragment)
                bottomNav.selectedItemId = R.id.menu_chat

                if (chatId != null) {
                    // Handle specific chat navigation
                    Log.d("MainActivity", "Navigating to chat: $chatId")
                }
            }
            "offers" -> {
                // Navigate to offers
                val offersIntent = Intent(this, OffersActivity::class.java)
                if (listingId != null) {
                    offersIntent.putExtra("listing_id", listingId)
                }
                startActivity(offersIntent)
            }
            "listing_detail" -> {
                // Navigate to listing detail
                if (listingId != null) {
                    val detailIntent = Intent(this, ListingDetailActivity::class.java).apply {
                        putExtra("listing_id", listingId)
                        // Add other required extras from intent
                        putExtra("title", intent.getStringExtra("title") ?: "")
                        putExtra("price", intent.getDoubleExtra("price", 0.0))
                        putExtra("imageUrl", intent.getStringExtra("imageUrl") ?: "")
                        putExtra("category", intent.getStringExtra("category") ?: "")
                        putExtra("condition", intent.getStringExtra("condition") ?: "")
                        putExtra("description", intent.getStringExtra("description") ?: "")
                        putExtra("ownerName", intent.getStringExtra("ownerName") ?: "")
                        putExtra("ownerUid", intent.getStringExtra("ownerUid") ?: "")
                    }
                    startActivity(detailIntent)
                }
            }
            "profile" -> {
                // Navigate to profile
                loadFragment(profileFragment)
                bottomNav.selectedItemId = R.id.menu_profile
            }
            "search" -> {
                // Navigate to search
                loadFragment(searchFragment)
                bottomNav.selectedItemId = R.id.menu_library
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }

    fun navigateTo(menuItemId: Int) {
        bottomNav.selectedItemId = menuItemId
    }

    fun navigateToChat(chatId: String? = null, otherUserId: String? = null, otherUserName: String? = null) {
        loadFragment(chatFragment)
        bottomNav.selectedItemId = R.id.menu_chat

        // If specific chat parameters are provided, handle them
        if (chatId != null && otherUserId != null && otherUserName != null) {
            // Create bundle for fragment arguments
            val bundle = Bundle().apply {
                putString("chatId", chatId)
                putString("otherUserId", otherUserId)
                putString("otherUserName", otherUserName)
            }
            // Set arguments for chat fragment if needed
            Log.d("MainActivity", "Navigating to specific chat: $chatId")
        }
    }

    fun navigateToOffers(listingId: String? = null) {
        val intent = Intent(this, OffersActivity::class.java)
        if (listingId != null) {
            intent.putExtra("listing_id", listingId)
        }
        startActivity(intent)
    }

    fun navigateToListingDetail(
        listingId: String,
        title: String = "",
        price: Double = 0.0,
        imageUrl: String = "",
        category: String = "",
        condition: String = "",
        description: String = "",
        ownerName: String = "",
        ownerUid: String = ""
    ) {
        val intent = Intent(this, ListingDetailActivity::class.java).apply {
            putExtra("listing_id", listingId)
            putExtra("title", title)
            putExtra("price", price)
            putExtra("imageUrl", imageUrl)
            putExtra("category", category)
            putExtra("condition", condition)
            putExtra("description", description)
            putExtra("ownerName", ownerName)
            putExtra("ownerUid", ownerUid)
        }
        startActivity(intent)
    }
}