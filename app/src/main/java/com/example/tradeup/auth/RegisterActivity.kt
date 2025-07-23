// File: app/src/main/java/com/example/tradeup/auth/RegisterActivity.kt

package com.example.tradeup.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.tradeup.R
import com.example.tradeup.onboarding.SetupProfileActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException

class RegisterActivity : AppCompatActivity() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnRegister: Button
    private lateinit var btnLogin: Button
    private lateinit var tvError: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        initViews()
        setupTextWatchers()
        setupClickListeners()
    }

    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        btnLogin = findViewById(R.id.btnLogin)
        tvError = findViewById(R.id.tvError)
        progressBar = findViewById(R.id.progressBar)

        btnRegister.isEnabled = false
        tvError.visibility = View.GONE
    }

    private fun setupTextWatchers() {
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateForm()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        etEmail.addTextChangedListener(watcher)
        etPassword.addTextChangedListener(watcher)
        etConfirmPassword.addTextChangedListener(watcher)

    }

    private fun validateForm() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        val isEmailValid = email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
        val isPasswordValid = password.isNotEmpty() && password.length >= 6
        val doPasswordsMatch = password == confirmPassword

        btnRegister.isEnabled = isEmailValid && isPasswordValid && doPasswordsMatch

        // Show password mismatch error
        if (confirmPassword.isNotEmpty() && !doPasswordsMatch) {
            etConfirmPassword.error = "Passwords do not match"
        } else {
            etConfirmPassword.error = null
        }

        // Show password length error
        if (password.isNotEmpty() && password.length < 6) {
            etPassword.error = "Password must be at least 6 characters"
        } else {
            etPassword.error = null
        }
    }

    private fun setupClickListeners() {
        btnRegister.setOnClickListener {
            performRegistration()
        }

        btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun performRegistration() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        // Validate input
        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            etEmail.requestFocus()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Please enter a valid email"
            etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            etPassword.error = "Password is required"
            etPassword.requestFocus()
            return
        }

        if (password.length < 6) {
            etPassword.error = "Password must be at least 6 characters"
            etPassword.requestFocus()
            return
        }

        if (password != confirmPassword) {
            etConfirmPassword.error = "Passwords do not match"
            etConfirmPassword.requestFocus()
            return
        }


        showLoading(true)
        tvError.visibility = View.GONE

        // ✅ FIXED: Direct Firebase Auth call instead of FirebaseAuthHelper
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                showLoading(false)

                if (task.isSuccessful) {
                    // Registration successful
                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()

                    // Send email verification
                    sendEmailVerification()

                    // Navigate to profile setup
                    startActivity(Intent(this, SetupProfileActivity::class.java))
                    finish()
                } else {
                    // Registration failed
                    handleRegistrationError(task.exception)
                }
            }
    }

    private fun sendEmailVerification() {
        val user = auth.currentUser
        user?.sendEmailVerification()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Verification email sent to ${user.email}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun handleRegistrationError(exception: Exception?) {
        val errorMessage = when (exception) {
            is FirebaseAuthWeakPasswordException -> "Password is too weak. Please choose a stronger password."
            is FirebaseAuthInvalidCredentialsException -> "Invalid email format. Please check your email."
            is FirebaseAuthUserCollisionException -> "An account with this email already exists. Please login instead."
            else -> exception?.message ?: "Registration failed. Please try again."
        }

        tvError.text = errorMessage
        tvError.visibility = View.VISIBLE
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnRegister.isEnabled = !show && validateFormFields()
        btnLogin.isEnabled = !show

        // Disable input fields during loading
        etEmail.isEnabled = !show
        etPassword.isEnabled = !show
        etConfirmPassword.isEnabled = !show
    }

    private fun validateFormFields(): Boolean {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        return email.isNotEmpty() &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                password.isNotEmpty() &&
                password.length >= 6 &&
                password == confirmPassword
    }
}