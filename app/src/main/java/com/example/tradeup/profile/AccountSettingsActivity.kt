package com.example.tradeup.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tradeup.R
import com.example.tradeup.auth.LoginActivity
import com.example.tradeup.data.remote.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class AccountSettingsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var userRepository: UserRepository
    private lateinit var firestore: FirebaseFirestore

    // Views
    private lateinit var ivBackButton: ImageView
    private lateinit var btnChangePassword: LinearLayout
    private lateinit var btnEmailVerification: LinearLayout
    private lateinit var btnDeactivateAccount: LinearLayout
    private lateinit var btnDeleteAccount: LinearLayout
    private lateinit var tvEmailStatus: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_settings)

        auth = FirebaseAuth.getInstance()
        userRepository = UserRepository()
        firestore = FirebaseFirestore.getInstance()

        initViews()
        setupClickListeners()
        updateEmailVerificationStatus()
    }

    private fun initViews() {
        ivBackButton = findViewById(R.id.ivBackButton)
        btnChangePassword = findViewById(R.id.btnChangePassword)
        btnEmailVerification = findViewById(R.id.btnEmailVerification)
        btnDeactivateAccount = findViewById(R.id.btnDeactivateAccount)
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount)
        tvEmailStatus = findViewById(R.id.tvEmailStatus)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupClickListeners() {
        ivBackButton.setOnClickListener {
            finish()
        }

        btnChangePassword.setOnClickListener {
            sendPasswordResetEmail()
        }

        btnEmailVerification.setOnClickListener {
            sendEmailVerification()
        }

        btnDeactivateAccount.setOnClickListener {
            showDeactivateAccountDialog()
        }

        btnDeleteAccount.setOnClickListener {
            showDeleteAccountDialog()
        }
    }

    private fun updateEmailVerificationStatus() {
        val user = auth.currentUser
        if (user != null) {
            if (user.isEmailVerified) {
                tvEmailStatus.text = "✅ Email verified"
                tvEmailStatus.setTextColor(getColor(R.color.success))
                btnEmailVerification.alpha = 0.6f
                btnEmailVerification.isEnabled = false
            } else {
                tvEmailStatus.text = "❌ Email not verified"
                tvEmailStatus.setTextColor(getColor(R.color.error))
            }
        }
    }

    private fun sendPasswordResetEmail() {
        val user = auth.currentUser
        val userEmail = user?.email // This is String?

        if (userEmail.isNullOrEmpty()) {
            Toast.makeText(this, "No email found", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        // ✅ Use non-null userEmail (now guaranteed to be non-null)
        auth.sendPasswordResetEmail(userEmail)
            .addOnCompleteListener { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    Toast.makeText(this, "Password reset email sent to $userEmail", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Failed to send reset email: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun sendEmailVerification() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "No user found", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        user.sendEmailVerification()
            .addOnCompleteListener { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    // ✅ Safe handling of nullable email
                    val emailText = user.email ?: "your email"
                    Toast.makeText(this, "Verification email sent to $emailText", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Failed to send verification email: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun showDeactivateAccountDialog() {
        AlertDialog.Builder(this)
            .setTitle("Deactivate Account")
            .setMessage("Are you sure you want to deactivate your account?\n\n• Your profile will be hidden\n• Your listings will be paused\n• You can reactivate anytime by logging back in")
            .setPositiveButton("Deactivate") { _, _ ->
                deactivateAccount()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteAccountDialog() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Delete Account")
            .setMessage("Are you sure you want to permanently delete your account?\n\n⚠️ WARNING: This action cannot be undone!\n\n• All your data will be permanently deleted\n• Your listings will be removed\n• Your messages will be deleted\n• You will lose access to all account information")
            .setPositiveButton("DELETE FOREVER") { _, _ ->
                showFinalDeleteConfirmation()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFinalDeleteConfirmation() {
        val editText = EditText(this).apply {
            hint = "Type 'DELETE' to confirm"
            textSize = 16f
        }

        AlertDialog.Builder(this)
            .setTitle("Final Confirmation")
            .setMessage("Type 'DELETE' to permanently delete your account:")
            .setView(editText)
            .setPositiveButton("DELETE FOREVER") { _, _ ->
                val input = editText.text.toString().trim()
                if (input.equals("DELETE", ignoreCase = true)) {
                    deleteAccountPermanently()
                } else {
                    Toast.makeText(this, "Deletion cancelled - incorrect confirmation", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deactivateAccount() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                // For deactivation, we just logout - account stays but user can't access
                auth.signOut()
                Toast.makeText(this@AccountSettingsActivity, "Account deactivated. You can reactivate by logging back in.", Toast.LENGTH_LONG).show()

                val intent = Intent(this@AccountSettingsActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()

            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(this@AccountSettingsActivity, "Error deactivating account: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteAccountPermanently() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "No user found", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                // ✅ Use safe user.uid (non-null)
                firestore.collection("users").document(user.uid).delete()

                // Delete user authentication account
                user.delete()
                    .addOnCompleteListener { task ->
                        showLoading(false)
                        if (task.isSuccessful) {
                            Toast.makeText(this@AccountSettingsActivity, "Account deleted permanently", Toast.LENGTH_LONG).show()

                            val intent = Intent(this@AccountSettingsActivity, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@AccountSettingsActivity, "Failed to delete account. You may need to re-login and try again.", Toast.LENGTH_LONG).show()
                        }
                    }

            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(this@AccountSettingsActivity, "Error deleting account: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnChangePassword.isEnabled = !show
        btnEmailVerification.isEnabled = !show && !(auth.currentUser?.isEmailVerified ?: false)
        btnDeactivateAccount.isEnabled = !show
        btnDeleteAccount.isEnabled = !show
    }
}