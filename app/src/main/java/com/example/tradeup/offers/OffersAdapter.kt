package com.example.tradeup.offers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tradeup.R
import com.example.tradeup.data.model.Offer
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class OffersAdapter(
    private val offers: List<Offer>,
    private val isReceivedTab: Boolean,
    private val onAcceptOffer: (Offer) -> Unit,
    private val onRejectOffer: (Offer) -> Unit,
    private val onWithdrawOffer: (Offer) -> Unit
) : RecyclerView.Adapter<OffersAdapter.OfferViewHolder>() {

    inner class OfferViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivListingImage: ImageView = view.findViewById(R.id.ivListingImage)
        val tvListingTitle: TextView = view.findViewById(R.id.tvListingTitle)
        val tvOfferPrice: TextView = view.findViewById(R.id.tvOfferPrice)
        val tvOriginalPrice: TextView = view.findViewById(R.id.tvOriginalPrice)
        val tvOfferStatus: TextView = view.findViewById(R.id.tvOfferStatus)
        val tvOfferDate: TextView = view.findViewById(R.id.tvOfferDate)
        val tvOfferMessage: TextView = view.findViewById(R.id.tvOfferMessage)
        val tvBuyerSellerName: TextView = view.findViewById(R.id.tvBuyerSellerName)
        val layoutActions: LinearLayout = view.findViewById(R.id.layoutActions)
        val btnAccept: Button = view.findViewById(R.id.btnAccept)
        val btnReject: Button = view.findViewById(R.id.btnReject)
        val btnWithdraw: Button = view.findViewById(R.id.btnWithdraw)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfferViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_offer, parent, false)
        return OfferViewHolder(view)
    }

    override fun onBindViewHolder(holder: OfferViewHolder, position: Int) {
        val offer = offers[position]

        // Load listing image
        Glide.with(holder.itemView.context)
            .load(offer.listingImageUrl)
            .placeholder(R.drawable.ic_image_placeholder)
            .error(R.drawable.ic_image_placeholder)
            .into(holder.ivListingImage)

        // Set basic info
        holder.tvListingTitle.text = offer.listingTitle
        holder.tvOfferPrice.text = "Offer: ${formatPrice(offer.offerPrice)}"
        holder.tvOriginalPrice.text = "Original: ${formatPrice(offer.originalPrice)}"
        holder.tvOfferStatus.text = offer.getStatusDisplay()
        holder.tvOfferDate.text = formatDate(offer.createdAt)

        // Set buyer/seller name based on tab
        if (isReceivedTab) {
            holder.tvBuyerSellerName.text = "From: ${offer.buyerName}"
        } else {
            holder.tvBuyerSellerName.text = "To: ${offer.sellerName}"
        }

        // Show message if exists
        if (offer.message.isNotEmpty()) {
            holder.tvOfferMessage.visibility = View.VISIBLE
            holder.tvOfferMessage.text = "\"${offer.message}\""
        } else {
            holder.tvOfferMessage.visibility = View.GONE
        }

        // Show appropriate action buttons
        setupActionButtons(holder, offer)
    }

    private fun setupActionButtons(holder: OfferViewHolder, offer: Offer) {
        holder.layoutActions.visibility = View.GONE
        holder.btnAccept.visibility = View.GONE
        holder.btnReject.visibility = View.GONE
        holder.btnWithdraw.visibility = View.GONE

        when {
            // Received offers - show accept/reject for pending offers
            isReceivedTab && offer.status == "pending" -> {
                holder.layoutActions.visibility = View.VISIBLE
                holder.btnAccept.visibility = View.VISIBLE
                holder.btnReject.visibility = View.VISIBLE

                holder.btnAccept.setOnClickListener { onAcceptOffer(offer) }
                holder.btnReject.setOnClickListener { onRejectOffer(offer) }
            }

            // Sent offers - show withdraw for pending offers
            !isReceivedTab && offer.status == "pending" -> {
                holder.layoutActions.visibility = View.VISIBLE
                holder.btnWithdraw.visibility = View.VISIBLE

                holder.btnWithdraw.setOnClickListener { onWithdrawOffer(offer) }
            }
        }

        // Set status color
        when (offer.status) {
            "pending" -> holder.tvOfferStatus.setTextColor(holder.itemView.context.getColor(R.color.warning))
            "accepted" -> holder.tvOfferStatus.setTextColor(holder.itemView.context.getColor(R.color.success))
            "rejected" -> holder.tvOfferStatus.setTextColor(holder.itemView.context.getColor(R.color.error))
            "withdrawn" -> holder.tvOfferStatus.setTextColor(holder.itemView.context.getColor(R.color.text_tertiary))
        }
    }

    override fun getItemCount() = offers.size

    private fun formatPrice(price: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        return "${formatter.format(price)} ₫"
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}