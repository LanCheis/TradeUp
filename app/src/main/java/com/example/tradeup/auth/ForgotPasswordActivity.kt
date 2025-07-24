package com.example.tradeup.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.tradeup.R
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var btnSendReset: Button
    private lateinit var btnBackToLogin: TextView
    private lateinit var tvError: TextView
    private lateinit var tvSuccess: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

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

        btnSendReset.isEnabled = false
        tvError.visibility = View.GONE
        tvSuccess.visibility = View.GONE
    }

    private fun setupTextWatchers() {
        etEmail.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateEmail()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun validateEmail() {
        val email = etEmail.text.toString().trim()
        val isValid = email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

        btnSendReset.isEnabled = isValid

        // Visual feedback
        if (email.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setBackgroundResource(R.drawable.edittext_error_background)
        } else {
            etEmail.setBackgroundResource(R.drawable.edittext_background)
        }
    }

    private fun setupClickListeners() {
        btnSendReset.setOnClickListener {
            sendPasswordResetEmail()
        }

        btnBackToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun sendPasswordResetEmail() {
        val email = etEmail.text.toString().trim()

        if (!validateInput(email)) {
            return
        }

        showLoading(true)
        hideMessages()

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                showLoading(false)

                if (task.isSuccessful) {
                    showSuccess("Password reset email sent to $email\n\nCheck your inbox and follow the instructions to reset your password.")

                    // Disable the form after successful send
                    etEmail.isEnabled = false
                    btnSendReset.text = "Email Sent ✓"
                    btnSendReset.isEnabled = false

                } else {
                    handleResetError(task.exception)
                }
            }
    }

    private fun validateInput(email: String): Boolean {
        when {
            email.isEmpty() -> {
                showError("Please enter your email address")
                etEmail.requestFocus()
                return false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                showError("Please enter a valid email address")
                etEmail.requestFocus()
                return false
            }
            else -> return true
        }
    }

    private fun handleResetError(exception: Exception?) {
        val errorMessage = when {
            exception?.message?.contains("user-not-found") == true ->
                "No account found with this email address. Please check your email or create a new account."
            exception?.message?.contains("invalid-email") == true ->
                "Invalid email address format. Please check your email."
            exception?.message?.contains("too-many-requests") == true ->
                "Too many reset attempts. Please wait a few minutes before trying again."
            else ->
                "Failed to send reset email. Please check your internet connection and try again."
        }

        showError(errorMessage)
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnSendReset.isEnabled = !show && validateEmailField()
        etEmail.isEnabled = !show
    }

    private fun validateEmailField(): Boolean {
        val email = etEmail.text.toString().trim()
        return email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
        tvSuccess.visibility = View.GONE
        tvError.announceForAccessibility(message)
    }

    private fun showSuccess(message: String) {
        tvSuccess.text = message
        tvSuccess.visibility = View.VISIBLE
        tvError.visibility = View.GONE
        tvSuccess.announceForAccessibility(message)
    }

    private fun hideMessages() {
        tvError.visibility = View.GONE
        tvSuccess.visibility = View.GONE
    }
}