package com.example.tradeup.browse

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tradeup.R
import com.example.tradeup.search.SearchActivity

class CategoryBrowseFragment : Fragment() {

    private lateinit var rvCategories: RecyclerView
    private val categories = listOf(
        CategoryItem("📱", "Electronics", "Đồ điện tử"),
        CategoryItem("👕", "Fashion", "Thời trang"),
        CategoryItem("🏠", "Home & Garden", "Đồ gia dụng"),
        CategoryItem("🚗", "Vehicles", "Xe cộ"),
        CategoryItem("📚", "Books", "Sách"),
        CategoryItem("🎮", "Gaming", "Game"),
        CategoryItem("⚽", "Sports", "Thể thao"),
        CategoryItem("🎵", "Music", "Âm nhạc"),
        CategoryItem("🔧", "Tools", "Dụng cụ"),
        CategoryItem("❓", "Others", "Khác")
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_category_browse, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvCategories = view.findViewById(R.id.rvCategories)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val adapter = CategoryAdapter(categories) { category ->
            // Navigate to search with selected category
            val intent = Intent(requireContext(), SearchActivity::class.java).apply {
                putExtra("category", category.apiName)
            }
            startActivity(intent)
        }

        rvCategories.layoutManager = GridLayoutManager(requireContext(), 2)
        rvCategories.adapter = adapter
    }
}