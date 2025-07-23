package com.example.tradeup.listing

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.tradeup.R
import com.example.tradeup.chat.MessageActivity
import com.example.tradeup.data.model.Offer
import com.example.tradeup.data.remote.UserRepository
import com.example.tradeup.offers.OfferRepository
import com.example.tradeup.profile.ViewProfileActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import java.text.NumberFormat
import java.util.*

class ListingDetailActivity : AppCompatActivity() {

    private lateinit var ivDetailImage: ImageView
    private lateinit var tvDetailTitle: TextView
    private lateinit var tvDetailPrice: TextView
    private lateinit var tvDetailCategory: TextView
    private lateinit var tvDetailCondition: TextView
    private lateinit var tvDetailDescription: TextView
    private lateinit var tvOwnerName: TextView
    private lateinit var ivOwnerAvatar: ImageView
    private lateinit var btnShare: Button
    private lateinit var btnChat: Button
    private lateinit var btnMakeOffer: Button
    private lateinit var btnViewProfile: Button
    private lateinit var layoutOwnerActions: LinearLayout
    private lateinit var progressBar: ProgressBar

    private var listingId: String = ""
    private var ownerId: String = ""
    private var isOwner: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listing_detail)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Item Details"

        initViews()
        setupClickListeners()
        loadListingData()
    }

    private fun initViews() {
        ivDetailImage = findViewById(R.id.ivDetailImage)
        tvDetailTitle = findViewById(R.id.tvDetailTitle)
        tvDetailPrice = findViewById(R.id.tvDetailPrice)
        tvDetailCategory = findViewById(R.id.tvDetailCategory)
        tvDetailCondition = findViewById(R.id.tvDetailCondition)
        tvDetailDescription = findViewById(R.id.tvDetailDescription)
        tvOwnerName = findViewById(R.id.tvOwnerName)
        ivOwnerAvatar = findViewById(R.id.ivOwnerAvatar)
        btnShare = findViewById(R.id.btnShare)
        btnChat = findViewById(R.id.btnChat)
        btnMakeOffer = findViewById(R.id.btnMakeOffer)
        btnViewProfile = findViewById(R.id.btnViewProfile)
        layoutOwnerActions = findViewById(R.id.layoutOwnerActions)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupClickListeners() {
        btnShare.setOnClickListener {
            shareContent()
        }

        btnChat.setOnClickListener {
            startChat()
        }

        btnMakeOffer.setOnClickListener {
            showMakeOfferDialog()
        }

        btnViewProfile.setOnClickListener {
            val intent = Intent(this, ViewProfileActivity::class.java).apply {
                putExtra("USER_ID", ownerId)
            }
            startActivity(intent)
        }

        ivOwnerAvatar.setOnClickListener {
            btnViewProfile.performClick()
        }
    }

    private fun loadListingData() {
        // Get data from intent
        listingId = intent.getStringExtra("listing_id") ?: ""
        val title = intent.getStringExtra("title") ?: ""
        val price = intent.getDoubleExtra("price", 0.0)
        val imageUrl = intent.getStringExtra("imageUrl") ?: ""
        val category = intent.getStringExtra("category") ?: ""
        val condition = intent.getStringExtra("condition") ?: ""
        val description = intent.getStringExtra("description") ?: ""
        val location = intent.getStringExtra("location") ?: ""
        val ownerName = intent.getStringExtra("ownerName") ?: ""
        val ownerAvatar = intent.getStringExtra("ownerAvatar") ?: ""
        ownerId = intent.getStringExtra("ownerUid") ?: ""
        val isNegotiable = intent.getBooleanExtra("isNegotiable", true)

        // Check if current user owns this listing
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        isOwner = currentUserId == ownerId

        // Set up UI based on ownership
        setupUIForOwnership()

        // Load listing image
        loadListingImage(imageUrl)

        // Load owner avatar
        loadOwnerAvatar(ownerAvatar)

        // Set text data
        setTextData(title, price, category, condition, description, ownerName, location, isNegotiable)
    }

    private fun setupUIForOwnership() {
        if (isOwner) {
            // Hide interaction buttons for owner
            btnChat.visibility = View.GONE
            btnMakeOffer.visibility = View.GONE
            btnViewProfile.visibility = View.GONE

            // Could add owner-specific actions here
            layoutOwnerActions.visibility = View.VISIBLE
        } else {
            // Show interaction buttons for non-owners
            btnChat.visibility = View.VISIBLE
            btnMakeOffer.visibility = View.VISIBLE
            btnViewProfile.visibility = View.VISIBLE
            layoutOwnerActions.visibility = View.GONE
        }
    }

    private fun loadListingImage(imageUrl: String?) {
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(ivDetailImage)
        } else {
            ivDetailImage.setImageResource(R.drawable.ic_image_placeholder)
        }
    }

    private fun loadOwnerAvatar(avatarUrl: String?) {
        if (!avatarUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .circleCrop()
                .into(ivOwnerAvatar)
        } else {
            ivOwnerAvatar.setImageResource(R.drawable.ic_avatar_placeholder)
        }
    }

    private fun setTextData(
        title: String, price: Double, category: String,
        condition: String, description: String, ownerName: String,
        location: String, isNegotiable: Boolean
    ) {
        tvDetailTitle.text = title
        tvDetailPrice.text = formatPrice(price, isNegotiable)
        tvDetailCategory.text = "📂 $category"
        tvDetailCondition.text = "✨ $condition"
        tvDetailDescription.text = description.ifEmpty { "No description provided" }
        tvOwnerName.text = "👤 $ownerName"

        if (location.isNotEmpty()) {
            tvOwnerName.text = "${tvOwnerName.text}\n📍 $location"
        }
    }

    private fun shareContent() {
        val title = tvDetailTitle.text.toString()
        val price = tvDetailPrice.text.toString()
        val category = tvDetailCategory.text.toString()
        val condition = tvDetailCondition.text.toString()
        val ownerName = tvOwnerName.text.toString()
        val description = tvDetailDescription.text.toString()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Check out this item on TradeUp")
            putExtra(
                Intent.EXTRA_TEXT,
                "📢 $title\n\n" +
                        "$price\n" +
                        "$category\n" +
                        "$condition\n" +
                        "$ownerName\n\n" +
                        "📄 Description: $description\n\n" +
                        "Download TradeUp to see more!"
            )
        }
        startActivity(Intent.createChooser(shareIntent, "Share via..."))
    }

    private fun startChat() {
        if (isOwner) return

        // Create or find existing chat
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        // For now, create a simple chat ID
        val chatId = "${currentUser.uid}_${ownerId}_$listingId"

        val intent = Intent(this, MessageActivity::class.java).apply {
            putExtra("chatId", chatId)
            putExtra("otherUserId", ownerId)
            putExtra("otherUserName", tvOwnerName.text.toString().removePrefix("👤 "))
            putExtra("listingTitle", tvDetailTitle.text.toString())
        }
        startActivity(intent)
    }

    private fun showMakeOfferDialog() {
        if (isOwner) return

        val dialogView = layoutInflater.inflate(R.layout.dialog_make_offer, null)
        val etOfferAmount = dialogView.findViewById<TextInputEditText>(R.id.etOfferAmount)
        val etOfferMessage = dialogView.findViewById<TextInputEditText>(R.id.etOfferMessage)
        val tvOriginalPrice = dialogView.findViewById<TextView>(R.id.tvOriginalPrice)

        // Set original price
        tvOriginalPrice.text = "Original price: ${tvDetailPrice.text}"

        val dialog = AlertDialog.Builder(this)
            .setTitle("💰 Make an Offer")
            .setView(dialogView)
            .setPositiveButton("Send Offer") { _, _ ->
                val offerAmountText = etOfferAmount.text.toString().trim()
                val offerMessage = etOfferMessage.text.toString().trim()

                if (offerAmountText.isNotEmpty()) {
                    try {
                        val offerAmount = offerAmountText.toDouble()
                        createOffer(offerAmount, offerMessage)
                    } catch (e: NumberFormatException) {
                        Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Please enter an offer amount", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun createOffer(offerAmount: Double, message: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        showLoading(true)

        // Get current user profile for offer details
        UserRepository.getUserProfile(currentUser.uid) { userProfile ->
            val originalPrice = intent.getDoubleExtra("price", 0.0)

            val offer = Offer(
                listingId = listingId,
                listingTitle = tvDetailTitle.text.toString(),
                listingImageUrl = intent.getStringExtra("imageUrl") ?: "",
                buyerId = currentUser.uid,
                buyerName = userProfile?.name ?: currentUser.displayName ?: "Anonymous",
                buyerAvatar = userProfile?.profileImageUrl ?: "",
                sellerId = ownerId,
                sellerName = tvOwnerName.text.toString().removePrefix("👤 ").split("\n")[0],
                originalPrice = originalPrice,
                offerPrice = offerAmount,
                message = message
            )

            OfferRepository.createOffer(offer) { success, error ->
                showLoading(false)

                if (success) {
                    Toast.makeText(this, "✅ Offer sent successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "❌ Failed to send offer: $error", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnMakeOffer.isEnabled = !show
        btnChat.isEnabled = !show
        btnShare.isEnabled = !show
    }

    private fun formatPrice(price: Double, isNegotiable: Boolean): String {
        return if (price > 0) {
            val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
            val priceText = "💰 ${formatter.format(price)} ₫"
            if (isNegotiable) "$priceText (Negotiable)" else priceText
        } else {
            "💰 Contact for price"
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
