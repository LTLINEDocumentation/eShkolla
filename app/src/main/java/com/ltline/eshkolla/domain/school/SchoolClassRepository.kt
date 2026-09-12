package com.ltline.eshkolla.domain.school

import com.ltline.eshkolla.domain.model.SchoolClass

interface SchoolClassRepository {
    suspend fun getClasses(): List<SchoolClass>
    suspend fun getClass(id: String): SchoolClass?
}
