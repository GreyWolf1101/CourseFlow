package com.courseflow.app.importer

import com.courseflow.app.model.CourseSession
import com.courseflow.app.model.WeekPattern
import com.courseflow.app.model.mergeContinuousCourses

data class ParsedSchedule(
    val courses: List<CourseSession>,
    val warnings: List<String> = emptyList(),
)

/**
 * A deliberately tolerant parser for common Chinese university timetable exports.
 * It understands table-shaped TSV as well as one-course-per-line text produced by OCR.
 */
class StructuredScheduleParser {
    private val dayToken = Regex("(?:星期|周)([一二三四五六日天])|\\b(Mon|Tue|Wed|Thu|Fri|Sat|Sun)(?:day)?\\b", RegexOption.IGNORE_CASE)
    private val periodToken = Regex("第?\\s*(\\d{1,2})\\s*(?:[-~～—–至到]\\s*(\\d{1,2}))?\\s*(?:节|大节|课)?")
    private val weekRange = Regex("第?\\s*(\\d{1,2})\\s*(?:[-~～—–至到]\\s*(\\d{1,2}))?\\s*周")
    private val teacherLabel = Regex("(?:任课教师|教师|老师|授课人)\\s*[:：]?\\s*([^\\s,，;；]{1,16})")
    private val roomLabel = Regex("(?:上课地点|地点|教室|场地)\\s*[:：]?\\s*([^,，;；\\n\\t]{1,24}?)(?=\\s{2,}|\\s+第?\\d|$)")

    fun parse(rawText: String, totalWeeks: Int = 20): ParsedSchedule {
        val normalized = rawText
            .replace('\u00A0', ' ')
            .replace("\r\n", "\n")
            .replace('\r', '\n')
        if (normalized.isBlank()) return ParsedSchedule(emptyList(), listOf("文件中没有识别到文字"))

        val found = mutableListOf<CourseSession>()
        found += parseTables(normalized, totalWeeks)
        found += parseLabeledLines(normalized, totalWeeks)

        val distinct = found.distinctBy {
            listOf(it.name, it.dayOfWeek, it.startPeriod, it.periodSpan, it.startWeek, it.endWeek, it.weekPattern).joinToString("|")
        }
        val merged = mergeContinuousCourses(distinct).mapIndexed { index, course ->
            course.copy(colorIndex = stableColor(course.name, index))
        }

        val warnings = buildList {
            if (merged.isEmpty()) add("没有自动定位到课程，请确认文件包含星期、节次和课程名称")
            if (normalized.contains(".doc", ignoreCase = true)) add("旧版 Office 格式建议另存为 DOCX/XLSX 后再导入")
        }
        return ParsedSchedule(merged, warnings)
    }

    private fun parseTables(text: String, totalWeeks: Int): List<CourseSession> {
        val rows = text.lines().filter { it.contains('\t') }.map { it.split('\t') }
        if (rows.isEmpty()) return emptyList()

        val result = mutableListOf<CourseSession>()
        rows.forEachIndexed { headerIndex, header ->
            val dayColumns = header.mapIndexedNotNull { column, value ->
                parseDay(value)?.let { column to it }
            }.toMap()
            if (dayColumns.size < 2) return@forEachIndexed

            rows.drop(headerIndex + 1).takeWhile { row -> row.none { cell -> dayToken.containsMatchIn(cell) } }.forEach { row ->
                val periodCell = row.take(dayColumns.keys.minOrNull() ?: 1).joinToString(" ")
                val period = parsePeriod(periodCell) ?: return@forEach
                dayColumns.forEach { (column, day) ->
                    val cell = row.getOrNull(column).orEmpty().trim()
                    if (cell.isNotBlank()) parseCell(cell, day, period.first, period.second, totalWeeks)?.let(result::add)
                }
            }
        }
        return result
    }

    private fun parseLabeledLines(text: String, totalWeeks: Int): List<CourseSession> {
        val result = mutableListOf<CourseSession>()
        val lines = text.lines().map { it.trim() }.filter { it.length >= 4 }
        lines.forEach { line ->
            val dayMatch = dayToken.find(line) ?: return@forEach
            val periodMatch = periodToken.find(line, dayMatch.range.last + 1)
                ?: periodToken.find(line.substring(0, dayMatch.range.first))
                ?: return@forEach
            val day = parseDay(dayMatch.value) ?: return@forEach
            val start = periodMatch.groupValues[1].toIntOrNull() ?: return@forEach
            val end = periodMatch.groupValues[2].toIntOrNull() ?: start
            if (start !in 1..30 || end !in start..30) return@forEach

            var name = line.substring(0, dayMatch.range.first).trim(' ', '-', '—', '|', ':', '：')
            if (name.length < 2 || name.matches(Regex("\\d+"))) {
                name = line.substring(periodMatch.range.last + 1)
                    .substringBefore(weekRange.find(line)?.value ?: "__missing__")
                    .trim(' ', '-', '—', '|', ':', '：')
            }
            name = cleanCourseName(name)
            if (name.length < 2) return@forEach
            result += buildCourse(name, line, day, start, end - start + 1, totalWeeks)
        }
        return result
    }

    private fun parseCell(cell: String, day: Int, start: Int, span: Int, totalWeeks: Int): CourseSession? {
        val pieces = cell.lines().flatMap { it.split(Regex("\\s{2,}|[|]")) }
            .map { it.trim() }.filter { it.isNotBlank() }
        var name = pieces.firstOrNull { candidate ->
            !weekRange.containsMatchIn(candidate) &&
                !candidate.matches(Regex("第?\\d+(?:[-~～—–至]\\d+)?节")) &&
                !teacherLabel.containsMatchIn(candidate) && !roomLabel.containsMatchIn(candidate)
        }.orEmpty()
        name = cleanCourseName(name)
        if (name.length < 2) return null
        return buildCourse(name, cell, day, start, span, totalWeeks)
    }

    private fun buildCourse(
        name: String,
        metadata: String,
        day: Int,
        start: Int,
        span: Int,
        totalWeeks: Int,
    ): CourseSession {
        val weeks = weekRange.find(metadata)
        val startWeek = weeks?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(1, totalWeeks) ?: 1
        val endWeek = (weeks?.groupValues?.get(2)?.toIntOrNull() ?: startWeek.takeIf { weeks != null } ?: totalWeeks)
            .coerceIn(startWeek, totalWeeks)
        val pattern = when {
            Regex("单\\s*周|\\(单\\)|（单）").containsMatchIn(metadata) -> WeekPattern.ODD
            Regex("双\\s*周|\\(双\\)|（双）").containsMatchIn(metadata) -> WeekPattern.EVEN
            else -> WeekPattern.EVERY
        }
        val teacher = teacherLabel.find(metadata)?.groupValues?.get(1)?.trim()
            ?: metadata.lines().firstOrNull { it.endsWith("老师") }?.trim().orEmpty()
        val room = roomLabel.find(metadata)?.groupValues?.get(1)?.trim()
            ?: metadata.lines().firstOrNull { looksLikeRoom(it) }?.trim().orEmpty()
        return CourseSession(
            name = name,
            teacher = teacher,
            room = room,
            dayOfWeek = day,
            startPeriod = start,
            periodSpan = span.coerceAtLeast(1),
            startWeek = startWeek,
            endWeek = endWeek,
            weekPattern = pattern,
        )
    }

    private fun cleanCourseName(value: String): String = value
        .replace(Regex("^(课程|课程名称)\\s*[:：]\\s*"), "")
        .replace(teacherLabel, "")
        .replace(roomLabel, "")
        .replace(weekRange, "")
        .trim(' ', ',', '，', ';', '；', '-', '—', ':', '：')
        .take(36)

    private fun looksLikeRoom(value: String): Boolean =
        Regex("(?:楼|室|馆|场|中心|校区).{0,12}(?:\\d{2,4}|[A-Za-z]\\d+)|[A-Za-z][-—]?\\d{2,4}").containsMatchIn(value)

    private fun parsePeriod(value: String): Pair<Int, Int>? {
        val match = periodToken.find(value) ?: return null
        val start = match.groupValues[1].toIntOrNull() ?: return null
        val end = match.groupValues[2].toIntOrNull() ?: start
        return if (start in 1..30 && end in start..30) start to (end - start + 1) else null
    }

    private fun parseDay(value: String): Int? {
        val match = dayToken.find(value) ?: return null
        return when (match.groupValues[1].ifBlank { match.groupValues[2].lowercase() }) {
            "一", "mon" -> 1
            "二", "tue" -> 2
            "三", "wed" -> 3
            "四", "thu" -> 4
            "五", "fri" -> 5
            "六", "sat" -> 6
            "日", "天", "sun" -> 7
            else -> null
        }
    }

    private fun stableColor(name: String, fallback: Int): Int {
        val hash = name.fold(fallback) { acc, char -> acc * 31 + char.code }
        return Math.floorMod(hash, 12)
    }
}
