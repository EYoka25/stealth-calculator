package com.opencalc.backend.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessagePayload(
    val messageId: String,
    val roomId: String,
    val senderAlias: String,
    val payloadText: String,
    val timestamp: Long,
    val mediaUrl: String? = null,
    val statusTick: Int = 0
)

@Serializable
data class DeliveryReceipt(
    val messageId: String,
    val statusTick: Int, // 1 = Delivered, 2 = Read
    val timestamp: Long
)

@Serializable
data class MediaUploadResponse(
    val url: String,
    val objectName: String
)

@Serializable
data class PresenceUpdate(
    val roomId: String,
    val alias: String,
    val status: String // "online", "offline", "typing"
)
