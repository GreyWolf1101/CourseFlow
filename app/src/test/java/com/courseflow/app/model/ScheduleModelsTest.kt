package com.courseflow.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ScheduleModelsTest {
    @Test
    fun `odd and even courses only appear in matching weeks`() {
        val odd = CourseSession(name = "测试", dayOfWeek = 1, startPeriod = 1, weekPattern = WeekPattern.ODD)
        assertTrue(odd.occursInWeek(3))
        assertFalse(odd.occursInWeek(4))
    }

    @Test
    fun `continuous identical sessions are merged`() {
        val first = CourseSession(id = "1", name = "高等数学", teacher = "王老师", room = "A101", dayOfWeek = 1, startPeriod = 1)
        val second = first.copy(id = "2", startPeriod = 2)
        val merged = mergeContinuousCourses(listOf(first, second))
        assertEquals(1, merged.size)
        assertEquals(2, merged.single().periodSpan)
    }

    @Test
    fun `week number is based on semester monday`() {
        val config = SemesterConfig(startDate = "2026-08-31", totalWeeks = 20)
        assertEquals(1, config.weekFor(LocalDate.of(2026, 9, 3)))
        assertEquals(3, config.weekFor(LocalDate.of(2026, 9, 17)))
    }

    @Test
    fun `default periods cover classes through 23 o'clock`() {
        val periods = defaultPeriods()
        assertEquals(13, periods.size)
        assertEquals("23:00", periods.last().endTime())
    }
}
