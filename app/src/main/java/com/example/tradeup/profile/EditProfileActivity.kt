// app/src/main/java/com/example/tradeup/profile/EditProfileActivity.kt

package com.example.tradeup.profile

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.tradeup.R
import com.example.tradeup.data.model.User
import com.example.tradeup.data.remote.UserRepository
import com.example.tradeup.utils.CloudinaryHelper
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar

class EditProfileActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var btnSave: Button
    private lateinit var ivAvatar: ImageView
    private lateinit var btnChangeAvatar: Button
    private lateinit var etUsername: TextInputEditText
    private lateinit var etName: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etAddress: TextInputEditText
    private lateinit var etBio: TextInputEditText
    private lateinit var etInterest: TextInputEditText
    private lateinit var etBirthday: TextInputEditText
    private lateinit var spinnerGender: Spinner
    private lateinit var progressBar: ProgressBar

    private var selectedImageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Edit Profile"

        initViews()
        setupGenderSpinner()
        setupListeners()
        loadCurrentUserData()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnSave = findViewById(R.id.btnSave)
        ivAvatar = findViewById(R.id.ivAvatar)
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar)
        etUsername = findViewById(R.id.etUsername)
        etName = findViewById(R.id.etName)
        etPhone = findViewById(R.id.etPhone)
        etAddress = findViewById(R.id.etAddress)
        etBio = findViewById(R.id.etBio)
        etInterest = findViewById(R.id.etInterest)
        etBirthday = findViewById(R.id.etBirthday)
        spinnerGender = findViewById(R.id.spinnerGender)
        progressBar = findViewById(R.id.progressBar)

        progressBar.visibility = android.view.View.GONE
    }

    private fun setupGenderSpinner() {
        val genders = arrayOf("Select Gender", "Male", "Female", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, genders)
        spinnerGender.adapter = adapter
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnChangeAvatar.setOnClickListener {
            selectProfileImage()
        }

        ivAvatar.setOnClickListener {
            selectProfileImage()
        }

        etBirthday.setOnClickListener {
            showDatePicker()
        }

        btnSave.setOnClickListener {
            saveProfile()
        }
    }

    private fun selectProfileImage() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            selectedImageUri?.let { uri ->
                Glide.with(this)
                    .load(uri)
                    .error(R.drawable.ic_avatar_placeholder)
                    .into(ivAvatar)
                btnChangeAvatar.text = "✓ Image Selected"
            }
        }
    }

    private fun loadCurrentUserData() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        UserRepository.getUserProfile(uid) { user ->
            if (user != null) {
                etUsername.setText(user.username)
                etName.setText(user.name)
                etPhone.setText(user.phone)
                etAddress.setText(user.address)
                etBio.setText(user.bio)
                etInterest.setText(user.interests)
                etBirthday.setText(user.birthday)

                // Set gender spinner
                val genderPosition = when (user.gender) {
                    "Male" -> 1
                    "Female" -> 2
                    "Other" -> 3
                    else -> 0
                }
                spinnerGender.setSelection(genderPosition)

                // Load profile image
                if (user.profileImageUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(user.profileImageUrl)
                        .error(R.drawable.ic_avatar_placeholder)
                        .into(ivAvatar)
                }
            }
        }
    }

    private fun saveProfile() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        showLoading(true)

        if (selectedImageUri != null) {
            // Upload new image first
            CloudinaryHelper.uploadProfileImage(
                context = this,
                imageUri = selectedImageUri!!,
                userId = currentUser.uid,
                onSuccess = { imageUrl ->
                    saveUserProfileData(currentUser.uid, imageUrl)
                },
                onFailure = { error ->
                    showLoading(false)
                    Toast.makeText(this, "❌ Image upload failed: $error", Toast.LENGTH_LONG).show()
                },
                onProgress = { progress ->
                    btnSave.text = "Uploading... $progress%"
                }
            )
        } else {
            // No new image, save with existing image URL
            UserRepository.getUserProfile(currentUser.uid) { existingUser ->
                saveUserProfileData(currentUser.uid, existingUser?.profileImageUrl ?: "")
            }
        }
    }

    private fun saveUserProfileData(userId: String, imageUrl: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        val updatedUser = User(
            uid = userId,
            email = currentUser.email ?: "",
            name = etName.text.toString().trim(),
            username = etUsername.text.toString().trim(),
            phone = etPhone.text.toString().trim(),
            address = etAddress.text.toString().trim(),
            bio = etBio.text.toString().trim(),
            interests = etInterest.text.toString().trim(),
            birthday = etBirthday.text.toString().trim(),
            gender = if (spinnerGender.selectedItemPosition > 0)
                spinnerGender.selectedItem.toString() else "",
            profileImageUrl = imageUrl
        )

        UserRepository.saveUserProfile(updatedUser) { success, error ->
            showLoading(false)

            if (success) {
                Toast.makeText(this, "✅ Profile updated successfully!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "❌ Failed to save: $error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                etBirthday.setText(
                    String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
                )
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        btnSave.isEnabled = !show
        btnChangeAvatar.isEnabled = !show

        if (show) {
            btnSave.text = "Saving..."
        } else {
            btnSave.text = "Save Changes"
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}