package com.darkempire78.opencalculator.stealth.model

data class ChatMessage(
    val messageId: String,
    val roomId: String,
    val senderAlias: String,
    val payloadText: String,
    val timestamp: Long,
    val isOutgoing: Boolean,
    val statusTick: Int = 0,
    val mediaLocalPath: String? = null,
    val mediaUrl: String? = null
)
