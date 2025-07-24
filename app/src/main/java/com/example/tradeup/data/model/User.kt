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
    val rating: Double = 0.0, // FR-7.2.1: Average rating (1-5 stars)
    val totalTransactions: Int = 0, // FR-7.2.1: Total completed transactions
    val ratingCount: Int = 0, // Number of ratings received
    val joinedDate: Long = System.currentTimeMillis(), // When user joined
    val isActive: Boolean = true, // FR-1.2.3: Account status for deactivation
    val lastLoginTime: Long = System.currentTimeMillis(), // Track last login

    // Additional properties for compatibility
    val displayName: String = name, // Computed property for compatibility
    val isProfileComplete: Boolean = name.isNotEmpty() && email.isNotEmpty()
)

// FR-7.1.1 & FR-7.1.2: Rating data model
data class UserRating(
    val id: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val stars: Float = 0f, // 1-5 stars
    val comment: String = "", // Optional written feedback
    val transactionId: String = "", // Related to which transaction
    val timestamp: Long = System.currentTimeMillis()
)