package com.example.tradeup.data.remote

import android.content.Context
import android.net.Uri
import com.example.tradeup.data.model.User
import com.example.tradeup.utils.CloudinaryHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // Save user profile
    suspend fun saveUserProfile(user: User): Boolean {
        return try {
            firestore.collection("users")
                .document(user.uid)
                .set(user)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Get user profile
    suspend fun getUserProfile(uid: String): User? {
        return try {
            val document = firestore.collection("users")
                .document(uid)
                .get()
                .await()

            document.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // Upload profile image using Cloudinary (with Context parameter)
    suspend fun uploadProfileImage(context: Context, imageUri: Uri, userId: String): String? {
        return try {
            CloudinaryHelper.uploadProfileImage(context, imageUri, userId)
        } catch (e: Exception) {
            null
        }
    }

    // Create initial user profile after registration
    suspend fun createInitialProfile(uid: String, email: String): Boolean {
        val user = User(
            uid = uid,
            email = email,
            displayName = email.substringBefore("@"), // Default display name
            emailVerified = auth.currentUser?.isEmailVerified ?: false // ✅ FIXED: Changed to emailVerified
        )
        return saveUserProfile(user)
    }
}