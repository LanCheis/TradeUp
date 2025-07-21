package com.example.tradeup.data.model

data class Listing(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val category: String = "",
    val condition: String = "",
    val location: String = "",
    val imageUrl: String = "",
    val ownerUid: String = "",
    val status: String = "Available",
    val tags: List<String> = emptyList(),
    val views: Int = 0,
    val interactions: Int = 0
)
