package com.example.tradeup.listing

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_listing, parent, false)
        return ListingViewHolder(view)
    }

    override fun onBindViewHolder(holder: ListingViewHolder, position: Int) {
        val listing = listings[position]
        holder.tvTitle.text = listing.title
        holder.tvCategory.text = listing.category
        Glide.with(holder.itemView.context)
            .load(listing.imageUrl)
            .into(holder.ivImage)

        override fun onBindViewHolder(holder: ListingViewHolder, position: Int) {
            val listing = listings[position]
            holder.tvTitle.text = listing.title
            holder.tvCategory.text = listing.category

            Glide.with(holder.itemView.context)
                .load(listing.imageUrl)
                .into(holder.ivImage)

            // 👇 Bắt sự kiện click để mở chi tiết
            holder.itemView.setOnClickListener {
                val context = holder.itemView.context
                val intent = Intent(context, ListingDetailActivity::class.java).apply {
                    putExtra("imageUrl", listing.imageUrl)
                    putExtra("title", listing.title)
                    putExtra("category", listing.category)
                    putExtra("description", listing.description)
                }
                context.startActivity(intent)
            }
        }

    }

    override fun getItemCount(): Int = listings.size
}
