package com.opencalc.backend.service

import com.opencalc.backend.db.MessagesTable
import com.opencalc.backend.db.OfflineMessageQueueTable
import com.opencalc.backend.db.RoomsTable
import com.opencalc.backend.model.ChatMessagePayload
import com.opencalc.backend.model.Room
import com.opencalc.backend.model.RoomAuthRequest
import com.opencalc.backend.model.RoomAuthResponse
import com.opencalc.backend.model.RoomCreateRequest
import io.ktor.server.config.ApplicationConfig
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.time.Instant
import java.util.UUID

class RoomService {

    fun authenticate(request: RoomAuthRequest): RoomAuthResponse {
        return transaction {
            val existingRoom = RoomsTable.select { RoomsTable.id eq request.roomId }.firstOrNull()

            if (existingRoom != null) {
                // Room exists - verify password
                val storedHash = existingRoom[RoomsTable.passwordHash]
                if (storedHash != null) {
                    if (request.passwordHash == null) {
                        return@transaction RoomAuthResponse(
                            success = false,
                            error = "Password required"
                        )
                    }
                    if (!BCrypt.checkpw(request.passwordHash, storedHash)) {
                        return@transaction RoomAuthResponse(
                            success = false,
                            error = "Invalid password"
                        )
                    }
                }

                // Generate JWT token
                val token = JwtService.generateToken(request.roomId)
                RoomAuthResponse(success = true, token = token)
            } else {
                // Create new room (auto-create on first join)
                val passwordHash = request.passwordHash?.let { BCrypt.hashpw(it, BCrypt.gensalt()) }

                RoomsTable.insert {
                    it[id] = request.roomId
                    it[RoomsTable.passwordHash] = passwordHash
                }

                val token = JwtService.generateToken(request.roomId)
                RoomAuthResponse(success = true, token = token)
            }
        }
    }

    fun getRoom(roomId: String): Room? {
        return transaction {
            RoomsTable.select { RoomsTable.id eq roomId }.firstOrNull()?.let { row ->
                Room(
                    id = row[RoomsTable.id],
                    hasPassword = row[RoomsTable.passwordHash] != null,
                    createdAt = row[RoomsTable.createdAt].toEpochMilli()
                )
            }
        }
    }

    fun storeOfflineMessage(roomId: String, payload: ChatMessagePayload) {
        transaction {
            OfflineMessageQueueTable.insert {
                it[OfflineMessageQueueTable.roomId] = roomId
                it[senderAlias] = payload.senderAlias
                it[payloadText] = payload.payloadText
                it[timestamp] = payload.timestamp
                it[mediaUrl] = payload.mediaUrl
            }
        }
    }

    fun getOfflineMessages(roomId: String): List<ChatMessagePayload> {
        return transaction {
            OfflineMessageQueueTable.select { OfflineMessageQueueTable.roomId eq roomId }
                .map { row ->
                    ChatMessagePayload(
                        messageId = "offline_${row[OfflineMessageQueueTable.id]}",
                        roomId = row[OfflineMessageQueueTable.roomId],
                        senderAlias = row[OfflineMessageQueueTable.senderAlias],
                        payloadText = row[OfflineMessageQueueTable.payloadText],
                        timestamp = row[OfflineMessageQueueTable.timestamp],
                        mediaUrl = row[OfflineMessageQueueTable.mediaUrl],
                        statusTick = 1
                    )
                }
                .also {
                    // Delete after retrieval
                    OfflineMessageQueueTable.deleteWhere { OfflineMessageQueueTable.roomId eq roomId }
                }
        }
    }

    fun persistMessage(payload: ChatMessagePayload) {
        transaction {
            MessagesTable.insert {
                it[messageId] = payload.messageId
                it[roomId] = payload.roomId
                it[senderAlias] = payload.senderAlias
                it[payloadText] = payload.payloadText
                it[timestamp] = payload.timestamp
                it[mediaUrl] = payload.mediaUrl
                it[statusTick] = payload.statusTick
            }
        }
    }
}
