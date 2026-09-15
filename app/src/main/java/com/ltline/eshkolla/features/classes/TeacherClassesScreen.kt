package com.ltline.eshkolla.features.classes

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ltline.eshkolla.data.school.ApiClassRepository
import com.ltline.eshkolla.data.school.TeacherClass
import com.ltline.eshkolla.domain.model.Student

@Composable
fun TeacherClassesScreen(
    onBack: () -> Unit,
    onGrades: () -> Unit,
    onAbsences: () -> Unit
) {
    val repository = remember { ApiClassRepository() }
    var classes by remember { mutableStateOf<List<TeacherClass>>(emptyList()) }
    var selectedClass by remember { mutableStateOf<TeacherClass?>(null) }
    var students by remember { mutableStateOf<List<Student>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        runCatching { repository.getMyClasses() }
            .onSuccess { classes = it }
            .onFailure { error = it.message ?: "Gabim gjatë ngarkimit të klasave." }
        loading = false
    }

    LaunchedEffect(selectedClass?.id) {
        val classId = selectedClass?.id ?: return@LaunchedEffect
        loading = true
        runCatching { repository.getStudents(classId) }
            .onSuccess { students = it }
            .onFailure { error = it.message ?: "Gabim gjatë ngarkimit të nxënësve." }
        loading = false
    }

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(onClick = onBack) { Text("← Paneli") }
        Text("Klasat e mia", style = MaterialTheme.typography.headlineMedium)
        Text("Mësimdhënës: Leonard Tahiraj • Matematikë")

        if (selectedClass == null) {
            Text("Zgjidhni klasën", style = MaterialTheme.typography.titleMedium)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(classes) { schoolClass ->
                    Card(Modifier.fillMaxWidth().clickable { selectedClass = schoolClass; error = null }) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(schoolClass.name, style = MaterialTheme.typography.titleLarge)
                                Text("Niveli: ${schoolClass.gradeLevel}")
                            }
                            Text("${schoolClass.studentCount} nxënës")
                        }
                    }
                }
            }
        } else {
            Text(selectedClass!!.name, style = MaterialTheme.typography.titleLarge)
            Text("Nxënësit e klasës", style = MaterialTheme.typography.titleMedium)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(students) { student ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(student.fullName, style = MaterialTheme.typography.titleMedium)
                            Text("ID: ${student.id}")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = onGrades) { Text("Notë") }
                                OutlinedButton(onClick = onAbsences) { Text("Mungesë") }
                            }
                        }
                    }
                }
            }
            OutlinedButton(onClick = { selectedClass = null; students = emptyList() }, Modifier.fillMaxWidth()) {
                Text("← Kthehu te klasat")
            }
        }

        if (loading) Text("Duke ngarkuar…")
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (!loading && error == null && classes.isEmpty() && selectedClass == null) {
            Text("Nuk ka klasa të caktuara për këtë mësimdhënës.")
        }
    }
}
