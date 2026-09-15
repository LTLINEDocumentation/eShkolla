package com.ltline.eshkolla.auth

import com.ltline.eshkolla.api.UserDto
import com.ltline.eshkolla.db.Database
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class AuthService {
    private data class Account(val id: String, val username: String, val fullName: String, val role: String)

    companion object {
        private val sessions = ConcurrentHashMap<String, Account>()
    }

    fun login(username: String, password: String): Pair<String, UserDto>? {
        val account = Database.connection().use { connection ->
            connection.prepareStatement("SELECT id, username, full_name, role, password_hash, active FROM users WHERE username = ? LIMIT 1").use { ps ->
                ps.setString(1, username)
                ps.executeQuery().use { rs ->
                    if (!rs.next() || !rs.getBoolean("active")) return@use null
                    val hash = rs.getString("password_hash")
                    if (!PasswordHasher.verify(password, hash)) return@use null
                    Account(rs.getString("id"), rs.getString("username"), rs.getString("full_name"), rs.getString("role"))
                }
            }
        } ?: return null

        val token = UUID.randomUUID().toString()
        sessions[token] = account
        return token to account.toDto()
    }

    fun userFor(token: String): UserDto? = sessions[token]?.toDto()
    fun logout(token: String) { sessions.remove(token) }

    fun teacherIdFor(token: String): String? {
        val account = sessions[token] ?: return null
        if (account.role != "MESIMDHENES") return null
        return Database.connection().use { connection ->
            connection.prepareStatement("SELECT id FROM teachers WHERE user_id = ? AND active = TRUE LIMIT 1").use { ps ->
                ps.setString(1, account.id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.getString("id") else null }
            }
        }
    }

    private fun Account.toDto() = UserDto(id, username, fullName, role, true)
}
