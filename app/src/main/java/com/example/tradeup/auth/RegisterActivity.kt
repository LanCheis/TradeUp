package com.example.tradeup.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.tradeup.MainActivity
import com.example.tradeup.R
import com.example.tradeup.data.remote.FirebaseAuthHelper
import com.example.tradeup.onboarding.SetupProfileActivity
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var tvGoToLogin: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmailVerification: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        etEmail = findViewById(R.id.etRegisterEmail)
        etPassword = findViewById(R.id.etRegisterPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        tvGoToLogin = findViewById(R.id.tvGoToLogin)
        progressBar = findViewById(R.id.progressBar)
        tvEmailVerification = findViewById(R.id.tvEmailVerification)
    }

    private fun setupClickListeners() {
        btnRegister.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (validateInput(email, password, confirmPassword)) {
                registerUser(email, password)
            }
        }

        tvGoToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun validateInput(email: String, password: String, confirmPassword: String): Boolean {
        // Email validation
        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            etEmail.requestFocus()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Please enter a valid email"
            etEmail.requestFocus()
            return false
        }

        // Password validation
        if (password.isEmpty()) {
            etPassword.error = "Password is required"
            etPassword.requestFocus()
            return false
        }

        if (password.length < 6) {
            etPassword.error = "Password must be at least 6 characters"
            etPassword.requestFocus()
            return false
        }

        // Password pattern validation
        if (!isPasswordValid(password)) {
            etPassword.error = "Password must contain at least one letter and one number"
            etPassword.requestFocus()
            return false
        }

        // Confirm password validation
        if (confirmPassword != password) {
            etConfirmPassword.error = "Passwords do not match"
            etConfirmPassword.requestFocus()
            return false
        }

        return true
    }

    private fun isPasswordValid(password: String): Boolean {
        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }
        return hasLetter && hasDigit
    }

    private fun registerUser(email: String, password: String) {
        showLoading(true)

        FirebaseAuthHelper.registerWithEmail(email, password) { success, error ->
            showLoading(false)

            if (success) {
                // Send email verification
                sendEmailVerification()
            } else {
                Toast.makeText(this, error ?: "Registration failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendEmailVerification() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.sendEmailVerification()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                showEmailVerificationUI()
                Toast.makeText(this, "Verification email sent to ${user.email}", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Failed to send verification email", Toast.LENGTH_SHORT).show()
                // Still proceed to profile setup
                navigateToSetupProfile()
            }
        }
    }

    private fun showEmailVerificationUI() {
        tvEmailVerification.text = "📧 Please check your email and verify your account before proceeding."
        tvEmailVerification.visibility = android.view.View.VISIBLE

        btnRegister.text = "Email Sent - Continue"
        btnRegister.setOnClickListener {
            checkEmailVerificationAndProceed()
        }
    }

    private fun checkEmailVerificationAndProceed() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.reload()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                if (user.isEmailVerified) {
                    Toast.makeText(this, "Email verified successfully!", Toast.LENGTH_SHORT).show()
                    navigateToSetupProfile()
                } else {
                    Toast.makeText(this, "Please verify your email first", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun navigateToSetupProfile() {
        startActivity(Intent(this, SetupProfileActivity::class.java))
        finish()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        btnRegister.isEnabled = !show
    }
}