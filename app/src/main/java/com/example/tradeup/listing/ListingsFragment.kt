package com.example.tradeup.listing

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.tradeup.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth

class ListingsFragment : Fragment() {

    private lateinit var fabAddListing: FloatingActionButton
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_listings_main, container, false)

        auth = FirebaseAuth.getInstance()

        initViews(view)
        setupClickListeners()

        return view
    }

    private fun initViews(view: View) {
        fabAddListing = view.findViewById(R.id.fabAddListing)
    }

    private fun setupClickListeners() {
        fabAddListing.setOnClickListener {
            // Check if user is logged in
            if (auth.currentUser != null) {
                val intent = Intent(requireContext(), AddListingActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show()
            }
        }
    }
}