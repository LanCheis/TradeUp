package com.example.tradeup.listing

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tradeup.R
import com.example.tradeup.data.model.Category
import com.example.tradeup.data.model.Listing
import com.example.tradeup.data.remote.ListingRepository
import com.example.tradeup.utils.CloudinaryHelper
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class AddListingActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var listingRepository: ListingRepository
    private lateinit var cloudinaryHelper: CloudinaryHelper

    // Views - only the essential ones
    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var etPrice: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var etLocation: EditText
    private lateinit var switchNegotiable: Switch
    private lateinit var btnCreateListing: Button
    private lateinit var progressBar: ProgressBar

    // Optional views (with safe initialization)
    private var chipGroupCondition: ChipGroup? = null
    private var btnSelectImages: Button? = null

    // Data
    private val selectedImageUris = mutableListOf<Uri>()
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_listing)

        // Initialize Firebase and repositories
        auth = FirebaseAuth.getInstance()
        listingRepository = ListingRepository()
        cloudinaryHelper = CloudinaryHelper()

        // Setup toolbar
        setupToolbar()

        // Initialize views
        initViews()

        // Setup image picker
        setupImagePicker()

        // Setup spinners and chips
        setupCategorySpinner()
        setupConditionChips()

        // Setup click listeners
        setupClickListeners()

        Log.d("AddListingActivity", "✅ AddListingActivity created successfully")
    }

    private fun setupToolbar() {
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            title = "Create New Listing"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    private fun initViews() {
        // Essential views (must exist)
        etTitle = findViewById(R.id.etTitle)
        etDescription = findViewById(R.id.etDescription)
        etPrice = findViewById(R.id.etPrice)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        etLocation = findViewById(R.id.etLocation)
        switchNegotiable = findViewById(R.id.switchNegotiable)
        btnCreateListing = findViewById(R.id.btnCreateListing)
        progressBar = findViewById(R.id.progressBar)

        // Optional views (safe initialization)
        try {
            chipGroupCondition = findViewById(R.id.chipGroupCondition)
        } catch (e: Exception) {
            Log.w("AddListingActivity", "chipGroupCondition not found: ${e.message}")
        }

        try {
            btnSelectImages = findViewById(R.id.btnSelectImages)
        } catch (e: Exception) {
            Log.w("AddListingActivity", "btnSelectImages not found: ${e.message}")
        }
    }

    private fun setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                data?.let { intent ->
                    if (intent.clipData != null) {
                        // Multiple images selected
                        val clipData = intent.clipData!!
                        for (i in 0 until clipData.itemCount) {
                            val uri = clipData.getItemAt(i).uri
                            if (selectedImageUris.size < 10) {
                                selectedImageUris.add(uri)
                            }
                        }
                    } else if (intent.data != null) {
                        // Single image selected
                        val uri = intent.data!!
                        if (selectedImageUris.size < 10) {
                            selectedImageUris.add(uri)
                        }
                    }

                    updateImageButtonText()
                }
            }
        }
    }

    private fun setupCategorySpinner() {
        val categories = Category.getAllCategories().map { it.displayName }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter
    }

    private fun setupConditionChips() {
        chipGroupCondition?.let { chipGroup ->
            val conditions = Listing.getAllConditions()

            conditions.forEach { condition ->
                val chip = Chip(this)
                chip.text = condition
                chip.isCheckable = true
                chip.id = View.generateViewId()
                chipGroup.addView(chip)
            }

            // Set single selection
            chipGroup.isSingleSelection = true
        }
    }

    private fun setupClickListeners() {
        btnSelectImages?.setOnClickListener {
            openImagePicker()
        }

        btnCreateListing.setOnClickListener {
            createListing()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png"))
        }

        imagePickerLauncher.launch(Intent.createChooser(intent, "Select Images"))
    }

    private fun updateImageButtonText() {
        btnSelectImages?.text = if (selectedImageUris.isEmpty()) {
            "Select Images (0/10)"
        } else {
            "Select Images (${selectedImageUris.size}/10)"
        }
    }

    private fun createListing() {
        if (!validateInputs()) return

        showLoading(true)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            showError("Please login first")
            showLoading(false)
            return
        }

        lifecycleScope.launch {
            try {
                // Upload images to Cloudinary first
                val imageUrls = mutableListOf<String>()

                for (uri in selectedImageUris) {
                    val uploadedUrl = cloudinaryHelper.uploadImage(uri, this@AddListingActivity)
                    if (uploadedUrl != null) {
                        imageUrls.add(uploadedUrl)
                    }
                }

                // Create listing object
                val listing = Listing(
                    id = "", // Firestore will generate this
                    title = etTitle.text.toString().trim(),
                    description = etDescription.text.toString().trim(),
                    price = etPrice.text.toString().toDoubleOrNull() ?: 0.0,
                    category = getSelectedCategory(),
                    condition = getSelectedCondition(),
                    location = etLocation.text.toString().trim(),
                    imageUrls = imageUrls,
                    sellerId = currentUser.uid,
                    sellerName = currentUser.displayName ?: "Unknown",
                    isNegotiable = switchNegotiable.isChecked,
                    createdAt = com.google.firebase.Timestamp.now(),
                    updatedAt = com.google.firebase.Timestamp.now(),
                    status = "available"
                )

                // Save to Firestore
                val success = listingRepository.createListing(listing)

                showLoading(false)

                if (success) {
                    Toast.makeText(this@AddListingActivity, "Listing created successfully!", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    showError("Failed to create listing. Please try again.")
                }

            } catch (e: Exception) {
                Log.e("AddListingActivity", "Error creating listing: ${e.message}")
                showLoading(false)
                showError("Error creating listing: ${e.message}")
            }
        }
    }

    private fun validateInputs(): Boolean {
        when {
            etTitle.text.toString().trim().isEmpty() -> {
                etTitle.error = "Please enter a title"
                etTitle.requestFocus()
                return false
            }
            etDescription.text.toString().trim().isEmpty() -> {
                etDescription.error = "Please enter a description"
                etDescription.requestFocus()
                return false
            }
            etPrice.text.toString().trim().isEmpty() -> {
                etPrice.error = "Please enter a price"
                etPrice.requestFocus()
                return false
            }
            etPrice.text.toString().toDoubleOrNull() == null || etPrice.text.toString().toDouble() <= 0 -> {
                etPrice.error = "Please enter a valid price"
                etPrice.requestFocus()
                return false
            }
            getSelectedCondition().isEmpty() -> {
                Toast.makeText(this, "Please select a condition", Toast.LENGTH_SHORT).show()
                return false
            }
            etLocation.text.toString().trim().isEmpty() -> {
                etLocation.error = "Please enter a location"
                etLocation.requestFocus()
                return false
            }
        }
        return true
    }

    private fun getSelectedCategory(): String {
        val position = spinnerCategory.selectedItemPosition
        return Category.getAllCategories()[position].name
    }

    private fun getSelectedCondition(): String {
        chipGroupCondition?.let { chipGroup ->
            val selectedChipId = chipGroup.checkedChipId
            return if (selectedChipId != View.NO_ID) {
                findViewById<Chip>(selectedChipId).text.toString()
            } else {
                ""
            }
        }
        // Fallback if no chip group - return default condition
        return "Good"
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnCreateListing.isEnabled = !show
        btnSelectImages?.isEnabled = !show
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}