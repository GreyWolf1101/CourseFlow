package com.courseflow.app.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.UUID

enum class WeekPattern(val label: String) {
    EVERY("每周"), ODD("单周"), EVEN("双周");

    fun includes(week: Int): Boolean = when (this) {
        EVERY -> true
        ODD -> week % 2 == 1
        EVEN -> week % 2 == 0
    }
}

data class CourseSession(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val teacher: String = "",
    val room: String = "",
    val note: String = "",
    val dayOfWeek: Int,
    val startPeriod: Int,
    val periodSpan: Int = 1,
    val startWeek: Int = 1,
    val endWeek: Int = 20,
    val weekPattern: WeekPattern = WeekPattern.EVERY,
    val colorIndex: Int = 0,
) {
    fun occursInWeek(week: Int): Boolean = week in startWeek..endWeek && weekPattern.includes(week)

    fun canMergeWith(other: CourseSession): Boolean =
        name.trim().equals(other.name.trim(), ignoreCase = true) &&
            teacher.trim() == other.teacher.trim() && room.trim() == other.room.trim() &&
            dayOfWeek == other.dayOfWeek && startWeek == other.startWeek && endWeek == other.endWeek &&
            weekPattern == other.weekPattern && startPeriod + periodSpan == other.startPeriod
}

data class PeriodDefinition(
    val index: Int,
    val startTime: String,
    val durationMinutes: Int = 45,
) {
    fun endTime(): String = runCatching {
        LocalTime.parse(startTime).plusMinutes(durationMinutes.toLong()).toString()
    }.getOrDefault("--:--")
}

data class SemesterConfig(
    val name: String = "2026—2027学年 第一学期",
    val startDate: String = LocalDate.now()
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString(),
    val totalWeeks: Int = 20,
    val periods: List<PeriodDefinition> = defaultPeriods(),
) {
    fun monday(): LocalDate = runCatching { LocalDate.parse(startDate) }
        .getOrElse { LocalDate.now() }
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun weekFor(date: LocalDate = LocalDate.now()): Int {
        val delta = ChronoUnit.DAYS.between(monday(), date)
        return (Math.floorDiv(delta, 7) + 1).toInt().coerceIn(1, totalWeeks)
    }

    fun dateFor(week: Int, day: Int): LocalDate =
        monday().plusWeeks((week - 1).toLong()).plusDays((day - 1).toLong())
}

data class ScheduleState(
    val config: SemesterConfig = SemesterConfig(),
    val courses: List<CourseSession> = emptyList(),
)

fun defaultPeriods(): List<PeriodDefinition> = listOf(
    PeriodDefinition(1, "08:00", 45),
    PeriodDefinition(2, "08:55", 45),
    PeriodDefinition(3, "09:50", 45),
    PeriodDefinition(4, "10:45", 45),
    PeriodDefinition(5, "14:00", 45),
    PeriodDefinition(6, "14:55", 45),
    PeriodDefinition(7, "15:50", 45),
    PeriodDefinition(8, "16:45", 45),
    PeriodDefinition(9, "19:00", 45),
    PeriodDefinition(10, "19:55", 45),
    PeriodDefinition(11, "20:50", 45),
    PeriodDefinition(12, "21:45", 45),
    PeriodDefinition(13, "22:35", 25),
)

fun mergeContinuousCourses(source: List<CourseSession>): List<CourseSession> {
    val sorted = source.sortedWith(compareBy<CourseSession> { it.dayOfWeek }.thenBy { it.startPeriod })
    val result = mutableListOf<CourseSession>()
    sorted.forEach { current ->
        val index = result.indexOfLast { it.canMergeWith(current) }
        if (index >= 0) {
            val previous = result[index]
            result[index] = previous.copy(periodSpan = previous.periodSpan + current.periodSpan)
        } else {
            result += current
        }
    }
    return result
}
