package com.ltline.eshkolla.api

import kotlinx.serialization.Serializable

@Serializable
data class ApiError(val code: String, val message: String)

@Serializable
data class HealthResponse(val status: String, val service: String, val version: String)

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class LoginResponse(val token: String, val user: UserDto)

@Serializable
data class UserDto(
    val id: String,
    val username: String,
    val fullName: String,
    val role: String,
    val isActive: Boolean
)

@Serializable
data class StudentDto(
    val id: String,
    val fullName: String,
    val classId: String,
    val birthDate: String,
    val isActive: Boolean
)

@Serializable
data class StudentRequest(
    val fullName: String,
    val classId: String,
    val birthDate: String
)

@Serializable
data class GradeDto(
    val id: String,
    val studentId: String,
    val subjectId: String,
    val teacherId: String,
    val value: Int,
    val period: String,
    val academicYear: String,
    val note: String? = null
)

@Serializable
data class GradeRequest(
    val studentId: String,
    val subjectId: String,
    val teacherId: String,
    val value: Int,
    val period: String,
    val academicYear: String,
    val note: String? = null
)

@Serializable
data class AbsenceDto(
    val id: String,
    val studentId: String,
    val subjectId: String,
    val teacherId: String,
    val date: String,
    val status: String,
    val note: String? = null
)

@Serializable
data class AbsenceRequest(
    val studentId: String,
    val subjectId: String,
    val teacherId: String,
    val date: String,
    val status: String,
    val note: String? = null
)

@Serializable
data class PagedResponse<T>(
    val items: List<T>,
    val page: Int,
    val pageSize: Int,
    val total: Int
)
