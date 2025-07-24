package com.example.tradeup.profile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
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

    companion object {
        private const val EDIT_PROFILE_REQUEST = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        auth = FirebaseAuth.getInstance()
        userRepository = UserRepository()

        initViews(view)
        setupClickListeners()
        loadUserProfile()

        return view
    }

    private fun initViews(view: View) {
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
    }

    private fun setupClickListeners() {
        // FR-1.2.2: Edit profile with result callback
        btnEditProfile.setOnClickListener {
            val intent = Intent(requireContext(), EditProfileActivity::class.java)
            startActivityForResult(intent, EDIT_PROFILE_REQUEST)
        }

        // FR-1.2.3: Account settings
        btnAccountSettings.setOnClickListener {
            val intent = Intent(requireContext(), AccountSettingsActivity::class.java)
            startActivity(intent)
        }

        // FR-1.1.5: Logout
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
                Log.d("ProfileFragment", "🔍 Loading profile for user: ${currentUser.uid}")
                val userProfile = userRepository.getUserProfile(currentUser.uid)

                if (userProfile != null) {
                    Log.d("ProfileFragment", "✅ Profile loaded successfully")
                    Log.d("ProfileFragment", "🔍 Profile picture URL: '${userProfile.profilePictureUrl}'")
                    Log.d("ProfileFragment", "🔍 Updated timestamp: ${userProfile.updatedAt.seconds}")

                    // FR-1.2.1: Display all profile information
                    tvDisplayName.text = userProfile.displayName.ifEmpty { "No name set" }
                    tvBio.text = userProfile.bio.ifEmpty { "No bio added yet" }
                    tvPhone.text = userProfile.phoneNumber.ifEmpty { "No phone number" }
                    tvAddress.text = userProfile.address.ifEmpty { "No address set" }

                    // Format member since date
                    val memberSince = java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault())
                        .format(userProfile.createdAt.toDate())
                    tvMemberSince.text = "Member since $memberSince"

                    // FR-1.2.1: Display rating
                    if (userProfile.reviewCount > 0) {
                        ratingBar.rating = userProfile.rating.toFloat()
                        tvRatingText.text = "${String.format("%.1f", userProfile.rating)} (${userProfile.reviewCount} reviews)"
                    } else {
                        ratingBar.rating = 0f
                        tvRatingText.text = "No reviews yet"
                    }

                    // 🔧 SIMPLIFIED: Load profile picture
                    loadProfileImage(userProfile.profilePictureUrl, userProfile.updatedAt.seconds)

                } else {
                    Log.e("ProfileFragment", "❌ Profile not found in database")
                    Toast.makeText(context, "Profile not found", Toast.LENGTH_SHORT).show()
                }

                showLoading(false)

            } catch (e: Exception) {
                showLoading(false)
                Log.e("ProfileFragment", "❌ Error loading profile: ${e.message}")
                Toast.makeText(context, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 🔧 SIMPLIFIED: Profile image loading without complex listener
    // 🔧 IMPROVED: Better error handling and logging
    private fun loadProfileImage(imageUrl: String, timestamp: Long) {
        Log.d("ProfileFragment", "🔍 loadProfileImage called:")
        Log.d("ProfileFragment", "     - imageUrl: '$imageUrl'")
        Log.d("ProfileFragment", "     - timestamp: $timestamp")

        if (imageUrl.isNotEmpty()) {
            Log.d("ProfileFragment", "📸 Loading image from URL")
            Glide.with(this)
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(false)
                .signature(ObjectKey(timestamp))
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: com.bumptech.glide.load.engine.GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e("ProfileFragment", "❌ Glide load failed: ${e?.message}")
                        return false
                    }

                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                        dataSource: com.bumptech.glide.load.DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.d("ProfileFragment", "✅ Image loaded successfully")
                        return false
                    }
                })
                .into(ivProfilePic)
        } else {
            Log.w("ProfileFragment", "⚠️ Image URL is empty, showing placeholder")
            ivProfilePic.setImageResource(R.drawable.ic_avatar_placeholder)
        }
    }

    private fun showLogoutConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        auth.signOut()
        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
        redirectToLogin()
    }

    private fun redirectToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        scrollView.visibility = if (show) View.GONE else View.VISIBLE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EDIT_PROFILE_REQUEST && resultCode == android.app.Activity.RESULT_OK) {
            Log.d("ProfileFragment", "🔄 Profile was updated, refreshing display...")
            loadUserProfile()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::auth.isInitialized) {
            Log.d("ProfileFragment", "🔄 Fragment resumed, reloading profile...")
            loadUserProfile()
        }
    }
}