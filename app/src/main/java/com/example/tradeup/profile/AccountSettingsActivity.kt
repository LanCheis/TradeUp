package com.example.tradeup.profile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tradeup.auth.LoginActivity
import com.example.tradeup.databinding.ActivityAccountSettingsBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

// FR-1.2.3: Account management and preferences
class AccountSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccountSettingsBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupToolbar()
        setupClickListeners()
        loadAccountInfo()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Account Settings"
        }

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupClickListeners() {
        // Change Password
        binding.btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        // Privacy Settings
        binding.btnPrivacySettings.setOnClickListener {
            Toast.makeText(this, "Privacy settings coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Notification Settings
        binding.btnNotificationSettings.setOnClickListener {
            Toast.makeText(this, "Notification settings coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Delete Account
        binding.btnDeleteAccount.setOnClickListener {
            showDeleteAccountConfirmation()
        }

        // Logout
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun loadAccountInfo() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            binding.tvEmail.text = currentUser.email
            binding.tvAccountCreated.text = "Account created: ${android.text.format.DateFormat.format("MMM dd, yyyy", currentUser.metadata?.creationTimestamp)}"
            binding.tvLastSignIn.text = "Last sign in: ${android.text.format.DateFormat.format("MMM dd, yyyy HH:mm", currentUser.metadata?.lastSignInTimestamp)}"
        }
    }

    private fun showChangePasswordDialog() {
        val currentUser = auth.currentUser
        if (currentUser?.email != null) {
            AlertDialog.Builder(this)
                .setTitle("🔑 Change Password")
                .setMessage("We'll send a password reset email to:\n${currentUser.email}")
                .setPositiveButton("Send Email") { _, _ ->
                    sendPasswordResetEmail(currentUser.email!!)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                Toast.makeText(this, "Password reset email sent! ✉️", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to send email: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showDeleteAccountConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Delete Account")
            .setMessage("This will permanently delete your account and all your data. This action cannot be undone.\n\nAre you sure you want to continue?")
            .setPositiveButton("Delete Forever") { _, _ ->
                showFinalDeleteConfirmation()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFinalDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("🗑️ Final Confirmation")
            .setMessage("Type DELETE to confirm account deletion:")
            .setView(android.widget.EditText(this).apply {
                hint = "Type DELETE here"
            })
            .setPositiveButton("Delete") { dialog, _ ->
                val editText = (dialog as AlertDialog).findViewById<android.widget.EditText>(android.R.id.edit)
                if (editText?.text.toString() == "DELETE") {
                    deleteAccount()
                } else {
                    Toast.makeText(this@AccountSettingsActivity, "Confirmation text doesn't match", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAccount() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            currentUser.delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                    redirectToLogin()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Failed to delete account: ${exception.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("🚪 Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                auth.signOut()
                redirectToLogin()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun redirectToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finishAffinity() // Close all activities
    }
}