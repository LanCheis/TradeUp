package com.example.tradeup.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.tradeup.R
import com.example.tradeup.auth.LoginActivity
import com.example.tradeup.data.remote.FirebaseAuthHelper
import com.example.tradeup.listing.CreateListingActivity
import com.example.tradeup.profile.EditProfileFragment

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvWelcome = view.findViewById<TextView>(R.id.tvWelcome)
        val btnCreate = view.findViewById<Button>(R.id.btnCreateListing)
        val btnView = view.findViewById<Button>(R.id.btnViewListings)
        val btnEdit = view.findViewById<Button>(R.id.btnEditProfile)

        val userEmail = FirebaseAuthHelper.getCurrentUser()?.email ?: "Guest"
        tvWelcome.text = getString(R.string.welcome_user, userEmail)

        btnCreate.setOnClickListener {
            startActivity(Intent(requireContext(), CreateListingActivity::class.java))
        }

        btnView.setOnClickListener {
            // No navigation needed; handled by MainActivity
        }

        btnEdit.setOnClickListener {
            // No navigation needed; handled by MainActivity
        }
    }
}