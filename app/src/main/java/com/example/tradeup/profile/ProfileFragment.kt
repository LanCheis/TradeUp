// File: app/src/main/java/com/example/tradeup/profile/ProfileFragment.kt

package com.example.tradeup.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.tradeup.R
import com.example.tradeup.auth.LoginActivity
import com.example.tradeup.data.remote.UserRepository
import com.example.tradeup.offers.MyOffersActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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
    private lateinit var tvJoinedDate: TextView
    private lateinit var btnLogout: Button
    private lateinit var btnEditProfile: Button
    private lateinit var btnDeleteAccount: Button
    private lateinit var btnMyOffers: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        initializeViews(view)
        setupClickListeners()
        loadUserProfile()
    }

    private fun initializeViews(view: View) {
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
        tvJoinedDate = view.findViewById(R.id.tvJoinedDate)
        btnLogout = view.findViewById(R.id.btnLogout)
        btnEditProfile = view.findViewById(R.id.btnEditProfile)
        btnDeleteAccount = view.findViewById(R.id.btnDeleteAccount)
        btnMyOffers = view.findViewById(R.id.btnMyOffers)
    }

    private fun setupClickListeners() {
        // Edit Profile Button
        btnEditProfile.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
            } catch (e: Exception) {
                Toast.makeText(context, "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // ✅ FIXED: My Offers Button with explicit Intent creation
        btnMyOffers.setOnClickListener {
            val intent = Intent(requireContext(), MyOffersActivity::class.java)
            startActivity(intent)
        }

        // Logout Button
        btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        // Delete Account Button
        btnDeleteAccount.setOnClickListener {
            showDeleteAccountConfirmationDialog()
        }

        // Avatar Click - Change Photo
        ivAvatar.setOnClickListener {
            Toast.makeText(context, "Photo change feature coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            redirectToLogin()
            return
        }

        // ✅ FIXED: Explicitly declare the callback type to resolve ambiguity
        val callback: (Boolean, Map<String, Any>?) -> Unit = { success, userData ->
            if (success && userData != null) {
                updateUIWithUserData(userData)
            } else {
                // Use Firebase Auth data as fallback
                updateUIWithAuthData(currentUser)
            }
        }

        UserRepository.getUserProfile(currentUser.uid, callback)
    }

    private fun updateUIWithUserData(userData: Map<String, Any>) {
        // Display Name
        val displayName = userData["name"] as? String ?: "Unknown User"
        tvDisplayName.text = displayName

        // Email
        val email = userData["email"] as? String ?: "No email"
        tvEmail.text = email

        // Name
        val name = userData["name"] as? String ?: displayName
        tvName.text = name

        // Username
        val username = userData["username"] as? String ?: "Not set"
        tvUsername.text = username

        // Phone
        val phone = userData["phone"] as? String ?: "Not provided"
        tvPhone.text = phone

        // Address
        val address = userData["address"] as? String ?: "Not provided"
        tvAddress.text = address

        // Bio
        val bio = userData["bio"] as? String ?: "No bio available"
        tvBio.text = bio

        // Gender
        val gender = userData["gender"] as? String ?: "Not specified"
        tvGender.text = gender

        // Birthday
        val birthday = userData["birthday"] as? String ?: "Not provided"
        tvBirthday.text = birthday

        // Interests
        val interests = userData["interests"] as? String ?: "Not specified"
        tvInterest.text = interests

        // Joined Date
        val joinedDate = userData["createdAt"] as? com.google.firebase.Timestamp
        if (joinedDate != null) {
            val date = joinedDate.toDate()
            val formatter = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
            tvJoinedDate.text = "Joined ${formatter.format(date)}"
        } else {
            tvJoinedDate.text = "Joined recently"
        }

        // Profile Picture
        val profileImageUrl = userData["profileImageUrl"] as? String
        if (!profileImageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(profileImageUrl)
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .into(ivAvatar)
        }
    }

    private fun updateUIWithAuthData(user: com.google.firebase.auth.FirebaseUser) {
        tvDisplayName.text = user.displayName ?: "Unknown User"
        tvEmail.text = user.email ?: "No email"
        tvName.text = user.displayName ?: "Unknown User"

        // Load profile photo from Auth
        if (user.photoUrl != null) {
            Glide.with(this)
                .load(user.photoUrl)
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .into(ivAvatar)
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteAccountConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("⚠️ Delete Account")
            .setMessage("This action cannot be undone. Are you absolutely sure you want to delete your account and all associated data?")
            .setPositiveButton("Delete") { _, _ ->
                showFinalDeleteConfirmation()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFinalDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("⚠️ Final Confirmation")
            .setMessage("Type 'DELETE' to confirm account deletion")
            .setView(createDeleteConfirmationInput())
            .setPositiveButton("Delete Account", null) // Set in show() to prevent auto-dismiss
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createDeleteConfirmationInput(): View {
        val editText = EditText(requireContext())
        editText.hint = "Type 'DELETE' here"
        editText.setPadding(50, 20, 50, 20)
        return editText
    }

    private fun performLogout() {
        try {
            FirebaseAuth.getInstance().signOut()

            val intent = Intent(context, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            // Hiển thị thông báo thành công
            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Logout failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performDeleteAccount() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "No user found", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading dialog
        val progressDialog = createProgressDialog("Deleting account...")
        progressDialog.show()

        // ✅ FIXED: Explicitly declare the callback type
        val deleteCallback: (Boolean) -> Unit = { success ->
            if (success) {
                // Then delete the authentication account
                currentUser.delete().addOnCompleteListener { task ->
                    progressDialog.dismiss()
                    if (task.isSuccessful) {
                        Toast.makeText(context, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                        redirectToLogin()
                    } else {
                        Toast.makeText(context, "Failed to delete account: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                progressDialog.dismiss()
                Toast.makeText(context, "Failed to delete user data", Toast.LENGTH_SHORT).show()
            }
        }

        UserRepository.deleteUserData(currentUser.uid, deleteCallback)
    }

    private fun createProgressDialog(message: String): android.app.ProgressDialog {
        val progressDialog = android.app.ProgressDialog(requireContext())
        progressDialog.setMessage(message)
        progressDialog.setCancelable(false)
        return progressDialog
    }

    private fun redirectToLogin() {
        val intent = Intent(context, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Refresh user data when returning to fragment
        loadUserProfile()
    }
}