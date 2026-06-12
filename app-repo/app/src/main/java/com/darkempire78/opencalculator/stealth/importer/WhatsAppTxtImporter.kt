package com.darkempire78.opencalculator.stealth.importer

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.darkempire78.opencalculator.stealth.db.MessageDao
import com.darkempire78.opencalculator.stealth.model.LocalMessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

class WhatsAppTxtImporter(private val dao: MessageDao) {

    companion object {
        private const val TAG = "WhatsAppTxtImporter"

        // WhatsApp export format: [DD/MM/YYYY, HH:MM:SS] Sender: Message
        private val WHATSAPP_PATTERN =
            "^\\[(\\d{2}/\\d{2}/\\d{4}),\\s(\\d{2}:\\d{2}:\\d{2})\\]\\s([^:]+):\\s(.+)$".toRegex()

        private val DATE_FORMAT = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    }

    data class ImportProgress(
        val processedLines: Int,
        val importedMessages: Int,
        val isComplete: Boolean,
        val error: String? = null
    )

    suspend fun importFile(
        uri: Uri,
        roomId: String,
        contentResolver: ContentResolver,
        onProgress: (ImportProgress) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    var line: String?
                    var processedLines = 0
                    var importedMessages = 0
                    var lastParsedMessage: LocalMessageEntity? = null
                    val batch = mutableListOf<LocalMessageEntity>()

                    while (reader.readLine().also { line = it } != null) {
                        processedLines++
                        val currentLine = line ?: continue

                        val match = WHATSAPP_PATTERN.matchEntire(currentLine)

                        if (match != null) {
                            // Save previous message if exists
                            lastParsedMessage?.let {
                                batch.add(it)
                                importedMessages++
                            }

                            // Parse new message
                            val (dateStr, timeStr, senderName, textContent) = match.destructured
                            val timestamp = parseTimestamp(dateStr, timeStr)

                            lastParsedMessage = LocalMessageEntity(
                                messageId = UUID.randomUUID().toString(),
                                roomId = roomId,
                                senderAlias = senderName.trim(),
                                payloadText = textContent.trim(),
                                timestamp = timestamp,
                                statusTick = 2, // Imported messages are treated as read
                                isImported = true,
                                mediaLocalPath = null
                            )
                        } else if (lastParsedMessage != null) {
                            // Multi-line continuation - append to last message
                            lastParsedMessage = lastParsedMessage!!.copy(
                                payloadText = lastParsedMessage!!.payloadText + "\n" + currentLine.trim()
                            )
                        }

                        // Batch insert every 50 messages
                        if (batch.size >= 50) {
                            dao.insertMessages(batch)
                            batch.clear()
                            onProgress(ImportProgress(processedLines, importedMessages, false))
                        }
                    }

                    // Insert remaining messages
                    lastParsedMessage?.let {
                        batch.add(it)
                        importedMessages++
                    }
                    if (batch.isNotEmpty()) {
                        dao.insertMessages(batch)
                    }

                    onProgress(ImportProgress(processedLines, importedMessages, true))
                } ?: throw Exception("Cannot open input stream")
            } catch (e: Exception) {
                Log.e(TAG, "Import error", e)
                onProgress(ImportProgress(0, 0, true, e.message))
            }
        }
    }

    private fun parseTimestamp(dateStr: String, timeStr: String): Long {
        return try {
            DATE_FORMAT.parse("$dateStr $timeStr")?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
