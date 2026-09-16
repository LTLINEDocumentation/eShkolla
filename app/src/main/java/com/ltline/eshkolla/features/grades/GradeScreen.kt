package com.ltline.eshkolla.features.grades

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ltline.eshkolla.data.school.ApiClassRepository
import com.ltline.eshkolla.data.school.TeacherClass
import com.ltline.eshkolla.domain.model.Grade
import com.ltline.eshkolla.domain.model.Student
import com.ltline.eshkolla.presentation.teacher.TeacherAssessmentViewModel

private data class AssessmentColumn(val key: String, val title: String)

private val assessmentColumns = listOf(
    AssessmentColumn("TEST_1", "Testi 1"),
    AssessmentColumn("TEST_2", "Testi 2"),
    AssessmentColumn("SEM_1", "Nota 1 e gjysmëvitit"),
    AssessmentColumn("TEST_3", "Testi 3"),
    AssessmentColumn("TEST_4", "Testi 4"),
    AssessmentColumn("SEM_2", "Nota 2 e gjysmëvitit"),
    AssessmentColumn("FINAL", "Nota Përfundimtare")
)

@Composable
fun GradeScreen(
    onBack: () -> Unit,
    studentId: String? = null,
    viewModel: TeacherAssessmentViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val classRepository = remember { ApiClassRepository() }
    var classes by remember { mutableStateOf<List<TeacherClass>>(emptyList()) }
    var selectedClass by remember { mutableStateOf<TeacherClass?>(null) }
    var classStudents by remember { mutableStateOf<List<Student>>(emptyList()) }
    var loadingClassData by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedCell by remember { mutableStateOf<Pair<Student, AssessmentColumn>?>(null) }
    val horizontalScroll = rememberScrollState()

    LaunchedEffect(Unit) {
        runCatching { classRepository.getMyClasses() }
            .onSuccess { loaded ->
                classes = loaded
                if (studentId != null) {
                    val student = state.students.firstOrNull { it.id == studentId }
                    selectedClass = loaded.firstOrNull { it.id == student?.classId }
                }
            }
            .onFailure { error = it.message ?: "Gabim gjatë ngarkimit të klasave." }
    }

    LaunchedEffect(selectedClass?.id) {
        val classId = selectedClass?.id ?: return@LaunchedEffect
        loadingClassData = true
        error = null
        runCatching { classRepository.getStudents(classId) }
            .onSuccess {
                classStudents = it
                    .filter(Student::isActive)
                    .sortedBy { student -> student.fullName.lowercase() }
            }
            .onFailure { error = it.message ?: "Gabim gjatë ngarkimit të nxënësve." }
        loadingClassData = false
    }

    val students = if (studentId != null) classStudents.filter { it.id == studentId } else classStudents

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(onClick = onBack) { Text("← Paneli") }
        Text("Notat", style = MaterialTheme.typography.headlineMedium)

        if (selectedClass == null) {
            Text(
                "Zgjidh klasën nga klasat që i ke në kompetencë.",
                style = MaterialTheme.typography.bodyLarge
            )
            Text("Klasat e mia", style = MaterialTheme.typography.titleLarge)

            if (classes.isEmpty() && !state.isLoading) {
                Text("Nuk ka klasa të caktuara për këtë mësimdhënës.")
            }

            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(classes) { schoolClass ->
                    Card(
                        Modifier.fillMaxWidth().clickable {
                            selectedClass = schoolClass
                            error = null
                        }
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(schoolClass.name, style = MaterialTheme.typography.titleLarge)
                            Text("Niveli: ${schoolClass.gradeLevel} • ${schoolClass.studentCount} nxënës")
                            Text(
                                "Hap regjistrin e notave →",
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }
        } else {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Klasa", style = MaterialTheme.typography.labelLarge)
                    Text(selectedClass!!.name, style = MaterialTheme.typography.titleLarge)
                    Text("Lista e nxënësve dhe tabela e vlerësimit")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { selectedClass = null; classStudents = emptyList() }) {
                    Text("← Klasat")
                }
                Text("${students.size} nxënës")
            }

            if (loadingClassData) {
                Text("Duke ngarkuar nxënësit…")
            } else if (students.isEmpty()) {
                Text("Nuk ka nxënës aktivë të regjistruar në këtë klasë.")
            } else {
                Card(Modifier.fillMaxWidth().weight(1f)) {
                    Row(Modifier.horizontalScroll(horizontalScroll)) {
                        Column(Modifier.width(210.dp)) {
                            TableHeader("Emri dhe Mbiemri", 210.dp)
                            students.forEach { student ->
                                TableNameCell(student.fullName)
                            }
                        }

                        assessmentColumns.forEach { column ->
                            Column(Modifier.width(145.dp)) {
                                TableHeader(column.title, 145.dp)
                                students.forEach { student ->
                                    val grade = gradeFor(state.grades, student.id, column.key)
                                    GradeCell(
                                        value = grade?.value,
                                        onClick = { selectedCell = student to column }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Text(
                "Kliko në qelizën e nxënësit për të vendosur notën 1–5.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }

    selectedCell?.let { (student, column) ->
        val current = gradeFor(state.grades, student.id, column.key)
        AlertDialog(
            onDismissRequest = { selectedCell = null },
            title = { Text("${column.title}\n${student.fullName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (current == null) "Zgjidh notën:" else "Nota aktuale: ${current.value}")
                    (1..5).forEach { value ->
                        Button(
                            onClick = {
                                val grade = Grade(
                                    id = current?.id ?: "TEMP-${student.id}-${column.key}-${System.currentTimeMillis()}",
                                    studentId = student.id,
                                    subjectId = current?.subjectId ?: "MAT",
                                    teacherId = current?.teacherId ?: selectedClass!!.teacherId,
                                    value = value,
                                    period = periodFor(column.key),
                                    academicYear = current?.academicYear ?: currentAcademicYear(),
                                    note = column.title
                                )
                                if (current == null) viewModel.saveGrade(grade) else viewModel.updateGrade(grade)
                                selectedCell = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(value.toString())
                        }
                    }
                    if (current != null) {
                        TextButton(
                            onClick = {
                                viewModel.deleteGrade(current.id)
                                selectedCell = null
                            }
                        ) { Text("Fshi notën") }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedCell = null }) { Text("Anulo") }
            }
        )
    }
}

@Composable
private fun TableHeader(title: String, width: androidx.compose.ui.unit.Dp) {
    Text(
        title,
        modifier = Modifier
            .width(width)
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(10.dp),
        style = MaterialTheme.typography.labelLarge
    )
}

@Composable
private fun TableNameCell(name: String) {
    Row(
        modifier = Modifier
            .width(210.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun GradeCell(value: Int?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .width(145.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(value?.toString() ?: "—", style = MaterialTheme.typography.titleMedium)
    }
}

private fun gradeFor(grades: List<Grade>, studentId: String, key: String): Grade? =
    grades.firstOrNull {
        it.studentId == studentId &&
            it.note == assessmentColumns.firstOrNull { column -> column.key == key }?.title
    }

private fun periodFor(key: String): String = when (key) {
    "SEM_1", "TEST_1", "TEST_2" -> "Periudha I"
    else -> "Periudha II"
}

private fun currentAcademicYear(): String {
    val now = java.time.LocalDate.now()
    val startYear = if (now.monthValue >= 9) now.year else now.year - 1
    return "$startYear/${startYear + 1}"
}
