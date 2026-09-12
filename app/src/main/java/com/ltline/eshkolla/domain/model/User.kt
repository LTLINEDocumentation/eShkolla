package com.ltline.eshkolla.domain.model

data class User(
    val id: String,
    val username: String,
    val fullName: String,
    val role: UserRole,
    val isActive: Boolean = true
)
