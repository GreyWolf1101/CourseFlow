package com.courseflow.app.model

import com.courseflow.app.data.toJson
import com.courseflow.app.data.toScheduleState
import com.courseflow.app.importer.ScheduleShareCodec
import com.courseflow.app.widget.ScheduleWidgetContent
import org.junit.Assert.*
import org.junit.Test

class PeriodSettingsTest {
    private val config = SemesterConfig(periods = linkedSchoolPeriods(), continuousTeaching = true)
    private fun course(start: Int, span: Int = 2) = CourseSession(name = "联排课", dayOfWeek = 1, startPeriod = start, periodSpan = span)

    @Test fun `school template matches every illustrated block including evening break`() {
        assertNull(periodValidationError(config.periods))
        val expected = mapOf(1 to "08:00–09:30", 3 to "09:45–11:15", 6 to "13:00–14:30",
            8 to "14:45–16:15", 11 to "18:00–19:30", 13 to "19:40–21:10")
        expected.forEach { (start, time) -> assertEquals(time, config.courseTime(course(start))) }
        assertEquals("11:30–12:15", config.courseTime(course(5, 1)))
        assertEquals("16:30–17:15", config.courseTime(course(10, 1)))
        assertEquals("10:30–12:00", config.courseTime(course(4)))
        assertEquals("10:30–12:15", config.copy(continuousTeaching = false).courseTime(course(4)))
    }

    @Test fun `batch generation supports ordinary breaks and groups relative to selected range`() {
        assertEquals(listOf("08:00", "09:00", "10:00"), generatePeriods(1, 3, "08:00", 45, 1, 15).map { it.startTime })
        assertEquals(config.periods.subList(5, 10), generatePeriods(6, 10, "13:00", 45, 2, 15))
        assertTrue(runCatching { generatePeriods(1, 14, "18:00", 45, 2, 15) }.isFailure)
        assertTrue(runCatching { generatePeriods(1, 3, "25:00", 45, 1, 10) }.isFailure)
    }

    @Test fun `resize preserves existing custom times and validates overlap and midnight`() {
        val resized = resizePeriods(config.periods, 16)
        assertEquals(config.periods, resized.take(14))
        assertEquals(16, resized.size)
        assertEquals("21:10", resized[14].startTime)
        assertEquals(config.periods.take(10), resizePeriods(config.periods, 10))
        assertEquals(30, resizePeriods(config.periods, 30).size)
        assertNotNull(periodValidationError(resizePeriods(config.periods, 30)))
        assertNotNull(periodValidationError(listOf(PeriodDefinition(1, "08:00"), PeriodDefinition(2, "08:30"))))
    }

    @Test fun `linked times survive persistence sharing and render identically in widget`() {
        val state = ScheduleState(config, listOf(course(4), course(13)))
        assertEquals(state, state.toJson().toScheduleState())
        val decoded = ScheduleShareCodec.decode(ScheduleShareCodec.encode(state))
        assertEquals(config, decoded.config)
        assertEquals("10:30–12:00", ScheduleWidgetContent.time(state, course(4)))
        assertEquals("19:40–21:10", decoded.config!!.courseTime(decoded.courses.last()))
        val oldJson = state.toJson()
        oldJson.getJSONObject("config").remove("continuousTeaching")
        assertFalse(oldJson.toScheduleState().config.continuousTeaching)
    }
}
