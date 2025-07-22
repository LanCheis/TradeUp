package com.example.tradeup.listing

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tradeup.R
import com.example.tradeup.data.model.Listing
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MyListingsActivity : AppCompatActivity() {

    private lateinit var rvMyListings: RecyclerView
    private val myListings = mutableListOf<Listing>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_listings)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Products"

        initViews()
        loadMyListings()
    }

    private fun initViews() {
        rvMyListings = findViewById(R.id.rvMyListings)

        // Simple adapter for now
        val adapter = SimpleMyListingAdapter(myListings) { listing, newStatus ->
            updateListingStatus(listing, newStatus)
        }

        rvMyListings.layoutManager = LinearLayoutManager(this)
        rvMyListings.adapter = adapter
    }

    private fun loadMyListings() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("listings")
            .whereEqualTo("ownerUid", currentUid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                myListings.clear()
                for (doc in result) {
                    val listing = doc.toObject(Listing::class.java)
                    myListings.add(listing)
                }
                rvMyListings.adapter?.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Could not load listings", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateListingStatus(listing: Listing, newStatus: String) {
        val updatedListing = listing.copy(
            status = newStatus,
            updatedAt = System.currentTimeMillis()
        )

        FirebaseFirestore.getInstance()
            .collection("listings")
            .document(listing.id)
            .set(updatedListing)
            .addOnSuccessListener {
                Toast.makeText(this, "Status updated", Toast.LENGTH_SHORT).show()
                loadMyListings()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}