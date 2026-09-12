package com.ltline.eshkolla.domain.model

data class Grade(
    val id: String,
    val studentId: String,
    val subjectId: String,
    val teacherId: String,
    val value: Int,
    val period: String,
    val academicYear: String,
    val note: String? = null
) {
    init {
        require(value in 1..5) { "Nota duhet të jetë nga 1 deri në 5." }
    }
}
