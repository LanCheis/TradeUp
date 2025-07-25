package com.example.tradeup.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

// FR-2.1.1, FR-2.1.2: Data structure for listings
data class Listing(
    @DocumentId
    var id: String = "",

    // FR-2.1.1: Required fields - every listing MUST have these
    val title: String = "",                    // What item is being sold
    val description: String = "",              // Details about the item
    val price: Double = 0.0,                  // How much it costs
    val category: String = "",                // Electronics, Clothing, etc.
    val condition: String = "",               // New, Used, Like New, etc.
    val location: String = "",                // Where item is located
    val imageUrls: List<String> = emptyList(), // At least 1 photo required

    // FR-2.1.2: Optional fields - nice to have but not required
    val additionalTags: List<String> = emptyList(), // Extra keywords for search
    val isNegotiable: Boolean = false,        // Can buyers make offers?

    // FR-2.1.3 & FR-6.1: GPS location data (optional)
    val latitude: Double? = 0.0,             // GPS coordinates
    val longitude: Double? = 0.0,

    // System fields - automatically filled
    val sellerId: String = "",                // Who's selling this
    val sellerName: String = "",
    val sellerImageUrl: String = "",

    // FR-2.2.2: Listing status management
    val status: String = "Available",         // Available, Sold, Paused

    // FR-2.2.3 & FR-3.1.3: Analytics tracking
    val views: Int = 0,                       // How many people viewed
    val interactions: Int = 0,                // Messages, offers, etc.
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),

    // FR-3.1.1: Search-specific fields
    val keywords: List<String> = emptyList(), // Auto-generated from title/description

)

// Helper constants to avoid typos
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
        "Electronics", "Clothing", "Home & Garden",
        "Sports", "Books", "Automotive", "Toys", "Other"
    )
}

