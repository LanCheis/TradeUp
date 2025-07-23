package com.example.tradeup.listing

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.tradeup.R
import com.example.tradeup.offers.MakeOfferActivity
import com.google.firebase.auth.FirebaseAuth
import java.text.NumberFormat
import java.util.*

class ListingDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listing_detail)

        // Set up action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Item Details"

        // Get all the data from intent
        val image = intent.getStringExtra("imageUrl")
        val title = intent.getStringExtra("title")
        val category = intent.getStringExtra("category")
        val condition = intent.getStringExtra("condition")
        val description = intent.getStringExtra("description")
        val ownerName = intent.getStringExtra("ownerName")
        val ownerAvatar = intent.getStringExtra("ownerAvatar")
        val ownerUid = intent.getStringExtra("ownerUid")
        val location = intent.getStringExtra("location")
        val price = intent.getDoubleExtra("price", 0.0)
        val isNegotiable = intent.getBooleanExtra("isNegotiable", false)
        val listingId = intent.getStringExtra("listing_id") ?: ""

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
        val btnMakeOffer = findViewById<Button>(R.id.btnMakeOffer)

        // Set up buttons
        setupButtons(
            btnShare,
            btnMakeOffer,
            title,
            price,
            category,
            condition,
            ownerName,
            location,
            description,
            isNegotiable,
            listingId,
            ownerUid
        )

        // Load listing image
        loadListingImage(ivImage, image)

        // Load owner avatar
        loadOwnerAvatar(ivOwnerAvatar, ownerAvatar)

        // Set text data
        setTextData(
            tvTitle, tvPrice, tvCategory, tvCondition, tvDescription, tvOwnerName,
            title, price, category, condition, description, ownerName, isNegotiable
        )
    }

    private fun setupButtons(
        btnShare: Button, btnMakeOffer: Button, title: String?, price: Double,
        category: String?, condition: String?, ownerName: String?,
        location: String?, description: String?, isNegotiable: Boolean,
        listingId: String, ownerUid: String?
    ) {

        // Share button
        btnShare.setOnClickListener {
            shareContent(title, price, category, condition, ownerName, location, description)
        }

        // Make offer button - only show if negotiable and not owner
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (isNegotiable && currentUser != null && currentUser.uid != ownerUid) {
            btnMakeOffer.visibility = View.VISIBLE
            btnMakeOffer.setOnClickListener {
                val intent = Intent(this, MakeOfferActivity::class.java).apply {
                    putExtra("listing_id", listingId)
                    putExtra("listing_title", title)
                    putExtra("listing_image", intent.getStringExtra("imageUrl"))
                    putExtra("seller_uid", ownerUid)
                    putExtra("seller_name", ownerName)
                    putExtra("asking_price", price)
                }
                startActivity(intent)
            }
        } else {
            btnMakeOffer.visibility = View.GONE
        }
    }

    private fun shareContent(
        title: String?, price: Double, category: String?,
        condition: String?, ownerName: String?, location: String?,
        description: String?
    ) {
        val formattedPrice = formatPrice(price)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Share listing from TradeUp")
            putExtra(
                Intent.EXTRA_TEXT,
                "📢 ${title ?: "Product"}\n\n" +
                        "💰 Price: $formattedPrice\n" +
                        "📂 Category: ${category ?: "Unknown"}\n" +
                        "✨ Condition: ${condition ?: "Unknown"}\n" +
                        "👤 Seller: ${ownerName ?: "Anonymous"}\n" +
                        "📍 Location: ${location ?: "Unknown"}\n\n" +
                        "📄 Description: ${description ?: "No description"}\n\n" +
                        "Download TradeUp to see more!"
            )
        }
        startActivity(Intent.createChooser(shareIntent, "Share via..."))
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

    private fun setTextData(
        tvTitle: TextView, tvPrice: TextView, tvCategory: TextView,
        tvCondition: TextView, tvDescription: TextView, tvOwnerName: TextView,
        title: String?, price: Double, category: String?,
        condition: String?, description: String?, ownerName: String?,
        isNegotiable: Boolean
    ) {

        tvTitle.text = title ?: "No title"
        tvPrice.text = formatPrice(price) + if (isNegotiable) " (Negotiable)" else ""
        tvCategory.text = "📂 ${category ?: "Unknown"}"
        tvCondition.text = "✨ ${condition ?: "Unknown"}"
        tvDescription.text = description ?: "No description"
        tvOwnerName.text = "👤 ${ownerName ?: "Anonymous"}"
    }

    private fun formatPrice(price: Double): String {
        return if (price > 0) {
            val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
            "💰 ${formatter.format(price)} ₫"
        } else {
            "💰 Contact for price"
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}