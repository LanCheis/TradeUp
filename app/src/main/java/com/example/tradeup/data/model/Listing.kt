package com.example.tradeup.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.*

/**
 * FR-2.1.1: Listing data model for TradeUp marketplace items
 */
data class Listing(
    @DocumentId
    val id: String = "",

    // FR-2.1.1: Basic listing information
    val title: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val category: String = "",
    val condition: String = "",

    // FR-2.1.3: Location information
    val location: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,

    // FR-2.1.4: Image support (up to 10 images, JPEG/PNG)
    val imageUrls: List<String> = emptyList(),
    val thumbnailUrl: String? = null,

    // Seller information
    val sellerId: String = "",
    val sellerName: String = "",
    val sellerProfilePicUrl: String? = null,

    // FR-2.1.5: Listing settings
    val isNegotiable: Boolean = false,
    val isPromoted: Boolean = false,

    // FR-2.2.2: Listing status options
    val status: String = "available", // available, sold, paused, deleted

    // FR-2.2.3: Analytics data
    val viewCount: Int = 0,
    val favoriteCount: Int = 0,
    val inquiryCount: Int = 0,

    // Timestamps
    val createdAt: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now(),
    val updatedAt: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now(),

    // Search and recommendation metadata
    val tags: List<String> = emptyList(),
    val keywords: String = "", // Auto-generated from title + description

    // Transaction data
    val soldAt: com.google.firebase.Timestamp? = null,
    val soldPrice: Double? = null,
    val buyerId: String? = null
) {
    // FR-3.1.3: Helper function for distance calculation
    fun distanceFrom(userLat: Double, userLng: Double): Double? {
        return if (latitude != null && longitude != null) {
            calculateDistance(userLat, userLng, latitude, longitude)
        } else null
    }

    // Helper function to check if listing is active
    fun isActive(): Boolean = status == "available"

    // Helper function to check if listing belongs to user
    fun belongsToUser(userId: String): Boolean = sellerId == userId

    fun toPreviewData(): ListingPreview {
        return ListingPreview(
            id = id,
            title = title,
            price = price,
            thumbnailUrl = imageUrls.firstOrNull(),
            location = location,
            condition = condition,
            isNegotiable = isNegotiable,
            createdAt = createdAt.seconds // Convert Timestamp to Long
        )
    }

    companion object {
        // Listing status constants
        const val STATUS_AVAILABLE = "available"
        const val STATUS_SOLD = "sold"
        const val STATUS_PAUSED = "paused"
        const val STATUS_DELETED = "deleted"

        // Condition constants
        const val CONDITION_NEW = "New"
        const val CONDITION_LIKE_NEW = "Like New"
        const val CONDITION_GOOD = "Good"
        const val CONDITION_FAIR = "Fair"
        const val CONDITION_POOR = "Poor"

        fun getAllConditions(): List<String> {
            return listOf(CONDITION_NEW, CONDITION_LIKE_NEW, CONDITION_GOOD, CONDITION_FAIR, CONDITION_POOR)
        }
    }
}

/**
 * Lightweight listing data for lists and previews
 */
data class ListingPreview(
    val id: String,
    val title: String,
    val price: Double,
    val thumbnailUrl: String?,
    val location: String,
    val condition: String,
    val isNegotiable: Boolean,
    val createdAt: Long
)

/**
 * Calculate distance between two points using Haversine formula
 */
private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val earthRadius = 6371.0 // Earth's radius in kilometers

    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)

    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLng / 2) * Math.sin(dLng / 2)

    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

    return earthRadius * c
}