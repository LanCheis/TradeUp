package com.example.tradeup.chat

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.*
import com.example.tradeup.R
import com.example.tradeup.data.model.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

class ChatFragment : Fragment() {

    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter
    private lateinit var db: FirebaseFirestore
    private var listenerRegistration: ListenerRegistration? = null
    private var chatRoomId: String = ""
    private var currentUserId: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupFirestore()
        setupRecyclerView()
        setupClickListeners()
        loadMessages()
    }

    private fun initViews(view: View) {
        rvMessages = view.findViewById(R.id.rvMessages)
        etMessage = view.findViewById(R.id.etMessage)
        btnSend = view.findViewById(R.id.btnSend)
    }

    private fun setupFirestore() {
        db = Firebase.firestore
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        chatRoomId = arguments?.getString("chatRoomId") ?: "general"
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter(messages, currentUserId)
        rvMessages.adapter = adapter
        rvMessages.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupClickListeners() {
        btnSend.setOnClickListener {
            sendMessage()
        }

        etMessage.setOnEditorActionListener { _, _, _ ->
            sendMessage()
            true
        }
    }

    private fun sendMessage() {
        val messageText = etMessage.text.toString().trim()
        if (messageText.isNotEmpty()) {
            val message = ChatMessage(
                id = UUID.randomUUID().toString(),
                senderId = currentUserId,
                senderName = FirebaseAuth.getInstance().currentUser?.displayName ?: "Anonymous",
                message = messageText,
                timestamp = System.currentTimeMillis()
            )

            db.collection("chatRooms")
                .document(chatRoomId)
                .collection("messages")
                .document(message.id)
                .set(message)
                .addOnSuccessListener {
                    etMessage.text.clear()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Failed to send message: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadMessages() {
        listenerRegistration = db.collection("chatRooms")
            .document(chatRoomId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(requireContext(), "Listen failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                messages.clear()
                for (doc in snapshots!!) {
                    val message = doc.toObject(ChatMessage::class.java)
                    messages.add(message)
                }
                adapter.notifyDataSetChanged()
                scrollToBottom()
            }
    }

    private fun scrollToBottom() {
        if (messages.isNotEmpty()) {
            rvMessages.scrollToPosition(messages.size - 1)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerRegistration?.remove()
    }
}