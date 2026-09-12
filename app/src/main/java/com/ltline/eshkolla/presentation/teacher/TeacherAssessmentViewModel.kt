package com.ltline.eshkolla.presentation.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ltline.eshkolla.data.assessment.FakeAbsenceRepository
import com.ltline.eshkolla.data.assessment.FakeGradeRepository
import com.ltline.eshkolla.domain.model.Absence
import com.ltline.eshkolla.domain.model.Grade
import com.ltline.eshkolla.domain.repository.AbsenceRepository
import com.ltline.eshkolla.domain.repository.GradeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

 data class TeacherAssessmentUiState(
    val grades: List<Grade> = emptyList(),
    val absences: List<Absence> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class TeacherAssessmentViewModel(
    private val gradeRepository: GradeRepository = FakeGradeRepository(),
    private val absenceRepository: AbsenceRepository = FakeAbsenceRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(TeacherAssessmentUiState())
    val uiState: StateFlow<TeacherAssessmentUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                gradeRepository.getGrades() to absenceRepository.getAbsences()
            }.onSuccess { (grades, absences) ->
                _uiState.value = TeacherAssessmentUiState(grades = grades, absences = absences)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = it.message ?: "Gabim gjatë ngarkimit.")
            }
        }
    }

    fun saveGrade(grade: Grade) {
        viewModelScope.launch {
            runCatching { gradeRepository.addGrade(grade) }
                .onSuccess { refresh() }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message) }
        }
    }

    fun saveAbsence(absence: Absence) {
        viewModelScope.launch {
            runCatching { absenceRepository.addAbsence(absence) }
                .onSuccess { refresh() }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message) }
        }
    }
}
