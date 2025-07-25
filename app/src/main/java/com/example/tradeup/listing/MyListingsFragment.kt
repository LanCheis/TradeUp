package com.example.tradeup.listing

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.tradeup.R
import com.example.tradeup.data.model.Listing
import com.example.tradeup.data.remote.ListingRepository
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * FR-2.2.1: Fragment to display and manage user's own listings
 */
class MyListingsFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var listingRepository: ListingRepository

    // Views matching your layout
    private lateinit var tvTotalListings: TextView
    private lateinit var tvAvailableCount: TextView
    private lateinit var tvSoldCount: TextView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var recyclerListings: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var btnCreateFirstListing: MaterialButton

    // Data
    private lateinit var listingsAdapter: MyListingsAdapter
    private val listings = mutableListOf<Listing>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_my_listings, container, false)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        listingRepository = ListingRepository()

        // Initialize views
        initViews(view)
        setupRecyclerView()
        setupClickListeners()

        // Load user's listings
        loadMyListings()

        return view
    }

    private fun initViews(view: View) {
        // Stats header
        tvTotalListings = view.findViewById(R.id.tvTotalListings)
        tvAvailableCount = view.findViewById(R.id.tvAvailableCount)
        tvSoldCount = view.findViewById(R.id.tvSoldCount)

        // Main content
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        recyclerListings = view.findViewById(R.id.recyclerListings)
        progressBar = view.findViewById(R.id.progressBar)
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout)
        btnCreateFirstListing = view.findViewById(R.id.btnCreateFirstListing)
    }

    private fun setupRecyclerView() {
        listingsAdapter = MyListingsAdapter(listings) { listing, action ->
            when (action) {
                "edit" -> editListing(listing)
                "delete" -> deleteListing(listing)
                "toggle_status" -> toggleListingStatus(listing)
            }
        }

        recyclerListings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = listingsAdapter
        }
    }

    private fun setupClickListeners() {
        // Swipe to refresh
        swipeRefresh.setOnRefreshListener {
            loadMyListings()
        }

        // Create first listing button
        btnCreateFirstListing.setOnClickListener {
            startActivity(Intent(requireContext(), AddListingActivity::class.java))
        }
    }

    /**
     * FR-2.2.1: Load current user's listings
     */
    private fun loadMyListings() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                val userListings = listingRepository.getUserListings(currentUser.uid)

                listings.clear()
                listings.addAll(userListings)
                listingsAdapter.notifyDataSetChanged()

                // Update stats
                updateStats()

                showLoading(false)
                swipeRefresh.isRefreshing = false

                // Show/hide empty state
                updateEmptyState()

            } catch (e: Exception) {
                showLoading(false)
                swipeRefresh.isRefreshing = false
                Toast.makeText(context, "Error loading listings: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * FR-2.2.3: Update listing statistics
     */
    private fun updateStats() {
        val totalCount = listings.size
        val availableCount = listings.count { it.status == "available" }
        val soldCount = listings.count { it.status == "sold" }

        tvTotalListings.text = totalCount.toString()
        tvAvailableCount.text = availableCount.toString()
        tvSoldCount.text = soldCount.toString()
    }

    private fun updateEmptyState() {
        if (listings.isEmpty()) {
            emptyStateLayout.visibility = View.VISIBLE
            recyclerListings.visibility = View.GONE
        } else {
            emptyStateLayout.visibility = View.GONE
            recyclerListings.visibility = View.VISIBLE
        }
    }

    /**
     * FR-2.2.1: Edit a listing
     */
    private fun editListing(listing: Listing) {
        // For now, just show a message since EditListingActivity might not exist
        Toast.makeText(context, "Edit functionality coming soon!", Toast.LENGTH_SHORT).show()

        // Uncomment when EditListingActivity is created:
        // val intent = Intent(requireContext(), EditListingActivity::class.java)
        // intent.putExtra("listing_id", listing.id)
        // startActivity(intent)
    }

    /**
     * FR-2.2.1: Delete a listing
     */
    private fun deleteListing(listing: Listing) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Listing")
            .setMessage("Are you sure you want to delete '${listing.title}'?")
            .setPositiveButton("Delete") { _, _ ->
                performDeleteListing(listing)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performDeleteListing(listing: Listing) {
        lifecycleScope.launch {
            try {
                val success = listingRepository.deleteListing(listing.id)

                if (success) {
                    listings.remove(listing)
                    listingsAdapter.notifyDataSetChanged()
                    updateStats()
                    updateEmptyState()
                    Toast.makeText(context, "Listing deleted successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to delete listing", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error deleting listing: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * FR-2.2.2: Toggle listing status (available/paused/sold)
     */
    private fun toggleListingStatus(listing: Listing) {
        val statuses = arrayOf("available", "paused", "sold")
        val statusNames = arrayOf("Available", "Paused", "Sold")

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Change Status")
            .setItems(statusNames) { _, which ->
                val newStatus = statuses[which]
                updateListingStatus(listing, newStatus)
            }
            .show()
    }

    private fun updateListingStatus(listing: Listing, newStatus: String) {
        lifecycleScope.launch {
            try {
                val success = listingRepository.updateListingStatus(listing.id, newStatus)

                if (success) {
                    // Update local listing status
                    val index = listings.indexOfFirst { it.id == listing.id }
                    if (index != -1) {
                        listings[index] = listing.copy(status = newStatus)
                        listingsAdapter.notifyItemChanged(index)
                        updateStats()
                    }

                    Toast.makeText(context, "Status updated to $newStatus", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to update status", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error updating status: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        // Refresh listings when returning from other activities
        loadMyListings()
    }
}