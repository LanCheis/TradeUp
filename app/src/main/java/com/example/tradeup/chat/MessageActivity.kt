package com.example.tradeup.chat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.tradeup.R

class MessageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)

        val chatId = intent.getStringExtra("chatId") ?: ""
        val otherUserId = intent.getStringExtra("otherUserId") ?: ""
        val otherUserName = intent.getStringExtra("otherUserName") ?: ""
        val listingTitle = intent.getStringExtra("listingTitle") ?: ""

        // Load the MessageFragment
        if (savedInstanceState == null) {
            val fragment = MessageFragment.newInstance(chatId, otherUserId, otherUserName, listingTitle)
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
        }
    }
}