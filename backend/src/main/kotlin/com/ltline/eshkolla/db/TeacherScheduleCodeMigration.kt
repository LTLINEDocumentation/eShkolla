package com.ltline.eshkolla.db

import java.sql.Connection

/**
 * Regjistri real i kodeve 1–17 që përdoren në tabelën e orarit.
 * Kodi mbetet i qëndrueshëm edhe kur ndryshon mësimdhënësi.
 */
object TeacherScheduleCodeMigration {
    fun run(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS teacher_schedule_codes (" +
                    "code INT PRIMARY KEY CHECK (code BETWEEN 1 AND 17)," +
                    "teacher_id VARCHAR(64) REFERENCES teachers(id)," +
                    "display_name VARCHAR(200)," +
                    "subject_id VARCHAR(64) REFERENCES subjects(id)," +
                    "active BOOLEAN NOT NULL DEFAULT TRUE"
                    + ")"
            )
            statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS teacher_schedule_code_classes (" +
                    "code INT NOT NULL REFERENCES teacher_schedule_codes(code) ON DELETE CASCADE," +
                    "class_id VARCHAR(64) NOT NULL REFERENCES classes(id) ON DELETE CASCADE," +
                    "PRIMARY KEY (code,class_id)" +
                    ")"
            )
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_teacher_schedule_code_classes_class ON teacher_schedule_code_classes(class_id)")
        }

        val defaults = listOf(
            Triple(1, "Shpresa Mecini", "Gjuhë shqipe"),
            Triple(2, "Hajriz Tahiri", "Gjuhë shqipe"),
            Triple(3, "Gentiana Beqiri", "Gjuhë angleze"),
            Triple(4, "Adhurim Fazliu", "Kimi"),
            Triple(5, "Fakete Avdiu", "TIK"),
            Triple(6, "Leonard Tahiraj", "Matematikë"),
            Triple(7, "Nexhmie Mecini", "Matematikë"),
            Triple(8, "Sadri Hylenaj", "Histori"),
            Triple(9, "Brahim Hamza", "Gjeografi"),
            Triple(10, "Rasim Hasani", "Fizikë"),
            Triple(11, "Besa Behrami", "Edukatë figurative"),
            Triple(12, "Albulena Veseli", "Biologji"),
            Triple(13, "Ylli Kadriu", "Edukatë fizike"),
            Triple(14, null, null),
            Triple(15, "Bleona Bajraktari", "Gjuhë gjermane"),
            Triple(16, "Selman Thaqi", "Edukatë muzikore"),
            Triple(17, "Xhavit Tahiri", null)
        )

        defaults.forEach { (code, name, subject) ->
            connection.prepareStatement(
                "INSERT INTO teacher_schedule_codes(code,teacher_id,display_name,subject_id,active) " +
                    "SELECT ?,t.id,?,s.id,TRUE FROM (SELECT 1) seed " +
                    "LEFT JOIN teachers t ON lower(trim(t.full_name))=lower(trim(?)) AND t.active=TRUE " +
                    "LEFT JOIN subjects s ON lower(trim(s.name))=lower(trim(?)) AND s.active=TRUE " +
                    "WHERE NOT EXISTS (SELECT 1 FROM teacher_schedule_codes WHERE code=?)"
            ).use { ps ->
                ps.setInt(1, code)
                ps.setString(2, name)
                ps.setString(3, name)
                ps.setString(4, subject)
                ps.setInt(5, code)
                ps.executeUpdate()
            }
        }
    }
}
