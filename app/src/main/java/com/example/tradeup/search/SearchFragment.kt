// app/src/main/java/com/example/tradeup/search/SearchFragment.kt
package com.example.tradeup.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tradeup.R
import com.example.tradeup.data.model.Listing
import com.example.tradeup.listing.ListingAdapter
import com.google.firebase.firestore.*
import java.util.*

class SearchFragment : Fragment() {

    private lateinit var etSearch: EditText
    private lateinit var spCategory: Spinner
    private lateinit var spSortBy: Spinner
    private lateinit var rvResults: RecyclerView
    private lateinit var tvResultCount: TextView
    private lateinit var progressBar: ProgressBar

    private val searchResults = mutableListOf<Listing>()
    private lateinit var adapter: ListingAdapter
    private var searchTimer: Timer? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        setupSearch()
        setupFilters()
        loadAllListings()
    }

    private fun initViews(view: View) {
        etSearch = view.findViewById(R.id.etSearch)
        spCategory = view.findViewById(R.id.spCategory)
        spSortBy = view.findViewById(R.id.spSortBy)
        rvResults = view.findViewById(R.id.rvResults)
        tvResultCount = view.findViewById(R.id.tvResultCount)
        progressBar = view.findViewById(R.id.progressBar)
    }

    private fun setupRecyclerView() {
        adapter = ListingAdapter(searchResults)
        rvResults.adapter = adapter
        rvResults.layoutManager = GridLayoutManager(requireContext(), 2)
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                searchTimer?.cancel()
                searchTimer = Timer()
                searchTimer?.schedule(object : TimerTask() {
                    override fun run() {
                        activity?.runOnUiThread {
                            performSearch()
                        }
                    }
                }, 300) // 300ms delay
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupFilters() {
        // Categories
        val categories = arrayOf("All Categories", "Electronics", "Fashion", "Home & Garden", "Sports", "Books", "Other")
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)
        spCategory.adapter = categoryAdapter

        // Sort options
        val sortOptions = arrayOf("Newest First", "Oldest First", "Price: Low to High", "Price: High to Low", "Most Popular")
        val sortAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, sortOptions)
        spSortBy.adapter = sortAdapter

        spCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                performSearch()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spSortBy.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                performSearch()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun performSearch() {
        val query = etSearch.text.toString().trim().lowercase()
        val selectedCategory = spCategory.selectedItem.toString()
        val sortBy = spSortBy.selectedItemPosition

        showLoading(true)

        var firestoreQuery: Query = FirebaseFirestore.getInstance().collection("listings")

        // Filter by category
        if (selectedCategory != "All Categories") {
            firestoreQuery = firestoreQuery.whereEqualTo("category", selectedCategory)
        }

        // Apply sorting
        when (sortBy) {
            0 -> firestoreQuery = firestoreQuery.orderBy("createdAt", Query.Direction.DESCENDING)
            1 -> firestoreQuery = firestoreQuery.orderBy("createdAt", Query.Direction.ASCENDING)
            2 -> firestoreQuery = firestoreQuery.orderBy("price", Query.Direction.ASCENDING)
            3 -> firestoreQuery = firestoreQuery.orderBy("price", Query.Direction.DESCENDING)
            4 -> firestoreQuery = firestoreQuery.orderBy("views", Query.Direction.DESCENDING)
        }

        firestoreQuery.get()
            .addOnSuccessListener { documents ->
                searchResults.clear()

                for (document in documents) {
                    val listing = document.toObject(Listing::class.java)

                    // Text search filtering
                    if (query.isEmpty() ||
                        listing.title.lowercase().contains(query) ||
                        listing.description.lowercase().contains(query) ||
                        listing.tags.any { it.lowercase().contains(query) }) {
                        searchResults.add(listing)
                    }
                }

                adapter.notifyDataSetChanged()
                updateResultCount()
                showLoading(false)
            }
            .addOnFailureListener {
                showLoading(false)
                Toast.makeText(requireContext(), "Search failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadAllListings() {
        performSearch()
    }

    private fun updateResultCount() {
        tvResultCount.text = "${searchResults.size} items found"
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        rvResults.visibility = if (show) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchTimer?.cancel()
    }
}