// app/src/main/java/com/example/tradeup/profile/ProfileFragment.kt

package com.example.tradeup.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.tradeup.R
import com.example.tradeup.auth.LoginActivity
import com.example.tradeup.data.remote.UserRepository
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment() {

    private lateinit var ivAvatar: ImageView
    private lateinit var tvDisplayName: TextView
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvBio: TextView
    private lateinit var tvUsername: TextView
    private lateinit var tvGender: TextView
    private lateinit var tvBirthday: TextView
    private lateinit var tvInterest: TextView
    private lateinit var btnLogout: Button
    private lateinit var btnEditProfile: Button
    private lateinit var btnDeleteAccount: Button
    private lateinit var btnMyOffers: Button // ✅ NEW

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupClickListeners()
        loadUserData()
    }

    private fun initViews(view: View) {
        ivAvatar = view.findViewById(R.id.ivAvatar)
        tvDisplayName = view.findViewById(R.id.tvDisplayName)
        tvName = view.findViewById(R.id.tvName)
        tvEmail = view.findViewById(R.id.tvEmail)
        tvPhone = view.findViewById(R.id.tvPhone)
        tvAddress = view.findViewById(R.id.tvAddress)
        tvBio = view.findViewById(R.id.tvBio)
        tvUsername = view.findViewById(R.id.tvUsername)
        tvGender = view.findViewById(R.id.tvGender)
        tvBirthday = view.findViewById(R.id.tvBirthday)
        tvInterest = view.findViewById(R.id.tvInterest)
        btnEditProfile = view.findViewById(R.id.btnEditProfile)
        btnLogout = view.findViewById(R.id.btnLogout)
        btnDeleteAccount = view.findViewById(R.id.btnDeleteAccount)
        btnMyOffers = view.findViewById(R.id.btnMyOffers) // ✅ NEW
    }

    private fun setupClickListeners() {
        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }

        // ✅ FIXED: Use Activity-based navigation instead of Fragment navigation
        btnEditProfile.setOnClickListener {
            // Create EditProfileActivity instead of using fragment navigation
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }

        btnDeleteAccount.setOnClickListener {
            showDeleteAccountDialog()
        }

        // ✅ NEW: Navigate to offers
        btnMyOffers.setOnClickListener {
            startActivity(Intent(requireContext(), com.example.tradeup.offer.OffersActivity::class.java))
        }
    }

    private fun showDeleteAccountDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("⚠️ Delete Account")
            .setMessage("Are you sure you want to permanently delete your account? This action cannot be undone.\n\n• All your data will be deleted\n• All your listings will be removed\n• You cannot recover this account")
            .setPositiveButton("DELETE") { _, _ ->
                confirmDeleteAccount()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun confirmDeleteAccount() {
        btnDeleteAccount.isEnabled = false
        btnDeleteAccount.text = "Deleting..."

        UserRepository.deleteUserAccount { success, error ->
            if (isAdded) {
                if (success) {
                    Toast.makeText(requireContext(), "Account deleted successfully", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(requireContext(), LoginActivity::class.java))
                    requireActivity().finish()
                } else {
                    btnDeleteAccount.isEnabled = true
                    btnDeleteAccount.text = "⚠️ Delete Account"
                    Toast.makeText(requireContext(), "Failed to delete account: $error", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun loadUserData() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        UserRepository.getUserProfile(uid) { user ->
            if (user != null && isAdded) {
                tvDisplayName.text = user.name.ifEmpty { "User Name" }
                tvEmail.text = user.email
                tvName.text = user.name.ifEmpty { "Not set" }
                tvPhone.text = user.phone.ifEmpty { "Not set" }
                tvAddress.text = user.address.ifEmpty { "Not set" }
                tvBio.text = user.bio.ifEmpty { "No bio yet" }
                tvUsername.text = user.username.ifEmpty { "Not set" }
                tvGender.text = user.gender.ifEmpty { "Not set" }
                tvBirthday.text = user.birthday.ifEmpty { "Not set" }
                tvInterest.text = user.interests.ifEmpty { "Not set" }

                if (user.profileImageUrl.isNotEmpty()) {
                    Glide.with(requireContext())
                        .load(user.profileImageUrl)
                        .error(R.drawable.ic_avatar_placeholder)
                        .into(ivAvatar)
                }
            } else {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}