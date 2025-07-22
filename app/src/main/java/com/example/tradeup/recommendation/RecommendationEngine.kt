package com.example.tradeup.recommendations

import com.example.tradeup.data.model.Listing
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

object RecommendationEngine {

    fun getPersonalizedRecommendations(
        onResult: (List<Listing>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        if (currentUserId == null) {
            // Show popular items for non-logged users
            getPopularItems(onResult, onError)
            return
        }

        // Get user's browsing history and preferences
        getUserPreferences(currentUserId) { categories ->
            if (categories.isNotEmpty()) {
                getRecommendationsByCategories(categories, onResult, onError)
            } else {
                getNearbyItems(onResult, onError)
            }
        }
    }

    private fun getUserPreferences(userId: String, onResult: (List<String>) -> Unit) {
        // This would typically analyze user's viewing/purchasing history
        // For now, return some default categories
        onResult(listOf("Đồ điện tử", "Thời trang"))
    }

    private fun getRecommendationsByCategories(
        categories: List<String>,
        onResult: (List<Listing>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        FirebaseFirestore.getInstance()
            .collection("listings")
            .whereIn("category", categories)
            .orderBy("views", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { documents ->
                val recommendations = documents.map { it.toObject(Listing::class.java) }
                onResult(recommendations)
            }
            .addOnFailureListener(onError)
    }

    private fun getPopularItems(
        onResult: (List<Listing>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        FirebaseFirestore.getInstance()
            .collection("listings")
            .orderBy("views", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { documents ->
                val popular = documents.map { it.toObject(Listing::class.java) }
                onResult(popular)
            }
            .addOnFailureListener(onError)
    }

    private fun getNearbyItems(
        onResult: (List<Listing>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        // For now, just get recent items
        // In a real app, you'd use location-based queries
        FirebaseFirestore.getInstance()
            .collection("listings")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { documents ->
                val nearby = documents.map { it.toObject(Listing::class.java) }
                onResult(nearby)
            }
            .addOnFailureListener(onError)
    }
}