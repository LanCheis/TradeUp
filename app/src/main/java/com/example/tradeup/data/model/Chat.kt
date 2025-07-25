package com.example.tradeup.data.model

data class Chat(
    val id: String = "",
    val participant1Id: String = "",
    val participant1Name: String = "",
    val participant1Avatar: String = "",
    val participant2Id: String = "",
    val participant2Name: String = "",
    val participant2Avatar: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val unreadCount1: Int = 0,
    val unreadCount2: Int = 0,
    val isBlocked: Boolean = false,
    val blockedBy: String = ""
)