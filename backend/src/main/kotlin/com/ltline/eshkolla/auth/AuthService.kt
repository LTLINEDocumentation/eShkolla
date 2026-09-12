package com.ltline.eshkolla.auth

import com.ltline.eshkolla.api.UserDto
import java.security.SecureRandom
import java.security.spec.PBEKeySpec
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.SecretKeyFactory

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
    private val random = SecureRandom()

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
        val salt = ByteArray(16).also(random::nextBytes)
        val spec = PBEKeySpec(value.toCharArray(), salt, ITERATIONS, KEY_BITS)
        val derived = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        spec.clearPassword()
        return "$ITERATIONS.$KEY_BITS.${Base64.getEncoder().encodeToString(salt)}.${Base64.getEncoder().encodeToString(derived)}"
    }

    private fun verify(value: String, encoded: String): Boolean {
        val parts = encoded.split('.')
        if (parts.size != 4) return false
        val iterations = parts[0].toIntOrNull() ?: return false
        val keyBits = parts[1].toIntOrNull() ?: return false
        val salt = Base64.getDecoder().decode(parts[2])
        val expected = Base64.getDecoder().decode(parts[3])
        val spec = PBEKeySpec(value.toCharArray(), salt, iterations, keyBits)
        val actual = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        spec.clearPassword()
        return java.security.MessageDigest.isEqual(expected, actual)
    }

    private companion object {
        const val ITERATIONS = 120_000
        const val KEY_BITS = 256
    }
}
