package com.ltline.eshkolla.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GradeTest {
    @Test
    fun validGradeIsAccepted() {
        val grade = Grade("G1", "NX001", "L1", "T1", 5, "Periudha I", "2026/2027")
        assertEquals(5, grade.value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun gradeOutsideRangeIsRejected() {
        Grade("G2", "NX001", "L1", "T1", 6, "Periudha I", "2026/2027")
    }
}
