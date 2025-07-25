package com.example.tradeup.data.remote

import com.example.tradeup.data.model.BlockedUser
import com.example.tradeup.data.model.Chat
import com.example.tradeup.data.model.ChatReport
import com.example.tradeup.data.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class ChatRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // FR-4.1.1: Get or create chat between two users
    suspend fun getOrCreateChat(otherUserId: String, otherUserName: String, otherUserAvatar: String): String? {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return null
            val currentUser = auth.currentUser!!

            // Create chat ID by combining user IDs in consistent order
            val chatId = if (currentUserId < otherUserId) {
                "${currentUserId}_${otherUserId}"
            } else {
                "${otherUserId}_${currentUserId}"
            }

            // Check if chat already exists
            val existingChat = firestore.collection("chats")
                .document(chatId)
                .get()
                .await()

            if (!existingChat.exists()) {
                // Create new chat
                val chatData = hashMapOf(
                    "id" to chatId,
                    "participant1Id" to currentUserId,
                    "participant1Name" to (currentUser.displayName ?: "User"),
                    "participant1Avatar" to (currentUser.photoUrl?.toString() ?: ""),
                    "participant2Id" to otherUserId,
                    "participant2Name" to otherUserName,
                    "participant2Avatar" to otherUserAvatar,
                    "lastMessage" to "",
                    "lastMessageTime" to System.currentTimeMillis(),
                    "unreadCount1" to 0,
                    "unreadCount2" to 0,
                    "isBlocked" to false,
                    "blockedBy" to ""
                )

                firestore.collection("chats")
                    .document(chatId)
                    .set(chatData)
                    .await()
            }

            chatId
        } catch (e: Exception) {
            null
        }
    }

    // FR-4.1.1: Send message
    suspend fun sendMessage(chatId: String, receiverId: String, content: String, imageUrl: String? = null): Boolean {
        return try {
            val currentUser = auth.currentUser ?: return false

            val messageData = hashMapOf(
                "chatId" to chatId,
                "senderId" to currentUser.uid,
                "senderName" to (currentUser.displayName ?: "User"),
                "senderAvatar" to (currentUser.photoUrl?.toString() ?: ""),
                "receiverId" to receiverId,
                "content" to content,
                "imageUrl" to imageUrl,
                "timestamp" to System.currentTimeMillis(),
                "isRead" to false
            )

            // Add message to messages collection
            firestore.collection("messages")
                .add(messageData)
                .await()

            // Update chat with last message
            firestore.collection("chats")
                .document(chatId)
                .update(
                    mapOf(
                        "lastMessage" to content,
                        "lastMessageTime" to System.currentTimeMillis()
                    )
                )
                .await()

            true
        } catch (e: Exception) {
            false
        }
    }

    // FR-4.1.3: Check if user is blocked
    suspend fun isUserBlocked(userId: String): Boolean {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return false

            val result = firestore.collection("blocked_users")
                .whereEqualTo("blockerId", currentUserId)
                .whereEqualTo("blockedUserId", userId)
                .get()
                .await()

            !result.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    // FR-4.1.3: Block a user
    suspend fun blockUser(userId: String, userName: String): Boolean {
        return try {
            val currentUser = auth.currentUser ?: return false

            val blockData = hashMapOf(
                "blockerId" to currentUser.uid,
                "blockerName" to (currentUser.displayName ?: "User"),
                "blockedUserId" to userId,
                "blockedUserName" to userName,
                "timestamp" to System.currentTimeMillis()
            )

            firestore.collection("blocked_users")
                .add(blockData)
                .await()

            true
        } catch (e: Exception) {
            false
        }
    }

    // FR-4.1.3: Report inappropriate conversation
    suspend fun reportChat(reportedUserId: String, reportedUserName: String, chatId: String, reason: String, description: String = ""): Boolean {
        return try {
            val currentUser = auth.currentUser ?: return false

            val reportData = hashMapOf(
                "reporterId" to currentUser.uid,
                "reporterName" to (currentUser.displayName ?: "User"),
                "reportedUserId" to reportedUserId,
                "reportedUserName" to reportedUserName,
                "chatId" to chatId,
                "reason" to reason,
                "description" to description,
                "timestamp" to System.currentTimeMillis(),
                "status" to "pending"
            )

            firestore.collection("chat_reports")
                .add(reportData)
                .await()

            true
        } catch (e: Exception) {
            false
        }
    }

    // Get messages for chat (simplified version)
    fun getMessages(chatId: String, callback: (List<Message>) -> Unit) {
        firestore.collection("messages")
            .whereEqualTo("chatId", chatId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    callback(emptyList())
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Message(
                            id = doc.id,
                            chatId = doc.getString("chatId") ?: "",
                            senderId = doc.getString("senderId") ?: "",
                            senderName = doc.getString("senderName") ?: "",
                            senderAvatar = doc.getString("senderAvatar") ?: "",
                            receiverId = doc.getString("receiverId") ?: "",
                            content = doc.getString("content") ?: "",
                            imageUrl = doc.getString("imageUrl"),
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            isRead = doc.getBoolean("isRead") ?: false
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                callback(messages)
            }
    }
}