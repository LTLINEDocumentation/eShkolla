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
data class ScheduleDto(val id: String, val classId: String, val teacherId: String, val subjectId: String, val weekday: Int, val startTime: String, val endTime: String, val room: String? = null, val active: Boolean = true)
@Serializable
data class ScheduleRequest(val classId: String, val teacherId: String, val subjectId: String, val weekday: Int, val startTime: String, val endTime: String, val room: String? = null, val active: Boolean = true)
@Serializable
data class NotificationDto(val id: String, val title: String, val message: String, val audience: String, val createdAt: String, val active: Boolean = true)
@Serializable
data class NotificationRequest(val title: String, val message: String, val audience: String = "ALL")
@Serializable
data class AcademicReport(val studentId: String, val studentName: String, val classId: String, val gradeCount: Int, val averageGrade: Double, val absenceCount: Int, val justifiedAbsences: Int, val unexcusedAbsences: Int)

fun Application.configureSchoolOperationsApi(authService: AuthService) {
    routing {
        route("/api/v1") {
            get("/schedule") {
                val user = requireUser(call, authService) ?: return@get
                val rows = Database.connection().use { c ->
                    val sql = when (user.role) {
                        "MESIMDHENES" -> "SELECT s.id,s.class_id,s.teacher_id,s.subject_id,s.weekday,s.start_time,s.end_time,s.room,s.active FROM schedules s WHERE s.teacher_id=(SELECT id FROM teachers WHERE user_id=? AND active=TRUE LIMIT 1) ORDER BY s.weekday,s.start_time"
                        else -> "SELECT id,class_id,teacher_id,subject_id,weekday,start_time,end_time,room,active FROM schedules WHERE active=TRUE ORDER BY weekday,start_time"
                    }
                    c.prepareStatement(sql).use { ps ->
                        if (user.role == "MESIMDHENES") ps.setString(1, user.id)
                        ps.executeQuery().use { rs -> buildList { while (rs.next()) add(ScheduleDto(rs.getString("id"),rs.getString("class_id"),rs.getString("teacher_id"),rs.getString("subject_id"),rs.getInt("weekday"),rs.getString("start_time"),rs.getString("end_time"),rs.getString("room"),rs.getBoolean("active"))) } }
                    }
                }
                call.respond(rows)
            }
            post("/schedule") {
                val user = requireUser(call, authService) ?: return@post
                if (user.role !in setOf("ADMINISTRATOR","DREJTOR")) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN","Vetëm administratori ose drejtori mund të menaxhojë orarin.")); return@post }
                val req = call.receive<ScheduleRequest>()
                if (!validSchedule(req)) { call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR","Të dhënat e orarit nuk janë valide.")); return@post }
                val id = "SCH-${System.currentTimeMillis()}"
                Database.connection().use { c -> c.prepareStatement("INSERT INTO schedules(id,class_id,teacher_id,subject_id,weekday,start_time,end_time,room,active) VALUES (?,?,?,?,?,?,?,?,?)").use { ps -> ps.setString(1,id);ps.setString(2,req.classId);ps.setString(3,req.teacherId);ps.setString(4,req.subjectId);ps.setInt(5,req.weekday);ps.setString(6,req.startTime);ps.setString(7,req.endTime);ps.setString(8,req.room);ps.setBoolean(9,req.active);ps.executeUpdate() } }
                call.respond(HttpStatusCode.Created, ScheduleDto(id,req.classId,req.teacherId,req.subjectId,req.weekday,req.startTime,req.endTime,req.room,req.active))
            }
            put("/schedule/{id}") {
                val user = requireUser(call, authService) ?: return@put
                if (user.role !in setOf("ADMINISTRATOR","DREJTOR")) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN","Nuk keni të drejtë të ndryshoni orarin.")); return@put }
                val id = call.parameters["id"].orEmpty(); val req = call.receive<ScheduleRequest>()
                if (!validSchedule(req)) { call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR","Të dhënat e orarit nuk janë valide.")); return@put }
                val count = Database.connection().use { c -> c.prepareStatement("UPDATE schedules SET class_id=?,teacher_id=?,subject_id=?,weekday=?,start_time=?,end_time=?,room=?,active=? WHERE id=?").use { ps -> ps.setString(1,req.classId);ps.setString(2,req.teacherId);ps.setString(3,req.subjectId);ps.setInt(4,req.weekday);ps.setString(5,req.startTime);ps.setString(6,req.endTime);ps.setString(7,req.room);ps.setBoolean(8,req.active);ps.setString(9,id);ps.executeUpdate() } }
                if (count == 0) call.respond(HttpStatusCode.NotFound,ApiError("NOT_FOUND","Ora nuk u gjet.")) else call.respond(ScheduleDto(id,req.classId,req.teacherId,req.subjectId,req.weekday,req.startTime,req.endTime,req.room,req.active))
            }
            delete("/schedule/{id}") {
                val user = requireUser(call, authService) ?: return@delete
                if (user.role !in setOf("ADMINISTRATOR","DREJTOR")) { call.respond(HttpStatusCode.Forbidden,ApiError("FORBIDDEN","Nuk keni të drejtë të fshini orarin.")); return@delete }
                val count = Database.connection().use { c -> c.prepareStatement("DELETE FROM schedules WHERE id=?").use { ps -> ps.setString(1,call.parameters["id"].orEmpty());ps.executeUpdate() } }
                if (count == 0) call.respond(HttpStatusCode.NotFound,ApiError("NOT_FOUND","Ora nuk u gjet.")) else call.respond(HttpStatusCode.NoContent)
            }
            get("/notifications") {
                requireUser(call, authService) ?: return@get
                val rows = Database.connection().use { c -> c.prepareStatement("SELECT id,title,message,audience,created_at,active FROM notifications WHERE active=TRUE ORDER BY created_at DESC").use { ps -> ps.executeQuery().use { rs -> buildList { while(rs.next()) add(NotificationDto(rs.getString("id"),rs.getString("title"),rs.getString("message"),rs.getString("audience"),rs.getTimestamp("created_at").toInstant().toString(),rs.getBoolean("active"))) } } } }
                call.respond(rows)
            }
            post("/notifications") {
                val user = requireUser(call, authService) ?: return@post
                if (user.role !in setOf("ADMINISTRATOR","DREJTOR")) { call.respond(HttpStatusCode.Forbidden,ApiError("FORBIDDEN","Vetëm administratori ose drejtori mund të publikojë njoftime."));return@post }
                val req=call.receive<NotificationRequest>()
                if(req.title.isBlank()||req.message.isBlank()||req.audience.isBlank()){call.respond(HttpStatusCode.BadRequest,ApiError("VALIDATION_ERROR","Titulli, mesazhi dhe audienca janë të detyrueshme."));return@post}
                val id="NTF-${System.currentTimeMillis()}"
                Database.connection().use { c->c.prepareStatement("INSERT INTO notifications(id,title,message,audience) VALUES(?,?,?,?)").use{ps->ps.setString(1,id);ps.setString(2,req.title.trim());ps.setString(3,req.message.trim());ps.setString(4,req.audience.trim().uppercase());ps.executeUpdate()}}
                call.respond(HttpStatusCode.Created,NotificationDto(id,req.title.trim(),req.message.trim(),req.audience.trim().uppercase(),java.time.Instant.now().toString(),true))
            }
            get("/reports/academic") {
                val user=requireUser(call,authService) ?: return@get
                val requested=call.request.queryParameters["studentId"]
                val studentId = when(user.role){"NXENES"->studentForUser(user.id);"PRIND"->requested?.takeIf{childOf(user.id,it)};else->requested}
                val rows=Database.connection().use{c->
                    val sql=if(studentId!=null) "SELECT s.id,s.full_name,s.class_id,COUNT(DISTINCT g.id) grade_count,COALESCE(AVG(g.value),0) average_grade,COUNT(DISTINCT a.id) absence_count,COUNT(DISTINCT CASE WHEN a.status='E_ARSYESHME' THEN a.id END) justified_absences,COUNT(DISTINCT CASE WHEN a.status='E_PAAFTESUAR' THEN a.id END) unexcused_absences FROM students s LEFT JOIN grades g ON g.student_id=s.id LEFT JOIN absences a ON a.student_id=s.id WHERE s.id=? GROUP BY s.id,s.full_name,s.class_id" else "SELECT s.id,s.full_name,s.class_id,COUNT(DISTINCT g.id) grade_count,COALESCE(AVG(g.value),0) average_grade,COUNT(DISTINCT a.id) absence_count,COUNT(DISTINCT CASE WHEN a.status='E_ARSYESHME' THEN a.id END) justified_absences,COUNT(DISTINCT CASE WHEN a.status='E_PAAFTESUAR' THEN a.id END) unexcused_absences FROM students s LEFT JOIN grades g ON g.student_id=s.id LEFT JOIN absences a ON a.student_id=s.id WHERE s.active=TRUE GROUP BY s.id,s.full_name,s.class_id ORDER BY s.full_name"
                    c.prepareStatement(sql).use{ps->if(studentId!=null)ps.setString(1,studentId);ps.executeQuery().use{rs->buildList{while(rs.next())add(AcademicReport(rs.getString("id"),rs.getString("full_name"),rs.getString("class_id"),rs.getInt("grade_count"),rs.getDouble("average_grade"),rs.getInt("absence_count"),rs.getInt("justified_absences"),rs.getInt("unexcused_absences")))}}}}
                }
                call.respond(rows)
            }
        }
    }
}

private suspend fun requireUser(call: ApplicationCall, auth: AuthService): UserDto? { val token=call.request.headers["Authorization"]?.removePrefix("Bearer ")?.takeIf{it.isNotBlank()}; if(token==null){call.respond(HttpStatusCode.Unauthorized,ApiError("UNAUTHORIZED","Kyçja është e nevojshme."));return null}; val user=auth.userFor(token);if(user==null){call.respond(HttpStatusCode.Unauthorized,ApiError("UNAUTHORIZED","Sesioni nuk është i vlefshëm."));return null};return user }
private fun validSchedule(r:ScheduleRequest)=r.classId.isNotBlank()&&r.teacherId.isNotBlank()&&r.subjectId.isNotBlank()&&r.weekday in 1..7&&Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(r.startTime)&&Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(r.endTime)
private fun studentForUser(userId:String):String?=Database.connection().use{c->c.prepareStatement("SELECT student_id FROM student_users WHERE user_id=?").use{ps->ps.setString(1,userId);ps.executeQuery().use{rs->if(rs.next())rs.getString(1)else null}}}
private fun childOf(userId:String,studentId:String)=Database.connection().use{c->c.prepareStatement("SELECT 1 FROM parent_students WHERE user_id=? AND student_id=?").use{ps->ps.setString(1,userId);ps.setString(2,studentId);ps.executeQuery().use{it.next()}}}
