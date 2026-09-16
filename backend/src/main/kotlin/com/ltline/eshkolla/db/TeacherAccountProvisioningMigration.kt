package com.ltline.eshkolla.db

import com.ltline.eshkolla.auth.PasswordHasher
import java.sql.Connection
import java.text.Normalizer
import java.util.UUID

/**
 * Krijon vetëm User-at që mungojnë për mësimdhënësit ekzistues.
 * Nuk ndryshon User-at, lëndët, klasat ose lidhjet ekzistuese.
 * Fjalëkalimi fillestar ruhet vetëm si hash.
 *
 * Rregulli i dakorduar:
 * username = emri.mbiemri
 * password fillestar = iniciali i emrit + iniciali i mbiemrit + 12345678
 * p.sh. Leonard Tahiraj -> leonard.tahiraj / LT12345678
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
                        if (existingUserId == null) {
                            findOrProvisionUser(connection, teacherId, rs.getString("full_name").trim())
                        }
                    }
                }
            }
            connection.commit()
        } catch (e: Exception) {
            connection.rollback()
            throw e
        } finally {
            connection.autoCommit = true
        }
    }

    private fun findOrProvisionUser(connection: Connection, teacherId: String, fullName: String): String? {
        val parts = fullName.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (parts.size < 2) return null

        val first = parts.first()
        val last = parts.last()
        val username = "${normalize(first)}.${normalize(last)}".lowercase()

        // Nëse User-i ekziston, vetëm lidhe me mësimdhënësin. Mos krijo dublikatë.
        connection.prepareStatement(
            "SELECT id FROM users WHERE lower(username)=lower(?) LIMIT 1"
        ).use { ps ->
            ps.setString(1, username)
            ps.executeQuery().use { rs ->
                if (rs.next()) {
                    val existingUserId = rs.getString("id")
                    connection.prepareStatement(
                        "UPDATE teachers SET user_id=? WHERE id=? AND user_id IS NULL"
                    ).use { update ->
                        update.setString(1, existingUserId)
                        update.setString(2, teacherId)
                        update.executeUpdate()
                    }
                    return existingUserId
                }
            }
        }

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

        connection.prepareStatement(
            "UPDATE teachers SET user_id=? WHERE id=? AND user_id IS NULL"
        ).use { ps ->
            ps.setString(1, userId)
            ps.setString(2, teacherId)
            ps.executeUpdate()
        }

        return userId
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^A-Za-z0-9]"), "")
}
