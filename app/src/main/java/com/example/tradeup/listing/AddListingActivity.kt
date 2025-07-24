package com.example.tradeup.listing

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.tradeup.R
import com.example.tradeup.data.model.*
import com.example.tradeup.data.remote.ListingRepository
import com.example.tradeup.utils.CloudinaryHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class AddListingActivity : AppCompatActivity() {

    // Views
    private lateinit var etTitle: EditText
    private lateinit var etPrice: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerCondition: Spinner
    private lateinit var etDescription: EditText
    private lateinit var etLocation: EditText
    private lateinit var btnGetLocation: Button
    private lateinit var switchNegotiable: Switch
    private lateinit var recyclerImages: androidx.recyclerview.widget.RecyclerView
    private lateinit var btnChoosePhotos: Button
    private lateinit var btnPreview: Button
    private lateinit var progressBar: ProgressBar

    // Data
    private val selectedImages = mutableListOf<Uri>()
    private var currentLocation: Location? = null
    private lateinit var imagesAdapter: SelectedImagesAdapter

    // Dependencies
    private lateinit var auth: FirebaseAuth
    private lateinit var listingRepository: ListingRepository
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Constants
    private companion object {
        const val MAX_IMAGES = 10
        const val LOCATION_PERMISSION_REQUEST = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_listing)

        // Initialize Cloudinary
        CloudinaryHelper.initialize(this)

        initDependencies()
        initViews()
        setupSpinners()
        setupImageAdapter()
        setupClickListeners()
        setupToolbar()
    }

    private fun initDependencies() {
        auth = FirebaseAuth.getInstance()
        listingRepository = ListingRepository()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun initViews() {
        etTitle = findViewById(R.id.etTitle)
        etPrice = findViewById(R.id.etPrice)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        spinnerCondition = findViewById(R.id.spinnerCondition)
        etDescription = findViewById(R.id.etDescription)
        etLocation = findViewById(R.id.etLocation)
        btnGetLocation = findViewById(R.id.btnGetLocation)
        switchNegotiable = findViewById(R.id.switchNegotiable)
        recyclerImages = findViewById(R.id.recyclerImages)
        btnChoosePhotos = findViewById(R.id.btnChoosePhotos)
        btnPreview = findViewById(R.id.btnPreview)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupSpinners() {
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, Categories.ALL)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = categoryAdapter

        val conditions = listOf(
            ItemCondition.NEW,
            ItemCondition.LIKE_NEW,
            ItemCondition.GOOD,
            ItemCondition.FAIR,
            ItemCondition.POOR
        )
        val conditionAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, conditions)
        conditionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCondition.adapter = conditionAdapter
    }

    // Show/hide image preview with smooth animation
    private fun updateImageVisibility() {
        if (selectedImages.isEmpty()) {
            // No images - hide recycler smoothly
            recyclerImages.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    recyclerImages.visibility = View.GONE
                }
        } else {
            // Images exist - show recycler smoothly
            if (recyclerImages.visibility == View.GONE) {
                recyclerImages.visibility = View.VISIBLE
                recyclerImages.alpha = 0f
                recyclerImages.animate()
                    .alpha(1f)
                    .setDuration(200)
            }
        }
    }



    private fun setupImageAdapter() {
        imagesAdapter = SelectedImagesAdapter(selectedImages) { position ->
            selectedImages.removeAt(position)
            imagesAdapter.notifyItemRemoved(position)
            updateImageCounter()
        }

        recyclerImages.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
            this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false
        )
        recyclerImages.adapter = imagesAdapter
    }

    private fun setupClickListeners() {
        btnGetLocation.setOnClickListener {
            if (hasLocationPermission()) {
                getCurrentLocation()
            } else {
                requestLocationPermission()
            }
        }

        btnChoosePhotos.setOnClickListener {
            if (selectedImages.size >= MAX_IMAGES) {
                Toast.makeText(this, "Maximum $MAX_IMAGES images allowed", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            openImagePicker()
        }

        btnPreview.setOnClickListener {
            if (validateForm()) {
                openPreview()
            }
        }
    }

    private fun setupToolbar() {
        supportActionBar?.apply {
            title = getString(R.string.create_new_listing)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST
        )
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        // Double-check permissions before calling location services
        if (!hasLocationPermission()) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                showLoading(false)
                if (location != null) {
                    currentLocation = location
                    etLocation.setText("${location.latitude}, ${location.longitude}")
                    Toast.makeText(this, "Location updated!", Toast.LENGTH_SHORT).show()
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

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val availableSlots = MAX_IMAGES - selectedImages.size
            val newImages = uris.take(availableSlots)

            selectedImages.addAll(newImages)
            imagesAdapter.notifyDataSetChanged()
            updateImageCounter()

            if (uris.size > availableSlots) {
                Toast.makeText(this, "Only $availableSlots more images could be added", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openImagePicker() {
        imagePickerLauncher.launch("image/*")
    }

    private fun updateImageCounter() {
        val text = if (selectedImages.isEmpty()) {
            "📷 Choose Photos (0/10)"
        } else {
            "📷 Choose Photos (${selectedImages.size}/10) ✅"
        }
        btnChoosePhotos.text = text
    }

    private fun validateForm(): Boolean {
        var isValid = true

        if (etTitle.text.toString().trim().isEmpty()) {
            etTitle.error = "Title is required"
            isValid = false
        }

        if (etPrice.text.toString().trim().isEmpty()) {
            etPrice.error = "Price is required"
            isValid = false
        }

        if (etDescription.text.toString().trim().isEmpty()) {
            etDescription.error = "Description is required"
            isValid = false
        }

        if (etLocation.text.toString().trim().isEmpty()) {
            etLocation.error = "Location is required"
            isValid = false
        }

        if (selectedImages.isEmpty()) {
            Toast.makeText(this, "At least 1 photo is required", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    private fun openPreview() {
        val intent = Intent(this, PreviewListingActivity::class.java).apply {
            putExtra("title", etTitle.text.toString().trim())
            putExtra("price", etPrice.text.toString().trim().toDoubleOrNull() ?: 0.0)
            putExtra("category", spinnerCategory.selectedItem.toString())
            putExtra("condition", spinnerCondition.selectedItem.toString())
            putExtra("description", etDescription.text.toString().trim())
            putExtra("location", etLocation.text.toString().trim())
            putExtra("negotiable", switchNegotiable.isChecked)
            putExtra("latitude", currentLocation?.latitude)
            putExtra("longitude", currentLocation?.longitude)
            putStringArrayListExtra("imageUris", ArrayList(selectedImages.map { it.toString() }))
        }
        startActivity(intent)
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnPreview.isEnabled = !show
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Toast.makeText(this, "Location permission needed for GPS feature", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}