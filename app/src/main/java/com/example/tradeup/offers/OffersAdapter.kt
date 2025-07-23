// File: app/src/main/java/com/example/tradeup/offers/OffersAdapter.kt

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
        holder.tvOfferPrice.text = "Offer: ${formatPrice(offer.calculateOfferAmount())}"

        // Set user name based on tab type
        holder.tvUserName.text = if (tabType == "received") {
            "From: ${offer.buyerName}"
        } else {
            "To: ${offer.sellerName}"
        }

        // ✅ COMPLETELY SAFE DATE HANDLING
        val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        val dateText = try {
            // Convert Timestamp? to Long safely
            val timestamp: Long = offer.createdAt?.toDate()?.time ?: System.currentTimeMillis()
            val date = Date(timestamp)
            dateFormat.format(date)
        } catch (e: Exception) {
            // If anything fails, show a simple fallback
            "Recent"
        }
        holder.tvDate.text = dateText

        // Set message
        if (offer.message.isNotEmpty()) {
            holder.tvMessage.text = "\"${offer.message}\""
            holder.tvMessage.visibility = View.VISIBLE
        } else {
            holder.tvMessage.text = "No message"
            holder.tvMessage.visibility = View.GONE
        }

        // Set status and status indicator
        setStatusAppearance(holder, offer.status)

        // Configure action buttons
        configureActionButtons(holder, offer)
    }

    private fun setStatusAppearance(holder: OfferViewHolder, status: String) {
        val context = holder.itemView.context

        when (status.lowercase()) {
            "pending" -> {
                holder.tvStatus.text = "Pending"
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.warning))
                holder.statusIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.warning))
            }
            "accepted" -> {
                holder.tvStatus.text = "Accepted"
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.success))
                holder.statusIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.success))
            }
            "rejected" -> {
                holder.tvStatus.text = "Rejected"
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.error))
                holder.statusIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.error))
            }
            "countered" -> {
                holder.tvStatus.text = "Countered"
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.info))
                holder.statusIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.info))
            }
            else -> {
                holder.tvStatus.text = status.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                holder.statusIndicator.setBackgroundColor(ContextCompat.getColor(context, android.R.color.darker_gray))
            }
        }
    }

    private fun configureActionButtons(holder: OfferViewHolder, offer: Offer) {
        // Show action buttons only for received offers that are pending
        val shouldShowActions = tabType == "received" && offer.status.equals("pending", ignoreCase = true)

        if (shouldShowActions) {
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
        return try {
            val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
            "${formatter.format(price)} ₫"
        } catch (e: Exception) {
            String.format("%.0f ₫", price)
        }
    }

    fun updateOffers(newOffers: List<Offer>, newTabType: String) {
        offers.clear()
        offers.addAll(newOffers)
        tabType = newTabType
        notifyDataSetChanged()
    }

    override fun getItemCount() = offers.size
}