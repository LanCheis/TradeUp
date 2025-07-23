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

        // Save individual rating
        db.collection("users").document(targetUserId)
            .collection("ratings")
            .document(raterId)
            .set(ratingData)
            .addOnSuccessListener {
                // Update user's aggregate rating
                updateUserRating(targetUserId)
                onComplete(true)
            }
            .addOnFailureListener { onComplete(false) }
    }

    // ✅ NEW: Update user's aggregate rating
    private fun updateUserRating(userId: String) {
        db.collection("users").document(userId)
            .collection("ratings")
            .get()
            .addOnSuccessListener { snapshot ->
                val ratings = snapshot.documents.mapNotNull {
                    it.getDouble("stars")?.toFloat()
                }

                if (ratings.isNotEmpty()) {
                    val averageRating = ratings.average()
                    val ratingCount = ratings.size

                    // Update user document with new rating
                    db.collection("users").document(userId)
                        .update(
                            mapOf(
                                "rating" to averageRating,
                                "ratingCount" to ratingCount
                            )
                        )
                }
            }
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
            .addOnFailureListener { onResult(0f) }
    }

    // 🆕 NEW: Account deletion functionality
    fun deleteUserAccount(onComplete: (Boolean, String?) -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            onComplete(false, "No user logged in")
            return
        }

        val uid = currentUser.uid

        // Step 1: Delete user data from Firestore
        usersCollection.document(uid).delete()
            .addOnSuccessListener {
                // Step 2: Delete user's listings
                deleteUserListings(uid) { listingsDeleted ->
                    if (listingsDeleted) {
                        // Step 3: Delete user's ratings
                        deleteUserRatings(uid) { ratingsDeleted ->
                            if (ratingsDeleted) {
                                // Step 4: Delete Firebase Auth account
                                currentUser.delete()
                                    .addOnSuccessListener {
                                        onComplete(true, null)
                                    }
                                    .addOnFailureListener { exception ->
                                        onComplete(false, "Failed to delete account: ${exception.message}")
                                    }
                            } else {
                                onComplete(false, "Failed to delete user ratings")
                            }
                        }
                    } else {
                        onComplete(false, "Failed to delete user listings")
                    }
                }
            }
            .addOnFailureListener { exception ->
                onComplete(false, "Failed to delete user data: ${exception.message}")
            }
    }

    // 🆕 NEW: Helper method to delete user's listings
    private fun deleteUserListings(userId: String, onComplete: (Boolean) -> Unit) {
        FirebaseFirestore.getInstance()
            .collection("listings")
            .whereEqualTo("ownerUid", userId)
            .get()
            .addOnSuccessListener { documents ->
                val batch = FirebaseFirestore.getInstance().batch()
                for (document in documents) {
                    batch.delete(document.reference)
                }

                batch.commit()
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { onComplete(false) }
            }
            .addOnFailureListener { onComplete(false) }
    }

    // 🆕 NEW: Helper method to delete user's ratings
    private fun deleteUserRatings(userId: String, onComplete: (Boolean) -> Unit) {
        // Delete ratings given by this user
        db.collection("user_reviews")
            .whereEqualTo("fromUserId", userId)
            .get()
            .addOnSuccessListener { fromDocuments ->
                val batch = db.batch()
                for (document in fromDocuments) {
                    batch.delete(document.reference)
                }

                // Delete ratings given to this user
                db.collection("user_reviews")
                    .whereEqualTo("toUserId", userId)
                    .get()
                    .addOnSuccessListener { toDocuments ->
                        for (document in toDocuments) {
                            batch.delete(document.reference)
                        }

                        batch.commit()
                            .addOnSuccessListener { onComplete(true) }
                            .addOnFailureListener { onComplete(false) }
                    }
                    .addOnFailureListener { onComplete(false) }
            }
            .addOnFailureListener { onComplete(false) }
    }

    fun deleteUserProfile(uid: String, onComplete: (Boolean, String?) -> Unit) {
        usersCollection.document(uid).delete()
            .addOnSuccessListener { onComplete(true, null) }
            .addOnFailureListener { onComplete(false, it.message) }
    }
}