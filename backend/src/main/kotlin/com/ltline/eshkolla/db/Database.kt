package com.ltline.eshkolla.db

import com.ltline.eshkolla.auth.PasswordHasher
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.sql.Connection
import java.util.UUID

object Database {
    private val dataSource: HikariDataSource by lazy {
        val config = HikariConfig()
        val databaseUrl = System.getenv("DATABASE_URL")?.trim().orEmpty()
        if (databaseUrl.isNotBlank()) {
            val parsed = parseDatabaseUrl(databaseUrl)
            config.jdbcUrl = parsed.jdbcUrl
            parsed.username?.let(config::setUsername)
            parsed.password?.let(config::setPassword)
        } else {
            config.jdbcUrl = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/eshkolla"
            config.username = System.getenv("DB_USER") ?: "postgres"
            config.password = System.getenv("DB_PASSWORD") ?: "postgres"
        }
        config.maximumPoolSize = (System.getenv("DB_POOL_SIZE")?.toIntOrNull() ?: 5).coerceIn(1, 20)
        config.minimumIdle = 1
        config.connectionTimeout = 10_000
        config.validationTimeout = 5_000
        config.poolName = "eshkolla-db"
        HikariDataSource(config)
    }

    fun connection(): Connection = dataSource.connection

    fun initialize() {
        connection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS users (id VARCHAR(64) PRIMARY KEY, username VARCHAR(120) NOT NULL UNIQUE, full_name VARCHAR(200) NOT NULL, role VARCHAR(40) NOT NULL, password_hash TEXT NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE)")
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS schools (id VARCHAR(64) PRIMARY KEY, name VARCHAR(200) NOT NULL UNIQUE, address VARCHAR(300), active BOOLEAN NOT NULL DEFAULT TRUE)")
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS subjects (id VARCHAR(64) PRIMARY KEY, name VARCHAR(160) NOT NULL UNIQUE, code VARCHAR(40) UNIQUE, active BOOLEAN NOT NULL DEFAULT TRUE)")
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS teachers (id VARCHAR(64) PRIMARY KEY, user_id VARCHAR(64) UNIQUE REFERENCES users(id), full_name VARCHAR(200) NOT NULL, subject_id VARCHAR(64) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE)")
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS classes (id VARCHAR(64) PRIMARY KEY, name VARCHAR(120) NOT NULL, grade_level INT NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE)")
                statement.executeUpdate("ALTER TABLE classes ADD COLUMN IF NOT EXISTS school_id VARCHAR(64) REFERENCES schools(id)")
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS students (id VARCHAR(64) PRIMARY KEY, full_name VARCHAR(200) NOT NULL, class_id VARCHAR(64) NOT NULL REFERENCES classes(id), birth_date VARCHAR(20) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE)")
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS teacher_classes (teacher_id VARCHAR(64) NOT NULL REFERENCES teachers(id), class_id VARCHAR(64) NOT NULL REFERENCES classes(id), PRIMARY KEY (teacher_id, class_id))")
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS teacher_subjects (teacher_id VARCHAR(64) NOT NULL REFERENCES teachers(id), subject_id VARCHAR(64) NOT NULL REFERENCES subjects(id), class_id VARCHAR(64) NOT NULL REFERENCES classes(id), PRIMARY KEY (teacher_id, subject_id, class_id))")
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS student_users (user_id VARCHAR(64) PRIMARY KEY REFERENCES users(id), student_id VARCHAR(64) UNIQUE NOT NULL REFERENCES students(id))")
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS parent_students (user_id VARCHAR(64) NOT NULL REFERENCES users(id), student_id VARCHAR(64) NOT NULL REFERENCES students(id), PRIMARY KEY (user_id, student_id))")
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS grades (id VARCHAR(64) PRIMARY KEY, student_id VARCHAR(64) NOT NULL REFERENCES students(id), subject_id VARCHAR(64) NOT NULL, teacher_id VARCHAR(64) NOT NULL REFERENCES teachers(id), value INT NOT NULL CHECK (value BETWEEN 1 AND 5), period VARCHAR(80) NOT NULL, academic_year VARCHAR(20) NOT NULL, note TEXT)")
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS absences (id VARCHAR(64) PRIMARY KEY, student_id VARCHAR(64) NOT NULL REFERENCES students(id), subject_id VARCHAR(64) NOT NULL, teacher_id VARCHAR(64) NOT NULL REFERENCES teachers(id), date VARCHAR(20) NOT NULL, status VARCHAR(40) NOT NULL, note TEXT)")
                statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_students_class ON students(class_id)")
                statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_grades_student ON grades(student_id)")
                statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_absences_student ON absences(student_id)")
                statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_teacher_subjects_class ON teacher_subjects(class_id)")
            }
            bootstrapAdmin(connection)
        }
    }

    private fun bootstrapAdmin(connection: Connection) {
        val username = System.getenv("BOOTSTRAP_ADMIN_USERNAME")?.trim().orEmpty()
        val password = System.getenv("BOOTSTRAP_ADMIN_PASSWORD").orEmpty()
        if (username.isBlank() || password.length < 10) return
        val fullName = System.getenv("BOOTSTRAP_ADMIN_NAME")?.trim().takeUnless { it.isNullOrBlank() } ?: "Administrator eShkolla"
        val hash = PasswordHasher.create(password)
        connection.prepareStatement("SELECT id FROM users WHERE username = ?").use { find ->
            find.setString(1, username)
            find.executeQuery().use { rs ->
                if (rs.next()) {
                    connection.prepareStatement("UPDATE users SET full_name=?, role='ADMINISTRATOR', password_hash=?, active=TRUE WHERE id=?").use { update ->
                        update.setString(1, fullName); update.setString(2, hash); update.setString(3, rs.getString("id")); update.executeUpdate()
                    }
                } else {
                    connection.prepareStatement("INSERT INTO users(id,username,full_name,role,password_hash,active) VALUES (?,?,?,?,?,TRUE)").use { insert ->
                        insert.setString(1, UUID.randomUUID().toString()); insert.setString(2, username); insert.setString(3, fullName); insert.setString(4, "ADMINISTRATOR"); insert.setString(5, hash); insert.executeUpdate()
                    }
                }
            }
        }
    }

    private data class ParsedDatabaseUrl(val jdbcUrl: String, val username: String?, val password: String?)
    private fun parseDatabaseUrl(value: String): ParsedDatabaseUrl {
        if (value.startsWith("jdbc:")) return ParsedDatabaseUrl(value, null, null)
        val normalized = value.replaceFirst(Regex("^postgres(ql)?://"), "postgresql://")
        val uri = URI(normalized)
        val userInfo = uri.rawUserInfo?.split(":", limit = 2)
        val username = userInfo?.getOrNull(0)?.decodeUrlComponent()
        val password = userInfo?.getOrNull(1)?.decodeUrlComponent()
        val host = uri.host ?: error("DATABASE_URL nuk përmban host të vlefshëm")
        val port = if (uri.port > 0) ":${uri.port}" else ""
        val path = uri.rawPath?.takeIf { it.isNotBlank() } ?: "/postgres"
        val query = uri.rawQuery?.takeIf { it.isNotBlank() }?.let { if (it.contains("sslmode=")) "?$it" else "?$it&sslmode=require" } ?: "?sslmode=require"
        return ParsedDatabaseUrl("jdbc:postgresql://$host$port$path$query", username, password)
    }
    private fun String.decodeUrlComponent(): String = URLDecoder.decode(replace("+", "%2B"), StandardCharsets.UTF_8)
}
