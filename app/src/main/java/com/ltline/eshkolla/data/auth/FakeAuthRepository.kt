package com.ltline.eshkolla.data.auth

import com.ltline.eshkolla.domain.auth.AuthRepository
import com.ltline.eshkolla.domain.model.User
import com.ltline.eshkolla.domain.model.UserRole

class FakeAuthRepository : AuthRepository {
    private val demoUsers = listOf(
        User("1", "admin", "Administrator", UserRole.ADMINISTRATOR),
        User("2", "drejtor", "Drejtor i shkollës", UserRole.DREJTOR),
        User("3", "leonard.tahiraj", "Leonard Tahiraj", UserRole.MESIMDHENES),
        User("4", "nxenes", "Nxënës Demo", UserRole.NXENES),
        User("5", "prind", "Prind Demo", UserRole.PRIND)
    )

    override suspend fun login(username: String, password: String): Result<User> {
        if (username.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Plotësoni përdoruesin dhe fjalëkalimin."))
        }

        val user = demoUsers.firstOrNull { it.username.equals(username.trim(), ignoreCase = true) }
            ?: return Result.failure(IllegalArgumentException("Përdoruesi nuk u gjet."))

        if (password != "123456") {
            return Result.failure(IllegalArgumentException("Fjalëkalimi është i pasaktë."))
        }

        if (!user.isActive) {
            return Result.failure(IllegalStateException("Llogaria është joaktive."))
        }

        return Result.success(user)
    }

    override suspend fun logout() = Unit
}
