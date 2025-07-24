package com.example.tradeup.listing

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tradeup.R

class CreateListingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_placeholder)

        // Show coming soon message
        Toast.makeText(
            this,
            "Create listing functionality coming soon! 📝",
            Toast.LENGTH_LONG
        ).show()

        // Go back after showing message
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Create Listing"
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}