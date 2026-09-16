package com.ltline.eshkolla.features.schedule

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ltline.eshkolla.data.school.ApiTimetableRepository
import com.ltline.eshkolla.data.school.ScheduleEntry
import com.ltline.eshkolla.data.school.TimetableData

@Composable
fun ScheduleScreen(onBack: () -> Unit) {
    val repository = remember { ApiTimetableRepository() }
    var timetable by remember { mutableStateOf<TimetableData?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scroll = rememberScrollState()

    LaunchedEffect(Unit) {
        loading = true
        runCatching { repository.getMyTimetable() }
            .onSuccess { timetable = it }
            .onFailure { error = it.message ?: "Gabim gjatë ngarkimit të orarit." }
        loading = false
    }

    val days = listOf("E Hënë", "E Martë", "E Mërkurë", "E Enjte", "E Premte")
    val periods = timetable?.settings?.lessonsPerDay ?: 6

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = onBack) { Text("← Paneli") }
        Text("Orari im", style = MaterialTheme.typography.headlineMedium)
        Text("Orari i mësimdhënësit merret nga llogaria aktive dhe serveri eShkolla.")

        if (loading) Text("Duke ngarkuar orarin…")
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        timetable?.let { data ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Orari i lidhur me llogarinë", style = MaterialTheme.typography.titleMedium)
                    Text("${data.entries.size} orë të caktuara")
                }
            }

            Card(Modifier.fillMaxWidth().weight(1f)) {
                Row(Modifier.horizontalScroll(scroll)) {
                    Column(Modifier.width(86.dp)) {
                        Header("Ora")
                        (1..periods).forEach { number ->
                            Text("Ora $number", Modifier.width(86.dp).padding(10.dp))
                        }
                    }
                    days.forEachIndexed { dayIndex, day ->
                        Column(Modifier.width(190.dp)) {
                            Header(day)
                            (1..periods).forEach { period ->
                                val entry = data.entries.firstOrNull { it.weekday == dayIndex + 1 && lessonNumber(it, data) == period }
                                LessonCell(entry)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(10.dp))
}

@Composable
private fun LessonCell(entry: ScheduleEntry?) {
    Card(Modifier.width(190.dp).padding(3.dp)) {
        Column(Modifier.padding(8.dp)) {
            if (entry == null) {
                Text("—", style = MaterialTheme.typography.bodySmall)
            } else {
                Text(entry.subjectId, style = MaterialTheme.typography.titleSmall)
                Text("Klasa: ${entry.classId}")
                Text("${entry.startTime} – ${entry.endTime}")
                entry.room?.let { Text("Dhoma: $it") }
            }
        }
    }
}

private fun lessonNumber(entry: ScheduleEntry, data: TimetableData): Int {
    return data.entries
        .filter { it.weekday == entry.weekday }
        .sortedBy { it.startTime }
        .indexOfFirst { it.id == entry.id } + 1
}
