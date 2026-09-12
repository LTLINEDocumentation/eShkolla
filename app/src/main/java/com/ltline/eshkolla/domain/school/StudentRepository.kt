package com.ltline.eshkolla.domain.school

import com.ltline.eshkolla.domain.model.Student

interface StudentRepository {
    suspend fun getStudents(): List<Student>
    suspend fun getStudentsByClass(classId: String): List<Student>
    suspend fun getStudent(id: String): Student?
    suspend fun addStudent(student: Student): Student
    suspend fun updateStudent(student: Student): Student
    suspend fun setStudentActive(id: String, active: Boolean): Student?
}
