package com.example.tradeup.listing

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tradeup.R
import com.example.tradeup.data.model.Categories
import com.example.tradeup.data.model.ItemCondition
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class AddListingActivity : AppCompatActivity() {

    // UI Components - matching your XML layout
    private lateinit var etTitle: TextInputEditText
    private lateinit var etPrice: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var etLocation: TextInputEditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerCondition: Spinner
    private lateinit var switchNegotiable: Switch
    private lateinit var btnGetLocation: MaterialButton
    private lateinit var btnChoosePhotos: MaterialButton
    private lateinit var btnPreview: Button
    private lateinit var recyclerImages: RecyclerView
    private lateinit var progressBar: ProgressBar

    // Data
    private val selectedImages = mutableListOf<Uri>()
    private lateinit var imagesAdapter: AddListingImagesAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null

    companion object {
        private const val MAX_IMAGES = 10 // FR-2.1.4: Up to 10 images
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_listing)

        setupToolbar()
        setupViews()
        setupSpinners()
        setupImagePicker()
        setupLocationServices()
        setupClickListeners()
    }

    private fun setupToolbar() {
        supportActionBar?.apply {
            title = "Create New Listing"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupViews() {
        // Initialize all UI components using IDs from your XML
        etTitle = findViewById(R.id.etTitle)
        etPrice = findViewById(R.id.etPrice)
        etDescription = findViewById(R.id.etDescription)
        etLocation = findViewById(R.id.etLocation)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        spinnerCondition = findViewById(R.id.spinnerCondition)
        switchNegotiable = findViewById(R.id.switchNegotiable)
        btnGetLocation = findViewById(R.id.btnGetLocation)
        btnChoosePhotos = findViewById(R.id.btnChoosePhotos)
        btnPreview = findViewById(R.id.btnPreview)
        recyclerImages = findViewById(R.id.recyclerImages)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupSpinners() {
        // FR-2.1.1: Category dropdown setup
        val categoryAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            listOf("Select Category") + Categories.ALL
        )
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = categoryAdapter

        // FR-2.1.1: Condition dropdown setup
        val conditionList = listOf(
            "Select Condition",
            ItemCondition.NEW,
            ItemCondition.LIKE_NEW,
            ItemCondition.GOOD,
            ItemCondition.FAIR,
            ItemCondition.POOR
        )

        val conditionAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            conditionList
        )
        conditionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCondition.adapter = conditionAdapter
    }

    private fun setupImagePicker() {
        // FR-2.1.4: Setup image selection for up to 10 images
        imagesAdapter = AddListingImagesAdapter { position ->
            // Remove image when user clicks the 'X' button
            selectedImages.removeAt(position)
            imagesAdapter.updateImages(selectedImages)
            updateImageCounter()

            // Show/hide RecyclerView based on image count
            recyclerImages.visibility = if (selectedImages.isEmpty()) View.GONE else View.VISIBLE
        }

        recyclerImages.layoutManager = GridLayoutManager(this, 3)
        recyclerImages.adapter = imagesAdapter
    }

    private fun setupLocationServices() {
        // FR-2.1.3: Initialize GPS location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun setupClickListeners() {
        // FR-2.1.3: Get current location button
        btnGetLocation.setOnClickListener {
            requestLocationPermissionAndGetLocation()
        }

        // FR-2.1.4: Choose photos button
        btnChoosePhotos.setOnClickListener {
            openImagePicker()
        }

        // FR-2.1.5: Preview listing button
        btnPreview.setOnClickListener {
            if (validateForm()) {
                openPreview()
            }
        }
    }

    // FR-2.1.3: Location permission and GPS functionality
    private fun requestLocationPermissionAndGetLocation() {
        when {
            ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                getCurrentLocation()
            }
            else -> {
                // Request permission
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun getCurrentLocation() {
        showLoading(true)

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                showLoading(false)
                if (location != null) {
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude

                    // Format location nicely
                    val locationText = String.format(
                        "%.4f, %.4f",
                        location.latitude,
                        location.longitude
                    )
                    etLocation.setText(locationText)

                    Toast.makeText(this, "📍 Location updated!", Toast.LENGTH_SHORT).show()

                    // Change button text to indicate success
                    btnGetLocation.text = "✅ GPS"
                } else {
                    Toast.makeText(this, "Unable to get location. Try again.", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { exception ->
                showLoading(false)
                Toast.makeText(this, "Location error: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        } catch (securityException: SecurityException) {
            showLoading(false)
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // FR-2.1.4: Image picker for up to 10 images
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val availableSlots = MAX_IMAGES - selectedImages.size
            val newImages = uris.take(availableSlots)

            selectedImages.addAll(newImages)
            imagesAdapter.updateImages(selectedImages)
            updateImageCounter()

            // Show RecyclerView when images are added
            recyclerImages.visibility = View.VISIBLE

            if (uris.size > availableSlots) {
                Toast.makeText(
                    this,
                    "Only $availableSlots more images could be added",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun openImagePicker() {
        if (selectedImages.size >= MAX_IMAGES) {
            Toast.makeText(this, "Maximum $MAX_IMAGES images allowed", Toast.LENGTH_SHORT).show()
            return
        }
        imagePickerLauncher.launch("image/*")
    }

    private fun updateImageCounter() {
        // Update button text to show current count
        btnChoosePhotos.text = "Choose Photos (${selectedImages.size}/$MAX_IMAGES)"

        // Change button style based on image count
        if (selectedImages.isEmpty()) {
            btnChoosePhotos.setIconResource(R.drawable.ic_add_photo)
        } else {
            btnChoosePhotos.setIconResource(R.drawable.ic_check)
        }
    }

    // FR-2.1.1: Form validation - ensure all required fields are filled
    private fun validateForm(): Boolean {
        var isValid = true

        // Clear previous errors
        etTitle.error = null
        etPrice.error = null
        etDescription.error = null
        etLocation.error = null

        // Check title
        if (etTitle.text.toString().trim().isEmpty()) {
            etTitle.error = "Title is required"
            etTitle.requestFocus()
            isValid = false
        }

        // Check price
        val priceText = etPrice.text.toString().trim()
        if (priceText.isEmpty()) {
            etPrice.error = "Price is required"
            if (isValid) etPrice.requestFocus()
            isValid = false
        } else {
            val price = priceText.toDoubleOrNull()
            if (price == null || price <= 0) {
                etPrice.error = "Please enter a valid price"
                if (isValid) etPrice.requestFocus()
                isValid = false
            }
        }

        // Check description
        if (etDescription.text.toString().trim().isEmpty()) {
            etDescription.error = "Description is required"
            if (isValid) etDescription.requestFocus()
            isValid = false
        }

        // Check location
        if (etLocation.text.toString().trim().isEmpty()) {
            etLocation.error = "Location is required"
            if (isValid) etLocation.requestFocus()
            isValid = false
        }

        // Check category
        if (spinnerCategory.selectedItemPosition == 0) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        // Check condition
        if (spinnerCondition.selectedItemPosition == 0) {
            Toast.makeText(this, "Please select item condition", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        // FR-2.1.1: At least 1 photo required
        if (selectedImages.isEmpty()) {
            Toast.makeText(this, "At least 1 photo is required", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    // FR-2.1.5: Open preview screen before posting
    private fun openPreview() {
        val intent = Intent(this, PreviewListingActivity::class.java).apply {
            // Required fields
            putExtra("title", etTitle.text.toString().trim())
            putExtra("price", etPrice.text.toString().trim().toDoubleOrNull() ?: 0.0)
            putExtra("category", spinnerCategory.selectedItem.toString())
            putExtra("condition", spinnerCondition.selectedItem.toString())
            putExtra("description", etDescription.text.toString().trim())
            putExtra("location", etLocation.text.toString().trim())

            // FR-2.1.2: Optional fields
            putExtra("negotiable", switchNegotiable.isChecked)

            // FR-2.1.3: GPS coordinates
            putExtra("latitude", currentLatitude ?: 0.0)
            putExtra("longitude", currentLongitude ?: 0.0)

            // FR-2.1.4: Images
            putStringArrayListExtra("imageUris", ArrayList(selectedImages.map { it.toString() }))
        }
        startActivity(intent)
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE

        // Disable buttons during loading
        btnGetLocation.isEnabled = !show
        btnChoosePhotos.isEnabled = !show
        btnPreview.isEnabled = !show

        // Show loading text on GPS button
        if (show) {
            btnGetLocation.text = "..."
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                btnGetLocation.text = "GPS (Permission Denied)"
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    // Handle back from preview - refresh image counter
    override fun onResume() {
        super.onResume()
        updateImageCounter()
    }
}