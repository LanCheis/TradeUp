package com.example.tradeup.data.model

// FR-3.1.2: Category data model for search
data class Category(
    val id: String = "",
    val name: String = "",
    val icon: String = "",
    val color: String = "",
    val itemCount: Int = 0,
    val isActive: Boolean = true,
    val sortOrder: Int = 0
)

// FR-6.1, FR-6.2, FR-6.3: Location data model
data class LocationData(
    val id: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val city: String = "",
    val district: String = "",
    val address: String = "",
    val country: String = "Vietnam",
    val isUserLocation: Boolean = false,
    val radius: Double = 10.0 // Default search radius in km
)

// Search filter data model
data class SearchFilter(
    val query: String = "",
    val category: String = "",
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val condition: String = "",
    val location: String = "",
    val radius: Double = 50.0, // km
    val sortBy: String = "newest", // "newest", "oldest", "price_low", "price_high", "distance"
    val isNegotiable: Boolean? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

// Constants for predefined values
object AppConstants {
    val CATEGORIES = listOf(
        "Electronics",
        "Fashion",
        "Home & Garden",
        "Vehicles",
        "Sports",
        "Books",
        "Toys",
        "Music",
        "Other"
    )

    val CONDITIONS = listOf(
        "New",
        "Like New",
        "Good",
        "Fair",
        "Poor"
    )

    val SORT_OPTIONS = listOf(
        "newest" to "Newest First",
        "oldest" to "Oldest First",
        "price_low" to "Price: Low to High",
        "price_high" to "Price: High to Low",
        "distance" to "Distance"
    )
}