package com.ltline.eshkolla.features.absences

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ltline.eshkolla.data.school.ApiStudentRepository
import com.ltline.eshkolla.domain.model.Absence
import com.ltline.eshkolla.domain.model.AbsenceStatus
import com.ltline.eshkolla.domain.model.Student
import com.ltline.eshkolla.presentation.teacher.TeacherAssessmentViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AbsenceScreen(onBack: () -> Unit, viewModel: TeacherAssessmentViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    var students by remember { mutableStateOf<List<Student>>(emptyList()) }
    var search by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Student?>(null) }
    var date by remember { mutableStateOf("2026-09-15") }
    var note by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(AbsenceStatus.E_PAAFTESUAR) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching { withContext(Dispatchers.IO) { ApiStudentRepository().getStudents() } }
            .onSuccess { students = it.filter(Student::isActive) }
            .onFailure { error = it.message ?: "Nxënësit nuk u ngarkuan." }
    }

    val filtered = students.filter { it.fullName.contains(search, true) || it.id.contains(search, true) }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = onBack) { Text("← Paneli") }
        Text("Regjistri i mungesave", style = MaterialTheme.typography.headlineMedium)
        Text("Mësimdhënës: Leonard Tahiraj • Matematikë")

        OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), label = { Text("Kërko nxënësin") }, singleLine = true)
        if (selected == null) {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filtered) { student ->
                    Card(Modifier.fillMaxWidth().clickable { selected = student }) {
                        Column(Modifier.padding(12.dp)) {
                            Text(student.fullName, style = MaterialTheme.typography.titleMedium)
                            Text("${student.id} • Klasa ${student.classId}")
                        }
                    }
                }
            }
        } else {
            Text("Nxënësi: ${selected!!.fullName}", style = MaterialTheme.typography.titleMedium)
            Text("ID: ${selected!!.id} • Klasa: ${selected!!.classId}")
            OutlinedButton(onClick = { selected = null }) { Text("Ndrysho nxënësin") }
            OutlinedTextField(date, { date = it }, Modifier.fillMaxWidth(), label = { Text("Data (YYYY-MM-DD)") }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { status = AbsenceStatus.E_PAAFTESUAR }) { Text("E paarsyeshme") }
                OutlinedButton(onClick = { status = AbsenceStatus.E_ARSYESHME }) { Text("E arsyeshme") }
            }
            OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), label = { Text("Arsyeja / shënim") }, singleLine = true)
            Button(onClick = {
                error = when {
                    date.length != 10 -> "Data duhet të jetë në formatin YYYY-MM-DD."
                    else -> null
                }
                if (error == null) viewModel.saveAbsence(Absence("TEMP", selected!!.id, "MAT", "M001", date, status, note.trim().takeIf { it.isNotBlank() }))
            }, Modifier.fillMaxWidth()) { Text("Ruaj mungesën") }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text("Mungesat e regjistruara", style = MaterialTheme.typography.titleLarge)
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(state.absences) { absence ->
                Card(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text("Nxënësi: ${absence.studentId}"); Text("${absence.date} • ${absence.note ?: "Mungesë"}") }
                    Text(if (absence.status == AbsenceStatus.E_ARSYESHME) "Arsyeshme" else "Paarsyeshme")
                } }
            }
        }
    }
}
