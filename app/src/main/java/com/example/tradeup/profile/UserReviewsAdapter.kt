// File: app/src/main/java/com/example/tradeup/profile/UserReviewsAdapter.kt

package com.example.tradeup.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tradeup.R
import com.example.tradeup.data.model.UserRating
import com.example.tradeup.data.remote.UserRepository
import java.text.SimpleDateFormat
import java.util.*

class UserReviewsAdapter(private val reviews: List<UserRating>) :
    RecyclerView.Adapter<UserReviewsAdapter.ReviewViewHolder>() {

    inner class ReviewViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvReviewerName: TextView = view.findViewById(R.id.tvReviewerName)
        val ratingBar: RatingBar = view.findViewById(R.id.ratingBar)
        val tvComment: TextView = view.findViewById(R.id.tvComment)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_review, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviews[position]

        // ✅ FIXED: Load reviewer name using the correct callback signature
        UserRepository.getUserProfile(review.fromUserId) { success, userData ->
            if (success && userData != null) {
                val name = userData["name"] as? String ?: "Anonymous User"
                holder.tvReviewerName.text = name
            } else {
                holder.tvReviewerName.text = "Anonymous User"
            }
        }

        holder.ratingBar.rating = review.stars
        holder.tvComment.text = if (review.comment.isNotEmpty()) {
            review.comment
        } else {
            "No comment provided"
        }

        // Format date
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        holder.tvDate.text = dateFormat.format(Date(review.timestamp))
    }

    override fun getItemCount() = reviews.size
}