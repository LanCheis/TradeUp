package com.example.tradeup.listing

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tradeup.R
import com.example.tradeup.data.model.Listing

class UserListingsAdapter(
    private val onActionClick: (Listing, String) -> Unit
) : RecyclerView.Adapter<UserListingsAdapter.ListingViewHolder>() {

    private var listings = mutableListOf<Listing>()

    class ListingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivImage: ImageView = view.findViewById(R.id.ivListingImage)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvPrice: TextView = view.findViewById(R.id.tvPrice)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvViews: TextView = view.findViewById(R.id.tvViews)
        val tvInteractions: TextView = view.findViewById(R.id.tvInteractions)
        val btnOptions: ImageView = view.findViewById(R.id.btnOptions)
        val chipNegotiable: View = view.findViewById(R.id.chipNegotiable)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_listing, parent, false)
        return ListingViewHolder(view)
    }

    override fun onBindViewHolder(holder: ListingViewHolder, position: Int) {
        val listing = listings[position]

        // Basic info
        holder.tvTitle.text = listing.title
        holder.tvPrice.text = "₫${String.format("%,.0f", listing.price)}"

        // FR-2.2.2: Status with color coding
        holder.tvStatus.text = listing.status
        holder.tvStatus.setBackgroundResource(
            when (listing.status) {
                "Available" -> R.drawable.status_available_background
                "Sold" -> R.drawable.status_sold_background
                "Paused" -> R.drawable.status_paused_background
                else -> R.drawable.status_available_background
            }
        )

        // FR-2.2.3: Analytics display
        holder.tvViews.text = "${listing.views} views"
        holder.tvInteractions.text = "${listing.interactions} interactions"

        // Negotiable indicator
        holder.chipNegotiable.visibility = if (listing.isNegotiable) View.VISIBLE else View.GONE

        // Load first image
        if (listing.imageUrls.isNotEmpty()) {
            Glide.with(holder.ivImage.context)
                .load(listing.imageUrls[0])
                .centerCrop()
                .placeholder(R.drawable.ic_image_placeholder)
                .into(holder.ivImage)
        } else {
            holder.ivImage.setImageResource(R.drawable.ic_image_placeholder)
        }

        // FR-2.2.1: Options menu
        holder.btnOptions.setOnClickListener {
            showOptionsMenu(it, listing)
        }
    }

    private fun showOptionsMenu(view: View, listing: Listing) {
        val popup = PopupMenu(view.context, view)
        popup.menuInflater.inflate(R.menu.listing_options_menu, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit -> {
                    onActionClick(listing, "edit")
                    true
                }
                R.id.action_change_status -> {
                    onActionClick(listing, "toggle_status")
                    true
                }
                R.id.action_view_analytics -> {
                    onActionClick(listing, "view_analytics")
                    true
                }
                R.id.action_delete -> {
                    onActionClick(listing, "delete")
                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    override fun getItemCount() = listings.size

    fun updateListings(newListings: List<Listing>) {
        listings.clear()
        listings.addAll(newListings)
        notifyDataSetChanged()
    }
}