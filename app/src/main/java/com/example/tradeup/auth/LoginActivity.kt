package com.example.tradeup.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.activity.result.contract.ActivityResultContracts
import com.example.tradeup.MainActivity
import com.example.tradeup.R
import com.example.tradeup.data.remote.UserRepository
import com.example.tradeup.onboarding.SetupProfileActivity
import com.example.tradeup.utils.ProfileValidator
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var userRepository: UserRepository
    private lateinit var googleSignInClient: GoogleSignInClient

    // Views
    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var loginBtn: Button
    private lateinit var googleBtn: Button
    private lateinit var errorText: TextView
    private lateinit var forgotPasswordText: TextView
    private lateinit var goToRegisterText: TextView
    private lateinit var progressBar: ProgressBar

    // FR-1.1.1: Google Sign-In launcher
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                showProgress(false)
                showError("Google sign-in failed: ${e.message}")
            }
        } else {
            showProgress(false)
            // User cancelled - no error message needed
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth and Repository
        auth = FirebaseAuth.getInstance()
        userRepository = UserRepository()

        initViews()
        setupGoogleSignIn() // ✅ FIXED: Call this in onCreate
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

            // FR-1.1.4: Button disabled initially
            loginBtn.isEnabled = false
            errorText.visibility = View.GONE
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing views: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // FR-1.1.1: Configure Google Sign-In
    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
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

    // FR-1.1.4: Enable button only when both fields are filled correctly
    private fun validateFields() {
        val email = emailField.text.toString().trim()
        val password = passwordField.text.toString().trim()

        val isValid = email.isNotEmpty() && password.isNotEmpty() &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

        loginBtn.isEnabled = isValid

        // Visual feedback for email validation
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

        // FR-1.1.1: Google Sign-In button
        googleBtn.setOnClickListener {
            signInWithGoogle()
        }

        // FR-1.1.3: Forgot password
        forgotPasswordText.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        goToRegisterText.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    // FR-1.1.1: Email/password login
    private fun loginWithEmail() {
        val email = emailField.text.toString().trim()
        val password = passwordField.text.toString().trim()

        if (!validateInput(email, password)) return

        showProgress(true)
        hideError()

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // ✅ Check profile completeness before proceeding
                    checkProfileCompletenessAndNavigate()
                } else {
                    showProgress(false)
                    val error = when (task.exception?.message) {
                        "There is no user record corresponding to this identifier. The user may have been deleted." ->
                            "No account found with this email address."
                        "The password is invalid or the user does not have a password." ->
                            "Incorrect password. Please try again."
                        "The email address is badly formatted." ->
                            "Please enter a valid email address."
                        else -> task.exception?.message ?: "Login failed. Please try again."
                    }
                    showError(error)
                }
            }
    }

    // FR-1.1.1: Google Sign-In process
    private fun signInWithGoogle() {
        showProgress(true)
        hideError()
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    // FR-1.1.1: Authenticate with Firebase using Google credentials
    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Check if this is a new user
                    val isNewUser = task.result?.additionalUserInfo?.isNewUser ?: false

                    if (isNewUser) {
                        // Create initial profile for new Google users
                        val user = auth.currentUser
                        user?.let { firebaseUser ->
                            lifecycleScope.launch {
                                val success = userRepository.createInitialProfile(
                                    firebaseUser.uid,
                                    firebaseUser.email ?: ""
                                )

                                showProgress(false)

                                if (success) {
                                    Toast.makeText(this@LoginActivity,
                                        "Welcome to TradeUp!", Toast.LENGTH_SHORT).show()
                                    navigateToProfileSetup()
                                } else {
                                    showError("Account created but profile setup failed.")
                                }
                            }
                        }
                    } else {
                        // Existing user - check profile completeness
                        checkProfileCompletenessAndNavigate()
                    }

                } else {
                    showProgress(false)
                    showError("Google authentication failed: ${task.exception?.message}")
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

        // Disable/enable all interactive elements during loading
        loginBtn.isEnabled = !show && validateFormFields()
        googleBtn.isEnabled = !show
        emailField.isEnabled = !show
        passwordField.isEnabled = !show
        forgotPasswordText.isEnabled = !show
        goToRegisterText.isEnabled = !show
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

    private fun hideError() {
        errorText.visibility = View.GONE
    }
}