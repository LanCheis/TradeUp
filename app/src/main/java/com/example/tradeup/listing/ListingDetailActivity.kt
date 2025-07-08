package com.example.tradeup.listing

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.tradeup.R

class ListingDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listing_detail)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Chi tiết bài đăng"

        val image = intent.getStringExtra("imageUrl")
        val title = intent.getStringExtra("title")
        val category = intent.getStringExtra("category")
        val description = intent.getStringExtra("description")
        val ivImage = findViewById<ImageView>(R.id.ivDetailImage)
        val tvTitle = findViewById<TextView>(R.id.tvDetailTitle)
        val tvCategory = findViewById<TextView>(R.id.tvDetailCategory)
        val tvDescription = findViewById<TextView>(R.id.tvDetailDescription)
        val btnShare = findViewById<Button>(R.id.btnShare)
        btnShare.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Chia sẻ bài đăng")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "📢 ${title ?: ""}\n\n📂 Danh mục: $category\n\n📄 $description"
                )
            }
            startActivity(Intent.createChooser(shareIntent, "Chia sẻ qua..."))
        }

        Glide.with(this).load(image).into(ivImage)
        tvTitle.text = title
        tvCategory.text = "Danh mục: $category"
        tvDescription.text = description
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
