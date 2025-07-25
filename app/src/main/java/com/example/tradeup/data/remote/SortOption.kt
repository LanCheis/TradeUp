package com.example.tradeup.data.remote

/**
 * FR-3.1.3: Sorting options for listings
 */
enum class SortOption(val displayName: String) {
    RELEVANCE("Relevance"),
    NEWEST("Newest First"),
    OLDEST("Oldest First"),
    PRICE_LOW_TO_HIGH("Price: Low to High"),
    PRICE_HIGH_TO_LOW("Price: High to Low");

    companion object {
        fun getAllOptions(): List<SortOption> {
            return values().toList()
        }

        fun getDisplayNames(): List<String> {
            return values().map { it.displayName }
        }
    }
}