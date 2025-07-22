package com.example.tradeup.profile

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.tradeup.R
import com.example.tradeup.data.model.User
import com.example.tradeup.data.remote.UserRepository
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar

class EditProfileFragment : Fragment() {

    private lateinit var btnBack: ImageButton
    private lateinit var btnSave: Button
    private lateinit var ivAvatar: ImageView
    private lateinit var etUsername: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etBio: TextInputEditText
    private lateinit var etInterest: TextInputEditText
    private lateinit var etBirthday: TextInputEditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupListeners()
        loadCurrentUserData()
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        btnSave = view.findViewById(R.id.btnSave)
        ivAvatar = view.findViewById(R.id.ivAvatar)
        etUsername = view.findViewById(R.id.etUsername)
        etPhone = view.findViewById(R.id.etPhone)
        etBio = view.findViewById(R.id.etBio)
        etInterest = view.findViewById(R.id.etInterest)
        etBirthday = view.findViewById(R.id.etBirthday)
    }

    private fun setupListeners() {
        // ✅ FIXED: Use parentFragmentManager to go back
        btnBack.setOnClickListener {
            try {
                parentFragmentManager.popBackStack()
            } catch (e: Exception) {
                requireActivity().onBackPressed()
            }
        }

        // Show date picker for birthday
        etBirthday.setOnClickListener {
            showDatePicker()
        }

        // Save profile button
        btnSave.setOnClickListener {
            saveProfile()
        }
    }

    private fun loadCurrentUserData() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        UserRepository.getUserProfile(uid) { user ->
            if (user != null && isAdded) {
                // Pre-fill form with current data
                etUsername.setText(user.username)
                etPhone.setText(user.phone)
                etBio.setText(user.bio)
                etInterest.setText(user.interests)
                etBirthday.setText(user.birthday)
            }
        }
    }

    private fun saveProfile() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        // ✅ Show loading state
        btnSave.isEnabled = false
        btnSave.text = "Đang lưu..."

        // Get current user data first
        UserRepository.getUserProfile(currentUser.uid) { existingUser ->
            val updatedUser = User(
                uid = currentUser.uid,
                email = currentUser.email ?: "",
                name = existingUser?.name ?: currentUser.displayName ?: "",
                username = etUsername.text.toString().trim(),
                phone = etPhone.text.toString().trim(),
                bio = etBio.text.toString().trim(),
                interests = etInterest.text.toString().trim(),
                birthday = etBirthday.text.toString().trim(),
                // Keep existing data that we're not editing
                address = existingUser?.address ?: "",
                gender = existingUser?.gender ?: "",
                profileImageUrl = existingUser?.profileImageUrl ?: "",
                rating = existingUser?.rating ?: 0.0,
                totalTransactions = existingUser?.totalTransactions ?: 0
            )

            UserRepository.saveUserProfile(updatedUser) { success, error ->
                btnSave.isEnabled = true
                btnSave.text = "Save"

                if (isAdded) {
                    if (success) {
                        Toast.makeText(requireContext(), "✅ Profile updated!", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack() // Go back to profile
                    } else {
                        Toast.makeText(requireContext(), "❌ Failed to save: $error", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                etBirthday.setText(
                    String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
                )
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }
}