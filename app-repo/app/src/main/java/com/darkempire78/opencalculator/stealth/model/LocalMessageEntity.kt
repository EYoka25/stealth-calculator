package com.darkempire78.opencalculator.stealth.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [Index(value = ["roomId"])]
)
data class LocalMessageEntity(
    @PrimaryKey val messageId: String,
    val roomId: String,
    val senderAlias: String,
    val payloadText: String,
    val timestamp: Long,
    val statusTick: Int,
    val isImported: Boolean,
    val mediaLocalPath: String? = null
)
