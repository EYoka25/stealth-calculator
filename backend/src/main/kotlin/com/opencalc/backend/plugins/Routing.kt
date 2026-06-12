package com.opencalc.backend.plugins

import com.opencalc.backend.model.MediaUploadResponse
import com.opencalc.backend.model.RoomAuthRequest
import com.opencalc.backend.service.RoomService
import com.opencalc.backend.service.StorageService
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
// Added the serialization import
import kotlinx.serialization.Serializable

// 🛡️ Safe structures that tell Kotlin exactly how to generate the JSON blueprints
@Serializable
data class ErrorResponse(val error: String)

@Serializable
data class HealthResponse(val status: String, val timestamp: Long)

fun Application.configureRouting() {
    val roomService = RoomService()
    val storageService = StorageService()

    routing {
        route("/api") {
            // Room authentication
            post("/auth/room") {
                val request = call.receive<RoomAuthRequest>()
                val response = roomService.authenticate(request)
                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.Unauthorized, response)
                }
            }

            // Get room info
            get("/room/{roomId}") {
                val roomId = call.parameters["roomId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Room ID required") // Fixed mapOf -> ErrorResponse
                )
                val room = roomService.getRoom(roomId)
                if (room != null) {
                    call.respond(HttpStatusCode.OK, room)
                } else {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Room not found")) // Fixed mapOf -> ErrorResponse
                }
            }

            // Media upload
            post("/media/upload") {
                val multipart = call.receiveMultipart()
                var fileName = ""
                var contentType = "application/octet-stream"
                var fileBytes: ByteArray? = null

                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FileItem -> {
                            fileName = part.originalFileName ?: "unknown"
                            contentType = part.contentType?.toString() ?: contentType
                            fileBytes = part.streamProvider().readBytes()
                        }
                        else -> {}
                    }
                    part.dispose()
                }

                if (fileBytes != null) {
                    val url = storageService.uploadFile(fileName, contentType, fileBytes!!)
                    call.respond(
                        HttpStatusCode.OK,
                        MediaUploadResponse(url = url, objectName = fileName)
                    )
                } else {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("No file provided")) // Fixed mapOf -> ErrorResponse
                }
            }

            // Health check
            get("/health") {
                // Fixed the Map<String, Any> crash by passing a dedicated HealthResponse object
                call.respond(
                    HttpStatusCode.OK, 
                    HealthResponse(status = "healthy", timestamp = System.currentTimeMillis())
                )
            }
        }
    }
}