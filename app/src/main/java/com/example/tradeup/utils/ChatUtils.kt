package com.example.tradeup.utils

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import com.example.tradeup.chat.ChatActivity

object ChatUtils {

    // FR-4.1.1: Helper to start chat from Activity
    fun startChat(
        context: Context,
        otherUserId: String,
        otherUserName: String,
        otherUserAvatar: String = ""
    ) {
        val intent = Intent(context, ChatActivity::class.java).apply {
            putExtra(ChatActivity.EXTRA_OTHER_USER_ID, otherUserId)
            putExtra(ChatActivity.EXTRA_OTHER_USER_NAME, otherUserName)
            putExtra(ChatActivity.EXTRA_OTHER_USER_AVATAR, otherUserAvatar)
        }
        context.startActivity(intent)
    }

    // FR-4.1.1: Helper to start chat from Fragment
    fun startChatFromFragment(
        fragment: Fragment,
        otherUserId: String,
        otherUserName: String,
        otherUserAvatar: String = ""
    ) {
        startChat(fragment.requireContext(), otherUserId, otherUserName, otherUserAvatar)
    }
}