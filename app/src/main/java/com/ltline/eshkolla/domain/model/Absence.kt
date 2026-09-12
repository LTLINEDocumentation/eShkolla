package com.ltline.eshkolla.domain.model

enum class AbsenceStatus {
    E_PAAFTESUAR,
    E_ARSYESHME
}

data class Absence(
    val id: String,
    val studentId: String,
    val subjectId: String,
    val teacherId: String,
    val date: String,
    val status: AbsenceStatus = AbsenceStatus.E_PAAFTESUAR,
    val note: String? = null
)
