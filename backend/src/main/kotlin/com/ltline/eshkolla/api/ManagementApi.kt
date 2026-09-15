package com.ltline.eshkolla.api

import com.ltline.eshkolla.auth.AuthService
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

fun Application.configureManagementApi(authService: AuthService) {
    routing {
        route("/api/v1/management") {
            get("/teachers") {
                val user = call.managementUser(authService) ?: return@get
                if (user.role !in setOf("ADMINISTRATOR", "DREJTOR")) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Nuk keni të drejtë për menaxhimin e mësimdhënësve.")); return@get }
                val teachers = Database.connection().use { c ->
                    c.prepareStatement("SELECT t.id,t.full_name,t.subject_id,t.active,u.username FROM teachers t JOIN users u ON u.id=t.user_id ORDER BY t.full_name").use { ps -> ps.executeQuery().use { rs -> buildList {
                        while (rs.next()) { val id=rs.getString("id"); add(ManagementTeacher(id,rs.getString("full_name"),rs.getString("subject_id"),rs.getString("username"),rs.getBoolean("active"),classIds(c,id))) }
                    } } }
                }
                call.respond(teachers)
            }
            get("/classes") {
                val user=call.managementUser(authService) ?: return@get
                if(user.role !in setOf("ADMINISTRATOR","DREJTOR")){call.respond(HttpStatusCode.Forbidden,ApiError("FORBIDDEN","Nuk keni të drejtë për menaxhimin e klasave."));return@get}
                val classes=Database.connection().use { c -> c.prepareStatement("SELECT id,name,grade_level,active,(SELECT COUNT(*) FROM students s WHERE s.class_id=c.id AND s.active=TRUE) AS student_count FROM classes c ORDER BY grade_level,name").use { ps->ps.executeQuery().use { rs->buildList { while(rs.next()){val id=rs.getString("id");add(ManagementClass(id,rs.getString("name"),rs.getInt("grade_level"),rs.getBoolean("active"),rs.getInt("student_count"),teacherIds(c,id)))}}}}}
                call.respond(classes)
            }
            get("/users") {
                val user=call.managementUser(authService) ?: return@get
                if(user.role!="ADMINISTRATOR"){call.respond(HttpStatusCode.Forbidden,ApiError("FORBIDDEN","Vetëm administratori menaxhon përdoruesit."));return@get}
                val users=Database.connection().use { c->c.prepareStatement("SELECT id,username,full_name,role,active FROM users ORDER BY full_name").use { ps->ps.executeQuery().use { rs->buildList { while(rs.next())add(UserManagement(rs.getString("id"),rs.getString("username"),rs.getString("full_name"),rs.getString("role"),rs.getBoolean("active")))}}}}
                call.respond(users)
            }
            put("/users/{id}/status") {
                val user=call.managementUser(authService) ?: return@put
                if(user.role!="ADMINISTRATOR"){call.respond(HttpStatusCode.Forbidden,ApiError("FORBIDDEN","Vetëm administratori mund të ndryshojë statusin e përdoruesit."));return@put}
                val id=call.parameters["id"].orEmpty();val req=call.receive<UserStatusRequest>()
                if(id==user.id&&!req.active){call.respond(HttpStatusCode.BadRequest,ApiError("VALIDATION_ERROR","Administratori nuk mund të çaktivizojë llogarinë e vet."));return@put}
                val updated=Database.connection().use { c->c.prepareStatement("UPDATE users SET active=? WHERE id=?").use { ps->ps.setBoolean(1,req.active);ps.setString(2,id);ps.executeUpdate()}}
                if(updated==0)call.respond(HttpStatusCode.NotFound,ApiError("NOT_FOUND","Përdoruesi nuk u gjet."))else call.respond(HttpStatusCode.NoContent)
            }
            post("/teacher-assignments") {
                val user=call.managementUser(authService) ?: return@post
                if(user.role !in setOf("ADMINISTRATOR","DREJTOR")){call.respond(HttpStatusCode.Forbidden,ApiError("FORBIDDEN","Nuk keni të drejtë të caktoni mësimdhënës."));return@post}
                val req=call.receive<TeacherAssignmentRequest>()
                if(req.teacherId.isBlank()||req.classId.isBlank()){call.respond(HttpStatusCode.BadRequest,ApiError("VALIDATION_ERROR","Mësimdhënësi dhe klasa janë të detyrueshme."));return@post}
                val inserted=Database.connection().use { c->c.prepareStatement("INSERT INTO teacher_classes(teacher_id,class_id) VALUES (?,?) ON CONFLICT DO NOTHING").use { ps->ps.setString(1,req.teacherId);ps.setString(2,req.classId);ps.executeUpdate()}}
                if(inserted==0)call.respond(HttpStatusCode.Conflict,ApiError("ALREADY_EXISTS","Caktimi ekziston ose të dhënat nuk janë valide."))else call.respond(HttpStatusCode.Created,req)
            }
            delete("/teacher-assignments/{teacherId}/{classId}") {
                val user=call.managementUser(authService) ?: return@delete
                if(user.role !in setOf("ADMINISTRATOR","DREJTOR")){call.respond(HttpStatusCode.Forbidden,ApiError("FORBIDDEN","Nuk keni të drejtë të hiqni caktimin."));return@delete}
                val deleted=Database.connection().use { c->c.prepareStatement("DELETE FROM teacher_classes WHERE teacher_id=? AND class_id=?").use { ps->ps.setString(1,call.parameters["teacherId"].orEmpty());ps.setString(2,call.parameters["classId"].orEmpty());ps.executeUpdate()}}
                if(deleted==0)call.respond(HttpStatusCode.NotFound,ApiError("NOT_FOUND","Caktimi nuk u gjet."))else call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private suspend fun ApplicationCall.managementUser(auth: AuthService): UserDto? {
    val token=request.headers["Authorization"]?.removePrefix("Bearer ")?.takeIf { it.isNotBlank() }
    if(token==null){respond(HttpStatusCode.Unauthorized,ApiError("UNAUTHORIZED","Kyçja është e nevojshme."));return null}
    val user=auth.userFor(token)
    if(user==null){respond(HttpStatusCode.Unauthorized,ApiError("UNAUTHORIZED","Sesioni nuk është i vlefshëm."));return null}
    return user
}

private fun classIds(c: java.sql.Connection, teacherId: String): List<String> = c.prepareStatement("SELECT class_id FROM teacher_classes WHERE teacher_id=? ORDER BY class_id").use { ps->ps.setString(1,teacherId);ps.executeQuery().use { rs->buildList{while(rs.next())add(rs.getString(1))}}}
private fun teacherIds(c: java.sql.Connection, classId: String): List<String> = c.prepareStatement("SELECT teacher_id FROM teacher_classes WHERE class_id=? ORDER BY teacher_id").use { ps->ps.setString(1,classId);ps.executeQuery().use { rs->buildList{while(rs.next())add(rs.getString(1))}}}
