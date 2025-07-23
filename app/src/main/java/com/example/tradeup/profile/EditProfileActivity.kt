package com.example.tradeup.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.tradeup.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
//import de.hdodenhof.circleimageview.CircleImageView
import java.util.*

class EditProfileActivity : AppCompatActivity() {

    private lateinit var ivAvatar: ImageView
    private lateinit var btnChangeAvatar: Button
    private lateinit var etFullName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etAddress: EditText
    private lateinit var etBio: EditText
    private lateinit var etUsername: EditText
    private lateinit var spGender: Spinner
    private lateinit var etBirthday: EditText
    private lateinit var etInterests: EditText
    private lateinit var btnSave: Button
    private lateinit var progressBar: ProgressBar

    private var selectedImageUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            Glide.with(this)
                .load(it)
                .placeholder(R.drawable.ic_profile)
                .into(ivAvatar)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        initViews()
        setupSpinner()
        setupClickListeners()
        loadCurrentUserData()
    }

    private fun initViews() {
        ivAvatar = findViewById(R.id.ivAvatar)
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar)
        etFullName = findViewById(R.id.etFullName)
        etPhone = findViewById(R.id.etPhone)
        etAddress = findViewById(R.id.etAddress)
        etBio = findViewById(R.id.etBio)
        etUsername = findViewById(R.id.etUsername)
        etBirthday = findViewById(R.id.etBirthday)
        etInterests = findViewById(R.id.etInterests)
        btnSave = findViewById(R.id.btnSave)
        progressBar = findViewById(R.id.progressBar)

        // Simple approach - just use the main ID
        spGender = findViewById(R.id.spinnerGender)

        // Go back to Profile
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupSpinner() {
        val genders = arrayOf("Select Gender", "Male", "Female", "Other", "Cheese", "Not prefer to say")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genders)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spGender.adapter = adapter
    }

    private fun setupClickListeners() {
        btnChangeAvatar.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        btnSave.setOnClickListener {
            saveProfile()
        }

        // Handle back button
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun loadCurrentUserData() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            // Load basic info
            etFullName.setText(user.displayName ?: "")

            // Load avatar
            user.photoUrl?.let { photoUrl ->
                Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(ivAvatar)
            }

            // Load additional data from Firestore
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val data = document.data
                        etPhone.setText(data?.get("phone")?.toString() ?: "")
                        etAddress.setText(data?.get("address")?.toString() ?: "")
                        etBio.setText(data?.get("bio")?.toString() ?: "")
                        etUsername.setText(data?.get("username")?.toString() ?: "")
                        etBirthday.setText(data?.get("birthday")?.toString() ?: "")
                        etInterests.setText(data?.get("interests")?.toString() ?: "")

                        // Set gender spinner
                        val gender = data?.get("gender")?.toString() ?: ""
                        val genderAdapter = spGender.adapter as ArrayAdapter<String>
                        val position = genderAdapter.getPosition(gender)
                        if (position >= 0) {
                            spGender.setSelection(position)
                        }
                    }
                }
        }
    }

    private fun saveProfile() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        val fullName = etFullName.text.toString().trim()
        if (fullName.isEmpty()) {
            etFullName.error = "Please enter your full name"
            etFullName.requestFocus()
            showLoading(false)
            return
        }

        // If image is selected, upload it first
        if (selectedImageUri != null) {
            uploadImage { imageUrl ->
                updateUserProfile(imageUrl)
            }
        } else {
            updateUserProfile(null)
        }
    }

    private fun uploadImage(callback: (String?) -> Unit) {
        selectedImageUri?.let { uri ->
            val fileName = "profile_images/${FirebaseAuth.getInstance().currentUser?.uid}.jpg"
            val storageRef = FirebaseStorage.getInstance().reference.child(fileName)

            storageRef.putFile(uri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        callback(downloadUri.toString())
                    }.addOnFailureListener {
                        callback(null)
                    }
                }
                .addOnFailureListener {
                    callback(null)
                }
        } ?: callback(null)
    }

    private fun updateUserProfile(imageUrl: String?) {
        val currentUser = FirebaseAuth.getInstance().currentUser!!

        val userData = hashMapOf<String, Any>(
            "fullName" to etFullName.text.toString().trim(),
            "phone" to etPhone.text.toString().trim(),
            "address" to etAddress.text.toString().trim(),
            "bio" to etBio.text.toString().trim(),
            "username" to etUsername.text.toString().trim(),
            "gender" to spGender.selectedItem.toString(),
            "birthday" to etBirthday.text.toString().trim(),
            "interests" to etInterests.text.toString().trim(),
            "updatedAt" to System.currentTimeMillis()
        )

        imageUrl?.let {
            userData["profileImageUrl"] = it
        }

        // Update Firestore
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(currentUser.uid)
            .set(userData)
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        btnSave.isEnabled = !show
    }
}