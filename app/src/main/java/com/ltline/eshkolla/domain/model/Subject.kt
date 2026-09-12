package com.ltline.eshkolla.domain.model

data class Subject(
    val id: String,
    val schoolId: String,
    val name: String,
    val shortName: String? = null,
    val weeklyHours: Int = 1,
    val isActive: Boolean = true
)
