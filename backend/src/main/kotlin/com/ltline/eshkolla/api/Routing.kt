package com.ltline.eshkolla.api

import com.ltline.eshkolla.auth.AuthService
import com.ltline.eshkolla.repository.AbsenceRepository
import com.ltline.eshkolla.repository.GradeRepository
import com.ltline.eshkolla.repository.PostgresAbsenceRepository
import com.ltline.eshkolla.repository.PostgresGradeRepository
import com.ltline.eshkolla.repository.PostgresStudentRepository
import com.ltline.eshkolla.repository.PostgresTeacherClassRepository
import com.ltline.eshkolla.repository.StudentRepository
import com.ltline.eshkolla.repository.TeacherClassRepository
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
private val studentRepository: StudentRepository = PostgresStudentRepository()
private val gradeRepository: GradeRepository = PostgresGradeRepository()
private val absenceRepository: AbsenceRepository = PostgresAbsenceRepository()
private val teacherClassRepository: TeacherClassRepository = PostgresTeacherClassRepository()

fun Application.configureRouting() {
    routing {
        get("/health") { call.respond(HealthResponse("ok", "eShkolla API", "0.1.0")) }
        routeApiV1()
    }
}

private fun io.ktor.server.routing.Route.routeApiV1() {
    route("/api/v1") {
        post("/auth/login") {
            val request = call.receive<LoginRequest>()
            if (request.username.isBlank() || request.password.isBlank()) { call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR", "Përdoruesi dhe fjalëkalimi janë të detyrueshëm.")); return@post }
            val result = authService.login(request.username.trim(), request.password)
            if (result == null) { call.respond(HttpStatusCode.Unauthorized, ApiError("INVALID_CREDENTIALS", "Të dhënat e kyçjes nuk janë të sakta.")); return@post }
            call.respond(LoginResponse(result.first, result.second))
        }
        post("/auth/logout") { bearerToken()?.let(authService::logout); call.respond(HttpStatusCode.NoContent) }
        get("/auth/me") { val user = requireUser() ?: return@get; call.respond(user) }

        get("/classes") {
            val user = requireUser() ?: return@get
            val teacherId = if (user.role == "MESIMDHENES") authService.teacherIdFor(bearerToken().orEmpty()) else call.request.queryParameters["teacherId"]?.trim()
            if (user.role == "MESIMDHENES" && teacherId == null) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Profili i mësimdhënësit nuk është i lidhur.")); return@get }
            val allowed = teacherId?.let(::classesForTeacher)
            val classes = studentRepository.findAll().groupBy { it.classId }
                .filter { allowed == null || it.key in allowed }
                .map { (classId, students) -> SchoolClassDto(classId, "Klasa $classId", classId.removePrefix("C").toIntOrNull()?.plus(4) ?: 8, teacherId ?: "M001", students.count { it.isActive }) }
                .sortedBy { it.id }
            call.respond(classes)
        }
        get("/classes/{id}/students") {
            val user = requireUser() ?: return@get
            val classId = call.parameters["id"].orEmpty()
            if (user.role == "MESIMDHENES") {
                val teacherId = authService.teacherIdFor(bearerToken().orEmpty())
                if (teacherId == null || !teacherHasClass(teacherId, classId)) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Kjo klasë nuk është pjesë e ngarkesës së mësimdhënësit.")); return@get }
            }
            call.respond(studentRepository.findAll().filter { it.classId == classId && it.isActive })
        }
        get("/students") {
            val user = requireUser() ?: return@get
            val allowed = if (user.role == "MESIMDHENES") authService.teacherIdFor(bearerToken().orEmpty())?.let(::classesForTeacher) else null
            val search = call.request.queryParameters["search"]?.trim()?.lowercase().orEmpty()
            val active = call.request.queryParameters["active"]?.toBooleanStrictOrNull()
            val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20
            val all = studentRepository.findAll().filter { (allowed == null || it.classId in allowed) && (search.isBlank() || it.fullName.lowercase().contains(search) || it.id.lowercase().contains(search)) && (active == null || it.isActive == active) }
            val from = ((page - 1) * pageSize).coerceAtMost(all.size); val to = (from + pageSize).coerceAtMost(all.size)
            call.respond(PagedResponse(all.subList(from, to), page, pageSize, all.size))
        }
        get("/students/{id}") {
            val user = requireUser() ?: return@get
            val student = studentRepository.findById(call.parameters["id"].orEmpty())
            if (student == null) { call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Nxënësi nuk u gjet.")); return@get }
            if (user.role == "MESIMDHENES") {
                val teacherId = authService.teacherIdFor(bearerToken().orEmpty())
                if (teacherId == null || student.classId !in classesForTeacher(teacherId)) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Ky nxënës nuk është pjesë e ngarkesës së mësimdhënësit.")); return@get }
            }
            call.respond(student)
        }
        post("/students") {
            requireWriteUser() ?: return@post
            val request = call.receive<StudentRequest>(); validateStudent(request)?.let { call.respond(HttpStatusCode.BadRequest, it); return@post }
            val student = StudentDto("NX-${UUID.randomUUID().toString().take(8).uppercase()}", request.fullName.trim(), request.classId.trim(), request.birthDate.trim(), request.isActive)
            call.respond(HttpStatusCode.Created, studentRepository.save(student))
        }
        put("/students/{id}") {
            requireWriteUser() ?: return@put
            val id = call.parameters["id"].orEmpty(); if (studentRepository.findById(id) == null) { call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Nxënësi nuk u gjet.")); return@put }
            val request = call.receive<StudentRequest>(); validateStudent(request)?.let { call.respond(HttpStatusCode.BadRequest, it); return@put }
            call.respond(studentRepository.save(StudentDto(id, request.fullName.trim(), request.classId.trim(), request.birthDate.trim(), request.isActive)))
        }
        delete("/students/{id}") {
            requireWriteUser() ?: return@delete
            if (studentRepository.delete(call.parameters["id"].orEmpty())) call.respond(HttpStatusCode.NoContent) else call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Nxënësi nuk u gjet."))
        }
        get("/grades") {
            val user = requireUser() ?: return@get
            val teacherId = if (user.role == "MESIMDHENES") authService.teacherIdFor(bearerToken().orEmpty()) else call.request.queryParameters["teacherId"]?.trim()
            if (user.role == "MESIMDHENES" && teacherId == null) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Profili i mësimdhënësit nuk është i lidhur.")); return@get }
            val classId = call.request.queryParameters["classId"]?.trim(); val studentId = call.request.queryParameters["studentId"]?.trim()
            if (user.role == "MESIMDHENES" && classId != null && !teacherHasClass(teacherId!!, classId)) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Nuk keni qasje në këtë klasë.")); return@get }
            val studentIds = classId?.let { value -> studentRepository.findAll().filter { it.classId == value }.map { it.id }.toSet() }
            call.respond(gradeRepository.findAll(studentId, teacherId).filter { studentIds == null || it.studentId in studentIds })
        }
        post("/grades") {
            val user = requireWriteUser() ?: return@post; val request = call.receive<GradeRequest>(); validateGrade(request)?.let { call.respond(HttpStatusCode.BadRequest, it); return@post }
            val teacherId = if (user.role == "MESIMDHENES") authService.teacherIdFor(bearerToken().orEmpty()) else request.teacherId
            if (teacherId == null) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Profili i mësimdhënësit nuk është i lidhur.")); return@post }
            val student = studentRepository.findById(request.studentId)
            if (student == null) { call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Nxënësi nuk u gjet.")); return@post }
            if (user.role == "MESIMDHENES" && !teacherHasClass(teacherId, student.classId)) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Nuk mund të regjistroni vlerësim për këtë nxënës.")); return@post }
            val grade = GradeDto("G-${UUID.randomUUID().toString().take(8).uppercase()}", request.studentId, request.subjectId, teacherId, request.value, request.period.trim(), request.academicYear.trim(), request.note?.trim()?.takeIf { it.isNotBlank() })
            call.respond(HttpStatusCode.Created, gradeRepository.save(grade))
        }
        put("/grades/{id}") {
            val user = requireWriteUser() ?: return@put
            val id = call.parameters["id"].orEmpty()
            val existing = gradeRepository.findById(id)
            if (existing == null) { call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Nota nuk u gjet.")); return@put }
            val request = call.receive<GradeRequest>(); validateGrade(request)?.let { call.respond(HttpStatusCode.BadRequest, it); return@put }
            val teacherId = if (user.role == "MESIMDHENES") authService.teacherIdFor(bearerToken().orEmpty()) else request.teacherId
            if (teacherId == null || (user.role == "MESIMDHENES" && existing.teacherId != teacherId)) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Nuk mund të ndryshoni këtë notë.")); return@put }
            val student = studentRepository.findById(request.studentId)
            if (student == null) { call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Nxënësi nuk u gjet.")); return@put }
            if (user.role == "MESIMDHENES" && !teacherHasClass(teacherId, student.classId)) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Nuk mund të ndryshoni notë për këtë nxënës.")); return@put }
            call.respond(gradeRepository.save(GradeDto(id, request.studentId, request.subjectId, teacherId, request.value, request.period.trim(), request.academicYear.trim(), request.note?.trim()?.takeIf { it.isNotBlank() })))
        }
        delete("/grades/{id}") {
            val user = requireWriteUser() ?: return@delete
            val id = call.parameters["id"].orEmpty(); val existing = gradeRepository.findById(id)
            if (existing == null) { call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Nota nuk u gjet.")); return@delete }
            if (user.role == "MESIMDHENES" && existing.teacherId != authService.teacherIdFor(bearerToken().orEmpty())) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Nuk mund të fshini këtë notë.")); return@delete }
            if (gradeRepository.delete(id)) call.respond(HttpStatusCode.NoContent) else call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Nota nuk u gjet."))
        }
        get("/absences") {
            val user = requireUser() ?: return@get
            val teacherId = if (user.role == "MESIMDHENES") authService.teacherIdFor(bearerToken().orEmpty()) else call.request.queryParameters["teacherId"]?.trim()
            if (user.role == "MESIMDHENES" && teacherId == null) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Profili i mësimdhënësit nuk është i lidhur.")); return@get }
            val studentId = call.request.queryParameters["studentId"]?.trim()
            if (user.role == "MESIMDHENES" && studentId != null) {
                val student = studentRepository.findById(studentId)
                if (student == null || !teacherHasClass(teacherId!!, student.classId)) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Nuk keni qasje te ky nxënës.")); return@get }
            }
            call.respond(absenceRepository.findAll(studentId, teacherId))
        }
        post("/absences") {
            val user = requireWriteUser() ?: return@post; val request = call.receive<AbsenceRequest>(); validateAbsence(request)?.let { call.respond(HttpStatusCode.BadRequest, it); return@post }
            val teacherId = if (user.role == "MESIMDHENES") authService.teacherIdFor(bearerToken().orEmpty()) else request.teacherId
            if (teacherId == null) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Profili i mësimdhënësit nuk është i lidhur.")); return@post }
            val student = studentRepository.findById(request.studentId)
            if (student == null) { call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Nxënësi nuk u gjet.")); return@post }
            if (user.role == "MESIMDHENES" && !teacherHasClass(teacherId, student.classId)) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Nuk mund të regjistroni mungesë për këtë nxënës.")); return@post }
            val absence = AbsenceDto("A-${UUID.randomUUID().toString().take(8).uppercase()}", request.studentId, request.subjectId, teacherId, request.date.trim(), request.status.trim(), request.note?.trim()?.takeIf { it.isNotBlank() })
            call.respond(HttpStatusCode.Created, absenceRepository.save(absence))
        }
        put("/absences/{id}") {
            val user = requireWriteUser() ?: return@put
            val id = call.parameters["id"].orEmpty(); val existing = absenceRepository.findById(id)
            if (existing == null) { call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Mungesa nuk u gjet.")); return@put }
            val request = call.receive<AbsenceRequest>(); validateAbsence(request)?.let { call.respond(HttpStatusCode.BadRequest, it); return@put }
            val teacherId = if (user.role == "MESIMDHENES") authService.teacherIdFor(bearerToken().orEmpty()) else request.teacherId
            if (teacherId == null || (user.role == "MESIMDHENES" && existing.teacherId != teacherId)) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Nuk mund të ndryshoni këtë mungesë.")); return@put }
            val student = studentRepository.findById(request.studentId)
            if (student == null) { call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Nxënësi nuk u gjet.")); return@put }
            if (user.role == "MESIMDHENES" && !teacherHasClass(teacherId, student.classId)) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Nuk mund të ndryshoni mungesë për këtë nxënës.")); return@put }
            call.respond(absenceRepository.save(AbsenceDto(id, request.studentId, request.subjectId, teacherId, request.date.trim(), request.status.trim(), request.note?.trim()?.takeIf { it.isNotBlank() })))
        }
        delete("/absences/{id}") {
            val user = requireWriteUser() ?: return@delete
            val id = call.parameters["id"].orEmpty(); val existing = absenceRepository.findById(id)
            if (existing == null) { call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Mungesa nuk u gjet.")); return@delete }
            if (user.role == "MESIMDHENES" && existing.teacherId != authService.teacherIdFor(bearerToken().orEmpty())) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Nuk mund të fshini këtë mungesë.")); return@delete }
            if (absenceRepository.delete(id)) call.respond(HttpStatusCode.NoContent) else call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Mungesa nuk u gjet."))
        }
    }
}

private fun teacherHasClass(teacherId: String, classId: String): Boolean = teacherClassRepository.isAssigned(teacherId, classId)
private fun classesForTeacher(teacherId: String): Set<String> = teacherClassRepository.getClassIdsForTeacher(teacherId)
private suspend fun io.ktor.server.application.ApplicationCall.requireUser(): UserDto? { val user = authService.userFor(bearerToken().orEmpty()); if (user == null) respond(HttpStatusCode.Unauthorized, ApiError("UNAUTHORIZED", "Kyçja është e nevojshme.")); return user }
private suspend fun io.ktor.server.application.ApplicationCall.requireWriteUser(): UserDto? { val user = requireUser() ?: return null; if (user.role !in setOf("ADMINISTRATOR", "DREJTOR", "MESIMDHENES")) { respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Nuk keni të drejtë për këtë veprim.")); return null }; return user }
private fun io.ktor.server.application.ApplicationCall.bearerToken(): String? = request.headers["Authorization"]?.removePrefix("Bearer ")?.takeIf { it.isNotBlank() }
private fun validateStudent(r: StudentRequest): ApiError? = when { r.fullName.trim().length < 2 -> ApiError("VALIDATION_ERROR", "Emri i nxënësit është shumë i shkurtër."); r.classId.trim().isBlank() -> ApiError("VALIDATION_ERROR", "Klasa është e detyrueshme."); r.birthDate.trim().isBlank() -> ApiError("VALIDATION_ERROR", "Datëlindja është e detyrueshme."); else -> null }
private fun validateGrade(r: GradeRequest): ApiError? = when { r.studentId.isBlank() -> ApiError("VALIDATION_ERROR", "Nxënësi është i detyrueshëm."); r.subjectId.isBlank() -> ApiError("VALIDATION_ERROR", "Lënda është e detyrueshme."); r.teacherId.isBlank() -> ApiError("VALIDATION_ERROR", "Mësimdhënësi është i detyrueshëm."); r.value !in 1..5 -> ApiError("VALIDATION_ERROR", "Nota duhet të jetë nga 1 deri në 5."); r.period.isBlank() -> ApiError("VALIDATION_ERROR", "Periudha është e detyrueshme."); r.academicYear.isBlank() -> ApiError("VALIDATION_ERROR", "Viti shkollor është i detyrueshëm."); else -> null }
private fun validateAbsence(r: AbsenceRequest): ApiError? = when { r.studentId.isBlank() -> ApiError("VALIDATION_ERROR", "Nxënësi është i detyrueshëm."); r.subjectId.isBlank() -> ApiError("VALIDATION_ERROR", "Lënda është e detyrueshme."); r.teacherId.isBlank() -> ApiError("VALIDATION_ERROR", "Mësimdhënësi është i detyrueshëm."); r.date.isBlank() -> ApiError("VALIDATION_ERROR", "Data është e detyrueshme."); r.status !in setOf("E_PAAFTESUAR", "E_ARSYESHME") -> ApiError("VALIDATION_ERROR", "Statusi i mungesës nuk është i vlefshëm."); else -> null }
