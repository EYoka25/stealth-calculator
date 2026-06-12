package com.opencalc.backend.plugins

import com.opencalc.backend.model.ChatMessagePayload
import com.opencalc.backend.model.DeliveryReceipt
import com.opencalc.backend.service.ConnectionRegistry
import com.opencalc.backend.service.JwtService
import com.opencalc.backend.service.RoomService
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

fun Application.configureWebSockets() {
    val roomService = RoomService()

    routing {
        webSocket("/chat/{roomId}") {
            val roomId = call.parameters["roomId"] ?: run {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Room ID required"))
                return@webSocket
            }

            // Validate token from query parameter or header
            val token = call.request.queryParameters["token"]
                ?: call.request.headers["Authorization"]?.removePrefix("Bearer ")

            if (token == null || JwtService.verifyToken(token) != roomId) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid token"))
                return@webSocket
            }

            val sessionId = UUID.randomUUID().toString()
            println("WebSocket connected: room=$roomId, session=$sessionId")

            // Register connection
            ConnectionRegistry.register(roomId, sessionId, this)

            try {
                // Send any offline messages
                val offlineMessages = roomService.getOfflineMessages(roomId)
                offlineMessages.forEach { msg ->
                    send(Frame.Text(Json.encodeToString(msg)))
                }

                // Listen for incoming messages
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            try {
                                val text = frame.readText()
                                val payload = Json.decodeFromString<ChatMessagePayload>(text)

                                // Validate room
                                if (payload.roomId != roomId) continue

                                // Persist message
                                roomService.persistMessage(payload)

                                // Broadcast to all participants
                                ConnectionRegistry.broadcast(roomId, payload, excludeSession = sessionId)

                                // Send delivery receipt back to sender
                                val receipt = DeliveryReceipt(
                                    messageId = payload.messageId,
                                    statusTick = 1, // Delivered
                                    timestamp = System.currentTimeMillis()
                                )
                                send(Frame.Text(Json.encodeToString(receipt)))

                            } catch (e: Exception) {
                                println("Error processing WebSocket message: ${e.message}")
                            }
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                println("WebSocket error: ${e.message}")
            } finally {
                ConnectionRegistry.unregister(roomId, sessionId)
                println("WebSocket disconnected: room=$roomId, session=$sessionId")
            }
        }
    }
}
