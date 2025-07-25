package com.example.tradeup.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.tradeup.R
import com.example.tradeup.data.remote.UserRepository
import com.example.tradeup.utils.CloudinaryHelper
import com.google.firebase.auth.FirebaseAuth
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch

class EditProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var userRepository: UserRepository
    private lateinit var cloudinaryHelper: CloudinaryHelper

    // Views
    private lateinit var ivProfilePic: CircleImageView
    private lateinit var etDisplayName: EditText
    private lateinit var etBio: EditText
    private lateinit var etPhone: EditText
    private lateinit var etAddress: EditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var btnChangePhoto: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var scrollView: ScrollView

    // Image selection
    private var selectedImageUri: Uri? = null
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // Initialize Firebase and repositories
        auth = FirebaseAuth.getInstance()
        userRepository = UserRepository()
        cloudinaryHelper = CloudinaryHelper()

        // Setup toolbar
        setupToolbar()

        // Initialize views
        initViews()

        // Setup image picker
        setupImagePicker()

        // Setup click listeners
        setupClickListeners()

        // Load current profile data
        loadCurrentProfile()

        Log.d("EditProfileActivity", "✅ EditProfileActivity created successfully")
    }

    private fun setupToolbar() {
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            title = "Edit Profile"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    private fun initViews() {
        ivProfilePic = findViewById(R.id.ivProfilePic)
        etDisplayName = findViewById(R.id.etDisplayName)
        etBio = findViewById(R.id.etBio)
        etPhone = findViewById(R.id.etPhone)
        etAddress = findViewById(R.id.etAddress)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)
        btnChangePhoto = findViewById(R.id.btnChangePhoto)
        progressBar = findViewById(R.id.progressBar)
        scrollView = findViewById(R.id.scrollView)
    }

    private fun setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                data?.data?.let { uri ->
                    selectedImageUri = uri
                    // Show selected image immediately
                    Glide.with(this)
                        .load(uri)
                        .placeholder(R.drawable.ic_avatar_placeholder)
                        .error(R.drawable.ic_avatar_placeholder)
                        .into(ivProfilePic)
                }
            }
        }
    }

    private fun setupClickListeners() {
        btnChangePhoto.setOnClickListener {
            openImagePicker()
        }

        btnSave.setOnClickListener {
            saveProfile()
        }

        btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png"))
        }
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select Profile Picture"))
    }

    private fun loadCurrentProfile() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                val userProfile = userRepository.getUserProfile(currentUser.uid)

                if (userProfile != null) {
                    // Fill form with current data
                    etDisplayName.setText(userProfile.displayName)
                    etBio.setText(userProfile.bio)
                    etPhone.setText(userProfile.phoneNumber)
                    etAddress.setText(userProfile.address)

                    // Load current profile picture
                    if (userProfile.profilePictureUrl.isNotEmpty()) {
                        Glide.with(this@EditProfileActivity)
                            .load(userProfile.profilePictureUrl)
                            .placeholder(R.drawable.ic_avatar_placeholder)
                            .error(R.drawable.ic_avatar_placeholder)
                            .into(ivProfilePic)
                    }
                } else {
                    Toast.makeText(this@EditProfileActivity, "Profile not found", Toast.LENGTH_SHORT).show()
                }

                showLoading(false)

            } catch (e: Exception) {
                Log.e("EditProfileActivity", "Error loading profile: ${e.message}")
                showLoading(false)
                Toast.makeText(this@EditProfileActivity, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveProfile() {
        if (!validateInputs()) return

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                // Upload new profile picture if selected
                var profilePictureUrl: String? = null
                selectedImageUri?.let { uri ->
                    profilePictureUrl = cloudinaryHelper.uploadImage(uri, this@EditProfileActivity)
                }

                // Update profile data
                val updates = mutableMapOf<String, Any>(
                    "displayName" to etDisplayName.text.toString().trim(),
                    "bio" to etBio.text.toString().trim(),
                    "phoneNumber" to etPhone.text.toString().trim(),
                    "address" to etAddress.text.toString().trim(),
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )

                // Add profile picture URL if uploaded
                profilePictureUrl?.let { url ->
                    updates["profilePictureUrl"] = url
                }

                val success = userRepository.updateUserProfile(currentUser.uid, updates)

                showLoading(false)

                if (success) {
                    Toast.makeText(this@EditProfileActivity, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@EditProfileActivity, "Failed to update profile. Please try again.", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e("EditProfileActivity", "Error saving profile: ${e.message}")
                showLoading(false)
                Toast.makeText(this@EditProfileActivity, "Error saving profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateInputs(): Boolean {
        when {
            etDisplayName.text.toString().trim().isEmpty() -> {
                etDisplayName.error = "Please enter your display name"
                etDisplayName.requestFocus()
                return false
            }
            etDisplayName.text.toString().trim().length < 2 -> {
                etDisplayName.error = "Display name must be at least 2 characters"
                etDisplayName.requestFocus()
                return false
            }
            etPhone.text.toString().trim().isNotEmpty() && etPhone.text.toString().trim().length < 10 -> {
                etPhone.error = "Please enter a valid phone number"
                etPhone.requestFocus()
                return false
            }
        }
        return true
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        scrollView.alpha = if (show) 0.5f else 1.0f
        btnSave.isEnabled = !show
        btnChangePhoto.isEnabled = !show
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}