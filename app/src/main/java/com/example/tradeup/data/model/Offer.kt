// app/src/main/java/com/example/tradeup/data/model/Offer.kt
package com.example.tradeup.data.model

data class Offer(
    val id: String = "",
    val listingId: String = "",
    val listingTitle: String = "",
    val listingImageUrl: String = "",
    val buyerId: String = "",
    val buyerName: String = "",
    val buyerAvatar: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val originalPrice: Double = 0.0,
    val offerPrice: Double = 0.0,
    val message: String = "",
    val status: String = "pending", // pending, accepted, rejected, counter, expired
    val counterOffer: Double = 0.0,
    val counterMessage: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (24 * 60 * 60 * 1000) // 24 hours
) {
    fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt

    fun getDisplayStatus(): String = when (status) {
        "pending" -> if (isExpired()) "Expired" else "Pending"
        "accepted" -> "Accepted"
        "rejected" -> "Rejected"
        "counter" -> "Counter Offer"
        "expired" -> "Expired"
        else -> "Unknown"
    }

    fun getStatusColor(): String = when (status) {
        "pending" -> "#FFA500" // Orange
        "accepted" -> "#4CAF50" // Green
        "rejected" -> "#F44336" // Red
        "counter" -> "#2196F3" // Blue
        "expired" -> "#9E9E9E" // Gray
        else -> "#9E9E9E"
    }
}