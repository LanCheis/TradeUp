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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException

class RegisterActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var btnGoToLogin: TextView
    private lateinit var tvError: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

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
        btnGoToLogin = findViewById(R.id.btnGoToLogin)
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
        val doPasswordsMatch = password == confirmPassword && confirmPassword.isNotEmpty()

        btnRegister.isEnabled = isEmailValid && isPasswordValid && doPasswordsMatch

        // Show real-time validation feedback
        if (email.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setBackgroundResource(R.drawable.edittext_error_background)
        } else {
            etEmail.setBackgroundResource(R.drawable.edittext_background)
        }

        if (password.isNotEmpty() && password.length < 6) {
            etPassword.setBackgroundResource(R.drawable.edittext_error_background)
        } else {
            etPassword.setBackgroundResource(R.drawable.edittext_background)
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

        // Final validation
        if (!validateInput(email, password, confirmPassword)) {
            return
        }

        showLoading(true)
        hideError()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                showLoading(false)

                if (task.isSuccessful) {
                    // Registration successful
                    sendEmailVerification()
                    navigateToProfileSetup()
                } else {
                    // Registration failed
                    handleRegistrationError(task.exception)
                }
            }
    }

    private fun validateInput(email: String, password: String, confirmPassword: String): Boolean {
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
            password.isEmpty() -> {
                showError("Please enter a password")
                etPassword.requestFocus()
                return false
            }
            password.length < 6 -> {
                showError("Password must be at least 6 characters long")
                etPassword.requestFocus()
                return false
            }
            confirmPassword.isEmpty() -> {
                showError("Please confirm your password")
                etConfirmPassword.requestFocus()
                return false
            }
            password != confirmPassword -> {
                showError("Passwords do not match")
                etConfirmPassword.requestFocus()
                return false
            }

            else -> return true
        }
    }

    private fun sendEmailVerification() {
        val user = auth.currentUser
        user?.sendEmailVerification()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Verification email sent to ${user.email}\nPlease check your inbox.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Failed to send verification email. You can resend it later.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun handleRegistrationError(exception: Exception?) {
        val errorMessage = when (exception) {
            is FirebaseAuthWeakPasswordException -> "Password is too weak. Please choose a stronger password with a mix of letters, numbers, and symbols."
            is FirebaseAuthInvalidCredentialsException -> "Invalid email format. Please check your email address."
            is FirebaseAuthUserCollisionException -> "An account with this email already exists. Please try logging in instead."
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

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
        tvError.announceForAccessibility(message)
    }

    private fun hideError() {
        tvError.visibility = View.GONE
    }
}