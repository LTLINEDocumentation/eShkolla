package com.ltline.eshkolla.domain.model

data class SchoolClass(
    val id: String,
    val schoolId: String,
    val grade: Int,
    val section: String,
    val academicYear: String,
    val classTeacherId: String? = null
) {
    val displayName: String
        get() = "$grade/$section"
}
