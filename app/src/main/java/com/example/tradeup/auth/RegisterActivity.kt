package com.example.tradeup.auth

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.tradeup.R
import com.example.tradeup.data.remote.FirebaseAuthHelper
import com.example.tradeup.utils.Validators

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val emailField = findViewById<EditText>(R.id.etRegisterEmail)
        val passwordField = findViewById<EditText>(R.id.etRegisterPassword)
        val confirmField = findViewById<EditText>(R.id.etConfirmPassword)
        val registerBtn = findViewById<Button>(R.id.btnRegister)
        val goToLogin = findViewById<TextView>(R.id.tvGoToLogin)

        registerBtn.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()
            val confirm = confirmField.text.toString().trim()

            if (!Validators.isValidEmail(email)) {
                emailField.error = "Invalid email"
                return@setOnClickListener
            }

            if (password.length < 6) {
                passwordField.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }

            if (password != confirm) {
                confirmField.error = "Passwords do not match"
                return@setOnClickListener
            }

            FirebaseAuthHelper.registerWithEmail(email, password) { success, error ->
                if (success) {
                    Toast.makeText(this, "Registered. Check email to verify!", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, error ?: "Registration failed", Toast.LENGTH_SHORT).show()
                }
            }
        }

        goToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
