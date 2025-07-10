package com.example.tradeup.profile

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.tradeup.R
import com.example.tradeup.data.model.User
import com.example.tradeup.data.remote.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.*

class EditProfileActivity : AppCompatActivity() {

    private lateinit var ivAvatar: ImageView
    private lateinit var etName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etAddress: EditText
    private lateinit var etBio: EditText
    private lateinit var etUsername: EditText
    private lateinit var spGender: Spinner
    private lateinit var etBirthday: EditText
    private lateinit var etInterest: EditText
    private lateinit var btnSave: Button

    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val PICK_IMAGE_REQUEST = 100
    private var imageUri: Uri? = null
    private var uploadedImageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        ivAvatar = findViewById(R.id.ivAvatar)
        etName = findViewById(R.id.etName)
        etPhone = findViewById(R.id.etPhone)
        etAddress = findViewById(R.id.etAddress)
        etBio = findViewById(R.id.etBio)
        etUsername = findViewById(R.id.etUsername)
        spGender = findViewById(R.id.spGender)
        etBirthday = findViewById(R.id.etBirthday)
        etInterest = findViewById(R.id.etInterest)
        btnSave = findViewById(R.id.btnSave)

        val genders = arrayOf("Nam", "Nữ", "Khác")
        spGender.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, genders)

        ivAvatar.setOnClickListener { openImagePicker() }
        etBirthday.setOnClickListener { showDatePicker() }

        btnSave.setOnClickListener { saveProfile() }

        loadUser()
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            imageUri = data.data
            ivAvatar.setImageURI(imageUri)
            imageUri?.let { uploadImageToCloudinary(it) }
        }
    }

    private fun uploadImageToCloudinary(uri: Uri) {
        val filePath = FileUtils.getFilePathFromUri(this, uri) ?: return
        val file = File(filePath)
        val requestBody = RequestBody.create("image/*".toMediaTypeOrNull(), file)
        val multipart = MultipartBody.Part.createFormData("file", file.name, requestBody)
        val preset = RequestBody.create("text/plain".toMediaTypeOrNull(), "android_unsigned")

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.cloudinary.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val cloudinary = retrofit.create(CloudinaryService::class.java)
        cloudinary.uploadImage(multipart, preset).enqueue(object : Callback<CloudinaryResponse> {
            override fun onResponse(call: Call<CloudinaryResponse>, response: Response<CloudinaryResponse>) {
                if (response.isSuccessful) {
                    uploadedImageUrl = response.body()?.secureUrl
                    Toast.makeText(this@EditProfileActivity, "Ảnh đã được tải lên", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<CloudinaryResponse>, t: Throwable) {
                Log.e("CloudinaryUpload", "Lỗi tải ảnh: ${t.message}")
            }
        })
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val dialog = DatePickerDialog(
            this,
            { _, year, month, day ->
                etBirthday.setText(String.format("%02d/%02d/%04d", day, month + 1, year))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        dialog.show()
    }

    private fun loadUser() {
        UserRepository.getUserProfile(currentUid) { user ->
            user?.let {
                etName.setText(it.name)
                etPhone.setText(it.phone)
                etAddress.setText(it.address)
                etBio.setText(it.bio)
                etUsername.setText(it.username)
                etBirthday.setText(it.birthday)
                etInterest.setText(it.interests)
                val genderIndex = when (it.gender) {
                    "Nam" -> 0
                    "Nữ" -> 1
                    "Khác" -> 2
                    else -> 0
                }
                spGender.setSelection(genderIndex)
                uploadedImageUrl = it.profileImageUrl
                Glide.with(this).load(it.profileImageUrl).into(ivAvatar)
            }
        }
    }

    private fun saveProfile() {
        val user = User(
            uid = currentUid,
            name = etName.text.toString(),
            email = FirebaseAuth.getInstance().currentUser?.email ?: "",
            phone = etPhone.text.toString(),
            address = etAddress.text.toString(),
            bio = etBio.text.toString(),
            username = etUsername.text.toString(),
            gender = spGender.selectedItem.toString(),
            birthday = etBirthday.text.toString(),
            interests = etInterest.text.toString(),
            profileImageUrl = uploadedImageUrl ?: ""
        )

        UserRepository.saveUserProfile(user) { success, error ->
            if (success) {
                Toast.makeText(this, "✅ Hồ sơ đã được cập nhật", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "❌ Lỗi: $error", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
