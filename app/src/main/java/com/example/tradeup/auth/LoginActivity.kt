// File: app/src/main/java/com/example/tradeup/auth/LoginActivity.kt

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
import com.example.tradeup.data.model.User
import com.example.tradeup.data.remote.UserRepository
import com.example.tradeup.onboarding.SetupProfileActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private var hasNavigated = false
    private lateinit var progressBar: ProgressBar
    private lateinit var auth: FirebaseAuth

    private val RC_SIGN_IN = 1001
    private lateinit var googleClient: GoogleSignInClient

    private lateinit var emailField: TextInputEditText
    private lateinit var passwordField: TextInputEditText
    private lateinit var loginBtn: Button
    private lateinit var googleBtn: Button
    private lateinit var errorText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        initViews()
        setupTextWatchers()
        setupClickListeners()
        setupGoogleSignIn()
    }

    private fun initViews() {
        emailField = findViewById(R.id.etEmail)
        passwordField = findViewById(R.id.etPassword)
        loginBtn = findViewById(R.id.btnLogin)
        googleBtn = findViewById(R.id.btnGoogle)
        errorText = findViewById(R.id.tvError)
        progressBar = findViewById(R.id.progressBar)

        loginBtn.isEnabled = false
        errorText.visibility = View.GONE
    }

    private fun setupTextWatchers() {
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateForm()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        emailField.addTextChangedListener(watcher)
        passwordField.addTextChangedListener(watcher)
    }

    private fun validateForm() {
        val email = emailField.text.toString().trim()
        val password = passwordField.text.toString().trim()

        loginBtn.isEnabled = email.isNotEmpty() && password.isNotEmpty() &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun setupClickListeners() {
        loginBtn.setOnClickListener {
            performEmailLogin()
        }

        googleBtn.setOnClickListener {
            performGoogleSignIn()
        }
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleClient = GoogleSignIn.getClient(this, gso)
    }

    private fun performEmailLogin() {
        val email = emailField.text.toString().trim()
        val password = passwordField.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            errorText.text = "Please fill in all fields"
            errorText.visibility = View.VISIBLE
            return
        }

        showLoading(true)
        errorText.visibility = View.GONE

        // ✅ FIXED: Direct Firebase Auth call with explicit callback types
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                showLoading(false)

                if (task.isSuccessful) {
                    checkUserProfileAndNavigate()
                } else {
                    errorText.text = task.exception?.message ?: "Login failed"
                    errorText.visibility = View.VISIBLE
                }
            }
    }

    private fun performGoogleSignIn() {
        showLoading(true)
        googleBtn.isEnabled = false

        val signInIntent = googleClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                showLoading(false)
                googleBtn.isEnabled = true
                errorText.text = "Google Sign-In failed: ${e.message}"
                errorText.visibility = View.VISIBLE
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        if (account != null) {
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)

            // ✅ FIXED: Direct Firebase Auth call
            auth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    showLoading(false)
                    googleBtn.isEnabled = true

                    if (task.isSuccessful) {
                        checkUserProfileAndNavigate()
                    } else {
                        errorText.text = task.exception?.message ?: "Google Sign-In failed"
                        errorText.visibility = View.VISIBLE
                    }
                }
        } else {
            showLoading(false)
            googleBtn.isEnabled = true
            errorText.text = "Google Sign-In failed"
            errorText.visibility = View.VISIBLE
        }
    }

    private fun checkUserProfileAndNavigate() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // ✅ FIXED: Explicit callback type to resolve ambiguity
            val profileCallback: (User?) -> Unit = { user ->
                if (user != null && isProfileComplete(user)) {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    startActivity(Intent(this, SetupProfileActivity::class.java))
                    finish()
                }
            }

            UserRepository.getUserProfile(currentUser.uid, profileCallback)
        }
    }

    private fun isProfileComplete(user: User): Boolean {
        return user.name.isNotEmpty() &&
                user.phone.isNotEmpty() &&
                user.bio.isNotEmpty() &&
                user.gender.isNotEmpty()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        loginBtn.isEnabled = !show &&
                emailField.text.toString().isNotBlank() &&
                passwordField.text.toString().isNotBlank()
        googleBtn.isEnabled = !show
    }
}