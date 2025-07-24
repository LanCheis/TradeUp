package com.example.tradeup.listing

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.tradeup.R
import com.example.tradeup.chat.MessageActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import de.hdodenhof.circleimageview.CircleImageView

class ListingDetailActivity : AppCompatActivity() {

    private lateinit var viewPagerImages: ViewPager2
    private lateinit var tvImageCounter: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvPrice: TextView
    private lateinit var tvCategory: TextView
    private lateinit var tvCondition: TextView
    private lateinit var tvNegotiable: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvDistance: TextView
    private lateinit var ivSellerAvatar: CircleImageView
    private lateinit var tvSellerName: TextView
    private lateinit var ratingBar: RatingBar
    private lateinit var tvRating: TextView
    private lateinit var tvListingDate: TextView
    private lateinit var btnShare: Button
    private lateinit var btnContactSeller: Button
    private lateinit var btnMakeOffer: Button

    private var listingId: String = ""
    private var sellerId: String = ""
    private var sellerName: String = ""
    private var originalPrice: Double = 0.0
    private var isNegotiable: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listing_detail)

        initViews()
        getIntentData()
        setupClickListeners()
        loadListingData()
    }

    private fun initViews() {
        viewPagerImages = findViewById(R.id.viewPagerImages)
        tvImageCounter = findViewById(R.id.tvImageCounter)
        tvTitle = findViewById(R.id.tvTitle)
        tvPrice = findViewById(R.id.tvPrice)
        tvCategory = findViewById(R.id.tvCategory)
        tvCondition = findViewById(R.id.tvCondition)
        tvNegotiable = findViewById(R.id.tvNegotiable)
        tvDescription = findViewById(R.id.tvDescription)
        tvLocation = findViewById(R.id.tvLocation)
        tvDistance = findViewById(R.id.tvDistance)
        ivSellerAvatar = findViewById(R.id.ivSellerAvatar)
        tvSellerName = findViewById(R.id.tvSellerName)
        ratingBar = findViewById(R.id.ratingBar)
        tvRating = findViewById(R.id.tvRating)
        tvListingDate = findViewById(R.id.tvListingDate)
        btnShare = findViewById(R.id.btnShare)
        btnContactSeller = findViewById(R.id.btnContactSeller)
        btnMakeOffer = findViewById(R.id.btnMakeOffer)
    }

    private fun getIntentData() {
        listingId = intent.getStringExtra("listing_id") ?: ""
        sellerId = intent.getStringExtra("ownerUid") ?: ""
        sellerName = intent.getStringExtra("ownerName") ?: ""
        originalPrice = intent.getDoubleExtra("price", 0.0)
        isNegotiable = intent.getBooleanExtra("negotiable", false)

        // Set basic data from intent
        tvTitle.text = intent.getStringExtra("title") ?: ""
        tvPrice.text = "${String.format("%,.0f", originalPrice)} VND"
        tvCategory.text = intent.getStringExtra("category") ?: ""
        tvCondition.text = intent.getStringExtra("condition") ?: ""
        tvDescription.text = intent.getStringExtra("description") ?: ""
        tvLocation.text = intent.getStringExtra("location") ?: ""
        tvSellerName.text = sellerName

        // Show/hide negotiable status
        if (isNegotiable) {
            tvNegotiable.visibility = android.view.View.VISIBLE
            btnMakeOffer.visibility = android.view.View.VISIBLE
        }
    }

    private fun setupClickListeners() {
        btnContactSeller.setOnClickListener {
            val intent = Intent(this, MessageActivity::class.java).apply {
                putExtra("other_user_id", sellerId)
                putExtra("other_user_name", sellerName)
                putExtra("listing_id", listingId)
            }
            startActivity(intent)
        }

        btnMakeOffer.setOnClickListener {
            val intent = Intent(this, MakeOfferActivity::class.java).apply {
                putExtra("listing_id", listingId)
                putExtra("seller_id", sellerId)
                putExtra("seller_name", sellerName)
                putExtra("original_price", originalPrice)
                putExtra("listing_title", tvTitle.text.toString())
                putExtra("listing_image", intent.getStringExtra("imageUrl"))
            }
            startActivity(intent)
        }

        btnShare.setOnClickListener {
            shareListingLink()
        }
    }

    private fun loadListingData() {
        // Load images if available
        val imageUrl = intent.getStringExtra("imageUrl")
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(imageUrl)
                .centerCrop()
                .placeholder(R.drawable.placeholder_image)
                .into(ivSellerAvatar)
        }

        // Update image counter
        tvImageCounter.text = "1/1"

        // Set listing date
        tvListingDate.text = "Recently posted"

        // Set default rating
        ratingBar.rating = 4.5f
        tvRating.text = "4.5"
    }

    private fun shareListingLink() {
        val shareText = """
            Check out this item on TradeUp!
            
            ${tvTitle.text}
            Price: ${tvPrice.text}
            
            Download TradeUp to view more details and contact the seller.
        """.trimIndent()

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(shareIntent, "Share listing"))
    }
}