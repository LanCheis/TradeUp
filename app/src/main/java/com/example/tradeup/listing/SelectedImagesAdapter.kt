package com.example.tradeup.listing

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tradeup.R

class SelectedImagesAdapter(
    private val images: MutableList<Uri>,
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<SelectedImagesAdapter.ImageViewHolder>() {

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.ivImage)
        val btnRemove: ImageView = view.findViewById(R.id.btnRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_selected_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageUri = images[position]

        // Load image with Glide
        Glide.with(holder.imageView.context)
            .load(imageUri)
            .centerCrop()
            .placeholder(R.drawable.ic_image_placeholder)
            .into(holder.imageView)

        // Remove button click
        holder.btnRemove.setOnClickListener {
            onRemoveClick(position)
        }
    }

    override fun getItemCount() = images.size
}