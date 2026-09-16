package com.ltline.eshkolla.api

import com.ltline.eshkolla.auth.AuthService
import com.ltline.eshkolla.db.Database
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Serializable
data class TimetableSettingsDto(
    val id: String,
    val schoolId: String? = null,
    val firstLessonStart: String,
    val lessonDurationMinutes: Int,
    val shortBreakMinutes: Int,
    val longBreakAfterLesson: Int,
    val longBreakMinutes: Int,
    val lessonsPerDay: Int,
    val active: Boolean = true
)

@Serializable
data class TimetableSettingsRequest(
    val schoolId: String? = null,
    val firstLessonStart: String,
    val lessonDurationMinutes: Int,
    val shortBreakMinutes: Int,
    val longBreakAfterLesson: Int,
    val longBreakMinutes: Int,
    val lessonsPerDay: Int,
    val active: Boolean = true
)

@Serializable
data class LessonPeriodDto(
    val lessonNumber: Int,
    val startTime: String,
    val endTime: String,
    val breakAfterMinutes: Int,
    val breakTypeAfter: String
)

@Serializable
data class TimetableDto(
    val settings: TimetableSettingsDto,
    val periods: List<LessonPeriodDto>,
    val schedule: List<ScheduleDto>
)

fun Application.configureTimetableApi(authService: AuthService) {
    routing {
        route("/api/v1") {
            get("/timetable") {
                val user = requireTimetableUser(call, authService) ?: return@get
                val settings = loadSettings(call.request.queryParameters["schoolId"])
                val periods = buildPeriods(settings)
                val schedule = loadScopedSchedule(user, call.request.queryParameters["classId"])
                call.respond(TimetableDto(settings, periods, schedule))
            }

            get("/management/timetable-settings") {
                val user = requireTimetableUser(call, authService) ?: return@get
                if (user.role !in setOf("ADMINISTRATOR", "DREJTOR")) {
                    call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Nuk keni të drejtë të menaxhoni orarin e shkollës."))
                    return@get
                }
                call.respond(loadSettings(call.request.queryParameters["schoolId"]))
            }

            put("/management/timetable-settings") {
                val user = requireTimetableUser(call, authService) ?: return@put
                if (user.role !in setOf("ADMINISTRATOR", "DREJTOR")) {
                    call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Nuk keni të drejtë të ndryshoni orarin e shkollës."))
                    return@put
                }
                val request = call.receive<TimetableSettingsRequest>()
                if (!validSettings(request)) {
                    call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR", "Parametrat e orarit nuk janë valide."))
                    return@put
                }
                val id = request.schoolId?.trim()?.takeIf { it.isNotBlank() }?.let { "SCHOOL-$it" } ?: "DEFAULT"
                Database.connection().use { connection ->
                    val updated = connection.prepareStatement(
                        "UPDATE school_timetable_settings SET school_id=?,first_lesson_start=?,lesson_duration_minutes=?,short_break_minutes=?,long_break_after_lesson=?,long_break_minutes=?,lessons_per_day=?,active=? WHERE id=?"
                    ).use { ps ->
                        ps.setString(1, request.schoolId?.trim()?.takeIf { it.isNotBlank() })
                        ps.setString(2, request.firstLessonStart)
                        ps.setInt(3, request.lessonDurationMinutes)
                        ps.setInt(4, request.shortBreakMinutes)
                        ps.setInt(5, request.longBreakAfterLesson)
                        ps.setInt(6, request.longBreakMinutes)
                        ps.setInt(7, request.lessonsPerDay)
                        ps.setBoolean(8, request.active)
                        ps.setString(9, id)
                        ps.executeUpdate()
                    }
                    if (updated == 0) {
                        connection.prepareStatement(
                            "INSERT INTO school_timetable_settings(id,school_id,first_lesson_start,lesson_duration_minutes,short_break_minutes,long_break_after_lesson,long_break_minutes,lessons_per_day,active) VALUES(?,?,?,?,?,?,?,?,?)"
                        ).use { ps ->
                            ps.setString(1, id)
                            ps.setString(2, request.schoolId?.trim()?.takeIf { it.isNotBlank() })
                            ps.setString(3, request.firstLessonStart)
                            ps.setInt(4, request.lessonDurationMinutes)
                            ps.setInt(5, request.shortBreakMinutes)
                            ps.setInt(6, request.longBreakAfterLesson)
                            ps.setInt(7, request.longBreakMinutes)
                            ps.setInt(8, request.lessonsPerDay)
                            ps.setBoolean(9, request.active)
                            ps.executeUpdate()
                        }
                    }
                }
                call.respond(loadSettings(request.schoolId))
            }
        }
    }
}

private suspend fun requireTimetableUser(call: ApplicationCall, auth: AuthService): UserDto? {
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

private fun loadSettings(schoolId: String?): TimetableSettingsDto = Database.connection().use { connection ->
    val normalized = schoolId?.trim()?.takeIf { it.isNotBlank() }
    val sql = if (normalized == null) {
        "SELECT id,school_id,first_lesson_start,lesson_duration_minutes,short_break_minutes,long_break_after_lesson,long_break_minutes,lessons_per_day,active FROM school_timetable_settings WHERE id='DEFAULT' LIMIT 1"
    } else {
        "SELECT id,school_id,first_lesson_start,lesson_duration_minutes,short_break_minutes,long_break_after_lesson,long_break_minutes,lessons_per_day,active FROM school_timetable_settings WHERE school_id=? AND active=TRUE LIMIT 1"
    }
    connection.prepareStatement(sql).use { ps ->
        if (normalized != null) ps.setString(1, normalized)
        ps.executeQuery().use { rs ->
            if (!rs.next()) {
                return@use TimetableSettingsDto("DEFAULT", null, "08:00", 45, 5, 4, 30, 6, true)
            }
            TimetableSettingsDto(
                rs.getString("id"), rs.getString("school_id"), rs.getString("first_lesson_start"),
                rs.getInt("lesson_duration_minutes"), rs.getInt("short_break_minutes"),
                rs.getInt("long_break_after_lesson"), rs.getInt("long_break_minutes"),
                rs.getInt("lessons_per_day"), rs.getBoolean("active")
            )
        }
    }
}

private fun buildPeriods(settings: TimetableSettingsDto): List<LessonPeriodDto> {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    var cursor = LocalTime.parse(settings.firstLessonStart, formatter)
    return (1..settings.lessonsPerDay).map { number ->
        val start = cursor
        val end = start.plusMinutes(settings.lessonDurationMinutes.toLong())
        val isLong = number == settings.longBreakAfterLesson && number < settings.lessonsPerDay
        val breakMinutes = if (number == settings.lessonsPerDay) 0 else if (isLong) settings.longBreakMinutes else settings.shortBreakMinutes
        val breakType = if (breakMinutes == 0) "NONE" else if (isLong) "PUSHIM_I_GJATE" else "PAUZE"
        cursor = end.plusMinutes(breakMinutes.toLong())
        LessonPeriodDto(number, start.format(formatter), end.format(formatter), breakMinutes, breakType)
    }
}

private fun validSettings(request: TimetableSettingsRequest): Boolean =
    timeRegex.matches(request.firstLessonStart) &&
        request.lessonDurationMinutes in 20..120 &&
        request.shortBreakMinutes in 0..30 &&
        request.longBreakMinutes in 0..90 &&
        request.lessonsPerDay in 1..12 &&
        request.longBreakAfterLesson in 1..request.lessonsPerDay

private val timeRegex = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

private fun loadScopedSchedule(user: UserDto, classId: String?): List<ScheduleDto> = Database.connection().use { connection ->
    val requested = classId?.trim()?.takeIf { it.isNotBlank() }
    val sql: String
    val parameter: String?
    when (user.role) {
        "MESIMDHENES" -> {
            sql = if (requested == null) "SELECT s.id,s.class_id,s.teacher_id,s.subject_id,s.weekday,s.start_time,s.end_time,s.room,s.active FROM schedules s JOIN teachers t ON t.id=s.teacher_id WHERE t.user_id=? AND t.active=TRUE AND s.active=TRUE ORDER BY s.weekday,s.start_time" else "SELECT s.id,s.class_id,s.teacher_id,s.subject_id,s.weekday,s.start_time,s.end_time,s.room,s.active FROM schedules s JOIN teachers t ON t.id=s.teacher_id WHERE t.user_id=? AND t.active=TRUE AND s.active=TRUE AND s.class_id=? ORDER BY s.weekday,s.start_time"
            parameter = user.id
        }
        "NXENES" -> {
            val studentId = findStudentId(user.id)
            if (studentId == null) return@use emptyList()
            val ownClass = findStudentClass(studentId)
            sql = "SELECT s.id,s.class_id,s.teacher_id,s.subject_id,s.weekday,s.start_time,s.end_time,s.room,s.active FROM schedules s WHERE s.class_id=? AND s.active=TRUE ORDER BY s.weekday,s.start_time"
            parameter = ownClass
        }
        "PRIND" -> {
            sql = "SELECT s.id,s.class_id,s.teacher_id,s.subject_id,s.weekday,s.start_time,s.end_time,s.room,s.active FROM schedules s WHERE s.class_id IN (SELECT st.class_id FROM students st JOIN parent_students p ON p.student_id=st.id WHERE p.user_id=? AND st.active=TRUE) AND s.active=TRUE ORDER BY s.weekday,s.start_time"
            parameter = user.id
        }
        else -> {
            sql = if (requested == null) "SELECT id,class_id,teacher_id,subject_id,weekday,start_time,end_time,room,active FROM schedules WHERE active=TRUE ORDER BY weekday,start_time" else "SELECT id,class_id,teacher_id,subject_id,weekday,start_time,end_time,room,active FROM schedules WHERE active=TRUE AND class_id=? ORDER BY weekday,start_time"
            parameter = requested
        }
    }
    connection.prepareStatement(sql).use { ps ->
        ps.setString(1, parameter ?: "")
        if (user.role == "MESIMDHENES" && requested != null) ps.setString(2, requested)
        ps.executeQuery().use { rs ->
            buildList {
                while (rs.next()) add(ScheduleDto(rs.getString("id"),rs.getString("class_id"),rs.getString("teacher_id"),rs.getString("subject_id"),rs.getInt("weekday"),rs.getString("start_time"),rs.getString("end_time"),rs.getString("room"),rs.getBoolean("active")))
            }
        }
    }
}

private fun findStudentId(userId: String): String? = Database.connection().use { c -> c.prepareStatement("SELECT student_id FROM student_users WHERE user_id=? LIMIT 1").use { p -> p.setString(1,userId); p.executeQuery().use { r -> if(r.next()) r.getString(1) else null } } }
private fun findStudentClass(studentId: String): String? = Database.connection().use { c -> c.prepareStatement("SELECT class_id FROM students WHERE id=? LIMIT 1").use { p -> p.setString(1,studentId); p.executeQuery().use { r -> if(r.next()) r.getString(1) else null } } }
