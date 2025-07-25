package com.example.tradeup.ui.browse

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tradeup.R
import com.example.tradeup.data.model.Listing
import com.example.tradeup.databinding.ItemListingGridBinding
import java.text.NumberFormat
import java.util.*

class ListingsAdapter(
    private val onItemClick: (Listing) -> Unit
) : ListAdapter<Listing, ListingsAdapter.ListingViewHolder>(ListingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListingViewHolder {
        val binding = ItemListingGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ListingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ListingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ListingViewHolder(
        private val binding: ItemListingGridBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(listing: Listing) {
            with(binding) {
                // FR-2.1.1: Display required fields
                titleText.text = listing.title
                priceText.text = NumberFormat.getCurrencyInstance(Locale.US).format(listing.price)
                locationText.text = listing.location
                conditionText.text = listing.condition

                // FIXED: Use imageUrls instead of photos
                if (listing.imageUrls.isNotEmpty()) {
                    Glide.with(itemView.context)
                        .load(listing.imageUrls[0])
                        .placeholder(R.drawable.ic_placeholder_image)
                        .error(R.drawable.ic_placeholder_image)
                        .centerCrop()
                        .into(imageView)
                } else {
                    imageView.setImageResource(R.drawable.ic_placeholder_image)
                }

                // FR-2.2.3: Show analytics (views)
                viewsText.text = "${listing.views} views"

                // FR-5.1.1: Show if negotiable (for offers)
                negotiableChip.visibility = if (listing.isNegotiable) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }

                // Click handler
                root.setOnClickListener {
                    onItemClick(listing)
                }
            }
        }
    }

    private class ListingDiffCallback : DiffUtil.ItemCallback<Listing>() {
        override fun areItemsTheSame(oldItem: Listing, newItem: Listing): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Listing, newItem: Listing): Boolean {
            return oldItem == newItem
        }
    }
}