package com.example.tradeup.auth

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.tradeup.R
import com.google.firebase.auth.FirebaseAuth

// FR-1.1.3: Password recovery through email
class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    // Views
    private lateinit var etEmail: EditText
    private lateinit var btnSendReset: Button
    private lateinit var btnBackToLogin: TextView
    private lateinit var tvError: TextView
    private lateinit var tvSuccess: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var ivBackButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        initViews()
        setupTextWatchers()
        setupClickListeners()
    }

    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        btnSendReset = findViewById(R.id.btnSendReset)
        btnBackToLogin = findViewById(R.id.btnBackToLogin)
        tvError = findViewById(R.id.tvError)
        tvSuccess = findViewById(R.id.tvSuccess)
        progressBar = findViewById(R.id.progressBar)
        ivBackButton = findViewById(R.id.ivBackButton)

        // FR-1.1.4: Button disabled initially
        btnSendReset.isEnabled = false
        tvError.visibility = View.GONE
        tvSuccess.visibility = View.GONE
    }

    private fun setupTextWatchers() {
        etEmail.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateEmailField()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    // FR-1.1.4: Enable button only when email is valid
    private fun validateEmailField() {
        val email = etEmail.text.toString().trim()
        val isValidEmail = email.isNotEmpty() &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

        btnSendReset.isEnabled = isValidEmail

        // Visual feedback for email validation
        if (email.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setBackgroundResource(R.drawable.edittext_error_background)
        } else {
            etEmail.setBackgroundResource(R.drawable.edittext_background)
        }
    }

    private fun setupClickListeners() {
        btnSendReset.setOnClickListener {
            sendPasswordReset()
        }

        btnBackToLogin.setOnClickListener {
            finish() // Go back to previous activity (LoginActivity)
        }

        ivBackButton.setOnClickListener {
            finish()
        }
    }

    // FR-1.1.3: Send password reset email
    private fun sendPasswordReset() {
        val email = etEmail.text.toString().trim()

        if (!validateInput(email)) return

        showProgress(true)
        hideMessages()

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                showProgress(false)

                if (task.isSuccessful) {
                    // FR-1.1.3: Success - show confirmation message
                    showSuccess("Password reset email sent to $email\nCheck your inbox and follow the instructions.")

                    // Optionally disable the button to prevent spam
                    btnSendReset.isEnabled = false
                    btnSendReset.text = "Email Sent"

                } else {
                    // Handle different error cases
                    val error = when (task.exception?.message) {
                        "There is no user record corresponding to this identifier. The user may have been deleted." ->
                            "No account found with this email address."
                        "The email address is badly formatted." ->
                            "Please enter a valid email address."
                        else ->
                            task.exception?.message ?: "Failed to send reset email. Please try again."
                    }
                    showError(error)
                }
            }
    }

    private fun validateInput(email: String): Boolean {
        when {
            email.isEmpty() -> {
                showError("Please enter your email address")
                return false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                showError("Please enter a valid email address")
                return false
            }
        }
        return true
    }

    private fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnSendReset.isEnabled = !show && etEmail.text.toString().trim().isNotEmpty()
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
        tvSuccess.visibility = View.GONE
    }

    private fun showSuccess(message: String) {
        tvSuccess.text = message
        tvSuccess.visibility = View.VISIBLE
        tvError.visibility = View.GONE
    }

    private fun hideMessages() {
        tvError.visibility = View.GONE
        tvSuccess.visibility = View.GONE
    }
}