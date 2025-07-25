package com.example.tradeup.listing

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
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

class MyListingsFragment : Fragment() {

    private lateinit var recyclerListings: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var btnCreateFirstListing: MaterialButton
    private lateinit var tvTotalListings: TextView
    private lateinit var tvAvailableCount: TextView
    private lateinit var tvSoldCount: TextView

    private lateinit var listingsAdapter: UserListingsAdapter
    private val listingRepository = ListingRepository()
    private val auth = FirebaseAuth.getInstance()
    private var currentListings = mutableListOf<Listing>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_my_listings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        setupSwipeRefresh()
        setupEmptyState()
        loadUserListings()
    }

    private fun initViews(view: View) {
        recyclerListings = view.findViewById(R.id.recyclerListings)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout)
        progressBar = view.findViewById(R.id.progressBar)
        btnCreateFirstListing = view.findViewById(R.id.btnCreateFirstListing)
        tvTotalListings = view.findViewById(R.id.tvTotalListings)
        tvAvailableCount = view.findViewById(R.id.tvAvailableCount)
        tvSoldCount = view.findViewById(R.id.tvSoldCount)
    }

    private fun setupRecyclerView() {
        listingsAdapter = UserListingsAdapter { listing, action ->
            handleListingAction(listing, action)
        }

        recyclerListings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = listingsAdapter
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            loadUserListings()
        }
    }

    private fun setupEmptyState() {
        btnCreateFirstListing.setOnClickListener {
            startActivity(Intent(requireContext(), AddListingActivity::class.java))
        }
    }

    // FR-2.2.1: Load user's listings
    private fun loadUserListings() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showError("Please log in to view your listings")
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            listingRepository.getUserListings(currentUser.uid).fold(
                onSuccess = { listings ->
                    currentListings.clear()
                    currentListings.addAll(listings)
                    updateUI(listings)
                    showLoading(false)
                },
                onFailure = { exception ->
                    showError("Failed to load listings: ${exception.message}")
                    showLoading(false)
                }
            )
        }
    }

    private fun updateUI(listings: List<Listing>) {
        swipeRefresh.isRefreshing = false

        if (listings.isEmpty()) {
            showEmptyState(true)
            updateStats(0, 0, 0)
        } else {
            showEmptyState(false)
            listingsAdapter.updateListings(listings)

            // Update statistics
            val available = listings.count { it.status == "Available" }
            val sold = listings.count { it.status == "Sold" }
            val total = listings.size
            updateStats(total, available, sold)
        }
    }

    private fun updateStats(total: Int, available: Int, sold: Int) {
        tvTotalListings.text = total.toString()
        tvAvailableCount.text = available.toString()
        tvSoldCount.text = sold.toString()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showEmptyState(show: Boolean) {
        emptyStateLayout.visibility = if (show) View.VISIBLE else View.GONE
        recyclerListings.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        swipeRefresh.isRefreshing = false
        showLoading(false)
    }

    // FR-2.2.1: Handle listing actions (edit/delete/change status)
    private fun handleListingAction(listing: Listing, action: String) {
        when (action) {
            "edit" -> {
                // Navigate to edit listing
                Toast.makeText(requireContext(), "Edit listing: ${listing.title}", Toast.LENGTH_SHORT).show()
            }
            "delete" -> {
                deleteListing(listing)
            }
            "change_status" -> {
                showStatusChangeDialog(listing)
            }
            "view_analytics" -> {
                showAnalytics(listing)
            }
        }
    }

    private fun deleteListing(listing: Listing) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Listing")
            .setMessage("Are you sure you want to delete '${listing.title}'?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    listingRepository.deleteListing(listing.id).fold(
                        onSuccess = {
                            Toast.makeText(requireContext(), "Listing deleted", Toast.LENGTH_SHORT).show()
                            loadUserListings() // Refresh
                        },
                        onFailure = { exception ->
                            showError("Failed to delete: ${exception.message}")
                        }
                    )
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showStatusChangeDialog(listing: Listing) {
        val statuses = arrayOf("Available", "Sold", "Paused")
        val currentIndex = statuses.indexOf(listing.status)

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Change Status")
            .setSingleChoiceItems(statuses, currentIndex) { dialog, which ->
                val newStatus = statuses[which]
                updateListingStatus(listing, newStatus)
                dialog.dismiss()
            }
            .show()
    }

    private fun updateListingStatus(listing: Listing, newStatus: String) {
        lifecycleScope.launch {
            val updates = mapOf("status" to newStatus)
            listingRepository.updateListing(listing.id, updates).fold(
                onSuccess = {
                    Toast.makeText(requireContext(), "Status updated to $newStatus", Toast.LENGTH_SHORT).show()
                    loadUserListings() // Refresh
                },
                onFailure = { exception ->
                    showError("Failed to update status: ${exception.message}")
                }
            )
        }
    }

    private fun showAnalytics(listing: Listing) {
        val message = """
            Views: ${listing.views}
            Interactions: ${listing.interactions}
            Created: ${listing.createdAt}
            Status: ${listing.status}
        """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Analytics: ${listing.title}")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        // Refresh when returning to fragment
        loadUserListings()
    }
}