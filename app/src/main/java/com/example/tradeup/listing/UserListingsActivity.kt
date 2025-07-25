package com.example.tradeup.listing

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.tradeup.R
import com.example.tradeup.data.model.Listing
import com.example.tradeup.data.model.ListingStatus
import com.example.tradeup.data.remote.ListingRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.launch

class UserListingsActivity : AppCompatActivity() {

    // UI Components
    private lateinit var recyclerListings: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvEmptyState: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvTotalListings: TextView
    private lateinit var tvAvailableCount: TextView
    private lateinit var tvSoldCount: TextView
    private lateinit var tvPausedCount: TextView
    private lateinit var btnCreateFirstListing: Button

    // Data & Adapters
    private lateinit var listingsAdapter: UserListingsAdapter
    private val listingRepository = ListingRepository()
    private val auth = FirebaseAuth.getInstance()
    private var currentListings = mutableListOf<Listing>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_listings)

        setupToolbar()
        setupViews()
        setupRecyclerView()
        setupSwipeRefresh()
        setupClickListeners()
        loadUserListings()
    }

    private fun setupToolbar() {
        supportActionBar?.apply {
            title = "My Listings"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupViews() {
        recyclerListings = findViewById(R.id.recyclerListings)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        progressBar = findViewById(R.id.progressBar)
        tvTotalListings = findViewById(R.id.tvTotalListings)
        tvAvailableCount = findViewById(R.id.tvAvailableCount)
        tvSoldCount = findViewById(R.id.tvSoldCount)
        tvPausedCount = findViewById(R.id.tvPausedCount)
        btnCreateFirstListing = findViewById(R.id.btnCreateFirstListing)
    }

    private fun setupRecyclerView() {
        // FR-2.2.1: Setup adapter with action callbacks
        listingsAdapter = UserListingsAdapter { listing: Listing, action: String ->
            when (action) {
                "edit" -> editListing(listing)
                "delete" -> showDeleteConfirmation(listing)
                "toggle_status" -> showStatusChangeDialog(listing)
                "view_analytics" -> showAnalytics(listing)
                "view_details" -> viewListingDetails(listing)
                "options_error" -> {
                    Toast.makeText(this, "Menu error occurred", Toast.LENGTH_SHORT).show()
                }
            }
        }

        recyclerListings.apply {
            layoutManager = LinearLayoutManager(this@UserListingsActivity)
            adapter = listingsAdapter
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            loadUserListings()
        }
    }

    private fun setupClickListeners() {
        btnCreateFirstListing.setOnClickListener {
            startActivity(Intent(this, AddListingActivity::class.java))
        }
    }

    // FR-2.2.1: Load user's listings from database
    private fun loadUserListings() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to view your listings", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                val result = listingRepository.getUserListings(currentUser.uid)

                if (result.isSuccess) {
                    val listings: List<Listing> = result.getOrNull() ?: emptyList()
                    currentListings.clear()
                    currentListings.addAll(listings)

                    updateUI(listings)
                } else {
                    showError("Failed to load listings: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                showError("Error loading listings: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun updateUI(listings: List<Listing>) {
        listingsAdapter.updateListings(listings)
        updateStatistics(listings)

        // Show empty state if no listings
        if (listings.isEmpty()) {
            recyclerListings.visibility = View.GONE
            tvEmptyState.visibility = View.VISIBLE
        } else {
            recyclerListings.visibility = View.VISIBLE
            tvEmptyState.visibility = View.GONE
        }
    }

    // FR-2.2.3: Update analytics display
    private fun updateStatistics(listings: List<Listing>) {
        val totalListings = listings.size
        val availableCount = listings.count { it.status == ListingStatus.AVAILABLE }
        val soldCount = listings.count { it.status == ListingStatus.SOLD }
        val pausedCount = listings.count { it.status == ListingStatus.PAUSED }

        tvTotalListings.text = "Total: $totalListings"
        tvAvailableCount.text = "Available: $availableCount"
        tvSoldCount.text = "Sold: $soldCount"
        tvPausedCount.text = "Paused: $pausedCount"
    }

    // FR-2.2.1: Edit listing functionality
    private fun editListing(listing: Listing) {
        val intent = Intent(this, EditListingActivity::class.java).apply {
            putExtra("listing_id", listing.id)
        }
        startActivity(intent)
    }

    // FR-2.2.1: View listing details
    private fun viewListingDetails(listing: Listing) {
        // TODO: Create ListingDetailsActivity or show in a dialog
        Toast.makeText(this, "Viewing: ${listing.title}", Toast.LENGTH_SHORT).show()
    }

    // FR-2.2.1: Delete listing with confirmation
    private fun showDeleteConfirmation(listing: Listing) {
        AlertDialog.Builder(this)
            .setTitle("Delete Listing")
            .setMessage("Are you sure you want to delete \"${listing.title}\"?\n\nThis action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteListing(listing)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteListing(listing: Listing) {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val result = listingRepository.deleteListing(listing.id)

                if (result.isSuccess) {
                    Toast.makeText(this@UserListingsActivity, "Listing deleted successfully", Toast.LENGTH_SHORT).show()
                    loadUserListings() // Refresh the list
                } else {
                    showError("Failed to delete listing")
                }
            } catch (e: Exception) {
                showError("Error: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    // FR-2.2.2: Change listing status (Available/Sold/Paused)
    private fun showStatusChangeDialog(listing: Listing) {
        val statusOptions: Array<String> = arrayOf(
            ListingStatus.AVAILABLE,
            ListingStatus.SOLD,
            ListingStatus.PAUSED
        )

        val currentIndex = statusOptions.indexOf(listing.status)

        AlertDialog.Builder(this)
            .setTitle("Change Status")
            .setSingleChoiceItems(statusOptions, currentIndex) { dialog, which ->
                val newStatus = statusOptions[which]
                if (newStatus != listing.status) {
                    changeListingStatus(listing, newStatus)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun changeListingStatus(listing: Listing, newStatus: String) {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val updates: Map<String, Any> = mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                val result = listingRepository.updateListing(listing.id, updates)

                if (result.isSuccess) {
                    Toast.makeText(this@UserListingsActivity, "Status updated to $newStatus", Toast.LENGTH_SHORT).show()
                    loadUserListings() // Refresh the list
                } else {
                    showError("Failed to update status")
                }
            } catch (e: Exception) {
                showError("Error: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    // FR-2.2.3: Show listing analytics
    private fun showAnalytics(listing: Listing) {
        val intent = Intent(this, ListingAnalyticsActivity::class.java).apply {
            putExtra("listing_id", listing.id)
            putExtra("listing_title", listing.title)
        }
        startActivity(intent)
    }

    private fun showLoading(show: Boolean) {
        swipeRefresh.isRefreshing = false
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    // Refresh when returning from edit
    override fun onResume() {
        super.onResume()
        loadUserListings()
    }
}