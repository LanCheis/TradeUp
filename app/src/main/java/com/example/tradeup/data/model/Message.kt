package com.example.tradeup.data.model

import com.google.firebase.Timestamp

// FR-4.2.1: Notification data model (keeping notifications for other features)
data class NotificationData(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "", // "offer", "listing_update", "promotion", "system"
    val data: Map<String, String> = emptyMap(), // Additional data for navigation
    val isRead: Boolean = false,
    val timestamp: Timestamp? = null,
    val imageUrl: String = ""
)

// TODO: Chat and Message models will be added in future updates
// This placeholder reminds us to implement chat functionality later
object ChatFeature {
    const val IS_ENABLED = false
    const val COMING_SOON_MESSAGE = "Chat feature is under development and will be available soon! 🚀"
}