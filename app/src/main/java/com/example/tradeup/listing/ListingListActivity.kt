package com.example.tradeup.listing

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tradeup.R
import com.example.tradeup.data.model.Listing
import com.google.firebase.firestore.FirebaseFirestore

class ListingListActivity : AppCompatActivity() {

    private lateinit var rvListings: RecyclerView
    private val listingList = mutableListOf<Listing>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_listings)

        rvListings = findViewById(R.id.rvListings)
        rvListings.layoutManager = LinearLayoutManager(this)
        rvListings.adapter = ListingAdapter(listingList)

        loadListings()
    }

    private fun loadListings() {
        FirebaseFirestore.getInstance()
            .collection("listings")
            .get()
            .addOnSuccessListener { result ->
                listingList.clear()
                for (doc in result) {
                    val item = doc.toObject(Listing::class.java)
                    listingList.add(item)
                }
                rvListings.adapter?.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "❌ Không tải được danh sách", Toast.LENGTH_SHORT).show()
            }
    }
}
