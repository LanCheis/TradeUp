package com.example.tradeup.listing

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tradeup.R
import com.example.tradeup.data.model.Listing
import com.example.tradeup.data.model.ListingStatus
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying user's listings in a RecyclerView
 * FR-2.2.1: View/edit/delete listings from user dashboard
 * FR-2.2.2: Listing status options: Available, Sold, Paused
 * FR-2.2.3: Listing analytics (views, interactions)
 */
class UserListingsAdapter(
    private val onActionClick: (Listing, String) -> Unit
) : RecyclerView.Adapter<UserListingsAdapter.ListingViewHolder>() {

    private var listings = mutableListOf<Listing>()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    // Constants for performance thresholds
    companion object {
        private const val HIGH_PERFORMANCE_THRESHOLD = 100
        private const val GOOD_PERFORMANCE_THRESHOLD = 50
        private const val DAYS_OLD_THRESHOLD = 7 * 24 * 60 * 60 * 1000L // 7 days in milliseconds
    }

    /**
     * ViewHolder class containing all UI elements for a listing item
     */
    class ListingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Basic listing elements
        val ivListingImage: ImageView = view.findViewById(R.id.ivListingImage)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvPrice: TextView = view.findViewById(R.id.tvPrice)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvCondition: TextView = view.findViewById(R.id.tvCondition)

        // Status elements
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val ivStatusIcon: ImageView = view.findViewById(R.id.ivStatusIcon)
        val ivStatusBadgeIcon: ImageView = view.findViewById(R.id.ivStatusBadgeIcon)

        // Analytics elements
        val tvViews: TextView = view.findViewById(R.id.tvViews)
        val tvInteractions: TextView = view.findViewById(R.id.tvInteractions)
        val tvFavorites: TextView = view.findViewById(R.id.tvFavorites)
        val tvCreatedDate: TextView = view.findViewById(R.id.tvCreatedDate)

        // Feature indicators
        val chipNegotiable: TextView = view.findViewById(R.id.chipNegotiable)
        val tvPhotoCount: TextView = view.findViewById(R.id.tvPhotoCount)
        val ivLocationIndicator: ImageView = view.findViewById(R.id.ivLocationIndicator)
        val ivPerformanceIndicator: ImageView = view.findViewById(R.id.ivPerformanceIndicator)

        // Action buttons
        val btnOptions: ImageView = view.findViewById(R.id.btnOptions)
        val btnQuickEdit: ImageView? = try {
            view.findViewById(R.id.btnQuickEdit)
        } catch (e: Exception) {
            null // Optional view, might not exist in all layouts
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_listing, parent, false)
        return ListingViewHolder(view)
    }

    override fun onBindViewHolder(holder: ListingViewHolder, position: Int) {
        val listing = listings[position]
        val context = holder.itemView.context

        try {
            // Bind basic listing information
            bindBasicInfo(holder, listing)

            // Bind status information with icons and colors
            bindStatusInfo(holder, listing, context)

            // Bind analytics data
            bindAnalyticsInfo(holder, listing)

            // Bind feature indicators
            bindFeatureIndicators(holder, listing, context)

            // Bind performance indicators
            bindPerformanceIndicators(holder, listing, context)

            // Load listing image
            loadListingImage(holder, listing)

            // Setup click listeners
            setupClickListeners(holder, listing)

            // Setup accessibility
            setupAccessibility(holder, listing)

        } catch (e: Exception) {
            // Handle any binding errors gracefully
            e.printStackTrace()
            bindErrorState(holder, listing)
        }
    }

    /**
     * Bind basic listing information (title, price, category, condition)
     */
    private fun bindBasicInfo(holder: ListingViewHolder, listing: Listing) {
        holder.tvTitle.text = listing.title.takeIf { it.isNotEmpty() } ?: "Untitled Listing"
        holder.tvPrice.text = "₫${String.format("%,.0f", listing.price)}"
        holder.tvCategory.text = listing.category.takeIf { it.isNotEmpty() } ?: "Unknown"
        holder.tvCondition.text = listing.condition.takeIf { it.isNotEmpty() } ?: "Not specified"
    }

    /**
     * FR-2.2.2: Bind status information with appropriate colors and icons
     */
    private fun bindStatusInfo(holder: ListingViewHolder, listing: Listing, context: android.content.Context) {
        holder.tvStatus.text = listing.status

        when (listing.status) {
            ListingStatus.AVAILABLE -> {
                holder.tvStatus.setBackgroundResource(R.drawable.status_available_background)
                holder.tvStatus.setTextColor(context.getColor(R.color.green_700))
                holder.ivStatusIcon.setImageResource(R.drawable.ic_available)
                holder.ivStatusBadgeIcon.setImageResource(R.drawable.ic_available)
            }
            ListingStatus.SOLD -> {
                holder.tvStatus.setBackgroundResource(R.drawable.status_sold_background)
                holder.tvStatus.setTextColor(context.getColor(R.color.blue_700))
                holder.ivStatusIcon.setImageResource(R.drawable.ic_sold)
                holder.ivStatusBadgeIcon.setImageResource(R.drawable.ic_sold)
            }
            ListingStatus.PAUSED -> {
                holder.tvStatus.setBackgroundResource(R.drawable.status_paused_background)
                holder.tvStatus.setTextColor(context.getColor(R.color.orange_700))
                holder.ivStatusIcon.setImageResource(R.drawable.ic_paused)
                holder.ivStatusBadgeIcon.setImageResource(R.drawable.ic_paused)
            }
            else -> {
                // Default to available status
                holder.tvStatus.setBackgroundResource(R.drawable.status_available_background)
                holder.tvStatus.setTextColor(context.getColor(R.color.green_700))
                holder.ivStatusIcon.setImageResource(R.drawable.ic_available)
                holder.ivStatusBadgeIcon.setImageResource(R.drawable.ic_available)
            }
        }
    }

    /**
     * FR-2.2.3: Bind analytics information (views, interactions, favorites, created date)
     */
    private fun bindAnalyticsInfo(holder: ListingViewHolder, listing: Listing) {
        // Views and interactions
        holder.tvViews.text = "${listing.views} views"
        holder.tvInteractions.text = "${listing.interactions} chats"

        // Favorites count (placeholder for future implementation)
        val favoritesCount = 0 // TODO: Implement favorites tracking
        holder.tvFavorites.text = "$favoritesCount saves"

        // Created date
        try {
            val createdDate = listing.createdAt.toDate()
            holder.tvCreatedDate.text = "Created: ${dateFormat.format(createdDate)}"
        } catch (e: Exception) {
            holder.tvCreatedDate.text = "Created: Unknown"
        }
    }

    /**
     * Bind feature indicators (negotiable, photo count, GPS location)
     */
    private fun bindFeatureIndicators(holder: ListingViewHolder, listing: Listing, context: android.content.Context) {
        // Negotiable indicator
        if (listing.isNegotiable) {
            holder.chipNegotiable.visibility = View.VISIBLE
            holder.chipNegotiable.text = "💰 Negotiable"
        } else {
            holder.chipNegotiable.visibility = View.GONE
        }

        // Photo count badge
        if (listing.imageUrls.size > 1) {
            holder.tvPhotoCount.text = "📷 ${listing.imageUrls.size}"
            holder.tvPhotoCount.visibility = View.VISIBLE
        } else {
            holder.tvPhotoCount.visibility = View.GONE
        }

        // GPS location indicator
        if (listing.latitude != null && listing.longitude != null) {
            holder.ivLocationIndicator.visibility = View.VISIBLE
            holder.ivLocationIndicator.setColorFilter(context.getColor(R.color.blue_600))
        } else {
            holder.ivLocationIndicator.visibility = View.GONE
        }
    }

    /**
     * Bind performance indicators based on listing engagement
     */
    private fun bindPerformanceIndicators(holder: ListingViewHolder, listing: Listing, context: android.content.Context) {
        val totalEngagement = listing.views + listing.interactions
        val isOldListing = try {
            listing.createdAt.toDate().time < (System.currentTimeMillis() - DAYS_OLD_THRESHOLD)
        } catch (e: Exception) {
            false
        }

        when {
            totalEngagement > HIGH_PERFORMANCE_THRESHOLD -> {
                // High performance - trending up
                holder.ivPerformanceIndicator.visibility = View.VISIBLE
                holder.ivPerformanceIndicator.setImageResource(R.drawable.ic_trending_up)
                holder.ivPerformanceIndicator.setColorFilter(context.getColor(R.color.green_600))
                holder.ivPerformanceIndicator.contentDescription = "High performing listing"
            }
            totalEngagement > GOOD_PERFORMANCE_THRESHOLD -> {
                // Good performance - fire icon
                holder.ivPerformanceIndicator.visibility = View.VISIBLE
                holder.ivPerformanceIndicator.setImageResource(R.drawable.ic_fire)
                holder.ivPerformanceIndicator.setColorFilter(context.getColor(R.color.orange_600))
                holder.ivPerformanceIndicator.contentDescription = "Good performing listing"
            }
            listing.views == 0 && isOldListing -> {
                // Old listing with no views - needs attention
                holder.ivPerformanceIndicator.visibility = View.VISIBLE
                holder.ivPerformanceIndicator.setImageResource(R.drawable.ic_warning)
                holder.ivPerformanceIndicator.setColorFilter(context.getColor(R.color.red_400))
                holder.ivPerformanceIndicator.contentDescription = "Listing needs attention"
            }
            else -> {
                holder.ivPerformanceIndicator.visibility = View.GONE
            }
        }
    }

    /**
     * Load listing image using Glide with proper error handling
     */
    private fun loadListingImage(holder: ListingViewHolder, listing: Listing) {
        if (listing.imageUrls.isNotEmpty()) {
            Glide.with(holder.ivListingImage.context)
                .load(listing.imageUrls[0])
                .centerCrop()
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_error)
                .fallback(R.drawable.ic_image_placeholder)
                .into(holder.ivListingImage)
        } else {
            holder.ivListingImage.setImageResource(R.drawable.ic_image_placeholder)
        }
    }

    /**
     * Setup click listeners for various actions
     */
    private fun setupClickListeners(holder: ListingViewHolder, listing: Listing) {
        // Options menu
        holder.btnOptions.setOnClickListener {
            showOptionsMenu(it, listing)
        }

        // Quick edit button (if available)
        holder.btnQuickEdit?.setOnClickListener {
            onActionClick(listing, "edit")
        }

        // Full item click for viewing details
        holder.itemView.setOnClickListener {
            onActionClick(listing, "view_details")
        }

        // Long click for quick actions
        holder.itemView.setOnLongClickListener {
            showOptionsMenu(holder.btnOptions, listing)
            true
        }
    }

    /**
     * Setup accessibility descriptions for better user experience
     */
    private fun setupAccessibility(holder: ListingViewHolder, listing: Listing) {
        holder.itemView.contentDescription = buildString {
            append("Listing: ${listing.title}, ")
            append("Price: ₫${String.format("%,.0f", listing.price)}, ")
            append("Status: ${listing.status}, ")
            append("Category: ${listing.category}, ")
            append("Condition: ${listing.condition}, ")
            append("Views: ${listing.views}, ")
            append("Interactions: ${listing.interactions}")
            if (listing.isNegotiable) append(", Price negotiable")
            if (listing.latitude != null && listing.longitude != null) append(", Has location")
        }
    }

    /**
     * Handle error state when binding fails
     */
    private fun bindErrorState(holder: ListingViewHolder, listing: Listing) {
        holder.tvTitle.text = listing.title.takeIf { it.isNotEmpty() } ?: "Error loading listing"
        holder.tvPrice.text = "₫${String.format("%,.0f", listing.price)}"
        holder.tvStatus.text = listing.status
        holder.tvViews.text = "Error"
        holder.tvInteractions.text = "Error"
        holder.ivListingImage.setImageResource(R.drawable.ic_image_error)
    }

    /**
     * FR-2.2.1: Show options menu for listing actions
     */
    private fun showOptionsMenu(view: View, listing: Listing) {
        val popup = PopupMenu(view.context, view)

        try {
            popup.menuInflater.inflate(R.menu.listing_options_menu, popup.menu)

            // Customize menu based on listing status
            val menu = popup.menu
            when (listing.status) {
                ListingStatus.SOLD -> {
                    menu.findItem(R.id.action_change_status)?.title = "🔄 Mark as Available"
                }
                ListingStatus.PAUSED -> {
                    menu.findItem(R.id.action_change_status)?.title = "🔄 Resume Listing"
                }
                else -> {
                    menu.findItem(R.id.action_change_status)?.title = "🔄 Change Status"
                }
            }

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_edit -> {
                        onActionClick(listing, "edit")
                        true
                    }
                    R.id.action_change_status -> {
                        onActionClick(listing, "toggle_status")
                        true
                    }
                    R.id.action_view_analytics -> {
                        onActionClick(listing, "view_analytics")
                        true
                    }
                    R.id.action_delete -> {
                        onActionClick(listing, "delete")
                        true
                    }
                    else -> false
                }
            }

            popup.show()
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to basic actions if menu fails
            onActionClick(listing, "options_error")
        }
    }

    override fun getItemCount(): Int = listings.size

    /**
     * Update the listings data and refresh the RecyclerView
     */
    fun updateListings(newListings: List<Listing>) {
        val oldSize = listings.size
        listings.clear()
        listings.addAll(newListings)

        // Use more efficient notification methods
        when {
            oldSize == 0 && newListings.isNotEmpty() -> {
                notifyItemRangeInserted(0, newListings.size)
            }
            oldSize > 0 && newListings.isEmpty() -> {
                notifyItemRangeRemoved(0, oldSize)
            }
            else -> {
                notifyDataSetChanged() // Fallback for complex changes
            }
        }
    }

    /**
     * Add a single listing to the adapter
     */
    fun addListing(listing: Listing, position: Int = 0) {
        listings.add(position, listing)
        notifyItemInserted(position)
    }

    /**
     * Remove a listing from the adapter
     */
    fun removeListing(position: Int) {
        if (position in 0 until listings.size) {
            listings.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    /**
     * Update a specific listing
     */
    fun updateListing(position: Int, updatedListing: Listing) {
        if (position in 0 until listings.size) {
            listings[position] = updatedListing
            notifyItemChanged(position)
        }
    }

    /**
     * Get listing at specific position
     */
    fun getListingAt(position: Int): Listing? {
        return if (position in 0 until listings.size) listings[position] else null
    }

    /**
     * Clear all listings
     */
    fun clearListings() {
        val oldSize = listings.size
        listings.clear()
        notifyItemRangeRemoved(0, oldSize)
    }
}