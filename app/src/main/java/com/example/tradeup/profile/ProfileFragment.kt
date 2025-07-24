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
import com.example.tradeup.data.model.User
import com.example.tradeup.offers.MyOffersActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
    private lateinit var tvJoinedDate: TextView
    private lateinit var tvRating: TextView
    private lateinit var tvTotalTransactions: TextView
    private lateinit var ratingBar: RatingBar
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

        // Rating views (add these to layout if missing)
        try {
            tvRating = view.findViewById(R.id.tvRating)
            tvTotalTransactions = view.findViewById(R.id.tvTotalTransactions)
            ratingBar = view.findViewById(R.id.ratingBar)
        } catch (e: Exception) {
            // Views don't exist in layout, that's okay
        }

        btnLogout = view.findViewById(R.id.btnLogout)
        btnEditProfile = view.findViewById(R.id.btnEditProfile)
        btnDeleteAccount = view.findViewById(R.id.btnDeleteAccount)
        btnMyOffers = view.findViewById(R.id.btnMyOffers)
    }

    private fun setupClickListeners() {
        // Edit Profile Button
        btnEditProfile.setOnClickListener {
            Toast.makeText(context, "Edit Profile feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        // My Offers Button
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

        // 🆕 Use the updated UserRepository method
        UserRepository.getUserProfile(currentUser.uid) { user ->
            if (user != null) {
                updateUIWithUserData(user)
            } else {
                // If no Firestore data, use Firebase Auth data
                updateUIWithAuthData(currentUser)
            }
        }
    }

    private fun updateUIWithUserData(user: User) {
        tvDisplayName.text = user.name.ifEmpty { "No Name" }
        tvName.text = user.name.ifEmpty { "No Name Set" }
        tvEmail.text = user.email.ifEmpty { "No Email" }
        tvPhone.text = user.phone.ifEmpty { "No Phone" }
        tvAddress.text = user.address.ifEmpty { "No Address" }
        tvBio.text = user.bio.ifEmpty { "No Bio" }
        tvUsername.text = user.username.ifEmpty { "No Username" }
        tvGender.text = user.gender.ifEmpty { "Not Specified" }
        tvBirthday.text = user.birthday.ifEmpty { "Not Set" }
        tvInterest.text = user.interests.ifEmpty { "No Interests" }

        // Format join date
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        tvJoinedDate.text = "Joined ${dateFormat.format(Date(user.joinedDate))}"

        // Update rating info if views exist
        try {
            tvRating.text = String.format("%.1f", user.rating)
            tvTotalTransactions.text = "${user.totalTransactions} transactions"
            ratingBar.rating = user.rating.toFloat()
        } catch (e: Exception) {
            // Rating views don't exist
        }

        // Load profile image
        if (user.profileImageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(user.profileImageUrl)
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .into(ivAvatar)
        }
    }

    private fun updateUIWithAuthData(user: com.google.firebase.auth.FirebaseUser) {
        tvDisplayName.text = user.displayName ?: "Unknown User"
        tvEmail.text = user.email ?: "No email"
        tvName.text = user.displayName ?: "Unknown User"
        tvJoinedDate.text = "Recently Joined"

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
        val input = EditText(requireContext())
        input.hint = "Type 'DELETE' to confirm"
        input.setPadding(50, 20, 50, 20)

        AlertDialog.Builder(requireContext())
            .setTitle("⚠️ Final Confirmation")
            .setMessage("Type 'DELETE' to confirm account deletion")
            .setView(input)
            .setPositiveButton("Delete Account") { _, _ ->
                if (input.text.toString().uppercase() == "DELETE") {
                    performDeleteAccount()
                } else {
                    Toast.makeText(context, "Please type 'DELETE' to confirm", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        try {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
            redirectToLogin()
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

        // Use the updated UserRepository method
        UserRepository.deleteUserData(currentUser.uid) { success ->
            if (success) {
                currentUser.delete().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(context, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                        redirectToLogin()
                    } else {
                        Toast.makeText(context, "Failed to delete account: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(context, "Failed to delete user data", Toast.LENGTH_SHORT).show()
            }
        }
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