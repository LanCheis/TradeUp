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
import com.example.tradeup.search.SearchActivity // Add this import
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

    private val recentListings = mutableListOf<Listing>()
    private lateinit var listingAdapter: HorizontalListingAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupGreeting()
        setupClickListeners()
        setupRecyclerView()
        loadUserName()
        loadRecentListings()
    }

    private fun initViews(view: View) {
        tvGreeting = view.findViewById(R.id.tvGreeting)
        tvUserName = view.findViewById(R.id.tvUserName)
        btnSearch = view.findViewById(R.id.btnSearch)
        btnNotification = view.findViewById(R.id.btnNotification)
        btnSettings = view.findViewById(R.id.btnSettings)
        btnSellItem = view.findViewById(R.id.btnSellItem)
        btnBrowseCategories = view.findViewById(R.id.btnBrowseCategories)
        tvSeeAll = view.findViewById(R.id.tvSeeAll)
        rvRecentListings = view.findViewById(R.id.rvRecentListings)
        layoutLoading = view.findViewById(R.id.layoutLoading)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)
    }

    private fun setupGreeting() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        val greeting = when (hour) {
            in 6..11 -> "Good Morning! ☀️"
            in 12..17 -> "Good Afternoon! 🌤️"
            in 18..23 -> "Good Evening! 🌆"
            else -> "Good Night! 🌙"
        }

        tvGreeting.text = greeting
    }

    private fun setupClickListeners() {
        btnSearch.setOnClickListener {
            // Fixed: Add proper Intent creation
            val intent = Intent(requireContext(), SearchActivity::class.java)
            startActivity(intent)
        }

        btnNotification.setOnClickListener {
            Toast.makeText(requireContext(), "🔔 No new notifications", Toast.LENGTH_SHORT).show()
        }

        btnSettings.setOnClickListener {
            Toast.makeText(requireContext(), "⚙️ Settings coming soon!", Toast.LENGTH_SHORT).show()
        }

        btnSellItem.setOnClickListener {
            startActivity(Intent(requireContext(), CreateListingActivity::class.java))
        }

        btnBrowseCategories.setOnClickListener {
            // Navigate to library tab
            (requireActivity() as? com.example.tradeup.MainActivity)?.navigateTo(R.id.menu_library)
        }

        tvSeeAll.setOnClickListener {
            // Navigate to library tab to see all listings
            (requireActivity() as? com.example.tradeup.MainActivity)?.navigateTo(R.id.menu_library)
        }
    }

    private fun setupRecyclerView() {
        listingAdapter = HorizontalListingAdapter(recentListings)
        rvRecentListings.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        rvRecentListings.adapter = listingAdapter
    }

    private fun loadUserName() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            UserRepository.getUserProfile(currentUser.uid) { user ->
                if (user != null && isAdded) {
                    val firstName = user.name.split(" ").firstOrNull() ?: "User"
                    tvUserName.text = "Welcome back, $firstName!"
                } else if (isAdded) {
                    tvUserName.text = "Welcome back!"
                }
            }
        } else {
            tvUserName.text = "Welcome to TradeUp!"
        }
    }

    private fun loadRecentListings() {
        showLoading(true)

        FirebaseFirestore.getInstance()
            .collection("listings")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { result ->
                if (isAdded) {
                    recentListings.clear()

                    for (document in result) {
                        val listing = document.toObject(Listing::class.java)
                        recentListings.add(listing)
                    }

                    listingAdapter.notifyDataSetChanged()
                    showLoading(false)

                    if (recentListings.isEmpty()) {
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

    private fun showLoading(show: Boolean) {
        layoutLoading.visibility = if (show) View.VISIBLE else View.GONE
        rvRecentListings.visibility = if (show) View.GONE else View.VISIBLE
        layoutEmpty.visibility = View.GONE
    }

    private fun showEmpty(show: Boolean) {
        layoutEmpty.visibility = if (show) View.VISIBLE else View.GONE
        rvRecentListings.visibility = if (show) View.GONE else View.VISIBLE
        layoutLoading.visibility = View.GONE
    }
}