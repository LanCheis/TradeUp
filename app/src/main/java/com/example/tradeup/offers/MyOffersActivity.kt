// File: app/src/main/java/com/example/tradeup/offers/MyOffersActivity.kt

package com.example.tradeup.offers

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.tradeup.R
import com.example.tradeup.data.model.Offer
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MyOffersActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var btnBack: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private val receivedOffers = mutableListOf<Offer>()
    private val sentOffers = mutableListOf<Offer>()

    private lateinit var receivedAdapter: OffersAdapter
    private lateinit var sentAdapter: OffersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_offers)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        initializeViews()
        setupTabs()
        setupClickListeners()
        loadOffers()
    }

    private fun initializeViews() {
        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)
        btnBack = findViewById(R.id.btnBack)
        progressBar = findViewById(R.id.progressBar)
        tvEmptyState = findViewById(R.id.tvEmptyState)
    }

    private fun setupTabs() {
        // Create adapters for both tabs
        receivedAdapter = OffersAdapter(receivedOffers, "received") { offer, action ->
            handleOfferAction(offer, action)
        }

        sentAdapter = OffersAdapter(sentOffers, "sent") { offer, action ->
            handleOfferAction(offer, action)
        }

        // Setup ViewPager with FragmentStateAdapter would be ideal,
        // but for simplicity, we'll use RecyclerViews directly
        setupSimpleTabLayout()
    }

    private fun setupSimpleTabLayout() {
        // Add tabs
        tabLayout.addTab(tabLayout.newTab().setText("Received"))
        tabLayout.addTab(tabLayout.newTab().setText("Sent"))

        // Handle tab selection
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

        // Show received offers by default
        showReceivedOffers()
    }

    private fun showReceivedOffers() {
        // This is a simplified approach - in a real app you'd use a ViewPager
        // For now, we'll just update the empty state and load data
        if (receivedOffers.isEmpty()) {
            tvEmptyState.text = "No received offers yet"
            tvEmptyState.visibility = View.VISIBLE
        } else {
            tvEmptyState.visibility = View.GONE
        }
    }

    private fun showSentOffers() {
        if (sentOffers.isEmpty()) {
            tvEmptyState.text = "No sent offers yet"
            tvEmptyState.visibility = View.VISIBLE
        } else {
            tvEmptyState.visibility = View.GONE
        }
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            onBackPressed()
        }
    }

    private fun loadOffers() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showEmptyState("Please login to view your offers")
            return
        }

        showLoading(true)

        // Load received offers (where current user is seller)
        firestore.collection("offers")
            .whereEqualTo("sellerId", currentUser.uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                receivedOffers.clear()
                documents.forEach { doc ->
                    val offer = doc.toObject(Offer::class.java).copy(id = doc.id)
                    receivedOffers.add(offer)
                }
                receivedAdapter.updateOffers(receivedOffers, "received")

                // Load sent offers (where current user is buyer)
                loadSentOffers(currentUser.uid)
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                showEmptyState("Failed to load offers: ${exception.message}")
            }
    }

    private fun loadSentOffers(userId: String) {
        firestore.collection("offers")
            .whereEqualTo("buyerId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                sentOffers.clear()
                documents.forEach { doc ->
                    val offer = doc.toObject(Offer::class.java).copy(id = doc.id)
                    sentOffers.add(offer)
                }
                sentAdapter.updateOffers(sentOffers, "sent")

                showLoading(false)
                updateEmptyState()
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                showEmptyState("Failed to load sent offers: ${exception.message}")
            }
    }

    private fun updateEmptyState() {
        val currentTab = tabLayout.selectedTabPosition
        when (currentTab) {
            0 -> showReceivedOffers()
            1 -> showSentOffers()
        }
    }

    private fun handleOfferAction(offer: Offer, action: String) {
        when (action) {
            "accept" -> acceptOffer(offer)
            "reject" -> rejectOffer(offer)
            "counter" -> showCounterOfferDialog(offer)
            "view" -> showOfferDetails(offer)
        }
    }

    private fun acceptOffer(offer: Offer) {
        updateOfferStatus(offer.id, "accepted") { success ->
            if (success) {
                Toast.makeText(this, "Offer accepted!", Toast.LENGTH_SHORT).show()
                loadOffers() // Refresh the list
            } else {
                Toast.makeText(this, "Failed to accept offer", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun rejectOffer(offer: Offer) {
        updateOfferStatus(offer.id, "rejected") { success ->
            if (success) {
                Toast.makeText(this, "Offer rejected", Toast.LENGTH_SHORT).show()
                loadOffers() // Refresh the list
            } else {
                Toast.makeText(this, "Failed to reject offer", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showCounterOfferDialog(offer: Offer) {
        // Create a simple dialog for counter offer
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Counter Offer")
            .setMessage("Counter offer feature coming soon!")
            .setPositiveButton("OK", null)
            .create()
        dialog.show()
    }

    private fun showOfferDetails(offer: Offer) {
        val message = buildOfferDetailsMessage(offer)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Offer Details")
            .setMessage(message)
            .setPositiveButton("Close", null)
            .create()
        dialog.show()
    }

    private fun buildOfferDetailsMessage(offer: Offer): String {
        val currentUser = auth.currentUser
        val isUserBuyer = currentUser?.uid == offer.buyerId
        val role = if (isUserBuyer) "Your Offer" else "Received Offer"

        return """
            $role
            
            Original Price: ${formatPrice(offer.originalPrice)}
            Offered Amount: ${formatPrice(offer.calculateOfferAmount())}
            Status: ${offer.status.uppercase()}
            
            ${if (offer.message.isNotEmpty()) "Message: \"${offer.message}\"" else "No message"}
            ${if (offer.counterOffer != null) "\nCounter Offer: ${formatPrice(offer.counterOffer)}" else ""}
            ${if (!offer.counterMessage.isNullOrEmpty()) "\nCounter Message: \"${offer.counterMessage}\"" else ""}
        """.trimIndent()
    }

    private fun formatPrice(price: Double): String {
        return "${String.format("%,.0f", price)} ₫"
    }

    private fun updateOfferStatus(offerId: String, status: String, callback: (Boolean) -> Unit) {
        firestore.collection("offers")
            .document(offerId)
            .update("status", status, "updatedAt", System.currentTimeMillis())
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showEmptyState(message: String) {
        tvEmptyState.text = message
        tvEmptyState.visibility = View.VISIBLE
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}