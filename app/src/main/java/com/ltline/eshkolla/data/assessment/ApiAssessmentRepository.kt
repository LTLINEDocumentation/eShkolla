package com.ltline.eshkolla.data.assessment

import com.ltline.eshkolla.data.auth.ApiConfig
import com.ltline.eshkolla.data.auth.ApiSession
import com.ltline.eshkolla.domain.model.Absence
import com.ltline.eshkolla.domain.model.AbsenceStatus
import com.ltline.eshkolla.domain.model.Grade
import com.ltline.eshkolla.domain.repository.AbsenceRepository
import com.ltline.eshkolla.domain.repository.GradeRepository
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

class ApiGradeRepository : GradeRepository {
    override suspend fun getGrades(): List<Grade> = parseGrades(request("/api/v1/grades"))
    override suspend fun getGradesByStudent(studentId: String): List<Grade> = parseGrades(request("/api/v1/grades?studentId=${encode(studentId)}"))
    override suspend fun addGrade(grade: Grade): Grade = parseGrade(request("/api/v1/grades", "POST", gradeBody(grade)))
    override suspend fun updateGrade(grade: Grade): Grade = throw UnsupportedOperationException("Përditësimi i notës do të aktivizohet me endpoint-in e dedikuar.")
    override suspend fun deleteGrade(id: String) = throw UnsupportedOperationException("Fshirja e notës do të aktivizohet me endpoint-in e dedikuar.")

    private fun gradeBody(g: Grade) = JSONObject().put("studentId", g.studentId).put("subjectId", g.subjectId).put("teacherId", g.teacherId).put("value", g.value).put("period", g.period).put("academicYear", g.academicYear).put("note", g.note)
}

class ApiAbsenceRepository : AbsenceRepository {
    override suspend fun getAbsences(): List<Absence> = parseAbsences(request("/api/v1/absences"))
    override suspend fun getAbsencesByStudent(studentId: String): List<Absence> = parseAbsences(request("/api/v1/absences?studentId=${encode(studentId)}"))
    override suspend fun addAbsence(absence: Absence): Absence = parseAbsence(request("/api/v1/absences", "POST", absenceBody(absence)))
    override suspend fun updateAbsence(absence: Absence): Absence = throw UnsupportedOperationException("Përditësimi i mungesës do të aktivizohet me endpoint-in e dedikuar.")
    override suspend fun deleteAbsence(id: String) = throw UnsupportedOperationException("Fshirja e mungesës do të aktivizohet me endpoint-in e dedikuar.")

    private fun absenceBody(a: Absence) = JSONObject().put("studentId", a.studentId).put("subjectId", a.subjectId).put("teacherId", a.teacherId).put("date", a.date).put("status", a.status.name).put("note", a.note)
}

private fun request(path: String, method: String = "GET", body: JSONObject? = null): String {
    val connection = (URL(ApiConfig.baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
        requestMethod = method
        connectTimeout = 10_000
        readTimeout = 15_000
        doInput = true
        doOutput = body != null
        setRequestProperty("Accept", "application/json")
        setRequestProperty("Content-Type", "application/json")
        ApiSession.token?.let { setRequestProperty("Authorization", "Bearer $it") }
    }
    body?.let { connection.outputStream.use { output -> output.write(it.toString().toByteArray(Charsets.UTF_8)) } }
    val code = connection.responseCode
    val stream = if (code in 200..299) connection.inputStream else connection.errorStream
    val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
    connection.disconnect()
    if (code !in 200..299) throw IOException(runCatching { JSONObject(response).optString("message") }.getOrDefault("Gabim gjatë komunikimit me serverin."))
    return response
}

private fun parseGrades(response: String): List<Grade> {
    val array = JSONArray(response)
    return buildList { for (i in 0 until array.length()) add(parseGrade(array.getJSONObject(i).toString())) }
}
private fun parseGrade(response: String): Grade {
    val j = JSONObject(response)
    return Grade(j.getString("id"), j.getString("studentId"), j.getString("subjectId"), j.getString("teacherId"), j.getInt("value"), j.getString("period"), j.getString("academicYear"), j.optString("note").takeIf { it.isNotBlank() })
}
private fun parseAbsences(response: String): List<Absence> {
    val array = JSONArray(response)
    return buildList { for (i in 0 until array.length()) add(parseAbsence(array.getJSONObject(i).toString())) }
}
private fun parseAbsence(response: String): Absence {
    val j = JSONObject(response)
    return Absence(j.getString("id"), j.getString("studentId"), j.getString("subjectId"), j.getString("teacherId"), j.getString("date"), AbsenceStatus.valueOf(j.getString("status")), j.optString("note").takeIf { it.isNotBlank() })
}
private fun encode(value: String): String = java.net.URLEncoder.encode(value, Charsets.UTF_8.name())
