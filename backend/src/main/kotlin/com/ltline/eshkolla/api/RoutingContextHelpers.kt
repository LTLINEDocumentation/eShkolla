package com.ltline.eshkolla.api

import com.ltline.eshkolla.auth.AuthService
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext

private val routingAuthService = AuthService()

internal fun RoutingContext.bearerToken(): String? =
    call.request.headers["Authorization"]?.removePrefix("Bearer ")?.takeIf { it.isNotBlank() }

internal suspend fun RoutingContext.requireUser(): UserDto? {
    val user = routingAuthService.userFor(bearerToken().orEmpty())
    if (user == null) {
        call.respond(HttpStatusCode.Unauthorized, ApiError("UNAUTHORIZED", "Kyçja është e nevojshme."))
        return null
    }
    return user
}

internal suspend fun RoutingContext.requireWriteUser(): UserDto? {
    val user = requireUser() ?: return null
    // Drejtori nuk shkruan drejtpërdrejt nota/mungesa në endpointet operative.
    // Veprimi i Drejtorit për këto të dhëna kalon përmes rrjedhës së njoftimit te Mësimdhënësi.
    if (user.role !in setOf("ADMINISTRATOR", "MESIMDHENES")) {
        call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Ky veprim nuk kryhet drejtpërdrejt nga ky rol."))
        return null
    }
    return user
}
