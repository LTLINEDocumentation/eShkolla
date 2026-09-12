package com.ltline.eshkolla.auth

import com.ltline.eshkolla.api.UserDto
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class AuthService {
    private data class Account(val id: String, val username: String, val fullName: String, val role: String, val passwordHash: String)

    private val accounts = listOf(
        account("1", "admin", "Administrator", "ADMINISTRATOR", "123456"),
        account("2", "drejtor", "Drejtor i shkollës", "DREJTOR", "123456"),
        account("3", "mesimdhenes", "Mësimdhënës Demo", "MESIMDHENES", "123456"),
        account("4", "nxenes", "Nxënës Demo", "NXENES", "123456"),
        account("5", "prind", "Prind Demo", "PRIND", "123456")
    )
    private val sessions = ConcurrentHashMap<String, Account>()

    fun login(username: String, password: String): Pair<String, UserDto>? {
        val account = accounts.firstOrNull { it.username == username && verify(password, it.passwordHash) } ?: return null
        val token = UUID.randomUUID().toString()
        sessions[token] = account
        return token to account.toDto()
    }

    fun userFor(token: String): UserDto? = sessions[token]?.toDto()
    fun logout(token: String) { sessions.remove(token) }

    private fun Account.toDto() = UserDto(id, username, fullName, role, true)

    private fun account(id: String, username: String, fullName: String, role: String, password: String) =
        Account(id, username, fullName, role, hash(password))

    private fun hash(value: String): String {
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val digest = MessageDigest.getInstance("SHA-256").digest(salt + value.toByteArray())
        return Base64.getEncoder().encodeToString(salt) + "." + Base64.getEncoder().encodeToString(digest)
    }

    private fun verify(value: String, encoded: String): Boolean {
        val parts = encoded.split('.')
        if (parts.size != 2) return false
        val salt = Base64.getDecoder().decode(parts[0])
        val expected = Base64.getDecoder().decode(parts[1])
        val actual = MessageDigest.getInstance("SHA-256").digest(salt + value.toByteArray())
        return MessageDigest.isEqual(expected, actual)
    }
}
