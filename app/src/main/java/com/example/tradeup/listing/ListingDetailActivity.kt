package com.example.tradeup.listing

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.tradeup.R

class ListingDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listing_detail)

        val image = intent.getStringExtra("imageUrl")
        val title = intent.getStringExtra("title")
        val category = intent.getStringExtra("category")
        val description = intent.getStringExtra("description")

        val ivImage = findViewById<ImageView>(R.id.ivDetailImage)
        val tvTitle = findViewById<TextView>(R.id.tvDetailTitle)
        val tvCategory = findViewById<TextView>(R.id.tvDetailCategory)
        val tvDescription = findViewById<TextView>(R.id.tvDetailDescription)

        Glide.with(this).load(image).into(ivImage)
        tvTitle.text = title
        tvCategory.text = "Danh mục: $category"
        tvDescription.text = description
    }
}
