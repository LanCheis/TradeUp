package com.example.tradeup.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tradeup.R
import com.example.tradeup.data.remote.UserRepository
import com.example.tradeup.onboarding.SetupProfileActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var btnGoToLogin: TextView
    private lateinit var tvError: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var auth: FirebaseAuth
    private lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        userRepository = UserRepository()

        initViews()
        setupTextWatchers()
        setupClickListeners()
    }

    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        btnGoToLogin = findViewById(R.id.btnGoToLogin)
        tvError = findViewById(R.id.tvError)
        progressBar = findViewById(R.id.progressBar)

        btnRegister.isEnabled = false
        tvError.visibility = View.GONE
    }

    private fun setupTextWatchers() {
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateFields()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        etEmail.addTextChangedListener(watcher)
        etPassword.addTextChangedListener(watcher)
        etConfirmPassword.addTextChangedListener(watcher)
    }

    private fun validateFields() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        val isEmailValid = email.isNotEmpty() &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
        val isPasswordValid = password.isNotEmpty() && password.length >= 6
        val doPasswordsMatch = password == confirmPassword

        // FR-1.1.4: Button disabled until fields are valid
        btnRegister.isEnabled = isEmailValid && isPasswordValid &&
                doPasswordsMatch && confirmPassword.isNotEmpty()

        // Visual feedback
        if (email.isNotEmpty() && !isEmailValid) {
            etEmail.setBackgroundResource(R.drawable.edittext_error_background)
        } else {
            etEmail.setBackgroundResource(R.drawable.edittext_background)
        }

        if (confirmPassword.isNotEmpty() && !doPasswordsMatch) {
            etConfirmPassword.setBackgroundResource(R.drawable.edittext_error_background)
        } else {
            etConfirmPassword.setBackgroundResource(R.drawable.edittext_background)
        }
    }

    private fun setupClickListeners() {
        btnRegister.setOnClickListener {
            performRegistration()
        }

        btnGoToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun performRegistration() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        if (!validateInput(email, password, confirmPassword)) {
            return
        }

        showLoading(true)
        hideError()

        // FR-1.1.1: Email/password registration
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // FR-1.1.2: Send email verification
                    sendEmailVerification()

                    // Create initial user profile
                    val user = auth.currentUser
                    user?.let { firebaseUser ->
                        lifecycleScope.launch {
                            val success = userRepository.createInitialProfile(
                                firebaseUser.uid,
                                email
                            )

                            showLoading(false)

                            if (success) {
                                navigateToProfileSetup()
                            } else {
                                showError("Account created but profile setup failed. Please try again.")
                            }
                        }
                    }
                } else {
                    showLoading(false)
                    handleRegistrationError(task.exception)
                }
            }
    }

    private fun validateInput(email: String, password: String, confirmPassword: String): Boolean {
        when {
            email.isEmpty() -> {
                showError("Please enter your email address")
                return false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                showError("Please enter a valid email address")
                return false
            }
            password.isEmpty() -> {
                showError("Please enter a password")
                return false
            }
            password.length < 6 -> {
                showError("Password must be at least 6 characters long")
                return false
            }
            confirmPassword.isEmpty() -> {
                showError("Please confirm your password")
                return false
            }
            password != confirmPassword -> {
                showError("Passwords do not match")
                return false
            }
        }
        return true
    }

    private fun sendEmailVerification() {
        auth.currentUser?.sendEmailVerification()
            ?.addOnCompleteListener { verificationTask ->
                if (verificationTask.isSuccessful) {
                    Toast.makeText(this,
                        "Verification email sent to ${auth.currentUser?.email}",
                        Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun handleRegistrationError(exception: Exception?) {
        val errorMessage = when (exception) {
            is FirebaseAuthWeakPasswordException ->
                "Please choose a stronger password with a mix of letters, numbers, and symbols."
            is FirebaseAuthInvalidCredentialsException ->
                "Invalid email format. Please check your email address."
            is FirebaseAuthUserCollisionException ->
                "An account with this email already exists. Please try logging in instead."
            else -> exception?.message ?: "Registration failed. Please check your internet connection and try again."
        }

        showError(errorMessage)
    }

    private fun navigateToProfileSetup() {
        Toast.makeText(this, "Account created successfully! 🎉", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, SetupProfileActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnRegister.isEnabled = !show && validateFormFields()

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

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
        tvError.announceForAccessibility(message)
    }

    private fun hideError() {
        tvError.visibility = View.GONE
    }
}