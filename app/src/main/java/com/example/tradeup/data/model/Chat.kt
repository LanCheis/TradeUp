package com.example.tradeup.data.model

data class Chat(
    val id: String = "",
    val participants: List<String> = emptyList(), // User IDs
    val participantNames: Map<String, String> = emptyMap(), // userId -> userName
    val listingId: String = "", // Related to which item
    val listingTitle: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val lastSenderId: String = "",
    val unreadCount: Map<String, Int> = emptyMap() // userId -> unread count
)