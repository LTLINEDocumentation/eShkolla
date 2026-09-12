package com.ltline.eshkolla.domain.school

import com.ltline.eshkolla.domain.model.School

interface SchoolRepository {
    suspend fun getSchools(): List<School>
    suspend fun getSchool(id: String): School?
}
