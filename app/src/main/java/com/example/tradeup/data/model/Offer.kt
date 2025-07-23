// File: app/src/main/java/com/example/tradeup/data/model/Offer.kt

package com.example.tradeup.data.model

import com.google.firebase.Timestamp

data class Offer(
    val id: String = "",
    val listingId: String = "",
    val buyerId: String = "",
    val buyerName: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val originalPrice: Double = 0.0,
    val offerAmount: Double = 0.0,
    val message: String = "",
    val status: String = "pending", // pending, accepted, rejected, countered
    val counterOffer: Double? = null,
    val counterMessage: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,

    // ✅ ADDED: Fields that the existing OffersAdapter expects
    val listingTitle: String = "",
    val listingImageUrl: String = "",
    val offeredPrice: Double = 0.0,
    val offerPrice: Double = 0.0
) {
    // ✅ Helper method to get timestamp as Long (for existing adapter compatibility)
    fun getCreatedAtLong(): Long {
        return createdAt?.toDate()?.time ?: System.currentTimeMillis()
    }

    // ✅ Helper method to get offer amount (handles both field names)
    fun calculateOfferAmount(): Double {
        return when {
            offeredPrice > 0 -> offeredPrice
            offerPrice > 0 -> offerPrice
            offerAmount > 0 -> offerAmount
            else -> 0.0
        }
    }
}