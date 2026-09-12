package com.ltline.eshkolla.domain.repository

import com.ltline.eshkolla.domain.model.Grade

interface GradeRepository {
    suspend fun getGrades(): List<Grade>
    suspend fun getGradesByStudent(studentId: String): List<Grade>
    suspend fun addGrade(grade: Grade): Grade
    suspend fun updateGrade(grade: Grade): Grade
    suspend fun deleteGrade(id: String)
}
