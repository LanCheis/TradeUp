package com.example.tradeup.listing

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.tradeup.R

class ListingDetailActivity : AppCompatActivity() {

    private lateinit var ivDetailImage: ImageView
    private lateinit var tvDetailTitle: TextView
    private lateinit var tvDetailCategory: TextView
    private lateinit var tvDetailDescription: TextView
    private lateinit var btnShare: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listing_detail)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Chi tiết bài đăng"

        initViews()
        loadDataFromIntent()
    }

    private fun initViews() {
        ivDetailImage = findViewById(R.id.ivDetailImage)
        tvDetailTitle = findViewById(R.id.tvDetailTitle)
        tvDetailCategory = findViewById(R.id.tvDetailCategory)
        tvDetailDescription = findViewById(R.id.tvDetailDescription)
        btnShare = findViewById(R.id.btnShare)
    }

    private fun loadDataFromIntent() {
        val imageUrl = intent.getStringExtra("imageUrl") ?: "" // ✅ Consistent field name
        val title = intent.getStringExtra("title") ?: "No Title"
        val category = intent.getStringExtra("category") ?: "No Category"
        val description = intent.getStringExtra("description") ?: "No Description"
        val price = intent.getDoubleExtra("price", 0.0)
        val location = intent.getStringExtra("location") ?: ""

        // Set text data
        tvDetailTitle.text = title
        tvDetailCategory.text = "Danh mục: $category"
        tvDetailDescription.text = description

        // ✅ Comprehensive image loading
        loadImageIntoView(ivDetailImage, imageUrl)

        // Setup share button
        btnShare.setOnClickListener {
            shareListingDetails(title, category, description, price)
        }
    }

    private fun loadImageIntoView(imageView: ImageView, imageUrl: String?) {
        if (imageUrl.isNullOrBlank()) {
            imageView.setImageResource(R.drawable.ic_image_placeholder)
            return
        }

        val requestOptions = RequestOptions()
            .placeholder(R.drawable.ic_image_placeholder)
            .error(R.drawable.ic_image_placeholder)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()

        try {
            Glide.with(this)
                .load(imageUrl)
                .apply(requestOptions)
                .into(imageView)
        } catch (e: Exception) {
            imageView.setImageResource(R.drawable.ic_image_placeholder)
        }
    }

    private fun shareListingDetails(title: String, category: String, description: String, price: Double) {
        val priceText = if (price > 0) {
            val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale("vi", "VN"))
            "${formatter.format(price)} ₫"
        } else {
            "Contact for price"
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Chia sẻ bài đăng")
            putExtra(
                Intent.EXTRA_TEXT,
                "📢 $title\n\n📂 Danh mục: $category\n💰 Giá: $priceText\n\n📄 $description"
            )
        }
        startActivity(Intent.createChooser(shareIntent, "Chia sẻ qua..."))
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}