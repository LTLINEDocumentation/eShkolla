package com.ltline.eshkolla.repository

import com.ltline.eshkolla.api.AbsenceDto
import com.ltline.eshkolla.api.GradeDto
import com.ltline.eshkolla.db.Database
import java.sql.ResultSet

interface GradeRepository {
    fun findAll(studentId: String? = null, teacherId: String? = null): List<GradeDto>
    fun findById(id: String): GradeDto?
    fun save(grade: GradeDto): GradeDto
    fun delete(id: String): Boolean
}

interface AbsenceRepository {
    fun findAll(studentId: String? = null, teacherId: String? = null): List<AbsenceDto>
    fun findById(id: String): AbsenceDto?
    fun save(absence: AbsenceDto): AbsenceDto
    fun delete(id: String): Boolean
}

class PostgresGradeRepository : GradeRepository {
    override fun findAll(studentId: String?, teacherId: String?): List<GradeDto> = Database.connection().use { c ->
        val sql = "SELECT id,student_id,subject_id,teacher_id,value,period,academic_year,note FROM grades WHERE (? IS NULL OR student_id=?) AND (? IS NULL OR teacher_id=?) ORDER BY id DESC"
        c.prepareStatement(sql).use { ps ->
            ps.setString(1, studentId); ps.setString(2, studentId); ps.setString(3, teacherId); ps.setString(4, teacherId)
            ps.executeQuery().use { rs -> rs.mapGrades() }
        }
    }

    override fun findById(id: String): GradeDto? = Database.connection().use { c ->
        c.prepareStatement("SELECT id,student_id,subject_id,teacher_id,value,period,academic_year,note FROM grades WHERE id=?").use { ps ->
            ps.setString(1, id); ps.executeQuery().use { rs -> rs.mapGrades().firstOrNull() }
        }
    }

    override fun save(grade: GradeDto): GradeDto = Database.connection().use { c ->
        c.prepareStatement("INSERT INTO grades(id,student_id,subject_id,teacher_id,value,period,academic_year,note) VALUES (?,?,?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET value=EXCLUDED.value,period=EXCLUDED.period,academic_year=EXCLUDED.academic_year,note=EXCLUDED.note").use { ps ->
            ps.setString(1, grade.id); ps.setString(2, grade.studentId); ps.setString(3, grade.subjectId); ps.setString(4, grade.teacherId); ps.setInt(5, grade.value); ps.setString(6, grade.period); ps.setString(7, grade.academicYear); ps.setString(8, grade.note); ps.executeUpdate()
        }; grade
    }

    override fun delete(id: String): Boolean = Database.connection().use { c ->
        c.prepareStatement("DELETE FROM grades WHERE id=?").use { ps -> ps.setString(1, id); ps.executeUpdate() > 0 }
    }

    private fun ResultSet.mapGrades(): List<GradeDto> { val result = mutableListOf<GradeDto>(); while (next()) result += GradeDto(getString("id"), getString("student_id"), getString("subject_id"), getString("teacher_id"), getInt("value"), getString("period"), getString("academic_year"), getString("note")); return result }
}

class PostgresAbsenceRepository : AbsenceRepository {
    override fun findAll(studentId: String?, teacherId: String?): List<AbsenceDto> = Database.connection().use { c ->
        val sql = "SELECT id,student_id,subject_id,teacher_id,date,status,note FROM absences WHERE (? IS NULL OR student_id=?) AND (? IS NULL OR teacher_id=?) ORDER BY date DESC,id DESC"
        c.prepareStatement(sql).use { ps ->
            ps.setString(1, studentId); ps.setString(2, studentId); ps.setString(3, teacherId); ps.setString(4, teacherId)
            ps.executeQuery().use { rs -> rs.mapAbsences() }
        }
    }

    override fun findById(id: String): AbsenceDto? = Database.connection().use { c ->
        c.prepareStatement("SELECT id,student_id,subject_id,teacher_id,date,status,note FROM absences WHERE id=?").use { ps ->
            ps.setString(1, id); ps.executeQuery().use { rs -> rs.mapAbsences().firstOrNull() }
        }
    }

    override fun save(absence: AbsenceDto): AbsenceDto = Database.connection().use { c ->
        c.prepareStatement("INSERT INTO absences(id,student_id,subject_id,teacher_id,date,status,note) VALUES (?,?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET date=EXCLUDED.date,status=EXCLUDED.status,note=EXCLUDED.note").use { ps ->
            ps.setString(1, absence.id); ps.setString(2, absence.studentId); ps.setString(3, absence.subjectId); ps.setString(4, absence.teacherId); ps.setString(5, absence.date); ps.setString(6, absence.status); ps.setString(7, absence.note); ps.executeUpdate()
        }; absence
    }

    override fun delete(id: String): Boolean = Database.connection().use { c ->
        c.prepareStatement("DELETE FROM absences WHERE id=?").use { ps -> ps.setString(1, id); ps.executeUpdate() > 0 }
    }

    private fun ResultSet.mapAbsences(): List<AbsenceDto> { val result = mutableListOf<AbsenceDto>(); while (next()) result += AbsenceDto(getString("id"), getString("student_id"), getString("subject_id"), getString("teacher_id"), getString("date"), getString("status"), getString("note")); return result }
}
