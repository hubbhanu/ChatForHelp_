

package com.example.chatapplication

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ChatActivity : AppCompatActivity() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageBox: EditText
    private lateinit var sendButton: ImageView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageList: ArrayList<Message>
    private lateinit var mDbRef: DatabaseReference

    private var receiverRoom: String? = null
    private var senderRoom: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val name = intent.getStringExtra("name")
        val receiverUid = intent.getStringExtra("uid")
        val senderUid = FirebaseAuth.getInstance().currentUser?.uid

        if (senderUid == null || receiverUid == null) {
            Toast.makeText(this, "Invalid user IDs", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        mDbRef = FirebaseDatabase.getInstance().getReference()

        senderRoom = receiverUid + senderUid
        receiverRoom = senderUid + receiverUid

        supportActionBar?.title = name

        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        messageBox = findViewById(R.id.messageBox)
        sendButton = findViewById(R.id.sentButton)

        messageList = ArrayList()
        messageAdapter = MessageAdapter(this, messageList)

        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = messageAdapter

        val senderName = FirebaseAuth.getInstance().currentUser?.displayName ?: "Unknown"

        mDbRef.child("chats").child(senderRoom!!).child("messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messageList.clear()
                    for (postSnapshot in snapshot.children) {
                        val message = postSnapshot.getValue(Message::class.java)
                        if (message != null) {
                            messageList.add(message)
                        }
                    }
                    messageAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ChatActivity, "Failed to load messages", Toast.LENGTH_SHORT)
                        .show()
                }
            })

        sendButton.setOnClickListener {
            val messageText = messageBox.text.toString().trim()
            val senderName = FirebaseAuth.getInstance().currentUser?.displayName.orEmpty()


            if (messageText.isNotEmpty()) {
                val messageObject = Message(messageText, senderUid, senderName)

                // Save the message to both sender and receiver rooms
                val senderMessagesRef = mDbRef.child("chats").child(senderRoom!!).child("messages").push()
                val receiverMessagesRef = mDbRef.child("chats").child(receiverRoom!!).child("messages").push()

                senderMessagesRef.setValue(messageObject).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        receiverMessagesRef.setValue(messageObject)
                    } else {
                        Toast.makeText(this, "Message failed to send", Toast.LENGTH_SHORT).show()
                    }
                }

                messageBox.setText("")
            } else {
                Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

    }
}
