package com.example.tradeup.listing

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.tradeup.R

class EditListingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_listing) // Reuse add listing layout for now

        supportActionBar?.apply {
            title = "Edit Listing"
            setDisplayHomeAsUpEnabled(true)
        }

        // TODO: Implement edit functionality
        val listingId = intent.getStringExtra("listing_id")
        // Load existing listing data and populate fields
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}