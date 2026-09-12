package com.ltline.eshkolla.domain.model

data class Announcement(
    val id: String,
    val schoolId: String,
    val title: String,
    val message: String,
    val authorUserId: String,
    val createdAt: String,
    val isPublished: Boolean = true
)
