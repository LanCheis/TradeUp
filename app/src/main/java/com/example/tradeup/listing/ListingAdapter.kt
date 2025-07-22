package com.example.tradeup.listing

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.tradeup.R
import com.example.tradeup.data.model.Listing
import com.example.tradeup.utils.CloudinaryHelper

class ListingAdapter(private val listings: List<Listing>) :
    RecyclerView.Adapter<ListingAdapter.ListingViewHolder>() {

    inner class ListingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivImage: ImageView = view.findViewById(R.id.ivImage)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_listing, parent, false)
        return ListingViewHolder(view)
    }

    override fun onBindViewHolder(holder: ListingViewHolder, position: Int) {
        val listing = listings[position]

        holder.tvTitle.text = listing.title
        holder.tvCategory.text = listing.category

        // Use optimized Cloudinary URL or fallback to original
        val imageUrl = if (listing.imageUrl.contains("cloudinary.com")) {
            CloudinaryHelper.getOptimizedUrl(
                originalUrl = listing.imageUrl,
                width = 200,
                height = 200,
                crop = "fill"
            )
        } else {
            listing.imageUrl
        }

        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .error(R.drawable.ic_image_placeholder)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(holder.ivImage)

        // 🔥 REPLACE THIS SECTION:
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, ListingDetailActivity::class.java).apply {
                putExtra("listing_id", listing.id)
                putExtra("title", listing.title)
                putExtra("imageUrl", listing.imageUrl)
                putExtra("category", listing.category)
                putExtra("description", listing.description)
                // ✅ ADD THESE NEW LINES:
                putExtra("price", listing.price)
                putExtra("condition", listing.condition)
                putExtra("location", listing.location)
                putExtra("isNegotiable", listing.isNegotiable)
                putExtra("sellerId", listing.ownerUid)
                putExtra("sellerName", listing.ownerName)
                putExtra("ownerName", listing.ownerName)
                putExtra("views", listing.views)
                putExtra("interactions", listing.interactions)
                putExtra("createdAt", listing.createdAt)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = listings.size
}