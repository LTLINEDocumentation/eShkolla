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
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS student_users (user_id VARCHAR(64) PRIMARY KEY REFERENCES users(id), student_id VARCHAR(64) UNIQUE NOT NULL REFERENCES students(id))")
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS parent_students (user_id VARCHAR(64) NOT NULL REFERENCES users(id), student_id VARCHAR(64) NOT NULL REFERENCES students(id), PRIMARY KEY (user_id, student_id))")
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
        connection.prepareStatement("INSERT INTO users(id,username,full_name,role,password_hash) VALUES (?,?,?,?,?) ON CONFLICT (id) DO NOTHING").use { ps ->
            val users = listOf(
                arrayOf("1", "admin", "Administrator", "ADMINISTRATOR", "120000.256.QiRFEz1UR6LS1H56XLO1lg==.DcCG1x4kyQLDlpFFIChVQxA3jGFZJkeXJR6Nc3QpG/k="),
                arrayOf("2", "drejtor", "Drejtor i shkollës", "DREJTOR", "120000.256.K4sPQFt/jEk7o9pCul1KFA==.Qex3SbmfEEE20GxXzwF0YCxCikntFLAer4FEp3teT9g="),
                arrayOf("3", "leonard.tahiraj", "Leonard Tahiraj", "MESIMDHENES", "120000.256.d0xej47sYaOo4L2r+tdpQQ==.iaJDzC157DG5Xx1Y/TdteURyncEFeCRik0VnHB9cQnk="),
                arrayOf("4", "nxenes", "Nxënës Demo", "NXENES", "120000.256.Y+gXYtwX6REChnZ0tSkE9Q==.241tPQuiTy03/PWOGhIMhBrd0Y0cUrFvjFTGJM1J6W8="),
                arrayOf("5", "prind", "Prind Demo", "PRIND", "120000.256.9Y8S/F2pUPYPUab4+XOxLA==.qHSGpKBGmw0aEoYBzOePooNcoxSPvWXbHvIEDhFLkMI=")
            )
            users.forEach { row ->
                ps.setString(1, row[0]); ps.setString(2, row[1]); ps.setString(3, row[2]); ps.setString(4, row[3]); ps.setString(5, row[4]); ps.addBatch()
            }
            ps.executeBatch()
        }
        connection.prepareStatement("INSERT INTO classes(id,name,grade_level) VALUES (?,?,?) ON CONFLICT (id) DO NOTHING").use { ps ->
            listOf("C03" to 7, "C04" to 8).forEach { (id, level) -> ps.setString(1, id); ps.setString(2, "Klasa $id"); ps.setInt(3, level); ps.addBatch() }
            ps.executeBatch()
        }
        connection.prepareStatement("INSERT INTO teachers(id,user_id,full_name,subject_id) VALUES (?,?,?,?) ON CONFLICT (id) DO NOTHING").use { ps ->
            ps.setString(1, "M001"); ps.setString(2, "3"); ps.setString(3, "Leonard Tahiraj"); ps.setString(4, "MAT"); ps.executeUpdate()
        }
        connection.prepareStatement("INSERT INTO teacher_classes(teacher_id,class_id) VALUES (?,?) ON CONFLICT DO NOTHING").use { ps ->
            listOf("C03", "C04").forEach { classId -> ps.setString(1, "M001"); ps.setString(2, classId); ps.addBatch() }
            ps.executeBatch()
        }
        connection.prepareStatement("INSERT INTO students(id,full_name,class_id,birth_date,active) VALUES (?,?,?,?,?) ON CONFLICT (id) DO NOTHING").use { ps ->
            listOf(arrayOf("NX001", "Ardit Krasniqi", "C03", "2012-03-01"), arrayOf("NX002", "Era Gashi", "C03", "2012-07-18"), arrayOf("NX003", "Diar Berisha", "C04", "2012-02-09"), arrayOf("NX004", "Suela Hoxha", "C04", "2012-11-22")).forEach { row ->
                ps.setString(1, row[0]); ps.setString(2, row[1]); ps.setString(3, row[2]); ps.setString(4, row[3]); ps.setBoolean(5, true); ps.addBatch()
            }
            ps.executeBatch()
        }
        connection.prepareStatement("INSERT INTO student_users(user_id,student_id) VALUES (?,?) ON CONFLICT DO NOTHING").use { ps ->
            ps.setString(1, "4"); ps.setString(2, "NX001"); ps.executeUpdate()
        }
        connection.prepareStatement("INSERT INTO parent_students(user_id,student_id) VALUES (?,?) ON CONFLICT DO NOTHING").use { ps ->
            ps.setString(1, "5"); ps.setString(2, "NX001"); ps.executeUpdate()
        }
    }
}