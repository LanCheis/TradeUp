package com.example.tradeup.profile

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.tradeup.R
import com.example.tradeup.data.remote.UserRepository
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch

class ViewPublicProfileActivity : AppCompatActivity() {

    private lateinit var userRepository: UserRepository

    // Views
    private lateinit var ivBackButton: ImageView
    private lateinit var ivProfilePic: CircleImageView
    private lateinit var tvDisplayName: TextView
    private lateinit var tvBio: TextView
    private lateinit var tvMemberSince: TextView
    private lateinit var ratingBar: RatingBar
    private lateinit var tvRatingText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var scrollView: ScrollView

    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_public_profile)

        userRepository = UserRepository()

        // Get user ID from intent
        userId = intent.getStringExtra("user_id") ?: ""
        if (userId.isEmpty()) {
            Toast.makeText(this, "Invalid user profile", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupClickListeners()
        loadUserProfile()
    }

    private fun initViews() {
        ivBackButton = findViewById(R.id.ivBackButton)
        ivProfilePic = findViewById(R.id.ivProfilePic)
        tvDisplayName = findViewById(R.id.tvDisplayName)
        tvBio = findViewById(R.id.tvBio)
        tvMemberSince = findViewById(R.id.tvMemberSince)
        ratingBar = findViewById(R.id.ratingBar)
        tvRatingText = findViewById(R.id.tvRatingText)
        progressBar = findViewById(R.id.progressBar)
        scrollView = findViewById(R.id.scrollView)
    }

    private fun setupClickListeners() {
        ivBackButton.setOnClickListener {
            finish()
        }
    }

    private fun loadUserProfile() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val userProfile = userRepository.getUserProfile(userId)

                if (userProfile != null) {
                    // FR-1.2.4: Display public profile information
                    tvDisplayName.text = userProfile.displayName.ifEmpty { "Anonymous User" }
                    tvBio.text = userProfile.bio.ifEmpty { "No bio available" }

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

                    // Load profile picture
                    if (userProfile.profilePictureUrl.isNotEmpty()) {
                        Glide.with(this@ViewPublicProfileActivity)
                            .load(userProfile.profilePictureUrl)
                            .placeholder(R.drawable.ic_avatar_placeholder)
                            .error(R.drawable.ic_avatar_placeholder)
                            .into(ivProfilePic)
                    }

                } else {
                    Toast.makeText(this@ViewPublicProfileActivity, "User profile not found", Toast.LENGTH_SHORT).show()
                    finish()
                }

                showLoading(false)

            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(this@ViewPublicProfileActivity, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        scrollView.visibility = if (show) View.GONE else View.VISIBLE
    }
}