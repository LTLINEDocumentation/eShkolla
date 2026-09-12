package com.ltline.eshkolla.api

import com.ltline.eshkolla.auth.AuthService
import com.ltline.eshkolla.repository.InMemoryStudentRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import java.util.UUID

private val authService = AuthService()
private val studentRepository = InMemoryStudentRepository()

fun Application.configureRouting() {
    routing {
        get("/health") {
            call.respond(HealthResponse("ok", "eShkolla API", "0.1.0"))
        }

        routeApiV1()
    }
}

private fun io.ktor.server.routing.Route.routeApiV1() {
    route("/api/v1") {
        post("/auth/login") {
            val request = call.receive<LoginRequest>()
            if (request.username.isBlank() || request.password.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR", "Përdoruesi dhe fjalëkalimi janë të detyrueshëm."))
                return@post
            }
            val result = authService.login(request.username.trim(), request.password)
            if (result == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiError("INVALID_CREDENTIALS", "Të dhënat e kyçjes nuk janë të sakta."))
                return@post
            }
            call.respond(LoginResponse(result.first, result.second))
        }

        post("/auth/logout") {
            bearerToken()?.let(authService::logout)
            call.respond(HttpStatusCode.NoContent)
        }

        get("/auth/me") {
            val user = requireUser() ?: return@get
            call.respond(user)
        }

        get("/students") {
            requireUser() ?: return@get
            val search = call.request.queryParameters["search"]?.trim()?.lowercase().orEmpty()
            val active = call.request.queryParameters["active"]?.toBooleanStrictOrNull()
            val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20
            val all = studentRepository.findAll().filter { student ->
                (search.isBlank() || student.fullName.lowercase().contains(search) || student.id.lowercase().contains(search)) &&
                    (active == null || student.isActive == active)
            }
            val from = ((page - 1) * pageSize).coerceAtMost(all.size)
            val to = (from + pageSize).coerceAtMost(all.size)
            call.respond(PagedResponse(all.subList(from, to), page, pageSize, all.size))
        }

        get("/students/{id}") {
            requireUser() ?: return@get
            val student = studentRepository.findById(call.parameters["id"].orEmpty())
            if (student == null) call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Nxënësi nuk u gjet."))
            else call.respond(student)
        }

        post("/students") {
            requireUser() ?: return@post
            val request = call.receive<StudentRequest>()
            validateStudent(request)?.let { error ->
                call.respond(HttpStatusCode.BadRequest, error)
                return@post
            }
            val student = StudentDto("NX-${UUID.randomUUID().toString().take(8).uppercase()}", request.fullName.trim(), request.classId.trim(), request.birthDate.trim(), true)
            call.respond(HttpStatusCode.Created, studentRepository.save(student))
        }

        put("/students/{id}") {
            requireUser() ?: return@put
            val id = call.parameters["id"].orEmpty()
            if (studentRepository.findById(id) == null) {
                call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Nxënësi nuk u gjet."))
                return@put
            }
            val request = call.receive<StudentRequest>()
            validateStudent(request)?.let { error ->
                call.respond(HttpStatusCode.BadRequest, error)
                return@put
            }
            call.respond(studentRepository.save(StudentDto(id, request.fullName.trim(), request.classId.trim(), request.birthDate.trim(), true)))
        }

        delete("/students/{id}") {
            requireUser() ?: return@delete
            if (studentRepository.delete(call.parameters["id"].orEmpty())) call.respond(HttpStatusCode.NoContent)
            else call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Nxënësi nuk u gjet."))
        }
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.requireUser(): UserDto? {
    val user = authService.userFor(bearerToken().orEmpty())
    if (user == null) respond(HttpStatusCode.Unauthorized, ApiError("UNAUTHORIZED", "Kyçja është e nevojshme."))
    return user
}

private fun io.ktor.server.application.ApplicationCall.bearerToken(): String? =
    request.headers["Authorization"]?.removePrefix("Bearer ")?.takeIf { it.isNotBlank() }

private fun validateStudent(request: StudentRequest): ApiError? = when {
    request.fullName.trim().length < 2 -> ApiError("VALIDATION_ERROR", "Emri i nxënësit është shumë i shkurtër.")
    request.classId.trim().isBlank() -> ApiError("VALIDATION_ERROR", "Klasa është e detyrueshme.")
    request.birthDate.trim().isBlank() -> ApiError("VALIDATION_ERROR", "Datëlindja është e detyrueshme.")
    else -> null
}
