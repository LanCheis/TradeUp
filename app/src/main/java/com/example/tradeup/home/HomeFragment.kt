package com.example.tradeup.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.tradeup.R
import com.example.tradeup.listing.CreateListingActivity

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnAddListing = view.findViewById<Button>(R.id.btnAddListing)
        btnAddListing.setOnClickListener {
            // Navigate to CreateListingActivity
            startActivity(Intent(requireContext(), CreateListingActivity::class.java))
        }
    }
}