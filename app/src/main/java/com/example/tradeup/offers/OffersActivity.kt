package com.example.tradeup.offers

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tradeup.R
import com.example.tradeup.data.model.Offer
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth

class OffersActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var rvOffers: RecyclerView
    private lateinit var layoutEmpty: View
    private lateinit var layoutLoading: View

    private val receivedOffers = mutableListOf<Offer>()
    private val sentOffers = mutableListOf<Offer>()
    private lateinit var offersAdapter: OffersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offers)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Offers"

        initViews()
        setupTabs()
        setupRecyclerView()
        loadOffers()
    }

    private fun initViews() {
        tabLayout = findViewById(R.id.tabLayout)
        rvOffers = findViewById(R.id.rvOffers)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        layoutLoading = findViewById(R.id.layoutLoading)
    }

    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Received"))
        tabLayout.addTab(tabLayout.newTab().setText("Sent"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showReceivedOffers()
                    1 -> showSentOffers()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupRecyclerView() {
        offersAdapter = OffersAdapter(emptyList()) { offer, action ->
            handleOfferAction(offer, action)
        }
        rvOffers.layoutManager = LinearLayoutManager(this)
        rvOffers.adapter = offersAdapter
    }

    private fun loadOffers() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        showLoading(true)

        // Load received offers (where user is seller)
        OfferRepository.getOffersBySeller(currentUserId) { offers ->
            receivedOffers.clear()
            receivedOffers.addAll(offers)

            // Load sent offers (where user is buyer)
            OfferRepository.getOffersByBuyer(currentUserId) { sentOffers ->
                this.sentOffers.clear()
                this.sentOffers.addAll(sentOffers)

                showLoading(false)
                showReceivedOffers() // Default to received offers
            }
        }
    }

    private fun showReceivedOffers() {
        offersAdapter.updateOffers(receivedOffers, "received")
        showEmptyState(receivedOffers.isEmpty())
    }

    private fun showSentOffers() {
        offersAdapter.updateOffers(sentOffers, "sent")
        showEmptyState(sentOffers.isEmpty())
    }

    private fun handleOfferAction(offer: Offer, action: String) {
        when (action) {
            "accept" -> {
                OfferRepository.acceptOffer(offer.id) { success, error ->
                    if (success) {
                        loadOffers() // Refresh
                    } else {
                        // Show error
                    }
                }
            }
            "reject" -> {
                OfferRepository.rejectOffer(offer.id) { success, error ->
                    if (success) {
                        loadOffers() // Refresh
                    } else {
                        // Show error
                    }
                }
            }
            "counter" -> {
                // Show counter offer dialog
                showCounterOfferDialog(offer)
            }
        }
    }

    private fun showCounterOfferDialog(offer: Offer) {
        // Implementation for counter offer dialog
        // This would show a dialog to enter counter offer amount and message
    }

    private fun showLoading(show: Boolean) {
        layoutLoading.visibility = if (show) View.VISIBLE else View.GONE
        rvOffers.visibility = if (show) View.GONE else View.VISIBLE
        layoutEmpty.visibility = View.GONE
    }

    private fun showEmptyState(show: Boolean) {
        layoutEmpty.visibility = if (show) View.VISIBLE else View.GONE
        rvOffers.visibility = if (show) View.GONE else View.VISIBLE
        layoutLoading.visibility = View.GONE
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}