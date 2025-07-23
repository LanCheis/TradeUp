package com.example.tradeup.offers

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tradeup.R
import com.example.tradeup.data.model.Offer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class OffersActivity : AppCompatActivity() {

    private lateinit var rvOffers: RecyclerView
    private lateinit var offersAdapter: OffersAdapter
    private val offers = mutableListOf<Offer>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offers)

        initViews()
        setupRecyclerView()
        loadOffers()
    }

    private fun initViews() {
        rvOffers = findViewById(R.id.rvOffers)
    }

    private fun setupRecyclerView() {
        offersAdapter = OffersAdapter(offers) { offer ->
            handleOfferAction(offer)
        }

        rvOffers.layoutManager = LinearLayoutManager(this)
        rvOffers.adapter = offersAdapter
    }

    private fun loadOffers() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        // Load offers where current user is either buyer or seller
        FirebaseFirestore.getInstance()
            .collection("offers")
            .whereIn("buyerId", listOf(currentUser.uid))
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error loading offers: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                offers.clear()
                snapshot?.documents?.forEach { doc ->
                    val offer = doc.toObject(Offer::class.java)
                    offer?.let {
                        offers.add(it.copy(id = doc.id))
                    }
                }

                // Also load offers where current user is seller
                loadSellerOffers(currentUser.uid)
            }
    }

    private fun loadSellerOffers(userId: String) {
        FirebaseFirestore.getInstance()
            .collection("offers")
            .whereEqualTo("sellerId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.forEach { doc ->
                    val offer = doc.toObject(Offer::class.java)
                    offer?.let {
                        // Avoid duplicates
                        val existingOffer = offers.find { existing -> existing.id == doc.id }
                        if (existingOffer == null) {
                            offers.add(it.copy(id = doc.id))
                        }
                    }
                }

                // Sort by creation date
                offers.sortByDescending { it.createdAt }
                offersAdapter.notifyDataSetChanged()
            }
    }

    private fun handleOfferAction(offer: Offer) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        if (offer.sellerId == currentUser.uid && offer.status == "pending") {
            // Seller can accept/reject/counter
            showSellerActions(offer)
        }
    }

    private fun showSellerActions(offer: Offer) {
        val options = arrayOf("Accept Offer", "Reject Offer", "Counter Offer")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Offer from ${offer.buyerName}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> acceptOffer(offer)
                    1 -> rejectOffer(offer)
                    2 -> counterOffer(offer)
                }
            }
            .show()
    }

    private fun acceptOffer(offer: Offer) {
        updateOfferStatus(offer.id, "accepted")
    }

    private fun rejectOffer(offer: Offer) {
        updateOfferStatus(offer.id, "rejected")
    }

    private fun counterOffer(offer: Offer) {
        // TODO: Implement counter offer dialog
        Toast.makeText(this, "Counter offer feature coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun updateOfferStatus(offerId: String, status: String) {
        FirebaseFirestore.getInstance()
            .collection("offers")
            .document(offerId)
            .update("status", status, "updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp())
            .addOnSuccessListener {
                Toast.makeText(this, "Offer $status successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update offer: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}