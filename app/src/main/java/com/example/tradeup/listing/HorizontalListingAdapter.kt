package com.example.tradeup.listing

import android.content.Intent
import android.util.Log
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
import com.example.tradeup.utils.CloudinaryHelper
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

        holder.tvTitle.text = if (listing.title.isNotEmpty()) listing.title else "No Title"
        holder.tvPrice.text = formatPrice(listing.price)
        holder.tvLocation.text = if (listing.location.isNotEmpty()) listing.location else "No Location"

        // ✅ Load image with debug info
        Log.d("HorizontalAdapter", "Loading listing: ${listing.title}, Image: ${listing.imageUrl}")
        loadImageIntoView(holder.ivImage, listing.imageUrl, listing.title)

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
                putExtra("ownerAvatar", listing.ownerAvatar) // ✅ ADDED: Pass owner avatar
                putExtra("ownerUid", listing.ownerUid) // ✅ ADDED: Might be useful
                putExtra("isNegotiable", listing.isNegotiable)
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

    // ✅ Enhanced image loading with optimization and logging
    private fun loadImageIntoView(imageView: ImageView, imageUrl: String?, itemTitle: String = "") {
        val context = imageView.context

        if (imageUrl.isNullOrBlank()) {
            Log.w("HorizontalAdapter", "No image URL for: $itemTitle")
            imageView.setImageResource(R.drawable.ic_image_placeholder)
            return
        }

        // ✅ ADDED: Use optimized URL for better performance if it's Cloudinary
        val optimizedUrl = if (imageUrl.contains("cloudinary.com")) {
            CloudinaryHelper.getOptimizedUrl(
                originalUrl = imageUrl,
                width = 300,
                height = 200,
                crop = "fill"
            )
        } else {
            imageUrl
        }

        val requestOptions = RequestOptions()
            .placeholder(R.drawable.ic_image_placeholder)
            .error(R.drawable.ic_image_placeholder)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()

        try {
            Log.d("HorizontalAdapter", "Loading optimized image for $itemTitle: $optimizedUrl")

            Glide.with(context)
                .load(optimizedUrl)
                .apply(requestOptions)
                .into(imageView)
        } catch (e: Exception) {
            Log.e("HorizontalAdapter", "Failed to load image for $itemTitle: ${e.message}")
            imageView.setImageResource(R.drawable.ic_image_placeholder)
        }
    }
}