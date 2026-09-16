package com.ltline.eshkolla.db

import java.sql.Connection

object TimetableMigration {
    fun run(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS school_timetable_settings (" +
                    "id VARCHAR(64) PRIMARY KEY," +
                    "school_id VARCHAR(64) REFERENCES schools(id)," +
                    "first_lesson_start VARCHAR(5) NOT NULL DEFAULT '08:00'," +
                    "lesson_duration_minutes INT NOT NULL DEFAULT 45," +
                    "short_break_minutes INT NOT NULL DEFAULT 5," +
                    "long_break_after_lesson INT NOT NULL DEFAULT 4," +
                    "long_break_minutes INT NOT NULL DEFAULT 30," +
                    "lessons_per_day INT NOT NULL DEFAULT 6," +
                    "active BOOLEAN NOT NULL DEFAULT TRUE"
                    + ")"
            )
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_timetable_settings_school ON school_timetable_settings(school_id)")
            statement.executeUpdate(
                "INSERT INTO school_timetable_settings(id,school_id,first_lesson_start,lesson_duration_minutes,short_break_minutes,long_break_after_lesson,long_break_minutes,lessons_per_day) " +
                    "SELECT 'DEFAULT',NULL,'08:00',45,5,4,30,6 WHERE NOT EXISTS (SELECT 1 FROM school_timetable_settings WHERE id='DEFAULT')"
            )
        }
    }
}
