package com.example.tradeup.data.model

data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "text", // text, image, offer
    val isRead: Boolean = false,
    val imageUrl: String = ""
)