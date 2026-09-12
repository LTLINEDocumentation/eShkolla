package com.ltline.eshkolla.data.assessment

import com.ltline.eshkolla.domain.model.Absence
import com.ltline.eshkolla.domain.model.AbsenceStatus
import com.ltline.eshkolla.domain.model.Grade
import com.ltline.eshkolla.domain.repository.AbsenceRepository
import com.ltline.eshkolla.domain.repository.GradeRepository
import java.util.UUID

class FakeGradeRepository : GradeRepository {
    private val grades = mutableListOf(
        Grade("G001", "NX001", "L01", "M001", 5, "Periudha I", "2026/2027", "Test"),
        Grade("G002", "NX002", "L01", "M001", 4, "Periudha I", "2026/2027", "Detyrë shtëpie"),
        Grade("G003", "NX003", "L01", "M001", 5, "Periudha I", "2026/2027", "Aktivitet në klasë")
    )

    override suspend fun getGrades() = grades.toList()
    override suspend fun getGradesByStudent(studentId: String) = grades.filter { it.studentId == studentId }
    override suspend fun addGrade(grade: Grade): Grade { grades += grade; return grade }
    override suspend fun updateGrade(grade: Grade): Grade {
        val i = grades.indexOfFirst { it.id == grade.id }
        if (i >= 0) grades[i] = grade
        return grade
    }
    override suspend fun deleteGrade(id: String) { grades.removeIf { it.id == id } }
}

class FakeAbsenceRepository : AbsenceRepository {
    private val absences = mutableListOf(
        Absence("A001", "NX001", "L01", "M001", "14.09.2026", AbsenceStatus.E_PAAFTESUAR),
        Absence("A002", "NX002", "L01", "M001", "16.09.2026", AbsenceStatus.E_ARSYESHME, "Arsyetim nga prindi"),
        Absence("A003", "NX003", "L01", "M001", "17.09.2026", AbsenceStatus.E_PAAFTESUAR)
    )

    override suspend fun getAbsences() = absences.toList()
    override suspend fun getAbsencesByStudent(studentId: String) = absences.filter { it.studentId == studentId }
    override suspend fun addAbsence(absence: Absence): Absence { absences += absence; return absence }
    override suspend fun updateAbsence(absence: Absence): Absence {
        val i = absences.indexOfFirst { it.id == absence.id }
        if (i >= 0) absences[i] = absence
        return absence
    }
    override suspend fun deleteAbsence(id: String) { absences.removeIf { it.id == id } }
}
