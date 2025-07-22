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
import java.util.*

class MessageFragment : Fragment() {

    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var tvToolbarTitle: TextView
    private lateinit var btnBack: ImageButton

    private val messages = mutableListOf<Message>()
    private lateinit var messageAdapter: MessageAdapter

    private var chatId: String = ""
    private var otherUserId: String = ""
    private var otherUserName: String = ""
    private var listingTitle: String = ""

    companion object {
        fun newInstance(chatId: String, otherUserId: String, otherUserName: String, listingTitle: String): MessageFragment {
            val fragment = MessageFragment()
            val args = Bundle().apply {
                putString("chatId", chatId)
                putString("otherUserId", otherUserId)
                putString("otherUserName", otherUserName)
                putString("listingTitle", listingTitle)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            chatId = it.getString("chatId", "")
            otherUserId = it.getString("otherUserId", "")
            otherUserName = it.getString("otherUserName", "")
            listingTitle = it.getString("listingTitle", "")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_messages, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupToolbar()
        setupRecyclerView()
        setupSendMessage()
        loadMessages()
    }

    private fun initViews(view: View) {
        rvMessages = view.findViewById(R.id.rvMessages)
        etMessage = view.findViewById(R.id.etMessage)
        btnSend = view.findViewById(R.id.btnSend)
        tvToolbarTitle = view.findViewById(R.id.tvToolbarTitle)
        btnBack = view.findViewById(R.id.btnBack)
    }

    private fun setupToolbar() {
        tvToolbarTitle.text = otherUserName
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(messages)
        rvMessages.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        rvMessages.adapter = messageAdapter
    }

    private fun setupSendMessage() {
        btnSend.setOnClickListener {
            val messageText = etMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
                etMessage.text.clear()
            }
        }
    }

    private fun sendMessage(content: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        val message = Message(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = currentUser.uid,
            senderName = currentUser.displayName ?: "User",
            receiverId = otherUserId,
            content = content,
            timestamp = System.currentTimeMillis()
        )

        // Add to Firestore
        FirebaseFirestore.getInstance()
            .collection("messages")
            .document(message.id)
            .set(message)
            .addOnSuccessListener {
                updateChatLastMessage(content)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to send message", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateChatLastMessage(lastMessage: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val updates = mapOf(
            "lastMessage" to lastMessage,
            "lastMessageTime" to System.currentTimeMillis(),
            "lastSenderId" to currentUserId
        )

        FirebaseFirestore.getInstance()
            .collection("chats")
            .document(chatId)
            .update(updates)
    }

    private fun loadMessages() {
        FirebaseFirestore.getInstance()
            .collection("messages")
            .whereEqualTo("chatId", chatId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null || !isAdded) return@addSnapshotListener

                messages.clear()
                snapshots?.forEach { doc ->
                    val message = doc.toObject(Message::class.java)
                    messages.add(message)
                }

                messageAdapter.notifyDataSetChanged()
                if (messages.isNotEmpty()) {
                    rvMessages.scrollToPosition(messages.size - 1)
                }
            }
    }
}