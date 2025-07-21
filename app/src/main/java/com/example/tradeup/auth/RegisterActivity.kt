package com.example.tradeup.auth

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.tradeup.R
import com.example.tradeup.data.remote.FirebaseAuthHelper
import com.example.tradeup.onboarding.SetupProfileActivity
import com.example.tradeup.utils.Validators

class RegisterActivity : AppCompatActivity() {

    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var confirmField: EditText
    private lateinit var registerBtn: Button
    private lateinit var goToLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        emailField = findViewById(R.id.etRegisterEmail)
        passwordField = findViewById(R.id.etRegisterPassword)
        confirmField = findViewById(R.id.etConfirmPassword)
        registerBtn = findViewById(R.id.btnRegister)
        goToLogin = findViewById(R.id.tvGoToLogin)
    }

    private fun setupClickListeners() {
        registerBtn.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()
            val confirm = confirmField.text.toString().trim()

            if (!validateInput(email, password, confirm)) {
                return@setOnClickListener
            }

            registerBtn.isEnabled = false
            registerBtn.text = "Đang đăng ký..."

            FirebaseAuthHelper.registerWithEmail(email, password) { success, error ->
                registerBtn.isEnabled = true
                registerBtn.text = "Đăng ký"

                if (success) {
                    Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_LONG).show()
                    // ✅ CHANGED: Always go to setup profile for new users
                    startActivity(Intent(this, SetupProfileActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, error ?: "Đăng ký thất bại", Toast.LENGTH_SHORT).show()
                }
            }
        }

        goToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun validateInput(email: String, password: String, confirm: String): Boolean {
        if (!Validators.isValidEmail(email)) {
            emailField.error = "Email không hợp lệ"
            emailField.requestFocus()
            return false
        }

        if (password.length < 6) {
            passwordField.error = "Mật khẩu phải có ít nhất 6 ký tự"
            passwordField.requestFocus()
            return false
        }

        if (password != confirm) {
            confirmField.error = "Mật khẩu xác nhận không khớp"
            confirmField.requestFocus()
            return false
        }

        return true
    }
}