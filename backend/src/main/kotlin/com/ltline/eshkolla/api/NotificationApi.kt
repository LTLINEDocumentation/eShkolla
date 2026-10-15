package com.ltline.eshkolla.api

import com.ltline.eshkolla.auth.AuthService
import com.ltline.eshkolla.db.Database
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import java.sql.Connection
import java.util.UUID

@Serializable
data class NotificationDto(
    val id: String,
    val title: String,
    val message: String,
    val type: String,
    val audienceRole: String?,
    val targetUserId: String?,
    val entityType: String?,
    val entityId: String?,
    val createdByName: String,
    val createdAt: String,
    val isRead: Boolean
)

@Serializable
data class AnnouncementRequest(
    val title: String,
    val message: String,
    val audienceRole: String = "ALL",
    val targetUserId: String? = null
)

object NotificationService {
    fun notifyUser(userId: String, title: String, message: String, type: String = "EVENT", entityType: String? = null, entityId: String? = null, createdBy: String? = null) {
        Database.connection().use { c ->
            create(c, title, message, type, null, userId, entityType, entityId, createdBy)
        }
    }

    fun notifyTeacherById(teacherId: String, title: String, message: String, entityType: String? = null, entityId: String? = null, createdBy: String? = null) {
        val userId = Database.connection().use { c ->
            c.prepareStatement("SELECT user_id FROM teachers WHERE id=? AND active=TRUE").use { p ->
                p.setString(1, teacherId)
                p.executeQuery().use { rs -> if (rs.next()) rs.getString(1) else null }
            }
        }
        if (!userId.isNullOrBlank() && userId != createdBy) notifyUser(userId, title, message, "EVENT", entityType, entityId, createdBy)
    }

    fun notifyStudentContacts(actorUserId: String, teacherId: String, studentId: String, title: String, message: String, entityType: String, entityId: String) {
        Database.connection().use { c ->
            val recipients = linkedSetOf<String>()
            c.prepareStatement("SELECT u.id FROM parent_students ps JOIN users u ON u.id=ps.user_id WHERE ps.student_id=? AND u.active=TRUE UNION SELECT su.user_id FROM student_users su JOIN users u ON u.id=su.user_id WHERE su.student_id=? AND u.active=TRUE").use { p ->
                p.setString(1, studentId); p.setString(2, studentId)
                p.executeQuery().use { rs -> while (rs.next()) recipients.add(rs.getString(1)) }
            }
            recipients.remove(actorUserId)
            recipients.forEach { create(c, title, message, "EVENT", null, it, entityType, entityId, actorUserId) }
        }
        notifyTeacherById(teacherId, title, message, entityType, entityId, actorUserId)
    }

    internal fun create(c: Connection, title: String, message: String, type: String, audienceRole: String?, targetUserId: String?, entityType: String?, entityId: String?, createdBy: String?) {
        c.prepareStatement("INSERT INTO notifications(id,title,message,type,audience_role,target_user_id,entity_type,entity_id,created_by) VALUES(?,?,?,?,?,?,?,?,?)").use { p ->
            p.setString(1, "NT-" + UUID.randomUUID().toString().take(12).uppercase())
            p.setString(2, title.trim().take(180))
            p.setString(3, message.trim().take(4000))
            p.setString(4, type)
            p.setString(5, audienceRole)
            p.setString(6, targetUserId)
            p.setString(7, entityType)
            p.setString(8, entityId)
            p.setString(9, createdBy)
            p.executeUpdate()
        }
    }
}

fun Application.configureNotificationApi(auth: AuthService) {
    routing {
        route("/api/v1/notifications") {
            get {
                val user = notificationUser(call, auth) ?: return@get
                val rows = Database.connection().use { c ->
                    c.prepareStatement("""
                        SELECT n.id,n.title,n.message,n.type,n.audience_role,n.target_user_id,n.entity_type,n.entity_id,
                               COALESCE(u.full_name,'Sistemi'),n.created_at,
                               EXISTS(SELECT 1 FROM notification_reads nr WHERE nr.notification_id=n.id AND nr.user_id=?) AS is_read
                        FROM notifications n
                        LEFT JOIN users u ON u.id=n.created_by
                        WHERE (n.target_user_id=? OR n.audience_role='ALL' OR n.audience_role=?)
                        ORDER BY n.created_at DESC
                        LIMIT 200
                    """.trimIndent()).use { p ->
                        p.setString(1, user.id); p.setString(2, user.id); p.setString(3, user.role)
                        p.executeQuery().use { rs -> buildList {
                            while (rs.next()) add(NotificationDto(
                                rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),
                                rs.getString(5),rs.getString(6),rs.getString(7),rs.getString(8),
                                rs.getString(9),rs.getTimestamp(10).toString(),rs.getBoolean(11)
                            ))
                        } }
                    }
                }
                call.respond(rows)
            }
            post {
                val user = notificationUser(call, auth) ?: return@post
                if (user.role !in setOf("ADMINISTRATOR", "DREJTOR")) {
                    call.respond(HttpStatusCode.Forbidden, ApiError("FORBIDDEN", "Vetëm Administratori ose Drejtori mund të publikojë njoftime të përgjithshme.")); return@post
                }
                val req = call.receive<AnnouncementRequest>()
                val allowedRoles = setOf("ALL", "ADMINISTRATOR", "DREJTOR", "MESIMDHENES", "PRIND", "NXENES")
                val audience = req.audienceRole.trim().uppercase()
                if (req.title.isBlank() || req.message.isBlank() || audience !in allowedRoles) {
                    call.respond(HttpStatusCode.BadRequest, ApiError("VALIDATION_ERROR", "Titulli, përmbajtja dhe grupi i synuar janë të detyrueshëm.")); return@post
                }
                if (!req.targetUserId.isNullOrBlank()) {
                    val exists = Database.connection().use { c -> c.prepareStatement("SELECT 1 FROM users WHERE id=? AND active=TRUE").use { p -> p.setString(1, req.targetUserId); p.executeQuery().use { it.next() } } }
                    if (!exists) { call.respond(HttpStatusCode.NotFound, ApiError("USER_NOT_FOUND", "Përdoruesi i synuar nuk u gjet.")); return@post }
                    NotificationService.notifyUser(req.targetUserId, req.title, req.message, "ANNOUNCEMENT", createdBy = user.id)
                } else {
                    Database.connection().use { c -> NotificationService.create(c, req.title, req.message, "ANNOUNCEMENT", audience, null, null, null, user.id) }
                }
                call.respond(HttpStatusCode.Created, mapOf("status" to "CREATED"))
            }
            post("/read-all") {
                val user = notificationUser(call, auth) ?: return@post
                Database.connection().use { c ->
                    c.prepareStatement("""
                        INSERT INTO notification_reads(notification_id,user_id)
                        SELECT n.id,? FROM notifications n
                        WHERE (n.target_user_id=? OR n.audience_role='ALL' OR n.audience_role=?)
                        ON CONFLICT(notification_id,user_id) DO NOTHING
                    """.trimIndent()).use { p -> p.setString(1,user.id);p.setString(2,user.id);p.setString(3,user.role);p.executeUpdate() }
                }
                call.respond(mapOf("status" to "OK"))
            }
            post("/{id}/read") {
                val user = notificationUser(call, auth) ?: return@post
                val id = call.parameters["id"].orEmpty()
                val visible = Database.connection().use { c -> c.prepareStatement("SELECT 1 FROM notifications WHERE id=? AND (target_user_id=? OR audience_role='ALL' OR audience_role=?)").use { p -> p.setString(1,id);p.setString(2,user.id);p.setString(3,user.role);p.executeQuery().use { it.next() } } }
                if (!visible) { call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "Njoftimi nuk u gjet.")); return@post }
                Database.connection().use { c -> c.prepareStatement("INSERT INTO notification_reads(notification_id,user_id) VALUES(?,?) ON CONFLICT(notification_id,user_id) DO NOTHING").use { p -> p.setString(1,id);p.setString(2,user.id);p.executeUpdate() } }
                call.respond(mapOf("status" to "OK"))
            }
        }
    }
}

private suspend fun notificationUser(call: io.ktor.server.application.ApplicationCall, auth: AuthService): UserDto? {
    val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")?.takeIf { it.isNotBlank() }
    val user = token?.let(auth::userFor)
    if (user == null) call.respond(HttpStatusCode.Unauthorized, ApiError("UNAUTHORIZED", "Kyçja është e nevojshme."))
    return user
}
