package com.example.tradeup.data.remote

import com.example.tradeup.data.model.Listing
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class ListingRepository {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("listings")

    // FR-2.1.5: Create new listing
    suspend fun createListing(listing: Listing): Result<String> {
        return try {
            val docRef = collection.add(listing).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // FR-2.2.1: Get user's listings
    suspend fun getUserListings(userId: String): Result<List<Listing>> {
        return try {
            val snapshot = collection
                .whereEqualTo("sellerId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val listings = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Listing::class.java)?.copy(id = doc.id)
            }
            Result.success(listings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // FR-2.2.1: Update listing
    suspend fun updateListing(listingId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            collection.document(listingId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // FR-2.2.1: Delete listing
    suspend fun deleteListing(listingId: String): Result<Unit> {
        return try {
            collection.document(listingId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // FR-2.2.3: Increment views
    suspend fun incrementViews(listingId: String): Result<Unit> {
        return try {
            collection.document(listingId).update("views", com.google.firebase.firestore.FieldValue.increment(1)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get all listings for browsing
    suspend fun getAllListings(): Result<List<Listing>> {
        return try {
            val snapshot = collection
                .whereEqualTo("status", "Available")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val listings = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Listing::class.java)?.copy(id = doc.id)
            }
            Result.success(listings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}