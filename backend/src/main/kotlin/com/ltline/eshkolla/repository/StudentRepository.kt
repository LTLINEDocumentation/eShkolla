package com.ltline.eshkolla.repository

import com.ltline.eshkolla.api.StudentDto
import com.ltline.eshkolla.db.Database
import java.sql.ResultSet

interface StudentRepository {
    fun findAll(): List<StudentDto>
    fun findById(id: String): StudentDto?
    fun save(student: StudentDto): StudentDto
    fun delete(id: String): Boolean
}

class PostgresStudentRepository : StudentRepository {
    override fun findAll(): List<StudentDto> = Database.connection().use { c ->
        c.prepareStatement("SELECT id,full_name,class_id,birth_date,active FROM students ORDER BY full_name").use { ps -> ps.executeQuery().use { rs -> rs.mapStudents() } }
    }

    override fun findById(id: String): StudentDto? = Database.connection().use { c ->
        c.prepareStatement("SELECT id,full_name,class_id,birth_date,active FROM students WHERE id=?").use { ps ->
            ps.setString(1, id); ps.executeQuery().use { rs -> if (rs.next()) rs.toStudent() else null }
        }
    }

    override fun save(student: StudentDto): StudentDto = Database.connection().use { c ->
        c.prepareStatement("INSERT INTO students(id,full_name,class_id,birth_date,active) VALUES (?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET full_name=EXCLUDED.full_name,class_id=EXCLUDED.class_id,birth_date=EXCLUDED.birth_date,active=EXCLUDED.active").use { ps ->
            ps.setString(1, student.id); ps.setString(2, student.fullName); ps.setString(3, student.classId); ps.setString(4, student.birthDate); ps.setBoolean(5, student.isActive); ps.executeUpdate()
        }
        student
    }

    override fun delete(id: String): Boolean = Database.connection().use { c ->
        c.prepareStatement("UPDATE students SET active=false WHERE id=? AND active=true").use { ps -> ps.setString(1, id); ps.executeUpdate() > 0 }
    }

    private fun ResultSet.toStudent() = StudentDto(getString("id"), getString("full_name"), getString("class_id"), getString("birth_date"), getBoolean("active"))
    private fun ResultSet.mapStudents(): List<StudentDto> { val result = mutableListOf<StudentDto>(); while (next()) result += toStudent(); return result }
}
