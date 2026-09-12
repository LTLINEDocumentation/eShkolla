package com.ltline.eshkolla.repository

import com.ltline.eshkolla.api.StudentDto
import java.util.concurrent.ConcurrentHashMap

interface StudentRepository {
    fun findAll(): List<StudentDto>
    fun findById(id: String): StudentDto?
    fun save(student: StudentDto): StudentDto
    fun delete(id: String): Boolean
}

class InMemoryStudentRepository : StudentRepository {
    private val students = ConcurrentHashMap<String, StudentDto>(
        listOf(
            StudentDto("NX001", "Ardit Krasniqi", "C03", "2012-03-01", true),
            StudentDto("NX002", "Era Gashi", "C03", "2012-07-18", true),
            StudentDto("NX003", "Diar Berisha", "C04", "2012-02-09", true),
            StudentDto("NX004", "Suela Hoxha", "C04", "2012-11-22", true)
        ).associateBy { it.id }
    )

    override fun findAll(): List<StudentDto> = students.values.sortedBy { it.fullName }
    override fun findById(id: String): StudentDto? = students[id]
    override fun save(student: StudentDto): StudentDto {
        students[student.id] = student
        return student
    }
    override fun delete(id: String): Boolean {
        val current = students[id] ?: return false
        students[id] = current.copy(isActive = false)
        return true
    }
}
