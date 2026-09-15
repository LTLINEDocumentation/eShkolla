package com.ltline.eshkolla.data.management

import com.ltline.eshkolla.data.auth.ApiConfig
import com.ltline.eshkolla.data.auth.ApiSession
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

data class ManagementTeacher(val id: String, val fullName: String, val subjectId: String, val username: String, val active: Boolean, val classIds: List<String>)
data class ManagementClass(val id: String, val name: String, val gradeLevel: Int, val active: Boolean, val studentCount: Int, val teacherIds: List<String>)
data class ManagementUser(val id: String, val username: String, val fullName: String, val role: String, val active: Boolean)

class ApiManagementRepository {
    suspend fun getTeachers(): List<ManagementTeacher> = parseTeachers(JSONArray(request("/api/v1/management/teachers")))
    suspend fun getClasses(): List<ManagementClass> = parseClasses(JSONArray(request("/api/v1/management/classes")))
    suspend fun getUsers(): List<ManagementUser> = parseUsers(JSONArray(request("/api/v1/management/users")))

    private fun request(path: String): String {
        val connection = (URL(ApiConfig.BASE_URL.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 10_000; readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
            ApiSession.token?.let { setRequestProperty("Authorization", "Bearer $it") }
        }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        connection.disconnect()
        if (code !in 200..299) throw IOException(runCatching { JSONObject(response).optString("message") }.getOrDefault("Gabim gjatë komunikimit me serverin."))
        return response
    }
    private fun parseTeachers(a: JSONArray) = buildList { for (i in 0 until a.length()) { val j=a.getJSONObject(i); add(ManagementTeacher(j.getString("id"),j.getString("fullName"),j.getString("subjectId"),j.getString("username"),j.getBoolean("active"),strings(j.optJSONArray("classIds")))) } }
    private fun parseClasses(a: JSONArray) = buildList { for (i in 0 until a.length()) { val j=a.getJSONObject(i); add(ManagementClass(j.getString("id"),j.getString("name"),j.getInt("gradeLevel"),j.getBoolean("active"),j.getInt("studentCount"),strings(j.optJSONArray("teacherIds")))) } }
    private fun parseUsers(a: JSONArray) = buildList { for (i in 0 until a.length()) { val j=a.getJSONObject(i); add(ManagementUser(j.getString("id"),j.getString("username"),j.getString("fullName"),j.getString("role"),j.getBoolean("active"))) } }
    private fun strings(a: JSONArray?): List<String> = if (a == null) emptyList() else buildList { for (i in 0 until a.length()) add(a.getString(i)) }
}
