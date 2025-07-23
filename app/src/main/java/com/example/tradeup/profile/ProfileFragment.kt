package com.example.tradeup.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.tradeup.R
import com.example.tradeup.auth.LoginActivity
import com.example.tradeup.data.remote.UserRepository
import com.example.tradeup.data.remote.FirebaseAuthHelper
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
    private lateinit var tvRating: TextView
    private lateinit var tvTransactions: TextView
    private lateinit var tvJoinedDate: TextView
    private lateinit var btnLogout: Button
    private lateinit var btnEditProfile: Button
    private lateinit var btnDeleteAccount: Button

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
        loadUserProfile()
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
        tvRating = view.findViewById(R.id.tvRating)
        tvTransactions = view.findViewById(R.id.tvTransactions)
        tvJoinedDate = view.findViewById(R.id.tvJoinedDate)
        btnLogout = view.findViewById(R.id.btnLogout)
        btnEditProfile = view.findViewById(R.id.btnEditProfile)
        btnDeleteAccount = view.findViewById(R.id.btnDeleteAccount)
    }

    private fun setupClickListeners() {
        // FR-1.1.5: Logout option accessible via profile
        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        // FR-1.2.2: Users can update profile
        btnEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }

        // FR-1.2.3: Option to permanently delete account (confirmation required)
        btnDeleteAccount.setOnClickListener {
            showDeleteAccountConfirmation()
        }
    }

    private fun loadUserProfile() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            UserRepository.getUserProfile(user.uid) { userProfile ->
                if (userProfile != null) {
                    populateUserData(userProfile)
                } else {
                    Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun populateUserData(user: com.example.tradeup.data.model.User) {
        // FR-1.2.1: Profile includes display name, profile picture, bio, contact info, and rating
        tvDisplayName.text = user.name.ifEmpty { "User" }
        tvName.text = "Name: ${user.name}"
        tvEmail.text = "Email: ${user.email}"
        tvPhone.text = "Phone: ${user.phone.ifEmpty { "Not provided" }}"
        tvAddress.text = "Address: ${user.address.ifEmpty { "Not provided" }}"
        tvBio.text = "Bio: ${user.bio.ifEmpty { "No bio yet" }}"
        tvUsername.text = "Username: ${user.username}"
        tvGender.text = "Gender: ${user.gender.ifEmpty { "Not specified" }}"
        tvBirthday.text = "Birthday: ${user.birthday.ifEmpty { "Not specified" }}"
        tvInterest.text = "Interests: ${user.interests.ifEmpty { "None specified" }}"

        // FR-7.2.1: Profile shows average rating, total transactions
        tvRating.text = "Rating: ${String.format("%.1f", user.rating)}/5.0 (${user.ratingCount} reviews)"
        tvTransactions.text = "Transactions: ${user.totalTransactions}"

        // Show joined date
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        tvJoinedDate.text = "Joined: ${dateFormat.format(Date(user.joinedDate))}"

        // Load profile image
        if (user.profileImageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(user.profileImageUrl)
                .placeholder(R.drawable.ic_profile)
                .into(ivAvatar)
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        FirebaseAuthHelper.logout()

        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    private fun showDeleteAccountConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Account")
            .setMessage("⚠️ This action cannot be undone. All your data will be permanently deleted.\n\nAre you absolutely sure?")
            .setPositiveButton("Delete Forever") { _, _ ->
                showFinalDeleteConfirmation()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFinalDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Final Confirmation")
            .setMessage("Type 'DELETE' to confirm permanent account deletion:")
            .setView(EditText(requireContext()).apply {
                hint = "Type DELETE here"
            })
            .setPositiveButton("Delete Account") { dialog, _ ->
                val editText = (dialog as AlertDialog).findViewById<EditText>(android.R.id.text1)
                if (editText?.text?.toString() == "DELETE") {
                    deleteUserAccount()
                } else {
                    Toast.makeText(requireContext(), "Deletion cancelled - incorrect confirmation", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteUserAccount() {
        UserRepository.deleteUserAccount { success, error ->
            if (success) {
                Toast.makeText(requireContext(), "Account deleted successfully", Toast.LENGTH_SHORT).show()
                val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                requireActivity().finish()
            } else {
                Toast.makeText(requireContext(), "Failed to delete account: $error", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh profile data when returning to fragment
        loadUserProfile()
    }
}