package com.example.tradeup.home

import android.content.Intent
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.tradeup.R
import com.google.firebase.auth.FirebaseAuth
import java.util.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etAddress: EditText
    private lateinit var etBio: EditText
    private lateinit var etUsername: EditText
    private lateinit var tvEmail: TextView
    private lateinit var spGender: Spinner
    private lateinit var etBirthday: EditText
    private lateinit var etInterest: EditText
    private lateinit var btnSave: Button

    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        etName = findViewById(R.id.etName)
        etPhone = findViewById(R.id.etPhone)
        etAddress = findViewById(R.id.etAddress)
        etBio = findViewById(R.id.etBio)
        etUsername = findViewById(R.id.etUsername)
        tvEmail = findViewById(R.id.tvEmail)
        spGender = findViewById(R.id.spGender)
        etBirthday = findViewById(R.id.etBirthday)
        etInterest = findViewById(R.id.etInterest)
        btnSave = findViewById(R.id.btnSave)

        val genders = arrayOf("Nam", "Nữ", "Khác")
        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, genders)
        spGender.adapter = genderAdapter

        etBirthday.setOnClickListener {
            showDatePicker()
        }

        btnSave.setOnClickListener {
            saveProfile()
        }

        loadUser()
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val dialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                etBirthday.setText(String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        dialog.show()
    }

    private fun saveProfile() {
        val user = com.example.tradeup.data.model.User(
            uid = currentUid,
            name = etName.text.toString(),
            email = FirebaseAuth.getInstance().currentUser?.email ?: "",
            phone = etPhone.text.toString(),
            address = etAddress.text.toString(),
            bio = etBio.text.toString(),
            username = etUsername.text.toString(),
            gender = spGender.selectedItem.toString(),
            birthday = etBirthday.text.toString(),
            interests = etInterest.text.toString(),
            profileImageUrl = ""
        )

        com.example.tradeup.data.remote.UserRepository.saveUserProfile(user) { success, error ->
            runOnUiThread {
                if (success) {
                    Toast.makeText(this, "✅ Đã cập nhật hồ sơ", Toast.LENGTH_SHORT).show()

                    // ✅ Quay về màn hình chính
                    val intent = Intent(this, com.example.tradeup.home.HomeActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "❌ Lỗi: $error", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadUser() {
        com.example.tradeup.data.remote.UserRepository.getUserProfile(currentUid) { user ->
            user?.let {
                etName.setText(it.name)
                etPhone.setText(it.phone)
                etAddress.setText(it.address)
                etBio.setText(it.bio)
                etUsername.setText(it.username)
                tvEmail.text = "Email: ${it.email}"
                etBirthday.setText(it.birthday)
                etInterest.setText(it.interests)
                val genderIndex = when (it.gender) {
                    "Nam" -> 0
                    "Nữ" -> 1
                    "Khác" -> 2
                    else -> 0
                }
                spGender.setSelection(genderIndex)
            }
        }
    }
}
