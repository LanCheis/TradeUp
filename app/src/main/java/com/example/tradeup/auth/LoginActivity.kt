package com.example.tradeup.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.tradeup.R
import com.example.tradeup.data.remote.FirebaseAuthHelper
import com.example.tradeup.home.HomeActivity
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

        googleBtn = findViewById(R.id.btnGoogle)
        errorText = findViewById(R.id.tvError)
        progressBar = findViewById(R.id.progressBar)

        // View binding
        emailField = findViewById(R.id.etEmail)
        passwordField = findViewById(R.id.etPassword)
        togglePasswordIcon = findViewById(R.id.ivTogglePassword)
        loginBtn = findViewById(R.id.btnLogin)
        googleBtn = findViewById(R.id.btnGoogle)
        errorText = findViewById(R.id.tvError)

        loginBtn.isEnabled = false
        errorText.visibility = View.GONE

        // TextWatcher to enable login button only when fields are filled
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                loginBtn.isEnabled =
                    emailField.text.toString().isNotBlank() && passwordField.text.toString().isNotBlank()
                errorText.visibility = View.GONE // clear error when typing
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        emailField.addTextChangedListener(watcher)
        passwordField.addTextChangedListener(watcher)

        // Toggle password visibility
        togglePasswordIcon.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                passwordField.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                togglePasswordIcon.setImageResource(R.drawable.ic_eye) // 👁
            } else {
                passwordField.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                togglePasswordIcon.setImageResource(R.drawable.ic_eye_off) // 👁‍🗨
            }
            passwordField.setSelection(passwordField.text.length)
        }

        loginBtn.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()

            FirebaseAuthHelper.loginWithEmail(email, password) { success, error ->
                if (success) {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                } else {
                    errorText.text = error ?: "Lỗi không xác định khi đăng nhập"
                    errorText.visibility = View.VISIBLE
                }
            }
        }

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleClient = GoogleSignIn.getClient(this, gso)

        googleBtn.setOnClickListener {
            if (!hasNavigated) {
                googleBtn.isEnabled = false
                progressBar.visibility = View.VISIBLE
                val intent = googleClient.signInIntent
                startActivityForResult(intent, RC_SIGN_IN)
            }
        }

        // Navigation links
        findViewById<TextView>(R.id.tvGoToRegister).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        findViewById<TextView>(R.id.tvForgotPassword).setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
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
                    progressBar.visibility = View.GONE

                    if (success && !hasNavigated) {
                        hasNavigated = true
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    } else if (!success) {
                        googleBtn.isEnabled = true
                        errorText.text = error ?: "Google đăng nhập thất bại"
                        errorText.visibility = View.VISIBLE
                    }
                }
            } catch (e: ApiException) {
                progressBar.visibility = View.GONE
                googleBtn.isEnabled = true
                errorText.text = "Google Sign-In thất bại: ${e.message}"
                errorText.visibility = View.VISIBLE

            }
        }
    }
}
