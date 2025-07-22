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
import com.bumptech.glide.request.RequestOptions
import com.example.tradeup.R
import com.example.tradeup.data.model.Listing

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

        holder.tvTitle.text = if (listing.title.isNotEmpty()) listing.title else "No Title"
        holder.tvCategory.text = if (listing.category.isNotEmpty()) listing.category else "No Category"

        // ✅ Comprehensive image loading with all error cases handled
        loadImageIntoView(holder.ivImage, listing.imageUrl)

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, ListingDetailActivity::class.java).apply {
                putExtra("listing_id", listing.id)
                putExtra("title", listing.title)
                putExtra("imageUrl", listing.imageUrl) // ✅ Consistent field name
                putExtra("category", listing.category)
                putExtra("description", listing.description)
                putExtra("price", listing.price)
                putExtra("location", listing.location)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = listings.size

    // ✅ Centralized image loading function
    private fun loadImageIntoView(imageView: ImageView, imageUrl: String?) {
        val context = imageView.context

        if (imageUrl.isNullOrBlank()) {
            // No image URL provided - show placeholder
            imageView.setImageResource(R.drawable.ic_image_placeholder)
            return
        }

        val requestOptions = RequestOptions()
            .placeholder(R.drawable.ic_image_placeholder)
            .error(R.drawable.ic_image_placeholder)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()

        try {
            Glide.with(context)
                .load(imageUrl)
                .apply(requestOptions)
                .into(imageView)
        } catch (e: Exception) {
            // If Glide fails for any reason, set placeholder
            imageView.setImageResource(R.drawable.ic_image_placeholder)
        }
    }
}