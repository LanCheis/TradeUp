package com.example.tradeup.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Listing(
    @DocumentId
    var id: String = "",

    // FR-2.1.1: Required fields
    val title: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val category: String = "",
    val condition: String = "",
    val location: String = "",
    val imageUrls: List<String> = emptyList(), // At least 1 photo required

    // FR-2.1.2: Optional fields
    val additionalTags: List<String> = emptyList(),
    val isNegotiable: Boolean = false, // Item behavior: negotiable pricing

    // FR-2.1.3: Location data
    val latitude: Double? = null,
    val longitude: Double? = null,

    // System fields
    val sellerId: String = "",
    val sellerName: String = "",
    val sellerImageUrl: String = "",

    // FR-2.2.2: Listing status
    val status: String = "Available", // Available, Sold, Paused

    // FR-2.2.3: Analytics
    val views: Int = 0,
    val interactions: Int = 0, // Messages, offers, etc.

    // Timestamps
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)

// Helper enums
object ListingStatus {
    const val AVAILABLE = "Available"
    const val SOLD = "Sold"
    const val PAUSED = "Paused"
}

object ItemCondition {
    const val NEW = "New"
    const val LIKE_NEW = "Like New"
    const val GOOD = "Good"
    const val FAIR = "Fair"
    const val POOR = "Poor"
}

object Categories {
    val ALL = listOf(
        "Electronics",
        "Clothing",
        "Home & Garden",
        "Sports",
        "Books",
        "Automotive",
        "Toys",
        "Other"
    )
}