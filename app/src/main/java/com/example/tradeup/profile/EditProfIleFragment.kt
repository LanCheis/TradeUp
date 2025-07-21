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

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
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

        // Show date picker
        binding.etBirthday.setOnClickListener {
            showDatePicker()
        }

        // Save profile button
        binding.btnSave.setOnClickListener {
            binding.etUsername.text.toString()
            binding.etPhone.text.toString()
            binding.etBio.text.toString()
            binding.etInterest.text.toString()
            binding.etBirthday.text.toString()

            // TODO: Save this data to Firebase or local storage
            // You could use a ViewModel, repository, or directly call Firebase here.
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                binding.etBirthday.setText(
                    String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
                )
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
