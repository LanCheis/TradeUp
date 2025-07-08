package com.example.tradeup.listing

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.tradeup.R
import com.example.tradeup.data.model.Listing
import com.example.tradeup.network.CloudinaryService
import com.example.tradeup.network.CloudinaryUploadResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

class CreateListingActivity : AppCompatActivity() {

    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var spCategory: Spinner
    private lateinit var ivPreview: ImageView
    private lateinit var btnPickImage: Button
    private lateinit var btnSubmit: Button

    private var imageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_listing)

        // Ánh xạ view
        etTitle = findViewById(R.id.etTitle)
        etDescription = findViewById(R.id.etDescription)
        spCategory = findViewById(R.id.spCategory)
        ivPreview = findViewById(R.id.ivPreview)
        btnPickImage = findViewById(R.id.btnPickImage)
        btnSubmit = findViewById(R.id.btnSubmit)

        // Spinner danh mục
        val categories = arrayOf("Đồ điện tử", "Thời trang", "Đồ gia dụng", "Khác")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spCategory.adapter = adapter

        btnPickImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        btnSubmit.setOnClickListener {
            if (imageUri != null) {
                uploadImageToCloudinary(imageUri!!)
            } else {
                Toast.makeText(this, "Vui lòng chọn ảnh!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.data
            Glide.with(this).load(imageUri).into(ivPreview)
        }
    }

    // ✅ Gửi ảnh lên Cloudinary
    private fun uploadImageToCloudinary(uri: Uri) {
        val inputStream = contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes() ?: return
        val requestFile = RequestBody.create("image/*".toMediaTypeOrNull(), bytes)
        val body = MultipartBody.Part.createFormData("file", "upload.jpg", requestFile)

        val preset = RequestBody.create("text/plain".toMediaTypeOrNull(), "android_unsigned")

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.cloudinary.com/v1_1/dovf2zc0u/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(CloudinaryService::class.java)
        val call = service.uploadImage(body, preset)

        call.enqueue(object : Callback<CloudinaryUploadResponse> {
            override fun onResponse(
                call: Call<CloudinaryUploadResponse>,
                response: Response<CloudinaryUploadResponse>
            ) {
                if (response.isSuccessful) {
                    val imageUrl = response.body()?.secure_url
                    if (imageUrl != null) {
                        saveListing(imageUrl)
                    } else {
                        Toast.makeText(this@CreateListingActivity, "Không nhận được URL ảnh!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@CreateListingActivity, "Tải ảnh thất bại!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<CloudinaryUploadResponse>, t: Throwable) {
                Toast.makeText(this@CreateListingActivity, "Lỗi: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // ✅ Lưu bài đăng sau khi có URL ảnh
    private fun saveListing(imageUrl: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val listing = Listing(
            id = UUID.randomUUID().toString(),
            title = etTitle.text.toString().trim(),
            description = etDescription.text.toString().trim(),
            category = spCategory.selectedItem.toString(),
            imageUrl = imageUrl,
            ownerUid = uid
        )

        FirebaseFirestore.getInstance().collection("listings")
            .document(listing.id)
            .set(listing)
            .addOnSuccessListener {
                Toast.makeText(this, "🎉 Bài đăng đã được tạo!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "❌ Lỗi: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
