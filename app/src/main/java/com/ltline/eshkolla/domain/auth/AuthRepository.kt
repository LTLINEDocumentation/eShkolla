package com.ltline.eshkolla.domain.auth

import com.ltline.eshkolla.domain.model.User

interface AuthRepository {
    suspend fun login(username: String, password: String): Result<User>
    suspend fun logout()
}
