package com.example.tradeup.auth

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
    private lateinit var tvError: TextView
    private lateinit var tvSuccess: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBackToLogin: TextView

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
        tvError = findViewById(R.id.tvError)
        tvSuccess = findViewById(R.id.tvSuccess)
        progressBar = findViewById(R.id.progressBar)
        btnBackToLogin = findViewById(R.id.btnBackToLogin)

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
        val isValidEmail = email.isNotEmpty() &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

        btnSendReset.isEnabled = isValidEmail

        if (email.isNotEmpty() && !isValidEmail) {
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
            finish()
        }
    }

    private fun sendPasswordResetEmail() {
        val email = etEmail.text.toString().trim()

        if (!validateInputs(email)) return

        showLoading(true)
        hideMessages()

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                showLoading(false)

                if (task.isSuccessful) {
                    showSuccess("Password reset email sent to $email")
                    etEmail.text.clear()
                } else {
                    val errorMessage = when {
                        task.exception?.message?.contains("user-not-found") == true ->
                            "No account found with this email address."
                        task.exception?.message?.contains("invalid-email") == true ->
                            "Please enter a valid email address."
                        else -> task.exception?.message ?: "Failed to send reset email."
                    }
                    showError(errorMessage)
                }
            }
    }

    private fun validateInputs(email: String): Boolean {
        if (email.isEmpty()) {
            showError("Please enter your email address")
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter a valid email address")
            return false
        }

        return true
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