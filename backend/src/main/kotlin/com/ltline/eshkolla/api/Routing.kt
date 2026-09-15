package com.ltline.eshkolla.api

import com.ltline.eshkolla.auth.AuthService
import com.ltline.eshkolla.repository.InMemoryAbsenceRepository
import com.ltline.eshkolla.repository.InMemoryGradeRepository
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
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.util.UUID

private val authService = AuthService()
private val studentRepository = InMemoryStudentRepository()
private val gradeRepository = InMemoryGradeRepository()
private val absenceRepository = InMemoryAbsenceRepository()

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
            requireWriteUser() ?: return@post
            val request = call.receive<StudentRequest>()
            validateStudent(request)?.let { error ->
                call.respond(HttpStatusCode.BadRequest, error)
                return@post
            }
            val student = StudentDto("NX-${UUID.randomUUID().toString().take(8).uppercase()}", request.fullName.trim(), request.classId.trim(), request.birthDate.trim(), true)
            call.respond(HttpStatusCode.Created, studentRepository.save(student))
        }

        put("/students/{id}") {
            requireWriteUser() ?: return@put
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
            call.respond(HttpStatusCode.OK, studentRepository.save(StudentDto(id, request.fullName.trim(), request.classId.trim(), request.birthDate.trim(), true)))
        }

        delete("/students/{id}") {
            requireWriteUser() ?: return@delete
            if (studentRepository.delete(call.parameters["id"].orEmpty())) call.respond(HttpStatusCode.NoContent)
            else call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Nxënësi nuk u gjet."))
        }

        get("/grades") {
            val user = requireUser() ?: return@get
            val requestedTeacher = call.request.queryParameters["teacherId"]?.trim()
            val teacherId = if (user.role == "MESIMDHENES") "M001" else requestedTeacher
            val classId = call.request.queryParameters["classId"]?.trim()
            val studentId = call.request.queryParameters["studentId"]?.trim()
            val studentIds = classId?.let { classValue ->
                studentRepository.findAll().filter { it.classId == classValue }.map { it.id }.toSet()
            }
            val grades = gradeRepository.findAll(studentId, teacherId).filter { grade ->
                studentIds == null || grade.studentId in studentIds
            }
            call.respond(grades)
        }

        post("/grades") {
            val user = requireWriteUser() ?: return@post
            val request = call.receive<GradeRequest>()
            validateGrade(request)?.let { error ->
                call.respond(HttpStatusCode.BadRequest, error)
                return@post
            }
            if (user.role == "MESIMDHENES" && request.teacherId != "M001") {
                call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Mësimdhënësi mund të regjistrojë vetëm vlerësimet e veta."))
                return@post
            }
            if (studentRepository.findById(request.studentId) == null) {
                call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Nxënësi nuk u gjet."))
                return@post
            }
            val grade = GradeDto("G-${UUID.randomUUID().toString().take(8).uppercase()}", request.studentId, request.subjectId, request.teacherId, request.value, request.period.trim(), request.academicYear.trim(), request.note?.trim()?.takeIf { it.isNotBlank() })
            call.respond(HttpStatusCode.Created, gradeRepository.save(grade))
        }

        get("/absences") {
            val user = requireUser() ?: return@get
            val requestedTeacher = call.request.queryParameters["teacherId"]?.trim()
            val teacherId = if (user.role == "MESIMDHENES") "M001" else requestedTeacher
            call.respond(absenceRepository.findAll(call.request.queryParameters["studentId"]?.trim(), teacherId))
        }

        post("/absences") {
            val user = requireWriteUser() ?: return@post
            val request = call.receive<AbsenceRequest>()
            validateAbsence(request)?.let { error ->
                call.respond(HttpStatusCode.BadRequest, error)
                return@post
            }
            if (user.role == "MESIMDHENES" && request.teacherId != "M001") {
                call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Mësimdhënësi mund të regjistrojë vetëm mungesat e veta."))
                return@post
            }
            if (studentRepository.findById(request.studentId) == null) {
                call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Nxënësi nuk u gjet."))
                return@post
            }
            val absence = AbsenceDto("A-${UUID.randomUUID().toString().take(8).uppercase()}", request.studentId, request.subjectId, request.teacherId, request.date.trim(), request.status.trim(), request.note?.trim()?.takeIf { it.isNotBlank() })
            call.respond(HttpStatusCode.Created, absenceRepository.save(absence))
        }
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.requireUser(): UserDto? {
    val user = authService.userFor(bearerToken().orEmpty())
    if (user == null) respond(HttpStatusCode.Unauthorized, ApiError("UNAUTHORIZED", "Kyçja është e nevojshme."))
    return user
}

private suspend fun io.ktor.server.application.ApplicationCall.requireWriteUser(): UserDto? {
    val user = requireUser() ?: return null
    if (user.role !in setOf("ADMINISTRATOR", "DREJTOR", "MESIMDHENES")) {
        respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Nuk keni të drejtë për këtë veprim."))
        return null
    }
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

private fun validateGrade(request: GradeRequest): ApiError? = when {
    request.studentId.isBlank() -> ApiError("VALIDATION_ERROR", "Nxënësi është i detyrueshëm.")
    request.subjectId.isBlank() -> ApiError("VALIDATION_ERROR", "Lënda është e detyrueshme.")
    request.teacherId.isBlank() -> ApiError("VALIDATION_ERROR", "Mësimdhënësi është i detyrueshëm.")
    request.value !in 1..5 -> ApiError("VALIDATION_ERROR", "Nota duhet të jetë nga 1 deri në 5.")
    request.period.isBlank() -> ApiError("VALIDATION_ERROR", "Periudha është e detyrueshme.")
    request.academicYear.isBlank() -> ApiError("VALIDATION_ERROR", "Viti shkollor është i detyrueshëm.")
    else -> null
}

private fun validateAbsence(request: AbsenceRequest): ApiError? = when {
    request.studentId.isBlank() -> ApiError("VALIDATION_ERROR", "Nxënësi është i detyrueshëm.")
    request.subjectId.isBlank() -> ApiError("VALIDATION_ERROR", "Lënda është e detyrueshme.")
    request.teacherId.isBlank() -> ApiError("VALIDATION_ERROR", "Mësimdhënësi është i detyrueshëm.")
    request.date.isBlank() -> ApiError("VALIDATION_ERROR", "Data është e detyrueshme.")
    request.status !in setOf("E_PAAFTESUAR", "E_ARSYESHME") -> ApiError("VALIDATION_ERROR", "Statusi i mungesës nuk është i vlefshëm.")
    else -> null
}
