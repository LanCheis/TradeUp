package com.example.tradeup.profile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.tradeup.R
import com.example.tradeup.data.remote.UserRepository
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : AppCompatActivity() {

    private lateinit var ivAvatar: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvBio: TextView
    private lateinit var tvUsername: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvGender: TextView
    private lateinit var tvBirthday: TextView
    private lateinit var tvInterest: TextView
    private lateinit var btnEditProfile: Button

    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        ivAvatar = findViewById(R.id.ivAvatar)
        tvName = findViewById(R.id.tvName)
        tvPhone = findViewById(R.id.tvPhone)
        tvAddress = findViewById(R.id.tvAddress)
        tvBio = findViewById(R.id.tvBio)
        tvUsername = findViewById(R.id.tvUsername)
        tvEmail = findViewById(R.id.tvEmail)
        tvGender = findViewById(R.id.tvGender)
        tvBirthday = findViewById(R.id.tvBirthday)
        tvInterest = findViewById(R.id.tvInterest)
        btnEditProfile = findViewById(R.id.btnEditProfile)

        btnEditProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        loadUser()
    }

    override fun onResume() {
        super.onResume()
        loadUser()
    }

    private fun loadUser() {
        UserRepository.getUserProfile(currentUid) { user ->
            user?.let {
                tvName.text = it.name
                tvPhone.text = it.phone
                tvAddress.text = it.address
                tvBio.text = it.bio
                tvUsername.text = it.username
                tvEmail.text = "Email: ${it.email}"
                tvGender.text = it.gender
                tvBirthday.text = it.birthday
                tvInterest.text = it.interests

                if (!it.profileImageUrl.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(it.profileImageUrl)
                        .placeholder(R.drawable.ic_person)
                        .into(ivAvatar)
                } else {
                    ivAvatar.setImageResource(R.drawable.ic_person)
                }
            } ?: run {
                Toast.makeText(this, "Không tìm thấy hồ sơ người dùng", Toast.LENGTH_SHORT).show()
                Log.e("ProfileActivity", "User profile not found")
            }
        }
    }
}
