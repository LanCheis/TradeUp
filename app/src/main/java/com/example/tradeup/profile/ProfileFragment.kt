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
import com.example.tradeup.offers.OffersActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private var ivAvatar: ImageView? = null
    private var tvDisplayName: TextView? = null
    private var tvEmail: TextView? = null
    private lateinit var btnLogout: Button
    private lateinit var btnEditProfile: Button

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
        // Find required views
        btnLogout = view.findViewById(R.id.btnLogout)
        btnEditProfile = view.findViewById(R.id.btnEditProfile)

        // Find optional views with safe calls
        ivAvatar = view.findViewById(R.id.ivAvatar)
        tvDisplayName = view.findViewById(R.id.tvDisplayName)
        tvEmail = view.findViewById(R.id.tvEmail)
    }

    private fun setupClickListeners() {
        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }

        btnEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }

        // Try to find and setup additional buttons if they exist
        view?.findViewById<Button>(R.id.btnDeleteAccount)?.setOnClickListener {
            showDeleteAccountDialog()
        }

        view?.findViewById<Button>(R.id.btnMyOffers)?.setOnClickListener {
            startActivity(Intent(requireContext(), OffersActivity::class.java))
        }
    }

    private fun showDeleteAccountDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("⚠️ Delete Account")
            .setMessage("Are you sure you want to permanently delete your account? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteAccount()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAccount() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.delete()
            ?.addOnSuccessListener {
                Toast.makeText(requireContext(), "Account deleted successfully", Toast.LENGTH_SHORT).show()
                startActivity(Intent(requireContext(), LoginActivity::class.java))
                requireActivity().finish()
            }
            ?.addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to delete account: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadUserData() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            // Load basic user info with safe calls
            tvDisplayName?.text = user.displayName ?: "No Name"
            tvEmail?.text = "Email: ${user.email ?: "No Email"}"

            // Load avatar with safe call
            user.photoUrl?.let { photoUrl ->
                ivAvatar?.let { imageView ->
                    Glide.with(this)
                        .load(photoUrl)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .into(imageView)
                }
            }

            // Load additional user data from Firestore
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val data = document.data
                        data?.let { userData ->
                            // Update additional profile fields if they exist
                            view?.findViewById<TextView>(R.id.tvName)?.text =
                                userData["fullName"]?.toString() ?: "No Name"
                            view?.findViewById<TextView>(R.id.tvPhone)?.text =
                                "Phone: ${userData["phone"]?.toString() ?: "No Phone"}"
                            view?.findViewById<TextView>(R.id.tvAddress)?.text =
                                userData["address"]?.toString() ?: "No Address"
                            view?.findViewById<TextView>(R.id.tvBio)?.text =
                                "Bio: ${userData["bio"]?.toString() ?: "No Bio"}"
                            view?.findViewById<TextView>(R.id.tvUsername)?.text =
                                "Username: ${userData["username"]?.toString() ?: "No Username"}"
                            view?.findViewById<TextView>(R.id.tvGender)?.text =
                                userData["gender"]?.toString() ?: "Not Specified"
                            view?.findViewById<TextView>(R.id.tvBirthday)?.text =
                                "Birthday: ${userData["birthday"]?.toString() ?: "Not Set"}"
                            view?.findViewById<TextView>(R.id.tvInterest)?.text =
                                "Interests: ${userData["interests"]?.toString() ?: "None"}"
                        }
                    }
                }
                .addOnFailureListener {
                    // Handle error silently
                }
        }
    }
}