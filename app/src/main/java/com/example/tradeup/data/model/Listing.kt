package com.example.tradeup.data.model

import com.google.firebase.Timestamp
import java.util.*

data class Listing(
    var id: String = "",
    val title: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val category: String = "",
    val condition: String = "",
    val location: String = "",
    val imageUrls: List<String> = emptyList(),
    val sellerId: String = "",
    val sellerName: String = "",
    val sellerImageUrl: String = "",
    val isNegotiable: Boolean = false,
    val isSold: Boolean = false,
    val isPaused: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val viewCount: Int = 0,
    val favoriteCount: Int = 0,
    val tags: List<String> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val contactInfo: ContactInfo? = null
)

data class ContactInfo(
    val email: String = "",
    val phone: String = "",
    val preferredContact: String = "chat" // "chat", "phone", "email"
)

// FR-5.1.1 & FR-5.1.2: Offers data model
data class Offer(
    val id: String = "",
    val listingId: String = "",
    val buyerId: String = "",
    val buyerName: String = "",
    val sellerId: String = "",
    val originalPrice: Double = 0.0,
    val offerPrice: Double = 0.0,
    val message: String = "",
    val status: String = "pending", // "pending", "accepted", "rejected", "countered"
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val expiresAt: Timestamp? = null
)

// FR-5.2.1 & FR-5.2.2: Transaction data model
data class Transaction(
    val id: String = "",
    val listingId: String = "",
    val buyerId: String = "",
    val sellerId: String = "",
    val finalPrice: Double = 0.0,
    val status: String = "pending", // "pending", "completed", "cancelled"
    val paymentStatus: String = "pending", // "pending", "paid", "failed"
    val deliveryMethod: String = "pickup", // "pickup", "delivery"
    val meetingLocation: String = "",
    val createdAt: Timestamp? = null,
    val completedAt: Timestamp? = null,
    val notes: String = ""
)