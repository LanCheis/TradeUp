package com.example.tradeup.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.tradeup.R
import com.example.tradeup.data.model.User
import com.example.tradeup.data.remote.UserRepository
import com.example.tradeup.databinding.ActivityEditProfileBinding
import com.example.tradeup.utils.CloudinaryHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

// FR-1.2.2: Modern profile editing with clean UI
class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var userRepository: UserRepository

    private var selectedImageUri: Uri? = null
    private var currentUser: User? = null

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            updateProfileImagePreview(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        userRepository = UserRepository()

        setupToolbar()
        setupClickListeners()
        loadCurrentProfile()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Edit Profile"
        }

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupClickListeners() {
        // Profile photo selection
        binding.ivProfilePhoto.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        binding.btnChangePhoto.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        // Save changes
        binding.btnSave.setOnClickListener {
            saveProfile()
        }

        // Cancel
        binding.btnCancel.setOnClickListener {
            onBackPressed()
        }
    }

    private fun loadCurrentProfile() {
        val firebaseUser = auth.currentUser
        if (firebaseUser == null) {
            finish()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                userRepository.getUserProfile(firebaseUser.uid).fold(
                    onSuccess = { user ->
                        currentUser = user
                        populateFields(user)
                        showLoading(false)
                    },
                    onFailure = { exception ->
                        Log.e("EditProfile", "Failed to load profile: ${exception.message}")
                        Toast.makeText(this@EditProfileActivity,
                            "Failed to load profile", Toast.LENGTH_SHORT).show()
                        showLoading(false)
                    }
                )
            } catch (e: Exception) {
                Log.e("EditProfile", "Error loading profile: ${e.message}")
                showLoading(false)
            }
        }
    }

    private fun populateFields(user: User) {
        binding.apply {
            etDisplayName.setText(user.displayName)
            etBio.setText(user.bio)
            etPhone.setText(user.phoneNumber)
            etAddress.setText(user.address)

            // Load profile image
            if (user.profileImageUrl.isNotEmpty()) {
                Glide.with(this@EditProfileActivity)
                    .load(user.profileImageUrl)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .circleCrop()
                    .into(ivProfilePhoto)
            }
        }
    }

    private fun updateProfileImagePreview(uri: Uri) {
        Glide.with(this)
            .load(uri)
            .circleCrop()
            .into(binding.ivProfilePhoto)
    }

    private fun saveProfile() {
        if (!validateInputs()) return

        showLoading(true)

        lifecycleScope.launch {
            try {
                var profileImageUrl = currentUser?.profileImageUrl ?: ""

                // Upload new image if selected
                if (selectedImageUri != null) {
                    val uploadResult = CloudinaryHelper.uploadProfileImage(
                        this@EditProfileActivity,
                        selectedImageUri!!,
                        auth.currentUser!!.uid
                    )
                    profileImageUrl = uploadResult ?: profileImageUrl
                }

                // Create updated user object
                val updatedUser = currentUser?.copy(
                    displayName = binding.etDisplayName.text.toString().trim(),
                    bio = binding.etBio.text.toString().trim(),
                    phoneNumber = binding.etPhone.text.toString().trim(),
                    address = binding.etAddress.text.toString().trim(),
                    profileImageUrl = profileImageUrl
                ) ?: return@launch

                // Save to Firebase
                userRepository.updateUserProfile(updatedUser).fold(
                    onSuccess = {
                        Toast.makeText(this@EditProfileActivity,
                            "Profile updated successfully! ✨", Toast.LENGTH_SHORT).show()
                        setResult(Activity.RESULT_OK)
                        finish()
                    },
                    onFailure = { exception ->
                        Log.e("EditProfile", "Failed to save: ${exception.message}")
                        Toast.makeText(this@EditProfileActivity,
                            "Failed to save profile", Toast.LENGTH_SHORT).show()
                        showLoading(false)
                    }
                )

            } catch (e: Exception) {
                Log.e("EditProfile", "Error saving profile: ${e.message}")
                Toast.makeText(this@EditProfileActivity,
                    "Error saving profile", Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
        }
    }

    private fun validateInputs(): Boolean {
        binding.apply {
            if (etDisplayName.text.toString().trim().isEmpty()) {
                etDisplayName.error = "Display name is required"
                etDisplayName.requestFocus()
                return false
            }

            val phone = etPhone.text.toString().trim()
            if (phone.isNotEmpty() && phone.length < 10) {
                etPhone.error = "Please enter a valid phone number"
                etPhone.requestFocus()
                return false
            }
        }
        return true
    }

    private fun showLoading(show: Boolean) {
        binding.apply {
            progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
            btnSave.isEnabled = !show
            btnSave.alpha = if (show) 0.5f else 1.0f
        }
    }
}