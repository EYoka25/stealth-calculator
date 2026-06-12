package com.darkempire78.opencalculator.stealth.network

import android.util.Log
import com.darkempire78.opencalculator.stealth.model.MediaUploadResponse
import com.darkempire78.opencalculator.stealth.model.RoomAuthRequest
import com.darkempire78.opencalculator.stealth.model.RoomAuthResponse
import com.darkempire78.opencalculator.stealth.model.WsMessagePayload
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

class KtorClient(private val baseUrl: String) {

    companion object {
        private const val TAG = "KtorClient"
    }

    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(WebSockets) {
            pingIntervalMillis = 20000
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 15000
        }
        install(Logging) {
            logger = io.ktor.client.plugins.logging.Logger.ANDROID
            level = LogLevel.INFO
        }
    }

    private var wsSession: WebSocketSession? = null
    val incomingMessages = Channel<WsMessagePayload>(Channel.BUFFERED)

    suspend fun authenticateRoom(roomId: String, password: String? = null): Result<RoomAuthResponse> {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/auth/room") {
                contentType(ContentType.Application.Json)
                setBody(RoomAuthRequest(roomId, password))
            }
            val body = response.body<RoomAuthResponse>()
            if (body.success) {
                Result.success(body)
            } else {
                Result.failure(Exception(body.error ?: "Authentication failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Auth error", e)
            Result.failure(e)
        }
    }

    suspend fun connectWebSocket(roomId: String, token: String) {
        try {
            wsSession = client.webSocketSession(
                host = baseUrl.replace("http://", "").replace("https://", "").split(":").first(),
                port = baseUrl.split(":").lastOrNull()?.toIntOrNull() ?: 8080,
                path = "/chat/$roomId"
            ) {
                header("Authorization", "Bearer $token")
            }

            wsSession?.incoming?.consumeAsFlow()
                ?.filterIsInstance<Frame.Text>()
                ?.map { frame ->
                    try {
                        Json.decodeFromString<WsMessagePayload>(frame.readText())
                    } catch (e: Exception) {
                        null
                    }
                }
                ?.collect { payload ->
                    payload?.let { incomingMessages.send(it) }
                }
        } catch (e: Exception) {
            Log.e(TAG, "WebSocket error", e)
        }
    }

    suspend fun sendWebSocketMessage(payload: WsMessagePayload) {
        try {
            val json = Json.encodeToString(WsMessagePayload.serializer(), payload)
            wsSession?.send(Frame.Text(json))
        } catch (e: Exception) {
            Log.e(TAG, "Send error", e)
        }
    }

    suspend fun disconnectWebSocket() {
        try {
            wsSession?.close()
            wsSession = null
        } catch (e: Exception) {
            Log.e(TAG, "Disconnect error", e)
        }
    }

    suspend fun uploadMedia(file: File, token: String): Result<MediaUploadResponse> {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/media/upload") {
                header("Authorization", "Bearer $token")
                setBody(MultiPartFormDataContent(
                    formData {
                        append("file", file.readBytes(), Headers.build {
                            append(HttpHeaders.ContentType, "image/webp")
                            append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                        })
                    }
                ))
            }
            Result.success(response.body<MediaUploadResponse>())
        } catch (e: Exception) {
            Log.e(TAG, "Upload error", e)
            Result.failure(e)
        }
    }

    fun close() {
        incomingMessages.close()
        client.close()
    }
}
