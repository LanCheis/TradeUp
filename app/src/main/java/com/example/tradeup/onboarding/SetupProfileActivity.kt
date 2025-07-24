package com.example.tradeup.onboarding

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.tradeup.MainActivity
import com.example.tradeup.R
import com.example.tradeup.data.model.User
import com.example.tradeup.data.remote.UserRepository
import com.example.tradeup.utils.ProfileValidator
import android.widget.EditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch

class SetupProfileActivity : AppCompatActivity() {

    private lateinit var ivAvatar: CircleImageView
    private lateinit var btnChangeAvatar: Button
    private lateinit var etFullName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etBio: EditText
    private lateinit var etAddress: EditText
    private lateinit var btnContinue: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvWelcome: TextView
    private lateinit var tvIncompleteMessage: TextView

    private var selectedImageUri: Uri? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var userRepository: UserRepository

    // Gallery launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            displaySelectedImage(it)
            Log.d("SetupProfile", "✅ Image selected: $it")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_profile)

        auth = FirebaseAuth.getInstance()
        userRepository = UserRepository()

        initViews()
        setupClickListeners()
        showIncompleteMessageIfNeeded()
        loadExistingProfile()
    }

    private fun initViews() {
        ivAvatar = findViewById(R.id.ivAvatar)
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar)
        etFullName = findViewById(R.id.etFullName)
        etPhone = findViewById(R.id.etPhone)
        etBio = findViewById(R.id.etBio)
        etAddress = findViewById(R.id.etAddress)
        btnContinue = findViewById(R.id.btnContinue)
        progressBar = findViewById(R.id.progressBar)
        tvWelcome = findViewById(R.id.tvWelcome)
        tvIncompleteMessage = findViewById(R.id.tvIncompleteMessage)

        val user = auth.currentUser
        tvWelcome.text = "Complete Your Profile"
    }

    private fun showIncompleteMessageIfNeeded() {
        val message = intent.getStringExtra("incomplete_message")
        if (!message.isNullOrEmpty()) {
            tvIncompleteMessage.text = message
            tvIncompleteMessage.visibility = View.VISIBLE
        } else {
            tvIncompleteMessage.visibility = View.GONE
        }
    }

    private fun setupClickListeners() {
        btnChangeAvatar.setOnClickListener {
            openImagePicker()
        }

        btnContinue.setOnClickListener {
            saveProfile()
        }
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

    private fun loadExistingProfile() {
        val currentUser = auth.currentUser ?: return

        lifecycleScope.launch {
            val userProfile = userRepository.getUserProfile(currentUser.uid)
            userProfile?.let { user ->
                // Pre-fill existing data
                etFullName.setText(user.displayName)
                etPhone.setText(user.phoneNumber)
                etBio.setText(user.bio)
                etAddress.setText(user.address)

                // Load existing profile picture
                if (user.profilePictureUrl.isNotEmpty()) {
                    Log.d("SetupProfile", "🔍 Loading existing image: ${user.profilePictureUrl}")
                    Glide.with(this@SetupProfileActivity)
                        .load(user.profilePictureUrl)
                        .placeholder(R.drawable.ic_avatar_placeholder)
                        .into(ivAvatar)
                } else {
                    Log.d("SetupProfile", "❌ No existing profile image URL")
                }
            }
        }
    }

    private fun saveProfile() {
        val displayName = etFullName.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val bio = etBio.text.toString().trim()
        val address = etAddress.text.toString().trim()

        // Clear previous visual errors
        etFullName.setBackgroundResource(R.drawable.edittext_background)
        etPhone.setBackgroundResource(R.drawable.edittext_background)
        etBio.setBackgroundResource(R.drawable.edittext_background)

        // Validate required fields
        var hasErrors = false

        if (displayName.isEmpty()) {
            etFullName.setBackgroundResource(R.drawable.edittext_error_background)
            etFullName.error = "Username is required"
            hasErrors = true
        }

        if (phone.isEmpty()) {
            etPhone.setBackgroundResource(R.drawable.edittext_error_background)
            etPhone.error = "Phone number is required"
            hasErrors = true
        }

        if (bio.isEmpty()) {
            etBio.setBackgroundResource(R.drawable.edittext_error_background)
            etBio.error = "Bio is required"
            hasErrors = true
        } else if (bio.length < 10) {
            etBio.setBackgroundResource(R.drawable.edittext_error_background)
            etBio.error = "Bio must be at least 10 characters"
            hasErrors = true
        }

        if (hasErrors) {
            Toast.makeText(this, "Please fix the errors above", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        val currentUser = auth.currentUser ?: return

        lifecycleScope.launch {
            try {
                // Get existing profile first
                val existingProfile = userRepository.getUserProfile(currentUser.uid)

                // 🔧 DEBUG: Upload image using Cloudinary
                var profileImageUrl = existingProfile?.profilePictureUrl ?: ""
                Log.d("SetupProfile", "🔍 Starting save process...")
                Log.d("SetupProfile", "🔍 Existing profile image URL: '$profileImageUrl'")
                Log.d("SetupProfile", "🔍 Selected image URI: $selectedImageUri")

                selectedImageUri?.let { uri ->
                    Log.d("SetupProfile", "🔧 Starting image upload...")
                    try {
                        val uploadedUrl = userRepository.uploadProfileImage(this@SetupProfileActivity, uri, currentUser.uid)
                        if (uploadedUrl != null) {
                            profileImageUrl = uploadedUrl
                            Log.d("SetupProfile", "✅ Image uploaded successfully: $profileImageUrl")
                        } else {
                            Log.e("SetupProfile", "❌ Image upload failed - returned null")
                            Toast.makeText(this@SetupProfileActivity, "Image upload failed, saving profile without image", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e("SetupProfile", "❌ Image upload exception: ${e.message}")
                        Toast.makeText(this@SetupProfileActivity, "Image upload error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                Log.d("SetupProfile", "🔍 Final profileImageUrl before saving: '$profileImageUrl'")

                // Create complete user profile with proper timestamp handling
                val updatedUser = User(
                    uid = currentUser.uid,
                    email = currentUser.email ?: "",
                    displayName = displayName,
                    profilePictureUrl = profileImageUrl,
                    bio = bio,
                    phoneNumber = phone,
                    address = address,
                    rating = existingProfile?.rating ?: 0.0,
                    reviewCount = existingProfile?.reviewCount ?: 0,
                    emailVerified = currentUser.isEmailVerified, // ✅ FIXED: Changed to emailVerified
                    createdAt = existingProfile?.createdAt ?: com.google.firebase.Timestamp.now(),
                    updatedAt = com.google.firebase.Timestamp.now()
                )

                Log.d("SetupProfile", "🔍 Saving user profile with image URL: '${updatedUser.profilePictureUrl}'")
                val success = userRepository.saveUserProfile(updatedUser)

                showLoading(false)

                if (success) {
                    Log.d("SetupProfile", "✅ Profile saved successfully!")

                    // Double-check profile completeness
                    if (ProfileValidator.isProfileComplete(updatedUser)) {
                        Toast.makeText(this@SetupProfileActivity,
                            "Profile completed successfully! 🎉", Toast.LENGTH_SHORT).show()
                        navigateToMain()
                    } else {
                        Toast.makeText(this@SetupProfileActivity,
                            "Please ensure all required fields are complete", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("SetupProfile", "❌ Failed to save profile to database")
                    Toast.makeText(this@SetupProfileActivity,
                        "Failed to save profile. Please try again.", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                showLoading(false)
                Log.e("SetupProfile", "❌ Exception in saveProfile: ${e.message}")
                Toast.makeText(this@SetupProfileActivity,
                    "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnContinue.isEnabled = !show
        btnChangeAvatar.isEnabled = !show

        etFullName.isEnabled = !show
        etPhone.isEnabled = !show
        etBio.isEnabled = !show
        etAddress.isEnabled = !show
    }
}