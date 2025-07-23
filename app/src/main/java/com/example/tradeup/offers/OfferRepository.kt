package com.example.tradeup.offers

import com.example.tradeup.data.model.Offer
import com.example.tradeup.utils.NotificationHelper
import com.example.tradeup.utils.OfferNotificationData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.*

object OfferRepository {
    private val db = FirebaseFirestore.getInstance()
    private val offersCollection = db.collection("offers")

    fun createOffer(offer: Offer, onComplete: (Boolean, String?) -> Unit) {
        val offerWithId = offer.copy(
            id = UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        offersCollection.document(offerWithId.id)
            .set(offerWithId)
            .addOnSuccessListener {
                // Send notification to seller
                sendOfferNotification(offerWithId)
                onComplete(true, null)
            }
            .addOnFailureListener { exception ->
                onComplete(false, exception.message)
            }
    }

    fun getOffersByBuyer(buyerId: String, onResult: (List<Offer>) -> Unit) {
        offersCollection
            .whereEqualTo("buyerId", buyerId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val offers = documents.map { it.toObject(Offer::class.java) }
                onResult(offers)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    fun getOffersBySeller(sellerId: String, onResult: (List<Offer>) -> Unit) {
        offersCollection
            .whereEqualTo("sellerId", sellerId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val offers = documents.map { it.toObject(Offer::class.java) }
                onResult(offers)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    fun getOffersForListing(listingId: String, onResult: (List<Offer>) -> Unit) {
        offersCollection
            .whereEqualTo("listingId", listingId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val offers = documents.map { it.toObject(Offer::class.java) }
                onResult(offers)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    fun acceptOffer(offerId: String, onComplete: (Boolean, String?) -> Unit) {
        updateOfferStatus(offerId, "accepted", onComplete)
    }

    fun rejectOffer(offerId: String, onComplete: (Boolean, String?) -> Unit) {
        updateOfferStatus(offerId, "rejected", onComplete)
    }

    fun counterOffer(
        offerId: String,
        counterPrice: Double,
        counterMessage: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val updates = mapOf(
            "status" to "counter",
            "counterOffer" to counterPrice,
            "counterMessage" to counterMessage,
            "updatedAt" to System.currentTimeMillis()
        )

        offersCollection.document(offerId)
            .update(updates)
            .addOnSuccessListener {
                onComplete(true, null)
            }
            .addOnFailureListener { exception ->
                onComplete(false, exception.message)
            }
    }

    private fun updateOfferStatus(offerId: String, status: String, onComplete: (Boolean, String?) -> Unit) {
        val updates = mapOf(
            "status" to status,
            "updatedAt" to System.currentTimeMillis()
        )

        offersCollection.document(offerId)
            .update(updates)
            .addOnSuccessListener {
                onComplete(true, null)
            }
            .addOnFailureListener { exception ->
                onComplete(false, exception.message)
            }
    }

    private fun sendOfferNotification(offer: Offer) {
        // This would typically be handled by your backend
        // For now, we'll create a local notification data structure
        val notificationData = OfferNotificationData(
            listingId = offer.listingId,
            listingTitle = offer.listingTitle,
            buyerId = offer.buyerId,
            buyerName = offer.buyerName,
            offerAmount = "${offer.offerPrice} ₫",
            message = offer.message
        )

        // In a real app, you'd send this to your backend to trigger FCM
        // For now, we'll just log it
        android.util.Log.d("OfferRepository", "Would send notification: $notificationData")
    }
}

