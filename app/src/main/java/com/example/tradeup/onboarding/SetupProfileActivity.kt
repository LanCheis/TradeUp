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

            // 🔧 UPDATED: Upload image using Cloudinary with context
            var profileImageUrl = existingProfile?.profilePictureUrl ?: ""
            selectedImageUri?.let { uri ->
                val uploadedUrl = userRepository.uploadProfileImage(this@SetupProfileActivity, uri, currentUser.uid)
                if (uploadedUrl != null) {
                    profileImageUrl = uploadedUrl
                }
            }

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
                isEmailVerified = currentUser.isEmailVerified,
                createdAt = existingProfile?.createdAt ?: com.google.firebase.Timestamp.now(),
                updatedAt = com.google.firebase.Timestamp.now() // Always update timestamp
            )

            val success = userRepository.saveUserProfile(updatedUser)

            showLoading(false)

            if (success) {
                // Clear image cache if new image was uploaded
                if (selectedImageUri != null) {
                    try {
                        Glide.with(this@SetupProfileActivity).clearMemoryCache()
                        // Clear disk cache on background thread
                        Thread {
                            Glide.get(this@SetupProfileActivity).clearDiskCache()
                        }.start()
                    } catch (e: Exception) {
                        // Ignore cache clearing errors
                    }
                }

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
                Toast.makeText(this@SetupProfileActivity,
                    "Failed to save profile. Please try again.", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            showLoading(false)
            Toast.makeText(this@SetupProfileActivity,
                "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}