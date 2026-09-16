package com.ltline.eshkolla.db

import com.ltline.eshkolla.auth.PasswordHasher
import java.sql.Connection
import java.text.Normalizer
import java.util.UUID

/**
 * Plotëson vetëm pjesët që mungojnë për mësimdhënësit ekzistues.
 * Nuk ndryshon llogaritë që ekzistojnë dhe nuk ruan fjalëkalimin fillestar në tekst të thjeshtë.
 *
 * Standardi fillestar:
 * username = emri.mbiemri
 * password = iniciali i emrit + iniciali i mbiemrit + 12345678
 */
object TeacherAccountProvisioningMigration {
    private const val DEFAULT_SUFFIX = "12345678"

    fun run(connection: Connection) {
        connection.autoCommit = false
        try {
            connection.prepareStatement(
                "SELECT id,user_id,full_name FROM teachers WHERE active=TRUE ORDER BY full_name"
            ).use { ps ->
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        val teacherId = rs.getString("id")
                        val existingUserId = rs.getString("user_id")
                        val fullName = rs.getString("full_name").trim()
                        val userId = existingUserId ?: provisionUser(connection, teacherId, fullName)
                        if (userId != null) {
                            ensureTeacherSubjectRelations(connection, teacherId)
                        }
                    }
                }
            }

            // teacher_classes është burimi i klasave të caktuara; teacher_subjects duhet
            // të jetë lidhja e plotë mësimdhënës + lëndë + paralele.
            connection.prepareStatement(
                "INSERT INTO teacher_classes(teacher_id,class_id) " +
                    "SELECT teacher_id,class_id FROM teacher_subjects " +
                    "ON CONFLICT (teacher_id,class_id) DO NOTHING"
            ).use { it.executeUpdate() }

            connection.commit()
        } catch (e: Exception) {
            connection.rollback()
            throw e
        } finally {
            connection.autoCommit = true
        }
    }

    private fun provisionUser(connection: Connection, teacherId: String, fullName: String): String? {
        val parts = fullName.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (parts.size < 2) return null

        val first = parts.first()
        val last = parts.last()
        val usernameBase = "${normalize(first)}.${normalize(last)}".lowercase()
        if (usernameBase.length < 3) return null

        val username = uniqueUsername(connection, usernameBase)
        // Mos krijojmë llogari të dyta për emra që përplasen me një user ekzistues.
        if (username != usernameBase) return null

        val password = "${first.first().uppercaseChar()}${last.first().uppercaseChar()}$DEFAULT_SUFFIX"
        val userId = UUID.randomUUID().toString()
        val hash = PasswordHasher.create(password)

        connection.prepareStatement(
            "INSERT INTO users(id,username,full_name,role,password_hash,active) VALUES(?,?,?,?,?,TRUE)"
        ).use { ps ->
            ps.setString(1, userId)
            ps.setString(2, username)
            ps.setString(3, fullName)
            ps.setString(4, "MESIMDHENES")
            ps.setString(5, hash)
            ps.executeUpdate()
        }

        connection.prepareStatement("UPDATE teachers SET user_id=? WHERE id=? AND user_id IS NULL").use { ps ->
            ps.setString(1, userId)
            ps.setString(2, teacherId)
            ps.executeUpdate()
        }
        return userId
    }

    private fun uniqueUsername(connection: Connection, base: String): String {
        connection.prepareStatement("SELECT id FROM users WHERE username=? LIMIT 1").use { ps ->
            ps.setString(1, base)
            ps.executeQuery().use { rs ->
                if (!rs.next()) return base
            }
        }
        return base
    }

    private fun ensureTeacherSubjectRelations(connection: Connection, teacherId: String) {
        connection.prepareStatement(
            "INSERT INTO teacher_subjects(teacher_id,subject_id,class_id) " +
                "SELECT t.id,t.subject_id,tc.class_id FROM teachers t " +
                "JOIN teacher_classes tc ON tc.teacher_id=t.id " +
                "WHERE t.id=? AND t.subject_id IS NOT NULL " +
                "ON CONFLICT (teacher_id,subject_id,class_id) DO NOTHING"
        ).use { ps ->
            ps.setString(1, teacherId)
            ps.executeUpdate()
        }
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^A-Za-z0-9]"), "")
}
