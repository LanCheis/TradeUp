package com.example.tradeup.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.tradeup.MainActivity
import com.example.tradeup.R
import com.example.tradeup.data.model.User
import com.example.tradeup.data.remote.FirebaseAuthHelper
import com.example.tradeup.data.remote.UserRepository
import com.example.tradeup.onboarding.SetupProfileActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private var hasNavigated = false
    private lateinit var progressBar: ProgressBar

    private val RC_SIGN_IN = 1001
    private lateinit var googleClient: GoogleSignInClient

    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var togglePasswordIcon: ImageView
    private lateinit var loginBtn: Button
    private lateinit var googleBtn: Button
    private lateinit var errorText: TextView

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initViews()
        setupTextWatchers()
        setupClickListeners()
        setupGoogleSignIn()
    }

    private fun initViews() {
        emailField = findViewById(R.id.etEmail)
        passwordField = findViewById(R.id.etPassword)
        togglePasswordIcon = findViewById(R.id.ivTogglePassword)
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
                loginBtn.isEnabled =
                    emailField.text.toString().isNotBlank() && passwordField.text.toString().isNotBlank()
                errorText.visibility = View.GONE
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        emailField.addTextChangedListener(watcher)
        passwordField.addTextChangedListener(watcher)
    }

    private fun setupClickListeners() {
        togglePasswordIcon.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                passwordField.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                togglePasswordIcon.setImageResource(R.drawable.ic_eye)
            } else {
                passwordField.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                togglePasswordIcon.setImageResource(R.drawable.ic_eye_off)
            }
            passwordField.setSelection(passwordField.text.length)
        }

        loginBtn.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()

            showLoading(true)
            FirebaseAuthHelper.loginWithEmail(email, password) { success, error ->
                showLoading(false)
                if (success) {
                    checkUserProfileAndNavigate()
                } else {
                    errorText.text = error ?: "Lỗi không xác định khi đăng nhập"
                    errorText.visibility = View.VISIBLE
                }
            }
        }

        findViewById<TextView>(R.id.tvGoToRegister).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        findViewById<TextView>(R.id.tvForgotPassword).setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleClient = GoogleSignIn.getClient(this, gso)

        googleBtn.setOnClickListener {
            if (!hasNavigated) {
                googleBtn.isEnabled = false
                showLoading(true)
                val intent = googleClient.signInIntent
                startActivityForResult(intent, RC_SIGN_IN)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java) as GoogleSignInAccount
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                FirebaseAuthHelper.signInWithGoogleCredential(credential) { success, error ->
                    showLoading(false)

                    if (success && !hasNavigated) {
                        hasNavigated = true
                        checkUserProfileAndNavigate()
                    } else if (!success) {
                        googleBtn.isEnabled = true
                        errorText.text = error ?: "Google đăng nhập thất bại"
                        errorText.visibility = View.VISIBLE
                    }
                }
            } catch (e: ApiException) {
                showLoading(false)
                googleBtn.isEnabled = true
                errorText.text = "Google Sign-In thất bại: ${e.message}"
                errorText.visibility = View.VISIBLE
            }
        }
    }

    // ✅ NEW METHOD - Check profile and navigate accordingly
    private fun checkUserProfileAndNavigate() {
        val currentUser = FirebaseAuthHelper.getCurrentUser()
        if (currentUser != null) {
            UserRepository.getUserProfile(currentUser.uid) { user ->
                if (user != null && isProfileComplete(user)) {
                    // Profile is complete, go to main app
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    // Profile incomplete or doesn't exist, go to setup
                    startActivity(Intent(this, SetupProfileActivity::class.java))
                    finish()
                }
            }
        }
    }

    // ✅ NEW METHOD - Check if profile has essential information
    private fun isProfileComplete(user: User): Boolean {
        return user.name.isNotEmpty() &&
                user.phone.isNotEmpty() &&
                user.bio.isNotEmpty() &&
                user.gender.isNotEmpty()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        loginBtn.isEnabled = !show
        googleBtn.isEnabled = !show
    }
}