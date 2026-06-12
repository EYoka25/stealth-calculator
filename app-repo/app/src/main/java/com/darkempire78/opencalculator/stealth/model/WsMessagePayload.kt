package com.darkempire78.opencalculator.stealth.model

import kotlinx.serialization.Serializable

@Serializable
data class WsMessagePayload(
    val messageId: String,
    val roomId: String,
    val senderAlias: String,
    val payloadText: String,
    val timestamp: Long,
    val mediaUrl: String? = null,
    val statusTick: Int = 0
)

@Serializable
data class RoomAuthRequest(
    val roomId: String,
    val passwordHash: String? = null
)

@Serializable
data class RoomAuthResponse(
    val success: Boolean,
    val token: String? = null,
    val error: String? = null
)
