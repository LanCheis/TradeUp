package com.example.tradeup.home

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.tradeup.R
import com.example.tradeup.data.model.User
import com.example.tradeup.data.remote.UserRepository
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity() {

    private lateinit var ivProfile: ImageView
    private lateinit var etName: EditText
    private lateinit var etBio: EditText
    private lateinit var etPhone: EditText
    private lateinit var btnSave: Button
    private var selectedImageUri: Uri? = null

    private val PICK_IMAGE = 101
    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        ivProfile = findViewById(R.id.ivProfile)
        etName = findViewById(R.id.etName)
        etBio = findViewById(R.id.etBio)
        etPhone = findViewById(R.id.etPhone)
        btnSave = findViewById(R.id.btnSave)

        loadUser()

        ivProfile.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE)
        }

        btnSave.setOnClickListener {
            val updatedUser = User(
                uid = currentUid,
                name = etName.text.toString(),
                bio = etBio.text.toString(),
                phone = etPhone.text.toString(),
                email = FirebaseAuth.getInstance().currentUser?.email ?: "",
                profileImageUrl = selectedImageUri?.toString() ?: ""
            )

            UserRepository.saveUserProfile(updatedUser) { success, error ->
                if (success) Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show()
                else Toast.makeText(this, error ?: "Update failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            ivProfile.setImageURI(selectedImageUri)
        }
    }

    private fun loadUser() {
        UserRepository.getUserProfile(currentUid) { user ->
            user?.let {
                etName.setText(it.name)
                etBio.setText(it.bio)
                etPhone.setText(it.phone)
                selectedImageUri = Uri.parse(it.profileImageUrl)
                Glide.with(this).load(it.profileImageUrl).into(ivProfile)
            }
        }
    }
}
