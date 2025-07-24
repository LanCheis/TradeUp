package com.example.tradeup

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.tradeup.auth.LoginActivity
import com.example.tradeup.data.remote.UserRepository
import com.example.tradeup.profile.ProfileFragment
import com.example.tradeup.utils.CloudinaryHelper
import com.example.tradeup.utils.ProfileValidator
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var userRepository: UserRepository

    // Fragments
    private val profileFragment = ProfileFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Cloudinary
        CloudinaryHelper.initialize(this)

        auth = FirebaseAuth.getInstance()
        userRepository = UserRepository()

        checkAuthenticationAndProfile()
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun checkAuthenticationAndProfile() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            redirectToLogin()
            return
        }

        lifecycleScope.launch {
            try {
                val userProfile = userRepository.getUserProfile(currentUser.uid)

                if (ProfileValidator.isProfileComplete(userProfile)) {
                    // Profile is complete - load profile fragment by default
                    loadFragment(profileFragment)
                } else {
                    // Profile incomplete - redirect to setup
                    val missingFields = ProfileValidator.getMissingFields(userProfile)
                    val message = "Complete your profile to continue:\n• ${missingFields.joinToString("\n• ")}"

                    Toast.makeText(this@MainActivity, "Profile setup required", Toast.LENGTH_LONG).show()
                    redirectToProfileSetup(message)
                }

            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
                redirectToLogin()
            }
        }
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    private fun redirectToProfileSetup(message: String = "") {
        try {
            // Try to find SetupProfileActivity
            val setupClass = Class.forName("com.example.tradeup.onboarding.SetupProfileActivity")
            val intent = Intent(this, setupClass)
            if (message.isNotEmpty()) {
                intent.putExtra("incomplete_message", message)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        } catch (e: ClassNotFoundException) {
            // SetupProfileActivity doesn't exist, just show profile
            Toast.makeText(this, "Please complete your profile", Toast.LENGTH_LONG).show()
            loadFragment(profileFragment)
        }
    }
}