package com.ltline.eshkolla.domain.model

data class School(
    val id: String,
    val name: String,
    val address: String,
    val phone: String? = null,
    val email: String? = null,
    val isActive: Boolean = true
)
