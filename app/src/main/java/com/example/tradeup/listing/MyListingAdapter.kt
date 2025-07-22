package com.example.tradeup.listing

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tradeup.R
import com.example.tradeup.data.model.Listing
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class MyListingAdapter(
    private val listings: List<Listing>,
    private val onStatusChange: (Listing, String) -> Unit
) : RecyclerView.Adapter<MyListingAdapter.MyListingViewHolder>() {

    inner class MyListingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivImage: ImageView = view.findViewById(R.id.ivImage)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvPrice: TextView = view.findViewById(R.id.tvPrice)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val btnChangeStatus: Button = view.findViewById(R.id.btnChangeStatus)
        val statusIndicator: View = view.findViewById(R.id.statusIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyListingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_listing, parent, false)
        return MyListingViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyListingViewHolder, position: Int) {
        val listing = listings[position]
        val context = holder.itemView.context

        // Load image
        Glide.with(context)
            .load(listing.imageUrl)
            .placeholder(R.drawable.ic_image_placeholder)
            .error(R.drawable.ic_image_placeholder)
            .into(holder.ivImage)

        // Set text data
        holder.tvTitle.text = listing.title
        holder.tvPrice.text = formatPrice(listing.price)

        // Format date
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi", "VN"))
        holder.tvDate.text = "📅 ${dateFormat.format(Date(listing.createdAt))}"

        // Set status with colors
        setStatusAppearance(holder, listing.status)

        // Status change button
        holder.btnChangeStatus.setOnClickListener {
            showStatusChangeDialog(context, listing, onStatusChange)
        }
    }

    private fun setStatusAppearance(holder: MyListingViewHolder, status: String) {
        val context = holder.itemView.context

        when (status) {
            "Available" -> {
                holder.tvStatus.text = "🟢 Available"
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.teal_700))
                holder.statusIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.teal_700))
                holder.btnChangeStatus.text = "Change Status"
            }
            "Sold" -> {
                holder.tvStatus.text = "✅ Sold"
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                holder.statusIndicator.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                holder.btnChangeStatus.text = "Sold"
            }
            "Auctioning" -> {
                holder.tvStatus.text = "🔥 Auctioning"
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.blue_500))
                holder.statusIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.blue_500))
                holder.btnChangeStatus.text = "Auctioning"
            }
            else -> {
                holder.tvStatus.text = "❓ Unknown"
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                holder.statusIndicator.setBackgroundColor(ContextCompat.getColor(context, android.R.color.darker_gray))
            }
        }
    }

    private fun showStatusChangeDialog(
        context: Context,
        listing: Listing,
        onStatusChange: (Listing, String) -> Unit
    ) {
        val statusOptions = arrayOf(
            "🟢 Available",
            "🔥 Auctioning",
            "✅ Sold"
        )
        val statusValues = arrayOf("Available", "Auctioning", "Sold")

        val currentIndex = statusValues.indexOf(listing.status)

        AlertDialog.Builder(context)
            .setTitle("🔄 Change Product Status")
            .setSingleChoiceItems(statusOptions, currentIndex) { dialog, which ->
                val newStatus = statusValues[which]
                onStatusChange(listing, newStatus)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun formatPrice(price: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        return "💰 ${formatter.format(price)} ₫"
    }

    override fun getItemCount() = listings.size
}