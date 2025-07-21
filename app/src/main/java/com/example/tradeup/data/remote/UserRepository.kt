package com.example.tradeup.data.remote

import com.example.tradeup.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    fun saveUserProfile(user: User, onComplete: (Boolean, String?) -> Unit) {
        usersCollection.document(user.uid).set(user)
            .addOnSuccessListener { onComplete(true, null) }
            .addOnFailureListener { onComplete(false, it.message) }
    }

    fun getUserProfile(uid: String?, onResult: (User?) -> Unit) {
        if (uid.isNullOrEmpty()) {
            onResult(null)
            return
        }

        usersCollection.document(uid).get()
            .addOnSuccessListener { snapshot ->
                val user = snapshot.toObject(User::class.java)
                onResult(user)
            }
            .addOnFailureListener {
                onResult(null)
            }
    }


    fun submitRating(targetUserId: String, stars: Float, onComplete: (Boolean) -> Unit) {
        val raterId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ratingData = mapOf(
            "stars" to stars,
            "timestamp" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(targetUserId)
            .collection("ratings")
            .document(raterId)
            .set(ratingData)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun fetchAverageRating(userId: String, onResult: (Float) -> Unit) {
        db.collection("users").document(userId)
            .collection("ratings")
            .get()
            .addOnSuccessListener { snapshot ->
                val total = snapshot.documents.sumOf { it.getDouble("stars") ?: 0.0 }
                val avg = if (snapshot.size() > 0) total / snapshot.size() else 0.0
                onResult(avg.toFloat())
            }
        fun submitRating(targetUserId: String, stars: Float, onComplete: (Boolean) -> Unit) {
            val raterId = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val ratingData = mapOf(
                "stars" to stars,
                "timestamp" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(targetUserId)
                .collection("ratings")
                .document(raterId)
                .set(ratingData)
                .addOnSuccessListener { onComplete(true) }
                .addOnFailureListener { onComplete(false) }
        }

        fun fetchAverageRating(userId: String, onResult: (Float) -> Unit) {
            db.collection("users").document(userId)
                .collection("ratings")
                .get()
                .addOnSuccessListener { snapshot ->
                    val total = snapshot.documents.sumOf { it.getDouble("stars") ?: 0.0 }
                    val avg = if (snapshot.size() > 0) total / snapshot.size() else 0.0
                    onResult(avg.toFloat())
                }
            fun deleteUserProfile(uid: String, onComplete: (Boolean, String?) -> Unit) {
                usersCollection.document(uid).delete()
                    .addOnSuccessListener { onComplete(true, null) }
                    .addOnFailureListener { onComplete(false, it.message) }
            }
        }
    }
}
