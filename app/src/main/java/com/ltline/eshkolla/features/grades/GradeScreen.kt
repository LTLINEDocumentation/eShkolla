package com.ltline.eshkolla.features.grades

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ltline.eshkolla.domain.model.Grade
import com.ltline.eshkolla.domain.model.Student
import com.ltline.eshkolla.presentation.teacher.TeacherAssessmentViewModel

@Composable
fun GradeScreen(onBack: () -> Unit, studentId: String? = null, viewModel: TeacherAssessmentViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    var selectedStudent by remember(studentId, state.students) { mutableStateOf(state.students.firstOrNull { it.id == studentId }) }
    var value by remember { mutableStateOf("") }
    var period by remember { mutableStateOf("Periudha I") }
    var note by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val availableStudents = if (studentId != null) state.students.filter { it.id == studentId } else state.students
    val filteredStudents = availableStudents.filter { search.isBlank() || it.fullName.contains(search.trim(), true) || it.id.contains(search.trim(), true) }
    val visibleGrades = if (studentId != null) state.grades.filter { it.studentId == studentId } else state.grades

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = onBack) { Text("← Paneli") }
        Text("Regjistri i notave", style = MaterialTheme.typography.headlineMedium)
        Text("Mësimdhënës: Leonard Tahiraj • Matematikë")
        if (selectedStudent == null) {
            Text("Zgjidhni nxënësin", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), label = { Text("Kërko nxënësin") }, singleLine = true)
            LazyColumn(Modifier.fillMaxWidth().weight(1f, false), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filteredStudents) { student ->
                    Card(Modifier.fillMaxWidth().clickable { selectedStudent = student; error = null }) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column { Text(student.fullName, style = MaterialTheme.typography.titleMedium); Text("ID: ${student.id} • Klasa: ${student.classId}") }
                            Text("Zgjidh")
                        }
                    }
                }
            }
        } else {
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                Text("Nxënësi", style = MaterialTheme.typography.labelLarge)
                Text(selectedStudent!!.fullName, style = MaterialTheme.typography.titleLarge)
                Text("ID: ${selectedStudent!!.id} • Klasa: ${selectedStudent!!.classId}")
            } }
            Text("Regjistroni notën", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value, { value = it.filter(Char::isDigit).take(1) }, Modifier.fillMaxWidth(), label = { Text("Nota 1–5") }, singleLine = true)
            OutlinedTextField(period, { period = it }, Modifier.fillMaxWidth(), label = { Text("Periudha") }, singleLine = true)
            OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), label = { Text("Shënim / lloji i vlerësimit") }, singleLine = true)
            Button(onClick = {
                val grade = value.toIntOrNull()
                error = when { grade !in 1..5 -> "Nota duhet të jetë nga 1 deri në 5."; period.isBlank() -> "Periudha është e detyrueshme."; else -> null }
                if (error == null) {
                    viewModel.saveGrade(Grade("TEMP-${selectedStudent!!.id}-${System.currentTimeMillis()}", selectedStudent!!.id, "MAT", "M001", grade!!, period.trim(), "2026/2027", note.trim().takeIf { it.isNotBlank() }))
                    value = ""; note = ""
                }
            }, Modifier.fillMaxWidth()) { Text("Ruaj notën") }
            if (studentId == null) OutlinedButton(onClick = { selectedStudent = null }, Modifier.fillMaxWidth()) { Text("Ndrysho nxënësin") }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (state.isLoading) Text("Duke ngarkuar të dhënat…")
        Text("Notat e regjistruara", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(visibleGrades) { grade ->
                Card(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text("${grade.period} • ${grade.note ?: "Vlerësim"}"); Text("Viti: ${grade.academicYear}") }
                    Text(grade.value.toString(), style = MaterialTheme.typography.headlineSmall)
                } }
            }
        }
    }
}
