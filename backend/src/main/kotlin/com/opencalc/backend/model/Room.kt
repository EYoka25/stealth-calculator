package com.opencalc.backend.model

import kotlinx.serialization.Serializable

@Serializable
data class Room(
    val id: String,
    val hasPassword: Boolean,
    val participantCount: Int = 0,
    val createdAt: Long
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

@Serializable
data class RoomCreateRequest(
    val roomId: String,
    val passwordHash: String? = null,
    val maxParticipants: Int = 50
)
