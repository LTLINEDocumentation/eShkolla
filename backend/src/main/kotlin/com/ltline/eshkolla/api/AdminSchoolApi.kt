package com.ltline.eshkolla.api

import com.ltline.eshkolla.auth.AuthService
import com.ltline.eshkolla.db.Database
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import java.sql.SQLException
import java.sql.Types
import java.text.Normalizer
import java.util.UUID

@Serializable data class SchoolDto(val id: String, val name: String, val address: String?, val active: Boolean)
@Serializable data class SchoolCreateRequest(val name: String, val address: String? = null)
@Serializable data class SubjectDto(val id: String, val name: String, val code: String?, val active: Boolean)
@Serializable data class SubjectCreateRequest(val name: String, val code: String? = null)
@Serializable data class ClassCreateRequest(val name: String, val gradeLevel: Int, val schoolId: String? = null)
@Serializable data class StudentCreateRequest(val fullName: String, val classId: String, val birthDate: String, val allowDuplicate: Boolean = false)
@Serializable data class StudentManagementDto(val id: String, val fullName: String, val classId: String, val className: String, val birthDate: String, val active: Boolean)
@Serializable data class StudentDuplicateCandidate(val id: String, val fullName: String, val className: String, val birthDate: String)
@Serializable data class StudentDuplicateResponse(val code: String, val message: String, val candidates: List<StudentDuplicateCandidate>)
@Serializable data class TeacherSubjectAssignmentRequest(val teacherId: String, val subjectId: String, val classId: String)
@Serializable data class TeacherSubjectAssignmentDto(val teacherId: String, val teacherName: String, val subjectId: String, val subjectName: String, val classId: String, val className: String)

fun Application.configureAdminSchoolApi(authService: AuthService) {
    routing {
        get("/api/v1/management/schools") {
            if (!requireAdminOrDirector(call, authService)) return@get
            val rows = Database.connection().use { c -> c.prepareStatement("SELECT id,name,address,active FROM schools ORDER BY name").use { ps -> ps.executeQuery().use { rs -> buildList { while (rs.next()) add(SchoolDto(rs.getString(1), rs.getString(2), rs.getString(3), rs.getBoolean(4))) } } } }
            call.respond(rows)
        }
        post("/api/v1/management/schools") {
            if (!requireAdmin(call, authService)) return@post
            val req = call.receive<SchoolCreateRequest>()
            if (req.name.trim().length < 2) { call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR", "Emri i shkollës është i detyrueshëm.")); return@post }
            try {
                val id = "SH-${UUID.randomUUID().toString().take(10).uppercase()}"
                Database.connection().use { c -> c.prepareStatement("INSERT INTO schools(id,name,address,active) VALUES (?,?,?,TRUE)").use { ps -> ps.setString(1,id); ps.setString(2,req.name.trim()); ps.setString(3,req.address?.trim()?.takeIf { it.isNotBlank() }); ps.executeUpdate() } }
                call.respond(HttpStatusCode.Created, SchoolDto(id, req.name.trim(), req.address?.trim(), true))
            } catch (_: SQLException) { call.respond(HttpStatusCode.Conflict, ApiError("ALREADY_EXISTS", "Kjo shkollë ekziston.")) }
        }
        get("/api/v1/management/subjects") {
            if (!requireAdminOrDirector(call, authService)) return@get
            val rows = Database.connection().use { c -> c.prepareStatement("SELECT id,name,code,active FROM subjects ORDER BY name").use { ps -> ps.executeQuery().use { rs -> buildList { while (rs.next()) add(SubjectDto(rs.getString(1), rs.getString(2), rs.getString(3), rs.getBoolean(4))) } } } }
            call.respond(rows)
        }
        post("/api/v1/management/subjects") {
            if (!requireAdmin(call, authService)) return@post
            val req = call.receive<SubjectCreateRequest>()
            if (req.name.trim().length < 2) { call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR", "Emri i lëndës është i detyrueshëm.")); return@post }
            try {
                val id = "SUB-${UUID.randomUUID().toString().take(10).uppercase()}"
                Database.connection().use { c -> c.prepareStatement("INSERT INTO subjects(id,name,code,active) VALUES (?,?,?,TRUE)").use { ps -> ps.setString(1,id); ps.setString(2,req.name.trim()); ps.setString(3,req.code?.trim()?.takeIf { it.isNotBlank() }); ps.executeUpdate() } }
                call.respond(HttpStatusCode.Created, SubjectDto(id, req.name.trim(), req.code?.trim(), true))
            } catch (_: SQLException) { call.respond(HttpStatusCode.Conflict, ApiError("ALREADY_EXISTS", "Kjo lëndë ose kodi ekziston.")) }
        }
        post("/api/v1/management/classes") {
            if (!requireAdmin(call, authService)) return@post
            val req = call.receive<ClassCreateRequest>()
            val name = req.name.trim()
            val schoolId = req.schoolId?.trim()?.takeIf { it.isNotBlank() }
            if (name.isBlank() || req.gradeLevel !in 1..13) { call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR", "Klasa dhe niveli 1-13 janë të detyrueshme.")); return@post }
            if (schoolId != null && !exists("schools", schoolId)) { call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR", "Shkolla nuk ekziston.")); return@post }
            val id = "C-${UUID.randomUUID().toString().take(10).uppercase()}"
            try {
                Database.connection().use { c ->
                    c.prepareStatement("ALTER TABLE classes ADD COLUMN IF NOT EXISTS school_id VARCHAR(64) REFERENCES schools(id)").use { it.executeUpdate() }
                    c.prepareStatement("INSERT INTO classes(id,name,grade_level,school_id,active) VALUES (?,?,?,?,TRUE)").use { ps -> ps.setString(1,id); ps.setString(2,name); ps.setInt(3,req.gradeLevel); if (schoolId == null) ps.setNull(4,Types.VARCHAR) else ps.setString(4,schoolId); ps.executeUpdate() }
                }
                call.respond(HttpStatusCode.Created, mapOf("id" to id, "name" to name, "gradeLevel" to req.gradeLevel, "schoolId" to schoolId))
            } catch (_: SQLException) { call.respond(HttpStatusCode.Conflict, ApiError("CLASS_CREATE_FAILED", "Klasa nuk u ruajt. Kontrolloni emrin, nivelin dhe shkollën e zgjedhur.")) }
        }
        get("/api/v1/management/students") {
            if (!requireAdminOrDirector(call, authService)) return@get
            val rows = Database.connection().use { c -> c.prepareStatement("SELECT s.id,s.full_name,s.class_id,c.name,s.birth_date,s.active FROM students s JOIN classes c ON c.id=s.class_id ORDER BY c.grade_level,c.name,s.full_name").use { ps -> ps.executeQuery().use { rs -> buildList { while (rs.next()) add(StudentManagementDto(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getBoolean(6))) } } } }
            call.respond(rows)
        }
        post("/api/v1/management/students") {
            if (!requireAdmin(call, authService)) return@post
            val req = call.receive<StudentCreateRequest>()
            val fullName = req.fullName.trim()
            val birthDate = req.birthDate.trim()
            if (fullName.length < 2 || birthDate.isBlank()) { call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR", "Emri dhe datëlindja janë të detyrueshme.")); return@post }
            if (!exists("classes", req.classId)) { call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR", "Klasa/paralelja nuk ekziston ose nuk është aktive.")); return@post }

            val candidates = findStudentsWithSameName(fullName)
            if (candidates.isNotEmpty() && !req.allowDuplicate) {
                call.respond(HttpStatusCode.Conflict, StudentDuplicateResponse(
                    "STUDENT_DUPLICATE_REVIEW",
                    "Ekziston tashmë një nxënës me të njëjtin emër dhe mbiemër. Kontrolloni të dhënat para regjistrimit.",
                    candidates
                ))
                return@post
            }

            val id = "ST-${UUID.randomUUID().toString().take(10).uppercase()}"
            Database.connection().use { c -> c.prepareStatement("INSERT INTO students(id,full_name,class_id,birth_date,active) VALUES (?,?,?,?,TRUE)").use { ps -> ps.setString(1,id); ps.setString(2,fullName); ps.setString(3,req.classId); ps.setString(4,birthDate); ps.executeUpdate() } }
            call.respond(HttpStatusCode.Created, StudentManagementDto(id, fullName, req.classId, className(req.classId), birthDate, true))
        }
        put("/api/v1/management/students/{id}") {
            if (!requireAdmin(call, authService)) return@put
            val id = call.parameters["id"]?.trim().orEmpty()
            val req = call.receive<StudentCreateRequest>()
            val fullName = req.fullName.trim()
            val birthDate = req.birthDate.trim()
            if (id.isBlank() || fullName.length < 2 || birthDate.isBlank()) { call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR", "ID, emri dhe datëlindja janë të detyrueshme.")); return@put }
            if (!exists("students", id)) { call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Nxënësi nuk ekziston ose është joaktiv.")); return@put }
            if (!exists("classes", req.classId)) { call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR", "Klasa/paralelja nuk ekziston ose nuk është aktive.")); return@put }
            val candidates = findStudentsWithSameName(fullName, id)
            if (candidates.isNotEmpty() && !req.allowDuplicate) {
                call.respond(HttpStatusCode.Conflict, StudentDuplicateResponse("STUDENT_DUPLICATE_REVIEW", "Ekziston tashmë një nxënës me të njëjtin emër dhe mbiemër. Kontrolloni të dhënat para ndryshimit.", candidates)); return@put
            }
            Database.connection().use { c -> c.prepareStatement("UPDATE students SET full_name=?,class_id=?,birth_date=? WHERE id=? AND active=TRUE").use { ps -> ps.setString(1,fullName); ps.setString(2,req.classId); ps.setString(3,birthDate); ps.setString(4,id); ps.executeUpdate() } }
            call.respond(StudentManagementDto(id, fullName, req.classId, className(req.classId), birthDate, true))
        }
        delete("/api/v1/management/students/{id}") {
            if (!requireAdmin(call, authService)) return@delete
            val id = call.parameters["id"]?.trim().orEmpty()
            if (id.isBlank() || !exists("students", id)) { call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Nxënësi nuk ekziston ose është tashmë joaktiv.")); return@delete }
            Database.connection().use { c -> c.prepareStatement("UPDATE students SET active=FALSE WHERE id=?").use { ps -> ps.setString(1,id); ps.executeUpdate() } }
            call.respond(mapOf("id" to id, "active" to false, "message" to "Nxënësi u çaktivizua."))
        }
        get("/api/v1/management/teacher-subject-assignments") {
            if (!requireAdminOrDirector(call, authService)) return@get
            val rows = Database.connection().use { c -> c.prepareStatement("SELECT ts.teacher_id,t.full_name,ts.subject_id,s.name,ts.class_id,c.name FROM teacher_subjects ts JOIN teachers t ON t.id=ts.teacher_id JOIN subjects s ON s.id=ts.subject_id JOIN classes c ON c.id=ts.class_id ORDER BY t.full_name,s.name,c.name").use { ps -> ps.executeQuery().use { rs -> buildList { while (rs.next()) add(TeacherSubjectAssignmentDto(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6))) } } } }
            call.respond(rows)
        }
        post("/api/v1/management/teacher-subject-assignments") {
            if (!requireAdmin(call, authService)) return@post
            val req = call.receive<TeacherSubjectAssignmentRequest>()
            if (!exists("teachers", req.teacherId) || !exists("subjects", req.subjectId) || !exists("classes", req.classId)) { call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR", "Mësimdhënësi, lënda ose klasa nuk ekziston.")); return@post }
            try {
                Database.connection().use { c -> c.prepareStatement("INSERT INTO teacher_subjects(teacher_id,subject_id,class_id) VALUES (?,?,?)").use { ps -> ps.setString(1,req.teacherId); ps.setString(2,req.subjectId); ps.setString(3,req.classId); ps.executeUpdate() } }
                Database.connection().use { c -> c.prepareStatement("INSERT INTO teacher_classes(teacher_id,class_id) VALUES (?,?) ON CONFLICT DO NOTHING").use { ps -> ps.setString(1,req.teacherId); ps.setString(2,req.classId); ps.executeUpdate() } }
                call.respond(HttpStatusCode.Created, req)
            } catch (_: SQLException) { call.respond(HttpStatusCode.Conflict, ApiError("ALREADY_EXISTS", "Ky caktim ekziston.")) }
        }
    }
}

private fun normalizeStudentName(value: String): String = Normalizer.normalize(value.trim().replace(Regex("\\s+"), " "), Normalizer.Form.NFC).lowercase()

private fun findStudentsWithSameName(fullName: String, excludeId: String? = null): List<StudentDuplicateCandidate> {
    val target = normalizeStudentName(fullName)
    return Database.connection().use { c ->
        c.prepareStatement("SELECT s.id,s.full_name,c.name,s.birth_date FROM students s JOIN classes c ON c.id=s.class_id WHERE s.active=TRUE ORDER BY s.full_name,c.name").use { ps ->
            ps.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) {
                        val id = rs.getString(1)
                        if (excludeId != null && id == excludeId) continue
                        if (normalizeStudentName(rs.getString(2)) == target) add(StudentDuplicateCandidate(id, rs.getString(2), rs.getString(3), rs.getString(4)))
                    }
                }
            }
        }
    }
}

private suspend fun requireAdmin(call: io.ktor.server.application.ApplicationCall, auth: AuthService): Boolean {
    val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")?.takeIf { it.isNotBlank() }
    val user = token?.let(auth::userFor)
    if (user == null) { call.respond(HttpStatusCode.Unauthorized, ApiError("UNAUTHORIZED", "Kyçja është e nevojshme.")); return false }
    if (user.role != "ADMINISTRATOR") { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Vetëm administratori ka këtë qasje.")); return false }
    return true
}

private suspend fun requireAdminOrDirector(call: io.ktor.server.application.ApplicationCall, auth: AuthService): Boolean {
    val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")?.takeIf { it.isNotBlank() }
    val user = token?.let(auth::userFor)
    if (user == null) { call.respond(HttpStatusCode.Unauthorized, ApiError("UNAUTHORIZED", "Kyçja është e nevojshme.")); return false }
    if (user.role !in setOf("ADMINISTRATOR","DREJTOR")) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Nuk keni këtë qasje.")); return false }
    return true
}

private fun exists(table: String, id: String): Boolean {
    val safe = setOf("schools", "classes", "teachers", "subjects", "students").firstOrNull { it == table } ?: return false
    return Database.connection().use { c -> c.prepareStatement("SELECT 1 FROM $safe WHERE id=? AND active=TRUE").use { ps -> ps.setString(1,id); ps.executeQuery().use { it.next() } } }
}

private fun className(classId: String): String = Database.connection().use { c ->
    c.prepareStatement("SELECT name FROM classes WHERE id=?").use { ps -> ps.setString(1,classId); ps.executeQuery().use { rs -> if (rs.next()) rs.getString(1) else classId } }
}
