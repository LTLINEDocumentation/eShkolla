package com.ltline.eshkolla.features.students

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ltline.eshkolla.domain.model.Student

@Composable
fun StudentScreen(viewModel: StudentViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    var search by remember { mutableStateOf("") }
    var classFilter by remember { mutableStateOf("Të gjitha") }
    var editing by remember { mutableStateOf<Student?>(null) }
    var showForm by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("← Paneli") }
            Button(onClick = { editing = null; showForm = true }) { Text("+ Shto") }
        }
        Text("Nxënësit", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 16.dp))
        OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), label = { Text("Kërko emër ose ID") }, singleLine = true)
        if (state is StudentListState.Success) {
            val students = (state as StudentListState.Success).students
            val classes = listOf("Të gjitha") + students.map { it.classId }.distinct().sorted()
            Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                classes.take(4).forEach { c -> OutlinedButton(onClick = { classFilter = c }) { Text(c) } }
            }
            val filtered = students.filter { s ->
                val q = search.trim().lowercase()
                (q.isBlank() || s.fullName.lowercase().contains(q) || s.id.lowercase().contains(q)) &&
                    (classFilter == "Të gjitha" || s.classId == classFilter)
            }.sortedBy { it.fullName.lowercase() }
            Text("${filtered.size} ${if (filtered.size == 1) "rezultat" else "rezultate"}", style = MaterialTheme.typography.labelMedium)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                items(filtered, key = { it.id }) { student ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(student.fullName, style = MaterialTheme.typography.titleLarge)
                            Text("ID: ${student.id} • Klasa: ${student.classId}")
                            Text("Datëlindja: ${student.birthDate ?: "—"}")
                            Text("Statusi: ${if (student.isActive) "Aktiv" else "Joaktiv"}")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { editing = student; showForm = true }) { Text("Ndrysho") }
                                TextButton(onClick = { viewModel.toggleActive(student) }) { Text(if (student.isActive) "Çaktivizo" else "Aktivizo") }
                            }
                        }
                    }
                }
            }
        } else if (state is StudentListState.Error) {
            Text((state as StudentListState.Error).message, color = MaterialTheme.colorScheme.error)
        } else {
            Text("Duke ngarkuar…")
        }
    }

    if (showForm) StudentFormDialog(editing, { showForm = false }) { student -> viewModel.saveStudent(student); showForm = false }
}

@Composable
private fun StudentFormDialog(initial: Student?, onDismiss: () -> Unit, onSave: (Student) -> Unit) {
    var name by remember { mutableStateOf(initial?.fullName ?: "") }
    var classId by remember { mutableStateOf(initial?.classId ?: "C1") }
    var birthDate by remember { mutableStateOf(initial?.birthDate ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Shto nxënës" else "Ndrysho nxënës") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Emri i plotë") }, singleLine = true)
            OutlinedTextField(classId, { classId = it }, label = { Text("ID e klasës") }, singleLine = true)
            OutlinedTextField(birthDate, { birthDate = it }, label = { Text("Datëlindja") }, singleLine = true)
        } },
        confirmButton = { Button(onClick = {
            val parts = name.trim().split(" ", limit = 2)
            if (parts.size == 2) onSave(Student(initial?.id ?: "NX${System.currentTimeMillis() % 100000}", initial?.schoolId ?: "S1", classId.trim(), parts[0], parts[1], birthDate.trim(), initial?.parentUserId, initial?.isActive ?: true))
        }) { Text("Ruaj") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anulo") } }
    )
}
