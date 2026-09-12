package com.ltline.eshkolla.data.school

import com.ltline.eshkolla.domain.model.School
import com.ltline.eshkolla.domain.model.SchoolClass
import com.ltline.eshkolla.domain.model.Student
import com.ltline.eshkolla.domain.model.Subject
import com.ltline.eshkolla.domain.model.Teacher
import com.ltline.eshkolla.domain.school.SchoolClassRepository
import com.ltline.eshkolla.domain.school.SchoolRepository
import com.ltline.eshkolla.domain.school.StudentRepository
import com.ltline.eshkolla.domain.school.SubjectRepository
import com.ltline.eshkolla.domain.school.TeacherRepository

class FakeSchoolRepository : SchoolRepository {
    private val schools = listOf(
        School("S1", "eShkolla Demo", "—", isActive = true)
    )
    override suspend fun getSchools(): List<School> = schools
    override suspend fun getSchool(id: String): School? = schools.firstOrNull { it.id == id }
}

class FakeSchoolClassRepository : SchoolClassRepository {
    private val classes = listOf(
        SchoolClass("C1", "S1", 8, "1", "2026/2027"),
        SchoolClass("C2", "S1", 8, "2", "2026/2027")
    )
    override suspend fun getClasses(): List<SchoolClass> = classes
    override suspend fun getClass(id: String): SchoolClass? = classes.firstOrNull { it.id == id }
}

class FakeStudentRepository : StudentRepository {
    private val students = listOf(
        Student("NX001", "S1", "C1", "Ardit", "Krasniqi", "2012-03-01"),
        Student("NX002", "S1", "C1", "Era", "Gashi", "2012-07-18"),
        Student("NX003", "S1", "C2", "Diar", "Berisha", "2012-02-09"),
        Student("NX004", "S1", "C2", "Suela", "Hoxha", "2012-11-22")
    )
    override suspend fun getStudents(): List<Student> = students
    override suspend fun getStudentsByClass(classId: String): List<Student> = students.filter { it.classId == classId }
    override suspend fun getStudent(id: String): Student? = students.firstOrNull { it.id == id }
}

class FakeTeacherRepository : TeacherRepository {
    private val teachers = listOf(
        Teacher("T1", "S1", "3", "T-1001", "Matematikë"),
        Teacher("T2", "S1", "3", "T-1002", "Gjuhë shqipe"),
        Teacher("T3", "S1", "3", "T-1003", "Fizikë")
    )
    override suspend fun getTeachers(): List<Teacher> = teachers
    override suspend fun getTeacher(id: String): Teacher? = teachers.firstOrNull { it.id == id }
}

class FakeSubjectRepository : SubjectRepository {
    private val subjects = listOf(
        Subject("L1", "S1", "Matematikë", "MAT", 4),
        Subject("L2", "S1", "Gjuhë shqipe", "GJSH", 4),
        Subject("L3", "S1", "Fizikë", "FIZ", 2),
        Subject("L4", "S1", "Informatikë", "INF", 1)
    )
    override suspend fun getSubjects(): List<Subject> = subjects
    override suspend fun getSubject(id: String): Subject? = subjects.firstOrNull { it.id == id }
}
