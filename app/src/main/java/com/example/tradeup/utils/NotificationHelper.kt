// app/src/main/java/com/example/tradeup/utils/NotificationHelper.kt
package com.example.tradeup.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.tradeup.MainActivity
import com.example.tradeup.R
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.auth.FirebaseAuth

object NotificationHelper {

    private const val TAG = "NotificationHelper"
    private const val CHANNEL_ID_MESSAGES = "tradeup_messages"
    private const val CHANNEL_ID_OFFERS = "tradeup_offers"
    private const val CHANNEL_ID_GENERAL = "tradeup_general"

    // Notification topics for subscribing to different types of notifications
    object Topics {
        const val NEW_LISTINGS = "new_listings"
        const val PRICE_DROPS = "price_drops"
        const val SIMILAR_ITEMS = "similar_items"
        const val GENERAL_UPDATES = "general_updates"
    }

    /**
     * Initialize FCM for the current user
     */
    fun initializeFCM(context: Context) {
        createNotificationChannels(context)

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            Log.d(TAG, "FCM Registration Token: $token")

            // Save token and send to server if user is logged in
            FCMTokenManager.saveToken(context, token)
            sendTokenToServerIfLoggedIn(token)
        }
    }

    /**
     * Create notification channels for different types of notifications
     */
    private fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Messages Channel
            val messagesChannel = NotificationChannel(
                CHANNEL_ID_MESSAGES,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new messages"
                enableVibration(true)
                enableLights(true)
            }

            // Offers Channel
            val offersChannel = NotificationChannel(
                CHANNEL_ID_OFFERS,
                "Offers & Transactions",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for new offers and transactions"
                enableVibration(true)
            }

            // General Channel
            val generalChannel = NotificationChannel(
                CHANNEL_ID_GENERAL,
                "General Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General app notifications"
            }

            notificationManager.createNotificationChannels(listOf(
                messagesChannel, offersChannel, generalChannel
            ))
        }
    }

    /**
     * Show notification based on type
     */
    fun showNotification(
        context: Context,
        title: String,
        message: String,
        type: String = "general",
        data: Map<String, String> = emptyMap()
    ) {
        val channelId = when (type) {
            "new_message" -> CHANNEL_ID_MESSAGES
            "new_offer", "offer_accepted", "offer_rejected" -> CHANNEL_ID_OFFERS
            else -> CHANNEL_ID_GENERAL
        }

        val intent = createNotificationIntent(context, type, data)
        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val icon = when (type) {
            "new_message" -> R.drawable.ic_chat
            "new_offer" -> R.drawable.ic_sale_badge
            else -> R.drawable.ic_notification
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(if (type == "new_message") NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(context.getColor(R.color.primary))

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    /**
     * Create appropriate intent based on notification type
     */
    private fun createNotificationIntent(context: Context, type: String, data: Map<String, String>): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            when (type) {
                "new_message" -> {
                    putExtra("navigate_to", "chat")
                    putExtra("chat_id", data["chatId"])
                }
                "new_offer" -> {
                    putExtra("navigate_to", "offers")
                    putExtra("listing_id", data["listingId"])
                }
                "listing_update" -> {
                    putExtra("navigate_to", "listing_detail")
                    putExtra("listing_id", data["listingId"])
                }
            }

            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
    }

    /**
     * Subscribe to notification topics based on user preferences
     */
    fun subscribeToTopics(enableNewListings: Boolean = true, enablePriceDrops: Boolean = true) {
        val messaging = FirebaseMessaging.getInstance()

        if (enableNewListings) {
            messaging.subscribeToTopic(Topics.NEW_LISTINGS)
                .addOnCompleteListener { task ->
                    val msg = if (task.isSuccessful) "Subscribed to new listings" else "Failed to subscribe"
                    Log.d(TAG, msg)
                }
        }

        if (enablePriceDrops) {
            messaging.subscribeToTopic(Topics.PRICE_DROPS)
                .addOnCompleteListener { task ->
                    val msg = if (task.isSuccessful) "Subscribed to price drops" else "Failed to subscribe"
                    Log.d(TAG, msg)
                }
        }

        // Always subscribe to general updates
        messaging.subscribeToTopic(Topics.GENERAL_UPDATES)
    }

    /**
     * Unsubscribe from all topics (useful when user logs out)
     */
    fun unsubscribeFromAllTopics() {
        val messaging = FirebaseMessaging.getInstance()

        listOf(
            Topics.NEW_LISTINGS,
            Topics.PRICE_DROPS,
            Topics.SIMILAR_ITEMS,
            Topics.GENERAL_UPDATES
        ).forEach { topic ->
            messaging.unsubscribeFromTopic(topic)
                .addOnCompleteListener { task ->
                    val msg = if (task.isSuccessful) "Unsubscribed from $topic" else "Failed to unsubscribe from $topic"
                    Log.d(TAG, msg)
                }
        }
    }

    /**
     * Check if notifications are enabled for the app
     */
    fun areNotificationsEnabled(context: Context): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.areNotificationsEnabled()
    }

    /**
     * Clear all notifications
     */
    fun clearAllNotifications(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
        Log.d(TAG, "All notifications cleared")
    }

    /**
     * Send FCM token to server (placeholder for future implementation)
     */
    private fun sendTokenToServerIfLoggedIn(token: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // Save token to Firestore for now
            val userData = hashMapOf(
                "fcmToken" to token,
                "lastTokenUpdate" to System.currentTimeMillis()
            )

            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.uid)
                .update(userData as Map<String, Any>)
                .addOnSuccessListener {
                    Log.d(TAG, "FCM token saved to Firestore")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to save FCM token", e)
                }
        }
    }

    /**
     * Handle notification permission request result
     */
    fun handleNotificationPermissionResult(granted: Boolean, context: Context) {
        if (granted) {
            Log.d(TAG, "Notification permission granted")
            initializeFCM(context)
            subscribeToTopics()
        } else {
            Log.d(TAG, "Notification permission denied")
            // Optionally show explanation to user about benefits of notifications
        }
    }
}

/**
 * FCM Token Manager for handling token storage
 */
object FCMTokenManager {
    private const val PREFS_NAME = "fcm_prefs"
    private const val KEY_FCM_TOKEN = "fcm_token"

    fun saveToken(context: Context, token: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
        Log.d("FCMTokenManager", "Token saved locally")
    }

    fun getToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_FCM_TOKEN, null)
    }

    fun clearToken(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_FCM_TOKEN).apply()
        Log.d("FCMTokenManager", "Token cleared")
    }
}

/**
 * Data classes for notification payloads
 */
data class NotificationData(
    val type: String,
    val title: String,
    val message: String,
    val data: Map<String, String> = emptyMap()
)

data class MessageNotificationData(
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val message: String,
    val listingTitle: String? = null
) {
    fun toMap(): Map<String, String> = mapOf(
        "type" to "new_message",
        "chatId" to chatId,
        "senderId" to senderId,
        "senderName" to senderName,
        "message" to message,
        "listingTitle" to (listingTitle ?: "")
    )
}

data class OfferNotificationData(
    val listingId: String,
    val listingTitle: String,
    val buyerId: String,
    val buyerName: String,
    val offerAmount: String,
    val message: String
) {
    fun toMap(): Map<String, String> = mapOf(
        "type" to "new_offer",
        "listingId" to listingId,
        "listingTitle" to listingTitle,
        "buyerId" to buyerId,
        "buyerName" to buyerName,
        "offerAmount" to offerAmount,
        "message" to message
    )
}

data class ListingNotificationData(
    val listingId: String,
    val listingTitle: String,
    val updateType: String, // "price_drop", "sold", "back_in_stock", etc.
    val message: String,
    val listingImage: String? = null
) {
    fun toMap(): Map<String, String> = mapOf(
        "type" to "listing_update",
        "listingId" to listingId,
        "listingTitle" to listingTitle,
        "updateType" to updateType,
        "message" to message,
        "listingImage" to (listingImage ?: "")
    )
}