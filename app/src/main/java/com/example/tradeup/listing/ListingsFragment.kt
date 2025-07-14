package com.example.tradeup.listing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tradeup.R
import com.example.tradeup.data.model.Listing
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ListingsFragment : Fragment() {

    private lateinit var rvListings: RecyclerView
    private val listingList = mutableListOf<Listing>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_listings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvListings = view.findViewById(R.id.rvListings)
        rvListings.layoutManager = LinearLayoutManager(requireContext())
        rvListings.adapter = ListingAdapter(listingList)

        loadListings()
    }

    private fun loadListings() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid

        FirebaseFirestore.getInstance()
            .collection("listings")
            .get()
            .addOnSuccessListener { result ->
                listingList.clear()
                for (doc in result) {
                    val item = doc.toObject(Listing::class.java)
                    if (item.ownerUid != currentUid) {
                        listingList.add(item)
                    }
                }
                rvListings.adapter?.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "❌ Không tải được danh sách", Toast.LENGTH_SHORT).show()
            }
    }
}
