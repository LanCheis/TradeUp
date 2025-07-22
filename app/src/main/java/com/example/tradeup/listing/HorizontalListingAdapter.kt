package com.example.tradeup.listing

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tradeup.R
import com.example.tradeup.data.model.Listing
import java.text.NumberFormat
import java.util.*

class HorizontalListingAdapter(private val listings: List<Listing>) :
    RecyclerView.Adapter<HorizontalListingAdapter.HorizontalViewHolder>() {

    inner class HorizontalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivImage: ImageView = view.findViewById(R.id.ivImage)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvPrice: TextView = view.findViewById(R.id.tvPrice)
        val tvLocation: TextView = view.findViewById(R.id.tvLocation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HorizontalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_listing_horizontal, parent, false)
        return HorizontalViewHolder(view)
    }

    override fun onBindViewHolder(holder: HorizontalViewHolder, position: Int) {
        val listing = listings[position]

        holder.tvTitle.text = listing.title
        holder.tvPrice.text = formatPrice(listing.price)
        holder.tvLocation.text = listing.location

        // ✅ SIMPLIFIED: Basic Glide usage that definitely works
        if (listing.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(listing.imageUrl)
                .into(holder.ivImage)
        } else {
            holder.ivImage.setImageResource(R.drawable.ic_avatar_placeholder)
        }

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, ListingDetailActivity::class.java).apply {
                putExtra("listing_id", listing.id)
                putExtra("title", listing.title)
                putExtra("price", listing.price)
                putExtra("imageUrl", listing.imageUrl)
                putExtra("category", listing.category)
                putExtra("condition", listing.condition)
                putExtra("description", listing.description)
                putExtra("location", listing.location)
                putExtra("ownerName", listing.ownerName)
                putExtra("isNegotiable", listing.isNegotiable)
                putExtra("sellerId", listing.ownerUid)
                putExtra("sellerName", listing.ownerName)
                putExtra("views", listing.views)
                putExtra("interactions", listing.interactions)
                putExtra("createdAt", listing.createdAt)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = listings.size

    private fun formatPrice(price: Double): String {
        return if (price > 0) {
            val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
            "${formatter.format(price)} ₫"
        } else {
            "Contact for price"
        }
    }
}