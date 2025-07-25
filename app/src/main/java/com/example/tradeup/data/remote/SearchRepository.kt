package com.example.tradeup.data.remote

import com.example.tradeup.data.model.Listing
import com.example.tradeup.data.model.Category
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class SearchRepository {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("listings")

    // FR-3.1.1: Simple search with real Firebase data
    suspend fun searchListings(
        keywords: String = "",
        category: String = "",
        minPrice: Double = 0.0,
        maxPrice: Double = Double.MAX_VALUE,
        condition: String = "",
        sortBy: SortOption = SortOption.NEWEST
    ): Result<List<Listing>> {
        return try {
            // Get all available listings (simple query, no index needed)
            val snapshot = collection
                .whereEqualTo("status", "Available")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            var listings = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Listing::class.java)?.copy(id = doc.id)
            }

            // Apply all filters CLIENT-SIDE (no server indexes needed)

            // Filter by keywords
            if (keywords.isNotEmpty()) {
                val searchTerms = keywords.lowercase().split(" ")
                listings = listings.filter { listing ->
                    searchTerms.any { term ->
                        listing.title.lowercase().contains(term) ||
                                listing.description.lowercase().contains(term)
                    }
                }
            }

            // Filter by category
            if (category.isNotEmpty() && category != "All") {
                listings = listings.filter { listing ->
                    listing.category == category
                }
            }

            // Filter by condition
            if (condition.isNotEmpty()) {
                listings = listings.filter { listing ->
                    listing.condition == condition
                }
            }

            // Filter by price range
            if (minPrice > 0 || maxPrice < Double.MAX_VALUE) {
                listings = listings.filter { listing ->
                    listing.price >= minPrice && listing.price <= maxPrice
                }
            }

            // Apply sorting
            listings = when (sortBy) {
                SortOption.NEWEST -> listings.sortedByDescending { it.createdAt.seconds }
                SortOption.PRICE_LOW_HIGH -> listings.sortedBy { it.price }
                SortOption.PRICE_HIGH_LOW -> listings.sortedByDescending { it.price }
                SortOption.MOST_VIEWED -> listings.sortedByDescending { it.views }
            }

            Result.success(listings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // FR-3.2.1: Get listings by category
    suspend fun getListingsByCategory(category: String): Result<List<Listing>> {
        return searchListings(category = category)
    }

    // FR-3.2.2: Get all listings (for initial load) with debug logging
    suspend fun getAllListings(): Result<List<Listing>> {
        return try {
            android.util.Log.d("SearchRepository", "🔍 Querying Firebase for listings...")

            val snapshot = collection
                .whereEqualTo("status", "Available")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()

            android.util.Log.d("SearchRepository", "📊 Firebase returned ${snapshot.documents.size} documents")

            val listings = snapshot.documents.mapNotNull { doc ->
                android.util.Log.d("SearchRepository", "📄 Processing document: ${doc.id}")
                val listing = doc.toObject(Listing::class.java)?.copy(id = doc.id)
                if (listing != null) {
                    android.util.Log.d("SearchRepository", "✅ Parsed listing: ${listing.title}")
                } else {
                    android.util.Log.w("SearchRepository", "⚠️ Failed to parse document: ${doc.id}")
                }
                listing
            }

            android.util.Log.d("SearchRepository", "🎯 Final result: ${listings.size} valid listings")
            Result.success(listings)
        } catch (e: Exception) {
            android.util.Log.e("SearchRepository", "💥 Error loading listings: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // FR-2.2.3: Increment views (real Firebase update)
    suspend fun incrementViews(listingId: String): Result<Unit> {
        return try {
            collection.document(listingId)
                .update("views", com.google.firebase.firestore.FieldValue.increment(1))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// FR-3.1.3: Sorting options enum
enum class SortOption(val displayName: String) {
    NEWEST("Newest"),
    PRICE_LOW_HIGH("Price: Low to High"),
    PRICE_HIGH_LOW("Price: High to Low"),
    MOST_VIEWED("Most Viewed")
}