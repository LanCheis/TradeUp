package com.example.tradeup.listing

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.tradeup.R
import com.example.tradeup.data.model.Listing
import com.example.tradeup.utils.CloudinaryHelper
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.*

class CreateListingActivity : AppCompatActivity() {

    private lateinit var etTitle: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var etPrice: TextInputEditText
    private lateinit var spCategory: Spinner
    private lateinit var spCondition: Spinner
    private lateinit var etLocation: TextInputEditText
    private lateinit var switchNegotiable: Switch
    private lateinit var ivListingImage: ImageView
    private lateinit var btnPickImage: Button
    private lateinit var btnSubmit: Button
    private lateinit var progressBar: ProgressBar

    private var imageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_listing_enhanced)

        initViews()
        setupSpinners()
        setupClickListeners()
    }

    private fun initViews() {
        etTitle = findViewById(R.id.etTitle)
        etDescription = findViewById(R.id.etDescription)
        etPrice = findViewById(R.id.etPrice)
        spCategory = findViewById(R.id.spCategory)
        spCondition = findViewById(R.id.spCondition)
        etLocation = findViewById(R.id.etLocation)
        switchNegotiable = findViewById(R.id.switchNegotiable)
        ivListingImage = findViewById(R.id.ivListingImage)
        btnPickImage = findViewById(R.id.btnPickImage)
        btnSubmit = findViewById(R.id.btnSubmit)
        progressBar = findViewById(R.id.progressBar)

        ivListingImage.setImageResource(R.drawable.ic_image_placeholder)
        progressBar.visibility = View.GONE
    }

    private fun setupSpinners() {
        // Categories
        val categories = arrayOf("Select Category", "Electronics", "Fashion", "Home & Garden", "Sports", "Books", "Other")
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spCategory.adapter = categoryAdapter

        // Conditions
        val conditions = arrayOf("Select Condition", "New", "Like New", "Good", "Fair", "Poor")
        val conditionAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, conditions)
        spCondition.adapter = conditionAdapter
    }

    private fun setupClickListeners() {
        btnPickImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        btnSubmit.setOnClickListener {
            if (validateInput()) {
                uploadImageAndCreateListing()
            }
        }

        // Price formatting
        etPrice.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                formatPrice()
            }
        }
    }

    private fun formatPrice() {
        val priceText = etPrice.text.toString().replace(",", "").replace("₫", "").trim()
        if (priceText.isNotEmpty()) {
            try {
                val price = priceText.toDouble()
                val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
                etPrice.setText("${formatter.format(price)} ₫")
            } catch (e: NumberFormatException) {
                // Invalid number format
            }
        }
    }

    private fun validateInput(): Boolean {
        var isValid = true

        if (etTitle.text.toString().trim().isEmpty()) {
            etTitle.error = "Title is required"
            isValid = false
        }

        if (etDescription.text.toString().trim().isEmpty()) {
            etDescription.error = "Description is required"
            isValid = false
        }

        if (etPrice.text.toString().trim().isEmpty()) {
            etPrice.error = "Price is required"
            isValid = false
        }

        if (spCategory.selectedItemPosition == 0) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (spCondition.selectedItemPosition == 0) {
            Toast.makeText(this, "Please select item condition", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (etLocation.text.toString().trim().isEmpty()) {
            etLocation.error = "Location is required"
            isValid = false
        }

        if (imageUri == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    private fun uploadImageAndCreateListing() {
        imageUri?.let { uri ->
            showLoading(true)

            CloudinaryHelper.uploadListingImage(
                context = this,
                imageUri = uri,
                onSuccess = { imageUrl ->
                    createListing(imageUrl)
                },
                onFailure = { error ->
                    handleUploadError(error)
                },
                onProgress = { progress ->
                    btnSubmit.text = "Uploading... $progress%"
                }
            )
        }
    }

    private fun createListing(imageUrl: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            showLoading(false)
            return
        }

        // Parse price
        val priceText = etPrice.text.toString().replace(",", "").replace("₫", "").trim()
        val price = try {
            priceText.toDouble()
        } catch (e: NumberFormatException) {
            0.0
        }

        val listing = Listing(
            id = UUID.randomUUID().toString(),
            title = etTitle.text.toString().trim(),
            description = etDescription.text.toString().trim(),
            price = price,
            category = spCategory.selectedItem.toString(),
            condition = spCondition.selectedItem.toString(),
            location = etLocation.text.toString().trim(),
            imageUrls = listOf(imageUrl), // Use imageUrls instead of imageUrl
            ownerUid = currentUser.uid,
            ownerName = currentUser.displayName ?: "Anonymous",
            isNegotiable = switchNegotiable.isChecked,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        FirebaseFirestore.getInstance()
            .collection("listings")
            .document(listing.id)
            .set(listing)
            .addOnSuccessListener {
                Toast.makeText(this, "🎉 Listing created successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "❌ Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
    }

    private fun handleUploadError(error: String) {
        Toast.makeText(this, "❌ Upload failed: $error", Toast.LENGTH_LONG).show()
        showLoading(false)
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnSubmit.isEnabled = !show
        if (!show) {
            btnSubmit.text = "Create Listing"
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            imageUri?.let { uri ->
                Glide.with(this)
                    .load(uri)
                    .error(R.drawable.ic_image_placeholder)
                    .into(ivListingImage)
                btnPickImage.text = "✓ Image Selected"
            }
        }
    }
}