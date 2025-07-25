package com.example.tradeup.data.remote

import android.util.Log
import com.example.tradeup.data.model.Listing
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class SearchRepository {
    private val collection = FirebaseFirestore.getInstance().collection("listings")

    // FR-3.2.2: Get all listings (method that was missing)
    suspend fun getAllListings(): Result<List<Listing>> {
        return try {
            Log.d("SearchRepository", "🔍 Loading all available listings...")

            val snapshot = collection
                .whereEqualTo("status", "Available")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()

            Log.d("SearchRepository", "📊 Firebase returned ${snapshot.documents.size} documents")

            val listings = snapshot.documents.mapNotNull { doc ->
                try {
                    val listing = doc.toObject(Listing::class.java)?.copy(id = doc.id)
                    if (listing != null) {
                        Log.d("SearchRepository", "✅ Parsed listing: ${listing.title}")
                    } else {
                        Log.w("SearchRepository", "⚠️ Failed to parse document: ${doc.id}")
                    }
                    listing
                } catch (e: Exception) {
                    Log.w("SearchRepository", "⚠️ Parse error for ${doc.id}: ${e.message}")
                    null
                }
            }

            Log.d("SearchRepository", "🎯 Final result: ${listings.size} valid listings")
            Result.success(listings)
        } catch (e: Exception) {
            Log.e("SearchRepository", "💥 Error loading listings: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // FR-3.1.1: Enhanced search with proper filtering
    suspend fun searchListings(
        keywords: String = "",
        category: String = "All",
        minPrice: Double = 0.0,
        maxPrice: Double = Double.MAX_VALUE,
        condition: String = "",
        sortBy: SortOption = SortOption.NEWEST
    ): Result<List<Listing>> {
        return try {
            Log.d("SearchRepository", "🔍 Starting search: keywords='$keywords', category='$category'")

            // Start with basic query
            val snapshot = collection
                .whereEqualTo("status", "Available")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()

            Log.d("SearchRepository", "📊 Firebase returned ${snapshot.documents.size} documents")

            var listings = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Listing::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.w("SearchRepository", "⚠️ Failed to parse document: ${doc.id}", e)
                    null
                }
            }

            // Apply client-side filtering
            if (keywords.isNotEmpty()) {
                listings = listings.filter { listing ->
                    listing.title.contains(keywords, ignoreCase = true) ||
                            listing.description.contains(keywords, ignoreCase = true)
                }
            }

            if (category != "All") {
                listings = listings.filter { it.category == category }
            }

            if (condition.isNotEmpty()) {
                listings = listings.filter { it.condition == condition }
            }

            if (minPrice > 0.0 || maxPrice < Double.MAX_VALUE) {
                listings = listings.filter { it.price >= minPrice && it.price <= maxPrice }
            }

            // Apply sorting - FR-3.1.3
            listings = when (sortBy) {
                SortOption.NEWEST -> listings.sortedByDescending { it.createdAt.seconds }
                SortOption.PRICE_LOW_HIGH -> listings.sortedBy { it.price }
                SortOption.PRICE_HIGH_LOW -> listings.sortedByDescending { it.price }
                SortOption.MOST_VIEWED -> listings.sortedByDescending { it.views }
            }

            Log.d("SearchRepository", "🎯 Final filtered result: ${listings.size} listings")
            Result.success(listings)

        } catch (e: Exception) {
            Log.e("SearchRepository", "💥 Search failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    // FR-2.2.3: Increment views with better error handling
    suspend fun incrementViews(listingId: String): Result<Unit> {
        return try {
            Log.d("SearchRepository", "👁️ Incrementing views for listing: $listingId")
            collection.document(listingId)
                .update("views", com.google.firebase.firestore.FieldValue.increment(1))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SearchRepository", "💥 Failed to increment views: ${e.message}", e)
            Result.failure(e)
        }
    }
}

// FR-3.1.3: Sorting options
enum class SortOption(val displayName: String) {
    NEWEST("Newest"),
    PRICE_LOW_HIGH("Price: Low to High"),
    PRICE_HIGH_LOW("Price: High to Low"),
    MOST_VIEWED("Most Viewed")
}