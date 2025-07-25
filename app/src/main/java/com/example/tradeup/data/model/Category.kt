package com.example.tradeup.data.model

/**
 * FR-3.1.1 & FR-3.2.1: Category system for organizing listings
 */
data class Category(
    val name: String,
    val displayName: String,
    val iconResource: String? = null,
    val subcategories: List<String> = emptyList()
) {
    companion object {
        // FR-3.2.1: Items organized under categories
        fun getAllCategories(): List<Category> {
            return listOf(
                Category(
                    name = "electronics",
                    displayName = "Electronics",
                    subcategories = listOf("Phones", "Laptops", "Gaming", "Audio", "Cameras")
                ),
                Category(
                    name = "fashion",
                    displayName = "Fashion & Beauty",
                    subcategories = listOf("Men's Clothing", "Women's Clothing", "Shoes", "Accessories", "Beauty")
                ),
                Category(
                    name = "home",
                    displayName = "Home & Garden",
                    subcategories = listOf("Furniture", "Decor", "Kitchen", "Garden", "Tools")
                ),
                Category(
                    name = "vehicles",
                    displayName = "Vehicles",
                    subcategories = listOf("Cars", "Motorcycles", "Bicycles", "Parts", "Accessories")
                ),
                Category(
                    name = "sports",
                    displayName = "Sports & Recreation",
                    subcategories = listOf("Fitness", "Outdoor", "Sports Equipment", "Games", "Hobbies")
                ),
                Category(
                    name = "books",
                    displayName = "Books & Media",
                    subcategories = listOf("Books", "Movies", "Music", "Games", "Magazines")
                ),
                Category(
                    name = "pets",
                    displayName = "Pets & Animals",
                    subcategories = listOf("Pet Supplies", "Pet Care", "Pet Food", "Toys", "Accessories")
                ),
                Category(
                    name = "services",
                    displayName = "Services",
                    subcategories = listOf("Tutoring", "Repair", "Cleaning", "Photography", "Other")
                ),
                Category(
                    name = "other",
                    displayName = "Other",
                    subcategories = listOf("Miscellaneous", "Collectibles", "Art", "Antiques", "Free Items")
                )
            )
        }

        fun getCategoryByName(name: String): Category? {
            return getAllCategories().find { it.name == name }
        }

        fun getCategoryDisplayNames(): List<String> {
            return getAllCategories().map { it.displayName }
        }
    }
}