package com.ltline.eshkolla.db

import java.sql.Connection

object SchoolOperationsMigration {
    fun run(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS schedules (id VARCHAR(64) PRIMARY KEY, class_id VARCHAR(64) NOT NULL REFERENCES classes(id), teacher_id VARCHAR(64) NOT NULL REFERENCES teachers(id), subject_id VARCHAR(64) NOT NULL, weekday INT NOT NULL CHECK (weekday BETWEEN 1 AND 7), start_time VARCHAR(5) NOT NULL, end_time VARCHAR(5) NOT NULL, room VARCHAR(80), active BOOLEAN NOT NULL DEFAULT TRUE)")
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS notifications (id VARCHAR(64) PRIMARY KEY, title VARCHAR(200) NOT NULL, message TEXT NOT NULL, audience VARCHAR(40) NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, active BOOLEAN NOT NULL DEFAULT TRUE)")
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_schedules_class ON schedules(class_id)")
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_schedules_teacher ON schedules(teacher_id)")
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_notifications_created ON notifications(created_at DESC)")
        }
    }
}
