package com.ltline.eshkolla.domain.model

data class Teacher(
    val id: String,
    val schoolId: String,
    val userId: String,
    val employeeNumber: String? = null,
    val qualification: String? = null,
    val isActive: Boolean = true
)
