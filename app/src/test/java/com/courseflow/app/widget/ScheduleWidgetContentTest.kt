package com.courseflow.app.widget

import com.courseflow.app.model.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class ScheduleWidgetContentTest {
    private val monday = LocalDate.of(2026,9,7)
    private val state = ScheduleState(SemesterConfig(startDate="2026-09-07",totalWeeks=2), listOf(
        CourseSession(name="单周课",dayOfWeek=1,startPeriod=1,weekPattern=WeekPattern.ODD),
        CourseSession(name="双周课",dayOfWeek=1,startPeriod=3,weekPattern=WeekPattern.EVEN)))
    @Test fun semesterBoundariesDoNotRepeatFirstOrLastWeek() {
        assertEquals("学期尚未开始",ScheduleWidgetContent.emptyMessage(state,monday.minusDays(1)))
        assertEquals("本学期已结束",ScheduleWidgetContent.emptyMessage(state,monday.plusWeeks(2)))
        assertTrue(ScheduleWidgetContent.courses(state,monday.minusDays(1)).isEmpty())
        assertTrue(ScheduleWidgetContent.courses(state,monday.plusWeeks(2)).isEmpty())
    }
    @Test fun coursesUseDateWeekAndWeekday() {
        assertEquals(listOf("单周课"),ScheduleWidgetContent.courses(state,monday).map { it.name })
        assertEquals(listOf("双周课"),ScheduleWidgetContent.courses(state,monday.plusWeeks(1)).map { it.name })
        assertTrue(ScheduleWidgetContent.courses(state,monday.plusDays(1)).isEmpty())
    }
    @Test fun emptyDaysAndSorting() {
        assertEquals("今天没有课啦",ScheduleWidgetContent.emptyMessage(state,monday.plusDays(1)))
        val many=state.copy(courses=state.courses.reversed().map { it.copy(weekPattern=WeekPattern.EVERY) })
        assertEquals(listOf(1,3),ScheduleWidgetContent.courses(many,monday).map { it.startPeriod })
    }
    @Test fun timeUsesLastPeriodDurationIncludingBreaks() {
        val custom=state.copy(config=state.config.copy(periods=listOf(
            PeriodDefinition(1,"08:00",40),PeriodDefinition(2,"09:00",50))))
        assertEquals("08:00–09:50",ScheduleWidgetContent.time(custom,state.courses.first().copy(periodSpan=2)))
        assertEquals("第1—3节",ScheduleWidgetContent.time(custom,state.courses.first().copy(periodSpan=3)))
    }
}
