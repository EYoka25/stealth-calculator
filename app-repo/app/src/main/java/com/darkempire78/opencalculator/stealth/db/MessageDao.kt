package com.darkempire78.opencalculator.stealth.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.darkempire78.opencalculator.stealth.model.LocalMessageEntity

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE roomId = :roomId ORDER BY timestamp ASC")
    suspend fun getMessagesForRoom(roomId: String): List<LocalMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: LocalMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<LocalMessageEntity>)

    @Query("UPDATE messages SET statusTick = :status WHERE messageId = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: Int)

    @Query("DELETE FROM messages WHERE roomId = :roomId")
    suspend fun clearRoomMessages(roomId: String)

    @Query("SELECT * FROM messages WHERE roomId = :roomId AND isImported = 1 ORDER BY timestamp ASC")
    suspend fun getImportedMessagesForRoom(roomId: String): List<LocalMessageEntity>

    @Query("DELETE FROM messages WHERE messageId = :messageId")
    suspend fun deleteMessage(messageId: String)
}
