package com.example.tradeup.offers

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tradeup.R
import com.example.tradeup.data.model.Offer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class OffersActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var tabReceived: TextView
    private lateinit var tabSent: TextView
    private lateinit var rvOffers: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var progressBar: ProgressBar

    private val receivedOffers = mutableListOf<Offer>()
    private val sentOffers = mutableListOf<Offer>()
    private lateinit var offersAdapter: OffersAdapter

    private var currentTab = "received" // "received" or "sent"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offers)

        initViews()
        setupClickListeners()
        setupRecyclerView()
        loadOffers()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tabReceived = findViewById(R.id.tabReceived)
        tabSent = findViewById(R.id.tabSent)
        rvOffers = findViewById(R.id.rvOffers)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        progressBar = findViewById(R.id.progressBar)

        // Set initial tab
        updateTabSelection()
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }

        tabReceived.setOnClickListener {
            currentTab = "received"
            updateTabSelection()
            updateRecyclerView()
        }

        tabSent.setOnClickListener {
            currentTab = "sent"
            updateTabSelection()
            updateRecyclerView()
        }
    }

    private fun setupRecyclerView() {
        offersAdapter = OffersAdapter(
            offers = if (currentTab == "received") receivedOffers else sentOffers,
            isReceivedTab = currentTab == "received",
            onAcceptOffer = { offer -> acceptOffer(offer) },
            onRejectOffer = { offer -> rejectOffer(offer) },
            onWithdrawOffer = { offer -> withdrawOffer(offer) }
        )
        rvOffers.layoutManager = LinearLayoutManager(this)
        rvOffers.adapter = offersAdapter
    }

    private fun updateTabSelection() {
        if (currentTab == "received") {
            tabReceived.setBackgroundResource(R.drawable.edittext_filled_background)
            tabReceived.setTextColor(getColor(R.color.primary))
            tabSent.setBackgroundResource(R.drawable.edittext_background)
            tabSent.setTextColor(getColor(R.color.text_secondary))
        } else {
            tabSent.setBackgroundResource(R.drawable.edittext_filled_background)
            tabSent.setTextColor(getColor(R.color.primary))
            tabReceived.setBackgroundResource(R.drawable.edittext_background)
            tabReceived.setTextColor(getColor(R.color.text_secondary))
        }
    }

    private fun updateRecyclerView() {
        offersAdapter = OffersAdapter(
            offers = if (currentTab == "received") receivedOffers else sentOffers,
            isReceivedTab = currentTab == "received",
            onAcceptOffer = { offer -> acceptOffer(offer) },
            onRejectOffer = { offer -> rejectOffer(offer) },
            onWithdrawOffer = { offer -> withdrawOffer(offer) }
        )
        rvOffers.adapter = offersAdapter

        val isEmpty = if (currentTab == "received") receivedOffers.isEmpty() else sentOffers.isEmpty()
        layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun loadOffers() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        showLoading(true)

        // Load received offers (where user is seller)
        FirebaseFirestore.getInstance()
            .collection("offers")
            .whereEqualTo("sellerId", currentUser.uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                receivedOffers.clear()
                for (doc in documents) {
                    val offer = doc.toObject(Offer::class.java)
                    receivedOffers.add(offer)
                }
                loadSentOffers(currentUser.uid)
            }
            .addOnFailureListener {
                showLoading(false)
                Toast.makeText(this, "Failed to load offers", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadSentOffers(userId: String) {
        // Load sent offers (where user is buyer)
        FirebaseFirestore.getInstance()
            .collection("offers")
            .whereEqualTo("buyerId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                sentOffers.clear()
                for (doc in documents) {
                    val offer = doc.toObject(Offer::class.java)
                    sentOffers.add(offer)
                }
                showLoading(false)
                updateRecyclerView()
            }
            .addOnFailureListener {
                showLoading(false)
                Toast.makeText(this, "Failed to load sent offers", Toast.LENGTH_SHORT).show()
            }
    }

    private fun acceptOffer(offer: Offer) {
        updateOfferStatus(offer, "accepted") {
            Toast.makeText(this, "✅ Offer accepted!", Toast.LENGTH_SHORT).show()
            loadOffers()
        }
    }

    private fun rejectOffer(offer: Offer) {
        updateOfferStatus(offer, "rejected") {
            Toast.makeText(this, "❌ Offer rejected", Toast.LENGTH_SHORT).show()
            loadOffers()
        }
    }

    private fun withdrawOffer(offer: Offer) {
        updateOfferStatus(offer, "withdrawn") {
            Toast.makeText(this, "🔄 Offer withdrawn", Toast.LENGTH_SHORT).show()
            loadOffers()
        }
    }

    private fun updateOfferStatus(offer: Offer, newStatus: String, onSuccess: () -> Unit) {
        val updatedOffer = offer.copy(
            status = newStatus,
            updatedAt = System.currentTimeMillis()
        )

        FirebaseFirestore.getInstance()
            .collection("offers")
            .document(offer.id)
            .set(updatedOffer)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update offer", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        rvOffers.visibility = if (show) View.GONE else View.VISIBLE
    }
}