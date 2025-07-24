package com.example.tradeup.data.model

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val profilePictureUrl: String = "",
    val bio: String = "",
    val phoneNumber: String = "",
    val address: String = "",
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val emailVerified: Boolean = false,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)