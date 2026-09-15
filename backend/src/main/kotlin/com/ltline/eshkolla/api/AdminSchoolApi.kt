package com.ltline.eshkolla.api

import com.ltline.eshkolla.auth.AuthService
import com.ltline.eshkolla.db.Database
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import java.sql.SQLException
import java.sql.Types
import java.util.UUID

@Serializable data class SchoolDto(val id: String, val name: String, val address: String?, val active: Boolean)
@Serializable data class SchoolCreateRequest(val name: String, val address: String? = null)
@Serializable data class SubjectDto(val id: String, val name: String, val code: String?, val active: Boolean)
@Serializable data class SubjectCreateRequest(val name: String, val code: String? = null)
@Serializable data class ClassCreateRequest(val name: String, val gradeLevel: Int, val schoolId: String? = null)
@Serializable data class StudentCreateRequest(val fullName: String, val classId: String, val birthDate: String)
@Serializable data class StudentManagementDto(val id: String, val fullName: String, val classId: String, val className: String, val birthDate: String, val active: Boolean)
@Serializable data class TeacherSubjectAssignmentRequest(val teacherId: String, val subjectId: String, val classId: String)
@Serializable data class TeacherSubjectAssignmentDto(val teacherId: String, val teacherName: String, val subjectId: String, val subjectName: String, val classId: String, val className: String)

fun Application.configureAdminSchoolApi(authService: AuthService) {
    routing {
        get("/api/v1/management/schools") {
            if (!requireAdmin(call, authService)) return@get
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
            if (!requireAdmin(call, authService)) return@get
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
            if (name.isBlank() || req.gradeLevel !in 1..13) {
                call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR", "Klasa dhe niveli 1-13 janë të detyrueshme."))
                return@post
            }
            if (schoolId != null && !exists("schools", schoolId)) {
                call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR", "Shkolla nuk ekziston."))
                return@post
            }
            val id = "C-${UUID.randomUUID().toString().take(10).uppercase()}"
            try {
                Database.connection().use { c ->
                    c.prepareStatement("ALTER TABLE classes ADD COLUMN IF NOT EXISTS school_id VARCHAR(64) REFERENCES schools(id)").use { it.executeUpdate() }
                    c.prepareStatement("INSERT INTO classes(id,name,grade_level,school_id,active) VALUES (?,?,?,?,TRUE)").use { ps ->
                        ps.setString(1, id)
                        ps.setString(2, name)
                        ps.setInt(3, req.gradeLevel)
                        if (schoolId == null) ps.setNull(4, Types.VARCHAR) else ps.setString(4, schoolId)
                        ps.executeUpdate()
                    }
                }
                call.respond(HttpStatusCode.Created, mapOf("id" to id, "name" to name, "gradeLevel" to req.gradeLevel, "schoolId" to schoolId))
            } catch (error: SQLException) {
                call.respond(HttpStatusCode.Conflict, ApiError("CLASS_CREATE_FAILED", "Klasa nuk u ruajt. Kontrolloni emrin, nivelin dhe shkollën e zgjedhur."))
            }
        }
        get("/api/v1/management/students") {
            if (!requireAdmin(call, authService)) return@get
            val rows = Database.connection().use { c -> c.prepareStatement("SELECT s.id,s.full_name,s.class_id,c.name,s.birth_date,s.active FROM students s JOIN classes c ON c.id=s.class_id ORDER BY c.grade_level,c.name,s.full_name").use { ps -> ps.executeQuery().use { rs -> buildList { while (rs.next()) add(StudentManagementDto(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getBoolean(6))) } } } }
            call.respond(rows)
        }
        post("/api/v1/management/students") {
            if (!requireAdmin(call, authService)) return@post
            val req = call.receive<StudentCreateRequest>()
            if (req.fullName.trim().length < 2 || req.birthDate.trim().isBlank()) { call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR", "Emri dhe datëlindja janë të detyrueshme.")); return@post }
            if (!exists("classes", req.classId)) { call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR", "Klasa nuk ekziston.")); return@post }
            val id = "ST-${UUID.randomUUID().toString().take(10).uppercase()}"
            Database.connection().use { c -> c.prepareStatement("INSERT INTO students(id,full_name,class_id,birth_date,active) VALUES (?,?,?,?,TRUE)").use { ps -> ps.setString(1,id); ps.setString(2,req.fullName.trim()); ps.setString(3,req.classId); ps.setString(4,req.birthDate.trim()); ps.executeUpdate() } }
            call.respond(HttpStatusCode.Created, mapOf("id" to id, "fullName" to req.fullName.trim(), "classId" to req.classId, "birthDate" to req.birthDate.trim()))
        }
        get("/api/v1/management/teacher-subject-assignments") {
            if (!requireAdmin(call, authService)) return@get
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

private suspend fun requireAdmin(call: io.ktor.server.application.ApplicationCall, auth: AuthService): Boolean {
    val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")?.takeIf { it.isNotBlank() }
    val user = token?.let(auth::userFor)
    if (user == null) { call.respond(HttpStatusCode.Unauthorized, ApiError("UNAUTHORIZED", "Kyçja është e nevojshme.")); return false }
    if (user.role != "ADMINISTRATOR") { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Vetëm administratori ka këtë qasje.")); return false }
    return true
}

private fun exists(table: String, id: String): Boolean {
    val safe = setOf("schools", "classes", "teachers", "subjects").firstOrNull { it == table } ?: return false
    return Database.connection().use { c -> c.prepareStatement("SELECT 1 FROM $safe WHERE id=? AND active=TRUE").use { ps -> ps.setString(1,id); ps.executeQuery().use { it.next() } } }
}
