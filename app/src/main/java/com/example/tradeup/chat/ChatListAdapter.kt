package com.example.tradeup.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tradeup.R
import com.example.tradeup.data.model.Chat
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class ChatListAdapter(
    private val chats: List<Chat>,
    private val onChatClick: (Chat) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUserName: TextView = view.findViewById(R.id.tvUserName)
        val tvLastMessage: TextView = view.findViewById(R.id.tvLastMessage)
        val tvListingTitle: TextView = view.findViewById(R.id.tvListingTitle)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvUnreadCount: TextView = view.findViewById(R.id.tvUnreadCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chats[position]
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val otherUserId = chat.participants.find { it != currentUserId } ?: ""
        val otherUserName = chat.participantNames[otherUserId] ?: "Unknown User"

        holder.tvUserName.text = otherUserName
        holder.tvLastMessage.text = chat.lastMessage.ifEmpty { "No messages yet" }
        holder.tvListingTitle.text = "About: ${chat.listingTitle}"
        holder.tvTime.text = formatTime(chat.lastMessageTime)

        // Show unread count
        val unreadCount = chat.unreadCount[currentUserId] ?: 0
        if (unreadCount > 0) {
            holder.tvUnreadCount.visibility = View.VISIBLE
            holder.tvUnreadCount.text = unreadCount.toString()
        } else {
            holder.tvUnreadCount.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onChatClick(chat)
        }
    }

    override fun getItemCount() = chats.size

    private fun formatTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
        }
    }
}