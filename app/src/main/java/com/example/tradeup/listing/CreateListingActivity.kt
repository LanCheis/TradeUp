package com.example.tradeup.listing

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.tradeup.R
import com.example.tradeup.data.model.Listing
import com.example.tradeup.utils.CloudinaryHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class CreateListingActivity : AppCompatActivity() {

    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var spCategory: Spinner
    private lateinit var ivListingImage: ImageView
    private lateinit var btnPickImage: Button
    private lateinit var btnSubmit: Button

    private var imageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_listing)

        initViews()
        setupSpinner()
        setupClickListeners()
    }

    private fun initViews() {
        etTitle = findViewById(R.id.etTitle)
        etDescription = findViewById(R.id.etDescription)
        spCategory = findViewById(R.id.spCategory)
        ivListingImage = findViewById(R.id.ivListingImage)
        btnPickImage = findViewById(R.id.btnPickImage)
        btnSubmit = findViewById(R.id.btnSubmit)

        // Set default placeholder image
        ivListingImage.setImageResource(R.drawable.ic_image_placeholder)
    }

    private fun setupSpinner() {
        val categories = arrayOf("Đồ điện tử", "Thời trang", "Đồ gia dụng", "Khác")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spCategory.adapter = adapter
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

        if (imageUri == null) {
            Toast.makeText(this, "Vui lòng chọn ảnh sản phẩm", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    // ✅ FIXED VERSION - This is Step 2
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            imageUri?.let { uri ->
                // ✅ FIXED: Using .error() instead of .placeholder()
                Glide.with(this)
                    .load(uri)
                    .error(R.drawable.ic_image_placeholder)
                    .into(ivListingImage)
            }
        }
    }

    private fun uploadImageAndCreateListing() {
        imageUri?.let { uri ->
            btnSubmit.isEnabled = false
            btnSubmit.text = "Đang tải ảnh lên..."

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
                    btnSubmit.text = "Đang tải lên... $progress%"
                }
            )
        }
    }

    private fun createListing(imageUrl: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show()
            resetUploadButton()
            return
        }

        val listing = Listing(
            id = UUID.randomUUID().toString(),
            title = etTitle.text.toString().trim(),
            description = etDescription.text.toString().trim(),
            category = spCategory.selectedItem.toString(),
            imageUrl = imageUrl,
            ownerUid = currentUser.uid,
            price = 0.0,
            condition = "Good",
            location = "Ho Chi Minh City"
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
                resetUploadButton()
            }
    }

    private fun handleUploadError(error: String) {
        Toast.makeText(this, "❌ Tải ảnh thất bại: $error", Toast.LENGTH_LONG).show()
        resetUploadButton()
    }

    private fun resetUploadButton() {
        btnSubmit.isEnabled = true
        btnSubmit.text = "Đăng bài"
    }
}