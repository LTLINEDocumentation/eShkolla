package com.ltline.eshkolla.api

import com.ltline.eshkolla.auth.AuthService
import com.ltline.eshkolla.db.Database
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Serializable
data class TimetableAssignmentRequest(val classId: String,val weekday: Int,val lessonNumber: Int,val code: Int,val room: String? = null,val active: Boolean = true)
@Serializable
data class TimetableAssignmentResponse(val id: String,val classId: String,val weekday: Int,val lessonNumber: Int,val code: Int,val teacherId: String,val teacherName: String,val subjectId: String,val subjectName: String,val startTime: String,val endTime: String,val room: String? = null)

fun Application.configureTimetableAssignmentApi(authService: AuthService) {
    routing { route("/api/v1/management/timetable-assignment") {
        post {
            if (requireTimetableManager(call, authService) == null) return@post
            val request = call.receive<TimetableAssignmentRequest>()
            val result = assign(request)
            if (result.error != null) { call.respond(result.status, ApiError(result.code, result.error)); return@post }
            call.respond(HttpStatusCode.Created, result.value!!)
        }
        put("/{id}") {
            if (requireTimetableManager(call, authService) == null) return@put
            val id = call.parameters["id"]?.trim().orEmpty()
            if (id.isBlank()) { call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR", "ID i orarit mungon.")); return@put }
            val request = call.receive<TimetableAssignmentRequest>()
            val result = assign(request, id)
            if (result.error != null) { call.respond(result.status, ApiError(result.code, result.error)); return@put }
            call.respond(HttpStatusCode.OK, result.value!!)
        }
    } }
}

private data class AssignmentResult(val status: HttpStatusCode = HttpStatusCode.Created,val code: String = "",val error: String? = null,val value: TimetableAssignmentResponse? = null)

private suspend fun requireTimetableManager(call: ApplicationCall, auth: AuthService): UserDto? {
    val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")?.trim().orEmpty()
    if (token.isBlank()) { call.respond(HttpStatusCode.Unauthorized, ApiError("UNAUTHORIZED", "Kyçja është e nevojshme.")); return null }
    val user = auth.userFor(token)
    if (user == null) { call.respond(HttpStatusCode.Unauthorized, ApiError("UNAUTHORIZED", "Sesioni nuk është i vlefshëm.")); return null }
    if (user.role !in setOf("ADMINISTRATOR", "DREJTOR")) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Vetëm administratori ose drejtori mund të ndërtojë orarin.")); return null }
    return user
}

private fun assign(request: TimetableAssignmentRequest, existingId: String? = null): AssignmentResult {
    if (request.classId.isBlank() || request.weekday !in 1..5 || request.lessonNumber !in 1..12 || request.code !in 1..17) return AssignmentResult(HttpStatusCode.BadRequest,"VALIDATION_ERROR","Klasa, dita, ora ose kodi nuk janë valide.")
    return Database.connection().use { connection ->
        connection.autoCommit = false
        try {
            if (existingId != null) {
                val exists = connection.prepareStatement("SELECT 1 FROM schedules WHERE id=? LIMIT 1").use { ps -> ps.setString(1, existingId); ps.executeQuery().use { it.next() } }
                if (!exists) { connection.rollback(); return@use AssignmentResult(HttpStatusCode.NotFound,"NOT_FOUND","Ora që po ndryshohet nuk ekziston më.") }
            }
            val settings = loadSettings(connection)
            if (request.lessonNumber > settings.lessons) { connection.rollback(); return@use AssignmentResult(HttpStatusCode.BadRequest,"VALIDATION_ERROR","Kjo shkollë ka vetëm ${settings.lessons} orë në ditë.") }
            val period = buildPeriod(settings, request.lessonNumber)
            val code = connection.prepareStatement("SELECT c.teacher_id,c.subject_id,t.full_name,s.name FROM teacher_schedule_codes c JOIN teachers t ON t.id=c.teacher_id AND t.active=TRUE JOIN subjects s ON s.id=c.subject_id AND s.active=TRUE WHERE c.code=? AND c.active=TRUE").use { ps ->
                ps.setInt(1,request.code); ps.executeQuery().use { rs -> if (rs.next()) CodeData(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4)) else null }
            } ?: run { connection.rollback(); return@use AssignmentResult(HttpStatusCode.BadRequest,"CODE_NOT_CONFIGURED","Kodi ${request.code} nuk ka mësimdhënës dhe lëndë të caktuar.") }
            val allowed = connection.prepareStatement("SELECT 1 FROM teacher_subjects ts JOIN classes cl ON cl.id=ts.class_id AND cl.active=TRUE WHERE ts.teacher_id=? AND ts.subject_id=? AND ts.class_id=? LIMIT 1").use { ps ->
                ps.setString(1,code.teacherId); ps.setString(2,code.subjectId); ps.setString(3,request.classId.trim()); ps.executeQuery().use { it.next() }
            }
            if (!allowed) { connection.rollback(); return@use AssignmentResult(HttpStatusCode.Conflict,"TEACHER_CLASS_SUBJECT_MISSING","Mësimdhënësi ${code.teacherName} nuk është i lidhur me këtë klasë/paralele për lëndën ${code.subjectName}.") }
            if (request.active) {
                val slotConflict = connection.prepareStatement("SELECT id FROM schedules WHERE active=TRUE AND weekday=? AND (teacher_id=? OR class_id=?) AND start_time < ? AND end_time > ? AND id<>COALESCE(?, '') LIMIT 1").use { ps ->
                    ps.setInt(1,request.weekday); ps.setString(2,code.teacherId); ps.setString(3,request.classId.trim()); ps.setString(4,period.second); ps.setString(5,period.first); ps.setString(6,existingId)
                    ps.executeQuery().use { rs -> if (rs.next()) rs.getString(1) else null }
                }
                if (slotConflict != null) { connection.rollback(); return@use AssignmentResult(HttpStatusCode.Conflict,"SCHEDULE_CONFLICT","Mësimdhënësi ${code.teacherName} ose kjo klasë/paralele e ka të zënë këtë interval kohor.") }
            }
            val id = existingId ?: "SCH-${UUID.randomUUID()}"
            val changed = if (existingId == null) {
                connection.prepareStatement("INSERT INTO schedules(id,class_id,teacher_id,subject_id,weekday,start_time,end_time,room,active) VALUES(?,?,?,?,?,?,?,?,?)").use { ps ->
                    ps.setString(1,id); ps.setString(2,request.classId.trim()); ps.setString(3,code.teacherId); ps.setString(4,code.subjectId); ps.setInt(5,request.weekday); ps.setString(6,period.first); ps.setString(7,period.second); ps.setString(8,request.room?.trim()?.takeIf { it.isNotBlank() }); ps.setBoolean(9,request.active); ps.executeUpdate()
                }
            } else {
                connection.prepareStatement("UPDATE schedules SET class_id=?,teacher_id=?,subject_id=?,weekday=?,start_time=?,end_time=?,room=?,active=? WHERE id=?").use { ps ->
                    ps.setString(1,request.classId.trim()); ps.setString(2,code.teacherId); ps.setString(3,code.subjectId); ps.setInt(4,request.weekday); ps.setString(5,period.first); ps.setString(6,period.second); ps.setString(7,request.room?.trim()?.takeIf { it.isNotBlank() }); ps.setBoolean(8,request.active); ps.setString(9,id); ps.executeUpdate()
                }
            }
            if (changed == 0) { connection.rollback(); return@use AssignmentResult(HttpStatusCode.NotFound,"NOT_FOUND","Ora që po ndryshohet nuk ekziston më.") }
            connection.commit()
            AssignmentResult(status=if(existingId==null)HttpStatusCode.Created else HttpStatusCode.OK,value=TimetableAssignmentResponse(id,request.classId.trim(),request.weekday,request.lessonNumber,request.code,code.teacherId,code.teacherName,code.subjectId,code.subjectName,period.first,period.second,request.room?.trim()?.takeIf { it.isNotBlank() }))
        } catch (e: Exception) { connection.rollback(); throw e } finally { connection.autoCommit=true }
    }
}

private data class CodeData(val teacherId:String,val subjectId:String,val teacherName:String,val subjectName:String)
private data class Settings(val first:String,val duration:Int,val shortBreak:Int,val longAfter:Int,val longBreak:Int,val lessons:Int)
private fun loadSettings(connection: java.sql.Connection): Settings = connection.prepareStatement("SELECT first_lesson_start,lesson_duration_minutes,short_break_minutes,long_break_after_lesson,long_break_minutes,lessons_per_day FROM school_timetable_settings WHERE id='DEFAULT' LIMIT 1").use { ps -> ps.executeQuery().use { rs -> if (rs.next()) Settings(rs.getString(1),rs.getInt(2),rs.getInt(3),rs.getInt(4),rs.getInt(5),rs.getInt(6)) else Settings("08:20",45,5,3,20,6) } }
private fun buildPeriod(settings:Settings,lesson:Int):Pair<String,String> { val f=DateTimeFormatter.ofPattern("HH:mm"); var cursor=LocalTime.parse(settings.first,f); repeat(lesson-1){ n -> cursor=cursor.plusMinutes(settings.duration.toLong()); cursor=cursor.plusMinutes(if(n+1==settings.longAfter) settings.longBreak.toLong() else settings.shortBreak.toLong()) }; return cursor.format(f) to cursor.plusMinutes(settings.duration.toLong()).format(f) }
