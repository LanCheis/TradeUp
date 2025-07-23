package com.example.tradeup.services

import android.util.Log
import com.example.tradeup.utils.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }

        // Check if message contains notification payload
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Message Notification Body: ${notification.body}")

            val type = remoteMessage.data["type"] ?: "general"

            NotificationHelper.showNotification(
                context = this,
                title = notification.title ?: "TradeUp",
                message = notification.body ?: "New notification",
                type = type,
                data = remoteMessage.data
            )
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")

        // Save token locally
        com.example.tradeup.utils.FCMTokenManager.saveToken(this, token)

        // Send token to server
        sendRegistrationToServer(token)
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"] ?: return

        when (type) {
            "new_message" -> handleNewMessageNotification(data)
            "new_offer" -> handleNewOfferNotification(data)
            "offer_accepted" -> handleOfferAcceptedNotification(data)
            "offer_rejected" -> handleOfferRejectedNotification(data)
            "item_sold" -> handleItemSoldNotification(data)
            "price_drop" -> handlePriceDropNotification(data)
            "listing_update" -> handleListingUpdateNotification(data)
            else -> handleGenericNotification(data)
        }
    }

    private fun handleNewMessageNotification(data: Map<String, String>) {
        val senderName = data["senderName"] ?: "Someone"
        val message = data["message"] ?: "sent you a message"
        val listingTitle = data["listingTitle"]

        val title = "💬 New Message"
        val body = if (listingTitle != null) {
            "$senderName: $message\nAbout: $listingTitle"
        } else {
            "$senderName: $message"
        }

        NotificationHelper.showNotification(
            context = this,
            title = title,
            message = body,
            type = "new_message",
            data = data
        )
    }

    private fun handleNewOfferNotification(data: Map<String, String>) {
        val buyerName = data["buyerName"] ?: "Someone"
        val offerAmount = data["offerAmount"] ?: "an amount"
        val listingTitle = data["listingTitle"] ?: "your item"

        val title = "💰 New Offer"
        val message = "$buyerName offered $offerAmount for $listingTitle"

        NotificationHelper.showNotification(
            context = this,
            title = title,
            message = message,
            type = "new_offer",
            data = data
        )
    }

    private fun handleOfferAcceptedNotification(data: Map<String, String>) {
        val sellerName = data["sellerName"] ?: "The seller"
        val listingTitle = data["listingTitle"] ?: "the item"

        val title = "🎉 Offer Accepted!"
        val message = "$sellerName accepted your offer for $listingTitle"

        NotificationHelper.showNotification(
            context = this,
            title = title,
            message = message,
            type = "offer_accepted",
            data = data
        )
    }

    private fun handleOfferRejectedNotification(data: Map<String, String>) {
        val sellerName = data["sellerName"] ?: "The seller"
        val listingTitle = data["listingTitle"] ?: "the item"

        val title = "❌ Offer Declined"
        val message = "$sellerName declined your offer for $listingTitle"

        NotificationHelper.showNotification(
            context = this,
            title = title,
            message = message,
            type = "offer_rejected",
            data = data
        )
    }

    private fun handleItemSoldNotification(data: Map<String, String>) {
        val listingTitle = data["listingTitle"] ?: "Your item"
        val buyerName = data["buyerName"] ?: "Someone"

        val title = "✅ Item Sold!"
        val message = "$listingTitle was sold to $buyerName"

        NotificationHelper.showNotification(
            context = this,
            title = title,
            message = message,
            type = "item_sold",
            data = data
        )
    }

    private fun handlePriceDropNotification(data: Map<String, String>) {
        val listingTitle = data["listingTitle"] ?: "An item"
        val newPrice = data["newPrice"] ?: "a lower price"

        val title = "📉 Price Drop Alert"
        val message = "$listingTitle is now available for $newPrice"

        NotificationHelper.showNotification(
            context = this,
            title = title,
            message = message,
            type = "price_drop",
            data = data
        )
    }

    private fun handleListingUpdateNotification(data: Map<String, String>) {
        val updateType = data["updateType"] ?: "updated"
        val listingTitle = data["listingTitle"] ?: "An item"
        val customMessage = data["message"]

        val title = when (updateType) {
            "back_in_stock" -> "🔄 Back in Stock"
            "price_change" -> "💰 Price Updated"
            "description_update" -> "📝 Item Updated"
            else -> "🔔 Update"
        }

        val message = customMessage ?: when (updateType) {
            "back_in_stock" -> "$listingTitle is back in stock"
            "price_change" -> "$listingTitle price has been updated"
            "description_update" -> "$listingTitle details have been updated"
            else -> "$listingTitle has been updated"
        }

        NotificationHelper.showNotification(
            context = this,
            title = title,
            message = message,
            type = "listing_update",
            data = data
        )
    }

    private fun handleGenericNotification(data: Map<String, String>) {
        val title = data["title"] ?: "TradeUp"
        val message = data["message"] ?: "You have a new notification"

        NotificationHelper.showNotification(
            context = this,
            title = title,
            message = message,
            type = "general",
            data = data
        )
    }

    private fun sendRegistrationToServer(token: String) {
        // Send token to Firestore for now
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val tokenData = hashMapOf(
                "fcmToken" to token,
                "tokenUpdatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "deviceType" to "android",
                "appVersion" to getAppVersion()
            )

            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.uid)
                .update(tokenData as Map<String, Any>)
                .addOnSuccessListener {
                    Log.d(TAG, "FCM token successfully sent to server")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to send FCM token to server", e)
                }
        }
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }
}