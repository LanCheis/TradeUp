package com.example.tradeup.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tradeup.MainActivity
import com.example.tradeup.R
import com.example.tradeup.data.remote.UserRepository
import com.example.tradeup.onboarding.SetupProfileActivity
import com.example.tradeup.utils.ProfileValidator
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var userRepository: UserRepository
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

        // Initialize Firebase Auth and Repository
        auth = FirebaseAuth.getInstance()
        userRepository = UserRepository()

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
            emailField.setBackgroundResource(R.drawable.edittext_error_background)
        } else {
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
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        goToRegisterText.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun loginWithEmail() {
        val email = emailField.text.toString().trim()
        val password = passwordField.text.toString().trim()

        if (!validateInput(email, password)) return

        showProgress(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // 🛡️ CHECK PROFILE COMPLETENESS BEFORE PROCEEDING
                    checkProfileCompletenessAndNavigate()
                } else {
                    showProgress(false)
                    val error = task.exception?.message ?: "Login failed. Please try again."
                    showError(error)
                }
            }
    }

    /**
     * 🛡️ Check if user profile is complete before allowing access to main app
     */
    private fun checkProfileCompletenessAndNavigate() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showProgress(false)
            showError("Authentication error. Please try again.")
            return
        }

        lifecycleScope.launch {
            try {
                val userProfile = userRepository.getUserProfile(currentUser.uid)

                showProgress(false)

                if (ProfileValidator.isProfileComplete(userProfile)) {
                    // ✅ Profile is complete - proceed to main app
                    Toast.makeText(this@LoginActivity, "Welcome back!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    // ❌ Profile incomplete - redirect to setup
                    val missingFields = ProfileValidator.getMissingFields(userProfile)
                    val message = "Please complete your profile:\n• ${missingFields.joinToString("\n• ")}"

                    Toast.makeText(this@LoginActivity,
                        "Profile setup required", Toast.LENGTH_LONG).show()

                    navigateToProfileSetup(message)
                }

            } catch (e: Exception) {
                showProgress(false)
                showError("Error checking profile: ${e.message}")
            }
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
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
                showError("Please enter your password")
                return false
            }
        }
        return true
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToProfileSetup(message: String = "") {
        val intent = Intent(this, SetupProfileActivity::class.java)
        if (message.isNotEmpty()) {
            intent.putExtra("incomplete_message", message)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        loginBtn.isEnabled = !show && validateFormFields()

        emailField.isEnabled = !show
        passwordField.isEnabled = !show
    }

    private fun validateFormFields(): Boolean {
        val email = emailField.text.toString().trim()
        val password = passwordField.text.toString().trim()

        return email.isNotEmpty() && password.isNotEmpty() &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun showError(message: String) {
        errorText.text = message
        errorText.visibility = View.VISIBLE
    }
}