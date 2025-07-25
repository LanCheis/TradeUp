package com.example.tradeup.data.model

data class BlockedUser(
    val id: String = "",
    val blockerId: String = "",
    val blockerName: String = "",
    val blockedUserId: String = "",
    val blockedUserName: String = "",
    val timestamp: Long = System.currentTimeMillis()
)