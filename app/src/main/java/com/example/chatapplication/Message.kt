package com.example.chatapplication


data class Message(
    var message: String? = null,
    var senderId: String? = null,
    var senderName: String? = null // Add senderName field
)

