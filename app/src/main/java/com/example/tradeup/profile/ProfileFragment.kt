package com.example.tradeup.profile

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.tradeup.R
import com.example.tradeup.auth.LoginActivity
import com.example.tradeup.data.remote.UserRepository
import com.example.tradeup.utils.CloudinaryHelper
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment() {

    private lateinit var ivAvatar: ImageView
    private lateinit var tvDisplayName: TextView
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvBio: TextView
    private lateinit var tvUsername: TextView
    private lateinit var tvGender: TextView
    private lateinit var tvBirthday: TextView
    private lateinit var tvInterest: TextView
    private lateinit var tvRating: TextView // ✅ NEW: Rating display
    private lateinit var tvJoinedDate: TextView // ✅ NEW: Join date
    private lateinit var btnLogout: Button
    private lateinit var btnEditProfile: Button
    private lateinit var btnDeleteAccount: Button
    private lateinit var btnChangeProfilePicture: Button // ✅ NEW: Change profile picture

    // ✅ NEW: Image picker
    private val PICK_IMAGE_REQUEST = 1001
    private var selectedImageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupClickListeners()
        loadUserData()
    }

    private fun initViews(view: View) {
        ivAvatar = view.findViewById(R.id.ivAvatar)
        tvDisplayName = view.findViewById(R.id.tvDisplayName)
        tvName = view.findViewById(R.id.tvName)
        tvEmail = view.findViewById(R.id.tvEmail)
        tvPhone = view.findViewById(R.id.tvPhone)
        tvAddress = view.findViewById(R.id.tvAddress)
        tvBio = view.findViewById(R.id.tvBio)
        tvUsername = view.findViewById(R.id.tvUsername)
        tvGender = view.findViewById(R.id.tvGender)
        tvBirthday = view.findViewById(R.id.tvBirthday)
        tvInterest = view.findViewById(R.id.tvInterest)
        tvRating = view.findViewById(R.id.tvRating) // ✅ NEW
        tvJoinedDate = view.findViewById(R.id.tvJoinedDate) // ✅ NEW
        btnEditProfile = view.findViewById(R.id.btnEditProfile)
        btnLogout = view.findViewById(R.id.btnLogout)
        btnDeleteAccount = view.findViewById(R.id.btnDeleteAccount)
        btnChangeProfilePicture = view.findViewById(R.id.btnChangeProfilePicture) // ✅ NEW
    }

    private fun setupClickListeners() {
        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }

        btnEditProfile.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Opening edit profile...", Toast.LENGTH_SHORT).show()
            }
        }

        // ✅ NEW: Profile picture change
        btnChangeProfilePicture.setOnClickListener {
            openImagePicker()
        }

        // ✅ NEW: Avatar click also opens image picker
        ivAvatar.setOnClickListener {
            openImagePicker()
        }

        // ✅ UPDATED: Enhanced delete account with proper confirmation
        btnDeleteAccount.setOnClickListener {
            showEnhancedDeleteAccountDialog()
        }
    }

    // ✅ NEW: Open image picker for profile picture
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    // ✅ NEW: Handle image selection result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            selectedImageUri?.let { uri ->
                uploadNewProfilePicture(uri)
            }
        }
    }

    // ✅ NEW: Upload new profile picture
    private fun uploadNewProfilePicture(imageUri: Uri) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        // Show loading state
        btnChangeProfilePicture.isEnabled = false
        btnChangeProfilePicture.text = "Uploading..."

        CloudinaryHelper.uploadProfileImage(
            context = requireContext(),
            imageUri = imageUri,
            userId = currentUser.uid,
            onSuccess = { newImageUrl ->
                updateProfileImageInDatabase(newImageUrl)
            },
            onFailure = { error ->
                btnChangeProfilePicture.isEnabled = true
                btnChangeProfilePicture.text = "Change Photo"
                Toast.makeText(requireContext(), "Failed to upload: $error", Toast.LENGTH_SHORT).show()
            },
            onProgress = { progress ->
                btnChangeProfilePicture.text = "Uploading $progress%"
            }
        )
    }

    // ✅ NEW: Update profile image URL in database
    private fun updateProfileImageInDatabase(newImageUrl: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        UserRepository.getUserProfile(currentUser.uid) { existingUser ->
            if (existingUser != null) {
                val updatedUser = existingUser.copy(profileImageUrl = newImageUrl)

                UserRepository.saveUserProfile(updatedUser) { success, error ->
                    btnChangeProfilePicture.isEnabled = true
                    btnChangeProfilePicture.text = "Change Photo"

                    if (isAdded) {
                        if (success) {
                            // Update UI immediately
                            Glide.with(requireContext())
                                .load(newImageUrl)
                                .error(R.drawable.ic_avatar_placeholder)
                                .into(ivAvatar)
                            Toast.makeText(requireContext(), "✅ Profile picture updated!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "❌ Failed to save: $error", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    // ✅ UPDATED: Enhanced delete confirmation per requirements
    private fun showEnhancedDeleteAccountDialog() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val userDisplayName = currentUser.displayName ?: currentUser.email ?: "Unknown"

        // Create custom dialog
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_delete_account, null)
        val etConfirmName = dialogView.findViewById<EditText>(R.id.etConfirmName)
        val tvInstructions = dialogView.findViewById<TextView>(R.id.tvInstructions)

        tvInstructions.text = "Type \"$userDisplayName\" to confirm deletion:"

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("⚠️ Delete Account Permanently")
            .setMessage("This action cannot be undone. All your data, listings, and messages will be permanently deleted.")
            .setView(dialogView)
            .setPositiveButton("DELETE") { _, _ ->
                val typedName = etConfirmName.text.toString().trim()
                if (typedName == userDisplayName) {
                    confirmDeleteAccount()
                } else {
                    Toast.makeText(requireContext(), "❌ Name doesn't match. Account not deleted.", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        // Initially disable delete button
        val deleteButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        deleteButton.isEnabled = false

        // Enable delete button only when name matches
        etConfirmName.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                deleteButton.isEnabled = s.toString().trim() == userDisplayName
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    // ✅ UPDATED: Confirm and execute account deletion
    private fun confirmDeleteAccount() {
        btnDeleteAccount.isEnabled = false
        btnDeleteAccount.text = "Deleting..."

        UserRepository.deleteUserAccount { success, error ->
            if (isAdded) {
                if (success) {
                    Toast.makeText(requireContext(), "✅ Account deleted successfully", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(requireContext(), LoginActivity::class.java))
                    requireActivity().finish()
                } else {
                    btnDeleteAccount.isEnabled = true
                    btnDeleteAccount.text = "⚠️ Delete Account"
                    Toast.makeText(requireContext(), "❌ Failed to delete: $error", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun loadUserData() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        UserRepository.getUserProfile(uid) { user ->
            if (user != null && isAdded) {
                // Update UI with user data
                tvDisplayName.text = user.name.ifEmpty { "User Name" }
                tvEmail.text = user.email
                tvName.text = user.name.ifEmpty { "Not set" }
                tvPhone.text = user.phone.ifEmpty { "Not set" }
                tvAddress.text = user.address.ifEmpty { "Not set" }
                tvBio.text = user.bio.ifEmpty { "No bio yet" }
                tvUsername.text = user.username.ifEmpty { "Not set" }
                tvGender.text = user.gender.ifEmpty { "Not set" }
                tvBirthday.text = user.birthday.ifEmpty { "Not set" }
                tvInterest.text = user.interests.ifEmpty { "Not set" }

                // ✅ NEW: Display rating and transaction count
                if (user.ratingCount > 0) {
                    tvRating.text = "⭐ ${String.format("%.1f", user.rating)} (${user.ratingCount} reviews)"
                } else {
                    tvRating.text = "⭐ No ratings yet"
                }

                // ✅ NEW: Display join date
                val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                tvJoinedDate.text = "Joined ${dateFormat.format(Date(user.joinedDate))}"

                // Load profile image
                if (user.profileImageUrl.isNotEmpty()) {
                    Glide.with(requireContext())
                        .load(user.profileImageUrl)
                        .error(R.drawable.ic_avatar_placeholder)
                        .into(ivAvatar)
                }
            } else {
                if (isAdded) {
                    Toast.makeText(requireContext(), "❌ Failed to load profile", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}