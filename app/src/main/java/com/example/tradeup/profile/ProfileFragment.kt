package com.example.tradeup.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.tradeup.R
import com.example.tradeup.auth.LoginActivity
import com.example.tradeup.data.remote.UserRepository
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment() {

    private lateinit var ivAvatar: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvBio: TextView
    private lateinit var tvUsername: TextView
    private lateinit var tvBirthday: TextView
    private lateinit var tvInterest: TextView
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

        // Bind views from layout
        ivAvatar = view.findViewById(R.id.ivAvatar)
        tvName = view.findViewById(R.id.tvName)
        tvEmail = view.findViewById(R.id.tvEmail)
        tvPhone = view.findViewById(R.id.tvPhone)
        tvBio = view.findViewById(R.id.tvBio)
        tvUsername = view.findViewById(R.id.tvUsername)
        tvBirthday = view.findViewById(R.id.tvBirthday)
        tvInterest = view.findViewById(R.id.tvInterest)
        btnEditProfile = view.findViewById(R.id.btnEditProfile)

        // Load data
        loadUserData()

        // Button actions
        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }

        btnEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileFragment::class.java))
        }
    }

    private fun loadUserData() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        UserRepository.getUserProfile(uid) { user ->
            user?.let {
                tvEmail.text = getString(R.string.email_label, it.email)
                tvPhone.text = getString(R.string.phone_label, it.phone)
                tvBio.text = getString(R.string.bio_label, it.bio)
                tvUsername.text = getString(R.string.username_label, it.username)
                tvBirthday.text = getString(R.string.birthday_label, it.birthday)
                tvInterest.text = getString(R.string.interest_label, it.interests)

                Glide.with(requireContext())
                    .load(it.profileImageUrl)
                    .into(ivAvatar)
            } ?: run {
                Toast.makeText(requireContext(), "Không tải được hồ sơ", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
