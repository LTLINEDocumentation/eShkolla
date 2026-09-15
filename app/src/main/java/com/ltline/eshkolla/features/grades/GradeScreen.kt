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
fun GradeScreen(onBack: () -> Unit, viewModel: TeacherAssessmentViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    var selectedStudent by remember { mutableStateOf<Student?>(null) }
    var value by remember { mutableStateOf("") }
    var period by remember { mutableStateOf("Periudha I") }
    var note by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val filteredStudents = state.students.filter {
        search.isBlank() || it.fullName.contains(search.trim(), ignoreCase = true) || it.id.contains(search.trim(), ignoreCase = true)
    }

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(onClick = onBack) { Text("← Paneli") }
        Text("Regjistri i notave", style = MaterialTheme.typography.headlineMedium)
        Text("Mësimdhënës: Leonard Tahiraj • Matematikë")

        Text("1. Zgjidhni nxënësin", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Kërko nxënësin") },
            singleLine = true
        )

        if (selectedStudent == null) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredStudents) { student ->
                    Card(
                        Modifier.fillMaxWidth().clickable {
                            selectedStudent = student
                            error = null
                        }
                    ) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(student.fullName, style = MaterialTheme.typography.titleMedium)
                                Text("ID: ${student.id} • Klasa: ${student.classId}")
                            }
                            Text("Zgjidh")
                        }
                    }
                }
            }
            if (!state.isLoading && filteredStudents.isEmpty()) {
                Text("Nuk u gjet asnjë nxënës aktiv.", color = MaterialTheme.colorScheme.error)
            }
        } else {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Nxënësi i zgjedhur", style = MaterialTheme.typography.labelLarge)
                    Text(selectedStudent!!.fullName, style = MaterialTheme.typography.titleLarge)
                    Text("ID: ${selectedStudent!!.id} • Klasa: ${selectedStudent!!.classId}")
                }
            }

            Text("2. Regjistroni notën", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = value,
                onValueChange = { value = it.filter(Char::isDigit).take(1) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nota 1–5") },
                singleLine = true
            )
            OutlinedTextField(
                value = period,
                onValueChange = { period = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Periudha") },
                singleLine = true
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Shënim / lloji i vlerësimit") },
                singleLine = true
            )

            Button(onClick = {
                val grade = value.toIntOrNull()
                error = when {
                    grade !in 1..5 -> "Nota duhet të jetë nga 1 deri në 5."
                    period.isBlank() -> "Periudha është e detyrueshme."
                    else -> null
                }
                if (error == null) {
                    viewModel.saveGrade(
                        Grade(
                            "TEMP-${selectedStudent!!.id}-${System.currentTimeMillis()}",
                            selectedStudent!!.id,
                            "MAT",
                            "M001",
                            grade!!,
                            period.trim(),
                            "2026/2027",
                            note.trim().takeIf { it.isNotBlank() }
                        )
                    )
                    value = ""
                    note = ""
                }
            }, Modifier.fillMaxWidth()) { Text("Ruaj notën") }

            OutlinedButton(onClick = { selectedStudent = null }, Modifier.fillMaxWidth()) {
                Text("Ndrysho nxënësin")
            }
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (state.isLoading) Text("Duke ngarkuar të dhënat…")

        Text("Notat e regjistruara", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.grades) { grade ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Nxënësi: ${grade.studentId}")
                            Text("${grade.period} • ${grade.note ?: "Vlerësim"}")
                        }
                        Text(grade.value.toString(), style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
        }
    }
}
