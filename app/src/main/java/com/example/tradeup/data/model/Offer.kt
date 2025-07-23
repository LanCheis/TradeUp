package com.example.tradeup.data.model

data class Offer(
    val id: String = "",
    val listingId: String = "",
    val listingTitle: String = "",
    val listingImageUrl: String = "",
    val buyerId: String = "",
    val buyerName: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val originalPrice: Double = 0.0,
    val offerPrice: Double = 0.0,
    val message: String = "",
    val status: String = "pending", // pending, accepted, rejected, withdrawn, counter
    val counterOffer: Double = 0.0,
    val counterMessage: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (24 * 60 * 60 * 1000) // 24 hours
) {
    fun isExpired(): Boolean {
        return System.currentTimeMillis() > expiresAt
    }

    fun getStatusDisplay(): String {
        return when (status) {
            "pending" -> "⏳ Pending"
            "accepted" -> "✅ Accepted"
            "rejected" -> "❌ Rejected"
            "withdrawn" -> "🔄 Withdrawn"
            "counter" -> "💬 Counter Offer"
            else -> "❓ Unknown"
        }
    }
}

data class Transaction(
    val id: String = "",
    val listingId: String = "",
    val listingTitle: String = "",
    val buyerId: String = "",
    val buyerName: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val finalPrice: Double = 0.0,
    val offerId: String = "", // Reference to accepted offer
    val status: String = "completed", // completed, cancelled, disputed
    val completedAt: Long = System.currentTimeMillis(),
    val rating: Float = 0f,
    val review: String = "",
    val hasRatedBuyer: Boolean = false,
    val hasRatedSeller: Boolean = false
)