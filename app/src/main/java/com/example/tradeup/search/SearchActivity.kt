package com.example.tradeup.search

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tradeup.R
import com.example.tradeup.data.model.Listing
import com.example.tradeup.listing.ListingAdapter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class SearchActivity : AppCompatActivity() {

    private lateinit var etSearch: EditText
    private lateinit var btnFilter: ImageButton
    private lateinit var btnSort: ImageButton
    private lateinit var spCategory: Spinner
    private lateinit var tvResultCount: TextView
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: LinearLayout

    private val searchResults = mutableListOf<Listing>()
    private lateinit var adapter: ListingAdapter
    private var searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    // Filter variables
    private var selectedCategory = "All"
    private var minPrice = 0.0
    private var maxPrice = Double.MAX_VALUE
    private var selectedCondition = "All"
    private var maxDistance = 100.0 // km
    private var sortBy = "newest" // newest, price_low, price_high, relevance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        initViews()
        setupRecyclerView()
        setupSearchListener()
        setupClickListeners()
        setupCategorySpinner()

        // Load initial results
        performSearch("")
    }

    private fun initViews() {
        etSearch = findViewById(R.id.etSearch)
        btnFilter = findViewById(R.id.btnFilter)
        btnSort = findViewById(R.id.btnSort)
        spCategory = findViewById(R.id.spCategory)
        tvResultCount = findViewById(R.id.tvResultCount)
        rvSearchResults = findViewById(R.id.rvSearchResults)
        progressBar = findViewById(R.id.progressBar)
        layoutEmpty = findViewById(R.id.layoutEmpty)

        // Auto-focus search field
        etSearch.requestFocus()
    }

    private fun setupRecyclerView() {
        adapter = ListingAdapter(searchResults)
        rvSearchResults.layoutManager = LinearLayoutManager(this)
        rvSearchResults.adapter = adapter
    }

    private fun setupSearchListener() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // Cancel previous search
                searchRunnable?.let { searchHandler.removeCallbacks(it) }

                // Schedule new search with 200ms delay (as per requirements)
                searchRunnable = Runnable {
                    performSearch(s.toString().trim())
                }
                searchHandler.postDelayed(searchRunnable!!, 200L)
            }
        })
    }

    private fun setupClickListeners() {
        btnFilter.setOnClickListener {
            showFilterBottomSheet()
        }

        btnSort.setOnClickListener {
            showSortOptions()
        }
    }

    private fun setupCategorySpinner() {
        val categories = arrayOf("All", "Đồ điện tử", "Thời trang", "Đồ gia dụng", "Khác")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spCategory.adapter = spinnerAdapter

        spCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCategory = categories[position]
                performSearch(etSearch.text.toString().trim())
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun performSearch(query: String) {
        showLoading(true)

        var firestoreQuery: Query = FirebaseFirestore.getInstance().collection("listings")

        // Apply category filter
        if (selectedCategory != "All") {
            firestoreQuery = firestoreQuery.whereEqualTo("category", selectedCategory)
        }

        // Apply price range filter
        if (minPrice > 0) {
            firestoreQuery = firestoreQuery.whereGreaterThanOrEqualTo("price", minPrice)
        }
        if (maxPrice < Double.MAX_VALUE) {
            firestoreQuery = firestoreQuery.whereLessThanOrEqualTo("price", maxPrice)
        }

        // Apply condition filter
        if (selectedCondition != "All") {
            firestoreQuery = firestoreQuery.whereEqualTo("condition", selectedCondition)
        }

        // Apply sorting
        when (sortBy) {
            "newest" -> firestoreQuery = firestoreQuery.orderBy("createdAt", Query.Direction.DESCENDING)
            "price_low" -> firestoreQuery = firestoreQuery.orderBy("price", Query.Direction.ASCENDING)
            "price_high" -> firestoreQuery = firestoreQuery.orderBy("price", Query.Direction.DESCENDING)
            "relevance" -> {
                // For relevance, we'll sort by views and interactions
                firestoreQuery = firestoreQuery.orderBy("views", Query.Direction.DESCENDING)
            }
        }

        firestoreQuery.get()
            .addOnSuccessListener { documents ->
                searchResults.clear()

                for (document in documents) {
                    val listing = document.toObject(Listing::class.java)

                    // Apply text search filter
                    if (query.isEmpty() || matchesSearchQuery(listing, query)) {
                        searchResults.add(listing)
                    }
                }

                // Update UI
                adapter.notifyDataSetChanged()
                updateResultCount()
                showLoading(false)
                showEmptyState(searchResults.isEmpty())
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                showEmptyState(true)
                Toast.makeText(this, "Search failed: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun matchesSearchQuery(listing: Listing, query: String): Boolean {
        val searchTerms = query.lowercase().split(" ")
        val searchableText = "${listing.title} ${listing.description} ${listing.category}".lowercase()

        return searchTerms.all { term ->
            searchableText.contains(term)
        }
    }

    private fun showFilterBottomSheet() {
        val bottomSheet = SearchFiltersBottomSheet.newInstance(
            selectedCondition, minPrice, maxPrice, maxDistance
        )
        bottomSheet.setOnFiltersAppliedListener { condition, minP, maxP, distance ->
            selectedCondition = condition
            minPrice = minP
            maxPrice = maxP
            maxDistance = distance
            performSearch(etSearch.text.toString().trim())
        }
        bottomSheet.show(supportFragmentManager, "filters")
    }

    private fun showSortOptions() {
        val sortOptions = arrayOf("Newest", "Price: Low to High", "Price: High to Low", "Most Relevant")
        val sortValues = arrayOf("newest", "price_low", "price_high", "relevance")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Sort by")
            .setItems(sortOptions) { _, which ->
                sortBy = sortValues[which]
                performSearch(etSearch.text.toString().trim())
            }
            .show()
    }

    private fun updateResultCount() {
        tvResultCount.text = "${searchResults.size} results found"
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        rvSearchResults.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showEmptyState(show: Boolean) {
        layoutEmpty.visibility = if (show) View.VISIBLE else View.GONE
        rvSearchResults.visibility = if (show) View.GONE else View.VISIBLE
    }
}