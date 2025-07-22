package com.example.tradeup.listing

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tradeup.R
import com.example.tradeup.data.model.Listing
import com.example.tradeup.search.SearchActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ListingsFragment : Fragment() {

    private lateinit var rvCategories: RecyclerView
    private lateinit var rvListings: RecyclerView
    private lateinit var btnSearch: MaterialButton
    private val listingList = mutableListOf<Listing>()

    // Categories data
    private val categories = listOf(
        CategoryData("📱", "Electronics", "Đồ điện tử"),
        CategoryData("👕", "Fashion", "Thời trang"),
        CategoryData("🏠", "Home & Garden", "Đồ gia dụng"),
        CategoryData("🚗", "Vehicles", "Xe cộ"),
        CategoryData("📚", "Books", "Sách"),
        CategoryData("🎮", "Gaming", "Game"),
        CategoryData("⚽", "Sports", "Thể thao"),
        CategoryData("🎵", "Music", "Âm nhạc")
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_listings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvCategories = view.findViewById(R.id.rvCategories)
        rvListings = view.findViewById(R.id.rvListings)
        btnSearch = view.findViewById(R.id.btnSearch)

        setupCategoriesRecyclerView()
        setupListingsRecyclerView()
        setupSearchButton()
        loadListings()
    }

    private fun setupCategoriesRecyclerView() {
        val categoryAdapter = SimpleCategoryAdapter(categories) { category ->
            // Navigate to search with selected category
            val intent = Intent(requireContext(), SearchActivity::class.java).apply {
                putExtra("category", category.apiName)
            }
            startActivity(intent)
        }

        rvCategories.layoutManager = GridLayoutManager(requireContext(), 4)
        rvCategories.adapter = categoryAdapter
    }

    private fun setupListingsRecyclerView() {
        rvListings.layoutManager = LinearLayoutManager(requireContext())
        rvListings.adapter = ListingAdapter(listingList)
    }

    private fun setupSearchButton() {
        btnSearch.setOnClickListener {
            startActivity(Intent(requireContext(), SearchActivity::class.java))
        }
    }

    private fun loadListings() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid

        FirebaseFirestore.getInstance()
            .collection("listings")
            .get()
            .addOnSuccessListener { result ->
                listingList.clear()
                for (doc in result) {
                    val item = doc.toObject(Listing::class.java)
                    if (item.ownerUid != currentUid) {
                        listingList.add(item)
                    }
                }
                rvListings.adapter?.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "❌ Không tải được danh sách", Toast.LENGTH_SHORT).show()
            }
    }

    data class CategoryData(
        val icon: String,
        val name: String,
        val apiName: String
    )
}