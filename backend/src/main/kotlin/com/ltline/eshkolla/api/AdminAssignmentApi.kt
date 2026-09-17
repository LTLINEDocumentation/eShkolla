package com.ltline.eshkolla.api

import com.ltline.eshkolla.auth.AuthService
import com.ltline.eshkolla.db.Database
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import java.sql.SQLException

@Serializable
data class TeacherSubjectAssignmentCreateRequest(
    val teacherId: String,
    val subjectId: String,
    val classId: String
)

@Serializable
data class TeacherSubjectAssignmentUpdateRequest(
    val oldTeacherId: String,
    val oldSubjectId: String,
    val oldClassId: String,
    val teacherId: String,
    val subjectId: String,
    val classId: String
)

fun Application.configureAdminAssignmentApi(authService: AuthService) {
    routing {
        post("/api/v1/management/teacher-subject-assignments") {
            if (!requireAssignmentManager(call, authService)) return@post
            val req = call.receive<TeacherSubjectAssignmentCreateRequest>()
            if (req.teacherId.isBlank() || req.subjectId.isBlank() || req.classId.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR", "Mësimdhënësi, lënda, klasa dhe paralelja janë të detyrueshme."))
                return@post
            }
            if (!exists("teachers", req.teacherId) || !exists("subjects", req.subjectId) || !exists("classes", req.classId)) {
                call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR", "Mësimdhënësi, lënda ose klasa/paralelja nuk ekziston."))
                return@post
            }
            try {
                Database.connection().use { c ->
                    c.autoCommit = false
                    try {
                        c.prepareStatement("INSERT INTO teacher_subjects(teacher_id,subject_id,class_id) VALUES (?,?,?)").use { ps ->
                            ps.setString(1, req.teacherId); ps.setString(2, req.subjectId); ps.setString(3, req.classId); ps.executeUpdate()
                        }
                        c.prepareStatement("INSERT INTO teacher_classes(teacher_id,class_id) VALUES (?,?) ON CONFLICT DO NOTHING").use { ps ->
                            ps.setString(1, req.teacherId); ps.setString(2, req.classId); ps.executeUpdate()
                        }
                        c.commit()
                    } catch (e: SQLException) {
                        c.rollback()
                        throw e
                    } finally {
                        c.autoCommit = true
                    }
                }
                call.respond(HttpStatusCode.Created, req)
            } catch (_: SQLException) {
                call.respond(HttpStatusCode.Conflict, ApiError("ALREADY_EXISTS", "Ky caktim ekziston tashmë ose të dhënat nuk janë valide."))
            }
        }

        put("/api/v1/management/teacher-subject-assignments") {
            if (!requireAssignmentManager(call, authService)) return@put
            val req = call.receive<TeacherSubjectAssignmentUpdateRequest>()
            if (!exists("teachers", req.teacherId) || !exists("subjects", req.subjectId) || !exists("classes", req.classId)) {
                call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR", "Mësimdhënësi, lënda ose klasa e re nuk ekziston."))
                return@put
            }
            val changed = Database.connection().use { c ->
                c.autoCommit = false
                try {
                    c.prepareStatement("DELETE FROM teacher_subjects WHERE teacher_id=? AND subject_id=? AND class_id=?").use { ps ->
                        ps.setString(1, req.oldTeacherId); ps.setString(2, req.oldSubjectId); ps.setString(3, req.oldClassId)
                        if (ps.executeUpdate() == 0) {
                            c.rollback()
                            false
                        } else {
                            c.prepareStatement("INSERT INTO teacher_subjects(teacher_id,subject_id,class_id) VALUES (?,?,?)").use { ps ->
                                ps.setString(1, req.teacherId); ps.setString(2, req.subjectId); ps.setString(3, req.classId)
                                ps.executeUpdate()
                            }
                            c.prepareStatement("INSERT INTO teacher_classes(teacher_id,class_id) VALUES (?,?) ON CONFLICT DO NOTHING").use { ps ->
                                ps.setString(1, req.teacherId); ps.setString(2, req.classId); ps.executeUpdate()
                            }
                            c.prepareStatement("DELETE FROM teacher_classes tc WHERE tc.teacher_id=? AND tc.class_id=? AND NOT EXISTS (SELECT 1 FROM teacher_subjects ts WHERE ts.teacher_id=tc.teacher_id AND ts.class_id=tc.class_id)").use { ps ->
                                ps.setString(1, req.oldTeacherId); ps.setString(2, req.oldClassId); ps.executeUpdate()
                            }
                            c.commit()
                            true
                        }
                    }
                } catch (e: SQLException) {
                    c.rollback()
                    throw e
                } finally {
                    c.autoCommit = true
                }
            }
            if (!changed) call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Caktimi që po ndryshohet nuk ekziston më."))
            else call.respond(mapOf("success" to true))
        }

        delete("/api/v1/management/teacher-subject-assignments") {
            if (!requireAssignmentManager(call, authService)) return@delete
            val teacherId = call.request.queryParameters["teacherId"]
            val subjectId = call.request.queryParameters["subjectId"]
            val classId = call.request.queryParameters["classId"]
            if (teacherId.isNullOrBlank() || subjectId.isNullOrBlank() || classId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR", "Caktimi nuk është specifikuar plotësisht."))
                return@delete
            }
            val deleted = Database.connection().use { c ->
                c.autoCommit = false
                try {
                    val count = c.prepareStatement("DELETE FROM teacher_subjects WHERE teacher_id=? AND subject_id=? AND class_id=?").use { ps ->
                        ps.setString(1, teacherId); ps.setString(2, subjectId); ps.setString(3, classId); ps.executeUpdate()
                    }
                    c.prepareStatement("DELETE FROM teacher_classes tc WHERE tc.teacher_id=? AND tc.class_id=? AND NOT EXISTS (SELECT 1 FROM teacher_subjects ts WHERE ts.teacher_id=tc.teacher_id AND ts.class_id=tc.class_id)").use { ps ->
                        ps.setString(1, teacherId); ps.setString(2, classId); ps.executeUpdate()
                    }
                    c.commit()
                    count > 0
                } catch (e: SQLException) {
                    c.rollback()
                    throw e
                } finally {
                    c.autoCommit = true
                }
            }
            if (!deleted) call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Caktimi nuk ekziston më."))
            else call.respond(mapOf("success" to true))
        }
    }
}

private suspend fun requireAssignmentManager(call: io.ktor.server.application.ApplicationCall, auth: AuthService): Boolean {
    val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")?.takeIf { it.isNotBlank() }
    val user = token?.let(auth::userFor)
    if (user == null) { call.respond(HttpStatusCode.Unauthorized, ApiError("UNAUTHORIZED", "Kyçja është e nevojshme.")); return false }
    if (user.role !in setOf("ADMINISTRATOR", "DREJTOR")) { call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Vetëm administratori ose drejtori mund të menaxhojë caktimet.")); return false }
    return true
}

private fun exists(table: String, id: String): Boolean {
    val safe = setOf("classes", "teachers", "subjects").firstOrNull { it == table } ?: return false
    return Database.connection().use { c -> c.prepareStatement("SELECT 1 FROM $safe WHERE id=? AND active=TRUE").use { ps -> ps.setString(1,id); ps.executeQuery().use { it.next() } } }
}
