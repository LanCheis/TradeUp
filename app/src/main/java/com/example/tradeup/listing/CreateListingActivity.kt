package com.example.tradeup.listing

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.tradeup.R
import com.example.tradeup.data.model.Listing
import com.example.tradeup.data.remote.UserRepository
import com.example.tradeup.utils.CloudinaryHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class CreateListingActivity : AppCompatActivity() {

    private lateinit var etTitle: EditText
    private lateinit var etPrice: EditText
    private lateinit var etDescription: EditText
    private lateinit var spCategory: Spinner
    private lateinit var spCondition: Spinner
    private lateinit var rvSelectedImages: RecyclerView
    private lateinit var btnPickImages: Button
    private lateinit var btnTakePhoto: Button
    private lateinit var btnSubmit: Button
    private lateinit var tvImageCount: TextView
    private lateinit var progressBar: ProgressBar

    // ✅ NEW: Multiple images support
    private val selectedImages = mutableListOf<Uri>()
    private lateinit var selectedImageAdapter: SelectedImageAdapter
    private val MAX_IMAGES = 5
    private val PICK_IMAGES_REQUEST = 1
    private val TAKE_PHOTO_REQUEST = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_listing)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "🛍️ Đăng bán sản phẩm"

        initViews()
        setupImageRecyclerView()
        setupSpinners()
        setupClickListeners()
        setupTextWatchers()
    }

    private fun initViews() {
        etTitle = findViewById(R.id.etTitle)
        etPrice = findViewById(R.id.etPrice)
        etDescription = findViewById(R.id.etDescription)
        spCategory = findViewById(R.id.spCategory)
        spCondition = findViewById(R.id.spCondition)
        rvSelectedImages = findViewById(R.id.rvSelectedImages)
        btnPickImages = findViewById(R.id.btnPickImages)
        btnTakePhoto = findViewById(R.id.btnTakePhoto)
        btnSubmit = findViewById(R.id.btnSubmit)
        tvImageCount = findViewById(R.id.tvImageCount)
        progressBar = findViewById(R.id.progressBar)

        progressBar.visibility = android.view.View.GONE
        btnSubmit.isEnabled = false
        updateImageCount()
    }

    // ✅ NEW: Setup image RecyclerView
    private fun setupImageRecyclerView() {
        selectedImageAdapter = SelectedImageAdapter(selectedImages) { position ->
            removeImage(position)
        }
        rvSelectedImages.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvSelectedImages.adapter = selectedImageAdapter
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateFieldColors()
                updateSubmitButton()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        etTitle.addTextChangedListener(textWatcher)
        etPrice.addTextChangedListener(textWatcher)
        etDescription.addTextChangedListener(textWatcher)
    }

    private fun updateFieldColors() {
        // Title field
        if (etTitle.text.toString().trim().isNotEmpty()) {
            etTitle.setBackgroundResource(R.drawable.edittext_filled)
            etTitle.setTextColor(ContextCompat.getColor(this, R.color.black))
        } else {
            etTitle.setBackgroundResource(R.drawable.edittext_background)
            etTitle.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        }

        // Price field
        if (etPrice.text.toString().trim().isNotEmpty()) {
            etPrice.setBackgroundResource(R.drawable.edittext_filled)
            etPrice.setTextColor(ContextCompat.getColor(this, R.color.black))
        } else {
            etPrice.setBackgroundResource(R.drawable.edittext_background)
        }

        // Description field
        if (etDescription.text.toString().trim().isNotEmpty()) {
            etDescription.setBackgroundResource(R.drawable.edittext_filled)
            etDescription.setTextColor(ContextCompat.getColor(this, R.color.black))
        } else {
            etDescription.setBackgroundResource(R.drawable.edittext_background)
        }
    }

    private fun updateSubmitButton() {
        val isComplete = etTitle.text.toString().trim().isNotEmpty() &&
                etPrice.text.toString().trim().isNotEmpty() &&
                etDescription.text.toString().trim().isNotEmpty() &&
                selectedImages.isNotEmpty()

        btnSubmit.isEnabled = isComplete
        btnSubmit.setBackgroundColor(
            if (isComplete)
                ContextCompat.getColor(this, R.color.blue_500)
            else
                ContextCompat.getColor(this, android.R.color.darker_gray)
        )
    }

    private fun setupSpinners() {
        val categories = arrayOf("Chọn danh mục", "📱 Đồ điện tử", "👕 Thời trang", "🏠 Đồ gia dụng", "📚 Sách", "⚽ Thể thao", "🎯 Khác")
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCategory.adapter = categoryAdapter

        val conditions = arrayOf("Chọn tình trạng", "✨ Như mới", "👍 Tốt", "👌 Khá tốt", "📦 Cũ")
        val conditionAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, conditions)
        conditionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCondition.adapter = conditionAdapter
    }

    private fun setupClickListeners() {
        // ✅ NEW: Multiple image selection
        btnPickImages.setOnClickListener {
            if (selectedImages.size >= MAX_IMAGES) {
                Toast.makeText(this, "Tối đa $MAX_IMAGES ảnh", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            startActivityForResult(Intent.createChooser(intent, "Chọn ảnh"), PICK_IMAGES_REQUEST)
        }

        btnTakePhoto.setOnClickListener {
            // TODO: Implement camera functionality
            Toast.makeText(this, "Chức năng chụp ảnh sẽ được thêm vào sau", Toast.LENGTH_SHORT).show()
        }

        btnSubmit.setOnClickListener {
            if (validateInput()) {
                uploadImagesAndCreateListing()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGES_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.let { handleImageSelection(it) }
        }
    }

    // ✅ NEW: Handle multiple image selection
    private fun handleImageSelection(data: Intent) {
        if (data.clipData != null) {
            // Multiple images selected
            val clipData = data.clipData!!
            val itemCount = clipData.itemCount

            for (i in 0 until itemCount) {
                if (selectedImages.size >= MAX_IMAGES) break

                val imageUri = clipData.getItemAt(i).uri
                selectedImages.add(imageUri)
            }
        } else if (data.data != null) {
            // Single image selected
            if (selectedImages.size < MAX_IMAGES) {
                selectedImages.add(data.data!!)
            }
        }

        selectedImageAdapter.notifyDataSetChanged()
        updateImageCount()
        updateSubmitButton()
    }

    // ✅ NEW: Remove image from selection
    private fun removeImage(position: Int) {
        selectedImages.removeAt(position)
        selectedImageAdapter.notifyItemRemoved(position)
        updateImageCount()
        updateSubmitButton()
    }

    // ✅ NEW: Update image count display
    private fun updateImageCount() {
        tvImageCount.text = "📊 Đã chọn: ${selectedImages.size}/$MAX_IMAGES ảnh"
    }

    private fun validateInput(): Boolean {
        if (etTitle.text.toString().trim().isEmpty()) {
            etTitle.error = "Vui lòng nhập tiêu đề"
            return false
        }

        if (etDescription.text.toString().trim().isEmpty()) {
            etDescription.error = "Vui lòng nhập mô tả"
            return false
        }

        val priceText = etPrice.text.toString().trim()
        if (priceText.isEmpty()) {
            etPrice.error = "Vui lòng nhập giá"
            return false
        }

        try {
            val price = priceText.toDouble()
            if (price <= 0) {
                etPrice.error = "Giá phải lớn hơn 0"
                return false
            }
        } catch (e: NumberFormatException) {
            etPrice.error = "Giá không hợp lệ"
            return false
        }

        if (spCategory.selectedItemPosition == 0) {
            Toast.makeText(this, "Vui lòng chọn danh mục", Toast.LENGTH_SHORT).show()
            return false
        }

        if (spCondition.selectedItemPosition == 0) {
            Toast.makeText(this, "Vui lòng chọn tình trạng", Toast.LENGTH_SHORT).show()
            return false
        }

        if (selectedImages.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất 1 ảnh", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    // ✅ NEW: Upload multiple images and create listing
    private fun uploadImagesAndCreateListing() {
        showLoading(true)
        val uploadedUrls = mutableListOf<String>()
        var uploadCount = 0

        selectedImages.forEachIndexed { index, imageUri ->
            CloudinaryHelper.uploadListingImage(
                context = this,
                imageUri = imageUri,
                onSuccess = { imageUrl ->
                    uploadedUrls.add(imageUrl)
                    uploadCount++

                    val progress = ((uploadCount * 100) / selectedImages.size)
                    btnSubmit.text = "Đang tải ảnh... $progress%"

                    if (uploadCount == selectedImages.size) {
                        // All images uploaded successfully
                        createListing(uploadedUrls)
                    }
                },
                onFailure = { error ->
                    Log.e("CreateListing", "Image upload failed: $error")
                    Toast.makeText(this, "❌ Lỗi tải ảnh ${index + 1}: $error", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                }
            )
        }
    }

    // ✅ NEW: Create listing with multiple images
    private fun createListing(imageUrls: List<String>) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show()
            showLoading(false)
            return
        }

        btnSubmit.text = "Đang lưu thông tin..."

        UserRepository.getUserProfile(currentUser.uid) { userProfile ->
            val price = etPrice.text.toString().toDouble()

            val listing = Listing(
                id = UUID.randomUUID().toString(),
                title = etTitle.text.toString().trim(),
                description = etDescription.text.toString().trim(),
                category = spCategory.selectedItem.toString(),
                condition = spCondition.selectedItem.toString(),
                price = price,
                imageUrl = imageUrls.firstOrNull() ?: "", // Main image for backward compatibility
                imageUrls = imageUrls, // ✅ NEW: All images
                ownerUid = currentUser.uid,
                ownerName = userProfile?.name ?: currentUser.displayName ?: "Anonymous User",
                ownerAvatar = userProfile?.profileImageUrl ?: currentUser.photoUrl?.toString() ?: "",
                location = userProfile?.address ?: "Ho Chi Minh City",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                isNegotiable = true
            )

            FirebaseFirestore.getInstance()
                .collection("listings")
                .document(listing.id)
                .set(listing)
                .addOnSuccessListener {
                    Toast.makeText(this, "🎉 Đăng sản phẩm thành công!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "❌ Lỗi: ${exception.message}", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                }
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        btnSubmit.isEnabled = !show
        btnPickImages.isEnabled = !show
        btnTakePhoto.isEnabled = !show
        etTitle.isEnabled = !show
        etPrice.isEnabled = !show
        etDescription.isEnabled = !show

        if (show) {
            btnSubmit.text = "Đang xử lý..."
        } else {
            btnSubmit.text = "🚀 Đăng bài"
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}