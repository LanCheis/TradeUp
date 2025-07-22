// Create: app/src/main/java/com/example/tradeup/profile/ViewProfileActivity.kt

package com.example.tradeup.profile

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.tradeup.R
import com.example.tradeup.data.remote.UserRepository

class ViewProfileActivity : AppCompatActivity() {

    private lateinit var ivAvatar: ImageView
    private lateinit var tvDisplayName: TextView
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvBio: TextView
    private lateinit var tvRating: TextView
    private lateinit var tvTransactions: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutProfile: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_profile)

        val userId = intent.getStringExtra("USER_ID")
        if (userId == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupClickListeners()
        loadUserProfile(userId)
    }

    private fun initViews() {
        ivAvatar = findViewById(R.id.ivAvatar)
        tvDisplayName = findViewById(R.id.tvDisplayName)
        tvName = findViewById(R.id.tvName)
        tvEmail = findViewById(R.id.tvEmail)
        tvPhone = findViewById(R.id.tvPhone)
        tvBio = findViewById(R.id.tvBio)
        tvRating = findViewById(R.id.tvRating)
        tvTransactions = findViewById(R.id.tvTransactions)
        btnBack = findViewById(R.id.btnBack)
        progressBar = findViewById(R.id.progressBar)
        layoutProfile = findViewById(R.id.layoutProfile)

        // Hide profile initially, show loading
        layoutProfile.visibility = android.view.View.GONE
        progressBar.visibility = android.view.View.VISIBLE
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
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

                // Rating and transactions
                tvRating.text = "⭐ ${user.rating} (${user.totalTransactions} transactions)"
                tvTransactions.text = "${user.totalTransactions} completed transactions"

                // Load profile image
                if (user.profileImageUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(user.profileImageUrl)
                        .error(R.drawable.ic_avatar_placeholder)
                        .into(ivAvatar)
                }

                // Set title
                supportActionBar?.title = "${user.name}'s Profile"
            } else {
                Toast.makeText(this, "Failed to load user profile", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}