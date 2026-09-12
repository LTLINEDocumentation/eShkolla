package com.ltline.eshkolla.features.students

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ltline.eshkolla.data.school.FakeSchoolRepositories
import com.ltline.eshkolla.domain.model.Student
import com.ltline.eshkolla.domain.school.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface StudentListState {
    data object Loading : StudentListState
    data class Success(val students: List<Student>) : StudentListState
    data class Error(val message: String) : StudentListState
}

class StudentViewModel(
    private val repository: StudentRepository = FakeSchoolRepositories.studentRepository()
) : ViewModel() {
    private val _state = MutableStateFlow<StudentListState>(StudentListState.Loading)
    val state: StateFlow<StudentListState> = _state.asStateFlow()

    init { loadStudents() }

    fun loadStudents() {
        viewModelScope.launch {
            _state.value = StudentListState.Loading
            runCatching { repository.getStudents() }
                .onSuccess { _state.value = StudentListState.Success(it) }
                .onFailure { _state.value = StudentListState.Error(it.message ?: "Nuk u ngarkuan nxënësit.") }
        }
    }

    fun saveStudent(student: Student) {
        viewModelScope.launch {
            runCatching {
                if (repository.getStudent(student.id) == null) repository.addStudent(student)
                else repository.updateStudent(student)
                repository.getStudents()
            }.onSuccess { _state.value = StudentListState.Success(it) }
                .onFailure { _state.value = StudentListState.Error(it.message ?: "Nuk u ruajt nxënësi.") }
        }
    }

    fun toggleActive(student: Student) {
        viewModelScope.launch {
            runCatching { repository.setStudentActive(student.id, !student.isActive); repository.getStudents() }
                .onSuccess { _state.value = StudentListState.Success(it) }
                .onFailure { _state.value = StudentListState.Error(it.message ?: "Nuk u ndryshua statusi.") }
        }
    }
}
