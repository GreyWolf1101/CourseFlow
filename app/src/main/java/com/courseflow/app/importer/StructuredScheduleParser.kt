package com.courseflow.app.importer

import com.courseflow.app.model.CourseSession
import com.courseflow.app.model.SemesterConfig
import com.courseflow.app.model.WeekPattern
import com.courseflow.app.model.mergeContinuousCourses
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class ParsedSchedule(
    val courses: List<CourseSession>,
    val warnings: List<String> = emptyList(),
    val config: SemesterConfig? = null,
)

/** Table cells and labeled records share the same field and calendar validation. */
class StructuredScheduleParser {
    private val dayToken = Regex("(?:星期|周)([一二三四五六日天])|\\b(Mon(?:day)?|Tue(?:sday)?|Wed(?:nesday)?|Thu(?:rsday)?|Fri(?:day)?|Sat(?:urday)?|Sun(?:day)?)\\b", RegexOption.IGNORE_CASE)
    private val range = "(\\d{1,2})(?:\\s*[-~～—–至到]\\s*(\\d{1,2}))?"
    private val periodToken = Regex("(?<![\\d:/.-])第?\\s*$range\\s*节")
    private val barePeriod = Regex("^第?\\s*$range\\s*(?:节)?$")
    private val weeksToken = Regex("(?<![\\d/.-])第?\\s*(\\d{1,2}(?:\\s*[-~～—–至到]\\s*\\d{1,2})?(?:\\s*[,，、]\\s*\\d{1,2}(?:\\s*[-~～—–至到]\\s*\\d{1,2})?)*)\\s*(?:周|\\(周\\)|（周）)")
    private val dateToken = Regex("(?<!\\d)(\\d{4})[-/.年](\\d{1,2})[-/.月](\\d{1,2})日?(?!\\d)")
    private val shortDate = Regex("(?<!\\d)\\d{1,2}[/月]\\d{1,2}日?(?!\\d)")
    private val labels = "课程名称|课程名|课程|任课教师|授课教师|教师|老师|授课人|上课地点|地点|教室|场地|周次|上课时间|时间|节次|备注"
    private val labelToken = Regex("(?<![\\p{IsHan}A-Za-z])($labels)(?:\\s*[:：]\\s*|\\s+)")
    private val nameLabel = Regex("^(?:课程名称|课程名|课程)\\s*[:：]\\s*")
    private val roomToken = Regex(".*(?:教学楼|实训楼|实验楼|知行楼|博雅楼|艺术楼|教室|实验室|体育馆|体育场|田径场|操场|校区|线上课堂|[楼室馆场].*\\d|[A-Za-z][-—]?\\d{2,4}|\\d{1,2}[-—]\\d{3}).*")
    private val teacherToken = Regex("^[\\p{IsHan}A-Za-z·. ]{1,24}(?:老师|教师|教授|讲师)$")
    private val personToken = Regex("^[赵钱孙李周吴郑王冯陈褚卫蒋沈韩杨朱秦许何吕施张孔曹严华金魏陶姜谢邹喻柏窦章云苏潘葛范彭郎鲁韦马苗方俞任袁柳鲍史唐费薛雷贺倪汤滕殷罗毕郝安常傅卞齐康伍余顾孟黄萧尹姚邵汪毛戴宋熊纪舒屈项董梁杜阮蓝闵席季强贾路江郭林钟徐高夏蔡田胡霍万卢莫房裘缪解应宗丁宣邓洪石崔龚程陆翁]{1}[\\p{IsHan}]{1,2}$")
    private val shortCourses = setOf("数学", "语文", "英语", "物理", "化学", "生物", "历史", "地理", "政治", "体育", "美术", "音乐", "高数", "大学语文", "大学英语")

    fun parse(rawText: String, totalWeeks: Int = 20, semesterStart: LocalDate? = null): ParsedSchedule {
        require(totalWeeks in 1..30)
        val text = rawText.replace('\uFEFF', ' ').replace('\u00A0', ' ').replace("\r\n", "\n").replace('\r', '\n')
        val warnings = linkedSetOf<String>()
        val found = mutableListOf<CourseSession>()
        var dayColumns = emptyMap<Int, Int>()
        var headers = emptyList<String>()
        var recordHeaders = emptyList<String>()
        text.lines().forEach { line ->
            if (line.contains('\t')) {
                val row = line.split('\t')
                if (row.any { it.trim() in listOf("课程名称", "课程名", "课程") } && row.any { it.trim() in listOf("星期", "上课星期", "上课日期", "日期") }) {
                    recordHeaders = row.map { it.trim() }
                    dayColumns = emptyMap()
                    return@forEach
                }
                if (recordHeaders.isNotEmpty()) {
                    fun value(vararg labels: String) = row.getOrNull(recordHeaders.indexOfFirst { it in labels }).orEmpty().trim()
                    val dayValue = value("星期", "上课星期")
                    val day = parseDay(dayValue) ?: dayValue.toIntOrNull()?.takeIf { it in 1..7 }
                    val period = parsePeriod(value("节次", "上课节次"), true)
                    if (period != null) {
                        val weeks = value("周次", "周数", "上课周次").let { if (it.isNotBlank() && !it.contains("周")) "$it 周" else it }
                        val cell = "课程名称：${value("课程名称", "课程名", "课程")}  教师：${value("教师", "老师", "任课教师")}  教室：${value("教室", "地点", "上课地点")}  $weeks  ${value("日期", "上课日期")}"
                        found += parseCell(cell, day, period, totalWeeks, semesterStart, warnings)
                    }
                    return@forEach
                }
                val days = row.mapIndexedNotNull { column, value ->
                    // A header must contain only a weekday and optional date, never a course record.
                    val stripped = value.replace(dateToken, "").replace(shortDate, "").trim(' ', '(', ')', '（', '）')
                    dayToken.matchEntire(stripped)?.let { column to parseDay(stripped)!! }
                }.toMap()
                if (days.isNotEmpty()) {
                    dayColumns = days
                    headers = row
                } else if (dayColumns.isNotEmpty()) {
                    val prefix = row.take(dayColumns.keys.min()).map { it.trim() }
                    val period = prefix.firstNotNullOfOrNull { parsePeriod(it, allowBare = true) }
                    if (period != null) dayColumns.forEach { (column, day) ->
                        val cell = row.getOrNull(column).orEmpty()
                        if (cell.isNotBlank()) {
                            val headerDate = headers.getOrElse(column) { "" }
                            found += parseCell(cell, day, period, totalWeeks, semesterStart, warnings, headerDate)
                        }
                    }
                }
            } else if (line.isBlank()) {
                dayColumns = emptyMap()
                recordHeaders = emptyList()
            } else {
                val day = parseDay(line)
                val period = parsePeriod(line)
                if (period != null && (day != null || dateToken.containsMatchIn(line))) {
                    found += parseCell(line, day, period, totalWeeks, semesterStart, warnings)
                }
            }
        }
        val distinct = found.distinctBy {
            listOf(it.name, it.teacher, it.room, it.dayOfWeek, it.startPeriod, it.periodSpan, it.startWeek, it.endWeek, it.weekPattern)
        }
        val courses = mergeContinuousCourses(distinct).map { it.copy(colorIndex = Math.floorMod(it.name.hashCode(), 12)) }
        if (courses.isEmpty()) warnings += "没有自动定位到可靠课程，请确认包含课程名称、星期和明确节次；教师、教室等信息不会作为课程导入"
        if (courses.isNotEmpty() && semesterStart != null) warnings += "日期按第一周周一 $semesterStart 计算，请核对学期起始日期"
        return ParsedSchedule(courses, warnings.toList())
    }

    private fun parseCell(
        cell: String, columnDay: Int?, period: Pair<Int, Int>, totalWeeks: Int,
        semesterStart: LocalDate?, warnings: MutableSet<String>, headerDate: String = "",
    ): List<CourseSession> {
        val segments = cell.split(Regex("\\s*\\|\\|\\s*"))
        if (segments.size > 1) return segments.flatMap { parseCell(it, columnDay, period, totalWeeks, semesterStart, warnings, headerDate) }
        val fields = labelToken.findAll(cell).toList()
        fun field(vararg names: String): String {
            val index = fields.indexOfFirst { it.groupValues[1] in names }
            if (index < 0) return ""
            val start = fields[index].range.last + 1
            val end = fields.getOrNull(index + 1)?.range?.first ?: cell.length
            return cell.substring(start, end).split(Regex("[|;；\\n]|\\s{2,}")).first().trim()
                .replace(weeksToken, "").replace(periodToken, "").replace(dayToken, "").replace(dateToken, "").trim(' ', ',', '，')
        }
        val explicitName = field("课程名称", "课程名", "课程")
        val unlabeled = cell.substring(0, fields.firstOrNull()?.range?.first ?: cell.length)
            .replace(dateToken, "  ").replace(shortDate, "  ").replace(dayToken, "  ")
            .replace(periodToken, "  ").replace(weeksToken, "  ")
            .replace(Regex("[（(]?[单双]周?[)）]?"), "  ")
        val pieces = unlabeled.split(Regex("\\s{2,}|[|;；\\n]")).map { it.trim(' ', ',', '，', ':', '：') }.filter { it.isNotBlank() }
        val name = explicitName.ifBlank { pieces.firstOrNull { isCourseName(it) }.orEmpty() }.replace(nameLabel, "")
        if (name.isBlank() || (explicitName.isBlank() && !isCourseName(name))) {
            if (cell.isNotBlank()) warnings += "已跳过无法确认课程名称的单元格（可能只有教师、教室或时间）"
            return emptyList()
        }
        val teacher = field("任课教师", "授课教师", "教师", "老师", "授课人").ifBlank {
            pieces.firstOrNull { it != name && (teacherToken.matches(it) || personToken.matches(it)) }.orEmpty()
        }
        val room = field("上课地点", "地点", "教室", "场地").ifBlank { pieces.firstOrNull { roomToken.matches(it) }.orEmpty() }
        val inlineDay = parseDay(cell)
        if (inlineDay != null && columnDay != null && inlineDay != columnDay) {
            warnings += "“$name”的星期与所在列冲突，已跳过，请检查原课表"
            return emptyList()
        }
        var day = inlineDay ?: columnDay
        val cellDates = dateToken.findAll(cell).toList()
        val dateMatches = cellDates.ifEmpty { dateToken.findAll(headerDate).toList() }
        val headerShort = if (dateMatches.isEmpty() && semesterStart != null) shortDate.find(headerDate)?.value else null
        val inferredHeaderDate = headerShort?.let { token ->
            val numbers = Regex("\\d+").findAll(token).map { it.value.toInt() }.toList()
            (semesterStart!!.year..semesterStart.year + 1).mapNotNull { year ->
                runCatching { LocalDate.of(year, numbers[0], numbers[1]) }.getOrNull()
            }.filter { it >= semesterStart && it < semesterStart.plusWeeks(totalWeeks.toLong()) }.singleOrNull()
        }
        if (headerShort != null && inferredHeaderDate == null) {
            warnings += "“$name”的表头日期不在当前学期内，已跳过；请核对第一周日期"
            return emptyList()
        }
        var dateWeek: Int? = null
        if (dateMatches.isNotEmpty() || inferredHeaderDate != null) {
            val dates = if (inferredHeaderDate != null) listOf(inferredHeaderDate) else dateMatches.map { m -> runCatching { LocalDate.of(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt()) }.getOrNull() }
            val date = dates.singleOrNull()
            if (date == null || semesterStart == null || (day != null && date.dayOfWeek.value != day)) {
                warnings += "“$name”的日期无效、存在多个日期、缺少学期起点或与星期冲突，已跳过"
                return emptyList()
            }
            day = date.dayOfWeek.value
            dateWeek = (Math.floorDiv(ChronoUnit.DAYS.between(semesterStart, date), 7) + 1).toInt()
            if (dateWeek !in 1..totalWeeks) {
                warnings += "“$name”的日期不在当前学期内，已跳过；请核对第一周日期"
                return emptyList()
            }
        } else if (shortDate.containsMatchIn(cell) || shortDate.containsMatchIn(headerDate)) {
            warnings += "“$name”的月日缺少年份，已跳过；请补全日期，避免跨年误判"
            return emptyList()
        }
        if (day == null) return emptyList()
        val pattern = when {
            Regex("单\\s*周|[（(]单[)）]").containsMatchIn(cell) -> WeekPattern.ODD
            Regex("双\\s*周|[（(]双[)）]").containsMatchIn(cell) -> WeekPattern.EVEN
            else -> WeekPattern.EVERY
        }
        val weekMatches = weeksToken.findAll(cell).toList()
        val ranges = weekMatches.flatMap { m -> m.groupValues[1].split(Regex("[,，、]")).map { part ->
            val numbers = Regex("\\d+").findAll(part).map { it.value.toInt() }.toList()
            numbers.first() to numbers.last()
        } }
        if (ranges.any { (start, end) -> start !in 1..totalWeeks || end !in start..totalWeeks }) {
            warnings += "“$name”的周次超出学期或顺序错误，已跳过，没有自动修改为其他周次"
            return emptyList()
        }
        // Header dates identify the displayed week; explicit cell week rules remain recurring.
        if (cellDates.isEmpty() && ranges.isNotEmpty()) dateWeek = null
        if (dateWeek != null && (ranges.isNotEmpty() && ranges.none { dateWeek in it.first..it.second } || !pattern.includes(dateWeek))) {
            warnings += "“$name”的日期与周次规则冲突，已跳过"
            return emptyList()
        }
        val finalRanges = if (dateWeek != null) listOf(dateWeek to dateWeek) else ranges.ifEmpty { listOf(1 to totalWeeks) }
        if (ranges.isEmpty() && dateWeek == null) warnings += "“$name”未提供周次，暂按第1—${totalWeeks}周${pattern.label}，请在预览中核对"
        val coursePeriod = parsePeriod(cell) ?: period
        return finalRanges.map { (startWeek, endWeek) ->
            CourseSession(name = name, teacher = teacher, room = room, dayOfWeek = day,
                startPeriod = coursePeriod.first, periodSpan = coursePeriod.second, startWeek = startWeek, endWeek = endWeek, weekPattern = pattern)
        }
    }

    internal fun isCourseName(value: String): Boolean {
        val name = value.trim()
        return name.length in 2..60 && name !in setOf("上午", "下午", "晚上", "午休", "课程表", "节次", "时间", "教师", "教室", "课程名称") &&
            !Regex("教学班|考核方式|核方式|选课备注|课程学时|总学时|周学时|学分[:：]|[/／]").containsMatchIn(name) &&
            !labelToken.containsMatchIn(name) && !teacherToken.matches(name) && !roomToken.matches(name) &&
            (!personToken.matches(name) || name in shortCourses) && !Regex("^[\\d\\s:：./()（）-]+$").matches(name) &&
            !weeksToken.containsMatchIn(name) && !periodToken.containsMatchIn(name) && !dateToken.containsMatchIn(name)
    }

    internal fun parsePeriod(value: String, allowBare: Boolean = false): Pair<Int, Int>? {
        val match = periodToken.find(value) ?: if (allowBare) barePeriod.matchEntire(value.trim()) else null
        match ?: return null
        val start = match.groupValues[1].toInt()
        val end = match.groupValues[2].toIntOrNull() ?: start
        return if (start in 1..30 && end in start..30) start to end - start + 1 else null
    }

    internal fun parseDay(value: String): Int? {
        val match = dayToken.find(value) ?: return null
        return when (match.groupValues[1].ifBlank { match.groupValues[2].take(3).lowercase() }) {
            "一", "mon" -> 1; "二", "tue" -> 2; "三", "wed" -> 3; "四", "thu" -> 4
            "五", "fri" -> 5; "六", "sat" -> 6; "日", "天", "sun" -> 7; else -> null
        }
    }
}
