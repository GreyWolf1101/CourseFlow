package com.courseflow.app.widget

import com.courseflow.app.model.CourseSession
import com.courseflow.app.model.ScheduleState
import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal object ScheduleWidgetContent {
    fun week(state: ScheduleState, date: LocalDate): Int =
        (Math.floorDiv(ChronoUnit.DAYS.between(state.config.monday(), date), 7) + 1).toInt()

    fun courses(state: ScheduleState, date: LocalDate): List<CourseSession> {
        val week = week(state, date)
        if (week !in 1..state.config.totalWeeks) return emptyList()
        return state.courses.filter { it.dayOfWeek == date.dayOfWeek.value && it.occursInWeek(week) }
            .sortedWith(compareBy({ it.startPeriod }, { it.name }))
    }

    fun emptyMessage(state: ScheduleState, date: LocalDate): String = when {
        week(state, date) < 1 -> "学期尚未开始"
        week(state, date) > state.config.totalWeeks -> "本学期已结束"
        else -> "今天没有课啦"
    }

    fun time(state: ScheduleState, course: CourseSession): String {
        val endIndex = course.startPeriod + course.periodSpan - 1
        val start = state.config.periods.firstOrNull { it.index == course.startPeriod }
        val end = state.config.periods.firstOrNull { it.index == endIndex }
        return if (start != null && end != null) "${start.startTime}–${end.endTime()}"
            else "第${course.startPeriod}—${endIndex}节"
    }

}
