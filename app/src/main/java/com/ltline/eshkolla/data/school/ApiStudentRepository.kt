package com.ltline.eshkolla.data.school

import com.ltline.eshkolla.data.auth.ApiConfig
import com.ltline.eshkolla.data.auth.ApiSession
import com.ltline.eshkolla.domain.model.Student
import com.ltline.eshkolla.domain.school.StudentRepository
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

class ApiStudentRepository : StudentRepository {
    override suspend fun getStudents(): List<Student> {
        val root = JSONObject(request("/api/v1/students?page=1&pageSize=100", "GET"))
        return parseStudents(root.getJSONArray("items"))
    }
    override suspend fun getStudentsByClass(classId: String): List<Student> = getStudents().filter { it.classId == classId }
    override suspend fun getStudent(id: String): Student? = runCatching { parseStudent(JSONObject(request("/api/v1/students/$id", "GET"))) }.getOrNull()
    override suspend fun addStudent(student: Student): Student = parseStudent(JSONObject(request("/api/v1/students", "POST", body(student))))
    override suspend fun updateStudent(student: Student): Student = parseStudent(JSONObject(request("/api/v1/students/${student.id}", "PUT", body(student))))
    override suspend fun setStudentActive(id: String, active: Boolean): Student? {
        val student = getStudent(id) ?: return null
        return updateStudent(student.copy(isActive = active))
    }
    private fun body(student: Student) = JSONObject().put("fullName", student.fullName).put("classId", student.classId).put("birthDate", student.birthDate.orEmpty()).put("isActive", student.isActive)
    private fun request(path: String, method: String, body: JSONObject? = null): String {
        val connection = (URL(ApiConfig.BASE_URL.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method; connectTimeout = 10_000; readTimeout = 15_000; doInput = true; doOutput = body != null
            setRequestProperty("Accept", "application/json"); setRequestProperty("Content-Type", "application/json")
            ApiSession.token?.let { setRequestProperty("Authorization", "Bearer $it") }
        }
        body?.let { value -> connection.outputStream.use { it.write(value.toString().toByteArray(Charsets.UTF_8)) } }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        connection.disconnect()
        if (code !in 200..299) throw IOException(runCatching { JSONObject(response).optString("message") }.getOrDefault("Gabim gjatë komunikimit me serverin."))
        return response
    }
    private fun parseStudents(array: JSONArray): List<Student> = buildList { for (i in 0 until array.length()) add(parseStudent(array.getJSONObject(i))) }
    private fun parseStudent(json: JSONObject): Student {
        val parts = json.getString("fullName").trim().split(Regex("\\s+"), limit = 2)
        return Student(json.getString("id"), "S1", json.getString("classId"), parts.firstOrNull().orEmpty(), parts.getOrNull(1).orEmpty(), json.optString("birthDate").takeIf { it.isNotBlank() }, isActive = json.optBoolean("isActive", true))
    }
}
