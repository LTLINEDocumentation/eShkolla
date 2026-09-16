package com.ltline.eshkolla.data.school

import com.ltline.eshkolla.data.auth.ApiConfig
import com.ltline.eshkolla.data.auth.ApiSession
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class TimetableSettings(
    val lessonsPerDay: Int,
    val firstLessonStart: String,
    val lessonDurationMinutes: Int
)

data class ScheduleEntry(
    val id: String,
    val classId: String,
    val teacherId: String?,
    val subjectId: String,
    val weekday: Int,
    val startTime: String,
    val endTime: String,
    val room: String?
)

data class TimetableData(
    val settings: TimetableSettings,
    val entries: List<ScheduleEntry>
)

class ApiTimetableRepository {
    suspend fun getMyTimetable(): TimetableData {
        val raw = request("/api/v1/timetable")
        val root = JSONObject(raw)
        val settings = root.getJSONObject("settings")
        val periods = root.optJSONArray("periods")
        val lessonsPerDay = settings.optInt("lessonsPerDay", periods?.length() ?: 6)
        val entries = buildList {
            val array = root.optJSONArray("schedule") ?: return@buildList
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(ScheduleEntry(
                    id = item.getString("id"),
                    classId = item.getString("classId"),
                    teacherId = item.optString("teacherId").takeIf { it.isNotBlank() },
                    subjectId = item.getString("subjectId"),
                    weekday = item.getInt("weekday"),
                    startTime = item.getString("startTime"),
                    endTime = item.getString("endTime"),
                    room = item.optString("room").takeIf { it.isNotBlank() }
                ))
            }
        }
        return TimetableData(
            TimetableSettings(lessonsPerDay, settings.optString("firstLessonStart", "08:00"), settings.optInt("lessonDurationMinutes", 45)),
            entries
        )
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
        if (code !in 200..299) throw IOException(runCatching { JSONObject(response).optString("message") }.getOrDefault("Gabim gjatë ngarkimit të orarit."))
        return response
    }
}
