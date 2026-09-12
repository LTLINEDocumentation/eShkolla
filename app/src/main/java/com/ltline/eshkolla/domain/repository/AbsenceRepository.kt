package com.ltline.eshkolla.domain.repository

import com.ltline.eshkolla.domain.model.Absence

interface AbsenceRepository {
    suspend fun getAbsences(): List<Absence>
    suspend fun getAbsencesByStudent(studentId: String): List<Absence>
    suspend fun addAbsence(absence: Absence): Absence
    suspend fun updateAbsence(absence: Absence): Absence
    suspend fun deleteAbsence(id: String)
}
