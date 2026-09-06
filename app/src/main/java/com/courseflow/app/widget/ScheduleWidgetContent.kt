package com.courseflow.app.widget

import com.courseflow.app.model.ScheduleState
import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal object ScheduleWidgetContent {
    fun week(state: ScheduleState, date: LocalDate): Int =
        (Math.floorDiv(ChronoUnit.DAYS.between(state.config.monday(), date), 7) + 1).toInt()

    fun day(state: ScheduleState, date: LocalDate, label: String, capacity: Int, lineLength: Int = 10, compact: Boolean = false): String {
        val week = week(state, date)
        if (week < 1) return "学期尚未开始"
        if (week > state.config.totalWeeks) return "本学期已结束"
        val courses = state.courses.filter { it.dayOfWeek == date.dayOfWeek.value && it.occursInWeek(week) }
            .sortedWith(compareBy({ it.startPeriod }, { it.name }))
        if (courses.isEmpty()) return if (compact) "${label}没有课啦" else "(ˊ▽ˋ)\n\n${label}没有课啦"
        val shown = courses.take(capacity)
        fun short(text: String) = text.replace('\n', ' ').let { if (it.length > lineLength) it.take(lineLength - 1) + "…" else it }
        return shown.joinToString("\n\n") { course ->
            val end = course.startPeriod + course.periodSpan - 1
            val time = state.config.periods.firstOrNull { it.index == course.startPeriod }?.startTime.orEmpty()
            "${short(course.name)}\n${time} · ${course.startPeriod}—${end}节" +
                course.room.takeIf { it.isNotBlank() }?.let { "\n${short(it)}" }.orEmpty()
        } + if (courses.size > capacity) "\n还有${courses.size - capacity}门 · 点击查看" else ""
    }
}
