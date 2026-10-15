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
import java.util.UUID

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
                val sql = when (user.role) {
                    "MESIMDHENES" -> "SELECT s.id,s.class_id,s.teacher_id,s.subject_id,s.weekday,s.start_time,s.end_time,s.room,s.active FROM schedules s JOIN teachers t ON t.id=s.teacher_id WHERE t.user_id=? AND t.active=TRUE ORDER BY s.weekday,s.start_time"
                    "PRIND" -> "SELECT s.id,s.class_id,s.teacher_id,s.subject_id,s.weekday,s.start_time,s.end_time,s.room,s.active FROM schedules s WHERE s.active=TRUE AND s.class_id IN (SELECT st.class_id FROM students st JOIN parent_students ps ON ps.student_id=st.id WHERE ps.user_id=? AND st.active=TRUE) ORDER BY s.weekday,s.start_time"
                    "NXENES" -> "SELECT s.id,s.class_id,s.teacher_id,s.subject_id,s.weekday,s.start_time,s.end_time,s.room,s.active FROM schedules s WHERE s.active=TRUE AND s.class_id=(SELECT st.class_id FROM students st JOIN student_users su ON su.student_id=st.id WHERE su.user_id=? AND st.active=TRUE LIMIT 1) ORDER BY s.weekday,s.start_time"
                    "ADMINISTRATOR", "DREJTOR" -> "SELECT id,class_id,teacher_id,subject_id,weekday,start_time,end_time,room,active FROM schedules WHERE active=TRUE ORDER BY weekday,start_time"
                    else -> { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Nuk keni qasje në orar.")); return@get }
                }
                val rows = Database.connection().use { connection ->
                    connection.prepareStatement(sql).use { ps ->
                        if (user.role !in setOf("ADMINISTRATOR", "DREJTOR")) ps.setString(1, user.id)
                        ps.executeQuery().use { rs ->
                            buildList {
                                while (rs.next()) add(ScheduleDto(
                                    rs.getString("id"),rs.getString("class_id"),rs.getString("teacher_id"),
                                    rs.getString("subject_id"),rs.getInt("weekday"),rs.getString("start_time"),
                                    rs.getString("end_time"),rs.getString("room"),rs.getBoolean("active")
                                ))
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
                    call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR", "Të dhënat e orarit nuk janë valide; ora e përfundimit duhet të jetë pas fillimit."))
                    return@post
                }
                if (!scheduleAssignmentExists(request)) {
                    call.respond(HttpStatusCode.BadRequest, ApiError("INVALID_ASSIGNMENT", "Mësimdhënësi nuk është caktuar për këtë lëndë dhe klasë/paralele."))
                    return@post
                }
                if (request.active && scheduleConflict(request)) {
                    call.respond(HttpStatusCode.Conflict, ApiError("SCHEDULE_CONFLICT", "Mësimdhënësi ose klasa e ka të zënë këtë interval kohor."))
                    return@post
                }
                val id = "SCH-${UUID.randomUUID().toString().take(10).uppercase()}"
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
                    call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR", "Të dhënat e orarit nuk janë valide; ora e përfundimit duhet të jetë pas fillimit."))
                    return@put
                }
                if (!scheduleAssignmentExists(request)) {
                    call.respond(HttpStatusCode.BadRequest, ApiError("INVALID_ASSIGNMENT", "Mësimdhënësi nuk është caktuar për këtë lëndë dhe klasë/paralele."))
                    return@put
                }
                if (request.active && scheduleConflict(request, id)) {
                    call.respond(HttpStatusCode.Conflict, ApiError("SCHEDULE_CONFLICT", "Mësimdhënësi ose klasa e ka të zënë këtë interval kohor."))
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

            get("/reports/academic") {
                val user = requireSchoolUser(call, authService) ?: return@get
                val requestedStudentId = call.request.queryParameters["studentId"]?.takeIf { it.isNotBlank() }

                val studentId = when (user.role) {
                    "NXENES" -> {
                        val linkedStudentId = studentForUser(user.id)
                        if (linkedStudentId == null || (requestedStudentId != null && requestedStudentId != linkedStudentId)) {
                            call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Nuk keni qasje në raportin e këtij nxënësi."))
                            return@get
                        }
                        linkedStudentId
                    }
                    "PRIND" -> {
                        if (requestedStudentId != null && !childOf(user.id, requestedStudentId)) {
                            call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Mund të shihni vetëm raportet e fëmijëve të lidhur me llogarinë tuaj."))
                            return@get
                        }
                        requestedStudentId
                    }
                    "MESIMDHENES" -> {
                        if (requestedStudentId != null && !teacherCanSeeStudent(user.id, requestedStudentId)) {
                            call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Mund të shihni vetëm nxënësit e klasave që ju janë caktuar."))
                            return@get
                        }
                        requestedStudentId
                    }
                    "ADMINISTRATOR", "DREJTOR" -> requestedStudentId
                    else -> {
                        call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Nuk keni qasje në raportet akademike."))
                        return@get
                    }
                }

                val scope = when {
                    studentId != null -> "s.id=?"
                    user.role == "PRIND" -> "s.active=TRUE AND s.id IN (SELECT student_id FROM parent_students WHERE user_id=?)"
                    user.role == "MESIMDHENES" -> "s.active=TRUE AND s.class_id IN (SELECT tc.class_id FROM teacher_classes tc JOIN teachers t ON t.id=tc.teacher_id WHERE t.user_id=? AND t.active=TRUE)"
                    else -> "s.active=TRUE"
                }
                val rows = Database.connection().use { connection ->
                    val sql = "SELECT s.id,s.full_name,s.class_id,COUNT(DISTINCT g.id) grade_count," +
                        "COALESCE(AVG(g.value),0) average_grade,COUNT(DISTINCT a.id) absence_count," +
                        "COUNT(DISTINCT CASE WHEN a.status='E_ARSYESHME' THEN a.id END) justified_absences," +
                        "COUNT(DISTINCT CASE WHEN a.status='E_PAAFTESUAR' THEN a.id END) unexcused_absences " +
                        "FROM students s LEFT JOIN grades g ON g.student_id=s.id LEFT JOIN absences a ON a.student_id=s.id " +
                        "WHERE $scope GROUP BY s.id,s.full_name,s.class_id ORDER BY s.full_name"
                    connection.prepareStatement(sql).use { ps ->
                        when {
                            studentId != null -> ps.setString(1, studentId)
                            user.role == "PRIND" || user.role == "MESIMDHENES" -> ps.setString(1, user.id)
                        }
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
        timeRegex.matches(request.endTime) &&
        request.endTime > request.startTime

private fun scheduleAssignmentExists(request: ScheduleRequest): Boolean = Database.connection().use { c ->
    c.prepareStatement(
        "SELECT 1 FROM teacher_subjects ts JOIN teachers t ON t.id=ts.teacher_id JOIN subjects s ON s.id=ts.subject_id JOIN classes cl ON cl.id=ts.class_id WHERE ts.teacher_id=? AND ts.subject_id=? AND ts.class_id=? AND t.active=TRUE AND s.active=TRUE AND cl.active=TRUE LIMIT 1"
    ).use { p ->
        p.setString(1, request.teacherId.trim()); p.setString(2, request.subjectId.trim()); p.setString(3, request.classId.trim())
        p.executeQuery().use { it.next() }
    }
}

private fun scheduleConflict(request: ScheduleRequest, excludingId: String? = null): Boolean = Database.connection().use { c ->
    c.prepareStatement(
        "SELECT 1 FROM schedules WHERE active=TRUE AND weekday=? AND (teacher_id=? OR class_id=?) AND start_time < ? AND end_time > ? AND id<>COALESCE(?, '') LIMIT 1"
    ).use { p ->
        p.setInt(1, request.weekday); p.setString(2, request.teacherId.trim()); p.setString(3, request.classId.trim())
        p.setString(4, request.endTime); p.setString(5, request.startTime); p.setString(6, excludingId)
        p.executeQuery().use { it.next() }
    }
}

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


private fun teacherCanSeeStudent(userId: String, studentId: String): Boolean = Database.connection().use { connection ->
    connection.prepareStatement(
        "SELECT 1 FROM students s JOIN teacher_classes tc ON tc.class_id=s.class_id " +
            "JOIN teachers t ON t.id=tc.teacher_id WHERE t.user_id=? AND t.active=TRUE AND s.id=? LIMIT 1"
    ).use { ps ->
        ps.setString(1, userId)
        ps.setString(2, studentId)
        ps.executeQuery().use { it.next() }
    }
}
