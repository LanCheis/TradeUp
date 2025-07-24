package com.example.tradeup.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tradeup.R
import com.example.tradeup.data.model.Listing
import com.example.tradeup.data.remote.UserRepository
import com.example.tradeup.listing.CreateListingActivity
import com.example.tradeup.listing.HorizontalListingAdapter
import com.example.tradeup.search.SearchActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var tvGreeting: TextView
    private lateinit var tvUserName: TextView
    private lateinit var btnSearch: ImageButton
    private lateinit var btnNotification: ImageButton
    private lateinit var btnSettings: ImageButton
    private lateinit var btnSellItem: LinearLayout
    private lateinit var btnBrowseCategories: LinearLayout
    private lateinit var tvSeeAll: TextView
    private lateinit var rvRecentListings: RecyclerView
    private lateinit var layoutLoading: LinearLayout
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var layoutComingSoon: LinearLayout

    private lateinit var tvWelcome: TextView
    private lateinit var auth: FirebaseAuth

    private val recentListings = mutableListOf<Listing>()
    private lateinit var listingAdapter: HorizontalListingAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        initializeViews(view)
        setupUI()
        loadUserProfile()
        loadRecentListings()
    }

    private fun initializeViews(view: View) {
        tvWelcome = view.findViewById(R.id.tvWelcome)
        btnSearch = view.findViewById(R.id.btnSearch)
        btnNotification = view.findViewById(R.id.btnNotification)
        btnSellItem = view.findViewById(R.id.btnSellItem)
        btnBrowseCategories = view.findViewById(R.id.btnBrowseCategories)
        tvSeeAll = view.findViewById(R.id.tvSeeAll)
        rvRecentListings = view.findViewById(R.id.rvRecentListings)
        layoutLoading = view.findViewById(R.id.layoutLoading)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)
        layoutComingSoon = view.findViewById(R.id.layoutComingSoon)
    }

    private fun setupUI() {
        // Setup RecyclerView
        listingAdapter = HorizontalListingAdapter(recentListings) { listing ->
            // Handle item click
            // Navigate to listing details
        }
        rvRecentListings.adapter = listingAdapter
        rvRecentListings.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.HORIZONTAL,
            false
        )

        // Setup click listeners
        btnSearch.setOnClickListener {
            startActivity(Intent(context, SearchActivity::class.java))
        }

        btnSellItem.setOnClickListener {
            startActivity(Intent(context, CreateListingActivity::class.java))
        }

        btnBrowseCategories.setOnClickListener {
            startActivity(Intent(context, SearchActivity::class.java))
        }

        tvSeeAll.setOnClickListener {
            startActivity(Intent(context, SearchActivity::class.java))
        }

        // Show coming soon announcement
        showComingSoonAnnouncement()
    }

    private fun showComingSoonAnnouncement() {
        layoutComingSoon.visibility = View.VISIBLE
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Fixed callback with explicit type declaration
            UserRepository.getUserProfile(currentUser.uid) { success, userData ->
                if (success && userData != null) {
                    val name = userData["name"] as? String ?: "User"
                    tvWelcome.text = "Welcome $name"
                } else {
                    // Fallback to auth display name
                    val displayName = currentUser.displayName ?: "User"
                    tvWelcome.text = "Welcome $displayName"
                }
            }
        } else {
            tvWelcome.text = "Welcome to TradeUp"
        }
    }

    private fun loadRecentListings() {
        layoutLoading.visibility = View.VISIBLE
        layoutEmpty.visibility = View.GONE

        val db = FirebaseFirestore.getInstance()
        db.collection("listings")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { documents ->
                layoutLoading.visibility = View.GONE
                recentListings.clear()

                for (document in documents) {
                    try {
                        val listing = document.toObject(Listing::class.java)
                        listing.id = document.id
                        recentListings.add(listing)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (recentListings.isEmpty()) {
                    layoutEmpty.visibility = View.VISIBLE
                } else {
                    layoutEmpty.visibility = View.GONE
                }

                listingAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                layoutLoading.visibility = View.GONE
                layoutEmpty.visibility = View.VISIBLE
                Toast.makeText(context, "Failed to load listings", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
        loadRecentListings()
    }
}