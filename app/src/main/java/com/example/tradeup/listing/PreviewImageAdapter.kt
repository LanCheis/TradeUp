package com.example.tradeup.listing

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tradeup.R

class PreviewImageAdapter(
    private val images: List<Uri>
) : RecyclerView.Adapter<PreviewImageAdapter.PreviewImageViewHolder>() {

    class PreviewImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.ivPreviewImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_preview_image, parent, false)
        return PreviewImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PreviewImageViewHolder, position: Int) {
        val uri = images[position]

        Glide.with(holder.itemView.context)
            .load(uri)
            .centerCrop()
            .placeholder(R.drawable.placeholder_image)
            .into(holder.imageView)
    }

    override fun getItemCount() = images.size
}