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
                call.respond(
                    TimetableDto(
                        settings,
                        buildPeriods(settings),
                        loadScopedSchedule(user, call.request.queryParameters["classId"])
                    )
                )
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
                val school = request.schoolId?.trim()?.takeIf { it.isNotBlank() }
                val id = school?.let { "SCHOOL-$it" } ?: "DEFAULT"
                Database.connection().use { connection ->
                    val updated = connection.prepareStatement(
                        "UPDATE school_timetable_settings SET school_id=?,first_lesson_start=?,lesson_duration_minutes=?,short_break_minutes=?,long_break_after_lesson=?,long_break_minutes=?,lessons_per_day=?,active=? WHERE id=?"
                    ).use { statement ->
                        statement.setString(1, school)
                        statement.setString(2, request.firstLessonStart)
                        statement.setInt(3, request.lessonDurationMinutes)
                        statement.setInt(4, request.shortBreakMinutes)
                        statement.setInt(5, request.longBreakAfterLesson)
                        statement.setInt(6, request.longBreakMinutes)
                        statement.setInt(7, request.lessonsPerDay)
                        statement.setBoolean(8, request.active)
                        statement.setString(9, id)
                        statement.executeUpdate()
                    }
                    if (updated == 0) {
                        connection.prepareStatement(
                            "INSERT INTO school_timetable_settings(id,school_id,first_lesson_start,lesson_duration_minutes,short_break_minutes,long_break_after_lesson,long_break_minutes,lessons_per_day,active) VALUES(?,?,?,?,?,?,?,?,?)"
                        ).use { statement ->
                            statement.setString(1, id)
                            statement.setString(2, school)
                            statement.setString(3, request.firstLessonStart)
                            statement.setInt(4, request.lessonDurationMinutes)
                            statement.setInt(5, request.shortBreakMinutes)
                            statement.setInt(6, request.longBreakAfterLesson)
                            statement.setInt(7, request.longBreakMinutes)
                            statement.setInt(8, request.lessonsPerDay)
                            statement.setBoolean(9, request.active)
                            statement.executeUpdate()
                        }
                    }
                }
                call.respond(loadSettings(school))
            }
        }
    }
}

private suspend fun requireTimetableUser(call: ApplicationCall, auth: AuthService): UserDto? {
    val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")?.trim().orEmpty()
    if (token.isBlank()) {
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

private fun loadSettings(schoolId: String?): TimetableSettingsDto {
    return Database.connection().use { connection ->
        val school = schoolId?.trim()?.takeIf { it.isNotBlank() }
        val sql = if (school == null) {
            "SELECT id,school_id,first_lesson_start,lesson_duration_minutes,short_break_minutes,long_break_after_lesson,long_break_minutes,lessons_per_day,active FROM school_timetable_settings WHERE id='DEFAULT' LIMIT 1"
        } else {
            "SELECT id,school_id,first_lesson_start,lesson_duration_minutes,short_break_minutes,long_break_after_lesson,long_break_minutes,lessons_per_day,active FROM school_timetable_settings WHERE school_id=? AND active=TRUE LIMIT 1"
        }
        connection.prepareStatement(sql).use { statement ->
            if (school != null) statement.setString(1, school)
            statement.executeQuery().use { resultSet ->
                if (!resultSet.next()) {
                    return@use TimetableSettingsDto("DEFAULT", null, "08:00", 45, 5, 4, 30, 6, true)
                }
                TimetableSettingsDto(
                    id = resultSet.getString("id"),
                    schoolId = resultSet.getString("school_id"),
                    firstLessonStart = resultSet.getString("first_lesson_start"),
                    lessonDurationMinutes = resultSet.getInt("lesson_duration_minutes"),
                    shortBreakMinutes = resultSet.getInt("short_break_minutes"),
                    longBreakAfterLesson = resultSet.getInt("long_break_after_lesson"),
                    longBreakMinutes = resultSet.getInt("long_break_minutes"),
                    lessonsPerDay = resultSet.getInt("lessons_per_day"),
                    active = resultSet.getBoolean("active")
                )
            }
        }
    }
}

private fun buildPeriods(settings: TimetableSettingsDto): List<LessonPeriodDto> {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    var cursor = LocalTime.parse(settings.firstLessonStart, formatter)
    return (1..settings.lessonsPerDay).map { number ->
        val start = cursor
        val end = start.plusMinutes(settings.lessonDurationMinutes.toLong())
        val longBreak = number == settings.longBreakAfterLesson && number < settings.lessonsPerDay
        val breakMinutes = when {
            number == settings.lessonsPerDay -> 0
            longBreak -> settings.longBreakMinutes
            else -> settings.shortBreakMinutes
        }
        cursor = end.plusMinutes(breakMinutes.toLong())
        LessonPeriodDto(
            lessonNumber = number,
            startTime = start.format(formatter),
            endTime = end.format(formatter),
            breakAfterMinutes = breakMinutes,
            breakTypeAfter = when {
                breakMinutes == 0 -> "NONE"
                longBreak -> "PUSHIM_I_GJATE"
                else -> "PAUZE"
            }
        )
    }
}

private fun validSettings(request: TimetableSettingsRequest): Boolean {
    return timeRegex.matches(request.firstLessonStart) &&
        request.lessonDurationMinutes in 20..120 &&
        request.shortBreakMinutes in 0..30 &&
        request.longBreakMinutes in 0..90 &&
        request.lessonsPerDay in 1..12 &&
        request.longBreakAfterLesson in 1..request.lessonsPerDay
}

private val timeRegex = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

private fun loadScopedSchedule(user: UserDto, classId: String?): List<ScheduleDto> {
    return Database.connection().use { connection ->
        val requestedClass = classId?.trim()?.takeIf { it.isNotBlank() }
        val sql: String
        val params = mutableListOf<String>()

        when (user.role) {
            "MESIMDHENES" -> {
                sql = if (requestedClass == null) {
                    "SELECT s.id,s.class_id,s.teacher_id,s.subject_id,s.weekday,s.start_time,s.end_time,s.room,s.active FROM schedules s JOIN teachers t ON t.id=s.teacher_id WHERE t.user_id=? AND t.active=TRUE AND s.active=TRUE ORDER BY s.weekday,s.start_time"
                } else {
                    "SELECT s.id,s.class_id,s.teacher_id,s.subject_id,s.weekday,s.start_time,s.end_time,s.room,s.active FROM schedules s JOIN teachers t ON t.id=s.teacher_id WHERE t.user_id=? AND t.active=TRUE AND s.active=TRUE AND s.class_id=? ORDER BY s.weekday,s.start_time"
                }
                params += user.id
                if (requestedClass != null) params += requestedClass
            }
            "NXENES" -> {
                val studentId = findStudentId(user.id) ?: return@use emptyList()
                val studentClass = findStudentClass(studentId) ?: return@use emptyList()
                sql = "SELECT id,class_id,teacher_id,subject_id,weekday,start_time,end_time,room,active FROM schedules WHERE class_id=? AND active=TRUE ORDER BY weekday,start_time"
                params += studentClass
            }
            "PRIND" -> {
                sql = "SELECT s.id,s.class_id,s.teacher_id,s.subject_id,s.weekday,s.start_time,s.end_time,s.room,s.active FROM schedules s WHERE s.class_id IN (SELECT st.class_id FROM students st JOIN parent_students p ON p.student_id=st.id WHERE p.user_id=? AND st.active=TRUE) AND s.active=TRUE ORDER BY s.weekday,s.start_time"
                params += user.id
            }
            else -> {
                sql = if (requestedClass == null) {
                    "SELECT id,class_id,teacher_id,subject_id,weekday,start_time,end_time,room,active FROM schedules WHERE active=TRUE ORDER BY weekday,start_time"
                } else {
                    "SELECT id,class_id,teacher_id,subject_id,weekday,start_time,end_time,room,active FROM schedules WHERE active=TRUE AND class_id=? ORDER BY weekday,start_time"
                }
                if (requestedClass != null) params += requestedClass
            }
        }

        connection.prepareStatement(sql).use { statement ->
            params.forEachIndexed { index, value -> statement.setString(index + 1, value) }
            statement.executeQuery().use { resultSet ->
                buildList {
                    while (resultSet.next()) {
                        add(
                            ScheduleDto(
                                id = resultSet.getString("id"),
                                classId = resultSet.getString("class_id"),
                                teacherId = resultSet.getString("teacher_id"),
                                subjectId = resultSet.getString("subject_id"),
                                weekday = resultSet.getInt("weekday"),
                                startTime = resultSet.getString("start_time"),
                                endTime = resultSet.getString("end_time"),
                                room = resultSet.getString("room"),
                                active = resultSet.getBoolean("active")
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun findStudentId(userId: String): String? {
    return Database.connection().use { connection ->
        connection.prepareStatement("SELECT student_id FROM student_users WHERE user_id=? LIMIT 1").use { statement ->
            statement.setString(1, userId)
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) resultSet.getString(1) else null
            }
        }
    }
}

private fun findStudentClass(studentId: String): String? {
    return Database.connection().use { connection ->
        connection.prepareStatement("SELECT class_id FROM students WHERE id=? LIMIT 1").use { statement ->
            statement.setString(1, studentId)
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) resultSet.getString(1) else null
            }
        }
    }
}
