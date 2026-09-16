package com.ltline.eshkolla.presentation.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ltline.eshkolla.data.assessment.ApiAbsenceRepository
import com.ltline.eshkolla.data.assessment.ApiGradeRepository
import com.ltline.eshkolla.data.school.ApiStudentRepository
import com.ltline.eshkolla.domain.model.Absence
import com.ltline.eshkolla.domain.model.Grade
import com.ltline.eshkolla.domain.model.Student
import com.ltline.eshkolla.domain.repository.AbsenceRepository
import com.ltline.eshkolla.domain.repository.GradeRepository
import com.ltline.eshkolla.domain.school.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TeacherAssessmentUiState(
    val grades: List<Grade> = emptyList(),
    val absences: List<Absence> = emptyList(),
    val students: List<Student> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class TeacherAssessmentViewModel(
    private val gradeRepository: GradeRepository = ApiGradeRepository(),
    private val absenceRepository: AbsenceRepository = ApiAbsenceRepository(),
    private val studentRepository: StudentRepository = ApiStudentRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(TeacherAssessmentUiState())
    val uiState: StateFlow<TeacherAssessmentUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                val grades = gradeRepository.getGrades()
                val absences = absenceRepository.getAbsences()
                val students = studentRepository.getStudents().filter { it.isActive }.sortedBy { it.fullName.lowercase() }
                Triple(grades, absences, students)
            }.onSuccess { (grades, absences, students) ->
                _uiState.value = TeacherAssessmentUiState(
                    grades = grades,
                    absences = absences,
                    students = students
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = it.message ?: "Gabim gjatë ngarkimit.")
            }
        }
    }

    fun loadGradesForClass(classId: String) {
        val apiRepository = gradeRepository as? ApiGradeRepository
        if (apiRepository == null) {
            refresh()
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching { apiRepository.getGradesByClass(classId) }
                .onSuccess { grades ->
                    _uiState.value = _uiState.value.copy(grades = grades, isLoading = false, error = null)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = error.message ?: "Gabim gjatë ngarkimit të notave.")
                }
        }
    }

    fun saveGrade(
        grade: Grade,
        onSuccess: () -> Unit = {},
        onFailure: () -> Unit = {}
    ) = execute("Gabim gjatë ruajtjes së notës.", onSuccess, onFailure) { gradeRepository.addGrade(grade) }

    fun updateGrade(
        grade: Grade,
        onSuccess: () -> Unit = {},
        onFailure: () -> Unit = {}
    ) = execute("Gabim gjatë ndryshimit të notës.", onSuccess, onFailure) { gradeRepository.updateGrade(grade) }

    fun deleteGrade(
        id: String,
        onSuccess: () -> Unit = {},
        onFailure: () -> Unit = {}
    ) = execute("Gabim gjatë fshirjes së notës.", onSuccess, onFailure) { gradeRepository.deleteGrade(id) }

    fun saveAbsence(absence: Absence) = execute("Gabim gjatë ruajtjes së mungesës.") { absenceRepository.addAbsence(absence) }

    fun updateAbsence(absence: Absence) = execute("Gabim gjatë ndryshimit të mungesës.") { absenceRepository.updateAbsence(absence) }

    fun deleteAbsence(id: String) = execute("Gabim gjatë fshirjes së mungesës.") { absenceRepository.deleteAbsence(id) }

    private fun execute(
        defaultError: String,
        onSuccess: () -> Unit = {},
        onFailure: () -> Unit = {},
        action: suspend () -> Any?
    ) {
        viewModelScope.launch {
            runCatching { action() }
                .onSuccess {
                    onSuccess()
                    refresh()
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(error = it.message ?: defaultError)
                    onFailure()
                }
        }
    }
}
