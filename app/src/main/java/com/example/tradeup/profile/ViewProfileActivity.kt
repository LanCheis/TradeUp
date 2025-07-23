package com.example.tradeup.profile

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tradeup.R
import com.example.tradeup.data.model.UserRating
import com.example.tradeup.data.remote.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class ViewProfileActivity : AppCompatActivity() {

    private lateinit var ivAvatar: ImageView
    private lateinit var tvDisplayName: TextView
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvBio: TextView
    private lateinit var tvRating: TextView
    private lateinit var tvJoinedDate: TextView
    private lateinit var tvTransactions: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var btnRateUser: Button // ✅ NEW: Rate this user button
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutProfile: LinearLayout
    private lateinit var rvReviews: RecyclerView // ✅ NEW: Reviews list
    private lateinit var tvReviewsTitle: TextView

    private var targetUserId: String = ""
    private val reviewsList = mutableListOf<UserRating>()
    private lateinit var reviewsAdapter: UserReviewsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_profile)

        targetUserId = intent.getStringExtra("USER_ID") ?: ""
        if (targetUserId.isEmpty()) {
            Toast.makeText(this, "❌ User not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupClickListeners()
        setupReviewsRecyclerView()
        loadUserProfile(targetUserId)
        loadUserReviews(targetUserId)
    }

    private fun initViews() {
        ivAvatar = findViewById(R.id.ivAvatar)
        tvDisplayName = findViewById(R.id.tvDisplayName)
        tvName = findViewById(R.id.tvName)
        tvEmail = findViewById(R.id.tvEmail)
        tvPhone = findViewById(R.id.tvPhone)
        tvBio = findViewById(R.id.tvBio)
        tvRating = findViewById(R.id.tvRating)
        tvJoinedDate = findViewById(R.id.tvJoinedDate) // ✅ NEW
        tvTransactions = findViewById(R.id.tvTransactions)
        btnBack = findViewById(R.id.btnBack)
        btnRateUser = findViewById(R.id.btnRateUser) // ✅ NEW
        progressBar = findViewById(R.id.progressBar)
        layoutProfile = findViewById(R.id.layoutProfile)
        rvReviews = findViewById(R.id.rvReviews) // ✅ NEW
        tvReviewsTitle = findViewById(R.id.tvReviewsTitle) // ✅ NEW

        // Hide profile initially, show loading
        layoutProfile.visibility = android.view.View.GONE
        progressBar.visibility = android.view.View.VISIBLE
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        // ✅ NEW: Rate user functionality
        btnRateUser.setOnClickListener {
            showRateUserDialog()
        }
    }

    // ✅ NEW: Setup reviews RecyclerView
    private fun setupReviewsRecyclerView() {
        reviewsAdapter = UserReviewsAdapter(reviewsList)
        rvReviews.layoutManager = LinearLayoutManager(this)
        rvReviews.adapter = reviewsAdapter
    }

    // ✅ NEW: Show rating dialog
    private fun showRateUserDialog() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null || currentUserId == targetUserId) {
            Toast.makeText(this, "❌ Cannot rate yourself", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_rate_user, null)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)
        val etComment = dialogView.findViewById<EditText>(R.id.etComment)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Rate this user")
            .setView(dialogView)
            .setPositiveButton("Submit") { _, _ ->
                val rating = ratingBar.rating
                val comment = etComment.text.toString().trim()

                if (rating > 0) {
                    submitUserRating(targetUserId, rating, comment)
                } else {
                    Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ✅ NEW: Submit user rating
    private fun submitUserRating(targetUserId: String, stars: Float, comment: String) {
        btnRateUser.isEnabled = false
        btnRateUser.text = "Submitting..."

        UserRepository.submitRating(targetUserId, stars) { success ->
            btnRateUser.isEnabled = true
            btnRateUser.text = "Rate User"

            if (success) {
                // Also save detailed review
                saveDetailedReview(targetUserId, stars, comment)
                Toast.makeText(this, "✅ Rating submitted!", Toast.LENGTH_SHORT).show()
                // Reload profile to show updated rating
                loadUserProfile(targetUserId)
                loadUserReviews(targetUserId)
            } else {
                Toast.makeText(this, "❌ Failed to submit rating", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ✅ NEW: Save detailed review
    private fun saveDetailedReview(targetUserId: String, stars: Float, comment: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        val review = UserRating(
            id = UUID.randomUUID().toString(),
            fromUserId = currentUser.uid,
            toUserId = targetUserId,
            stars = stars,
            comment = comment,
            timestamp = System.currentTimeMillis()
        )

        FirebaseFirestore.getInstance()
            .collection("user_reviews")
            .document(review.id)
            .set(review)
    }

    // ✅ NEW: Load user reviews
    private fun loadUserReviews(userId: String) {
        FirebaseFirestore.getInstance()
            .collection("user_reviews")
            .whereEqualTo("toUserId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { documents ->
                reviewsList.clear()
                for (doc in documents) {
                    val review = doc.toObject(UserRating::class.java)
                    reviewsList.add(review)
                }

                reviewsAdapter.notifyDataSetChanged()

                // Update reviews title
                tvReviewsTitle.text = if (reviewsList.isNotEmpty()) {
                    "Recent Reviews (${reviewsList.size})"
                } else {
                    "No reviews yet"
                }
            }
            .addOnFailureListener {
                tvReviewsTitle.text = "Unable to load reviews"
            }
    }

    private fun loadUserProfile(userId: String) {
        UserRepository.getUserProfile(userId) { user ->
            progressBar.visibility = android.view.View.GONE

            if (user != null) {
                layoutProfile.visibility = android.view.View.VISIBLE

                // Update UI with user data
                tvDisplayName.text = user.name.ifEmpty { "User" }
                tvName.text = user.name.ifEmpty { "Not provided" }
                tvEmail.text = if (user.email.isNotEmpty()) user.email else "Not provided"
                tvPhone.text = if (user.phone.isNotEmpty()) user.phone else "Not provided"
                tvBio.text = user.bio.ifEmpty { "No bio yet" }

                // ✅ UPDATED: Enhanced rating display
                if (user.ratingCount > 0) {
                    tvRating.text = "⭐ ${String.format("%.1f", user.rating)} (${user.ratingCount} reviews)"
                } else {
                    tvRating.text = "⭐ No ratings yet"
                }

                // ✅ NEW: Show join date
                val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                tvJoinedDate.text = "Joined ${dateFormat.format(Date(user.joinedDate))}"

                // Transactions count
                tvTransactions.text = "${user.totalTransactions} completed transactions"

                // Load profile image
                if (user.profileImageUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(user.profileImageUrl)
                        .error(R.drawable.ic_avatar_placeholder)
                        .into(ivAvatar)
                }

                // Update title
                supportActionBar?.title = "${user.name}'s Profile"

                // ✅ Hide rate button if viewing own profile
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                btnRateUser.visibility = if (currentUserId == userId) {
                    android.view.View.GONE
                } else {
                    android.view.View.VISIBLE
                }
            } else {
                Toast.makeText(this, "❌ Failed to load user profile", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}