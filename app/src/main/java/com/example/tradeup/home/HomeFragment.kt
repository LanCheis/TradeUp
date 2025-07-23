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
        tvWelcome = view.findViewById(R.id.tvWelcome)

        loadUserProfile()
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // ✅ FIXED: Explicitly declare callback type to resolve ambiguity
            val profileCallback: (Boolean, Map<String, Any>?) -> Unit = { success, userData ->
                if (success && userData != null) {
                    val name = userData["name"] as? String ?: "User"
                    tvWelcome.text = "Welcome $name"
                } else {
                    // Fallback to auth display name
                    val displayName = currentUser.displayName ?: "User"
                    tvWelcome.text = "Welcome $displayName"
                }
            }

            UserRepository.getUserProfile(currentUser.uid, profileCallback)
        } else {
            tvWelcome.text = "Welcome to TradeUp"
        }
    }


}