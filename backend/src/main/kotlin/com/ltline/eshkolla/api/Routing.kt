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
            val teacherId = when (user.role) {
                "MESIMDHENES" -> authService.teacherIdFor(bearerToken().orEmpty())
                "ADMINISTRATOR", "DREJTOR" -> call.request.queryParameters["teacherId"]?.trim()
                else -> null
            }
            if (user.role == "MESIMDHENES" && teacherId == null) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Profili i mësimdhënësit nuk është i lidhur.")); return@get }
            val allowed = teacherId?.let(::classesForTeacher)
            val linkedStudents = if (user.role in setOf("PRIND", "NXENES")) linkedStudentIds(user.id, user.role) else null
            val visibleStudents = studentRepository.findAll().filter { (allowed == null || it.classId in allowed) && (linkedStudents == null || it.id in linkedStudents) }
            val visibleClassIds = visibleStudents.map { it.classId }.toSet()
            val studentCounts = visibleStudents.filter { it.isActive }.groupingBy { it.classId }.eachCount()
            val classes = com.ltline.eshkolla.db.Database.connection().use { c ->
                c.prepareStatement("SELECT c.id,c.name,c.grade_level,COALESCE((SELECT tc.teacher_id FROM teacher_classes tc WHERE tc.class_id=c.id ORDER BY tc.teacher_id LIMIT 1),'') AS teacher_id FROM classes c WHERE c.active=TRUE ORDER BY c.grade_level,c.name").use { p ->
                    p.executeQuery().use { rs -> buildList {
                        while (rs.next()) {
                            val classId = rs.getString("id")
                            if ((allowed == null || classId in allowed) && (linkedStudents == null || classId in visibleClassIds)) {
                                add(SchoolClassDto(classId, rs.getString("name"), rs.getInt("grade_level"), teacherId ?: rs.getString("teacher_id"), studentCounts[classId] ?: 0))
                            }
                        }
                    } }
                }
            }
            call.respond(classes)
        }
        get("/classes/{id}/students") {
            val user = requireUser() ?: return@get
            val classId = call.parameters["id"].orEmpty()
            if (user.role == "MESIMDHENES") {
                val teacherId = authService.teacherIdFor(bearerToken().orEmpty())
                if (teacherId == null || !teacherHasClass(teacherId, classId)) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Kjo klasë nuk është pjesë e ngarkesës së mësimdhënësit.")); return@get }
            }
            val linkedStudents = if (user.role in setOf("PRIND", "NXENES")) linkedStudentIds(user.id, user.role) else null
            call.respond(studentRepository.findAll().filter { it.classId == classId && it.isActive && (linkedStudents == null || it.id in linkedStudents) })
        }
        get("/students") {
            val user = requireUser() ?: return@get
            val allowed = if (user.role == "MESIMDHENES") authService.teacherIdFor(bearerToken().orEmpty())?.let(::classesForTeacher) else null
            val linkedStudents = if (user.role in setOf("PRIND", "NXENES")) linkedStudentIds(user.id, user.role) else null
            val search = call.request.queryParameters["search"]?.trim()?.lowercase().orEmpty()
            val active = call.request.queryParameters["active"]?.toBooleanStrictOrNull()
            val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20
            val all = studentRepository.findAll().filter { (allowed == null || it.classId in allowed) && (linkedStudents == null || it.id in linkedStudents) && (search.isBlank() || it.fullName.lowercase().contains(search) || it.id.lowercase().contains(search)) && (active == null || it.isActive == active) }
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
            if (user.role in setOf("PRIND", "NXENES") && student.id !in linkedStudentIds(user.id, user.role)) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Nuk keni qasje te të dhënat e këtij nxënësi.")); return@get }
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
            val classStudentIds = classId?.let { value -> studentRepository.findAll().filter { it.classId == value }.map { it.id }.toSet() }
            val linkedStudents = if (user.role in setOf("PRIND", "NXENES")) linkedStudentIds(user.id, user.role) else null
            call.respond(gradeRepository.findAll(studentId, teacherId).filter { (classStudentIds == null || it.studentId in classStudentIds) && (linkedStudents == null || it.studentId in linkedStudents) })
        }
        post("/grades") {
            val user = requireWriteUser() ?: return@post
            if (user.role == "DREJTOR") { call.respond(HttpStatusCode.Forbidden, ApiError("DIRECTOR_MUST_PROPOSE", "Drejtori duhet ta dërgojë notën si propozim për aprovim nga mësimdhënësi i lëndës.")); return@post }
            val request = call.receive<GradeRequest>(); validateGrade(request)?.let { call.respond(HttpStatusCode.BadRequest, it); return@post }
            val teacherId = if (user.role == "MESIMDHENES") authService.teacherIdFor(bearerToken().orEmpty()) else request.teacherId
            if (teacherId == null) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Profili i mësimdhënësit nuk është i lidhur.")); return@post }
            val student = studentRepository.findById(request.studentId)
            if (student == null) { call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Nxënësi nuk u gjet.")); return@post }
            if (user.role == "MESIMDHENES" && !teacherHasSubjectInClass(teacherId, request.subjectId, student.classId)) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Nuk keni caktim për këtë lëndë dhe klasë.")); return@post }
            val grade = GradeDto("G-${UUID.randomUUID().toString().take(8).uppercase()}", request.studentId, request.subjectId, teacherId, request.value, request.period.trim(), request.academicYear.trim(), request.note?.trim()?.takeIf { it.isNotBlank() })
            val saved = gradeRepository.save(grade)
            NotificationService.notifyStudentContacts(user.id, teacherId, request.studentId, "Nota u regjistrua", "U regjistrua nota ${request.value} për nxënësin ${student.fullName} në lëndën ${request.subjectId}.", "GRADE", saved.id)
            call.respond(HttpStatusCode.Created, saved)
        }
        put("/grades/{id}") {
            val user = requireWriteUser() ?: return@put
            if (user.role == "DREJTOR") { call.respond(HttpStatusCode.Forbidden, ApiError("DIRECTOR_MUST_PROPOSE", "Drejtori duhet ta dërgojë ndryshimin si propozim për aprovim nga mësimdhënësi i lëndës.")); return@put }
            val id = call.parameters["id"].orEmpty()
            val existing = gradeRepository.findById(id)
            if (existing == null) { call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Nota nuk u gjet.")); return@put }
            val request = call.receive<GradeRequest>(); validateGrade(request)?.let { call.respond(HttpStatusCode.BadRequest, it); return@put }
            if (user.role == "MESIMDHENES" && (request.studentId != existing.studentId || request.subjectId != existing.subjectId)) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Mësimdhënësi mund të ndryshojë vlerën dhe shënimin, por jo ta zhvendosë notën te nxënës ose lëndë tjetër.")); return@put }
            val teacherId = if (user.role == "MESIMDHENES") authService.teacherIdFor(bearerToken().orEmpty()) else request.teacherId
            if (teacherId == null || (user.role == "MESIMDHENES" && existing.teacherId != teacherId)) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Nuk mund të ndryshoni këtë notë.")); return@put }
            val student = studentRepository.findById(request.studentId)
            if (student == null) { call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Nxënësi nuk u gjet.")); return@put }
            if (user.role == "MESIMDHENES" && !teacherHasSubjectInClass(teacherId, request.subjectId, student.classId)) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Nuk keni caktim për këtë lëndë dhe klasë.")); return@put }
            val saved = gradeRepository.save(GradeDto(id, request.studentId, request.subjectId, teacherId, request.value, request.period.trim(), request.academicYear.trim(), request.note?.trim()?.takeIf { it.isNotBlank() }))
            NotificationService.notifyStudentContacts(user.id, teacherId, request.studentId, "Nota u ndryshua", "Nota për nxënësin ${student.fullName} në lëndën ${request.subjectId} u ndryshua në ${request.value}.", "GRADE", saved.id)
            call.respond(saved)
        }
        delete("/grades/{id}") {
            val user = requireWriteUser() ?: return@delete
            if (user.role != "ADMINISTRATOR" && user.role != "MESIMDHENES") { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Vetëm administratori ose mësimdhënësi përgjegjës mund ta fshijë notën.")); return@delete }
            val id = call.parameters["id"].orEmpty(); val existing = gradeRepository.findById(id)
            if (existing == null) { call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Nota nuk u gjet.")); return@delete }
            if (user.role == "MESIMDHENES" && existing.teacherId != authService.teacherIdFor(bearerToken().orEmpty())) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Nuk mund të fshini këtë notë.")); return@delete }
            if (gradeRepository.delete(id)) {
                val student = studentRepository.findById(existing.studentId)
                NotificationService.notifyStudentContacts(user.id, existing.teacherId, existing.studentId, "Nota u fshi", "Nota ${existing.value} për nxënësin ${student?.fullName ?: existing.studentId} në lëndën ${existing.subjectId} u fshi.", "GRADE", existing.id)
                call.respond(HttpStatusCode.NoContent)
            } else call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Nota nuk u gjet."))
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
            val linkedStudents = if (user.role in setOf("PRIND", "NXENES")) linkedStudentIds(user.id, user.role) else null
            call.respond(absenceRepository.findAll(studentId, teacherId).filter { linkedStudents == null || it.studentId in linkedStudents })
        }
        post("/absences") {
            val user = requireWriteUser() ?: return@post; val request = call.receive<AbsenceRequest>(); validateAbsence(request)?.let { call.respond(HttpStatusCode.BadRequest, it); return@post }
            val teacherId = if (user.role == "MESIMDHENES") authService.teacherIdFor(bearerToken().orEmpty()) else request.teacherId
            if (teacherId == null) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Profili i mësimdhënësit nuk është i lidhur.")); return@post }
            val student = studentRepository.findById(request.studentId)
            if (student == null) { call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Nxënësi nuk u gjet.")); return@post }
            if (user.role == "MESIMDHENES" && !teacherHasClass(teacherId, student.classId)) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Nuk mund të regjistroni mungesë për këtë nxënës.")); return@post }
            val absence = AbsenceDto("A-${UUID.randomUUID().toString().take(8).uppercase()}", request.studentId, request.subjectId, teacherId, request.date.trim(), request.status.trim(), request.note?.trim()?.takeIf { it.isNotBlank() })
            val saved = absenceRepository.save(absence)
            NotificationService.notifyStudentContacts(user.id, teacherId, request.studentId, "U regjistrua mungesa", "U regjistrua mungesa për nxënësin ${student.fullName} më ${request.date}. Statusi: ${request.status}.", "ABSENCE", saved.id)
            call.respond(HttpStatusCode.Created, saved)
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
            val saved = absenceRepository.save(AbsenceDto(id, request.studentId, request.subjectId, teacherId, request.date.trim(), request.status.trim(), request.note?.trim()?.takeIf { it.isNotBlank() }))
            NotificationService.notifyStudentContacts(user.id, teacherId, request.studentId, "Mungesa u ndryshua", "Evidenca e mungesës për nxënësin ${student.fullName} më ${request.date} u ndryshua. Statusi: ${request.status}.", "ABSENCE", saved.id)
            call.respond(saved)
        }
        delete("/absences/{id}") {
            val user = requireWriteUser() ?: return@delete
            val id = call.parameters["id"].orEmpty(); val existing = absenceRepository.findById(id)
            if (existing == null) { call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Mungesa nuk u gjet.")); return@delete }
            if (user.role == "MESIMDHENES" && existing.teacherId != authService.teacherIdFor(bearerToken().orEmpty())) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Nuk mund të fshini këtë mungesë.")); return@delete }
            if (absenceRepository.delete(id)) {
                val student = studentRepository.findById(existing.studentId)
                NotificationService.notifyStudentContacts(user.id, existing.teacherId, existing.studentId, "Mungesa u fshi", "Evidenca e mungesës për nxënësin ${student?.fullName ?: existing.studentId} më ${existing.date} u fshi.", "ABSENCE", existing.id)
                call.respond(HttpStatusCode.NoContent)
            } else call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Mungesa nuk u gjet."))
        }
    }
}


private fun linkedStudentIds(userId: String, role: String): Set<String> {
    val sql = when (role) {
        "PRIND" -> "SELECT student_id FROM parent_students WHERE user_id=?"
        "NXENES" -> "SELECT student_id FROM student_users WHERE user_id=?"
        else -> return emptySet()
    }
    return com.ltline.eshkolla.db.Database.connection().use { c ->
        c.prepareStatement(sql).use { p ->
            p.setString(1, userId)
            p.executeQuery().use { rs -> buildSet { while (rs.next()) add(rs.getString(1)) } }
        }
    }
}

private fun teacherHasClass(teacherId: String, classId: String): Boolean = teacherClassRepository.isAssigned(teacherId, classId)
private fun teacherHasSubjectInClass(teacherId: String, subjectId: String, classId: String): Boolean = com.ltline.eshkolla.db.Database.connection().use { c -> c.prepareStatement("SELECT 1 FROM teacher_subjects WHERE teacher_id=? AND subject_id=? AND class_id=?").use { p -> p.setString(1, teacherId); p.setString(2, subjectId); p.setString(3, classId); p.executeQuery().use { it.next() } } }
private fun classesForTeacher(teacherId: String): Set<String> = teacherClassRepository.getClassIdsForTeacher(teacherId)
private suspend fun io.ktor.server.application.ApplicationCall.requireUser(): UserDto? { val user = authService.userFor(bearerToken().orEmpty()); if (user == null) respond(HttpStatusCode.Unauthorized, ApiError("UNAUTHORIZED", "Kyçja është e nevojshme.")); return user }
private suspend fun io.ktor.server.application.ApplicationCall.requireWriteUser(): UserDto? { val user = requireUser() ?: return null; if (user.role !in setOf("ADMINISTRATOR", "DREJTOR", "MESIMDHENES")) { respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Nuk keni të drejtë për këtë veprim.")); return null }; return user }
private fun io.ktor.server.application.ApplicationCall.bearerToken(): String? = request.headers["Authorization"]?.removePrefix("Bearer ")?.takeIf { it.isNotBlank() }
private fun validateStudent(r: StudentRequest): ApiError? = when { r.fullName.trim().length < 2 -> ApiError("VALIDATION_ERROR", "Emri i nxënësit është shumë i shkurtër."); r.classId.trim().isBlank() -> ApiError("VALIDATION_ERROR", "Klasa është e detyrueshme."); r.birthDate.trim().isBlank() -> ApiError("VALIDATION_ERROR", "Datëlindja është e detyrueshme."); else -> null }
private fun validateGrade(r: GradeRequest): ApiError? = when { r.studentId.isBlank() -> ApiError("VALIDATION_ERROR", "Nxënësi është i detyrueshëm."); r.subjectId.isBlank() -> ApiError("VALIDATION_ERROR", "Lënda është e detyrueshme."); r.teacherId.isBlank() -> ApiError("VALIDATION_ERROR", "Mësimdhënësi është i detyrueshëm."); r.value !in 1..5 -> ApiError("VALIDATION_ERROR", "Nota duhet të jetë nga 1 deri në 5."); r.period.isBlank() -> ApiError("VALIDATION_ERROR", "Periudha është e detyrueshme."); r.academicYear.isBlank() -> ApiError("VALIDATION_ERROR", "Viti shkollor është i detyrueshëm."); else -> null }
private fun validateAbsence(r: AbsenceRequest): ApiError? = when { r.studentId.isBlank() -> ApiError("VALIDATION_ERROR", "Nxënësi është i detyrueshëm."); r.subjectId.isBlank() -> ApiError("VALIDATION_ERROR", "Lënda është e detyrueshme."); r.teacherId.isBlank() -> ApiError("VALIDATION_ERROR", "Mësimdhënësi është i detyrueshëm."); r.date.isBlank() -> ApiError("VALIDATION_ERROR", "Data është e detyrueshme."); r.status !in setOf("E_PAAFTESUAR", "E_ARSYESHME") -> ApiError("VALIDATION_ERROR", "Statusi i mungesës nuk është i vlefshëm."); else -> null }
