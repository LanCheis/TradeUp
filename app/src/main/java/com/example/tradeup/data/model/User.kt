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
    val rating: Double = 0.0,
    val totalTransactions: Int = 0
)