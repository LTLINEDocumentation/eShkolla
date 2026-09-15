package com.ltline.eshkolla.api

import com.ltline.eshkolla.auth.AuthService
import com.ltline.eshkolla.db.Database
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable

@Serializable
data class RoleDataClass(val id: String, val name: String, val gradeLevel: Int, val studentCount: Int)

@Serializable
data class RoleDataSubject(val id: String, val name: String, val code: String?, val classIds: List<String>)

@Serializable
data class RoleDataStudent(val id: String, val fullName: String, val classId: String, val className: String, val birthDate: String, val active: Boolean)

@Serializable
data class RoleDataGrade(val id: String, val studentId: String, val studentName: String, val subjectId: String, val subjectName: String, val teacherId: String, val value: Int, val period: String, val academicYear: String, val note: String?)

@Serializable
data class RoleDataAbsence(val id: String, val studentId: String, val studentName: String, val subjectId: String, val subjectName: String, val teacherId: String, val date: String, val status: String, val note: String?)

@Serializable
data class TeacherRoleData(val teacherId: String, val classes: List<RoleDataClass>, val subjects: List<RoleDataSubject>, val students: List<RoleDataStudent>, val grades: List<RoleDataGrade>, val absences: List<RoleDataAbsence>)

fun Application.configureRoleDataApi(authService: AuthService) {
    routing {
        route("/api/v1") {
            get("/me/data") {
                val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")?.takeIf { it.isNotBlank() }
                val user = token?.let(authService::userFor)
                if (user == null) { call.respond(HttpStatusCode.Unauthorized, ApiError("UNAUTHORIZED", "Kyçja është e nevojshme.")); return@get }
                if (user.role != "MESIMDHENES") { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Ky endpoint është për mësimdhënësin.")); return@get }

                val data = Database.connection().use { c ->
                    val teacherId = c.prepareStatement("SELECT id FROM teachers WHERE user_id=? AND active=TRUE LIMIT 1").use { ps ->
                        ps.setString(1, user.id)
                        ps.executeQuery().use { rs -> if (rs.next()) rs.getString(1) else null }
                    }
                    if (teacherId == null) null else {
                        val classes = c.prepareStatement("SELECT c.id,c.name,c.grade_level,(SELECT COUNT(*) FROM students s WHERE s.class_id=c.id AND s.active=TRUE) FROM classes c JOIN teacher_classes tc ON tc.class_id=c.id WHERE tc.teacher_id=? AND c.active=TRUE ORDER BY c.grade_level,c.name").use { ps ->
                            ps.setString(1, teacherId)
                            ps.executeQuery().use { rs -> buildList { while (rs.next()) add(RoleDataClass(rs.getString(1),rs.getString(2),rs.getInt(3),rs.getInt(4))) } }
                        }
                        val subjects = c.prepareStatement("SELECT s.id,s.name,s.code,ts.class_id FROM subjects s JOIN teacher_subjects ts ON ts.subject_id=s.id WHERE ts.teacher_id=? AND s.active=TRUE ORDER BY s.name,ts.class_id").use { ps ->
                            ps.setString(1, teacherId)
                            ps.executeQuery().use { rs ->
                                val map = linkedMapOf<String, MutableList<String>>(); val names = linkedMapOf<String,Pair<String,String?>>()
                                while (rs.next()) { val id=rs.getString(1); names[id]=rs.getString(2) to rs.getString(3); map.getOrPut(id){mutableListOf()}.add(rs.getString(4)) }
                                names.entries.map { (id,n) -> RoleDataSubject(id,n.first,n.second,map[id]?.distinct().orEmpty()) }
                            }
                        }
                        val classIds = classes.map { it.id }.toSet()
                        val students = if (classIds.isEmpty()) emptyList() else c.prepareStatement("SELECT s.id,s.full_name,s.class_id,c.name,s.birth_date,s.active FROM students s JOIN classes c ON c.id=s.class_id WHERE s.class_id = ANY(?) ORDER BY c.grade_level,c.name,s.full_name").use { ps ->
                            ps.setArray(1,c.createArrayOf("varchar",classIds.toTypedArray()))
                            ps.executeQuery().use { rs -> buildList { while(rs.next()) add(RoleDataStudent(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getBoolean(6))) } }
                        }
                        val grades = c.prepareStatement("SELECT g.id,g.student_id,s.full_name,g.subject_id,COALESCE(sub.name,g.subject_id),g.teacher_id,g.value,g.period,g.academic_year,g.note FROM grades g JOIN students s ON s.id=g.student_id LEFT JOIN subjects sub ON sub.id=g.subject_id WHERE g.teacher_id=? ORDER BY g.academic_year DESC,g.period,s.full_name").use { ps ->
                            ps.setString(1,teacherId); ps.executeQuery().use { rs -> buildList { while(rs.next()) add(RoleDataGrade(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getInt(7),rs.getString(8),rs.getString(9),rs.getString(10))) } }
                        }
                        val absences = c.prepareStatement("SELECT a.id,a.student_id,s.full_name,a.subject_id,COALESCE(sub.name,a.subject_id),a.teacher_id,a.date,a.status,a.note FROM absences a JOIN students s ON s.id=a.student_id LEFT JOIN subjects sub ON sub.id=a.subject_id WHERE a.teacher_id=? ORDER BY a.date DESC,s.full_name").use { ps ->
                            ps.setString(1,teacherId); ps.executeQuery().use { rs -> buildList { while(rs.next()) add(RoleDataAbsence(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getString(7),rs.getString(8),rs.getString(9))) } }
                        }
                        TeacherRoleData(teacherId,classes,subjects,students,grades,absences)
                    }
                }
                if (data == null) call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Profili i mësimdhënësit nuk është i lidhur me përdoruesin.")) else call.respond(data)
            }
        }
    }
}
