package com.ltline.eshkolla.auth

/** Centralized authorization matrix for eShkolla. */
enum class Permission {
    USER_READ, USER_WRITE,
    SCHOOL_READ, SCHOOL_WRITE,
    TEACHER_ASSIGN,
    STUDENT_READ, STUDENT_WRITE,
    GRADE_READ, GRADE_WRITE, GRADE_UPDATE, GRADE_DELETE,
    ABSENCE_READ, ABSENCE_WRITE, ABSENCE_UPDATE, ABSENCE_DELETE,
    SCHEDULE_READ, SCHEDULE_WRITE,
    NOTIFICATION_READ, NOTIFICATION_WRITE,
    REPORT_READ, REPORT_WRITE,
    OWN_PROFILE_READ, OWN_PROFILE_WRITE,
    CHILD_PROFILE_READ, CHILD_ACADEMIC_READ
}

object RolePermissions {
    private val administrator = Permission.entries.toSet()

    private val director = setOf(
        Permission.USER_READ,
        Permission.SCHOOL_READ, Permission.SCHOOL_WRITE,
        Permission.TEACHER_ASSIGN,
        Permission.STUDENT_READ, Permission.STUDENT_WRITE,
        Permission.GRADE_READ,
        Permission.ABSENCE_READ,
        Permission.SCHEDULE_READ, Permission.SCHEDULE_WRITE,
        Permission.NOTIFICATION_READ, Permission.NOTIFICATION_WRITE,
        Permission.REPORT_READ, Permission.REPORT_WRITE,
        Permission.OWN_PROFILE_READ, Permission.OWN_PROFILE_WRITE
    )

    private val teacher = setOf(
        Permission.SCHOOL_READ,
        Permission.STUDENT_READ,
        Permission.GRADE_READ, Permission.GRADE_WRITE, Permission.GRADE_UPDATE, Permission.GRADE_DELETE,
        Permission.ABSENCE_READ, Permission.ABSENCE_WRITE, Permission.ABSENCE_UPDATE, Permission.ABSENCE_DELETE,
        Permission.SCHEDULE_READ,
        Permission.NOTIFICATION_READ,
        Permission.REPORT_READ,
        Permission.OWN_PROFILE_READ, Permission.OWN_PROFILE_WRITE
    )

    private val student = setOf(
        Permission.OWN_PROFILE_READ, Permission.OWN_PROFILE_WRITE,
        Permission.GRADE_READ, Permission.ABSENCE_READ,
        Permission.SCHEDULE_READ, Permission.NOTIFICATION_READ,
        Permission.REPORT_READ
    )

    private val parent = setOf(
        Permission.CHILD_PROFILE_READ, Permission.CHILD_ACADEMIC_READ,
        Permission.GRADE_READ, Permission.ABSENCE_READ,
        Permission.SCHEDULE_READ, Permission.NOTIFICATION_READ,
        Permission.REPORT_READ,
        Permission.OWN_PROFILE_READ, Permission.OWN_PROFILE_WRITE
    )

    fun allows(role: String, permission: Permission): Boolean = when (role.uppercase()) {
        "ADMIN", "ADMINISTRATOR" -> permission in administrator
        "DREJTOR" -> permission in director
        "MESIMDHENES" -> permission in teacher
        "NXENES" -> permission in student
        "PRIND" -> permission in parent
        else -> false
    }
}
