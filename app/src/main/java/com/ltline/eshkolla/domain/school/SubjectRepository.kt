package com.ltline.eshkolla.domain.school

import com.ltline.eshkolla.domain.model.Subject

interface SubjectRepository {
    suspend fun getSubjects(): List<Subject>
    suspend fun getSubject(id: String): Subject?
}
