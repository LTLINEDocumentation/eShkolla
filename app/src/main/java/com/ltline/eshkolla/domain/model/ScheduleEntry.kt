package com.ltline.eshkolla.domain.model

enum class WeekDay {
    E_HENE,
    E_MARTE,
    E_MERKURE,
    E_ENJTE,
    E_PREMTE,
    E_SHTUNE
}

data class ScheduleEntry(
    val id: String,
    val schoolId: String,
    val classId: String,
    val subjectId: String,
    val teacherId: String,
    val weekDay: WeekDay,
    val lessonNumber: Int,
    val room: String? = null
)
