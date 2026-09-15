package com.ltline.eshkolla.api

import com.ltline.eshkolla.auth.AuthService
import com.ltline.eshkolla.auth.Authorization
import com.ltline.eshkolla.auth.Permission
import com.ltline.eshkolla.auth.RolePermissions
import com.ltline.eshkolla.db.Database
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable

@Serializable
data class PermissionResponse(val permissions: List<String>)
@Serializable
data class RoleDashboardResponse(val role: String, val title: String, val modules: List<String>)
@Serializable
data class TeacherProfileResponse(val teacherId: String, val fullName: String, val subjectId: String, val classIds: List<String>)
@Serializable
data class LinkedStudentResponse(val id: String, val fullName: String, val classId: String)

fun Application.configureRoleApi(authService: AuthService) {
    val authorization = Authorization(authService)
    routing {
        route("/api/v1") {
            get("/me/profile") {
                val token = call.roleBearerToken()
                if (token == null) { call.respond(HttpStatusCode.Unauthorized, ApiError("UNAUTHORIZED", "Kyçja është e nevojshme.")); return@get }
                val user = authorization.user(token)
                if (user == null) { call.respond(HttpStatusCode.Unauthorized, ApiError("UNAUTHORIZED", "Sesioni nuk është i vlefshëm.")); return@get }
                call.respond(user)
            }
            get("/me/permissions") {
                val token = call.roleBearerToken()
                if (token == null) { call.respond(HttpStatusCode.Unauthorized, ApiError("UNAUTHORIZED", "Kyçja është e nevojshme.")); return@get }
                val user = authorization.user(token)
                if (user == null) { call.respond(HttpStatusCode.Unauthorized, ApiError("UNAUTHORIZED", "Sesioni nuk është i vlefshëm.")); return@get }
                call.respond(PermissionResponse(Permission.entries.filter { RolePermissions.allows(user.role, it) }.map { it.name }.sorted()))
            }
            get("/dashboard") {
                val token = call.roleBearerToken()
                if (token == null) { call.respond(HttpStatusCode.Unauthorized, ApiError("UNAUTHORIZED", "Kyçja është e nevojshme.")); return@get }
                val user = authorization.user(token)
                if (user == null) { call.respond(HttpStatusCode.Unauthorized, ApiError("UNAUTHORIZED", "Sesioni nuk është i vlefshëm.")); return@get }
                val modules = when (user.role) {
                    "ADMINISTRATOR" -> listOf("Përdoruesit", "Shkolla", "Mësimdhënësit", "Klasat", "Nxënësit", "Notat", "Mungesat", "Orari", "Njoftimet", "Raportet")
                    "DREJTOR" -> listOf("Shkolla", "Mësimdhënësit", "Klasat", "Nxënësit", "Notat", "Mungesat", "Orari", "Njoftimet", "Raportet")
                    "MESIMDHENES" -> listOf("Klasat e mia", "Nxënësit", "Notat", "Mungesat", "Orari", "Njoftimet", "Raportet")
                    "NXENES" -> listOf("Profili im", "Notat", "Mungesat", "Orari", "Njoftimet", "Raportet")
                    "PRIND" -> listOf("Fëmija im", "Notat", "Mungesat", "Orari", "Njoftimet", "Raportet")
                    else -> emptyList()
                }
                call.respond(RoleDashboardResponse(user.role, user.fullName, modules))
            }
            get("/me/teacher") {
                val token = call.roleBearerToken()
                if (token == null) { call.respond(HttpStatusCode.Unauthorized, ApiError("UNAUTHORIZED", "Kyçja është e nevojshme.")); return@get }
                val user = authorization.require(token, Permission.OWN_PROFILE_READ)
                if (user.role != "MESIMDHENES") { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Vetëm mësimdhënësi mund ta shohë këtë profil.")); return@get }
                val result = Database.connection().use { connection ->
                    connection.prepareStatement("SELECT t.id, t.full_name, t.subject_id, c.class_id FROM teachers t LEFT JOIN teacher_classes c ON c.teacher_id=t.id WHERE t.user_id=? AND t.active=TRUE ORDER BY c.class_id").use { ps ->
                        ps.setString(1, user.id)
                        ps.executeQuery().use { rs ->
                            if (!rs.next()) null else {
                                val teacherId = rs.getString("id"); val fullName = rs.getString("full_name"); val subjectId = rs.getString("subject_id")
                                val classes = mutableListOf<String>(); do { rs.getString("class_id")?.let(classes::add) } while (rs.next())
                                TeacherProfileResponse(teacherId, fullName, subjectId, classes.distinct())
                            }
                        }
                    }
                }
                if (result == null) call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Profili i mësimdhënësit nuk u gjet.")) else call.respond(result)
            }
            get("/me/children") {
                val token = call.roleBearerToken()
                if (token == null) { call.respond(HttpStatusCode.Unauthorized, ApiError("UNAUTHORIZED", "Kyçja është e nevojshme.")); return@get }
                val user = authorization.require(token, Permission.CHILD_PROFILE_READ)
                if (user.role != "PRIND") { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Vetëm prindi mund ta shohë këtë listë.")); return@get }
                val children = Database.connection().use { connection ->
                    connection.prepareStatement("SELECT s.id, s.full_name, s.class_id FROM parent_students ps JOIN students s ON s.id=ps.student_id WHERE ps.user_id=? AND s.active=TRUE ORDER BY s.full_name").use { ps ->
                        ps.setString(1, user.id); ps.executeQuery().use { rs -> buildList { while (rs.next()) add(LinkedStudentResponse(rs.getString("id"), rs.getString("full_name"), rs.getString("class_id"))) } }
                    }
                }
                call.respond(children)
            }
            get("/me/student") {
                val token = call.roleBearerToken()
                if (token == null) { call.respond(HttpStatusCode.Unauthorized, ApiError("UNAUTHORIZED", "Kyçja është e nevojshme.")); return@get }
                val user = authorization.require(token, Permission.OWN_PROFILE_READ)
                if (user.role != "NXENES") { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Vetëm nxënësi mund ta shohë këtë profil.")); return@get }
                val result = Database.connection().use { connection ->
                    connection.prepareStatement("SELECT s.id, s.full_name, s.class_id FROM student_users su JOIN students s ON s.id=su.student_id WHERE su.user_id=? AND s.active=TRUE LIMIT 1").use { ps ->
                        ps.setString(1, user.id); ps.executeQuery().use { rs -> if (rs.next()) LinkedStudentResponse(rs.getString("id"), rs.getString("full_name"), rs.getString("class_id")) else null }
                    }
                }
                if (result == null) call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Profili i nxënësit nuk u gjet.")) else call.respond(result)
            }
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.roleBearerToken(): String? = request.headers["Authorization"]?.removePrefix("Bearer ")?.takeIf { it.isNotBlank() }
