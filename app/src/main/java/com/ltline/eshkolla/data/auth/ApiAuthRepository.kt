package com.ltline.eshkolla.data.auth

import com.ltline.eshkolla.domain.auth.AuthRepository
import com.ltline.eshkolla.domain.model.User
import com.ltline.eshkolla.domain.model.UserRole
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class ApiAuthRepository(
    private val baseUrl: String = ApiConfig.BASE_URL
) : AuthRepository {
    private var token: String? = null

    override suspend fun login(username: String, password: String): Result<User> = runCatching {
        require(username.isNotBlank() && password.isNotBlank()) {
            "Plotësoni përdoruesin dhe fjalëkalimin."
        }

        val connection = openConnection("/api/v1/auth/login", "POST")
        connection.outputStream.use { output ->
            output.write(
                JSONObject()
                    .put("username", username.trim())
                    .put("password", password)
                    .toString()
                    .toByteArray(Charsets.UTF_8)
            )
        }

        val responseCode = connection.responseCode
        val response = readResponse(connection, responseCode)
        connection.disconnect()

        if (responseCode !in 200..299) {
            throw IOException(parseError(response))
        }

        val json = JSONObject(response)
        token = json.getString("token")
        parseUser(json.getJSONObject("user"))
    }

    override suspend fun logout() {
        val currentToken = token ?: return
        runCatching {
            val connection = openConnection("/api/v1/auth/logout", "POST")
            connection.setRequestProperty("Authorization", "Bearer $currentToken")
            connection.responseCode
            connection.disconnect()
        }
        token = null
    }

    private fun openConnection(path: String, method: String): HttpURLConnection {
        return (URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 15_000
            doInput = true
            doOutput = method == "POST"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json")
            token?.let { setRequestProperty("Authorization", "Bearer $it") }
        }
    }

    private fun readResponse(connection: HttpURLConnection, code: Int): String {
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        return stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
    }

    private fun parseError(body: String): String = runCatching {
        JSONObject(body).optString("message").ifBlank { "Gabim gjatë komunikimit me serverin." }
    }.getOrDefault("Gabim gjatë komunikimit me serverin.")

    private fun parseUser(json: JSONObject): User = User(
        id = json.getString("id"),
        username = json.getString("username"),
        fullName = json.getString("fullName"),
        role = UserRole.valueOf(json.getString("role")),
        isActive = json.optBoolean("isActive", true)
    )
}

object ApiConfig {
    const val BASE_URL = com.ltline.eshkolla.BuildConfig.API_BASE_URL
}
