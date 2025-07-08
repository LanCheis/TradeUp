package com.example.tradeup.home

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.tradeup.R
import com.example.tradeup.auth.LoginActivity
import com.example.tradeup.data.remote.FirebaseAuthHelper
import com.example.tradeup.listing.CreateListingActivity
import com.example.tradeup.listing.ListingListActivity
import com.example.tradeup.profile.ProfileActivity

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val btnCreateListing = findViewById<Button>(R.id.btnCreateListing)
        val btnViewListings = findViewById<Button>(R.id.btnViewListings)
        val user = FirebaseAuthHelper.getCurrentUser()
        val welcomeText = findViewById<TextView>(R.id.tvWelcome)
        val logoutBtn = findViewById<Button>(R.id.btnLogout)
        val btnEditProfile = findViewById<Button>(R.id.btnEditProfile)

        welcomeText.text = "Welcome, ${user?.email ?: "Guest"}"

        logoutBtn.setOnClickListener {
            FirebaseAuthHelper.logout()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }


        btnCreateListing.setOnClickListener {
            startActivity(Intent(this, CreateListingActivity::class.java))
        }

        btnViewListings.setOnClickListener {
            startActivity(Intent(this, ListingListActivity::class.java)) // Nếu có
        }

        btnEditProfile.setOnClickListener {
            // Mình đã import ProfileActivity ngay bên dưới
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }
}
