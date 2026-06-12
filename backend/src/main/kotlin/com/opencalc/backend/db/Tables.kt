package com.opencalc.backend.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object RoomsTable : Table("rooms") {
    val id = varchar("id", 64)
    val passwordHash = varchar("password_hash", 256).nullable()
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val maxParticipants = integer("max_participants").default(50)

    override val primaryKey = PrimaryKey(id)
}

object MessagesTable : Table("messages") {
    val messageId = varchar("message_id", 64)
    val roomId = varchar("room_id", 64).references(RoomsTable.id)
    val senderAlias = varchar("sender_alias", 128)
    val payloadText = text("payload_text")
    val timestamp = long("timestamp")
    val mediaUrl = varchar("media_url", 512).nullable()
    val statusTick = integer("status_tick").default(0)

    override val primaryKey = PrimaryKey(messageId)
}

object OfflineMessageQueueTable : Table("offline_message_queue") {
    val id = integer("id").autoIncrement()
    val roomId = varchar("room_id", 64).references(RoomsTable.id)
    val senderAlias = varchar("sender_alias", 128)
    val payloadText = text("payload_text")
    val timestamp = long("timestamp")
    val mediaUrl = varchar("media_url", 512).nullable()

    override val primaryKey = PrimaryKey(id)
}
