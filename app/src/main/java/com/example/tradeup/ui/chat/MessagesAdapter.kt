package com.example.tradeup.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.tradeup.R
import com.example.tradeup.data.model.Message
import com.google.firebase.auth.FirebaseAuth
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.*

class MessagesAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val messages = mutableListOf<Message>()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_sent, parent, false)
                SentMessageViewHolder(view)
            }
            VIEW_TYPE_RECEIVED -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_received, parent, false)
                ReceivedMessageViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message)
        }
    }

    override fun getItemCount(): Int = messages.size

    fun updateMessages(newMessages: List<Message>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    // ViewHolder for sent messages
    inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val ivMessageImage: ImageView = itemView.findViewById(R.id.ivMessageImage)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)

        fun bind(message: Message) {
            // FR-4.1.2: Display text content
            if (message.content.isNotEmpty()) {
                tvMessage.text = message.content
                tvMessage.visibility = View.VISIBLE
            } else {
                tvMessage.visibility = View.GONE
            }

            // FR-4.1.2: Display image if attached (simplified Glide usage)
            if (!message.imageUrl.isNullOrEmpty()) {
                ivMessageImage.visibility = View.VISIBLE
                Glide.with(itemView.context)
                    .load(message.imageUrl)
                    .apply(RequestOptions()
                        .placeholder(R.drawable.ic_camera)
                        .error(R.drawable.ic_camera)
                        .transform(RoundedCorners(16))
                    )
                    .into(ivMessageImage)
            } else {
                ivMessageImage.visibility = View.GONE
            }

            // Format and display timestamp
            tvTimestamp.text = formatTime(message.timestamp)
        }
    }

    // ViewHolder for received messages
    inner class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val ivMessageImage: ImageView = itemView.findViewById(R.id.ivMessageImage)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        private val ivSenderAvatar: CircleImageView = itemView.findViewById(R.id.ivSenderAvatar)

        fun bind(message: Message) {
            // FR-4.1.2: Display text content
            if (message.content.isNotEmpty()) {
                tvMessage.text = message.content
                tvMessage.visibility = View.VISIBLE
            } else {
                tvMessage.visibility = View.GONE
            }

            // FR-4.1.2: Display image if attached (simplified Glide usage)
            if (!message.imageUrl.isNullOrEmpty()) {
                ivMessageImage.visibility = View.VISIBLE
                Glide.with(itemView.context)
                    .load(message.imageUrl)
                    .apply(RequestOptions()
                        .placeholder(R.drawable.ic_camera)
                        .error(R.drawable.ic_camera)
                        .transform(RoundedCorners(16))
                    )
                    .into(ivMessageImage)
            } else {
                ivMessageImage.visibility = View.GONE
            }

            // Display sender avatar (simplified Glide usage)
            if (message.senderAvatar.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(message.senderAvatar)
                    .apply(RequestOptions()
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .circleCrop()
                    )
                    .into(ivSenderAvatar)
            } else {
                ivSenderAvatar.setImageResource(R.drawable.ic_person)
            }

            // Format and display timestamp
            tvTimestamp.text = formatTime(message.timestamp)
        }
    }

    private fun formatTime(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
}