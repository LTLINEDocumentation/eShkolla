package com.ltline.eshkolla

import com.ltline.eshkolla.api.configureRouting
import com.ltline.eshkolla.api.configureStatusPages
import com.ltline.eshkolla.db.Database
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun main() {
    embeddedServer(factory = Netty, port = System.getenv("PORT")?.toIntOrNull() ?: 8080, host = System.getenv("HOST") ?: "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    Database.initialize()
    install(CallLogging)
    install(ContentNegotiation) {
        json(Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true })
    }
    configureStatusPages()
    configureRouting()
}
