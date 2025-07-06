package com.example.tradeup.listing

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.tradeup.R
import com.example.tradeup.data.model.Listing
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class CreateListingActivity : AppCompatActivity() {

    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var spCategory: Spinner
    private lateinit var ivPreview: ImageView
    private lateinit var btnChooseImage: Button
    private lateinit var btnPost: Button

    private var imageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_listing)

        etTitle = findViewById(R.id.etTitle)
        etDescription = findViewById(R.id.etDescription)
        spCategory = findViewById(R.id.spCategory)
        ivPreview = findViewById(R.id.ivPreview)
        btnChooseImage = findViewById(R.id.btnChooseImage)
        btnPost = findViewById(R.id.btnPost)

        // Load categories
        val categories = arrayOf("Đồ điện tử", "Thời trang", "Đồ gia dụng", "Khác")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spCategory.adapter = adapter

        btnChooseImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        btnPost.setOnClickListener {
            if (imageUri != null) {
                uploadImageAndPost()
            } else {
                Toast.makeText(this, "Vui lòng chọn ảnh", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.data
            ivPreview.setImageURI(imageUri)
        }
    }

    private fun uploadImageAndPost() {
        val fileRef = FirebaseStorage.getInstance()
            .reference.child("listings/${UUID.randomUUID()}.jpg")

        fileRef.putFile(imageUri!!)
            .addOnSuccessListener {
                fileRef.downloadUrl.addOnSuccessListener { uri ->
                    saveListing(uri.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi tải ảnh: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveListing(imageUrl: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val listing = Listing(
            id = UUID.randomUUID().toString(),
            title = etTitle.text.toString(),
            description = etDescription.text.toString(),
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
