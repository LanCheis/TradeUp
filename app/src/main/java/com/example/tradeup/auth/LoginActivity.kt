package com.example.tradeup.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.tradeup.MainActivity
import com.example.tradeup.R
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var progressBar: ProgressBar

    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var loginBtn: Button
    private lateinit var googleBtn: Button
    private lateinit var errorText: TextView
    private lateinit var forgotPasswordText: TextView
    private lateinit var goToRegisterText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        initViews()
        setupTextWatchers()
        setupClickListeners()
    }

    private fun initViews() {
        try {
            emailField = findViewById(R.id.etEmail)
            passwordField = findViewById(R.id.etPassword)
            loginBtn = findViewById(R.id.btnLogin)
            googleBtn = findViewById(R.id.btnGoogle)
            errorText = findViewById(R.id.tvError)
            progressBar = findViewById(R.id.progressBar)
            forgotPasswordText = findViewById(R.id.tvForgotPassword)
            goToRegisterText = findViewById(R.id.tvGoToRegister)

            loginBtn.isEnabled = false
            errorText.visibility = View.GONE
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing views: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupTextWatchers() {
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateFields()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        emailField.addTextChangedListener(watcher)
        passwordField.addTextChangedListener(watcher)
    }

    private fun validateFields() {
        val email = emailField.text.toString().trim()
        val password = passwordField.text.toString().trim()

        val isValid = email.isNotEmpty() && password.isNotEmpty() &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

        loginBtn.isEnabled = isValid

        // Visual feedback
        if (email.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailField.setBackgroundResource(R.drawable.edittext_background)
        }
    }

    private fun setupClickListeners() {
        loginBtn.setOnClickListener {
            loginWithEmail()
        }

        googleBtn.setOnClickListener {
            Toast.makeText(this, "Google Sign-In coming soon!", Toast.LENGTH_SHORT).show()
        }

        forgotPasswordText.setOnClickListener {
            Toast.makeText(this, "Forgot password feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        goToRegisterText.setOnClickListener {
            Toast.makeText(this, "Registration coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loginWithEmail() {
        val email = emailField.text.toString().trim()
        val password = passwordField.text.toString().trim()

        if (!validateInput(email, password)) return

        showProgress(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                showProgress(false)

                if (task.isSuccessful) {
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    val error = task.exception?.message ?: "Login failed"
                    showError(error)
                }
            }
    }

    private fun validateInput(email: String, password: String): Boolean {
        hideError()

        when {
            email.isEmpty() -> {
                showError("Please enter your email")
                emailField.requestFocus()
                return false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                showError("Please enter a valid email address")
                emailField.requestFocus()
                return false
            }
            password.isEmpty() -> {
                showError("Please enter your password")
                passwordField.requestFocus()
                return false
            }
            password.length < 6 -> {
                showError("Password must be at least 6 characters")
                passwordField.requestFocus()
                return false
            }
            else -> return true
        }
    }

    private fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        loginBtn.isEnabled = !show && validateFieldsForButton()
        googleBtn.isEnabled = !show
    }

    private fun validateFieldsForButton(): Boolean {
        val email = emailField.text.toString().trim()
        val password = passwordField.text.toString().trim()

        return email.isNotEmpty() && password.isNotEmpty() &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun showError(message: String) {
        errorText.text = message
        errorText.visibility = View.VISIBLE
        errorText.announceForAccessibility(message)
    }

    private fun hideError() {
        errorText.visibility = View.GONE
    }
}