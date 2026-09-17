package com.ltline.eshkolla.api

import com.ltline.eshkolla.auth.AuthService
import com.ltline.eshkolla.db.Database
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable

@Serializable
data class AdminGradeData(val id:String,val studentId:String,val studentName:String,val subjectId:String,val subjectName:String,val teacherId:String,val teacherName:String,val value:Int,val period:String,val academicYear:String,val note:String?)
@Serializable
data class AdminAbsenceData(val id:String,val studentId:String,val studentName:String,val subjectId:String,val subjectName:String,val teacherId:String,val teacherName:String,val date:String,val status:String,val note:String?)

fun Application.configureAdminDataApi(authService: AuthService) {
    routing {
        get("/api/v1/management/grades") {
            if (!requireAdminDataAccess(call, authService)) return@get
            val rows = Database.connection().use { c -> c.prepareStatement("SELECT g.id,g.student_id,st.full_name,g.subject_id,COALESCE(su.name,g.subject_id),g.teacher_id,COALESCE(t.full_name,g.teacher_id),g.value,g.period,g.academic_year,g.note FROM grades g JOIN students st ON st.id=g.student_id LEFT JOIN subjects su ON su.id=g.subject_id LEFT JOIN teachers t ON t.id=g.teacher_id ORDER BY g.academic_year DESC,g.period,st.full_name").use { ps -> ps.executeQuery().use { rs -> buildList { while (rs.next()) add(AdminGradeData(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getString(7),rs.getInt(8),rs.getString(9),rs.getString(10),rs.getString(11))) } } } }
            call.respond(rows)
        }
        get("/api/v1/management/absences") {
            if (!requireAdminDataAccess(call, authService)) return@get
            val rows = Database.connection().use { c -> c.prepareStatement("SELECT a.id,a.student_id,st.full_name,a.subject_id,COALESCE(su.name,a.subject_id),a.teacher_id,COALESCE(t.full_name,a.teacher_id),a.date,a.status,a.note FROM absences a JOIN students st ON st.id=a.student_id LEFT JOIN subjects su ON su.id=a.subject_id LEFT JOIN teachers t ON t.id=a.teacher_id ORDER BY a.date DESC,st.full_name").use { ps -> ps.executeQuery().use { rs -> buildList { while (rs.next()) add(AdminAbsenceData(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getString(7),rs.getString(8),rs.getString(9),rs.getString(10))) } } } }
            call.respond(rows)
        }
    }
}

private suspend fun requireAdminDataAccess(call: io.ktor.server.application.ApplicationCall, auth: AuthService): Boolean {
    val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")?.takeIf { it.isNotBlank() }
    val user = token?.let(auth::userFor)
    if (user == null) { call.respond(HttpStatusCode.Unauthorized, ApiError("UNAUTHORIZED", "Kyçja është e nevojshme.")); return false }
    if (user.role !in setOf("ADMINISTRATOR", "DREJTOR")) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Nuk keni këtë qasje.")); return false }
    return true
}
