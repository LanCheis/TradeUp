package com.example.tradeup.offers

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.tradeup.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class MakeOfferActivity : AppCompatActivity() {

    private lateinit var ivListingImage: ImageView
    private lateinit var tvListingTitle: TextView
    private lateinit var tvListingPrice: TextView
    private lateinit var etOfferAmount: EditText
    private lateinit var etOfferMessage: EditText
    private lateinit var btnSendOffer: Button
    private lateinit var progressBar: ProgressBar

    private var listingId: String = ""
    private var sellerId: String = ""
    private var sellerName: String = ""
    private var originalPrice: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_make_offer)

        initViews()
        getIntentData()
        setupClickListeners()
    }

    private fun initViews() {
        ivListingImage = findViewById(R.id.ivListingImage)
        tvListingTitle = findViewById(R.id.tvListingTitle)
        tvListingPrice = findViewById(R.id.tvListingPrice)
        etOfferAmount = findViewById(R.id.etOfferAmount)
        etOfferMessage = findViewById(R.id.etOfferMessage)
        btnSendOffer = findViewById(R.id.btnSendOffer)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun getIntentData() {
        listingId = intent.getStringExtra("listing_id") ?: ""
        sellerId = intent.getStringExtra("seller_id") ?: ""
        sellerName = intent.getStringExtra("seller_name") ?: ""
        originalPrice = intent.getDoubleExtra("original_price", 0.0)

        val title = intent.getStringExtra("listing_title") ?: ""
        val imageUrl = intent.getStringExtra("listing_image") ?: ""

        tvListingTitle.text = title
        tvListingPrice.text = "Original Price: ${String.format("%,.0f", originalPrice)} VND"

        if (imageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(imageUrl)
                .centerCrop()
                .placeholder(R.drawable.placeholder_image)
                .into(ivListingImage)
        }

        // Pre-fill offer amount with 80% of original price
        val suggestedOffer = (originalPrice * 0.8).toLong()
        etOfferAmount.setText(String.format("%,d", suggestedOffer))
    }

    private fun setupClickListeners() {
        btnSendOffer.setOnClickListener {
            if (validateInput()) {
                sendOffer()
            }
        }
    }

    private fun validateInput(): Boolean {
        val offerAmountStr = etOfferAmount.text.toString().trim().replace("[^\\d]".toRegex(), "")

        if (offerAmountStr.isEmpty()) {
            etOfferAmount.error = "Please enter your offer amount"
            etOfferAmount.requestFocus()
            return false
        }

        val offerAmount = offerAmountStr.toDoubleOrNull()
        if (offerAmount == null || offerAmount <= 0) {
            etOfferAmount.error = "Please enter a valid amount"
            etOfferAmount.requestFocus()
            return false
        }

        if (offerAmount >= originalPrice) {
            etOfferAmount.error = "Offer should be less than original price"
            etOfferAmount.requestFocus()
            return false
        }

        return true
    }

    private fun sendOffer() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        val offerAmountStr = etOfferAmount.text.toString().replace("[^\\d]".toRegex(), "")
        val offerAmount = offerAmountStr.toDouble()
        val message = etOfferMessage.text.toString().trim()

        val offer = hashMapOf(
            "listingId" to listingId,
            "buyerId" to currentUser.uid,
            "buyerName" to (currentUser.displayName ?: "Anonymous"),
            "sellerId" to sellerId,
            "sellerName" to sellerName,
            "originalPrice" to originalPrice,
            "offerAmount" to offerAmount,
            "message" to message,
            "status" to "pending",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        FirebaseFirestore.getInstance()
            .collection("offers")
            .add(offer)
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, "Offer sent successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Failed to send offer: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        btnSendOffer.isEnabled = !show
    }
}