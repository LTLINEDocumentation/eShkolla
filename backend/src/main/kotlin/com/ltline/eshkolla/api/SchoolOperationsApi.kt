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
data class ScheduleDto(
    val id: String,
    val classId: String,
    val teacherId: String,
    val subjectId: String,
    val weekday: Int,
    val startTime: String,
    val endTime: String,
    val room: String? = null,
    val active: Boolean = true
)

@Serializable
data class ScheduleRequest(
    val classId: String,
    val teacherId: String,
    val subjectId: String,
    val weekday: Int,
    val startTime: String,
    val endTime: String,
    val room: String? = null,
    val active: Boolean = true
)

@Serializable
data class NotificationDto(
    val id: String,
    val title: String,
    val message: String,
    val audience: String,
    val createdAt: String,
    val active: Boolean = true
)

@Serializable
data class NotificationRequest(
    val title: String,
    val message: String,
    val audience: String = "ALL"
)

@Serializable
data class AcademicReport(
    val studentId: String,
    val studentName: String,
    val classId: String,
    val gradeCount: Int,
    val averageGrade: Double,
    val absenceCount: Int,
    val justifiedAbsences: Int,
    val unexcusedAbsences: Int
)

fun Application.configureSchoolOperationsApi(authService: AuthService) {
    routing {
        route("/api/v1") {
            get("/schedule") {
                val user = requireSchoolUser(call, authService) ?: return@get
                val rows = Database.connection().use { connection ->
                    val sql = if (user.role == "MESIMDHENES") {
                        "SELECT s.id,s.class_id,s.teacher_id,s.subject_id,s.weekday,s.start_time,s.end_time,s.room,s.active " +
                            "FROM schedules s JOIN teachers t ON t.id=s.teacher_id " +
                            "WHERE t.user_id=? AND t.active=TRUE ORDER BY s.weekday,s.start_time"
                    } else {
                        "SELECT id,class_id,teacher_id,subject_id,weekday,start_time,end_time,room,active " +
                            "FROM schedules WHERE active=TRUE ORDER BY weekday,start_time"
                    }
                    connection.prepareStatement(sql).use { ps ->
                        if (user.role == "MESIMDHENES") ps.setString(1, user.id)
                        ps.executeQuery().use { rs ->
                            buildList {
                                while (rs.next()) {
                                    add(
                                        ScheduleDto(
                                            rs.getString("id"),
                                            rs.getString("class_id"),
                                            rs.getString("teacher_id"),
                                            rs.getString("subject_id"),
                                            rs.getInt("weekday"),
                                            rs.getString("start_time"),
                                            rs.getString("end_time"),
                                            rs.getString("room"),
                                            rs.getBoolean("active")
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                call.respond(rows)
            }

            post("/schedule") {
                val user = requireSchoolUser(call, authService) ?: return@post
                if (user.role !in setOf("ADMINISTRATOR", "DREJTOR")) {
                    call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Nuk keni të drejtë të menaxhoni orarin."))
                    return@post
                }
                val request = call.receive<ScheduleRequest>()
                if (!validSchedule(request)) {
                    call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR", "Të dhënat e orarit nuk janë valide."))
                    return@post
                }
                val id = "SCH-${System.currentTimeMillis()}"
                Database.connection().use { connection ->
                    connection.prepareStatement(
                        "INSERT INTO schedules(id,class_id,teacher_id,subject_id,weekday,start_time,end_time,room,active) VALUES (?,?,?,?,?,?,?,?,?)"
                    ).use { ps ->
                        ps.setString(1, id)
                        ps.setString(2, request.classId.trim())
                        ps.setString(3, request.teacherId.trim())
                        ps.setString(4, request.subjectId.trim())
                        ps.setInt(5, request.weekday)
                        ps.setString(6, request.startTime)
                        ps.setString(7, request.endTime)
                        ps.setString(8, request.room?.trim())
                        ps.setBoolean(9, request.active)
                        ps.executeUpdate()
                    }
                }
                call.respond(HttpStatusCode.Created, request.toDto(id))
            }

            put("/schedule/{id}") {
                val user = requireSchoolUser(call, authService) ?: return@put
                if (user.role !in setOf("ADMINISTRATOR", "DREJTOR")) {
                    call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Nuk keni të drejtë të ndryshoni orarin."))
                    return@put
                }
                val id = call.parameters["id"].orEmpty()
                val request = call.receive<ScheduleRequest>()
                if (!validSchedule(request)) {
                    call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR", "Të dhënat e orarit nuk janë valide."))
                    return@put
                }
                val updated = Database.connection().use { connection ->
                    connection.prepareStatement(
                        "UPDATE schedules SET class_id=?,teacher_id=?,subject_id=?,weekday=?,start_time=?,end_time=?,room=?,active=? WHERE id=?"
                    ).use { ps ->
                        ps.setString(1, request.classId.trim())
                        ps.setString(2, request.teacherId.trim())
                        ps.setString(3, request.subjectId.trim())
                        ps.setInt(4, request.weekday)
                        ps.setString(5, request.startTime)
                        ps.setString(6, request.endTime)
                        ps.setString(7, request.room?.trim())
                        ps.setBoolean(8, request.active)
                        ps.setString(9, id)
                        ps.executeUpdate()
                    }
                }
                if (updated == 0) {
                    call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Ora nuk u gjet."))
                } else {
                    call.respond(request.toDto(id))
                }
            }

            delete("/schedule/{id}") {
                val user = requireSchoolUser(call, authService) ?: return@delete
                if (user.role !in setOf("ADMINISTRATOR", "DREJTOR")) {
                    call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Nuk keni të drejtë të fshini orarin."))
                    return@delete
                }
                val id = call.parameters["id"].orEmpty()
                val deleted = Database.connection().use { connection ->
                    connection.prepareStatement("DELETE FROM schedules WHERE id=?").use { ps ->
                        ps.setString(1, id)
                        ps.executeUpdate()
                    }
                }
                if (deleted == 0) call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Ora nuk u gjet."))
                else call.respond(HttpStatusCode.NoContent)
            }

            get("/notifications") {
                requireSchoolUser(call, authService) ?: return@get
                val rows = Database.connection().use { connection ->
                    connection.prepareStatement(
                        "SELECT id,title,message,audience,created_at,active FROM notifications WHERE active=TRUE ORDER BY created_at DESC"
                    ).use { ps ->
                        ps.executeQuery().use { rs ->
                            buildList {
                                while (rs.next()) {
                                    add(
                                        NotificationDto(
                                            rs.getString("id"),
                                            rs.getString("title"),
                                            rs.getString("message"),
                                            rs.getString("audience"),
                                            rs.getTimestamp("created_at").toInstant().toString(),
                                            rs.getBoolean("active")
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                call.respond(rows)
            }

            post("/notifications") {
                val user = requireSchoolUser(call, authService) ?: return@post
                if (user.role !in setOf("ADMINISTRATOR", "DREJTOR")) {
                    call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Vetëm administratori ose drejtori mund të publikojë njoftime."))
                    return@post
                }
                val request = call.receive<NotificationRequest>()
                if (request.title.isBlank() || request.message.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR", "Titulli dhe mesazhi janë të detyrueshme."))
                    return@post
                }
                val id = "NTF-${System.currentTimeMillis()}"
                val title = request.title.trim()
                val message = request.message.trim()
                val audience = request.audience.trim().uppercase().ifBlank { "ALL" }
                Database.connection().use { connection ->
                    connection.prepareStatement("INSERT INTO notifications(id,title,message,audience) VALUES(?,?,?,?)").use { ps ->
                        ps.setString(1, id)
                        ps.setString(2, title)
                        ps.setString(3, message)
                        ps.setString(4, audience)
                        ps.executeUpdate()
                    }
                }
                call.respond(
                    HttpStatusCode.Created,
                    NotificationDto(id, title, message, audience, java.time.Instant.now().toString(), true)
                )
            }

            get("/reports/academic") {
                val user = requireSchoolUser(call, authService) ?: return@get
                val requestedStudentId = call.request.queryParameters["studentId"]
                val studentId = when (user.role) {
                    "NXENES" -> studentForUser(user.id)
                    "PRIND" -> requestedStudentId?.takeIf { childOf(user.id, it) }
                    else -> requestedStudentId
                }
                val rows = Database.connection().use { connection ->
                    val sql = if (studentId != null) {
                        "SELECT s.id,s.full_name,s.class_id,COUNT(DISTINCT g.id) grade_count," +
                            "COALESCE(AVG(g.value),0) average_grade,COUNT(DISTINCT a.id) absence_count," +
                            "COUNT(DISTINCT CASE WHEN a.status='E_ARSYESHME' THEN a.id END) justified_absences," +
                            "COUNT(DISTINCT CASE WHEN a.status='E_PAAFTESUAR' THEN a.id END) unexcused_absences " +
                            "FROM students s LEFT JOIN grades g ON g.student_id=s.id LEFT JOIN absences a ON a.student_id=s.id " +
                            "WHERE s.id=? GROUP BY s.id,s.full_name,s.class_id"
                    } else {
                        "SELECT s.id,s.full_name,s.class_id,COUNT(DISTINCT g.id) grade_count," +
                            "COALESCE(AVG(g.value),0) average_grade,COUNT(DISTINCT a.id) absence_count," +
                            "COUNT(DISTINCT CASE WHEN a.status='E_ARSYESHME' THEN a.id END) justified_absences," +
                            "COUNT(DISTINCT CASE WHEN a.status='E_PAAFTESUAR' THEN a.id END) unexcused_absences " +
                            "FROM students s LEFT JOIN grades g ON g.student_id=s.id LEFT JOIN absences a ON a.student_id=s.id " +
                            "WHERE s.active=TRUE GROUP BY s.id,s.full_name,s.class_id ORDER BY s.full_name"
                    }
                    connection.prepareStatement(sql).use { ps ->
                        if (studentId != null) ps.setString(1, studentId)
                        ps.executeQuery().use { rs ->
                            buildList {
                                while (rs.next()) {
                                    add(
                                        AcademicReport(
                                            rs.getString("id"),
                                            rs.getString("full_name"),
                                            rs.getString("class_id"),
                                            rs.getInt("grade_count"),
                                            rs.getDouble("average_grade"),
                                            rs.getInt("absence_count"),
                                            rs.getInt("justified_absences"),
                                            rs.getInt("unexcused_absences")
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                call.respond(rows)
            }
        }
    }
}

private suspend fun requireSchoolUser(call: ApplicationCall, auth: AuthService): UserDto? {
    val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")?.takeIf { it.isNotBlank() }
    if (token == null) {
        call.respond(HttpStatusCode.Unauthorized, ApiError("UNAUTHORIZED", "Kyçja është e nevojshme."))
        return null
    }
    val user = auth.userFor(token)
    if (user == null) {
        call.respond(HttpStatusCode.Unauthorized, ApiError("UNAUTHORIZED", "Sesioni nuk është i vlefshëm."))
        return null
    }
    return user
}

private fun validSchedule(request: ScheduleRequest): Boolean =
    request.classId.isNotBlank() &&
        request.teacherId.isNotBlank() &&
        request.subjectId.isNotBlank() &&
        request.weekday in 1..7 &&
        timeRegex.matches(request.startTime) &&
        timeRegex.matches(request.endTime)

private val timeRegex = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

private fun ScheduleRequest.toDto(id: String) = ScheduleDto(
    id, classId.trim(), teacherId.trim(), subjectId.trim(), weekday, startTime, endTime, room?.trim(), active
)

private fun studentForUser(userId: String): String? = Database.connection().use { connection ->
    connection.prepareStatement("SELECT student_id FROM student_users WHERE user_id=? LIMIT 1").use { ps ->
        ps.setString(1, userId)
        ps.executeQuery().use { rs -> if (rs.next()) rs.getString(1) else null }
    }
}

private fun childOf(userId: String, studentId: String): Boolean = Database.connection().use { connection ->
    connection.prepareStatement("SELECT 1 FROM parent_students WHERE user_id=? AND student_id=? LIMIT 1").use { ps ->
        ps.setString(1, userId)
        ps.setString(2, studentId)
        ps.executeQuery().use { it.next() }
    }
}
