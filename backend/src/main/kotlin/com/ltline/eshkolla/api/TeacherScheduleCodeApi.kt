package com.ltline.eshkolla.api

import com.ltline.eshkolla.auth.AuthService
import com.ltline.eshkolla.db.Database
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
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
data class TeacherScheduleCodeDto(
    val code: Int,
    val teacherId: String? = null,
    val displayName: String? = null,
    val subjectId: String? = null,
    val subjectName: String? = null,
    val classIds: List<String> = emptyList(),
    val classNames: List<String> = emptyList(),
    val active: Boolean = true
)

@Serializable
data class TeacherScheduleCodeRequest(
    val code: Int,
    val teacherId: String? = null,
    val displayName: String? = null,
    val subjectId: String? = null,
    val classIds: List<String> = emptyList(),
    val active: Boolean = true
)

fun Application.configureTeacherScheduleCodeApi(authService: AuthService) {
    routing {
        route("/api/v1/management/timetable-codes") {
            get {
                if (requireManager(call, authService) == null) return@get
                call.respond(loadCodes())
            }
            post {
                if (requireManager(call, authService) == null) return@post
                val request = call.receive<TeacherScheduleCodeRequest>()
                val error = validate(request)
                if (error != null) {
                    call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR", error))
                    return@post
                }
                call.respond(HttpStatusCode.Created, saveCode(request, true))
            }
            put("/{code}") {
                if (requireManager(call, authService) == null) return@put
                val code = call.parameters["code"]?.toIntOrNull()
                if (code == null || code !in 1..17) {
                    call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR", "Kodi duhet të jetë nga 1 deri në 17."))
                    return@put
                }
                val request = call.receive<TeacherScheduleCodeRequest>().copy(code = code)
                val error = validate(request)
                if (error != null) {
                    call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR", error))
                    return@put
                }
                if (!codeExists(code)) {
                    call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Kodi nuk ekziston."))
                    return@put
                }
                call.respond(saveCode(request, false))
            }
            delete("/{code}") {
                if (requireManager(call, authService) == null) return@delete
                val code = call.parameters["code"]?.toIntOrNull()
                if (code == null || code !in 1..17) {
                    call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR", "Kodi duhet të jetë nga 1 deri në 17."))
                    return@delete
                }
                val result = Database.connection().use { connection ->
                    connection.autoCommit = false
                    try {
                        connection.prepareStatement("DELETE FROM teacher_schedule_code_classes WHERE code=?").use { statement ->
                            statement.setInt(1, code)
                            statement.executeUpdate()
                        }
                        val updated = connection.prepareStatement(
                            "UPDATE teacher_schedule_codes SET teacher_id=NULL,display_name=NULL,subject_id=NULL,active=TRUE WHERE code=?"
                        ).use { statement ->
                            statement.setInt(1, code)
                            statement.executeUpdate()
                        }
                        connection.commit()
                        updated
                    } catch (exception: Exception) {
                        connection.rollback()
                        throw exception
                    }
                }
                if (result == 0) {
                    call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Kodi nuk ekziston."))
                } else {
                    call.respond(loadCodes().first { it.code == code })
                }
            }
        }
    }
}

private suspend fun requireManager(
    call: io.ktor.server.application.ApplicationCall,
    auth: AuthService
): UserDto? {
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
    if (user.role !in setOf("ADMINISTRATOR", "DREJTOR")) {
        call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Vetëm administratori ose drejtori mund të menaxhojë kodet e orarit."))
        return null
    }
    return user
}

private fun validate(request: TeacherScheduleCodeRequest): String? {
    if (request.code !in 1..17) return "Kodi duhet të jetë nga 1 deri në 17."
    if (request.displayName.orEmpty().length > 200) return "Emri i mësimdhënësit është shumë i gjatë."
    if (request.classIds.distinct().size != request.classIds.size) return "Një klasë/paralele është përsëritur."
    return null
}

private fun codeExists(code: Int): Boolean {
    return Database.connection().use { connection ->
        connection.prepareStatement("SELECT 1 FROM teacher_schedule_codes WHERE code=?").use { statement ->
            statement.setInt(1, code)
            statement.executeQuery().use { resultSet -> resultSet.next() }
        }
    }
}

private fun saveCode(request: TeacherScheduleCodeRequest, allowCreate: Boolean): TeacherScheduleCodeDto {
    Database.connection().use { connection ->
        connection.autoCommit = false
        try {
            if (!allowCreate && !codeExistsIn(connection, request.code)) {
                error("Kodi nuk ekziston.")
            }
            connection.prepareStatement(
                "INSERT INTO teacher_schedule_codes(code,teacher_id,display_name,subject_id,active) VALUES(?,?,?,?,?) " +
                    "ON CONFLICT(code) DO UPDATE SET teacher_id=EXCLUDED.teacher_id,display_name=EXCLUDED.display_name,subject_id=EXCLUDED.subject_id,active=EXCLUDED.active"
            ).use { statement ->
                statement.setInt(1, request.code)
                statement.setString(2, request.teacherId?.trim()?.takeIf { it.isNotBlank() })
                statement.setString(3, request.displayName?.trim()?.takeIf { it.isNotBlank() })
                statement.setString(4, request.subjectId?.trim()?.takeIf { it.isNotBlank() })
                statement.setBoolean(5, request.active)
                statement.executeUpdate()
            }
            connection.prepareStatement("DELETE FROM teacher_schedule_code_classes WHERE code=?").use { statement ->
                statement.setInt(1, request.code)
                statement.executeUpdate()
            }
            request.classIds.map { it.trim() }.filter { it.isNotBlank() }.distinct().forEach { classId ->
                connection.prepareStatement(
                    "INSERT INTO teacher_schedule_code_classes(code,class_id) VALUES(?,?) ON CONFLICT DO NOTHING"
                ).use { statement ->
                    statement.setInt(1, request.code)
                    statement.setString(2, classId)
                    statement.executeUpdate()
                }
            }
            connection.commit()
        } catch (exception: Exception) {
            connection.rollback()
            throw exception
        }
    }
    return loadCodes().first { it.code == request.code }
}

private fun codeExistsIn(connection: java.sql.Connection, code: Int): Boolean {
    return connection.prepareStatement("SELECT 1 FROM teacher_schedule_codes WHERE code=?").use { statement ->
        statement.setInt(1, code)
        statement.executeQuery().use { resultSet -> resultSet.next() }
    }
}

private fun loadCodes(): List<TeacherScheduleCodeDto> {
    return Database.connection().use { connection ->
        val rows = connection.prepareStatement(
            "SELECT t.code,t.teacher_id,t.display_name,t.subject_id,s.name AS subject_name,t.active " +
                "FROM teacher_schedule_codes t LEFT JOIN subjects s ON s.id=t.subject_id ORDER BY t.code"
        ).use { statement ->
            statement.executeQuery().use { resultSet ->
                buildList {
                    while (resultSet.next()) {
                        add(
                            TeacherScheduleCodeDto(
                                code = resultSet.getInt("code"),
                                teacherId = resultSet.getString("teacher_id"),
                                displayName = resultSet.getString("display_name"),
                                subjectId = resultSet.getString("subject_id"),
                                subjectName = resultSet.getString("subject_name"),
                                active = resultSet.getBoolean("active")
                            )
                        )
                    }
                }
            }
        }

        rows.map { row ->
            val classes = connection.prepareStatement(
                "SELECT tc.class_id,c.name FROM teacher_schedule_code_classes tc " +
                    "JOIN classes c ON c.id=tc.class_id WHERE tc.code=? ORDER BY c.name"
            ).use { statement ->
                statement.setInt(1, row.code)
                statement.executeQuery().use { resultSet ->
                    buildList {
                        while (resultSet.next()) {
                            add(resultSet.getString(1) to resultSet.getString(2))
                        }
                    }
                }
            }
            row.copy(
                classIds = classes.map { it.first },
                classNames = classes.map { it.second }
            )
        }
    }
}
