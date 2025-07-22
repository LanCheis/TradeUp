package com.example.tradeup.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.example.tradeup.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.slider.RangeSlider

class SearchFiltersBottomSheet : BottomSheetDialogFragment() {

    private lateinit var spCondition: Spinner
    private lateinit var rangePriceSlider: RangeSlider
    private lateinit var sliderDistance: SeekBar
    private lateinit var tvMinPrice: TextView
    private lateinit var tvMaxPrice: TextView
    private lateinit var tvDistance: TextView
    private lateinit var btnApply: Button
    private lateinit var btnReset: Button

    private var onFiltersAppliedListener: ((String, Double, Double, Double) -> Unit)? = null

    companion object {
        fun newInstance(
            selectedCondition: String,
            minPrice: Double,
            maxPrice: Double,
            maxDistance: Double
        ): SearchFiltersBottomSheet {
            val fragment = SearchFiltersBottomSheet()
            val args = Bundle().apply {
                putString("condition", selectedCondition)
                putDouble("minPrice", minPrice)
                putDouble("maxPrice", maxPrice)
                putDouble("maxDistance", maxDistance)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_search_filters, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupConditionSpinner()
        setupPriceSlider()
        setupDistanceSlider()
        setupClickListeners()
        loadSavedValues()
    }

    private fun initViews(view: View) {
        spCondition = view.findViewById(R.id.spCondition)
        rangePriceSlider = view.findViewById(R.id.rangePriceSlider)
        sliderDistance = view.findViewById(R.id.sliderDistance)
        tvMinPrice = view.findViewById(R.id.tvMinPrice)
        tvMaxPrice = view.findViewById(R.id.tvMaxPrice)
        tvDistance = view.findViewById(R.id.tvDistance)
        btnApply = view.findViewById(R.id.btnApply)
        btnReset = view.findViewById(R.id.btnReset)
    }

    private fun setupConditionSpinner() {
        val conditions = arrayOf("All", "New", "Like New", "Good", "Fair", "Poor")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, conditions)
        spCondition.adapter = adapter
    }

    private fun setupPriceSlider() {
        rangePriceSlider.valueFrom = 0f
        rangePriceSlider.valueTo = 100000000f // 100 million VND
        rangePriceSlider.stepSize = 100000f // 100k VND steps

        rangePriceSlider.addOnChangeListener { _, _, _ ->
            updatePriceLabels()
        }
    }

    private fun setupDistanceSlider() {
        sliderDistance.max = 100 // 100km max
        sliderDistance.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvDistance.text = "$progress km"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupClickListeners() {
        btnApply.setOnClickListener {
            val condition = spCondition.selectedItem.toString()
            val priceValues = rangePriceSlider.values
            val minPrice = priceValues[0].toDouble()
            val maxPrice = priceValues[1].toDouble()
            val distance = sliderDistance.progress.toDouble()

            onFiltersAppliedListener?.invoke(condition, minPrice, maxPrice, distance)
            dismiss()
        }

        btnReset.setOnClickListener {
            resetFilters()
        }
    }

    private fun loadSavedValues() {
        arguments?.let { args ->
            val condition = args.getString("condition", "All")
            val minPrice = args.getDouble("minPrice", 0.0)
            val maxPrice = args.getDouble("maxPrice", 100000000.0)
            val distance = args.getDouble("maxDistance", 50.0)

            // Set condition spinner
            val conditionAdapter = spCondition.adapter as ArrayAdapter<String>
            val conditionPosition = conditionAdapter.getPosition(condition)
            spCondition.setSelection(conditionPosition)

            // Set price slider
            rangePriceSlider.values = listOf(minPrice.toFloat(), maxPrice.toFloat())
            updatePriceLabels()

            // Set distance slider
            sliderDistance.progress = distance.toInt()
            tvDistance.text = "${distance.toInt()} km"
        }
    }

    private fun updatePriceLabels() {
        val values = rangePriceSlider.values
        tvMinPrice.text = "${values[0].toInt().formatPrice()} ₫"
        tvMaxPrice.text = "${values[1].toInt().formatPrice()} ₫"
    }

    private fun resetFilters() {
        spCondition.setSelection(0)
        rangePriceSlider.values = listOf(0f, 100000000f)
        sliderDistance.progress = 50
        updatePriceLabels()
        tvDistance.text = "50 km"
    }

    fun setOnFiltersAppliedListener(listener: (String, Double, Double, Double) -> Unit) {
        onFiltersAppliedListener = listener
    }

    private fun Int.formatPrice(): String {
        return when {
            this >= 1000000 -> "${this / 1000000}M"
            this >= 1000 -> "${this / 1000}K"
            else -> toString()
        }
    }
}