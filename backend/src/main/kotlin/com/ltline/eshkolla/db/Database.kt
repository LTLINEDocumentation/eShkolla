package com.ltline.eshkolla.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.sql.Connection
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

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
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS teachers (id VARCHAR(64) PRIMARY KEY, user_id VARCHAR(64) UNIQUE REFERENCES users(id), full_name VARCHAR(200) NOT NULL, subject_id VARCHAR(64) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE)")
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS classes (id VARCHAR(64) PRIMARY KEY, name VARCHAR(120) NOT NULL, grade_level INT NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE)")
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS students (id VARCHAR(64) PRIMARY KEY, full_name VARCHAR(200) NOT NULL, class_id VARCHAR(64) NOT NULL REFERENCES classes(id), birth_date VARCHAR(20) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE)")
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS teacher_classes (teacher_id VARCHAR(64) NOT NULL REFERENCES teachers(id), class_id VARCHAR(64) NOT NULL REFERENCES classes(id), PRIMARY KEY (teacher_id, class_id))")
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS student_users (user_id VARCHAR(64) PRIMARY KEY REFERENCES users(id), student_id VARCHAR(64) UNIQUE NOT NULL REFERENCES students(id))")
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS parent_students (user_id VARCHAR(64) NOT NULL REFERENCES users(id), student_id VARCHAR(64) NOT NULL REFERENCES students(id), PRIMARY KEY (user_id, student_id))")
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS grades (id VARCHAR(64) PRIMARY KEY, student_id VARCHAR(64) NOT NULL REFERENCES students(id), subject_id VARCHAR(64) NOT NULL, teacher_id VARCHAR(64) NOT NULL REFERENCES teachers(id), value INT NOT NULL CHECK (value BETWEEN 1 AND 5), period VARCHAR(80) NOT NULL, academic_year VARCHAR(20) NOT NULL, note TEXT)")
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS absences (id VARCHAR(64) PRIMARY KEY, student_id VARCHAR(64) NOT NULL REFERENCES students(id), subject_id VARCHAR(64) NOT NULL, teacher_id VARCHAR(64) NOT NULL REFERENCES teachers(id), date VARCHAR(20) NOT NULL, status VARCHAR(40) NOT NULL, note TEXT)")
                statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_students_class ON students(class_id)")
                statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_grades_student ON grades(student_id)")
                statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_absences_student ON absences(student_id)")
            }
            bootstrapAdmin(connection)
        }
    }

    private fun bootstrapAdmin(connection: Connection) {
        val username = System.getenv("BOOTSTRAP_ADMIN_USERNAME")?.trim().orEmpty()
        val password = System.getenv("BOOTSTRAP_ADMIN_PASSWORD").orEmpty()
        if (username.isBlank() || password.length < 10) return
        val fullName = System.getenv("BOOTSTRAP_ADMIN_NAME")?.trim().takeUnless { it.isNullOrBlank() } ?: "Administrator eShkolla"
        val hash = PasswordHash.create(password)
        connection.prepareStatement(
            "INSERT INTO users(id,username,full_name,role,password_hash,active) VALUES (?,?,?,?,?,TRUE) " +
                "ON CONFLICT (username) DO UPDATE SET full_name=EXCLUDED.full_name, role='ADMINISTRATOR', active=TRUE"
        ).use { ps ->
            ps.setString(1, "ADMIN-BOOTSTRAP")
            ps.setString(2, username)
            ps.setString(3, fullName)
            ps.setString(4, "ADMINISTRATOR")
            ps.setString(5, hash)
            ps.executeUpdate()
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
        val jdbcUrl = "jdbc:postgresql://$host$port$path$query"
        return ParsedDatabaseUrl(jdbcUrl, username, password)
    }

    private fun String.decodeUrlComponent(): String = URLDecoder.decode(replace("+", "%2B"), StandardCharsets.UTF_8)

    private object PasswordHash {
        private const val ITERATIONS = 120_000
        private const val KEY_BITS = 256
        private const val SALT_BYTES = 16

        fun create(password: String): String {
            val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
            val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
            val hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            spec.clearPassword()
            return listOf(ITERATIONS, KEY_BITS, Base64.getEncoder().encodeToString(salt), Base64.getEncoder().encodeToString(hash)).joinToString(".")
        }
    }
}
