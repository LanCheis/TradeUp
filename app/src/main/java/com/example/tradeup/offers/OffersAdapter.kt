package com.example.tradeup.offers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tradeup.R
import com.example.tradeup.data.model.Offer
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class OffersAdapter(
    private var offers: MutableList<Offer>,
    private var tabType: String, // "received" or "sent"
    private val onOfferAction: (Offer, String) -> Unit
) : RecyclerView.Adapter<OffersAdapter.OfferViewHolder>() {

    inner class OfferViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivItemImage: ImageView = view.findViewById(R.id.ivItemImage)
        val tvItemTitle: TextView = view.findViewById(R.id.tvItemTitle)
        val tvOriginalPrice: TextView = view.findViewById(R.id.tvOriginalPrice)
        val tvOfferPrice: TextView = view.findViewById(R.id.tvOfferPrice)
        val tvUserName: TextView = view.findViewById(R.id.tvUserName)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        val layoutActions: LinearLayout = view.findViewById(R.id.layoutActions)
        val btnAccept: Button = view.findViewById(R.id.btnAccept)
        val btnReject: Button = view.findViewById(R.id.btnReject)
        val btnCounter: Button = view.findViewById(R.id.btnCounter)
        val statusIndicator: View = view.findViewById(R.id.statusIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfferViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_offer, parent, false)
        return OfferViewHolder(view)
    }

    override fun onBindViewHolder(holder: OfferViewHolder, position: Int) {
        val offer = offers[position]
        val context = holder.itemView.context

        // Load item image
        Glide.with(context)
            .load(offer.listingImageUrl)
            .placeholder(R.drawable.ic_image_placeholder)
            .error(R.drawable.ic_image_placeholder)
            .into(holder.ivItemImage)

        // Set basic info
        holder.tvItemTitle.text = offer.listingTitle
        holder.tvOriginalPrice.text = "Original: ${formatPrice(offer.originalPrice)}"
        holder.tvOfferPrice.text = "Offer: ${formatPrice(offer.offeredPrice)}"

        // Set user name based on tab type
        holder.tvUserName.text = if (tabType == "received") {
            "From: ${offer.buyerName}"
        } else {
            "To: ${offer.sellerName}"
        }

        // Set date - FIXED
        val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        val date = try {
            when {
                offer.createdAt > 0L -> Date(offer.createdAt)
                else -> Date() // Current date as fallback
            }
        } catch (e: Exception) {
            Date() // Current date as fallback
        }
        holder.tvDate.text = dateFormat.format(date)

        // Set message
        holder.tvMessage.text = if (offer.message.isNotEmpty()) {
            "\"${offer.message}\""
        } else {
            "No message"
        }
        holder.tvMessage.visibility = if (offer.message.isNotEmpty()) View.VISIBLE else View.GONE

        // Set status and status indicator
        setStatusAppearance(holder, offer.status)

        // Configure action buttons
        configureActionButtons(holder, offer)
    }

    private fun setStatusAppearance(holder: OfferViewHolder, status: String) {
        val context = holder.itemView.context

        when (status) {
            "pending" -> {
                holder.tvStatus.text = "Pending"
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.orange))
                holder.statusIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.orange))
            }
            "accepted" -> {
                holder.tvStatus.text = "Accepted"
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.green))
                holder.statusIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.green))
            }
            "rejected" -> {
                holder.tvStatus.text = "Rejected"
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.red))
                holder.statusIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.red))
            }
            "countered" -> {
                holder.tvStatus.text = "Countered"
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.blue))
                holder.statusIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.blue))
            }
            else -> {
                holder.tvStatus.text = status
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                holder.statusIndicator.setBackgroundColor(ContextCompat.getColor(context, android.R.color.darker_gray))
            }
        }
    }

    private fun configureActionButtons(holder: OfferViewHolder, offer: Offer) {
        // Show action buttons only for received offers that are pending
        if (tabType == "received" && offer.status == "pending") {
            holder.layoutActions.visibility = View.VISIBLE

            holder.btnAccept.setOnClickListener {
                onOfferAction(offer, "accept")
            }

            holder.btnReject.setOnClickListener {
                onOfferAction(offer, "reject")
            }

            holder.btnCounter.setOnClickListener {
                onOfferAction(offer, "counter")
            }
        } else {
            holder.layoutActions.visibility = View.GONE
        }

        // Add click listener for viewing details
        holder.itemView.setOnClickListener {
            onOfferAction(offer, "view")
        }
    }

    private fun formatPrice(price: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        return "${formatter.format(price)} ₫"
    }

    fun updateOffers(newOffers: List<Offer>, newTabType: String) {
        offers.clear()
        offers.addAll(newOffers)
        tabType = newTabType
        notifyDataSetChanged()
    }

    override fun getItemCount() = offers.size
}