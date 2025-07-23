package com.example.tradeup.offers

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.tradeup.R
import com.example.tradeup.data.model.Offer
import com.example.tradeup.data.remote.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.*

class MakeOfferActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var ivItemImage: ImageView
    private lateinit var tvItemTitle: TextView
    private lateinit var tvAskingPrice: TextView
    private lateinit var tvSellerName: TextView
    private lateinit var etOfferPrice: EditText
    private lateinit var etMessage: EditText
    private lateinit var btnSubmitOffer: Button
    private lateinit var btn90Percent: Button
    private lateinit var btn80Percent: Button
    private lateinit var btn70Percent: Button

    private var listingId: String = ""
    private var listingTitle: String = ""
    private var listingImageUrl: String = ""
    private var sellerUid: String = ""
    private var sellerName: String = ""
    private var askingPrice: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_make_offer)

        getIntentData()
        initViews()
        setupClickListeners()
        setupTextWatchers()
        displayItemInfo()
    }

    private fun getIntentData() {
        listingId = intent.getStringExtra("listing_id") ?: ""
        listingTitle = intent.getStringExtra("listing_title") ?: ""
        listingImageUrl = intent.getStringExtra("listing_image") ?: ""
        sellerUid = intent.getStringExtra("seller_uid") ?: ""
        sellerName = intent.getStringExtra("seller_name") ?: ""
        askingPrice = intent.getDoubleExtra("asking_price", 0.0)

        if (listingId.isEmpty() || sellerUid.isEmpty()) {
            Toast.makeText(this, "Invalid listing data", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        ivItemImage = findViewById(R.id.ivItemImage)
        tvItemTitle = findViewById(R.id.tvItemTitle)
        tvAskingPrice = findViewById(R.id.tvAskingPrice)
        tvSellerName = findViewById(R.id.tvSellerName)
        etOfferPrice = findViewById(R.id.etOfferPrice)
        etMessage = findViewById(R.id.etMessage)
        btnSubmitOffer = findViewById(R.id.btnSubmitOffer)
        btn90Percent = findViewById(R.id.btn90Percent)
        btn80Percent = findViewById(R.id.btn80Percent)
        btn70Percent = findViewById(R.id.btn70Percent)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }

        // Quick suggestion buttons
        btn90Percent.setOnClickListener {
            val offer = (askingPrice * 0.9).toLong()
            etOfferPrice.setText(offer.toString())
        }

        btn80Percent.setOnClickListener {
            val offer = (askingPrice * 0.8).toLong()
            etOfferPrice.setText(offer.toString())
        }

        btn70Percent.setOnClickListener {
            val offer = (askingPrice * 0.7).toLong()
            etOfferPrice.setText(offer.toString())
        }

        btnSubmitOffer.setOnClickListener {
            submitOffer()
        }
    }

    private fun setupTextWatchers() {
        etOfferPrice.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateSubmitButton()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun displayItemInfo() {
        tvItemTitle.text = listingTitle
        tvAskingPrice.text = "Asking: ${formatPrice(askingPrice)}"
        tvSellerName.text = "Seller: $sellerName"

        // Load item image
        Glide.with(this)
            .load(listingImageUrl)
            .error(R.drawable.ic_image_placeholder)
            .into(ivItemImage)
    }

    private fun updateSubmitButton() {
        val offerText = etOfferPrice.text.toString().trim()
        val isValid = offerText.isNotEmpty() &&
                try { offerText.toDouble() > 0 } catch (e: Exception) { false }

        btnSubmitOffer.isEnabled = isValid
    }

    private fun submitOffer() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        val offerPriceText = etOfferPrice.text.toString().trim()
        val message = etMessage.text.toString().trim()

        if (offerPriceText.isEmpty()) {
            etOfferPrice.error = "Please enter your offer"
            return
        }

        val offerPrice = try {
            offerPriceText.toDouble()
        } catch (e: Exception) {
            etOfferPrice.error = "Invalid price format"
            return
        }

        if (offerPrice <= 0) {
            etOfferPrice.error = "Offer must be greater than 0"
            return
        }

        if (offerPrice >= askingPrice) {
            etOfferPrice.error = "Offer should be less than asking price"
            return
        }

        // Show loading state
        btnSubmitOffer.isEnabled = false
        btnSubmitOffer.text = "Submitting..."

        // Get current user profile
        UserRepository.getUserProfile(currentUser.uid) { user ->
            val offer = Offer(
                id = UUID.randomUUID().toString(),
                listingId = listingId,
                listingTitle = listingTitle,
                listingImageUrl = listingImageUrl,
                buyerId = currentUser.uid,
                buyerName = user?.name ?: currentUser.displayName ?: "Anonymous",
                sellerId = sellerUid,
                sellerName = sellerName,
                originalPrice = askingPrice,
                offerPrice = offerPrice,
                message = message,
                status = "pending"
            )

            // Save offer to Firestore
            FirebaseFirestore.getInstance()
                .collection("offers")
                .document(offer.id)
                .set(offer)
                .addOnSuccessListener {
                    Toast.makeText(this, "✅ Offer submitted successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { error ->
                    btnSubmitOffer.isEnabled = true
                    btnSubmitOffer.text = "🚀 Submit Offer"
                    Toast.makeText(this, "❌ Failed to submit offer: ${error.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun formatPrice(price: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        return "${formatter.format(price)} ₫"
    }
}