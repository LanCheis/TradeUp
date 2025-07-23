package com.example.tradeup.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val bio: String = "",
    val username: String = "",
    val gender: String = "",
    val birthday: String = "",
    val interests: String = "",
    val profileImageUrl: String = "",
    val rating: Double = 0.0, // ✅ Average rating (1-5 stars)
    val totalTransactions: Int = 0, // ✅ Total completed transactions
    val ratingCount: Int = 0, // ✅ NEW: Number of ratings received
    val joinedDate: Long = System.currentTimeMillis(), // ✅ NEW: When user joined
    val isActive: Boolean = true // ✅ NEW: Account status
)

// ✅ NEW: Rating data model
data class UserRating(
    val id: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val stars: Float = 0f, // 1-5 stars
    val comment: String = "",
    val transactionId: String = "", // Related to which transaction
    val timestamp: Long = System.currentTimeMillis()
)