package com.ltline.eshkolla.repository

import com.ltline.eshkolla.db.Database

interface TeacherClassRepository {
    fun getClassIdsForTeacher(teacherId: String): Set<String>
    fun getTeacherIdsForClass(classId: String): Set<String>
    fun isAssigned(teacherId: String, classId: String): Boolean
    fun assign(teacherId: String, classId: String): Boolean
    fun remove(teacherId: String, classId: String): Boolean
}

class PostgresTeacherClassRepository : TeacherClassRepository {
    override fun getClassIdsForTeacher(teacherId: String): Set<String> =
        Database.connection().use { connection ->
            connection.prepareStatement(
                "SELECT class_id FROM teacher_classes WHERE teacher_id = ? ORDER BY class_id"
            ).use { ps ->
                ps.setString(1, teacherId)
                ps.executeQuery().use { rs ->
                    buildSet {
                        while (rs.next()) add(rs.getString("class_id"))
                    }
                }
            }
        }

    override fun getTeacherIdsForClass(classId: String): Set<String> =
        Database.connection().use { connection ->
            connection.prepareStatement(
                "SELECT teacher_id FROM teacher_classes WHERE class_id = ? ORDER BY teacher_id"
            ).use { ps ->
                ps.setString(1, classId)
                ps.executeQuery().use { rs ->
                    buildSet {
                        while (rs.next()) add(rs.getString("teacher_id"))
                    }
                }
            }
        }

    override fun isAssigned(teacherId: String, classId: String): Boolean =
        Database.connection().use { connection ->
            connection.prepareStatement(
                "SELECT 1 FROM teacher_classes WHERE teacher_id = ? AND class_id = ?"
            ).use { ps ->
                ps.setString(1, teacherId)
                ps.setString(2, classId)
                ps.executeQuery().use { rs -> rs.next() }
            }
        }

    override fun assign(teacherId: String, classId: String): Boolean =
        Database.connection().use { connection ->
            connection.prepareStatement(
                "INSERT INTO teacher_classes(teacher_id, class_id) VALUES (?, ?) ON CONFLICT DO NOTHING"
            ).use { ps ->
                ps.setString(1, teacherId)
                ps.setString(2, classId)
                ps.executeUpdate() > 0
            }
        }

    override fun remove(teacherId: String, classId: String): Boolean =
        Database.connection().use { connection ->
            connection.prepareStatement(
                "DELETE FROM teacher_classes WHERE teacher_id = ? AND class_id = ?"
            ).use { ps ->
                ps.setString(1, teacherId)
                ps.setString(2, classId)
                ps.executeUpdate() > 0
            }
        }
}
