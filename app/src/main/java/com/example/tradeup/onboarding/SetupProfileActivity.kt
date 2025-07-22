package com.example.tradeup.onboarding

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.tradeup.MainActivity
import com.example.tradeup.R
import com.example.tradeup.data.model.User
import com.example.tradeup.data.remote.UserRepository
import com.example.tradeup.utils.CloudinaryHelper
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import de.hdodenhof.circleimageview.CircleImageView

class SetupProfileActivity : AppCompatActivity() {

    private lateinit var ivAvatar: CircleImageView
    private lateinit var btnChangeAvatar: Button
    private lateinit var etFullName: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etBio: TextInputEditText
    private lateinit var spinnerGender: Spinner
    private lateinit var etInterests: TextInputEditText
    private lateinit var btnContinue: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvWelcome: TextView
    private lateinit var tvStep: TextView

    private var selectedImageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_profile)

        initViews()
        setupSpinner()
        setupClickListeners()
        loadCurrentUserInfo()
    }

    private fun initViews() {
        ivAvatar = findViewById(R.id.ivAvatar)
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar)
        etFullName = findViewById(R.id.etFullName)
        etPhone = findViewById(R.id.etPhone)
        etBio = findViewById(R.id.etBio)
        spinnerGender = findViewById(R.id.spinnerGender)
        etInterests = findViewById(R.id.etInterests)
        btnContinue = findViewById(R.id.btnContinue)
        progressBar = findViewById(R.id.progressBar)
        tvWelcome = findViewById(R.id.tvWelcome)
        tvStep = findViewById(R.id.tvStep)

        progressBar.visibility = View.GONE
    }

    private fun setupSpinner() {
        val genders = arrayOf("Chọn giới tính", "Nam", "Nữ", "Khác")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, genders)
        spinnerGender.adapter = adapter
    }

    private fun setupClickListeners() {
        btnChangeAvatar.setOnClickListener {
            selectProfileImage()
        }

        ivAvatar.setOnClickListener {
            selectProfileImage()
        }

        btnContinue.setOnClickListener {
            if (validateForm()) {
                saveProfileAndContinue()
            }
        }
    }

    private fun loadCurrentUserInfo() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            tvWelcome.text = "Chào mừng, ${currentUser.displayName ?: currentUser.email}!"
            etFullName.setText(currentUser.displayName ?: "")

            // Load existing avatar if available
            currentUser.photoUrl?.let { photoUrl ->
                loadImageIntoView(ivAvatar, photoUrl.toString())
            }
        }
    }

    private fun selectProfileImage() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            selectedImageUri?.let { uri ->
                loadImageIntoView(ivAvatar, uri.toString())
                btnChangeAvatar.text = "✓ Đã chọn ảnh"
            }
        }
    }

    private fun loadImageIntoView(imageView: CircleImageView, imageSource: String) {
        val requestOptions = RequestOptions()
            .placeholder(R.drawable.ic_avatar_placeholder)
            .error(R.drawable.ic_avatar_placeholder)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()

        try {
            Glide.with(this)
                .load(imageSource)
                .apply(requestOptions)
                .into(imageView)
        } catch (e: Exception) {
            imageView.setImageResource(R.drawable.ic_avatar_placeholder)
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        // Validate full name
        if (etFullName.text.toString().trim().isEmpty()) {
            etFullName.error = "Vui lòng nhập họ tên"
            isValid = false
        }

        // Validate phone
        val phone = etPhone.text.toString().trim()
        if (phone.isEmpty()) {
            etPhone.error = "Vui lòng nhập số điện thoại"
            isValid = false
        } else if (phone.length < 10) {
            etPhone.error = "Số điện thoại không hợp lệ"
            isValid = false
        }

        // Validate gender
        if (spinnerGender.selectedItemPosition == 0) {
            Toast.makeText(this, "Vui lòng chọn giới tính", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        // Bio is optional but should have some content for marketplace
        if (etBio.text.toString().trim().length < 10) {
            etBio.error = "Vui lòng viết giới thiệu ít nhất 10 ký tự"
            isValid = false
        }

        return isValid
    }

    private fun saveProfileAndContinue() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        showLoading(true)
        btnContinue.text = "Đang lưu thông tin..."

        // If user selected new avatar, upload it first
        if (selectedImageUri != null) {
            uploadAvatarThenSaveProfile(currentUser.uid)
        } else {
            // No new avatar, save profile directly
            saveUserProfile(currentUser.uid, currentUser.photoUrl?.toString() ?: "")
        }
    }

    private fun uploadAvatarThenSaveProfile(userId: String) {
        val selectedUri = selectedImageUri ?: return

        // ✅ Fixed: Create separate variables with explicit types for lambda parameters
        val onSuccessCallback: (String) -> Unit = { avatarUrl ->
            saveUserProfile(userId, avatarUrl)
        }

        val onFailureCallback: (String) -> Unit = { error ->
            showLoading(false)
            Toast.makeText(this, "❌ Lỗi tải ảnh: $error", Toast.LENGTH_LONG).show()
            btnContinue.text = "Tiếp tục"
        }

        val onProgressCallback: (Int) -> Unit = { progress ->
            btnContinue.text = "Đang tải ảnh... $progress%"
        }

        CloudinaryHelper.uploadProfileImage(
            context = this,
            imageUri = selectedUri,
            userId = userId,
            onSuccess = onSuccessCallback,
            onFailure = onFailureCallback,
            onProgress = onProgressCallback
        )
    }

    private fun saveUserProfile(userId: String, avatarUrl: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        val userProfile = User(
            uid = userId,
            name = etFullName.text.toString().trim(),
            email = currentUser.email ?: "",
            phone = etPhone.text.toString().trim(),
            bio = etBio.text.toString().trim(),
            gender = spinnerGender.selectedItem.toString(),
            interests = etInterests.text.toString().trim(),
            profileImageUrl = avatarUrl,
            // Mark profile as completed
            username = generateUsername(etFullName.text.toString().trim()),
            address = "", // Will be updated later
            birthday = "" // Will be updated later
        )

        // ✅ Fixed: Create separate variables with explicit types for callback
        val onCompleteCallback: (Boolean, String?) -> Unit = { success, error ->
            showLoading(false)

            if (success) {
                // Profile setup completed successfully
                Toast.makeText(this, "🎉 Hồ sơ đã được tạo thành công!", Toast.LENGTH_SHORT).show()
                navigateToMainApp()
            } else {
                Toast.makeText(this, "❌ Lỗi lưu hồ sơ: $error", Toast.LENGTH_LONG).show()
                btnContinue.text = "Tiếp tục"
            }
        }

        UserRepository.saveUserProfile(userProfile, onCompleteCallback)
    }

    private fun generateUsername(fullName: String): String {
        // Generate simple username from full name
        val cleanName = fullName.lowercase()
            .replace(" ", "")
            .replace("[^a-zA-Z0-9]".toRegex(), "")
        return "${cleanName}${System.currentTimeMillis() % 10000}"
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnContinue.isEnabled = !show

        // Disable form when loading
        etFullName.isEnabled = !show
        etPhone.isEnabled = !show
        etBio.isEnabled = !show
        etInterests.isEnabled = !show
        spinnerGender.isEnabled = !show
        btnChangeAvatar.isEnabled = !show
    }

    private fun navigateToMainApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    // ✅ Fixed: Override with super call
    override fun onBackPressed() {
        super.onBackPressed() // Call super method first
        Toast.makeText(this, "Vui lòng hoàn thành thiết lập hồ sơ", Toast.LENGTH_SHORT).show()
        // Don't actually go back - prevent back navigation during setup
    }
}