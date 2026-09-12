package com.ltline.eshkolla.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            environment.log.error("Kërkesë API dështoi", cause)
            call.respond(HttpStatusCode.InternalServerError, ApiError("INTERNAL_ERROR", "Ndodhi një gabim i brendshëm."))
        }
    }
}
