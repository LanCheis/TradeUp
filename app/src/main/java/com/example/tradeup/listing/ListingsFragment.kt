package com.example.tradeup.listing

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tradeup.R
import com.example.tradeup.data.model.Listing
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ListingsFragment : Fragment() {

    private lateinit var rvListings: RecyclerView
    private lateinit var fabAddListing: FloatingActionButton
    private lateinit var layoutLoading: LinearLayout
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var spinnerSort: Spinner
    private lateinit var btnViewToggle: ImageButton

    private val listingList = mutableListOf<Listing>()
    private lateinit var listingAdapter: ListingAdapter
    private var isGridView = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_listings_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) v x{
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        setupClickListeners()
        setupSortSpinner()
        loadListings()
    }

    private fun initViews(view: View) {
        rvListings = view.findViewById(R.id.rvListings)
        fabAddListing = view.findViewById(R.id.fabAddListing)
        layoutLoading = view.findViewById(R.id.layoutLoading)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)
        spinnerSort = view.findViewById(R.id.spinnerSort)
        btnViewToggle = view.findViewById(R.id.btnViewToggle)
    }

    private fun setupRecyclerView() {
        listingAdapter = ListingAdapter(listingList)
        rvListings.layoutManager = LinearLayoutManager(requireContext())
        rvListings.adapter = listingAdapter
    }

    private fun setupClickListeners() {
        fabAddListing.setOnClickListener {
            startActivity(Intent(requireContext(), CreateListingActivity::class.java))
        }

        btnViewToggle.setOnClickListener {
            toggleView()
        }
    }

    private fun setupSortSpinner() {
        val sortOptions = arrayOf("Newest First", "Oldest First", "Price: Low to High", "Price: High to Low", "Most Popular")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, sortOptions)
        spinnerSort.adapter = adapter

        spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                sortListings(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun toggleView() {
        isGridView = !isGridView

        rvListings.layoutManager = if (isGridView) {
            GridLayoutManager(requireContext(), 2)
        } else {
            LinearLayoutManager(requireContext())
        }

        btnViewToggle.setImageResource(
            if (isGridView) R.drawable.ic_view_list else R.drawable.ic_view_grid
        )
    }

    private fun loadListings() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        showLoading(true)

        FirebaseFirestore.getInstance()
            .collection("listings")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                if (isAdded) {
                    listingList.clear()
                    for (document in result) {
                        val listing = document.toObject(Listing::class.java)
                        // Show all listings except current user's own listings
                        if (listing.ownerUid != currentUserId) {
                            listingList.add(listing)
                        }
                    }
                    listingAdapter.notifyDataSetChanged()
                    showLoading(false)

                    if (listingList.isEmpty()) {
                        showEmpty(true)
                    }
                }
            }
            .addOnFailureListener { exception ->
                if (isAdded) {
                    showLoading(false)
                    showEmpty(true)
                    Toast.makeText(requireContext(), "Failed to load listings", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun sortListings(sortType: Int) {
        when (sortType) {
            0 -> listingList.sortByDescending { it.createdAt } // Newest First
            1 -> listingList.sortBy { it.createdAt } // Oldest First
            2 -> listingList.sortBy { it.price } // Price: Low to High
            3 -> listingList.sortByDescending { it.price } // Price: High to Low
            4 -> listingList.sortByDescending { it.views } // Most Popular
        }
        listingAdapter.notifyDataSetChanged()
    }

    private fun showLoading(show: Boolean) {
        layoutLoading.visibility = if (show) View.VISIBLE else View.GONE
        rvListings.visibility = if (show) View.GONE else View.VISIBLE
        layoutEmpty.visibility = View.GONE
    }

    private fun showEmpty(show: Boolean) {
        layoutEmpty.visibility = if (show) View.VISIBLE else View.GONE
        rvListings.visibility = if (show) View.GONE else View.VISIBLE
        layoutLoading.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        // Refresh listings when returning to fragment
        loadListings()
    }
}