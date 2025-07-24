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
            // 🔧 UPDATED: Upload new image using Cloudinary with context
            var profileImageUrl = existingUser.profilePictureUrl
            selectedImageUri?.let { uri ->
                val newImageUrl = userRepository.uploadProfileImage(this@EditProfileActivity, uri, firebaseUser.uid)
                if (newImageUrl != null) {
                    profileImageUrl = newImageUrl
                }
            }

            // FR-1.2.2: Update user profile
            val updatedUser = existingUser.copy(
                displayName = displayName,
                phoneNumber = phone,
                bio = bio,
                address = address,
                profilePictureUrl = profileImageUrl,
                updatedAt = com.google.firebase.Timestamp.now() // Always update timestamp
            )

            val success = userRepository.saveUserProfile(updatedUser)

            showLoading(false)

            if (success) {
                // Clear Glide cache for updated image
                if (selectedImageUri != null) {
                    try {
                        Glide.with(this@EditProfileActivity).clearMemoryCache()
                        // Clear disk cache on background thread
                        Thread {
                            Glide.get(this@EditProfileActivity).clearDiskCache()
                        }.start()
                    } catch (e: Exception) {
                        // Ignore cache clearing errors
                    }
                }

                Toast.makeText(this@EditProfileActivity, "Profile updated successfully! ✨", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK) // Set result for ProfileFragment
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