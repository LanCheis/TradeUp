package com.example.tradeup.listing

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tradeup.MainActivity
import com.example.tradeup.R
import com.example.tradeup.data.model.Listing
import com.example.tradeup.data.remote.ListingRepository
import com.example.tradeup.utils.CloudinaryHelper
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class PreviewListingActivity : AppCompatActivity() {

    private lateinit var recyclerImages: RecyclerView
    private lateinit var tvTitle: TextView
    private lateinit var tvPrice: TextView
    private lateinit var tvCategory: TextView
    private lateinit var tvCondition: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvNegotiable: TextView
    private lateinit var btnEdit: Button
    private lateinit var btnPost: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutContent: LinearLayout

    private lateinit var previewData: PreviewData
    private lateinit var imageAdapter: PreviewImageAdapter

    private lateinit var auth: FirebaseAuth
    private lateinit var listingRepository: ListingRepository

    data class PreviewData(
        val title: String,
        val price: Double,
        val category: String,
        val condition: String,
        val description: String,
        val location: String,
        val negotiable: Boolean,
        val latitude: Double?,
        val longitude: Double?,
        val imageUris: List<String>
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview_listing)

        // Initialize CloudinaryHelper
        CloudinaryHelper.initialize(this)

        auth = FirebaseAuth.getInstance()
        listingRepository = ListingRepository()

        initViews()
        extractPreviewData()
        setupImageAdapter()
        displayPreviewData()
        setupClickListeners()
        setupToolbar()
    }

    private fun initViews() {
        recyclerImages = findViewById(R.id.recyclerImages)
        tvTitle = findViewById(R.id.tvTitle)
        tvPrice = findViewById(R.id.tvPrice)
        tvCategory = findViewById(R.id.tvCategory)
        tvCondition = findViewById(R.id.tvCondition)
        tvDescription = findViewById(R.id.tvDescription)
        tvLocation = findViewById(R.id.tvLocation)
        tvNegotiable = findViewById(R.id.tvNegotiable)
        btnEdit = findViewById(R.id.btnEdit)
        btnPost = findViewById(R.id.btnPost)
        progressBar = findViewById(R.id.progressBar)
        layoutContent = findViewById(R.id.layoutContent)
    }

    private fun extractPreviewData() {
        previewData = PreviewData(
            title = intent.getStringExtra("title") ?: "",
            price = intent.getDoubleExtra("price", 0.0),
            category = intent.getStringExtra("category") ?: "",
            condition = intent.getStringExtra("condition") ?: "",
            description = intent.getStringExtra("description") ?: "",
            location = intent.getStringExtra("location") ?: "",
            negotiable = intent.getBooleanExtra("negotiable", false),
            latitude = intent.getDoubleExtra("latitude", 0.0).takeIf { it != 0.0 },
            longitude = intent.getDoubleExtra("longitude", 0.0).takeIf { it != 0.0 },
            imageUris = intent.getStringArrayListExtra("imageUris") ?: emptyList()
        )
    }

    private fun setupImageAdapter() {
        val imageUris = previewData.imageUris.map { Uri.parse(it) }
        imageAdapter = PreviewImageAdapter(imageUris)

        recyclerImages.layoutManager = LinearLayoutManager(
            this, LinearLayoutManager.HORIZONTAL, false
        )
        recyclerImages.adapter = imageAdapter
    }

    private fun displayPreviewData() {
        tvTitle.text = previewData.title
        tvPrice.text = "₫${String.format("%,.0f", previewData.price)}"
        tvCategory.text = previewData.category
        tvCondition.text = previewData.condition
        tvDescription.text = previewData.description
        tvLocation.text = previewData.location

        tvNegotiable.visibility = if (previewData.negotiable) {
            tvNegotiable.text = "💰 Price is negotiable"
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun setupClickListeners() {
        btnEdit.setOnClickListener { finish() }
        btnPost.setOnClickListener { confirmAndPostListing() }
    }

    private fun setupToolbar() {
        supportActionBar?.apply {
            title = getString(R.string.preview_your_listing)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun confirmAndPostListing() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val imageUrls = uploadImages()
                val currentUser = auth.currentUser!!

                val listing = Listing(
                    title = previewData.title,
                    description = previewData.description,
                    price = previewData.price,
                    category = previewData.category,
                    condition = previewData.condition,
                    location = previewData.location,
                    imageUrls = imageUrls,
                    isNegotiable = previewData.negotiable,
                    latitude = previewData.latitude,
                    longitude = previewData.longitude,
                    sellerId = currentUser.uid,
                    sellerName = currentUser.displayName ?: "User",
                    sellerImageUrl = currentUser.photoUrl?.toString() ?: "",
                    status = "Available",
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                )

                val result = listingRepository.createListing(listing)

                if (result.isSuccess) {
                    showSuccess()
                } else {
                    showError("Failed to create listing")
                }

            } catch (e: Exception) {
                showError("Error: ${e.message}")
            }
        }
    }

    private suspend fun uploadImages(): List<String> {
        val uploadedUrls = mutableListOf<String>()

        for ((index, uriString) in previewData.imageUris.withIndex()) {
            try {
                val uri = Uri.parse(uriString)
                // 🔥 FIX: Use new listing upload method with unique ID
                val listingId = "${System.currentTimeMillis()}_$index"
                val result = CloudinaryHelper.uploadListingImage(this, uri, listingId)

                if (result != null) {
                    uploadedUrls.add(result)
                    println("✅ Image ${index + 1} uploaded: $result")
                } else {
                    throw Exception("Failed to upload image ${index + 1}")
                }
            } catch (e: Exception) {
                throw Exception("Image ${index + 1} upload failed: ${e.message}")
            }
        }

        return uploadedUrls
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        layoutContent.visibility = if (show) View.GONE else View.VISIBLE
        btnEdit.isEnabled = !show
        btnPost.isEnabled = !show
    }

    private fun showSuccess() {
        Toast.makeText(this, "🎉 Listing posted successfully!", Toast.LENGTH_LONG).show()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun showError(message: String) {
        showLoading(false)
        Toast.makeText(this, "❌ $message", Toast.LENGTH_LONG).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}