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
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val listingTitle: String = "",
    val listingImageUrl: String = "",
    val offeredPrice: Double = 0.0,
    val timestamp: Timestamp = Timestamp.now() // For Firestore compatibility
)