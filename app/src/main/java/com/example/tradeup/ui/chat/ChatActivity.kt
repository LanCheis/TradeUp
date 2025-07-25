// File: app/src/main/java/com/example/tradeup/chat/ChatActivity.kt
package com.example.tradeup.chat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.tradeup.R
import com.example.tradeup.data.model.Message
import com.example.tradeup.data.remote.ChatRepository
import com.example.tradeup.databinding.ActivityChatBinding
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import java.util.UUID

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var chatRepository: ChatRepository
    private lateinit var messagesAdapter: MessagesAdapter

    private var chatId: String = ""
    private var otherUserId: String = ""
    private var otherUserName: String = ""
    private var otherUserAvatar: String = ""

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { uploadImageAndSend(it) }
    }

    companion object {
        const val EXTRA_OTHER_USER_ID = "other_user_id"
        const val EXTRA_OTHER_USER_NAME = "other_user_name"
        const val EXTRA_OTHER_USER_AVATAR = "other_user_avatar"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        chatRepository = ChatRepository()

        // Get user data from intent
        otherUserId = intent.getStringExtra(EXTRA_OTHER_USER_ID) ?: ""
        otherUserName = intent.getStringExtra(EXTRA_OTHER_USER_NAME) ?: ""
        otherUserAvatar = intent.getStringExtra(EXTRA_OTHER_USER_AVATAR) ?: ""

        if (otherUserId.isEmpty()) {
            Toast.makeText(this, "Error: User not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        setupRecyclerView()
        initializeChat()
    }

    private fun setupUI() {
        // FR-4.1.1: Display other user info in toolbar
        binding.tvUserName.text = otherUserName
        if (otherUserAvatar.isNotEmpty()) {
            Glide.with(this)
                .load(otherUserAvatar)
                .placeholder(R.drawable.ic_person)
                .circleCrop()
                .into(binding.ivUserAvatar)
        }

        // Click listeners
        binding.ivBack.setOnClickListener { finish() }

        // FR-4.1.3: Menu for block/report options
        binding.ivMenu.setOnClickListener { showOptionsMenu() }

        // FR-4.1.2: Send message
        binding.ivSend.setOnClickListener { sendMessage() }

        // FR-4.1.2: Attach image
        binding.ivAttachImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }
    }

    private fun setupRecyclerView() {
        messagesAdapter = MessagesAdapter()
        binding.rvMessages.apply {
            adapter = messagesAdapter
            layoutManager = LinearLayoutManager(this@ChatActivity)
        }
    }

    private fun initializeChat() {
        lifecycleScope.launch {
            val newChatId = chatRepository.getOrCreateChat(otherUserId, otherUserName, otherUserAvatar)
            if (newChatId != null) {
                chatId = newChatId
                observeMessages()
            } else {
                Toast.makeText(this@ChatActivity, "Error creating chat", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun observeMessages() {
        chatRepository.getMessages(chatId) { messages ->
            messagesAdapter.updateMessages(messages)
            if (messages.isNotEmpty()) {
                binding.rvMessages.smoothScrollToPosition(messages.size - 1)
            }
        }
    }

    // FR-4.1.2: Send text message
    private fun sendMessage() {
        val messageText = binding.etMessage.text.toString().trim()
        if (messageText.isEmpty()) return

        lifecycleScope.launch {
            val success = chatRepository.sendMessage(chatId, otherUserId, messageText)
            if (success) {
                binding.etMessage.setText("")
            } else {
                Toast.makeText(this@ChatActivity, "Failed to send message", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // FR-4.1.2: Upload image and send
    private fun uploadImageAndSend(imageUri: Uri) {
        val storage = FirebaseStorage.getInstance()
        val imageRef = storage.reference.child("chat_images/${UUID.randomUUID()}.jpg")

        imageRef.putFile(imageUri)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                imageRef.downloadUrl
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result
                    lifecycleScope.launch {
                        chatRepository.sendMessage(chatId, otherUserId, "", downloadUri.toString())
                    }
                } else {
                    Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // FR-4.1.3: Show options menu for block/report
    private fun showOptionsMenu() {
        val popup = PopupMenu(this, binding.ivMenu)
        popup.menuInflater.inflate(R.menu.menu_chat_options, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_report -> {
                    showReportDialog()
                    true
                }
                R.id.action_block -> {
                    showBlockDialog()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    // FR-4.1.3: Report user dialog
    private fun showReportDialog() {
        val reasons = arrayOf(
            "Inappropriate content",
            "Spam messages",
            "Harassment",
            "Scam/Fraud",
            "Other"
        )

        var selectedReason = ""

        AlertDialog.Builder(this)
            .setTitle("Report User")
            .setMessage("Why are you reporting ${otherUserName}?")
            .setSingleChoiceItems(reasons, -1) { _, which ->
                selectedReason = reasons[which]
            }
            .setPositiveButton("Report") { _, _ ->
                if (selectedReason.isNotEmpty()) {
                    reportUser(selectedReason)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // FR-4.1.3: Block user dialog
    private fun showBlockDialog() {
        AlertDialog.Builder(this)
            .setTitle("Block User")
            .setMessage("Are you sure you want to block ${otherUserName}? You won't receive messages from them anymore.")
            .setPositiveButton("Block") { _, _ ->
                blockUser()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // FR-4.1.3: Report user implementation
    private fun reportUser(reason: String) {
        lifecycleScope.launch {
            val success = chatRepository.reportChat(otherUserId, otherUserName, chatId, reason)
            if (success) {
                Toast.makeText(this@ChatActivity, "User reported successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@ChatActivity, "Failed to report user", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // FR-4.1.3: Block user implementation
    private fun blockUser() {
        lifecycleScope.launch {
            val success = chatRepository.blockUser(otherUserId, otherUserName)
            if (success) {
                Toast.makeText(this@ChatActivity, "User blocked successfully", Toast.LENGTH_SHORT).show()
                finish() // Close chat after blocking
            } else {
                Toast.makeText(this@ChatActivity, "Failed to block user", Toast.LENGTH_SHORT).show()
            }
        }
    }
}