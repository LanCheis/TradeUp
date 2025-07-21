package com.example.tradeup.data.model

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatar: String = "",
    val receiverId: String = "",
    val message: String = "",
    val imageUrl: String = "", // For image messages
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val messageType: String = "text" // text, image, offer
)

data class ChatRoom(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val listingId: String = "", // Associated listing
    val createdAt: Long = System.currentTimeMillis()
)
