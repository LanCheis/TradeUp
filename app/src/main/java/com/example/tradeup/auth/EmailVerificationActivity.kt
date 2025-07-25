package com.example.tradeup.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tradeup.R
import com.example.tradeup.data.remote.UserRepository
import com.example.tradeup.onboarding.SetupProfileActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

// FR-1.1.2: Email verification required for account activation
class EmailVerificationActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var userRepository: UserRepository
    private lateinit var checkHandler: Handler
    private lateinit var checkRunnable: Runnable

    // Views
    private lateinit var tvEmail: TextView
    private lateinit var btnResendEmail: Button
    private lateinit var btnCheckVerification: Button
    private lateinit var btnSkipForNow: TextView
    private lateinit var tvError: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var ivBackButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_verification)

        auth = FirebaseAuth.getInstance()
        userRepository = UserRepository()

        initViews()
        setupClickListeners()
        startAutoCheck()
    }

    private fun initViews() {
        tvEmail = findViewById(R.id.tvEmail)
        btnResendEmail = findViewById(R.id.btnResendEmail)
        btnCheckVerification = findViewById(R.id.btnCheckVerification)
        btnSkipForNow = findViewById(R.id.btnSkipForNow)
        tvError = findViewById(R.id.tvError)
        progressBar = findViewById(R.id.progressBar)
        ivBackButton = findViewById(R.id.ivBackButton)

        // Display current user's email
        val userEmail = auth.currentUser?.email ?: "your email"
        tvEmail.text = "We've sent a verification link to:\n$userEmail"

        tvError.visibility = View.GONE
    }

    private fun setupClickListeners() {
        btnResendEmail.setOnClickListener {
            resendVerificationEmail()
        }

        btnCheckVerification.setOnClickListener {
            checkEmailVerification()
        }

        btnSkipForNow.setOnClickListener {
            // Allow user to skip for now but they'll need to verify later
            proceedToProfileSetup()
        }

        ivBackButton.setOnClickListener {
            // Sign out and go back to login
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }
    }

    // FR-1.1.2: Resend verification email
    private fun resendVerificationEmail() {
        showProgress(true)
        btnResendEmail.isEnabled = false

        auth.currentUser?.sendEmailVerification()
            ?.addOnCompleteListener { task ->
                showProgress(false)

                if (task.isSuccessful) {
                    Toast.makeText(this, "Verification email sent!", Toast.LENGTH_SHORT).show()

                    // Disable button for 60 seconds to prevent spam
                    disableResendButton()
                } else {
                    btnResendEmail.isEnabled = true
                    showError("Failed to send email: ${task.exception?.message}")
                }
            }
    }

    private fun disableResendButton() {
        var countdown = 60
        btnResendEmail.text = "Resend in ${countdown}s"

        val timer = object : Runnable {
            override fun run() {
                countdown--
                if (countdown > 0) {
                    btnResendEmail.text = "Resend in ${countdown}s"
                    Handler(Looper.getMainLooper()).postDelayed(this, 1000)
                } else {
                    btnResendEmail.text = "Resend Email"
                    btnResendEmail.isEnabled = true
                }
            }
        }
        Handler(Looper.getMainLooper()).postDelayed(timer, 1000)
    }

    // FR-1.1.2: Check if email is verified
    private fun checkEmailVerification() {
        showProgress(true)

        auth.currentUser?.reload()?.addOnCompleteListener { task ->
            showProgress(false)

            if (task.isSuccessful) {
                val isVerified = auth.currentUser?.isEmailVerified ?: false

                if (isVerified) {
                    // Update user profile to mark email as verified
                    updateUserEmailVerification()
                } else {
                    showError("Email not verified yet. Please check your inbox and click the verification link.")
                }
            } else {
                showError("Failed to check verification status: ${task.exception?.message}")
            }
        }
    }

    private fun updateUserEmailVerification() {
        val currentUser = auth.currentUser ?: return

        lifecycleScope.launch {
            try {
                val userProfile = userRepository.getUserProfile(currentUser.uid)
                userProfile?.let { user ->
                    val updatedUser = user.copy(emailVerified = true)
                    val success = userRepository.saveUserProfile(updatedUser)

                    if (success) {
                        Toast.makeText(this@EmailVerificationActivity,
                            "Email verified successfully!", Toast.LENGTH_SHORT).show()
                        proceedToProfileSetup()
                    } else {
                        showError("Failed to update profile. Please try again.")
                    }
                }
            } catch (e: Exception) {
                showError("Error updating profile: ${e.message}")
            }
        }
    }

    // Start automatic checking every 5 seconds
    private fun startAutoCheck() {
        checkHandler = Handler(Looper.getMainLooper())
        checkRunnable = object : Runnable {
            override fun run() {
                auth.currentUser?.reload()?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val isVerified = auth.currentUser?.isEmailVerified ?: false
                        if (isVerified) {
                            updateUserEmailVerification()
                            return@addOnCompleteListener
                        }
                    }
                    // Check again in 5 seconds
                    checkHandler.postDelayed(this, 5000)
                }
            }
        }
        // Start checking after 3 seconds
        checkHandler.postDelayed(checkRunnable, 3000)
    }

    private fun proceedToProfileSetup() {
        stopAutoCheck()
        startActivity(Intent(this, SetupProfileActivity::class.java))
        finish()
    }

    private fun stopAutoCheck() {
        if (::checkHandler.isInitialized && ::checkRunnable.isInitialized) {
            checkHandler.removeCallbacks(checkRunnable)
        }
    }

    private fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAutoCheck()
    }
}