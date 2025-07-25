package com.example.tradeup.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.tradeup.R
import com.example.tradeup.data.model.User
import com.example.tradeup.data.remote.UserRepository
import com.google.firebase.auth.FirebaseAuth
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch

class EditProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var userRepository: UserRepository

    // ✅ PROPERLY DECLARED Views
    private lateinit var ivBackButton: ImageView
    private lateinit var ivAvatar: CircleImageView
    private lateinit var btnChangePhoto: Button
    private lateinit var etDisplayName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etBio: EditText
    private lateinit var etAddress: EditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: TextView
    private lateinit var progressBar: ProgressBar

    private var selectedImageUri: Uri? = null
    private var currentUser: User? = null

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            displaySelectedImage(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        auth = FirebaseAuth.getInstance()
        userRepository = UserRepository()

        // Initialize views FIRST
        initViews()
        setupClickListeners()
        loadCurrentProfile()
    }

    // ✅ FIXED: Proper view initialization
    private fun initViews() {
        try {
            ivBackButton = findViewById(R.id.ivBackButton)
            ivAvatar = findViewById(R.id.ivAvatar)
            btnChangePhoto = findViewById(R.id.btnChangePhoto)
            etDisplayName = findViewById(R.id.etDisplayName)
            etPhone = findViewById(R.id.etPhone)
            etBio = findViewById(R.id.etBio)
            etAddress = findViewById(R.id.etAddress)
            btnSave = findViewById(R.id.btnSave)
            btnCancel = findViewById(R.id.btnCancel)
            progressBar = findViewById(R.id.progressBar)
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing views: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupClickListeners() {
        ivBackButton.setOnClickListener {
            finish()
        }

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

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun openImagePicker() {
        imagePickerLauncher.launch("image/*")
    }

    private fun displaySelectedImage(uri: Uri) {
        Glide.with(this)
            .load(uri)
            .placeholder(R.drawable.ic_avatar_placeholder)
            .error(R.drawable.ic_avatar_placeholder)
            .into(ivAvatar)
    }

    private fun loadCurrentProfile() {
        val firebaseUser = auth.currentUser
        if (firebaseUser == null) {
            Toast.makeText(this, "Authentication error", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                val userProfile = userRepository.getUserProfile(firebaseUser.uid)

                if (userProfile != null) {
                    currentUser = userProfile

                    // Pre-fill form with current data
                    etDisplayName.setText(userProfile.displayName)
                    etPhone.setText(userProfile.phoneNumber)
                    etBio.setText(userProfile.bio)
                    etAddress.setText(userProfile.address)

                    // Load current profile picture
                    if (userProfile.profilePictureUrl.isNotEmpty()) {
                        Glide.with(this@EditProfileActivity)
                            .load(userProfile.profilePictureUrl)
                            .placeholder(R.drawable.ic_avatar_placeholder)
                            .error(R.drawable.ic_avatar_placeholder)
                            .into(ivAvatar)
                    }
                } else {
                    Toast.makeText(this@EditProfileActivity, "Profile not found", Toast.LENGTH_SHORT).show()
                    finish()
                }

                showLoading(false)

            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(this@EditProfileActivity, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun saveProfile() {
        val displayName = etDisplayName.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val bio = etBio.text.toString().trim()
        val address = etAddress.text.toString().trim()

        // Validation
        if (displayName.isEmpty()) {
            etDisplayName.error = "Display name is required"
            etDisplayName.requestFocus()
            return
        }

        if (phone.isEmpty()) {
            etPhone.error = "Phone number is required"
            etPhone.requestFocus()
            return
        }

        if (bio.isEmpty() || bio.length < 10) {
            etBio.error = "Bio must be at least 10 characters"
            etBio.requestFocus()
            return
        }

        val firebaseUser = auth.currentUser ?: return
        val existingUser = currentUser ?: return

        showLoading(true)

        lifecycleScope.launch {
            try {
                // Upload new image using Cloudinary
                var profileImageUrl = existingUser.profilePictureUrl
                selectedImageUri?.let { uri ->
                    val newImageUrl = userRepository.uploadProfileImage(this@EditProfileActivity, uri, firebaseUser.uid)
                    if (newImageUrl != null) {
                        profileImageUrl = newImageUrl
                    }
                }

                // Update user profile
                val updatedUser = existingUser.copy(
                    displayName = displayName,
                    phoneNumber = phone,
                    bio = bio,
                    address = address,
                    profilePictureUrl = profileImageUrl,
                    updatedAt = com.google.firebase.Timestamp.now()
                )

                val success = userRepository.saveUserProfile(updatedUser)

                showLoading(false)

                if (success) {
                    // Clear image cache if new image was uploaded
                    if (selectedImageUri != null) {
                        Thread {
                            try {
                                Glide.get(applicationContext).clearDiskCache()
                            } catch (e: Exception) {
                                // Ignore errors
                            }
                        }.start()
                    }

                    Toast.makeText(this@EditProfileActivity, "Profile updated successfully! ✨", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@EditProfileActivity, "Failed to update profile. Please try again.", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(this@EditProfileActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnSave.isEnabled = !show
        btnChangePhoto.isEnabled = !show

        etDisplayName.isEnabled = !show
        etPhone.isEnabled = !show
        etBio.isEnabled = !show
        etAddress.isEnabled = !show
    }
}