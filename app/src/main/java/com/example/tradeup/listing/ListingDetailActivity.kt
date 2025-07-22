package com.example.tradeup.listing

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.tradeup.R
import java.text.NumberFormat
import java.util.*

class ListingDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listing_detail)

        // Set up action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Chi tiết bài đăng"

        // Get all the data from intent
        val image = intent.getStringExtra("imageUrl")
        val title = intent.getStringExtra("title")
        val category = intent.getStringExtra("category")
        val condition = intent.getStringExtra("condition")
        val description = intent.getStringExtra("description")
        val ownerName = intent.getStringExtra("ownerName")
        val ownerAvatar = intent.getStringExtra("ownerAvatar")
        val location = intent.getStringExtra("location")
        val price = intent.getDoubleExtra("price", 0.0)

        // Find views
        val ivImage = findViewById<ImageView>(R.id.ivDetailImage)
        val tvTitle = findViewById<TextView>(R.id.tvDetailTitle)
        val tvPrice = findViewById<TextView>(R.id.tvDetailPrice)
        val tvCategory = findViewById<TextView>(R.id.tvDetailCategory)
        val tvCondition = findViewById<TextView>(R.id.tvDetailCondition)
        val tvDescription = findViewById<TextView>(R.id.tvDetailDescription)
        val tvOwnerName = findViewById<TextView>(R.id.tvOwnerName)
        val ivOwnerAvatar = findViewById<ImageView>(R.id.ivOwnerAvatar)
        val btnShare = findViewById<Button>(R.id.btnShare)

        // Set up share button
        btnShare.setOnClickListener {
            shareContent(title, price, category, condition, ownerName, location, description)
        }

        // Load listing image
        loadListingImage(ivImage, image)

        // Load owner avatar
        loadOwnerAvatar(ivOwnerAvatar, ownerAvatar)

        // Set text data
        setTextData(tvTitle, tvPrice, tvCategory, tvCondition, tvDescription, tvOwnerName,
            title, price, category, condition, description, ownerName)
    }

    private fun shareContent(title: String?, price: Double, category: String?,
                             condition: String?, ownerName: String?, location: String?,
                             description: String?) {
        val formattedPrice = formatPrice(price)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Chia sẻ bài đăng từ TradeUp")
            putExtra(
                Intent.EXTRA_TEXT,
                "📢 ${title ?: "Sản phẩm"}\n\n" +
                        "💰 Giá: $formattedPrice\n" +
                        "📂 Danh mục: ${category ?: "Không xác định"}\n" +
                        "✨ Tình trạng: ${condition ?: "Không xác định"}\n" +
                        "👤 Người bán: ${ownerName ?: "Anonymous"}\n" +
                        "📍 Vị trí: ${location ?: "Không xác định"}\n\n" +
                        "📄 Mô tả: ${description ?: "Không có mô tả"}\n\n" +
                        "Tải TradeUp để xem thêm!"
            )
        }
        startActivity(Intent.createChooser(shareIntent, "Chia sẻ qua..."))
    }

    private fun loadListingImage(imageView: ImageView, imageUrl: String?) {
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageView)
        } else {
            imageView.setImageResource(R.drawable.ic_image_placeholder)
        }
    }

    private fun loadOwnerAvatar(imageView: ImageView, avatarUrl: String?) {
        if (!avatarUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .circleCrop()
                .into(imageView)
        } else {
            imageView.setImageResource(R.drawable.ic_avatar_placeholder)
        }
    }

    private fun setTextData(tvTitle: TextView, tvPrice: TextView, tvCategory: TextView,
                            tvCondition: TextView, tvDescription: TextView, tvOwnerName: TextView,
                            title: String?, price: Double, category: String?,
                            condition: String?, description: String?, ownerName: String?) {

        tvTitle.text = title ?: "Không có tiêu đề"
        tvPrice.text = formatPrice(price)
        tvCategory.text = "📂 ${category ?: "Không xác định"}"
        tvCondition.text = "✨ ${condition ?: "Không xác định"}"
        tvDescription.text = description ?: "Không có mô tả"
        tvOwnerName.text = "👤 ${ownerName ?: "Anonymous"}"
    }

    private fun formatPrice(price: Double): String {
        return if (price > 0) {
            val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
            "💰 ${formatter.format(price)} ₫"
        } else {
            "💰 Liên hệ để biết giá"
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}