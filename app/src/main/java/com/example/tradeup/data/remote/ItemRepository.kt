package com.example.tradeup.data.remote

import android.content.Context
import android.net.Uri
import com.example.tradeup.data.model.Item
import com.example.tradeup.utils.CloudinaryHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date

class ItemRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // FR-2.1.1: Save new item listing
    suspend fun createItem(item: Item): Result<String> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))

            // Generate new document ID
            val itemRef = firestore.collection("items").document()
            val itemId = itemRef.id

            // Add system fields
            val itemWithMetadata = item.copy(
                id = itemId,
                sellerId = currentUser.uid,
                createdAt = Date(),
                updatedAt = Date()
            )

            // Save to Firestore
            itemRef.set(itemWithMetadata).await()

            Result.success(itemId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // FR-2.1.1: Upload multiple item photos using Cloudinary
    suspend fun uploadItemPhotos(context: Context, photoUris: List<Uri>, itemId: String): List<String> {
        val uploadedUrls = mutableListOf<String>()

        photoUris.forEachIndexed { index, uri ->
            try {
                val url = CloudinaryHelper.uploadItemImage(context, uri, itemId, index)
                url?.let { uploadedUrls.add(it) }
            } catch (e: Exception) {
                // Continue with other photos even if one fails
                e.printStackTrace()
            }
        }

        return uploadedUrls
    }

    // Get user's own listings
    suspend fun getUserItems(userId: String): List<Item> {
        return try {
            val snapshot = firestore.collection("items")
                .whereEqualTo("sellerId", userId)
                .whereEqualTo("isActive", true)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.toObjects(Item::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }
}