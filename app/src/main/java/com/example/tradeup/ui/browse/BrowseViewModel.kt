package com.example.tradeup.ui.browse

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tradeup.data.model.Listing
import com.example.tradeup.data.remote.SearchRepository
import com.example.tradeup.data.remote.SortOption
import kotlinx.coroutines.launch

class BrowseViewModel : ViewModel() {

    private val searchRepository = SearchRepository()

    private val _listings = MutableLiveData<List<Listing>>()
    val listings: LiveData<List<Listing>> = _listings

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    // Current search parameters
    private var currentKeywords = ""
    private var currentCategory = "All"
    private var currentSortOption = SortOption.NEWEST
    private var currentMinPrice = 0.0
    private var currentMaxPrice = Double.MAX_VALUE
    private var currentCondition = ""

    // FR-3.2.2: Load initial listings from Firebase (with debug logging)
    fun loadListings() {
        viewModelScope.launch {
            _isLoading.value = true
            android.util.Log.d("BrowseViewModel", "🔍 Starting to load listings...")

            searchRepository.getAllListings().fold(
                onSuccess = { listings ->
                    android.util.Log.d("BrowseViewModel", "✅ Successfully loaded ${listings.size} listings")
                    listings.forEach { listing ->
                        android.util.Log.d("BrowseViewModel", "📋 Listing: ${listing.title} by ${listing.sellerName}")
                    }
                    _listings.value = listings
                    _isLoading.value = false
                },
                onFailure = { exception ->
                    android.util.Log.e("BrowseViewModel", "❌ Failed to load listings: ${exception.message}")
                    exception.printStackTrace()
                    _error.value = exception.message ?: "Failed to load listings"
                    _isLoading.value = false
                }
            )
        }
    }

    // FR-3.1.1 & FR-3.1.2: Search with keywords (triggered after 200ms)
    fun searchListings(keywords: String) {
        currentKeywords = keywords
        performSearch()
    }

    // FR-3.2.1: Filter by category
    fun filterByCategory(category: String) {
        currentCategory = category
        performSearch()
    }

    // FR-3.1.3: Sort listings
    fun sortListings(sortOption: SortOption) {
        currentSortOption = sortOption
        performSearch()
    }

    // Apply price range filter
    fun applyPriceFilter(minPrice: Double, maxPrice: Double) {
        currentMinPrice = minPrice
        currentMaxPrice = maxPrice
        performSearch()
    }

    // Apply condition filter
    fun applyConditionFilter(condition: String) {
        currentCondition = condition
        performSearch()
    }

    // Core search function using Firebase
    private fun performSearch() {
        viewModelScope.launch {
            _isLoading.value = true
            searchRepository.searchListings(
                keywords = currentKeywords,
                category = currentCategory,
                minPrice = currentMinPrice,
                maxPrice = currentMaxPrice,
                condition = currentCondition,
                sortBy = currentSortOption
            ).fold(
                onSuccess = { listings ->
                    _listings.value = listings
                    _isLoading.value = false
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Search failed"
                    _isLoading.value = false
                }
            )
        }
    }

    // FR-2.2.3: Increment views when item is clicked
    fun incrementViews(listingId: String) {
        viewModelScope.launch {
            searchRepository.incrementViews(listingId)
        }
    }

    // Clear all filters and reload
    fun clearFilters() {
        currentKeywords = ""
        currentCategory = "All"
        currentSortOption = SortOption.NEWEST
        currentMinPrice = 0.0
        currentMaxPrice = Double.MAX_VALUE
        currentCondition = ""
        loadListings()
    }
}