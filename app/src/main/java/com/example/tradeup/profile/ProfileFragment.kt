package com.example.tradeup.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.example.tradeup.R
import com.example.tradeup.auth.LoginActivity
import com.example.tradeup.data.remote.UserRepository
import com.google.firebase.auth.FirebaseAuth
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var userRepository: UserRepository

    // Views
    private lateinit var ivProfilePic: CircleImageView
    private lateinit var tvDisplayName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvBio: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvMemberSince: TextView
    private lateinit var ratingBar: RatingBar
    private lateinit var tvRatingText: TextView
    private lateinit var btnEditProfile: Button
    private lateinit var btnAccountSettings: Button
    private lateinit var btnLogout: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var scrollView: ScrollView

    // FR-1.2.1: Modern activity result launcher
    private lateinit var editProfileLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        auth = FirebaseAuth.getInstance()
        userRepository = UserRepository()

        // Initialize activity launchers BEFORE using them
        initializeActivityLaunchers()

        initViews(view)
        setupClickListeners()
        loadUserProfile()

        return view
    }

    /**
     * Initialize modern activity result launchers
     */
    private fun initializeActivityLaunchers() {
        editProfileLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                // Profile was updated, refresh the display
                loadUserProfile()
            }
        }
    }

    private fun initViews(view: View) {
        try {
            ivProfilePic = view.findViewById(R.id.ivProfilePic)
            tvDisplayName = view.findViewById(R.id.tvDisplayName)
            tvEmail = view.findViewById(R.id.tvEmail)
            tvBio = view.findViewById(R.id.tvBio)
            tvPhone = view.findViewById(R.id.tvPhone)
            tvAddress = view.findViewById(R.id.tvAddress)
            tvMemberSince = view.findViewById(R.id.tvMemberSince)
            ratingBar = view.findViewById(R.id.ratingBar)
            tvRatingText = view.findViewById(R.id.tvRatingText)
            btnEditProfile = view.findViewById(R.id.btnEditProfile)
            btnAccountSettings = view.findViewById(R.id.btnAccountSettings)
            btnLogout = view.findViewById(R.id.btnLogout)
            progressBar = view.findViewById(R.id.progressBar)
            scrollView = view.findViewById(R.id.scrollView)
        } catch (e: Exception) {
            Toast.makeText(context, "Error initializing views: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickListeners() {
        // Edit profile
        btnEditProfile.setOnClickListener {
            val intent = Intent(requireContext(), EditProfileActivity::class.java)
            editProfileLauncher.launch(intent)
        }

        // Account settings - graceful fallback if activity doesn't exist
        btnAccountSettings.setOnClickListener {
            try {
                val intent = Intent(requireContext(), AccountSettingsActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Account settings coming soon!", Toast.LENGTH_SHORT).show()
            }
        }

        // Logout with confirmation
        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            redirectToLogin()
            return
        }

        showLoading(true)

        // Set basic info
        tvEmail.text = currentUser.email ?: "No email"

        lifecycleScope.launch {
            try {
                val userProfile = userRepository.getUserProfile(currentUser.uid)

                if (userProfile != null) {
                    // Display all profile information
                    tvDisplayName.text = userProfile.displayName.ifEmpty { "No name set" }
                    tvBio.text = userProfile.bio.ifEmpty { "No bio added yet" }
                    tvPhone.text = userProfile.phoneNumber.ifEmpty { "No phone number" }
                    tvAddress.text = userProfile.address.ifEmpty { "No address set" }

                    // Format member since date
                    val memberSince = java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault())
                        .format(userProfile.createdAt.toDate())
                    tvMemberSince.text = "Member since $memberSince"

                    // Display rating
                    if (userProfile.reviewCount > 0) {
                        ratingBar.rating = userProfile.rating.toFloat()
                        tvRatingText.text = "${String.format("%.1f", userProfile.rating)} (${userProfile.reviewCount} reviews)"
                    } else {
                        ratingBar.rating = 0f
                        tvRatingText.text = "No reviews yet"
                    }

                    // Load profile picture with cache busting
                    loadProfileImage(userProfile.profilePictureUrl, userProfile.updatedAt.seconds)

                } else {
                    Toast.makeText(context, "Profile not found", Toast.LENGTH_SHORT).show()
                }

                showLoading(false)

            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(context, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Load profile image with cache control
    private fun loadProfileImage(imageUrl: String, timestamp: Long) {
        try {
            if (imageUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(false)
                    .signature(ObjectKey(timestamp))
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .error(R.drawable.ic_avatar_placeholder)
                    .into(ivProfilePic)
            } else {
                ivProfilePic.setImageResource(R.drawable.ic_avatar_placeholder)
            }
        } catch (e: Exception) {
            ivProfilePic.setImageResource(R.drawable.ic_avatar_placeholder)
        }
    }

    private fun showLogoutConfirmation() {
        try {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout") { _, _ ->
                    performLogout()
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            performLogout()
        }
    }

    private fun performLogout() {
        try {
            auth.signOut()
            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
            redirectToLogin()
        } catch (e: Exception) {
            Toast.makeText(context, "Error during logout: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun redirectToLogin() {
        try {
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        } catch (e: Exception) {
            Toast.makeText(context, "Error redirecting to login", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoading(show: Boolean) {
        try {
            progressBar.visibility = if (show) View.VISIBLE else View.GONE
            scrollView.visibility = if (show) View.GONE else View.VISIBLE
        } catch (e: Exception) {
            // Ignore view errors during loading state changes
        }
    }

    // Refresh when fragment becomes visible
    override fun onResume() {
        super.onResume()
        if (::auth.isInitialized) {
            loadUserProfile()
        }
    }
}