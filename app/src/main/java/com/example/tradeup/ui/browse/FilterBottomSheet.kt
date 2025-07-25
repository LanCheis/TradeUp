package com.example.tradeup.ui.browse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.example.tradeup.R
import com.example.tradeup.data.model.Categories
import com.example.tradeup.data.model.ItemCondition
import com.example.tradeup.data.remote.SortOption
import com.example.tradeup.databinding.BottomSheetFilterBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.slider.RangeSlider

// FR-3.1.1, FR-3.1.3: Comprehensive filter interface
class FilterBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetFilterBinding? = null
    private val binding get() = _binding!!

    // Current filter values
    private var selectedCategory = "All"
    private var selectedCondition = ""
    private var selectedSort = SortOption.NEWEST
    private var minPrice = 0.0
    private var maxPrice = 10000000.0 // 10M VND

    // Callback to parent
    private var onFilterApplied: ((FilterOptions) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCategorySpinner()
        setupConditionSpinner()
        setupSortSpinner()
        setupPriceSlider()
        setupButtons()
    }

    // FR-3.2.1: Category selection
    private fun setupCategorySpinner() {
        val categories = listOf("All") + Categories.ALL
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spCategory.adapter = adapter
        binding.spCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCategory = categories[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // FR-3.1.1: Item condition filter
    private fun setupConditionSpinner() {
        val conditions = listOf("All Conditions", ItemCondition.NEW, ItemCondition.LIKE_NEW,
            ItemCondition.GOOD, ItemCondition.FAIR, ItemCondition.POOR)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, conditions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spCondition.adapter = adapter
        binding.spCondition.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCondition = if (position == 0) "" else conditions[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // FR-3.1.3: Sort options
    private fun setupSortSpinner() {
        val sortOptions = SortOption.values()
        val sortNames = sortOptions.map { it.displayName }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sortNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spSort.adapter = adapter
        binding.spSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedSort = sortOptions[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // FR-3.1.1: Price range filter
    private fun setupPriceSlider() {
        binding.priceSlider.apply {
            valueFrom = 0f
            valueTo = 10000000f // 10M VND
            values = listOf(minPrice.toFloat(), maxPrice.toFloat())

            addOnSliderTouchListener(object : RangeSlider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: RangeSlider) {}
                override fun onStopTrackingTouch(slider: RangeSlider) {
                    val values = slider.values
                    minPrice = values[0].toDouble()
                    maxPrice = values[1].toDouble()
                    updatePriceLabels()
                }
            })
        }
        updatePriceLabels()
    }

    private fun updatePriceLabels() {
        binding.tvMinPrice.text = "₫${String.format("%,.0f", minPrice)}"
        binding.tvMaxPrice.text = "₫${String.format("%,.0f", maxPrice)}"
    }

    private fun setupButtons() {
        // Apply filters
        binding.btnApply.setOnClickListener {
            val filterOptions = FilterOptions(
                category = selectedCategory,
                condition = selectedCondition,
                sortOption = selectedSort,
                minPrice = minPrice,
                maxPrice = maxPrice
            )
            onFilterApplied?.invoke(filterOptions)
            dismiss()
        }

        // Clear all filters
        binding.btnClear.setOnClickListener {
            // Reset to defaults
            binding.spCategory.setSelection(0)
            binding.spCondition.setSelection(0)
            binding.spSort.setSelection(0)
            binding.priceSlider.values = listOf(0f, 10000000f)

            selectedCategory = "All"
            selectedCondition = ""
            selectedSort = SortOption.NEWEST
            minPrice = 0.0
            maxPrice = 10000000.0
            updatePriceLabels()
        }

        // Cancel
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    fun setOnFilterAppliedListener(listener: (FilterOptions) -> Unit) {
        onFilterApplied = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Data class for filter options
data class FilterOptions(
    val category: String,
    val condition: String,
    val sortOption: SortOption,
    val minPrice: Double,
    val maxPrice: Double
)