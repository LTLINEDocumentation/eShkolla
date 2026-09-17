package com.ltline.eshkolla.db

import java.sql.Connection
import java.util.UUID

/**
 * Burimi i mësimdhënësve është users me role MESIMDHENES.
 * Për çdo përdorues aktiv që ende nuk ka rresht në teachers,
 * krijohet vetëm regjistri teknik i mësimdhënësit. Lënda caktohet më vonë te Caktimet.
 */
object UserTeacherProvisioningMigration {
    fun run(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.executeUpdate(
                """
                INSERT INTO teachers(id,user_id,full_name,subject_id,active)
                SELECT 'M-' || UPPER(REPLACE(SUBSTRING(CAST(u.id AS VARCHAR),1,8),'-','')), u.id, u.full_name, NULL, TRUE
                FROM users u
                WHERE u.role='MESIMDHENES'
                  AND u.active=TRUE
                  AND NOT EXISTS (SELECT 1 FROM teachers t WHERE t.user_id=u.id)
                """.trimIndent()
            )
        }
    }
}
