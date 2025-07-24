package com.example.tradeup

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.tradeup.listing.CreateListingActivity

class MainActivity : AppCompatActivity() {

    // Tabs
    private lateinit var tabHome: LinearLayout
    private lateinit var tabSearch: LinearLayout
    private lateinit var tabAdd: LinearLayout
    private lateinit var tabChat: LinearLayout
    private lateinit var tabProfile: LinearLayout

    // Icons
    private lateinit var iconHome: ImageView
    private lateinit var iconSearch: ImageView
    private lateinit var iconAdd: ImageView
    private lateinit var iconChat: ImageView
    private lateinit var iconProfile: ImageView

    // Labels
    private lateinit var labelHome: TextView
    private lateinit var labelSearch: TextView
    private lateinit var labelAdd: TextView
    private lateinit var labelChat: TextView
    private lateinit var labelProfile: TextView

    // Current selected tab
    private var currentTab = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupClickListeners()

        // Load home fragment by default
        selectTab(0)
        loadHomeFragment()
    }

    private fun initViews() {
        // Tab containers
        tabHome = findViewById(R.id.tabHome)
        tabSearch = findViewById(R.id.tabSearch)
        tabAdd = findViewById(R.id.tabAdd)
        tabChat = findViewById(R.id.tabChat)
        tabProfile = findViewById(R.id.tabProfile)

        // Icons
        iconHome = findViewById(R.id.iconHome)
        iconSearch = findViewById(R.id.iconSearch)
        iconAdd = findViewById(R.id.iconAdd)
        iconChat = findViewById(R.id.iconChat)
        iconProfile = findViewById(R.id.iconProfile)

        // Labels
        labelHome = findViewById(R.id.labelHome)
        labelSearch = findViewById(R.id.labelSearch)
        labelAdd = findViewById(R.id.labelAdd)
        labelChat = findViewById(R.id.labelChat)
        labelProfile = findViewById(R.id.labelProfile)
    }

    private fun setupClickListeners() {
        tabHome.setOnClickListener {
            selectTab(0)
            loadHomeFragment()
        }

        tabSearch.setOnClickListener {
            selectTab(1)
            loadSearchFragment()
        }

        tabAdd.setOnClickListener {
            // Navigate to CreateListingActivity
            startActivity(Intent(this, CreateListingActivity::class.java))
        }

        tabChat.setOnClickListener {
            selectTab(3)
            loadChatFragment()
        }

        tabProfile.setOnClickListener {
            selectTab(4)
            loadProfileFragment()
        }
    }

    private fun selectTab(tabIndex: Int) {
        // Reset all tabs
        resetAllTabs()

        currentTab = tabIndex

        // Highlight selected tab
        when (tabIndex) {
            0 -> highlightTab(iconHome, labelHome)
            1 -> highlightTab(iconSearch, labelSearch)
            3 -> highlightTab(iconChat, labelChat)
            4 -> highlightTab(iconProfile, labelProfile)
        }
    }

    private fun resetAllTabs() {
        // Reset Home
        iconHome.setColorFilter(ContextCompat.getColor(this, R.color.text_secondary))
        labelHome.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))

        // Reset Search
        iconSearch.setColorFilter(ContextCompat.getColor(this, R.color.text_secondary))
        labelSearch.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))

        // Reset Chat
        iconChat.setColorFilter(ContextCompat.getColor(this, R.color.text_secondary))
        labelChat.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))

        // Reset Profile
        iconProfile.setColorFilter(ContextCompat.getColor(this, R.color.text_secondary))
        labelProfile.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
    }

    private fun highlightTab(icon: ImageView, label: TextView) {
        icon.setColorFilter(ContextCompat.getColor(this, R.color.primary))
        label.setTextColor(ContextCompat.getColor(this, R.color.primary))
    }

    private fun loadHomeFragment() {
        loadSimpleFragment("🏠 Home", "Welcome to TradeUp!\n\nStart browsing items for sale in your area.")
    }

    private fun loadSearchFragment() {
        loadSimpleFragment("🔍 Search", "Search for items you want to buy.\n\nFeature coming soon!")
    }

    private fun loadChatFragment() {
        loadSimpleFragment("💬 Chat", "Your conversations with buyers and sellers.\n\nFeature coming soon!")
    }

    private fun loadProfileFragment() {
        loadSimpleFragment("👤 Profile", "Manage your profile and listings.\n\nFeature coming soon!")
    }

    private fun loadSimpleFragment(title: String, description: String) {
        val fragment = SimpleFragment.newInstance(title, description)

        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }
}