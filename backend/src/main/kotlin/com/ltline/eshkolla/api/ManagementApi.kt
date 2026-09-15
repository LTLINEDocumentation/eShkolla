package com.ltline.eshkolla.api

import com.ltline.eshkolla.auth.AuthService
import com.ltline.eshkolla.auth.PasswordHasher
import com.ltline.eshkolla.db.Database
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import java.sql.Connection
import java.sql.SQLException
import java.util.UUID

@Serializable
data class ManagementTeacher(val id: String, val fullName: String, val subjectId: String, val username: String, val active: Boolean, val classIds: List<String>)

@Serializable
data class ManagementClass(val id: String, val name: String, val gradeLevel: Int, val active: Boolean, val studentCount: Int, val teacherIds: List<String>)

@Serializable
data class TeacherAssignmentRequest(val teacherId: String, val classId: String)

@Serializable
data class UserManagement(val id: String, val username: String, val fullName: String, val role: String, val active: Boolean)

@Serializable
data class UserStatusRequest(val active: Boolean)

@Serializable
data class UserCreateRequest(
    val username: String,
    val fullName: String,
    val password: String,
    val role: String,
    val subjectId: String? = null,
    val teacherId: String? = null,
    val studentId: String? = null
)

@Serializable
data class PasswordUpdateRequest(val password: String)

@Serializable
data class UserCreated(val id: String, val username: String, val fullName: String, val role: String)

fun Application.configureManagementApi(authService: AuthService) {
    routing {
        route("/api/v1/management") {
            get("/teachers") {
                val user = call.managementUser(authService) ?: return@get
                if (user.role !in setOf("ADMINISTRATOR", "DREJTOR")) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Nuk keni të drejtë për menaxhimin e mësimdhënësve.")); return@get }
                val teachers = Database.connection().use { c ->
                    c.prepareStatement("SELECT t.id,t.full_name,t.subject_id,t.active,u.username FROM teachers t JOIN users u ON u.id=t.user_id ORDER BY t.full_name").use { ps -> ps.executeQuery().use { rs -> buildList {
                        while (rs.next()) { val id = rs.getString("id"); add(ManagementTeacher(id, rs.getString("full_name"), rs.getString("subject_id"), rs.getString("username"), rs.getBoolean("active"), classIds(c, id))) }
                    } } }
                }
                call.respond(teachers)
            }

            get("/classes") {
                val user = call.managementUser(authService) ?: return@get
                if (user.role !in setOf("ADMINISTRATOR", "DREJTOR")) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Nuk keni të drejtë për menaxhimin e klasave.")); return@get }
                val classes = Database.connection().use { c ->
                    c.prepareStatement("SELECT id,name,grade_level,active,(SELECT COUNT(*) FROM students s WHERE s.class_id=c.id AND s.active=TRUE) AS student_count FROM classes c ORDER BY grade_level,name").use { ps -> ps.executeQuery().use { rs -> buildList {
                        while (rs.next()) { val id = rs.getString("id"); add(ManagementClass(id, rs.getString("name"), rs.getInt("grade_level"), rs.getBoolean("active"), rs.getInt("student_count"), teacherIds(c, id))) }
                    } } }
                }
                call.respond(classes)
            }

            get("/users") {
                val user = call.managementUser(authService) ?: return@get
                if (user.role != "ADMINISTRATOR") { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Vetëm administratori menaxhon përdoruesit.")); return@get }
                val users = Database.connection().use { c -> c.prepareStatement("SELECT id,username,full_name,role,active FROM users ORDER BY full_name").use { ps -> ps.executeQuery().use { rs -> buildList {
                    while (rs.next()) add(UserManagement(rs.getString("id"), rs.getString("username"), rs.getString("full_name"), rs.getString("role"), rs.getBoolean("active")))
                } } } }
                call.respond(users)
            }

            post("/users") {
                val user = call.managementUser(authService) ?: return@post
                if (user.role != "ADMINISTRATOR") { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Vetëm administratori krijon përdorues.")); return@post }
                val request = call.receive<UserCreateRequest>()
                val validation = validateUserCreate(request)
                if (validation != null) { call.respond(HttpStatusCode.BadRequest, validation); return@post }
                try {
                    val created = Database.connection().use { connection -> createUser(connection, request) }
                    call.respond(HttpStatusCode.Created, created)
                } catch (_: SQLException) {
                    call.respond(HttpStatusCode.Conflict, ApiError("ALREADY_EXISTS", "Përdoruesi ose lidhja e kërkuar ekziston tashmë."))
                }
            }

            put("/users/{id}/password") {
                val user = call.managementUser(authService) ?: return@put
                if (user.role != "ADMINISTRATOR") { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Vetëm administratori ndryshon fjalëkalime.")); return@put }
                val password = call.receive<PasswordUpdateRequest>().password
                if (password.length < 10) { call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR", "Fjalëkalimi duhet të ketë së paku 10 karaktere.")); return@put }
                val id = call.parameters["id"].orEmpty()
                val updated = Database.connection().use { c -> c.prepareStatement("UPDATE users SET password_hash=? WHERE id=?").use { ps -> ps.setString(1, PasswordHasher.create(password)); ps.setString(2, id); ps.executeUpdate() } }
                if (updated == 0) call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Përdoruesi nuk u gjet.")) else call.respond(HttpStatusCode.NoContent)
            }

            put("/users/{id}/status") {
                val user = call.managementUser(authService) ?: return@put
                if (user.role != "ADMINISTRATOR") { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Vetëm administratori mund të ndryshojë statusin e përdoruesit.")); return@put }
                val id = call.parameters["id"].orEmpty()
                val req = call.receive<UserStatusRequest>()
                if (id == user.id && !req.active) { call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR", "Administratori nuk mund të çaktivizojë llogarinë e vet.")); return@put }
                val updated = Database.connection().use { c -> c.prepareStatement("UPDATE users SET active=? WHERE id=?").use { ps -> ps.setBoolean(1, req.active); ps.setString(2, id); ps.executeUpdate() } }
                if (updated == 0) call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Përdoruesi nuk u gjet.")) else call.respond(HttpStatusCode.NoContent)
            }

            post("/teacher-assignments") {
                val user = call.managementUser(authService) ?: return@post
                if (user.role !in setOf("ADMINISTRATOR", "DREJTOR")) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Nuk keni të drejtë të caktoni mësimdhënës.")); return@post }
                val req = call.receive<TeacherAssignmentRequest>()
                if (req.teacherId.isBlank() || req.classId.isBlank()) { call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR", "Mësimdhënësi dhe klasa janë të detyrueshme.")); return@post }
                val inserted = Database.connection().use { c -> c.prepareStatement("INSERT INTO teacher_classes(teacher_id,class_id) VALUES (?,?) ON CONFLICT DO NOTHING").use { ps -> ps.setString(1, req.teacherId); ps.setString(2, req.classId); ps.executeUpdate() } }
                if (inserted == 0) call.respond(HttpStatusCode.Conflict, ApiError("ALREADY_EXISTS", "Caktimi ekziston ose të dhënat nuk janë valide.")) else call.respond(HttpStatusCode.Created, req)
            }

            delete("/teacher-assignments/{teacherId}/{classId}") {
                val user = call.managementUser(authService) ?: return@delete
                if (user.role !in setOf("ADMINISTRATOR", "DREJTOR")) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Nuk keni të drejtë të hiqni caktimin.")); return@delete }
                val deleted = Database.connection().use { c -> c.prepareStatement("DELETE FROM teacher_classes WHERE teacher_id=? AND class_id=?").use { ps -> ps.setString(1, call.parameters["teacherId"].orEmpty()); ps.setString(2, call.parameters["classId"].orEmpty()); ps.executeUpdate() } }
                if (deleted == 0) call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Caktimi nuk u gjet.")) else call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private fun validateUserCreate(request: UserCreateRequest): ApiError? {
    val allowedRoles = setOf("ADMINISTRATOR", "DREJTOR", "MESIMDHENES", "NXENES", "PRIND")
    if (request.username.trim().length < 3) return ApiError("VALIDATION_ERROR", "Emri i përdoruesit duhet të ketë së paku 3 karaktere.")
    if (request.fullName.trim().length < 2) return ApiError("VALIDATION_ERROR", "Emri dhe mbiemri janë të detyrueshëm.")
    if (request.password.length < 10) return ApiError("VALIDATION_ERROR", "Fjalëkalimi duhet të ketë së paku 10 karaktere.")
    if (request.role !in allowedRoles) return ApiError("VALIDATION_ERROR", "Roli nuk është i vlefshëm.")
    if (request.role == "MESIMDHENES" && request.subjectId.isNullOrBlank()) return ApiError("VALIDATION_ERROR", "Lënda është e detyrueshme për mësimdhënësin.")
    if (request.role in setOf("NXENES", "PRIND") && request.studentId.isNullOrBlank()) return ApiError("VALIDATION_ERROR", "studentId është i detyrueshëm për këtë rol.")
    return null
}

private fun createUser(connection: Connection, request: UserCreateRequest): UserCreated {
    val id = "U-${UUID.randomUUID().toString().take(12).uppercase()}"
    val role = request.role.trim()
    val studentId = request.studentId?.trim()?.takeIf { it.isNotBlank() }
    val passwordHash = PasswordHasher.create(request.password)
    connection.autoCommit = false
    try {
        connection.prepareStatement("INSERT INTO users(id,username,full_name,role,password_hash,active) VALUES (?,?,?,?,?,TRUE)").use { ps ->
            ps.setString(1, id); ps.setString(2, request.username.trim()); ps.setString(3, request.fullName.trim()); ps.setString(4, role); ps.setString(5, passwordHash); ps.executeUpdate()
        }
        when (role) {
            "MESIMDHENES" -> {
                val teacherId = request.teacherId?.trim()?.takeIf { it.isNotBlank() } ?: "M-${UUID.randomUUID().toString().take(8).uppercase()}"
                connection.prepareStatement("INSERT INTO teachers(id,user_id,full_name,subject_id,active) VALUES (?,?,?,?,TRUE)").use { ps ->
                    ps.setString(1, teacherId); ps.setString(2, id); ps.setString(3, request.fullName.trim()); ps.setString(4, request.subjectId!!.trim()); ps.executeUpdate()
                }
            }
            "NXENES" -> {
                ensureStudentExists(connection, studentId!!)
                connection.prepareStatement("INSERT INTO student_users(user_id,student_id) VALUES (?,?)").use { ps -> ps.setString(1, id); ps.setString(2, studentId); ps.executeUpdate() }
            }
            "PRIND" -> {
                ensureStudentExists(connection, studentId!!)
                connection.prepareStatement("INSERT INTO parent_students(user_id,student_id) VALUES (?,?)").use { ps -> ps.setString(1, id); ps.setString(2, studentId); ps.executeUpdate() }
            }
        }
        connection.commit()
        return UserCreated(id, request.username.trim(), request.fullName.trim(), role)
    } catch (error: Exception) {
        connection.rollback()
        throw error
    } finally {
        connection.autoCommit = true
    }
}

private fun ensureStudentExists(connection: Connection, studentId: String) {
    connection.prepareStatement("SELECT 1 FROM students WHERE id=? AND active=TRUE").use { ps ->
        ps.setString(1, studentId)
        ps.executeQuery().use { rs -> if (!rs.next()) throw SQLException("Studenti nuk ekziston") }
    }
}

private suspend fun ApplicationCall.managementUser(auth: AuthService): UserDto? {
    val token = request.headers["Authorization"]?.removePrefix("Bearer ")?.takeIf { it.isNotBlank() }
    if (token == null) { respond(HttpStatusCode.Unauthorized, ApiError("UNAUTHORIZED", "Kyçja është e nevojshme.")); return null }
    val user = auth.userFor(token)
    if (user == null) { respond(HttpStatusCode.Unauthorized, ApiError("UNAUTHORIZED", "Sesioni nuk është i vlefshëm.")); return null }
    return user
}

private fun classIds(c: Connection, teacherId: String): List<String> = c.prepareStatement("SELECT class_id FROM teacher_classes WHERE teacher_id=? ORDER BY class_id").use { ps -> ps.setString(1, teacherId); ps.executeQuery().use { rs -> buildList { while (rs.next()) add(rs.getString(1)) } } }
private fun teacherIds(c: Connection, classId: String): List<String> = c.prepareStatement("SELECT teacher_id FROM teacher_classes WHERE class_id=? ORDER BY teacher_id").use { ps -> ps.setString(1, classId); ps.executeQuery().use { rs -> buildList { while (rs.next()) add(rs.getString(1)) } } }
