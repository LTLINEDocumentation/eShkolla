package com.ltline.eshkolla.data.auth

import com.ltline.eshkolla.domain.auth.AuthRepository
import com.ltline.eshkolla.domain.model.User
import com.ltline.eshkolla.domain.model.UserRole
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import org.json.JSONObject

object ApiSession {
    var token: String? = null
}

object ApiConfig {
    const val DEFAULT_BASE_URL = com.ltline.eshkolla.BuildConfig.API_BASE_URL
    var baseUrl: String = DEFAULT_BASE_URL
}

class ApiAuthRepository : AuthRepository {
    override suspend fun login(username: String, password: String): Result<User> = runCatching {
        require(username.isNotBlank() && password.isNotBlank()) { "Plotësoni përdoruesin dhe fjalëkalimin." }
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
        if (responseCode !in 200..299) throw IOException(parseError(response))
        val json = JSONObject(response)
        ApiSession.token = json.getString("token")
        parseUser(json.getJSONObject("user"))
    }.recoverCatching { error ->
        throw when (error) {
            is UnknownHostException, is ConnectException -> IOException(
                "Nuk mund të lidhemi me serverin. Kontrolloni Adresën e serverit dhe sigurohuni që eShkolla Backend është duke punuar."
            )
            is SocketTimeoutException -> IOException(
                "Serveri nuk u përgjigj në kohë. Kontrolloni lidhjen me internetin ose rrjetin lokal."
            )
            else -> error
        }
    }

    override suspend fun logout() {
        val currentToken = ApiSession.token ?: return
        runCatching {
            val connection = openConnection("/api/v1/auth/logout", "POST")
            connection.setRequestProperty("Authorization", "Bearer $currentToken")
            connection.responseCode
            connection.disconnect()
        }
        ApiSession.token = null
    }

    private fun openConnection(path: String, method: String): HttpURLConnection =
        (URL(ApiConfig.baseUrl.trim().trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 5_000
            readTimeout = 10_000
            doInput = true
            doOutput = method == "POST"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json")
            ApiSession.token?.let { setRequestProperty("Authorization", "Bearer $it") }
        }

    private fun readResponse(connection: HttpURLConnection, code: Int): String {
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        return stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
    }

    private fun parseError(body: String): String =
        runCatching {
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
