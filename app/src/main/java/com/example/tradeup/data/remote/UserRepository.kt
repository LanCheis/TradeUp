// File: app/src/main/java/com/example/tradeup/data/remote/UserRepository.kt

package com.example.tradeup.data.remote

import com.example.tradeup.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

class UserRepository {

    companion object {
        private val firestore = FirebaseFirestore.getInstance()
        private val auth = FirebaseAuth.getInstance()

        // ✅ OVERLOADED METHOD 1: For backward compatibility (Boolean only)
        fun checkUserProfileExists(userId: String, callback: (Boolean) -> Unit) {
            getUserProfile(userId) { success, userData ->
                callback(success)
            }
        }

        // ✅ OVERLOADED METHOD 2: For new usage (Boolean + Map)
        fun getUserProfile(userId: String, callback: (Boolean, Map<String, Any>?) -> Unit) {
            firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        callback(true, document.data)
                    } else {
                        callback(false, null)
                    }
                }
                .addOnFailureListener {
                    callback(false, null)
                }
        }

        // ✅ OVERLOADED METHOD 3: For User object compatibility
        fun getUserProfile(userId: String, callback: (User?) -> Unit) {
            getUserProfile(userId) { success, userData ->
                if (success && userData != null) {
                    val user = mapToUser(userData, userId)
                    callback(user)
                } else {
                    callback(null)
                }
            }
        }

        // ✅ HELPER: Convert Map to User object
        private fun mapToUser(data: Map<String, Any>, userId: String): User {
            return User(
                uid = userId,
                name = data["name"] as? String ?: "",
                email = data["email"] as? String ?: "",
                phone = data["phone"] as? String ?: "",
                address = data["address"] as? String ?: "",
                bio = data["bio"] as? String ?: "",
                username = data["username"] as? String ?: "",
                gender = data["gender"] as? String ?: "",
                birthday = data["birthday"] as? String ?: "",
                interests = data["interests"] as? String ?: "",
                profileImageUrl = data["profileImageUrl"] as? String ?: "",
                rating = (data["rating"] as? Number)?.toDouble() ?: 0.0,
                totalTransactions = (data["totalTransactions"] as? Number)?.toInt() ?: 0,
                ratingCount = (data["ratingCount"] as? Number)?.toInt() ?: 0,
                joinedDate = (data["joinedDate"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                isActive = data["isActive"] as? Boolean ?: true
            )
        }

        // ✅ RATING SUBMISSION - Two overloaded versions
        fun submitRating(userId: String, stars: Float, callback: (Boolean) -> Unit) {
            submitRating(userId, stars, "", callback)
        }

        fun submitRating(userId: String, stars: Float, comment: String, callback: (Boolean) -> Unit) {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                callback(false)
                return
            }

            val ratingData = hashMapOf(
                "fromUserId" to currentUser.uid,
                "toUserId" to userId,
                "stars" to stars.toDouble(),
                "comment" to comment,
                "timestamp" to FieldValue.serverTimestamp()
            )

            firestore.collection("ratings")
                .add(ratingData)
                .addOnSuccessListener {
                    updateUserRating(userId) { success ->
                        callback(success)
                    }
                }
                .addOnFailureListener {
                    callback(false)
                }
        }

        // ✅ USER PROFILE SAVING - Multiple overloaded versions
        fun saveUserProfile(user: User, callback: (Boolean, String?) -> Unit) {
            val userData = hashMapOf(
                "uid" to user.uid,
                "name" to user.name,
                "email" to user.email,
                "phone" to user.phone,
                "address" to user.address,
                "bio" to user.bio,
                "username" to user.username,
                "gender" to user.gender,
                "birthday" to user.birthday,
                "interests" to user.interests,
                "profileImageUrl" to user.profileImageUrl,
                "rating" to user.rating,
                "totalTransactions" to user.totalTransactions,
                "ratingCount" to user.ratingCount,
                "joinedDate" to user.joinedDate,
                "isActive" to user.isActive
            )

            firestore.collection("users")
                .document(user.uid)
                .set(userData)
                .addOnSuccessListener {
                    callback(true, null)
                }
                .addOnFailureListener { exception ->
                    callback(false, exception.message)
                }
        }

        // ✅ Alternative saveUserProfile for backward compatibility
        fun saveUserProfile(user: User, callback: (Boolean) -> Unit) {
            saveUserProfile(user) { success, _ ->
                callback(success)
            }
        }

        // ✅ UPDATE USER PROFILE
        fun updateUserProfile(userId: String, userData: Map<String, Any>, callback: (Boolean) -> Unit) {
            firestore.collection("users")
                .document(userId)
                .update(userData)
                .addOnSuccessListener {
                    callback(true)
                }
                .addOnFailureListener {
                    callback(false)
                }
        }

        // ✅ CREATE USER PROFILE
        fun createUserProfile(user: User, callback: (Boolean) -> Unit) {
            saveUserProfile(user, callback)
        }

        // ✅ DELETE USER DATA - Updated method name and signature
        fun deleteUserData(userId: String, callback: (Boolean) -> Unit) {
            firestore.collection("users")
                .document(userId)
                .delete()
                .addOnSuccessListener {
                    deleteUserRatings(userId) { ratingsDeleted ->
                        callback(ratingsDeleted)
                    }
                }
                .addOnFailureListener {
                    callback(false)
                }
        }

        // ✅ Alternative method name for compatibility
        fun deleteUserAccount(userId: String, callback: (Boolean) -> Unit) {
            deleteUserData(userId, callback)
        }

        // ✅ Additional methods for ProfileFragment compatibility
        fun getUserData(userId: String, callback: (Boolean, Map<String, Any>?) -> Unit) {
            getUserProfile(userId, callback)
        }

        fun getUserById(userId: String, callback: (User?) -> Unit) {
            getUserProfile(userId, callback)
        }

        // ✅ PRIVATE HELPER METHODS
        private fun updateUserRating(userId: String, callback: (Boolean) -> Unit) {
            firestore.collection("ratings")
                .whereEqualTo("toUserId", userId)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        callback(true)
                        return@addOnSuccessListener
                    }

                    var totalStars = 0.0
                    var count = 0

                    for (document in documents) {
                        val stars = document.getDouble("stars") ?: 0.0
                        totalStars += stars
                        count++
                    }

                    val averageRating = if (count > 0) totalStars / count else 0.0

                    firestore.collection("users")
                        .document(userId)
                        .update(
                            "rating", averageRating,
                            "ratingCount", count
                        )
                        .addOnSuccessListener {
                            callback(true)
                        }
                        .addOnFailureListener {
                            callback(false)
                        }
                }
                .addOnFailureListener {
                    callback(false)
                }
        }

        private fun deleteUserRatings(userId: String, callback: (Boolean) -> Unit) {
            firestore.collection("ratings")
                .whereEqualTo("fromUserId", userId)
                .get()
                .addOnSuccessListener { documents ->
                    val batch = firestore.batch()
                    for (document in documents) {
                        batch.delete(document.reference)
                    }

                    batch.commit()
                        .addOnSuccessListener {
                            deleteRatingsForUser(userId, callback)
                        }
                        .addOnFailureListener {
                            callback(false)
                        }
                }
                .addOnFailureListener {
                    callback(false)
                }
        }

        private fun deleteRatingsForUser(userId: String, callback: (Boolean) -> Unit) {
            firestore.collection("ratings")
                .whereEqualTo("toUserId", userId)
                .get()
                .addOnSuccessListener { documents ->
                    val batch = firestore.batch()
                    for (document in documents) {
                        batch.delete(document.reference)
                    }

                    batch.commit()
                        .addOnSuccessListener {
                            callback(true)
                        }
                        .addOnFailureListener {
                            callback(false)
                        }
                }
                .addOnFailureListener {
                    callback(false)
                }
        }
    }
}