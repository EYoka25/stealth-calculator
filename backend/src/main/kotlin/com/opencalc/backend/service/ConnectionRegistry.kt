package com.opencalc.backend.service

import com.opencalc.backend.model.ChatMessagePayload
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

object ConnectionRegistry {

    // roomId -> (sessionId -> WebSocketSession)
    private val connections = ConcurrentHashMap<String, ConcurrentHashMap<String, WebSocketSession>>()

    fun register(roomId: String, sessionId: String, session: WebSocketSession) {
        connections.getOrPut(roomId) { ConcurrentHashMap() }[sessionId] = session
    }

    fun unregister(roomId: String, sessionId: String) {
        connections[roomId]?.remove(sessionId)
        if (connections[roomId]?.isEmpty() == true) {
            connections.remove(roomId)
        }
    }

    suspend fun broadcast(
        roomId: String,
        payload: ChatMessagePayload,
        excludeSession: String? = null
    ) {
        val roomConnections = connections[roomId] ?: return
        val json = Json.encodeToString(payload)
        val frame = Frame.Text(json)

        roomConnections.forEach { (sessionId, session) ->
            if (sessionId != excludeSession) {
                try {
                    session.send(frame)
                } catch (e: Exception) {
                    // Session might be closed, unregister it
                    unregister(roomId, sessionId)
                }
            }
        }
    }

    fun getParticipantCount(roomId: String): Int {
        return connections[roomId]?.size ?: 0
    }

    fun isRoomActive(roomId: String): Boolean {
        return connections.containsKey(roomId) && connections[roomId]?.isNotEmpty() == true
    }
}
