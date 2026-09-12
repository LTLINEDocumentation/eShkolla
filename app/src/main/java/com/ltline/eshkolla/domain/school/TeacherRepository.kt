package com.ltline.eshkolla.domain.school

import com.ltline.eshkolla.domain.model.Teacher

interface TeacherRepository {
    suspend fun getTeachers(): List<Teacher>
    suspend fun getTeacher(id: String): Teacher?
}
