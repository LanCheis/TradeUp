package com.example.tradeup.data.model

data class ChatReport(
    val id: String = "",
    val reporterId: String = "",
    val reporterName: String = "",
    val reportedUserId: String = "",
    val reportedUserName: String = "",
    val chatId: String = "",
    val reason: String = "",
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "pending"
)