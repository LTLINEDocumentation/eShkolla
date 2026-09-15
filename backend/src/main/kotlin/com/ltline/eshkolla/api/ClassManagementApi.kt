package com.ltline.eshkolla.api

import com.ltline.eshkolla.auth.AuthService
import com.ltline.eshkolla.db.Database
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import java.sql.SQLException

@Serializable
data class ClassUpdateRequest(val name: String, val gradeLevel: Int)

fun Application.configureClassManagementApi(authService: AuthService) {
    routing {
        route("/api/v1/management/classes") {
            put("/{id}") {
                if (!requireClassAdmin(call, authService)) return@put
                val id = call.parameters["id"].orEmpty().trim()
                val req = call.receive<ClassUpdateRequest>()
                val name = req.name.trim()
                if (id.isBlank() || name.isBlank() || req.gradeLevel !in 1..13) {
                    call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR", "Emri i klasës dhe klasa/niveli 1-13 janë të detyrueshme."))
                    return@put
                }
                try {
                    val updated = Database.connection().use { c ->
                        c.prepareStatement("UPDATE classes SET name=?, grade_level=? WHERE id=?").use { ps ->
                            ps.setString(1, name)
                            ps.setInt(2, req.gradeLevel)
                            ps.setString(3, id)
                            ps.executeUpdate()
                        }
                    }
                    if (updated == 0) call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Klasa nuk u gjet."))
                    else call.respond(mapOf("id" to id, "name" to name, "gradeLevel" to req.gradeLevel))
                } catch (_: SQLException) {
                    call.respond(HttpStatusCode.Conflict, ApiError("CLASS_UPDATE_FAILED", "Klasa nuk mund të rregullohet."))
                }
            }

            delete("/{id}") {
                if (!requireClassAdmin(call, authService)) return@delete
                val id = call.parameters["id"].orEmpty().trim()
                if (id.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR", "ID e klasës mungon."))
                    return@delete
                }
                try {
                    Database.connection().use { c ->
                        val students = count(c, "SELECT COUNT(*) FROM students WHERE class_id=?", id)
                        val teacherLinks = count(c, "SELECT COUNT(*) FROM teacher_classes WHERE class_id=?", id)
                        val subjectLinks = count(c, "SELECT COUNT(*) FROM teacher_subjects WHERE class_id=?", id)
                        val schedules = count(c, "SELECT COUNT(*) FROM schedules WHERE class_id=?", id)
                        if (students > 0 || teacherLinks > 0 || subjectLinks > 0 || schedules > 0) {
                            call.respond(HttpStatusCode.Conflict, ApiError("CLASS_IN_USE", "Klasa nuk mund të fshihet sepse ka nxënës, mësimdhënës, caktime ose orar të lidhur me të."))
                            return@use
                        }
                        c.prepareStatement("DELETE FROM classes WHERE id=?").use { ps ->
                            ps.setString(1, id)
                            val deleted = ps.executeUpdate()
                            if (deleted == 0) call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Klasa nuk u gjet."))
                            else call.respond(HttpStatusCode.NoContent)
                        }
                    }
                } catch (_: SQLException) {
                    call.respond(HttpStatusCode.Conflict, ApiError("CLASS_DELETE_FAILED", "Klasa nuk mund të fshihet. Kontrolloni lidhjet e saj me të dhënat e shkollës."))
                }
            }
        }
    }
}

private suspend fun requireClassAdmin(call: io.ktor.server.application.ApplicationCall, auth: AuthService): Boolean {
    val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")?.takeIf { it.isNotBlank() }
    val user = token?.let(auth::userFor)
    if (user == null) {
        call.respond(HttpStatusCode.Unauthorized, ApiError("UNAUTHORIZED", "Kyçja është e nevojshme."))
        return false
    }
    if (user.role != "ADMINISTRATOR") {
        call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Vetëm administratori mund të menaxhojë klasat."))
        return false
    }
    return true
}

private fun count(connection: java.sql.Connection, sql: String, id: String): Int =
    connection.prepareStatement(sql).use { ps ->
        ps.setString(1, id)
        ps.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else 0 }
    }
