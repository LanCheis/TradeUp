package com.example.tradeup.offers

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tradeup.R
import com.example.tradeup.data.model.Offer
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class OffersAdapter(
    private var offers: List<Offer>,
    private val onOfferAction: (Offer, String) -> Unit
) : RecyclerView.Adapter<OffersAdapter.OfferViewHolder>() {

    private var viewType = "received" // "received" or "sent"

    inner class OfferViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivListing: ImageView = view.findViewById(R.id.ivListing)
        val tvListingTitle: TextView = view.findViewById(R.id.tvListingTitle)
        val tvOfferAmount: TextView = view.findViewById(R.id.tvOfferAmount)
        val tvOriginalPrice: TextView = view.findViewById(R.id.tvOriginalPrice)
        val tvUserName: TextView = view.findViewById(R.id.tvUserName)
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val btnAccept: Button = view.findViewById(R.id.btnAccept)
        val btnReject: Button = view.findViewById(R.id.btnReject)
        val btnCounter: Button = view.findViewById(R.id.btnCounter)
        val layoutActions: View = view.findViewById(R.id.layoutActions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfferViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_offer, parent, false)
        return OfferViewHolder(view)
    }

    override fun onBindViewHolder(holder: OfferViewHolder, position: Int) {
        val offer = offers[position]
        val context = holder.itemView.context

        // Load listing image
        Glide.with(context)
            .load(offer.listingImageUrl)
            .placeholder(R.drawable.ic_image_placeholder)
            .into(holder.ivListing)

        // Set basic info
        holder.tvListingTitle.text = offer.listingTitle
        holder.tvOfferAmount.text = formatPrice(offer.offerPrice)
        holder.tvOriginalPrice.text = "Original: ${formatPrice(offer.originalPrice)}"

        // Set user name based on view type
        holder.tvUserName.text = if (viewType == "received") {
            "From: ${offer.buyerName}"
        } else {
            "To: ${offer.sellerName}"
        }

        // Set message
        holder.tvMessage.text = if (offer.message.isNotEmpty()) {
            "\"${offer.message}\""
        } else {
            "No message"
        }

        // Set status
        holder.tvStatus.text = offer.getDisplayStatus()
        holder.tvStatus.setTextColor(Color.parseColor(offer.getStatusColor()))

        // Set date
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        holder.tvDate.text = dateFormat.format(Date(offer.createdAt))

        // Show/hide action buttons
        val showActions = viewType == "received" && offer.status == "pending" && !offer.isExpired()
        holder.layoutActions.visibility = if (showActions) View.VISIBLE else View.GONE

        if (showActions) {
            holder.btnAccept.setOnClickListener {
                onOfferAction(offer, "accept")
            }
            holder.btnReject.setOnClickListener {
                onOfferAction(offer, "reject")
            }
            holder.btnCounter.setOnClickListener {
                onOfferAction(offer, "counter")
            }
        }
    }

    override fun getItemCount() = offers.size

    fun updateOffers(newOffers: List<Offer>, type: String) {
        offers = newOffers
        viewType = type
        notifyDataSetChanged()
    }

    private fun formatPrice(price: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        return "${formatter.format(price)} ₫"
    }
}