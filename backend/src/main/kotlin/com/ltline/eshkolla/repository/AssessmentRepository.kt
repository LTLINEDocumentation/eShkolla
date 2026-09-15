package com.ltline.eshkolla.repository

import com.ltline.eshkolla.api.AbsenceDto
import com.ltline.eshkolla.api.GradeDto
import java.util.concurrent.ConcurrentHashMap

interface GradeRepository {
    fun findAll(studentId: String? = null, teacherId: String? = null): List<GradeDto>
    fun save(grade: GradeDto): GradeDto
}

interface AbsenceRepository {
    fun findAll(studentId: String? = null, teacherId: String? = null): List<AbsenceDto>
    fun save(absence: AbsenceDto): AbsenceDto
}

class InMemoryGradeRepository : GradeRepository {
    private val grades = ConcurrentHashMap<String, GradeDto>()

    override fun findAll(studentId: String?, teacherId: String?): List<GradeDto> = grades.values
        .filter { studentId == null || it.studentId == studentId }
        .filter { teacherId == null || it.teacherId == teacherId }
        .sortedByDescending { it.id }

    override fun save(grade: GradeDto): GradeDto {
        grades[grade.id] = grade
        return grade
    }
}

class InMemoryAbsenceRepository : AbsenceRepository {
    private val absences = ConcurrentHashMap<String, AbsenceDto>()

    override fun findAll(studentId: String?, teacherId: String?): List<AbsenceDto> = absences.values
        .filter { studentId == null || it.studentId == studentId }
        .filter { teacherId == null || it.teacherId == teacherId }
        .sortedByDescending { it.date }

    override fun save(absence: AbsenceDto): AbsenceDto {
        absences[absence.id] = absence
        return absence
    }
}
