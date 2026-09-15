package com.ltline.eshkolla.features.absences

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ltline.eshkolla.domain.model.Absence
import com.ltline.eshkolla.domain.model.AbsenceStatus
import com.ltline.eshkolla.presentation.teacher.TeacherAssessmentViewModel

@Composable
fun AbsenceScreen(onBack: () -> Unit, studentId: String? = null, viewModel: TeacherAssessmentViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val fixedStudent = state.students.firstOrNull { it.id == studentId }
    var selected by remember(studentId, state.students) { mutableStateOf(fixedStudent) }
    var search by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("2026-09-15") }
    var note by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(AbsenceStatus.E_PAAFTESUAR) }
    var error by remember { mutableStateOf<String?>(null) }
    var editingAbsence by remember { mutableStateOf<Absence?>(null) }
    var deletingAbsence by remember { mutableStateOf<Absence?>(null) }

    val availableStudents = if (studentId != null) state.students.filter { it.id == studentId } else state.students
    val filtered = availableStudents.filter { it.fullName.contains(search, true) || it.id.contains(search, true) }
    val visibleAbsences = if (studentId != null) state.absences.filter { it.studentId == studentId } else state.absences

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = onBack) { Text("← Paneli") }
        Text("Regjistri i mungesave", style = MaterialTheme.typography.headlineMedium)
        Text("Mësimdhënës: Leonard Tahiraj • Matematikë")
        if (selected == null) {
            OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), label = { Text("Kërko nxënësin") }, singleLine = true)
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filtered) { student -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                    Text(student.fullName, style = MaterialTheme.typography.titleMedium)
                    Text("${student.id} • Klasa ${student.classId}")
                    Button(onClick = { selected = student }) { Text("Zgjidh") }
                } } }
            }
        } else {
            Text("Nxënësi: ${selected!!.fullName}", style = MaterialTheme.typography.titleMedium)
            Text("ID: ${selected!!.id} • Klasa: ${selected!!.classId}")
            if (studentId == null) OutlinedButton(onClick = { selected = null }) { Text("Ndrysho nxënësin") }
            OutlinedTextField(date, { date = it }, Modifier.fillMaxWidth(), label = { Text("Data (YYYY-MM-DD)") }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { status = AbsenceStatus.E_PAAFTESUAR }) { Text("E paarsyeshme") }
                OutlinedButton(onClick = { status = AbsenceStatus.E_ARSYESHME }) { Text("E arsyeshme") }
            }
            OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), label = { Text("Arsyeja / shënim") }, singleLine = true)
            Button(onClick = {
                error = if (date.length == 10) null else "Data duhet të jetë në formatin YYYY-MM-DD."
                if (error == null) {
                    viewModel.saveAbsence(Absence("TEMP-${selected!!.id}-${System.currentTimeMillis()}", selected!!.id, "MAT", "M001", date, status, note.trim().takeIf { it.isNotBlank() }))
                    note = ""
                }
            }, Modifier.fillMaxWidth()) { Text("Ruaj mungesën") }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (state.isLoading) Text("Duke ngarkuar…")
        Text("Mungesat e regjistruara", style = MaterialTheme.typography.titleLarge)
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(visibleAbsences) { absence ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column { Text(absence.date); Text(absence.note ?: "Mungesë") }
                            Text(if (absence.status == AbsenceStatus.E_ARSYESHME) "Arsyeshme" else "Paarsyeshme")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { editingAbsence = absence }) { Text("Ndrysho") }
                            TextButton(onClick = { deletingAbsence = absence }) { Text("Fshi") }
                        }
                    }
                }
            }
        }
    }

    editingAbsence?.let { absence ->
        var editDate by remember(absence.id) { mutableStateOf(absence.date) }
        var editNote by remember(absence.id) { mutableStateOf(absence.note.orEmpty()) }
        var editStatus by remember(absence.id) { mutableStateOf(absence.status) }
        AlertDialog(
            onDismissRequest = { editingAbsence = null },
            title = { Text("Ndrysho mungesën") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(editDate, { editDate = it }, label = { Text("Data (YYYY-MM-DD)") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { editStatus = AbsenceStatus.E_PAAFTESUAR }) { Text("Paarsyeshme") }
                        OutlinedButton(onClick = { editStatus = AbsenceStatus.E_ARSYESHME }) { Text("Arsyeshme") }
                    }
                    OutlinedTextField(editNote, { editNote = it }, label = { Text("Shënim") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                        viewModel.updateAbsence(absence.copy(date = editDate, status = editStatus, note = editNote.trim().takeIf { it.isNotBlank() }))
                        editingAbsence = null
                    }
                }) { Text("Ruaj") }
            },
            dismissButton = { TextButton(onClick = { editingAbsence = null }) { Text("Anulo") } }
        )
    }

    deletingAbsence?.let { absence ->
        AlertDialog(
            onDismissRequest = { deletingAbsence = null },
            title = { Text("Fshi mungesën?") },
            text = { Text("Kjo mungesë do të hiqet nga regjistri i nxënësit.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteAbsence(absence.id); deletingAbsence = null }) { Text("Fshi") }
            },
            dismissButton = { TextButton(onClick = { deletingAbsence = null }) { Text("Anulo") } }
        )
    }
}
