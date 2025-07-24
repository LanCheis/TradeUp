package com.example.tradeup.offers

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

object OfferRepository {

    private val firestore = FirebaseFirestore.getInstance()

    fun createOffer(offer: Map<String, Any>, callback: (Boolean, String?) -> Unit) {
        firestore.collection("offers")
            .add(offer)
            .addOnSuccessListener { documentReference ->
                callback(true, documentReference.id)
            }
            .addOnFailureListener { e ->
                callback(false, e.message)
            }
    }

    fun getOffersByBuyer(buyerId: String, callback: (List<Offer>) -> Unit) {
        firestore.collection("offers")
            .whereEqualTo("buyerId", buyerId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val offers = mutableListOf<Offer>()
                for (document in snapshot.documents) {
                    val offer = document.toObject(Offer::class.java)
                    offer?.let {
                        offers.add(it.copy(id = document.id))
                    }
                }
                callback(offers)
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }

    fun getOffersBySeller(sellerId: String, callback: (List<Offer>) -> Unit) {
        firestore.collection("offers")
            .whereEqualTo("sellerId", sellerId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val offers = mutableListOf<Offer>()
                for (document in snapshot.documents) {
                    val offer = document.toObject(Offer::class.java)
                    offer?.let {
                        offers.add(it.copy(id = document.id))
                    }
                }
                callback(offers)
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }

    fun updateOfferStatus(offerId: String, status: String, callback: (Boolean) -> Unit) {
        firestore.collection("offers")
            .document(offerId)
            .update(
                mapOf(
                    "status" to status,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }
}