package com.ltline.eshkolla.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16

    fun create(password: String): String {
        require(password.length >= 10) { "Fjalëkalimi duhet të ketë së paku 10 karaktere." }
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
        val hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        spec.clearPassword()
        return listOf(ITERATIONS, KEY_BITS, Base64.getEncoder().encodeToString(salt), Base64.getEncoder().encodeToString(hash)).joinToString(".")
    }

    fun verify(password: String, encoded: String): Boolean = runCatching {
        val parts = encoded.split('.')
        if (parts.size != 4) return false
        val iterations = parts[0].toIntOrNull() ?: return false
        val keyBits = parts[1].toIntOrNull() ?: return false
        val salt = Base64.getDecoder().decode(parts[2])
        val expected = Base64.getDecoder().decode(parts[3])
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, keyBits)
        val actual = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        spec.clearPassword()
        MessageDigest.isEqual(expected, actual)
    }.getOrDefault(false)
}
