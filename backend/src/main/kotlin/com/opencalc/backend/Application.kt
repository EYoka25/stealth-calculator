package com.opencalc.backend

import com.opencalc.backend.db.DatabaseFactory
import com.opencalc.backend.plugins.configureRouting
import com.opencalc.backend.plugins.configureSerialization
import com.opencalc.backend.plugins.configureWebSockets
import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.websocket.*
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import java.time.Duration

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // Initialize database
    DatabaseFactory.init(environment.config)

    // Install CORS
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowCredentials = true
        anyHost()
    }

    // Install WebSockets
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(20)
        timeout = Duration.ofSeconds(60)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    // Plugin modules
    configureSerialization() // This handles ContentNegotiation setup safely
    configureRouting()
    configureWebSockets()
}