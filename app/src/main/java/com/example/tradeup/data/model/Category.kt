package com.example.tradeup.data.model

// FR-3.2.1: Categories for item organization
enum class Category(val displayName: String) {
    ALL("All"),
    ELECTRONICS("Electronics"),
    CLOTHING("Clothing & Fashion"),
    HOME("Home & Garden"),
    VEHICLES("Vehicles"),
    BOOKS("Books & Media"),
    SPORTS("Sports & Outdoors"),
    FURNITURE("Furniture"),
    OTHER("Other");

    companion object {
        fun fromString(value: String): Category {
            return values().find { it.displayName == value } ?: OTHER
        }
    }
}