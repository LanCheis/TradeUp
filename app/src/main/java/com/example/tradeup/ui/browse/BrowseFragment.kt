package com.example.tradeup.ui.browse

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.tradeup.R
import com.example.tradeup.databinding.FragmentBrowseBinding
import com.example.tradeup.data.model.Category
import com.example.tradeup.data.remote.SortOption
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BrowseFragment : Fragment() {

    private var _binding: FragmentBrowseBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BrowseViewModel by viewModels()
    private lateinit var listingsAdapter: ListingsAdapter

    private var searchJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBrowseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchBar()
        setupCategoryChips()
        setupSortSpinner()
        observeViewModel()

        // Load initial data
        viewModel.loadListings()
    }

    // FR-3.1.1: Setup search functionality
    private fun setupSearchBar() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            // FR-3.1.2: Search triggers 200ms after typing
            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(200) // 200ms delay as per requirement
                    viewModel.searchListings(s.toString())
                }
            }
        })

        // Filter button
        binding.filterButton.setOnClickListener {
            showFilterDialog()
        }
    }

    // ✅ NEW: Method to focus search bar when "Search" tab is tapped
    fun focusSearchBar() {
        // Only focus if binding exists and fragment is visible
        if (_binding != null && isVisible) {
            binding.searchEditText.post {
                binding.searchEditText.requestFocus()
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(binding.searchEditText, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    // FR-3.2.1: Setup category chips
    private fun setupCategoryChips() {
        // Use Categories from your model instead of Category enum
        val categories = listOf("All", "Electronics", "Clothing", "Home & Garden", "Sports", "Books", "Automotive", "Toys", "Other")

        categories.forEach { categoryName ->
            val chip = Chip(requireContext())
            chip.text = categoryName
            chip.isCheckable = true
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    // Uncheck other chips
                    for (i in 0 until binding.categoryChipGroup.childCount) {
                        val otherChip = binding.categoryChipGroup.getChildAt(i) as Chip
                        if (otherChip != chip) {
                            otherChip.isChecked = false
                        }
                    }
                    viewModel.filterByCategory(categoryName)
                } else if (binding.categoryChipGroup.checkedChipId == View.NO_ID) {
                    // If no chips are checked, show all
                    viewModel.filterByCategory("All")
                }
            }
            binding.categoryChipGroup.addView(chip)
        }

        // Select "All" by default
        (binding.categoryChipGroup.getChildAt(0) as Chip).isChecked = true
    }

    // FR-3.1.3: Setup sort spinner
    private fun setupSortSpinner() {
        binding.sortButton.setOnClickListener {
            showSortDialog()
        }
    }

    private fun setupRecyclerView() {
        listingsAdapter = ListingsAdapter { listing ->
            // FR-2.2.3: Increment views when item is clicked
            viewModel.incrementViews(listing.id)

            // TODO: Navigate to item detail screen
            // For now, show a toast with item info
            android.widget.Toast.makeText(
                requireContext(),
                "Clicked: ${listing.title} - $${listing.price}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = listingsAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.listings.observe(viewLifecycleOwner) { listings ->
            listingsAdapter.submitList(listings)
            binding.emptyStateText.visibility = if (listings.isEmpty()) View.VISIBLE else View.GONE

            // Update search results count
            updateResultsCount(listings.size)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                android.widget.Toast.makeText(requireContext(), error, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateResultsCount(count: Int) {
        // You can add a results count TextView to your layout if needed
        // For now, we'll update the toolbar subtitle through the activity
        activity?.title = if (count > 0) "Browse Items ($count)" else "Browse Items"
    }

    private fun showFilterDialog() {
        // Simple filter dialog - works with existing code
        val conditions = arrayOf("All Conditions", "New", "Like New", "Good", "Fair", "Poor")

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("🔍 Filter by Condition")
            .setItems(conditions) { _, which ->
                val condition = if (which == 0) "" else conditions[which]
                viewModel.applyConditionFilter(condition)

                // Update filter button text
                binding.filterButton.text = if (condition.isEmpty()) "🔍 Filter" else "🔍 Filter: $condition"
            }
            .show()
    }

    private fun updateFilterButtonText(filterOptions: FilterOptions) {
        val activeFilters = mutableListOf<String>()

        if (filterOptions.category != "All") {
            activeFilters.add(filterOptions.category)
        }
        if (filterOptions.condition.isNotEmpty()) {
            activeFilters.add(filterOptions.condition)
        }
        if (filterOptions.minPrice > 0 || filterOptions.maxPrice < 10000000) {
            activeFilters.add("Price")
        }

        val filterText = if (activeFilters.isEmpty()) {
            "🔍 Filter"
        } else {
            "🔍 Filter (${activeFilters.size})"
        }

        binding.filterButton.text = filterText
        binding.sortButton.text = "📊 ${filterOptions.sortOption.displayName}"
    }



    private fun showSortDialog() {
        val sortOptions = SortOption.values()
        val items = sortOptions.map { it.displayName }.toTypedArray()

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("📊 Sort by")
            .setItems(items) { _, which ->
                viewModel.sortListings(sortOptions[which])
                binding.sortButton.text = "📊 ${sortOptions[which].displayName}"
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        _binding = null
    }
}