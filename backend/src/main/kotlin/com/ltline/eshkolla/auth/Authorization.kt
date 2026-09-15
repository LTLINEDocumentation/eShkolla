package com.ltline.eshkolla.auth

import com.ltline.eshkolla.api.UserDto

class Authorization(private val authService: AuthService) {
    fun user(token: String): UserDto? = authService.userFor(token)

    fun require(token: String, permission: Permission): UserDto {
        val user = authService.userFor(token) ?: throw UnauthorizedException()
        if (!RolePermissions.allows(user.role, permission)) throw ForbiddenException()
        return user
    }
}

class UnauthorizedException : RuntimeException()
class ForbiddenException : RuntimeException()
