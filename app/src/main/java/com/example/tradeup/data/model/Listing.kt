package com.example.tradeup.data.model

data class Listing(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val imageUrl: String = "",
    val ownerUid: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
