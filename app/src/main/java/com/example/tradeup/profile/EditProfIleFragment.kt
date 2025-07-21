package com.example.tradeup.profile

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.tradeup.databinding.FragmentEditProfileBinding
import java.util.Calendar

class EditProfileFragment : Fragment() {

    // ViewBinding reference
    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
    }

    private fun setupListeners() {
        // Navigate back
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Show date picker when clicking on any date field
        binding.etDay.setOnClickListener { showDatePicker() }
        binding.etMonth.setOnClickListener { showDatePicker() }
        binding.etYear.setOnClickListener { showDatePicker() }

        // Save profile (stub)
        binding.btnSave.setOnClickListener {
            val day = binding.etDay.text.toString()
            val month = binding.etMonth.text.toString()
            val year = binding.etYear.text.toString()
            val birthday = "$day/$month/$year"
            // TODO: Use birthday string for saving
        }
    }


    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                binding.etDay.setText(String.format("%02d", dayOfMonth))
                binding.etMonth.setText(String.format("%02d", month + 1))
                binding.etYear.setText(String.format("%04d", year))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
