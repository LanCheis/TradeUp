package com.example.tradeup.chat

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.*
import com.example.tradeup.R
import com.example.tradeup.data.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*

class ChatFragment : Fragment() {

    private lateinit var rvChats: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var layoutLoading: LinearLayout

    private val chatList = mutableListOf<com.example.tradeup.data.model.Chat>()
    private lateinit var chatListAdapter: ChatListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_chat_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        loadUserChats()
    }

    private fun initViews(view: View) {
        rvChats = view.findViewById(R.id.rvChats)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)
        layoutLoading = view.findViewById(R.id.layoutLoading)
    }

    private fun setupRecyclerView() {
        chatListAdapter = ChatListAdapter(chatList) { chat ->
            // Navigate to individual chat
            val fragment = MessageFragment.newInstance(
                chatId = chat.id,
                otherUserId = getOtherUserId(chat),
                otherUserName = getOtherUserName(chat),
                listingTitle = chat.listingTitle
            )

            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack(null)
                .commit()
        }

        rvChats.layoutManager = LinearLayoutManager(requireContext())
        rvChats.adapter = chatListAdapter
    }

    private fun loadUserChats() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        showLoading(true)

        FirebaseFirestore.getInstance()
            .collection("chats")
            .whereArrayContains("participants", currentUserId)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null || !isAdded) return@addSnapshotListener

                chatList.clear()
                snapshots?.forEach { doc ->
                    val chat = doc.toObject(com.example.tradeup.data.model.Chat::class.java)
                    chatList.add(chat)
                }

                chatListAdapter.notifyDataSetChanged()
                showLoading(false)

                if (chatList.isEmpty()) {
                    showEmpty(true)
                }
            }
    }

    private fun getOtherUserId(chat: com.example.tradeup.data.model.Chat): String {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        return chat.participants.find { it != currentUserId } ?: ""
    }

    private fun getOtherUserName(chat: com.example.tradeup.data.model.Chat): String {
        val otherUserId = getOtherUserId(chat)
        return chat.participantNames[otherUserId] ?: "Unknown User"
    }

    private fun showLoading(show: Boolean) {
        layoutLoading.visibility = if (show) View.VISIBLE else View.GONE
        rvChats.visibility = if (show) View.GONE else View.VISIBLE
        layoutEmpty.visibility = View.GONE
    }

    private fun showEmpty(show: Boolean) {
        layoutEmpty.visibility = if (show) View.VISIBLE else View.GONE
        rvChats.visibility = if (show) View.GONE else View.VISIBLE
        layoutLoading.visibility = View.GONE
    }
}