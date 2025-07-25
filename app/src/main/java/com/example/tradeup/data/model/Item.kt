package com.example.tradeup.data.model

import java.util.Date

// FR-2.1.1 & FR-2.1.2: Item model with required and optional fields
data class Item(
    val id: String = "",
    val sellerId: String = "", // User who created the listing
    val sellerName: String = "", // Display name of seller

    // FR-2.1.1: Required fields
    val title: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val category: String = "",
    val condition: String = "", // New, Like New, Good, Fair, Poor
    val location: String = "",
    val photos: List<String> = emptyList(), // At least 1 photo required

    // FR-2.1.2: Optional fields
    val additionalTags: List<String> = emptyList(),
    val itemBehavior: String = "", // Optional behavior description

    // System fields
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val isActive: Boolean = true, // For soft deletion/deactivation
    val viewCount: Int = 0 // For analytics
)