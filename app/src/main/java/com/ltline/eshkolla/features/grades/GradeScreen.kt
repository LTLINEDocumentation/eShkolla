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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ltline.eshkolla.data.school.ApiClassRepository
import com.ltline.eshkolla.data.school.TeacherClass
import com.ltline.eshkolla.data.school.TeacherSubject
import com.ltline.eshkolla.domain.model.Grade
import com.ltline.eshkolla.domain.model.Student
import com.ltline.eshkolla.presentation.teacher.TeacherAssessmentViewModel
import java.util.Calendar

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
    var subjects by remember { mutableStateOf<List<TeacherSubject>>(emptyList()) }
    var selectedSubjectId by remember { mutableStateOf<String?>(null) }
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
        val schoolClass = selectedClass ?: return@LaunchedEffect
        loadingClassData = true
        error = null
        runCatching {
            val students = classRepository.getStudents(schoolClass.id)
            val classSubjects = classRepository.getMySubjectsForClass(schoolClass.id)
            students to classSubjects
        }.onSuccess { (students, classSubjects) ->
            classStudents = students.filter(Student::isActive).sortedBy { it.fullName.lowercase() }
            subjects = classSubjects
            selectedSubjectId = classSubjects.firstOrNull()?.id
            viewModel.loadGradesForClass(schoolClass.id)
        }.onFailure { error = it.message ?: "Gabim gjatë ngarkimit të të dhënave të klasës." }
        loadingClassData = false
    }

    val selectedSubject = subjects.firstOrNull { it.id == selectedSubjectId }
    val students = if (studentId != null) classStudents.filter { it.id == studentId } else classStudents
    val canEnterGrades = selectedClass != null && selectedSubject != null

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(onClick = onBack) { Text("← Paneli") }
        Text("Notat", style = MaterialTheme.typography.headlineMedium)

        if (selectedClass == null) {
            Text("Zgjidh klasën nga klasat që i ke në kompetencë.", style = MaterialTheme.typography.bodyLarge)
            Text("Klasat e mia", style = MaterialTheme.typography.titleLarge)

            if (classes.isEmpty() && !state.isLoading) {
                Text("Nuk ka klasa të caktuara për këtë mësimdhënës.")
            }

            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(classes, key = { it.id }) { schoolClass ->
                    Card(
                        Modifier.fillMaxWidth().clickable {
                            selectedClass = schoolClass
                            error = null
                        }
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(schoolClass.name, style = MaterialTheme.typography.titleLarge)
                            Text("Niveli: ${schoolClass.gradeLevel} • ${schoolClass.studentCount} nxënës")
                            Text("Hap regjistrin e notave →", modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            }
        } else {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Klasa", style = MaterialTheme.typography.labelLarge)
                    Text(selectedClass!!.name, style = MaterialTheme.typography.titleLarge)
                    Text("Lista e nxënësve dhe tabela e vlerësimit")
                    if (subjects.isNotEmpty()) {
                        Text("Lënda", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            subjects.forEach { subject ->
                                if (subject.id == selectedSubjectId) {
                                    Button(onClick = { selectedSubjectId = subject.id }) { Text(subject.name) }
                                } else {
                                    OutlinedButton(onClick = { selectedSubjectId = subject.id }) { Text(subject.name) }
                                }
                            }
                        }
                    } else if (!loadingClassData) {
                        Text("Nuk u gjet lëndë e kësaj klase në kompetencat e mësimdhënësit.", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = {
                    selectedCell = null
                    selectedClass = null
                    selectedSubjectId = null
                    subjects = emptyList()
                    classStudents = emptyList()
                }) { Text("← Klasat") }
                Text("${students.size} nxënës")
                selectedSubject?.let { Text("• ${it.name}") }
            }

            if (loadingClassData || state.isLoading) {
                Text("Duke ngarkuar nxënësit dhe notat…")
            } else if (students.isEmpty()) {
                Text("Nuk ka nxënës aktivë të regjistruar në këtë klasë.")
            } else if (!canEnterGrades) {
                Text("Për këtë klasë nuk ka lëndë të lidhur me kompetencat e mësimdhënësit.", color = MaterialTheme.colorScheme.error)
            } else {
                Card(Modifier.fillMaxWidth().weight(1f)) {
                    Row(Modifier.horizontalScroll(horizontalScroll)) {
                        Column(Modifier.width(210.dp)) {
                            TableHeader("Emri dhe Mbiemri", 210.dp)
                            students.forEach { student -> TableNameCell(student.fullName) }
                        }

                        assessmentColumns.forEach { column ->
                            Column(Modifier.width(145.dp)) {
                                TableHeader(column.title, 145.dp)
                                students.forEach { student ->
                                    val grade = gradeFor(state.grades, student.id, selectedSubjectId!!, column.key)
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
                "Renditja: Testi 1 → Testi 2 → Nota 1 e gjysmëvitit → Testi 3 → Testi 4 → Nota 2 e gjysmëvitit → Nota Përfundimtare.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }

    selectedCell?.let { (student, column) ->
        val subjectId = selectedSubjectId
        val schoolClass = selectedClass
        if (subjectId != null && schoolClass != null) {
            val current = gradeFor(state.grades, student.id, subjectId, column.key)
            val studentIndex = students.indexOfFirst { it.id == student.id }
            val columnIndex = assessmentColumns.indexOfFirst { it.key == column.key }

            fun nextCell(): Pair<Student, AssessmentColumn>? {
                if (studentIndex < 0 || columnIndex < 0) return null
                val nextStudent = students.getOrNull(studentIndex + 1)
                if (nextStudent != null) return nextStudent to column
                val nextColumn = assessmentColumns.getOrNull(columnIndex + 1)
                if (nextColumn != null) return students.firstOrNull()?.let { it to nextColumn }
                return null
            }

            AlertDialog(
                onDismissRequest = { selectedCell = null },
                title = { Text("${column.title}\n${student.fullName}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Zgjidh notën:")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            (1..5).forEach { value ->
                                Button(
                                    onClick = {
                                        val grade = Grade(
                                            id = current?.id ?: "",
                                            studentId = student.id,
                                            subjectId = subjectId,
                                            teacherId = schoolClass.teacherId,
                                            value = value,
                                            period = periodFor(column.key),
                                            academicYear = current?.academicYear ?: currentAcademicYear(),
                                            note = column.title
                                        )
                                        if (current == null) viewModel.saveGrade(grade) else viewModel.updateGrade(grade)
                                        selectedCell = nextCell()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) { Text(value.toString()) }
                            }
                        }
                        current?.let {
                            TextButton(onClick = {
                                viewModel.deleteGrade(it.id)
                                selectedCell = nextCell()
                            }) { Text("Fshi dhe vazhdo") }
                        }
                        Text(
                            "Pas ruajtjes kalon automatikisht te vlerësimi tjetër.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { selectedCell = null }) { Text("Mbyll") } }
            )
        }
    }
}

@Composable
private fun TableHeader(title: String, width: Dp) {
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

private fun gradeFor(grades: List<Grade>, studentId: String, subjectId: String, key: String): Grade? =
    grades.firstOrNull {
        it.studentId == studentId &&
            it.subjectId == subjectId &&
            it.note == assessmentColumns.firstOrNull { column -> column.key == key }?.title
    }

private fun periodFor(key: String): String = when (key) {
    "TEST_1", "TEST_2", "SEM_1" -> "Periudha I"
    else -> "Periudha II"
}

private fun currentAcademicYear(): String {
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH) + 1
    val startYear = if (month >= 9) year else year - 1
    return "$startYear/${startYear + 1}"
}
