package com.example.tradeup.data.remote

import com.example.tradeup.data.model.User
import com.example.tradeup.data.model.UserRating
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

object UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    // FR-1.2.1 & FR-1.2.2: Save/Update user profile
    fun saveUserProfile(user: User, onComplete: (Boolean, String?) -> Unit) {
        usersCollection.document(user.uid).set(user)
            .addOnSuccessListener { onComplete(true, null) }
            .addOnFailureListener { onComplete(false, it.message) }
    }

    // FR-1.2.1 & FR-1.2.4: Get user profile (own or others)
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

    // FR-1.2.2: Update profile fields
    fun updateUserProfile(uid: String, updates: Map<String, Any>, onComplete: (Boolean, String?) -> Unit) {
        usersCollection.document(uid).update(updates)
            .addOnSuccessListener { onComplete(true, null) }
            .addOnFailureListener { onComplete(false, it.message) }
    }

    // FR-7.1.1: Submit rating after transaction
    fun submitRating(targetUserId: String, stars: Float, comment: String, transactionId: String = "", onComplete: (Boolean) -> Unit) {
        val raterId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val ratingData = UserRating(
            id = "${raterId}_${targetUserId}_${System.currentTimeMillis()}",
            fromUserId = raterId,
            toUserId = targetUserId,
            stars = stars,
            comment = comment,
            transactionId = transactionId,
            timestamp = System.currentTimeMillis()
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

    // FR-7.2.1: Update user's aggregate rating and transaction count
    private fun updateUserRating(userId: String) {
        db.collection("users").document(userId)
            .collection("ratings")
            .get()
            .addOnSuccessListener { snapshot ->
                val ratings = snapshot.documents.mapNotNull {
                    it.toObject(UserRating::class.java)?.stars
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

    // FR-7.2.1: Fetch user ratings for display
    fun getUserRatings(userId: String, onResult: (List<UserRating>) -> Unit) {
        db.collection("users").document(userId)
            .collection("ratings")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val ratings = snapshot.documents.mapNotNull {
                    it.toObject(UserRating::class.java)
                }
                onResult(ratings)
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    // FR-1.2.3: Account deactivation (soft delete)
    fun deactivateAccount(onComplete: (Boolean, String?) -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            onComplete(false, "No user logged in")
            return
        }

        usersCollection.document(currentUser.uid)
            .update("isActive", false)
            .addOnSuccessListener {
                FirebaseAuth.getInstance().signOut()
                onComplete(true, null)
            }
            .addOnFailureListener { onComplete(false, it.message) }
    }

    // FR-1.2.3: Permanent account deletion with confirmation
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

    // Helper method to delete user's listings
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

    // Helper method to delete user's ratings
    private fun deleteUserRatings(userId: String, onComplete: (Boolean) -> Unit) {
        db.collection("users").document(userId)
            .collection("ratings")
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                for (document in snapshot.documents) {
                    batch.delete(document.reference)
                }

                batch.commit()
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { onComplete(false) }
            }
            .addOnFailureListener { onComplete(false) }
    }

    // Search users by name or username
    fun searchUsers(query: String, onResult: (List<User>) -> Unit) {
        usersCollection
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { snapshot ->
                val users = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(User::class.java)?.let { user ->
                        if (user.name.contains(query, ignoreCase = true) ||
                            user.username.contains(query, ignoreCase = true)) {
                            user
                        } else null
                    }
                }
                onResult(users)
            }
            .addOnFailureListener { onResult(emptyList()) }
    }
}