package com.ltline.eshkolla.domain.model

data class Student(
    val id: String,
    val schoolId: String,
    val classId: String,
    val firstName: String,
    val lastName: String,
    val birthDate: String? = null,
    val parentUserId: String? = null,
    val isActive: Boolean = true
) {
    val fullName: String
        get() = "$firstName $lastName"
}
