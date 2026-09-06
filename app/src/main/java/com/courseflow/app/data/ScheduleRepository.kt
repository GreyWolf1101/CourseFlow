package com.courseflow.app.data

import android.content.Context
import com.courseflow.app.model.CourseSession
import com.courseflow.app.model.PeriodDefinition
import com.courseflow.app.model.ScheduleState
import com.courseflow.app.model.SemesterConfig
import com.courseflow.app.model.WeekPattern
import com.courseflow.app.model.defaultPeriods
import com.courseflow.app.model.mergeContinuousCourses
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

class ScheduleRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = context.getSharedPreferences("courseflow_schedule", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<ScheduleState> = _state

    fun saveCourse(course: CourseSession) {
        val items = _state.value.courses.toMutableList()
        val index = items.indexOfFirst { it.id == course.id }
        if (index >= 0) items[index] = course else items += course
        update(_state.value.copy(courses = mergeContinuousCourses(items)))
    }

    fun deleteCourse(id: String) = update(
        _state.value.copy(courses = _state.value.courses.filterNot { it.id == id })
    )

    fun deleteCourses(ids: Set<String>) = update(
        _state.value.copy(courses = _state.value.courses.filterNot { it.id in ids })
    )

    fun clearCourses() = update(_state.value.copy(courses = emptyList()))

    fun importCourses(courses: List<CourseSession>, replace: Boolean, config: SemesterConfig? = null) {
        val all = if (replace) courses else _state.value.courses + courses
        val unique = all.distinctBy {
            listOf(
                it.name.trim(), it.teacher.trim(), it.room.trim(), it.dayOfWeek,
                it.startPeriod, it.periodSpan, it.startWeek, it.endWeek, it.weekPattern,
            ).joinToString("|")
        }
        update(_state.value.copy(courses = mergeContinuousCourses(unique), config = config ?: _state.value.config))
    }

    fun updateConfig(config: SemesterConfig) = update(_state.value.copy(config = config))

    private fun update(value: ScheduleState) {
        _state.value = value
        prefs.edit().putString("state", value.toJson().toString()).apply()
        com.courseflow.app.widget.ScheduleWidgetProvider.updateAll(appContext, value)
    }

    private fun load(): ScheduleState {
        val raw = prefs.getString("state", null) ?: return sampleState()
        return runCatching {
            val json = JSONObject(raw)
            val state = json.toScheduleState()
            if (json.optInt("schemaVersion", 1) < 2) state.withEveningPeriods() else state
        }.getOrElse { sampleState() }
    }
}

private fun ScheduleState.withEveningPeriods(): ScheduleState {
    val existingIndexes = config.periods.map { it.index }.toSet()
    val missing = defaultPeriods().filterNot { it.index in existingIndexes }
    if (missing.isEmpty()) return this
    return copy(config = config.copy(periods = (config.periods + missing).sortedBy { it.index }))
}

internal fun ScheduleState.toJson() = JSONObject().apply {
    put("schemaVersion", 2)
    put("config", JSONObject().apply {
        put("name", config.name)
        put("startDate", config.startDate)
        put("totalWeeks", config.totalWeeks)
        put("periods", JSONArray().apply {
            config.periods.forEach { period ->
                put(JSONObject().apply {
                    put("index", period.index)
                    put("startTime", period.startTime)
                    put("durationMinutes", period.durationMinutes)
                })
            }
        })
    })
    put("courses", JSONArray().apply {
        courses.forEach { course ->
            put(JSONObject().apply {
                put("id", course.id)
                put("name", course.name)
                put("teacher", course.teacher)
                put("room", course.room)
                put("note", course.note)
                put("dayOfWeek", course.dayOfWeek)
                put("startPeriod", course.startPeriod)
                put("periodSpan", course.periodSpan)
                put("startWeek", course.startWeek)
                put("endWeek", course.endWeek)
                put("weekPattern", course.weekPattern.name)
                put("colorIndex", course.colorIndex)
            })
        }
    })
}

internal fun JSONObject.toScheduleState(): ScheduleState {
    val configJson = getJSONObject("config")
    val periodsJson = configJson.getJSONArray("periods")
    val periods = (0 until periodsJson.length()).map { index ->
        periodsJson.getJSONObject(index).run {
            PeriodDefinition(getInt("index"), getString("startTime"), getInt("durationMinutes"))
        }
    }
    val coursesJson = getJSONArray("courses")
    val courses = (0 until coursesJson.length()).map { index ->
        coursesJson.getJSONObject(index).run {
            CourseSession(
                id = getString("id"),
                name = getString("name"),
                teacher = optString("teacher"),
                room = optString("room"),
                note = optString("note"),
                dayOfWeek = getInt("dayOfWeek"),
                startPeriod = getInt("startPeriod"),
                periodSpan = getInt("periodSpan"),
                startWeek = getInt("startWeek"),
                endWeek = getInt("endWeek"),
                weekPattern = runCatching { WeekPattern.valueOf(getString("weekPattern")) }.getOrDefault(WeekPattern.EVERY),
                colorIndex = optInt("colorIndex"),
            )
        }
    }
    return ScheduleState(
        SemesterConfig(
            name = configJson.getString("name"),
            startDate = configJson.getString("startDate"),
            totalWeeks = configJson.getInt("totalWeeks"),
            periods = periods,
        ),
        courses,
    )
}

private fun sampleState(): ScheduleState {
    val courses = listOf(
        CourseSession(id = "sample-1", name = "移动应用开发", teacher = "林老师", room = "实训楼 A304", note = "带电脑，课前提交签到", dayOfWeek = 1, startPeriod = 1, periodSpan = 2, startWeek = 1, endWeek = 16, colorIndex = 0),
        CourseSession(id = "sample-2", name = "数据结构", teacher = "周老师", room = "知行楼 205", dayOfWeek = 2, startPeriod = 3, periodSpan = 2, startWeek = 1, endWeek = 18, colorIndex = 1),
        CourseSession(id = "sample-3", name = "大学英语", teacher = "Mia", room = "博雅楼 412", dayOfWeek = 3, startPeriod = 1, periodSpan = 2, startWeek = 2, endWeek = 18, weekPattern = WeekPattern.EVEN, colorIndex = 2),
        CourseSession(id = "sample-4", name = "产品设计工作坊", teacher = "顾老师", room = "创客中心 3F", dayOfWeek = 4, startPeriod = 5, periodSpan = 3, startWeek = 1, endWeek = 12, colorIndex = 3),
        CourseSession(id = "sample-5", name = "体育", teacher = "陈老师", room = "东区田径场", dayOfWeek = 5, startPeriod = 3, periodSpan = 2, startWeek = 1, endWeek = 16, weekPattern = WeekPattern.ODD, colorIndex = 4),
        CourseSession(id = "sample-6", name = "摄影基础", teacher = "宋老师", room = "艺术楼 106", dayOfWeek = 6, startPeriod = 1, periodSpan = 2, startWeek = 5, endWeek = 14, colorIndex = 5),
        CourseSession(id = "sample-7", name = "创新创业讲座", teacher = "许老师", room = "线上课堂", note = "晚间课程", dayOfWeek = 7, startPeriod = 11, periodSpan = 2, startWeek = 1, endWeek = 8, colorIndex = 8),
    )
    return ScheduleState(courses = courses)
}
