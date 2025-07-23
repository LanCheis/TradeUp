package com.example.tradeup.utils

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import com.example.tradeup.R

class CustomRatingBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val stars = mutableListOf<ImageView>()
    private var rating: Float = 0f
    private var maxStars: Int = 5
    private var isIndicator: Boolean = false

    init {
        orientation = HORIZONTAL
        setupStars()
    }

    private fun setupStars() {
        removeAllViews()
        stars.clear()

        for (i in 0 until maxStars) {
            val star = ImageView(context).apply {
                setImageResource(R.drawable.ic_star_empty)
                setPadding(4, 4, 4, 4)
                layoutParams = LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT
                )
            }

            if (!isIndicator) {
                star.setOnClickListener {
                    setRating((i + 1).toFloat())
                }
            }

            stars.add(star)
            addView(star)
        }
    }

    fun setRating(rating: Float) {
        this.rating = rating
        updateStars()
    }

    fun getRating(): Float = rating

    fun setNumStars(numStars: Int) {
        this.maxStars = numStars
        setupStars()
        updateStars()
    }

    fun setIsIndicator(isIndicator: Boolean) {
        this.isIndicator = isIndicator
        setupStars()
    }

    private fun updateStars() {
        for (i in 0 until maxStars) {
            val star = stars[i]
            when {
                i < rating - 0.5f -> star.setImageResource(R.drawable.ic_star_filled)
                i < rating -> star.setImageResource(R.drawable.ic_star_half)
                else -> star.setImageResource(R.drawable.ic_star_empty)
            }
        }
    }
}