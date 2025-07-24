package com.example.tradeup.listing

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tradeup.R
import com.example.tradeup.utils.LocationHelper
import com.example.tradeup.utils.PermissionHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.NumberFormat
import java.util.*

class CreateListingActivity : AppCompatActivity() {

    // Form Views
    private lateinit var etTitle: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var etPrice: TextInputEditText
    private lateinit var etLocation: TextInputEditText
    private lateinit var spCategory: Spinner
    private lateinit var spCondition: Spinner
    private lateinit var switchNegotiable: SwitchMaterial
    private lateinit var btnGetLocation: MaterialButton
    private lateinit var btnPickImages: MaterialButton
    private lateinit var btnCreateListing: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvImageCount: TextView
    private lateinit var rvSelectedImages: RecyclerView

    // Preview Views
    private lateinit var mainFormLayout: ScrollView
    private lateinit var previewLayout: LinearLayout
    private lateinit var previewTitle: TextView
    private lateinit var previewPrice: TextView
    private lateinit var previewCategory: TextView
    private lateinit var previewCondition: TextView
    private lateinit var previewDescription: TextView
    private lateinit var previewLocation: TextView
    private lateinit var previewNegotiable: TextView
    private lateinit var previewImagesRv: RecyclerView
    private lateinit var btnBackToEdit: MaterialButton
    private lateinit var btnConfirmPost: MaterialButton
    private lateinit var previewProgressBar: ProgressBar

    // Data
    private var selectedImageUris = mutableListOf<Uri>()
    private var isNegotiable: Boolean = true
    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUris.clear()
            selectedImageUris.addAll(uris.take(10)) // Max 10 images
            updateImageCount()
            updateImageRecyclerView()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_listing)

        initViews()
        setupSpinners()
        setupClickListeners()
        setupRecyclerViews()
    }

    private fun initViews() {
        // Form views
        etTitle = findViewById(R.id.etTitle)
        etDescription = findViewById(R.id.etDescription)
        etPrice = findViewById(R.id.etPrice)
        etLocation = findViewById(R.id.etLocation)
        spCategory = findViewById(R.id.spCategory)
        spCondition = findViewById(R.id.spCondition)
        switchNegotiable = findViewById(R.id.switchNegotiable)
        btnGetLocation = findViewById(R.id.btnGetLocation)  // ✅ Matches your layout
        btnPickImages = findViewById(R.id.btnPickImages)
        btnCreateListing = findViewById(R.id.btnCreateListing)
        progressBar = findViewById(R.id.progressBar)
        tvImageCount = findViewById(R.id.tvImageCount)
        rvSelectedImages = findViewById(R.id.rvSelectedImages)

        // Preview views
        mainFormLayout = findViewById(R.id.mainFormLayout)
        previewLayout = findViewById(R.id.previewLayout)
        previewTitle = findViewById(R.id.previewTitle)
        previewPrice = findViewById(R.id.previewPrice)
        previewCategory = findViewById(R.id.previewCategory)
        previewCondition = findViewById(R.id.previewCondition)
        previewDescription = findViewById(R.id.previewDescription)
        previewLocation = findViewById(R.id.previewLocation)
        previewNegotiable = findViewById(R.id.previewNegotiable)
        previewImagesRv = findViewById(R.id.previewImagesRv)
        btnBackToEdit = findViewById(R.id.btnBackToEdit)
        btnConfirmPost = findViewById(R.id.btnConfirmPost)
        previewProgressBar = findViewById(R.id.previewProgressBar)
    }

    private fun setupSpinners() {
        // Category spinner
        val categories = arrayOf(
            "Select Category", "Electronics", "Clothing", "Home & Garden",
            "Sports", "Vehicles", "Books", "Toys & Games", "Health & Beauty", "Other"
        )
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
        // Get current location
        btnGetLocation.setOnClickListener {
            getCurrentLocation()
        }

        // Pick images
        btnPickImages.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        // Create listing (Preview)
        btnCreateListing.setOnClickListener {
            if (validateInputs()) {
                showPreview()
            }
        }

        // Negotiable switch
        switchNegotiable.setOnCheckedChangeListener { _, isChecked ->
            isNegotiable = isChecked
        }

        // Preview buttons
        btnBackToEdit.setOnClickListener {
            showForm()
        }

        btnConfirmPost.setOnClickListener {
            createListing()
        }
    }

    private fun setupRecyclerViews() {
        // Selected images recyclerview (horizontal)
        rvSelectedImages.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Preview images recyclerview (horizontal)
        previewImagesRv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }

    // 🆕 Location functionality
    private fun getCurrentLocation() {
        if (LocationHelper.hasLocationPermission(this)) {
            btnGetLocation.isEnabled = false
            btnGetLocation.text = "Getting location..."

            LocationHelper.getCurrentLocation(
                context = this,
                onSuccess = { locationData ->
                    etLocation.setText(locationData.address)
                    currentLatitude = locationData.latitude
                    currentLongitude = locationData.longitude

                    btnGetLocation.isEnabled = true
                    btnGetLocation.text = getString(R.string.use_current_location)

                    Toast.makeText(this, "Location updated!", Toast.LENGTH_SHORT).show()
                },
                onFailure = { errorMessage ->
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                    etLocation.setText("Ho Chi Minh City, Vietnam")

                    btnGetLocation.isEnabled = true
                    btnGetLocation.text = getString(R.string.use_current_location)
                }
            )
        } else {
            PermissionHelper.requestLocationPermissions(this)
        }
    }

    // 🆕 Permission handling
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        PermissionHelper.handleLocationPermissionResult(
            requestCode, permissions, grantResults,
            onGranted = { getCurrentLocation() },
            onDenied = {
                PermissionHelper.showLocationPermissionRationale(this, {
                    PermissionHelper.requestLocationPermissions(this)
                })
            },
            onPermanentlyDenied = {
                PermissionHelper.showPermissionSettingsDialog(this, "Location")
            }
        )
    }

    private fun updateImageCount() {
        tvImageCount.text = "${selectedImageUris.size}/10"
        rvSelectedImages.visibility = if (selectedImageUris.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateImageRecyclerView() {
        // Create simple adapter for selected images
        // You can implement a proper adapter here
        updateImageCount()
    }

    private fun validateInputs(): Boolean {
        when {
            etTitle.text.toString().trim().isEmpty() -> {
                etTitle.error = "Title is required"
                etTitle.requestFocus()
                return false
            }
            etDescription.text.toString().trim().isEmpty() -> {
                etDescription.error = "Description is required"
                etDescription.requestFocus()
                return false
            }
            etPrice.text.toString().trim().isEmpty() -> {
                etPrice.error = "Price is required"
                etPrice.requestFocus()
                return false
            }
            etPrice.text.toString().trim().toDoubleOrNull() == null -> {
                etPrice.error = "Please enter a valid price"
                etPrice.requestFocus()
                return false
            }
            etLocation.text.toString().trim().isEmpty() -> {
                etLocation.error = "Location is required"
                etLocation.requestFocus()
                return false
            }
            spCategory.selectedItemPosition == 0 -> {
                Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
                return false
            }
            spCondition.selectedItemPosition == 0 -> {
                Toast.makeText(this, "Please select a condition", Toast.LENGTH_SHORT).show()
                return false
            }
            selectedImageUris.isEmpty() -> {
                Toast.makeText(this, "Please select at least one image", Toast.LENGTH_SHORT).show()
                return false
            }
            else -> return true
        }
    }

    private fun showPreview() {
        // Update preview fields
        previewTitle.text = etTitle.text.toString().trim()
        previewPrice.text = formatPrice(etPrice.text.toString().trim().toDouble())
        previewCategory.text = spCategory.selectedItem.toString()
        previewCondition.text = spCondition.selectedItem.toString()
        previewDescription.text = etDescription.text.toString().trim()
        previewLocation.text = etLocation.text.toString().trim()
        previewNegotiable.text = if (isNegotiable) "Price is negotiable" else "Fixed price"

        // Show preview layout
        mainFormLayout.visibility = View.GONE
        previewLayout.visibility = View.VISIBLE
    }

    private fun showForm() {
        previewLayout.visibility = View.GONE
        mainFormLayout.visibility = View.VISIBLE
    }

    private fun formatPrice(price: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        return "${formatter.format(price)} ₫"
    }

    private fun createListing() {
        showPreviewLoading(true)

        // Upload images first
        uploadImages { imageUrls ->
            if (imageUrls.isNotEmpty()) {
                saveListing(imageUrls)
            } else {
                showPreviewLoading(false)
                Toast.makeText(this, "Failed to upload images. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadImages(callback: (List<String>) -> Unit) {
        if (selectedImageUris.isEmpty()) {
            callback(emptyList())
            return
        }

        val uploadedUrls = mutableListOf<String>()
        var uploadCount = 0

        selectedImageUris.forEach { uri ->
            val fileName = "listings/${UUID.randomUUID()}.jpg"
            val storageRef = FirebaseStorage.getInstance().reference.child(fileName)

            storageRef.putFile(uri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        uploadedUrls.add(downloadUri.toString())
                        uploadCount++

                        if (uploadCount == selectedImageUris.size) {
                            callback(uploadedUrls)
                        }
                    }
                }
                .addOnFailureListener {
                    uploadCount++
                    if (uploadCount == selectedImageUris.size) {
                        callback(uploadedUrls)
                    }
                }
        }
    }

    private fun saveListing(imageUrls: List<String>) {
        val currentUser = FirebaseAuth.getInstance().currentUser!!

        val listing = hashMapOf(
            "title" to etTitle.text.toString().trim(),
            "description" to etDescription.text.toString().trim(),
            "price" to etPrice.text.toString().trim().toDouble(),
            "currency" to "VND",
            "category" to spCategory.selectedItem.toString(),
            "condition" to spCondition.selectedItem.toString(),
            "location" to etLocation.text.toString().trim(),
            "latitude" to currentLatitude,
            "longitude" to currentLongitude,
            "imageUrl" to imageUrls.firstOrNull().orEmpty(),
            "imageUrls" to imageUrls,
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
                showPreviewLoading(false)
                Toast.makeText(this, "Listing created successfully!", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                showPreviewLoading(false)
                Toast.makeText(this, "Failed to create listing: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showPreviewLoading(show: Boolean) {
        previewProgressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnConfirmPost.isEnabled = !show
        btnBackToEdit.isEnabled = !show
    }
}