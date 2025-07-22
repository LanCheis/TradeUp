package com.example.tradeup.listing

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tradeup.R
import com.example.tradeup.data.model.Listing

class SimpleMyListingAdapter(
    private val listings: List<Listing>,
    private val onStatusChange: (Listing, String) -> Unit
) : RecyclerView.Adapter<SimpleMyListingAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivImage: ImageView = view.findViewById(R.id.ivImage)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvPrice: TextView = view.findViewById(R.id.tvPrice)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val btnChangeStatus: Button = view.findViewById(R.id.btnChangeStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_simple_listing, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val listing = listings[position]

        holder.tvTitle.text = listing.title
        holder.tvPrice.text = "${listing.price} ₫"
        holder.tvStatus.text = listing.status

        Glide.with(holder.itemView.context)
            .load(listing.imageUrl)
            .placeholder(R.drawable.ic_image_placeholder)
            .into(holder.ivImage)

        holder.btnChangeStatus.setOnClickListener {
            val newStatus = when (listing.status) {
                "Available" -> "Auctioning"
                "Auctioning" -> "Sold"
                else -> "Available"
            }
            onStatusChange(listing, newStatus)
        }
    }

    override fun getItemCount() = listings.size
}