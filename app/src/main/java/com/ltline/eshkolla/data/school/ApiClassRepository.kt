package com.ltline.eshkolla.data.school

import com.ltline.eshkolla.data.auth.ApiConfig
import com.ltline.eshkolla.data.auth.ApiSession
import com.ltline.eshkolla.domain.model.Student
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class TeacherClass(
    val id: String,
    val name: String,
    val gradeLevel: Int,
    val teacherId: String,
    val studentCount: Int
)

data class TeacherSubject(
    val id: String,
    val name: String,
    val code: String?,
    val classIds: List<String>
)

class ApiClassRepository {
    suspend fun getMyClasses(): List<TeacherClass> = parseClasses(request("/api/v1/classes"))

    suspend fun getStudents(classId: String): List<Student> = parseStudents(request("/api/v1/classes/$classId/students"))

    suspend fun getMySubjectsForClass(classId: String): List<TeacherSubject> {
        val root = JSONObject(request("/api/v1/me/data"))
        val subjects = root.optJSONArray("subjects") ?: return emptyList()
        return buildList {
            for (i in 0 until subjects.length()) {
                val item = subjects.getJSONObject(i)
                val classIds = item.optJSONArray("classIds")?.let { ids ->
                    buildList { for (j in 0 until ids.length()) add(ids.getString(j)) }
                }.orEmpty()
                if (classId in classIds) {
                    add(
                        TeacherSubject(
                            id = item.getString("id"),
                            name = item.getString("name"),
                            code = item.optString("code").takeIf { it.isNotBlank() },
                            classIds = classIds
                        )
                    )
                }
            }
        }
    }

    private fun request(path: String): String {
        val connection = (URL(ApiConfig.baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 15_000
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

    private fun parseClasses(raw: String): List<TeacherClass> {
        val array = JSONArray(raw)
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(TeacherClass(item.getString("id"), item.getString("name"), item.getInt("gradeLevel"), item.getString("teacherId"), item.getInt("studentCount")))
            }
        }
    }

    private fun parseStudents(raw: String): List<Student> {
        val array = JSONArray(raw)
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val parts = item.getString("fullName").trim().split(Regex("\\s+"), limit = 2)
                add(Student(item.getString("id"), "S1", item.getString("classId"), parts.firstOrNull().orEmpty(), parts.getOrNull(1).orEmpty(), item.optString("birthDate").takeIf { it.isNotBlank() }, isActive = item.optBoolean("isActive", true)))
            }
        }
    }
}
