package com.example.tradeup.listing

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tradeup.R
import com.example.tradeup.utils.CloudinaryHelper
import com.example.tradeup.utils.LocationHelper
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class CreateListingActivity : AppCompatActivity() {

    // UI Components - Main Form
    private lateinit var etTitle: EditText
    private lateinit var etPrice: EditText
    private lateinit var etDescription: EditText
    private lateinit var etLocation: EditText
    private lateinit var spCategory: Spinner
    private lateinit var spCondition: Spinner
    private lateinit var switchNegotiable: SwitchMaterial
    private lateinit var btnGetLocation: Button
    private lateinit var btnPickImages: Button
    private lateinit var btnCreateListing: Button
    private lateinit var rvSelectedImages: RecyclerView
    private lateinit var tvImageCount: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var mainFormLayout: View

    // UI Components - Preview Screen
    private lateinit var previewLayout: View
    private lateinit var previewTitle: TextView
    private lateinit var previewPrice: TextView
    private lateinit var previewCategory: TextView
    private lateinit var previewCondition: TextView
    private lateinit var previewDescription: TextView
    private lateinit var previewLocation: TextView
    private lateinit var previewNegotiable: TextView
    private lateinit var previewImagesRv: RecyclerView
    private lateinit var btnBackToEdit: Button
    private lateinit var btnConfirmPost: Button
    private lateinit var previewProgressBar: ProgressBar

    // Data
    private val selectedImages = mutableListOf<Uri>()
    private lateinit var selectedImageAdapter: SelectedImageAdapter
    private lateinit var previewImageAdapter: PreviewImageAdapter
    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null

    // Constants
    private val MAX_IMAGES = 10
    private val PICK_IMAGES_REQUEST = 1
    private val LOCATION_PERMISSION_REQUEST = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_listing)

        initViews()
        setupSpinners()
        setupImageRecyclerView()
        setupPreviewRecyclerView()
        setupClickListeners()
        setupPriceFormatting()

        // Initially show main form, hide preview
        showMainForm()
    }

    private fun initViews() {
        // Main form views
        mainFormLayout = findViewById(R.id.mainFormLayout)
        etTitle = findViewById(R.id.etTitle)
        etPrice = findViewById(R.id.etPrice)
        etDescription = findViewById(R.id.etDescription)
        etLocation = findViewById(R.id.etLocation)
        spCategory = findViewById(R.id.spCategory)
        spCondition = findViewById(R.id.spCondition)
        switchNegotiable = findViewById(R.id.switchNegotiable)
        btnGetLocation = findViewById(R.id.btnGetLocation)
        btnPickImages = findViewById(R.id.btnPickImages)
        btnCreateListing = findViewById(R.id.btnCreateListing)
        rvSelectedImages = findViewById(R.id.rvSelectedImages)
        tvImageCount = findViewById(R.id.tvImageCount)
        progressBar = findViewById(R.id.progressBar)

        // Preview views
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

        updateImageCount()
    }

    private fun setupSpinners() {
        // Category Spinner
        val categories = arrayOf("Đồ điện tử", "Thời trang", "Đồ gia dụng", "Xe cộ", "Sách", "Thể thao", "Khác")
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spCategory.adapter = categoryAdapter

        // Condition Spinner
        val conditions = arrayOf("Mới", "Như mới", "Tốt", "Khá tốt", "Cần sửa chữa")
        val conditionAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, conditions)
        spCondition.adapter = conditionAdapter
    }

    private fun setupImageRecyclerView() {
        selectedImageAdapter = SelectedImageAdapter(selectedImages) { position ->
            selectedImages.removeAt(position)
            selectedImageAdapter.notifyItemRemoved(position)
            updateImageCount()
        }

        rvSelectedImages.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvSelectedImages.adapter = selectedImageAdapter
    }

    private fun setupPreviewRecyclerView() {
        previewImageAdapter = PreviewImageAdapter(selectedImages)
        previewImagesRv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        previewImagesRv.adapter = previewImageAdapter
    }

    private fun setupClickListeners() {
        btnGetLocation.setOnClickListener {
            getCurrentLocation()
        }

        btnPickImages.setOnClickListener {
            if (selectedImages.size >= MAX_IMAGES) {
                Toast.makeText(this, "Maximum $MAX_IMAGES images allowed", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            pickImages()
        }

        // Single button to show preview
        btnCreateListing.setOnClickListener {
            if (validateInput()) {
                showPreviewScreen()
            }
        }

        // Preview screen buttons
        btnBackToEdit.setOnClickListener {
            showMainForm()
        }

        btnConfirmPost.setOnClickListener {
            submitListing()
        }
    }

    private fun setupPriceFormatting() {
        etPrice.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return

                isFormatting = true
                val text = s.toString()

                // Remove any existing formatting
                val cleanText = text.replace("[^\\d]".toRegex(), "")

                if (cleanText.isNotEmpty()) {
                    // Format with thousand separators
                    val number = cleanText.toLongOrNull()
                    if (number != null) {
                        val formatted = String.format("%,d", number)
                        etPrice.setText(formatted)
                        etPrice.setSelection(formatted.length)
                    }
                }

                isFormatting = false
            }
        })
    }

    private fun showMainForm() {
        mainFormLayout.visibility = View.VISIBLE
        previewLayout.visibility = View.GONE

        supportActionBar?.title = "Create Listing"
    }

    private fun showPreviewScreen() {
        populatePreviewData()

        mainFormLayout.visibility = View.GONE
        previewLayout.visibility = View.VISIBLE

        supportActionBar?.title = "Preview Listing"
    }

    private fun populatePreviewData() {
        previewTitle.text = etTitle.text.toString().trim()

        // Format price with VND currency
        val priceText = etPrice.text.toString().trim()
        val cleanPrice = priceText.replace("[^\\d]".toRegex(), "")
        val formattedPrice = if (cleanPrice.isNotEmpty()) {
            val number = cleanPrice.toLongOrNull()
            if (number != null) String.format("%,d VND", number) else "$priceText VND"
        } else "0 VND"

        previewPrice.text = formattedPrice
        previewCategory.text = "Category: ${spCategory.selectedItem}"
        previewCondition.text = "Condition: ${spCondition.selectedItem}"
        previewDescription.text = etDescription.text.toString().trim()
        previewLocation.text = "📍 ${etLocation.text.toString().trim()}"
        previewNegotiable.text = if (switchNegotiable.isChecked) "💰 Price is negotiable" else "💰 Fixed price"

        // Update preview images
        previewImageAdapter.notifyDataSetChanged()
    }

    private fun getCurrentLocation() {
        if (!LocationHelper.hasLocationPermission(this)) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
            return
        }

        btnGetLocation.isEnabled = false
        btnGetLocation.text = "Getting location..."

        LocationHelper.getCurrentLocation(
            this,
            onSuccess = { locationData ->
                currentLatitude = locationData.latitude
                currentLongitude = locationData.longitude
                etLocation.setText(locationData.address)

                btnGetLocation.isEnabled = true
                btnGetLocation.text = getString(R.string.use_current_location)
                Toast.makeText(this, "Location updated!", Toast.LENGTH_SHORT).show()
            },
            onFailure = { error ->
                btnGetLocation.isEnabled = true
                btnGetLocation.text = getString(R.string.use_current_location)
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun pickImages() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(Intent.createChooser(intent, "Select Images"), PICK_IMAGES_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGES_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.let { intent ->
                if (intent.clipData != null) {
                    val clipData = intent.clipData!!
                    val newImages = mutableListOf<Uri>()

                    for (i in 0 until clipData.itemCount) {
                        if (selectedImages.size + newImages.size >= MAX_IMAGES) break
                        newImages.add(clipData.getItemAt(i).uri)
                    }

                    selectedImages.addAll(newImages)
                    selectedImageAdapter.notifyDataSetChanged()
                    updateImageCount()
                } else if (intent.data != null) {
                    if (selectedImages.size < MAX_IMAGES) {
                        selectedImages.add(intent.data!!)
                        selectedImageAdapter.notifyItemInserted(selectedImages.size - 1)
                        updateImageCount()
                    }
                }
            }
        }
    }

    private fun updateImageCount() {
        tvImageCount.text = "${selectedImages.size}/$MAX_IMAGES"
        btnPickImages.text = if (selectedImages.isEmpty()) {
            getString(R.string.choose_photos)
        } else {
            getString(R.string.add_photos)
        }

        rvSelectedImages.visibility = if (selectedImages.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun validateInput(): Boolean {
        val title = etTitle.text.toString().trim()
        val price = etPrice.text.toString().trim().replace("[^\\d]".toRegex(), "")
        val description = etDescription.text.toString().trim()
        val location = etLocation.text.toString().trim()

        when {
            title.isEmpty() -> {
                etTitle.error = "Title is required"
                etTitle.requestFocus()
                return false
            }
            title.length < 5 -> {
                etTitle.error = "Title must be at least 5 characters"
                etTitle.requestFocus()
                return false
            }
            price.isEmpty() -> {
                etPrice.error = "Price is required"
                etPrice.requestFocus()
                return false
            }
            price.toDoubleOrNull() == null || price.toDouble() <= 0 -> {
                etPrice.error = "Please enter a valid price"
                etPrice.requestFocus()
                return false
            }
            description.isEmpty() -> {
                etDescription.error = "Description is required"
                etDescription.requestFocus()
                return false
            }
            description.length < 10 -> {
                etDescription.error = "Description must be at least 10 characters"
                etDescription.requestFocus()
                return false
            }
            location.isEmpty() -> {
                etLocation.error = "Location is required"
                etLocation.requestFocus()
                return false
            }
            selectedImages.isEmpty() -> {
                Toast.makeText(this, "Please add at least one image", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }

    private fun submitListing() {
        showPreviewLoading(true)

        uploadImages { imageUrls ->
            if (imageUrls.isNotEmpty()) {
                createListing(imageUrls)
            } else {
                showPreviewLoading(false)
                Toast.makeText(this, "Failed to upload images", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadImages(onComplete: (List<String>) -> Unit) {
        val imageUrls = mutableListOf<String>()
        var uploadedCount = 0

        if (selectedImages.isEmpty()) {
            onComplete(emptyList())
            return
        }

        selectedImages.forEach { uri ->
            CloudinaryHelper.uploadImage(uri, this) { success, url ->
                uploadedCount++
                if (success && url != null) {
                    imageUrls.add(url)
                }

                if (uploadedCount == selectedImages.size) {
                    onComplete(imageUrls)
                }
            }
        }
    }

    private fun createListing(imageUrls: List<String>) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            showPreviewLoading(false)
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        val cleanPrice = etPrice.text.toString().replace("[^\\d]".toRegex(), "")

        val listing = hashMapOf(
            "title" to etTitle.text.toString().trim(),
            "description" to etDescription.text.toString().trim(),
            "price" to cleanPrice.toDouble(),
            "category" to spCategory.selectedItem.toString(),
            "condition" to spCondition.selectedItem.toString(),
            "location" to etLocation.text.toString().trim(),
            "latitude" to currentLatitude,
            "longitude" to currentLongitude,
            "negotiable" to switchNegotiable.isChecked,
            "images" to imageUrls,
            "ownerUid" to currentUser.uid,
            "ownerName" to (currentUser.displayName ?: "Anonymous"),
            "status" to "Available",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
            "views" to 0,
            "interactions" to 0
        )

        FirebaseFirestore.getInstance()
            .collection("listings")
            .add(listing)
            .addOnSuccessListener {
                showPreviewLoading(false)
                Toast.makeText(this, "🎉 Listing posted successfully!", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                showPreviewLoading(false)
                Toast.makeText(this, "Failed to post listing: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showPreviewLoading(show: Boolean) {
        previewProgressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnConfirmPost.isEnabled = !show
        btnBackToEdit.isEnabled = !show

        btnConfirmPost.text = if (show) "Posting..." else getString(R.string.confirm_and_post)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onBackPressed() {
        if (previewLayout.visibility == View.VISIBLE) {
            showMainForm()
        } else {
            super.onBackPressed()
        }
    }
}