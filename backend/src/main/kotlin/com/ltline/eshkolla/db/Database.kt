package com.ltline.eshkolla.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection

object Database {
    private val dataSource: HikariDataSource by lazy {
        HikariConfig().apply {
            jdbcUrl = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/eshkolla"
            username = System.getenv("DB_USER") ?: "postgres"
            password = System.getenv("DB_PASSWORD") ?: "postgres"
            maximumPoolSize = (System.getenv("DB_POOL_SIZE")?.toIntOrNull() ?: 5).coerceIn(1, 20)
            minimumIdle = 1
            connectionTimeout = 10_000
            validationTimeout = 5_000
            poolName = "eshkolla-db"
        }.let(::HikariDataSource)
    }

    fun connection(): Connection = dataSource.connection

    fun initialize() {
        connection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS users (id VARCHAR(64) PRIMARY KEY, username VARCHAR(120) NOT NULL UNIQUE, full_name VARCHAR(200) NOT NULL, role VARCHAR(40) NOT NULL, password_hash TEXT NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE)")
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS teachers (id VARCHAR(64) PRIMARY KEY, user_id VARCHAR(64) UNIQUE REFERENCES users(id), full_name VARCHAR(200) NOT NULL, subject_id VARCHAR(64) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE)")
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS classes (id VARCHAR(64) PRIMARY KEY, name VARCHAR(120) NOT NULL, grade_level INT NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE)")
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS students (id VARCHAR(64) PRIMARY KEY, full_name VARCHAR(200) NOT NULL, class_id VARCHAR(64) NOT NULL REFERENCES classes(id), birth_date VARCHAR(20) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE)")
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS teacher_classes (teacher_id VARCHAR(64) NOT NULL REFERENCES teachers(id), class_id VARCHAR(64) NOT NULL REFERENCES classes(id), PRIMARY KEY (teacher_id, class_id))")
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS grades (id VARCHAR(64) PRIMARY KEY, student_id VARCHAR(64) NOT NULL REFERENCES students(id), subject_id VARCHAR(64) NOT NULL, teacher_id VARCHAR(64) NOT NULL REFERENCES teachers(id), value INT NOT NULL CHECK (value BETWEEN 1 AND 5), period VARCHAR(80) NOT NULL, academic_year VARCHAR(20) NOT NULL, note TEXT)")
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS absences (id VARCHAR(64) PRIMARY KEY, student_id VARCHAR(64) NOT NULL REFERENCES students(id), subject_id VARCHAR(64) NOT NULL, teacher_id VARCHAR(64) NOT NULL REFERENCES teachers(id), date VARCHAR(20) NOT NULL, status VARCHAR(40) NOT NULL, note TEXT)")
                statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_students_class ON students(class_id)")
                statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_grades_student ON grades(student_id)")
                statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_absences_student ON absences(student_id)")
            }
            seed(connection)
        }
    }

    private fun seed(connection: Connection) {
        connection.prepareStatement("INSERT INTO classes(id,name,grade_level) VALUES (?,?,?) ON CONFLICT (id) DO NOTHING").use { ps ->
            listOf("C03" to 7, "C04" to 8).forEach { (id, level) -> ps.setString(1, id); ps.setString(2, "Klasa $id"); ps.setInt(3, level); ps.addBatch() }
            ps.executeBatch()
        }
        connection.prepareStatement("INSERT INTO users(id,username,full_name,role,password_hash) VALUES (?,?,?,?,?) ON CONFLICT (id) DO NOTHING").use { ps ->
            ps.setString(1, "3"); ps.setString(2, "leonard.tahiraj"); ps.setString(3, "Leonard Tahiraj"); ps.setString(4, "MESIMDHENES"); ps.setString(5, "120000.256.Hw64c3yCXWTIECIEoATV/w==.Tz/qnvbZG+ulU9H3JfQEmezJfDjD4rydplgU6AypCNE="); ps.executeUpdate()
        }
        connection.prepareStatement("INSERT INTO teachers(id,user_id,full_name,subject_id) VALUES (?,?,?,?) ON CONFLICT (id) DO NOTHING").use { ps ->
            ps.setString(1, "M001"); ps.setString(2, "3"); ps.setString(3, "Leonard Tahiraj"); ps.setString(4, "MAT"); ps.executeUpdate()
        }
        connection.prepareStatement("INSERT INTO teacher_classes(teacher_id,class_id) VALUES (?,?) ON CONFLICT DO NOTHING").use { ps ->
            listOf("C03", "C04").forEach { classId -> ps.setString(1, "M001"); ps.setString(2, classId); ps.addBatch() }; ps.executeBatch()
        }
        connection.prepareStatement("INSERT INTO students(id,full_name,class_id,birth_date,active) VALUES (?,?,?,?,?) ON CONFLICT (id) DO NOTHING").use { ps ->
            listOf(arrayOf("NX001", "Ardit Krasniqi", "C03", "2012-03-01"), arrayOf("NX002", "Era Gashi", "C03", "2012-07-18"), arrayOf("NX003", "Diar Berisha", "C04", "2012-02-09"), arrayOf("NX004", "Suela Hoxha", "C04", "2012-11-22")).forEach { row ->
                ps.setString(1, row[0]); ps.setString(2, row[1]); ps.setString(3, row[2]); ps.setString(4, row[3]); ps.setBoolean(5, true); ps.addBatch()
            }; ps.executeBatch()
        }
    }
}
