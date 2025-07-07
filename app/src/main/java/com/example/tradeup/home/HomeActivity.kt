package com.example.tradeup.home

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.tradeup.R
import com.example.tradeup.auth.LoginActivity
import com.example.tradeup.data.remote.FirebaseAuthHelper
import com.example.tradeup.profile.ProfileActivity

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val user = FirebaseAuthHelper.getCurrentUser()
        val welcomeText = findViewById<TextView>(R.id.tvWelcome)
        val logoutBtn = findViewById<Button>(R.id.btnLogout)
        val btnEditProfile = findViewById<Button>(R.id.btnEditProfile)

        welcomeText.text = "Welcome, ${user?.email ?: "Guest"}"

        logoutBtn.setOnClickListener {
            FirebaseAuthHelper.logout()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnEditProfile.setOnClickListener {
            // Mình đã import ProfileActivity ngay bên dưới
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }
}
