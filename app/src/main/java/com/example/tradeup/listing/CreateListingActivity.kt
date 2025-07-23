package com.example.tradeup.listing

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.tradeup.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class CreateListingActivity : AppCompatActivity() {

    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var etPrice: EditText
    private lateinit var etLocation: EditText
    private lateinit var spCategory: Spinner
    private lateinit var spCondition: Spinner
    private lateinit var ivImage: ImageView
    private lateinit var btnCreateListing: Button
    private lateinit var progressBar: ProgressBar

    private var selectedImageUri: Uri? = null
    private var isNegotiable: Boolean = true // Default value

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            Glide.with(this)
                .load(it)
                .centerCrop()
                .placeholder(R.drawable.ic_image_placeholder)
                .into(ivImage)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_listing)

        initViews()
        setupSpinners()
        setupClickListeners()
    }

    private fun initViews() {
        etTitle = findViewById(R.id.etTitle)
        etDescription = findViewById(R.id.etDescription)
        etPrice = findViewById(R.id.etPrice)
        etLocation = findViewById(R.id.etLocation)
        spCategory = findViewById(R.id.spCategory)
        spCondition = findViewById(R.id.spCondition)
        ivImage = findViewById(R.id.ivImage)
        btnCreateListing = findViewById(R.id.btnCreateListing)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupSpinners() {
        // Category spinner
        val categories = arrayOf("Select Category", "Electronics", "Clothing", "Home & Garden", "Sports", "Vehicles", "Books", "Other")
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCategory.adapter = categoryAdapter

        // Condition spinner
        val conditions = arrayOf("Select Condition", "New", "Like New", "Good", "Fair", "Poor")
        val conditionAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, conditions)
        conditionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCondition.adapter = conditionAdapter
    }

    private fun setupClickListeners() {
        // Handle image selection via ImageView click
        ivImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        btnCreateListing.setOnClickListener {
            if (validateInput()) {
                createListing()
            }
        }
    }

    private fun validateInput(): Boolean {
        val title = etTitle.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val priceText = etPrice.text.toString().trim()
        val location = etLocation.text.toString().trim()

        if (title.isEmpty()) {
            etTitle.error = "Please enter a title"
            etTitle.requestFocus()
            return false
        }

        if (description.isEmpty()) {
            etDescription.error = "Please enter a description"
            etDescription.requestFocus()
            return false
        }

        if (priceText.isEmpty()) {
            etPrice.error = "Please enter a price"
            etPrice.requestFocus()
            return false
        }

        val price = priceText.toDoubleOrNull()
        if (price == null || price <= 0) {
            etPrice.error = "Please enter a valid price"
            etPrice.requestFocus()
            return false
        }

        if (location.isEmpty()) {
            etLocation.error = "Please enter a location"
            etLocation.requestFocus()
            return false
        }

        if (spCategory.selectedItemPosition == 0) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            return false
        }

        if (spCondition.selectedItemPosition == 0) {
            Toast.makeText(this, "Please select a condition", Toast.LENGTH_SHORT).show()
            return false
        }

        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image by clicking on the image area", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun createListing() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        // Upload image first
        uploadImage { imageUrl ->
            if (imageUrl != null) {
                saveListing(imageUrl)
            } else {
                showLoading(false)
                Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadImage(callback: (String?) -> Unit) {
        selectedImageUri?.let { uri ->
            val fileName = "listings/${UUID.randomUUID()}.jpg"
            val storageRef = FirebaseStorage.getInstance().reference.child(fileName)

            storageRef.putFile(uri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        callback(downloadUri.toString())
                    }.addOnFailureListener {
                        callback(null)
                    }
                }
                .addOnFailureListener {
                    callback(null)
                }
        } ?: callback(null)
    }

    private fun saveListing(imageUrl: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser!!

        val listing = hashMapOf(
            "title" to etTitle.text.toString().trim(),
            "description" to etDescription.text.toString().trim(),
            "price" to etPrice.text.toString().trim().toDouble(),
            "currency" to "VND",
            "category" to spCategory.selectedItem.toString(),
            "condition" to spCondition.selectedItem.toString(),
            "location" to etLocation.text.toString().trim(),
            "imageUrl" to imageUrl,
            "imageUrls" to listOf(imageUrl),
            "ownerUid" to currentUser.uid,
            "ownerName" to (currentUser.displayName ?: "Unknown User"),
            "ownerAvatar" to (currentUser.photoUrl?.toString() ?: ""),
            "status" to "Available",
            "tags" to emptyList<String>(),
            "views" to 0,
            "interactions" to 0,
            "isNegotiable" to isNegotiable,
            "createdAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis()
        )

        FirebaseFirestore.getInstance()
            .collection("listings")
            .add(listing)
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, "Listing created successfully!", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Failed to create listing: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        btnCreateListing.isEnabled = !show
    }
}