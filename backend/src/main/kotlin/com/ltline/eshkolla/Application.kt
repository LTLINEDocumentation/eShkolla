package com.ltline.eshkolla

import com.ltline.eshkolla.api.configureManagementApi
import com.ltline.eshkolla.api.configureRoleApi
import com.ltline.eshkolla.api.configureRouting
import com.ltline.eshkolla.api.configureSchoolOperationsApi
import com.ltline.eshkolla.api.configureStatusPages
import com.ltline.eshkolla.auth.AuthService
import com.ltline.eshkolla.db.Database
import com.ltline.eshkolla.db.SchoolOperationsMigration
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.http.content.staticFiles
import io.ktor.server.response.respondFile
import io.ktor.server.routing.get
import java.io.File
import kotlinx.serialization.json.Json

fun main() {
    embeddedServer(factory = Netty, port = System.getenv("PORT")?.toIntOrNull() ?: 8080, host = System.getenv("HOST") ?: "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    Database.initialize()
    Database.connection().use(SchoolOperationsMigration::run)
    install(CallLogging)
    install(ContentNegotiation) {
        json(Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true })
    }
    configureStatusPages()
    configureRouting()
    val authService = AuthService()
    configureRoleApi(authService)
    configureManagementApi(authService)
    configureSchoolOperationsApi(authService)
    routing {
        get("/") { call.respondFile(File("/app/web/index.html")) }
        staticFiles("/", File("/app/web"))
    }
}
