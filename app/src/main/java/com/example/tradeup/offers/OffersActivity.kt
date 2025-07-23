// app/src/main/java/com/example/tradeup/offer/OffersActivity.kt

package com.example.tradeup.offer

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tradeup.R
import com.example.tradeup.data.model.Offer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class OffersActivity : AppCompatActivity() {

    private lateinit var tabReceived: TextView
    private lateinit var tabSent: TextView
    private lateinit var rvOffers: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var progressBar: ProgressBar

    private val receivedOffers = mutableListOf<Offer>()
    private val sentOffers = mutableListOf<Offer>()
    private lateinit var offersAdapter: OffersAdapter
    private var currentTab = "received" // received or sent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offers)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Offers"

        initViews()
        setupTabs()
        setupRecyclerView()
        loadOffers()
    }

    private fun initViews() {
        tabReceived = findViewById(R.id.tabReceived)
        tabSent = findViewById(R.id.tabSent)
        rvOffers = findViewById(R.id.rvOffers)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupTabs() {
        tabReceived.setOnClickListener {
            switchTab("received")
        }

        tabSent.setOnClickListener {
            switchTab("sent")
        }

        // Set initial tab
        switchTab("received")
    }

    private fun switchTab(tab: String) {
        currentTab = tab

        // Update tab appearance
        if (tab == "received") {
            tabReceived.setBackgroundResource(R.drawable.tab_selected_background)
            tabReceived.setTextColor(resources.getColor(R.color.white, null))
            tabSent.setBackgroundResource(R.drawable.tab_unselected_background)
            tabSent.setTextColor(resources.getColor(R.color.text_primary, null))
        } else {
            tabSent.setBackgroundResource(R.drawable.tab_selected_background)
            tabSent.setTextColor(resources.getColor(R.color.white, null))
            tabReceived.setBackgroundResource(R.drawable.tab_unselected_background)
            tabReceived.setTextColor(resources.getColor(R.color.text_primary, null))
        }

        // Update adapter data
        offersAdapter.updateOffers(if (tab == "received") receivedOffers else sentOffers, tab)
        updateEmptyState()
    }

    private fun setupRecyclerView() {
        offersAdapter = OffersAdapter(receivedOffers, "received") { offer, action ->
            handleOfferAction(offer, action)
        }
        rvOffers.layoutManager = LinearLayoutManager(this)
        rvOffers.adapter = offersAdapter
    }

    private fun loadOffers() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        showLoading(true)

        // Load received offers (offers on my listings)
        FirebaseFirestore.getInstance()
            .collection("offers")
            .whereEqualTo("sellerId", currentUserId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) return@addSnapshotListener

                receivedOffers.clear()
                snapshots?.forEach { doc ->
                    val offer = doc.toObject(Offer::class.java)
                    receivedOffers.add(offer)
                }

                if (currentTab == "received") {
                    offersAdapter.updateOffers(receivedOffers, "received")
                    updateEmptyState()
                }
            }

        // Load sent offers (offers I made)
        FirebaseFirestore.getInstance()
            .collection("offers")
            .whereEqualTo("buyerId", currentUserId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) return@addSnapshotListener

                sentOffers.clear()
                snapshots?.forEach { doc ->
                    val offer = doc.toObject(Offer::class.java)
                    sentOffers.add(offer)
                }

                if (currentTab == "sent") {
                    offersAdapter.updateOffers(sentOffers, "sent")
                    updateEmptyState()
                }

                showLoading(false)
            }
    }

    private fun handleOfferAction(offer: Offer, action: String) {
        when (action) {
            "accept" -> acceptOffer(offer)
            "reject" -> rejectOffer(offer)
            "counter" -> showCounterOfferDialog(offer)
            "view" -> viewOfferDetails(offer)
        }
    }

    private fun acceptOffer(offer: Offer) {
        AlertDialog.Builder(this)
            .setTitle("Accept Offer")
            .setMessage("Accept ${offer.buyerName}'s offer of ${formatPrice(offer.offeredPrice)}?")
            .setPositiveButton("Accept") { _, _ ->
                updateOfferStatus(offer, "accepted")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun rejectOffer(offer: Offer) {
        AlertDialog.Builder(this)
            .setTitle("Reject Offer")
            .setMessage("Reject ${offer.buyerName}'s offer?")
            .setPositiveButton("Reject") { _, _ ->
                updateOfferStatus(offer, "rejected")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCounterOfferDialog(offer: Offer) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_counter_offer, null)
        val etCounterPrice = dialogView.findViewById<EditText>(R.id.etCounterPrice)
        val etCounterMessage = dialogView.findViewById<EditText>(R.id.etCounterMessage)

        AlertDialog.Builder(this)
            .setTitle("Counter Offer")
            .setView(dialogView)
            .setPositiveButton("Send Counter") { _, _ ->
                val counterPrice = etCounterPrice.text.toString().toDoubleOrNull()
                val counterMessage = etCounterMessage.text.toString()

                if (counterPrice != null && counterPrice > 0) {
                    sendCounterOffer(offer, counterPrice, counterMessage)
                } else {
                    Toast.makeText(this, "Please enter a valid price", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendCounterOffer(offer: Offer, counterPrice: Double, counterMessage: String) {
        val updatedOffer = offer.copy(
            status = "countered",
            counterOffer = counterPrice,
            counterMessage = counterMessage,
            updatedAt = System.currentTimeMillis()
        )

        updateOfferStatus(updatedOffer, "countered")
    }

    private fun updateOfferStatus(offer: Offer, newStatus: String) {
        val updatedOffer = offer.copy(
            status = newStatus,
            updatedAt = System.currentTimeMillis()
        )

        FirebaseFirestore.getInstance()
            .collection("offers")
            .document(offer.id)
            .set(updatedOffer)
            .addOnSuccessListener {
                Toast.makeText(this, "Offer ${newStatus}!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update offer", Toast.LENGTH_SHORT).show()
            }
    }

    private fun viewOfferDetails(offer: Offer) {
        // Implement detailed offer view if needed
        Toast.makeText(this, "Offer details coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun formatPrice(price: Double): String {
        return "${java.text.NumberFormat.getNumberInstance(java.util.Locale("vi", "VN")).format(price)} ₫"
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun updateEmptyState() {
        val currentOffers = if (currentTab == "received") receivedOffers else sentOffers
        layoutEmpty.visibility = if (currentOffers.isEmpty()) View.VISIBLE else View.GONE
        rvOffers.visibility = if (currentOffers.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}