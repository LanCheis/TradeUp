package com.example.tradeup.listing

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tradeup.R

class SelectedImageAdapter(
    private val images: MutableList<Uri>,
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<SelectedImageAdapter.ImageViewHolder>() {

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.ivSelectedImage)
        val removeButton: ImageButton = view.findViewById(R.id.btnRemoveImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_selected_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val uri = images[position]

        Glide.with(holder.itemView.context)
            .load(uri)
            .centerCrop()
            .placeholder(R.drawable.placeholder_image)
            .into(holder.imageView)

        holder.removeButton.setOnClickListener {
            onRemoveClick(position)
        }
    }

    override fun getItemCount() = images.size
}