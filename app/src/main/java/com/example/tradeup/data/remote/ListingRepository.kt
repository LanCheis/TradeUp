package com.example.tradeup.data.remote

import android.util.Log
import com.example.tradeup.data.model.Listing
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * FR-2.1.1 to FR-2.2.3: Repository for managing listings in Firestore
 */
class ListingRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val listingsCollection = firestore.collection("listings")

    companion object {
        private const val TAG = "ListingRepository"
    }

    /**
     * FR-2.1.1: Create a new listing
     */
    suspend fun createListing(listing: Listing): Boolean {
        return try {
            // Generate search keywords for better findability
            val searchKeywords = generateSearchKeywords(listing.title, listing.description, listing.category)

            val listingWithKeywords = listing.copy(
                keywords = searchKeywords,
                createdAt = com.google.firebase.Timestamp.now(),
                updatedAt = com.google.firebase.Timestamp.now()
            )

            val docRef = listingsCollection.add(listingWithKeywords).await()

            // Update with the generated ID
            listingsCollection.document(docRef.id)
                .update("id", docRef.id)
                .await()

            Log.d(TAG, "✅ Listing created successfully with ID: ${docRef.id}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating listing: ${e.message}")
            false
        }
    }

    /**
     * FR-2.2.1: Get all listings for a specific user
     */
    suspend fun getUserListings(userId: String): List<Listing> {
        return try {
            val snapshot = listingsCollection
                .whereEqualTo("sellerId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val listings = snapshot.toObjects(Listing::class.java)
            Log.d(TAG, "✅ Retrieved ${listings.size} listings for user: $userId")
            listings
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting user listings: ${e.message}")
            emptyList()
        }
    }

    /**
     * FR-3.1.1: Get all active listings (for browse)
     */
    suspend fun getAllActiveListings(): List<Listing> {
        return try {
            val snapshot = listingsCollection
                .whereEqualTo("status", "available")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100) // Limit for performance
                .get()
                .await()

            val listings = snapshot.toObjects(Listing::class.java)
            Log.d(TAG, "✅ Retrieved ${listings.size} active listings")
            listings
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting active listings: ${e.message}")
            emptyList()
        }
    }

    /**
     * FR-3.1.1: Search listings with filters
     */
    suspend fun searchListings(
        query: String = "",
        category: String = "",
        minPrice: Double? = null,
        maxPrice: Double? = null,
        condition: String = "",
        sortOption: SortOption = SortOption.NEWEST
    ): List<Listing> {
        return try {
            var firestoreQuery: Query = listingsCollection
                .whereEqualTo("status", "available")

            // Apply category filter
            if (category.isNotEmpty()) {
                firestoreQuery = firestoreQuery.whereEqualTo("category", category)
            }

            // Apply condition filter
            if (condition.isNotEmpty()) {
                firestoreQuery = firestoreQuery.whereEqualTo("condition", condition)
            }

            // Apply price range filters
            minPrice?.let { min ->
                firestoreQuery = firestoreQuery.whereGreaterThanOrEqualTo("price", min)
            }
            maxPrice?.let { max ->
                firestoreQuery = firestoreQuery.whereLessThanOrEqualTo("price", max)
            }

            // Apply sorting
            firestoreQuery = when (sortOption) {
                SortOption.NEWEST -> firestoreQuery.orderBy("createdAt", Query.Direction.DESCENDING)
                SortOption.OLDEST -> firestoreQuery.orderBy("createdAt", Query.Direction.ASCENDING)
                SortOption.PRICE_LOW_TO_HIGH -> firestoreQuery.orderBy("price", Query.Direction.ASCENDING)
                SortOption.PRICE_HIGH_TO_LOW -> firestoreQuery.orderBy("price", Query.Direction.DESCENDING)
                SortOption.RELEVANCE -> firestoreQuery.orderBy("createdAt", Query.Direction.DESCENDING)
            }

            val snapshot = firestoreQuery
                .limit(50) // Limit results
                .get()
                .await()

            var listings = snapshot.toObjects(Listing::class.java)

            // Apply text search filter (client-side for better performance)
            if (query.isNotEmpty()) {
                listings = listings.filter { listing ->
                    listing.title.contains(query, ignoreCase = true) ||
                            listing.description.contains(query, ignoreCase = true) ||
                            listing.keywords.contains(query, ignoreCase = true)
                }
            }

            Log.d(TAG, "✅ Search returned ${listings.size} listings")
            listings
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error searching listings: ${e.message}")
            emptyList()
        }
    }

    /**
     * FR-2.2.1: Get a specific listing by ID
     */
    suspend fun getListingById(listingId: String): Listing? {
        return try {
            val snapshot = listingsCollection.document(listingId).get().await()
            val listing = snapshot.toObject(Listing::class.java)

            // FR-2.2.3: Increment view count
            if (listing != null) {
                incrementViewCount(listingId)
            }

            Log.d(TAG, "✅ Retrieved listing: $listingId")
            listing
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting listing: ${e.message}")
            null
        }
    }

    /**
     * FR-2.2.1: Update a listing
     */
    suspend fun updateListing(listingId: String, updates: Map<String, Any>): Boolean {
        return try {
            val updatesWithTimestamp = updates.toMutableMap()
            updatesWithTimestamp["updatedAt"] = System.currentTimeMillis()

            listingsCollection.document(listingId)
                .update(updatesWithTimestamp)
                .await()

            Log.d(TAG, "✅ Listing updated: $listingId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating listing: ${e.message}")
            false
        }
    }

    /**
     * FR-2.2.1: Delete a listing
     */
    suspend fun deleteListing(listingId: String): Boolean {
        return try {
            listingsCollection.document(listingId).delete().await()
            Log.d(TAG, "✅ Listing deleted: $listingId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting listing: ${e.message}")
            false
        }
    }

    /**
     * FR-2.2.2: Update listing status
     */
    suspend fun updateListingStatus(listingId: String, status: String): Boolean {
        return try {
            val updates = mutableMapOf<String, Any>(
                "status" to status,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )

            // If marking as sold, add sold timestamp
            if (status == "sold") {
                updates["soldAt"] = com.google.firebase.Timestamp.now()
            }

            listingsCollection.document(listingId)
                .update(updates)
                .await()

            Log.d(TAG, "✅ Listing status updated to $status: $listingId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating listing status: ${e.message}")
            false
        }
    }

    /**
     * FR-2.2.3: Increment view count for analytics
     */
    private suspend fun incrementViewCount(listingId: String) {
        try {
            firestore.runTransaction { transaction ->
                val docRef = listingsCollection.document(listingId)
                val snapshot = transaction.get(docRef)

                val currentViews = snapshot.getLong("viewCount") ?: 0
                transaction.update(docRef, "viewCount", currentViews + 1)
            }.await()
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Failed to increment view count: ${e.message}")
        }
    }

    /**
     * FR-3.2.2: Get listings by category for recommendations
     */
    suspend fun getListingsByCategory(category: String, limit: Int = 20): List<Listing> {
        return try {
            val snapshot = listingsCollection
                .whereEqualTo("status", "available")
                .whereEqualTo("category", category)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val listings = snapshot.toObjects(Listing::class.java)
            Log.d(TAG, "✅ Retrieved ${listings.size} listings for category: $category")
            listings
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting listings by category: ${e.message}")
            emptyList()
        }
    }

    /**
     * Generate search keywords from title, description, and category
     */
    private fun generateSearchKeywords(title: String, description: String, category: String): String {
        val keywords = mutableSetOf<String>()

        // Add words from title
        title.lowercase().split("\\s+".toRegex()).forEach { word ->
            if (word.length > 2) keywords.add(word)
        }

        // Add words from description
        description.lowercase().split("\\s+".toRegex()).forEach { word ->
            if (word.length > 2) keywords.add(word)
        }

        // Add category
        keywords.add(category.lowercase())

        return keywords.joinToString(" ")
    }
}

// Note: SortOption is defined separately in SortOption.kt