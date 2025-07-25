package com.example.tradeup.data.remote

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.tradeup.data.model.User
import com.example.tradeup.utils.CloudinaryHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val cloudinaryHelper = CloudinaryHelper()

    companion object {
        private const val TAG = "UserRepository"
        private const val USERS_COLLECTION = "users"
    }

    /**
     * FR-1.2.1: Save user profile to Firestore
     */
    suspend fun saveUserProfile(user: User): Boolean {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(user.uid)
                .set(user)
                .await()

            Log.d(TAG, "✅ User profile saved: ${user.uid}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving user profile: ${e.message}")
            false
        }
    }

    /**
     * FR-1.2.1: Get user profile from Firestore
     */
    suspend fun getUserProfile(uid: String): User? {
        return try {
            val document = firestore.collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .await()

            val user = document.toObject(User::class.java)
            Log.d(TAG, "✅ User profile retrieved: $uid")
            user
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting user profile: ${e.message}")
            null
        }
    }

    /**
     * FR-1.2.1: Update user profile with provided data
     */
    suspend fun updateUserProfile(userId: String, updates: Map<String, Any>): Boolean {
        return try {
            val updatesWithTimestamp = updates.toMutableMap()
            updatesWithTimestamp["updatedAt"] = Timestamp.now()

            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(updatesWithTimestamp)
                .await()

            Log.d(TAG, "✅ User profile updated: $userId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating user profile: ${e.message}")
            false
        }
    }

    /**
     * FR-1.1.1: Create initial user profile after registration
     */
    suspend fun createInitialProfile(uid: String, email: String): Boolean {
        return try {
            val user = User(
                uid = uid,
                email = email,
                displayName = email.substringBefore("@"), // Default display name from email
                emailVerified = auth.currentUser?.isEmailVerified ?: false,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )

            val success = saveUserProfile(user)

            if (success) {
                Log.d(TAG, "✅ Initial profile created for: $email")
            } else {
                Log.e(TAG, "❌ Failed to create initial profile for: $email")
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating initial profile: ${e.message}")
            false
        }
    }

    /**
     * FR-1.2.1: Upload and update profile picture
     */
    suspend fun uploadProfileImage(imageUri: Uri, context: Context): String? {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.e(TAG, "❌ No authenticated user for profile image upload")
                return null
            }

            // Upload image to Cloudinary
            val uploadedUrl = cloudinaryHelper.uploadImage(imageUri, context)

            if (uploadedUrl != null) {
                // Update user profile with new image URL
                val updates = mapOf(
                    "profilePictureUrl" to uploadedUrl,
                    "updatedAt" to Timestamp.now()
                )

                val updateSuccess = updateUserProfile(currentUser.uid, updates)

                if (updateSuccess) {
                    Log.d(TAG, "✅ Profile image uploaded and updated: $uploadedUrl")
                    uploadedUrl
                } else {
                    Log.e(TAG, "❌ Failed to update profile with new image URL")
                    null
                }
            } else {
                Log.e(TAG, "❌ Failed to upload profile image to Cloudinary")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error uploading profile image: ${e.message}")
            null
        }
    }

    /**
     * FR-7.1.1: Update user rating after transaction
     */
    suspend fun updateUserRating(userId: String, newRating: Double, reviewText: String = ""): Boolean {
        return try {
            // Get current user data
            val currentUser = getUserProfile(userId)
            if (currentUser == null) {
                Log.e(TAG, "❌ User not found for rating update: $userId")
                return false
            }

            // Calculate new average rating
            val currentRating = currentUser.rating
            val currentReviewCount = currentUser.reviewCount
            val newReviewCount = currentReviewCount + 1
            val newAverageRating = ((currentRating * currentReviewCount) + newRating) / newReviewCount

            // Update user rating data
            val updates = mapOf(
                "rating" to newAverageRating,
                "reviewCount" to newReviewCount,
                "updatedAt" to Timestamp.now()
            )

            val success = updateUserProfile(userId, updates)

            if (success) {
                Log.d(TAG, "✅ User rating updated: $userId - New rating: $newAverageRating")
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating user rating: ${e.message}")
            false
        }
    }

    /**
     * FR-1.1.2: Update email verification status
     */
    suspend fun updateEmailVerificationStatus(userId: String, isVerified: Boolean): Boolean {
        return try {
            val updates = mapOf(
                "emailVerified" to isVerified,
                "updatedAt" to Timestamp.now()
            )

            val success = updateUserProfile(userId, updates)

            if (success) {
                Log.d(TAG, "✅ Email verification status updated: $userId - Verified: $isVerified")
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating email verification status: ${e.message}")
            false
        }
    }

    /**
     * Search users by display name (for chat/messaging features)
     */
    suspend fun searchUsersByName(query: String, limit: Int = 20): List<User> {
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .whereGreaterThanOrEqualTo("displayName", query)
                .whereLessThanOrEqualTo("displayName", query + "\uf8ff")
                .limit(limit.toLong())
                .get()
                .await()

            val users = snapshot.toObjects(User::class.java)
            Log.d(TAG, "✅ Found ${users.size} users matching: $query")
            users
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error searching users: ${e.message}")
            emptyList()
        }
    }

    /**
     * Get multiple user profiles by IDs (for displaying seller info in listings)
     */
    suspend fun getUserProfiles(userIds: List<String>): List<User> {
        return try {
            val users = mutableListOf<User>()

            // Firestore 'in' queries are limited to 10 items, so batch them
            userIds.chunked(10).forEach { batch ->
                val snapshot = firestore.collection(USERS_COLLECTION)
                    .whereIn("uid", batch)
                    .get()
                    .await()

                users.addAll(snapshot.toObjects(User::class.java))
            }

            Log.d(TAG, "✅ Retrieved ${users.size} user profiles")
            users
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting user profiles: ${e.message}")
            emptyList()
        }
    }

    /**
     * Check if user profile is complete
     */
    suspend fun isProfileComplete(userId: String): Boolean {
        return try {
            val user = getUserProfile(userId)
            user?.let {
                user.displayName.isNotEmpty() &&
                        user.phoneNumber.isNotEmpty() &&
                        user.address.isNotEmpty()
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking profile completeness: ${e.message}")
            false
        }
    }

    /**
     * Delete user profile (for account deletion)
     */
    suspend fun deleteUserProfile(userId: String): Boolean {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .delete()
                .await()

            Log.d(TAG, "✅ User profile deleted: $userId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting user profile: ${e.message}")
            false
        }
    }
}