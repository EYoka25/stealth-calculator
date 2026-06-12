package com.darkempire78.opencalculator.stealth.network

import android.content.Context
import android.util.Log
import com.darkempire78.opencalculator.stealth.SessionManager
import com.darkempire78.opencalculator.stealth.db.AppDatabase
import com.darkempire78.opencalculator.stealth.model.ChatMessage
import com.darkempire78.opencalculator.stealth.model.LocalMessageEntity
import com.darkempire78.opencalculator.stealth.model.WsMessagePayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class ChatRepository(context: Context) {

    companion object {
        private const val TAG = "ChatRepository"
    }

    private val sessionManager = SessionManager(context)
    val db = AppDatabase.getInstance(context)
    private val dao = db.messageDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var ktorClient: KtorClient? = null

    private val _messagesFlow = MutableSharedFlow<List<ChatMessage>>(replay = 1)
    val messagesFlow: Flow<List<ChatMessage>> = _messagesFlow.asSharedFlow()

    private val _newMessageFlow = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 64)
    val newMessageFlow: Flow<ChatMessage> = _newMessageFlow.asSharedFlow()

    private var currentRoomId: String? = null
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5

    fun init(serverUrl: String) {
        ktorClient = KtorClient(serverUrl)
    }

    suspend fun authenticate(roomId: String, password: String?, alias: String): Result<String> {
        return try {
            val client = ktorClient ?: return Result.failure(Exception("Client not initialized"))
            val result = client.authenticateRoom(roomId, password)
            result.fold(
                onSuccess = { response ->
                    val token = response.token ?: return Result.failure(Exception("No token"))
                    sessionManager.saveSession(roomId, token, alias)
                    currentRoomId = roomId
                    Result.success(token)
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Auth error", e)
            Result.failure(e)
        }
    }

    fun connectWebSocket() {
        val roomId = currentRoomId ?: sessionManager.getCurrentRoomId() ?: return
        val token = sessionManager.isAuthenticated().let { if (it) sessionManager.getCurrentRoomId() else null } ?: return
        currentRoomId = roomId

        scope.launch {
            try {
                ktorClient?.connectWebSocket(roomId, token ?: "")
                reconnectAttempts = 0
            } catch (e: Exception) {
                Log.e(TAG, "WebSocket connect error", e)
                handleReconnect()
            }
        }

        scope.launch {
            ktorClient?.incomingMessages?.receiveAsFlow()?.collect { payload ->
                handleIncomingMessage(payload)
            }
        }
    }

    private suspend fun handleIncomingMessage(payload: WsMessagePayload) {
        val entity = LocalMessageEntity(
            messageId = payload.messageId,
            roomId = payload.roomId,
            senderAlias = payload.senderAlias,
            payloadText = payload.payloadText,
            timestamp = payload.timestamp,
            statusTick = payload.statusTick,
            isImported = false,
            mediaLocalPath = payload.mediaUrl
        )
        dao.insertMessage(entity)

        val chatMessage = ChatMessage(
            messageId = payload.messageId,
            roomId = payload.roomId,
            senderAlias = payload.senderAlias,
            payloadText = payload.payloadText,
            timestamp = payload.timestamp,
            isOutgoing = payload.senderAlias == sessionManager.getSenderAlias(),
            statusTick = payload.statusTick,
            mediaUrl = payload.mediaUrl
        )
        _newMessageFlow.emit(chatMessage)
        loadMessagesForRoom(payload.roomId)
    }

    private suspend fun handleReconnect() {
        if (reconnectAttempts < maxReconnectAttempts) {
            reconnectAttempts++
            delay(5000L * reconnectAttempts)
            connectWebSocket()
        }
    }

    suspend fun sendMessage(text: String, mediaUrl: String? = null): ChatMessage? {
        val roomId = currentRoomId ?: return null
        val alias = sessionManager.getSenderAlias()
        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val payload = WsMessagePayload(
            messageId = messageId,
            roomId = roomId,
            senderAlias = alias,
            payloadText = text,
            timestamp = timestamp,
            mediaUrl = mediaUrl,
            statusTick = 0
        )

        // Save locally first
        val entity = LocalMessageEntity(
            messageId = messageId,
            roomId = roomId,
            senderAlias = alias,
            payloadText = text,
            timestamp = timestamp,
            statusTick = 0,
            isImported = false,
            mediaLocalPath = mediaUrl
        )
        dao.insertMessage(entity)

        // Send over WebSocket
        ktorClient?.sendWebSocketMessage(payload)

        val chatMessage = ChatMessage(
            messageId = messageId,
            roomId = roomId,
            senderAlias = alias,
            payloadText = text,
            timestamp = timestamp,
            isOutgoing = true,
            statusTick = 0,
            mediaUrl = mediaUrl
        )
        _newMessageFlow.emit(chatMessage)
        return chatMessage
    }

    suspend fun loadMessagesForRoom(roomId: String): List<ChatMessage> {
        return withContext(Dispatchers.IO) {
            val entities = dao.getMessagesForRoom(roomId)
            val alias = sessionManager.getSenderAlias()
            val messages = entities.map { entity ->
                ChatMessage(
                    messageId = entity.messageId,
                    roomId = entity.roomId,
                    senderAlias = entity.senderAlias,
                    payloadText = entity.payloadText,
                    timestamp = entity.timestamp,
                    isOutgoing = entity.senderAlias == alias,
                    statusTick = entity.statusTick,
                    mediaLocalPath = entity.mediaLocalPath
                )
            }
            _messagesFlow.emit(messages)
            messages
        }
    }

    suspend fun importMessages(messages: List<LocalMessageEntity>) {
        dao.insertMessages(messages)
        currentRoomId?.let { loadMessagesForRoom(it) }
    }

    suspend fun uploadMedia(file: File): Result<String> {
        val token = sessionManager.isAuthenticated().let { if (it) "token" else null }
            ?: return Result.failure(Exception("Not authenticated"))
        return ktorClient?.uploadMedia(file, token)?.fold(
            onSuccess = { Result.success(it.url) },
            onFailure = { Result.failure(it) }
        ) ?: Result.failure(Exception("Client not initialized"))
    }

    fun disconnect() {
        scope.launch {
            ktorClient?.disconnectWebSocket()
        }
    }

    fun cleanup() {
        disconnect()
        scope.cancel()
        ktorClient?.close()
    }
}
