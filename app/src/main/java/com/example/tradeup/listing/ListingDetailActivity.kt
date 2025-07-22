package com.example.tradeup.listing

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.tradeup.R
import com.example.tradeup.data.model.Chat
import com.example.tradeup.chat.MessageActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class ListingDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listing_detail)

        setupToolbar()
        initViews()
        setupClickListeners()
        loadListingData()
    }

    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Item Details"
    }

    private fun initViews() {
        // Get all data from intent
        val price = intent.getDoubleExtra("price", 0.0)
        val isNegotiable = intent.getBooleanExtra("isNegotiable", false)
        val condition = intent.getStringExtra("condition") ?: "Good"
        val location = intent.getStringExtra("location") ?: "Unknown"
        val ownerName = intent.getStringExtra("ownerName") ?: "Seller"
        val views = intent.getIntExtra("views", 0)
        val interactions = intent.getIntExtra("interactions", 0)
        val createdAt = intent.getLongExtra("createdAt", System.currentTimeMillis())

        // Basic info
        findViewById<TextView>(R.id.tvDetailTitle).text = intent.getStringExtra("title")
        findViewById<TextView>(R.id.tvDetailCategory).text = intent.getStringExtra("category")
        findViewById<TextView>(R.id.tvDetailDescription).text = intent.getStringExtra("description")
        findViewById<TextView>(R.id.tvDetailCondition).text = condition
        findViewById<TextView>(R.id.tvDetailLocation).text = location

        // Price formatting
        if (price > 0) {
            val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
            findViewById<TextView>(R.id.tvDetailPrice).text = "${formatter.format(price)} ₫"
        } else {
            findViewById<TextView>(R.id.tvDetailPrice).text = "Contact for price"
        }

        // Negotiable badge
        if (isNegotiable) {
            findViewById<TextView>(R.id.tvNegotiable).visibility = TextView.VISIBLE
        }

        // Condition badge
        findViewById<TextView>(R.id.tvConditionBadge).text = condition

        // Seller info
        findViewById<TextView>(R.id.tvSellerName).text = ownerName

        // Stats
        findViewById<TextView>(R.id.tvViews).text = views.toString()
        findViewById<TextView>(R.id.tvInteractions).text = interactions.toString()
        findViewById<TextView>(R.id.tvTimePosted).text = formatTimeAgo(createdAt)

        // Load image
        val imageUrl = intent.getStringExtra("imageUrl")
        Glide.with(this)
            .load(imageUrl)
            .error(R.drawable.ic_image_placeholder)
            .into(findViewById<ImageView>(R.id.ivDetailImage))
    }

    private fun setupClickListeners() {
        // Contact Seller
        findViewById<Button>(R.id.btnContactSeller).setOnClickListener {
            startChatWithSeller()
        }

        // Make Offer
        findViewById<Button>(R.id.btnMakeOffer).setOnClickListener {
            Toast.makeText(this, "💰 Make Offer feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Share
        findViewById<Button>(R.id.btnShare).setOnClickListener {
            shareItem()
        }

        // Report
        findViewById<Button>(R.id.btnReport).setOnClickListener {
            Toast.makeText(this, "🚩 Report sent to moderators", Toast.LENGTH_SHORT).show()
        }

        // View Profile
        findViewById<Button>(R.id.btnViewProfile).setOnClickListener {
            Toast.makeText(this, "👤 View Profile feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Favorite
        findViewById<ImageView>(R.id.btnFavorite).setOnClickListener {
            Toast.makeText(this, "❤️ Added to favorites!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadListingData() {
        // You can load additional data from Firestore here if needed
        // For now, we're using data passed through intent
    }

    private fun startChatWithSeller() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login to contact seller", Toast.LENGTH_SHORT).show()
            return
        }

        val sellerId = intent.getStringExtra("sellerId") ?: return
        val sellerName = intent.getStringExtra("sellerName") ?: "Seller"
        val listingId = intent.getStringExtra("listing_id") ?: ""
        val listingTitle = intent.getStringExtra("title") ?: ""

        // Check if user is trying to contact themselves
        if (currentUser.uid == sellerId) {
            Toast.makeText(this, "You cannot message yourself!", Toast.LENGTH_SHORT).show()
            return
        }

        val chatId = generateChatId(currentUser.uid, sellerId)

        val chat = Chat(
            id = chatId,
            participants = listOf(currentUser.uid, sellerId),
            participantNames = mapOf(
                currentUser.uid to (currentUser.displayName ?: "You"),
                sellerId to sellerName
            ),
            listingId = listingId,
            listingTitle = listingTitle,
            lastMessage = "",
            lastMessageTime = System.currentTimeMillis()
        )

        FirebaseFirestore.getInstance()
            .collection("chats")
            .document(chatId)
            .set(chat, SetOptions.merge())
            .addOnSuccessListener {
                val intent = Intent(this, MessageActivity::class.java).apply {
                    putExtra("chatId", chatId)
                    putExtra("otherUserId", sellerId)
                    putExtra("otherUserName", sellerName)
                    putExtra("listingTitle", listingTitle)
                }
                startActivity(intent)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to start chat", Toast.LENGTH_SHORT).show()
            }
    }

    private fun shareItem() {
        val title = intent.getStringExtra("title") ?: ""
        val category = intent.getStringExtra("category") ?: ""
        val description = intent.getStringExtra("description") ?: ""
        val price = intent.getDoubleExtra("price", 0.0)

        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        val priceText = if (price > 0) "${formatter.format(price)} ₫" else "Contact for price"

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Check out this item on TradeUp")
            putExtra(
                Intent.EXTRA_TEXT,
                "🛍️ $title\n\n💰 $priceText\n📂 Category: $category\n\n📄 $description\n\n📱 Download TradeUp to see more!"
            )
        }
        startActivity(Intent.createChooser(shareIntent, "Share via..."))
    }

    private fun generateChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) "${userId1}_${userId2}" else "${userId2}_${userId1}"
    }

    private fun formatTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            diff < 604800_000 -> "${diff / 86400_000}d ago"
            else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}