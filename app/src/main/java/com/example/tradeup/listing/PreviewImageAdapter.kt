package com.example.tradeup.listing

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tradeup.R

class PreviewImageAdapter(
    private val images: List<Uri>
) : RecyclerView.Adapter<PreviewImageAdapter.PreviewImageViewHolder>() {

    class PreviewImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.ivPreviewImage)
        val tvImageCount: TextView = view.findViewById(R.id.tvImageCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_preview_image, parent, false)
        return PreviewImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PreviewImageViewHolder, position: Int) {
        val imageUri = images[position]

        // Load image with Glide
        Glide.with(holder.imageView.context)
            .load(imageUri)
            .centerCrop()
            .placeholder(R.drawable.ic_image_placeholder)
            .into(holder.imageView)

        // Show image counter on first image
        if (position == 0 && images.size > 1) {
            holder.tvImageCount.visibility = View.VISIBLE
            holder.tvImageCount.text = holder.itemView.context.getString(
                R.string.image_count_format,
                1,
                images.size
            )
        } else {
            holder.tvImageCount.visibility = View.GONE
        }
    }

    override fun getItemCount() = images.size
}