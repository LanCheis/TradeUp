package com.example.tradeup.data.model

data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatar: String = "",
    val receiverId: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)