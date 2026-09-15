package com.ltline.eshkolla.features.grades

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
import com.ltline.eshkolla.presentation.teacher.TeacherAssessmentViewModel

@Composable
fun GradeScreen(onBack: () -> Unit, viewModel: TeacherAssessmentViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    var studentId by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var period by remember { mutableStateOf("Periudha I") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = onBack) { Text("← Paneli") }
        Text("Regjistri i notave", style = MaterialTheme.typography.headlineMedium)
        Text("Mësimdhënës: Leonard Tahiraj • Matematikë")

        OutlinedTextField(studentId, { studentId = it }, Modifier.fillMaxWidth(), label = { Text("ID e nxënësit (p.sh. NX001)") }, singleLine = true)
        OutlinedTextField(value, { value = it.filter(Char::isDigit).take(1) }, Modifier.fillMaxWidth(), label = { Text("Nota 1–5") }, singleLine = true)
        OutlinedTextField(period, { period = it }, Modifier.fillMaxWidth(), label = { Text("Periudha") }, singleLine = true)
        OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), label = { Text("Shënim / lloji i vlerësimit") }, singleLine = true)

        Button(onClick = {
            val grade = value.toIntOrNull()
            error = when {
                studentId.isBlank() -> "Zgjidhni ose shkruani ID-në e nxënësit."
                grade !in 1..5 -> "Nota duhet të jetë nga 1 deri në 5."
                period.isBlank() -> "Periudha është e detyrueshme."
                else -> null
            }
            if (error == null) viewModel.saveGrade(Grade("TEMP", studentId.trim(), "MAT", "M001", grade!!, period.trim(), "2026/2027", note.trim().takeIf { it.isNotBlank() }))
        }, Modifier.fillMaxWidth()) { Text("Ruaj notën") }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (state.isLoading) Text("Duke ngarkuar…")

        Text("Notat e regjistruara", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.grades) { grade ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("Nxënësi: ${grade.studentId}"); Text("${grade.period} • ${grade.note ?: "Vlerësim"}") }
                        Text(grade.value.toString(), style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
        }
    }
}
