package com.courseflow.app.importer

import com.courseflow.app.data.toJson
import com.courseflow.app.data.toScheduleState
import com.courseflow.app.model.CourseSession
import com.courseflow.app.model.PeriodDefinition
import com.courseflow.app.model.ScheduleState
import com.courseflow.app.model.SemesterConfig
import com.courseflow.app.model.WeekPattern
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object ScheduleShareCodec {
    private const val MAX_BYTES = 1024 * 1024
    private val localToken = Regex("CF1\\.([A-Za-z0-9_-]+)\\.([a-fA-F0-9]{8})(?![a-fA-F0-9])")
    private val wakeToken = Regex("(?<![a-fA-F0-9])([a-fA-F0-9]{32})(?![a-fA-F0-9])")

    fun encode(state: ScheduleState): String {
        validate(state)
        val bytes = state.toJson().toString().toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_BYTES) { "课表内容过大，无法生成口令" }
        val compressed = ByteArrayOutputStream().also { out -> GZIPOutputStream(out).use { it.write(bytes) } }.toByteArray()
        val checksum = CRC32().apply { update(compressed) }.value.toString(16).padStart(8, '0')
        return "【课序课表分享】\nCF1.${Base64.getUrlEncoder().withoutPadding().encodeToString(compressed)}.$checksum\n复制完整口令，在课序中选择“口令导入”。"
    }

    fun isLocal(text: String): Boolean = text.contains("CF1.")
    fun wakeUpKey(text: String): String = wakeToken.find(text)?.groupValues?.get(1)?.lowercase()
        ?: throw IllegalArgumentException("未找到 WakeUp 口令，请粘贴完整分享文案或32位分享口令")

    fun decode(text: String): ParsedSchedule {
        require(text.length <= MAX_BYTES * 2) { "口令过长，请检查内容" }
        val match = localToken.find(text) ?: error("课序口令不完整，请重新复制完整口令")
        val bytes = Base64.getUrlDecoder().decode(match.groupValues[1])
        require(CRC32().apply { update(bytes) }.value == match.groupValues[2].toLong(16)) { "口令校验失败，内容可能被截断或修改" }
        val json = GZIPInputStream(ByteArrayInputStream(bytes)).use { stream ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                require(out.size() + count <= MAX_BYTES) { "口令内容过大" }
                out.write(buffer, 0, count)
            }
            out.toString("UTF-8")
        }
        val state = JSONObject(json).toScheduleState()
        validate(state)
        return ParsedSchedule(state.courses.map { it.copy(id = UUID.randomUUID().toString()) }, config = state.config)
    }

    /** WakeUp shares contain five newline-separated JSON values; join courses by ID. */
    fun decodeWakeUp(shareData: String): ParsedSchedule {
        require(shareData.length <= MAX_BYTES) { "WakeUp 课表内容过大" }
        val parts = shareData.lines().filter { it.isNotBlank() }
        require(parts.size >= 5) { "WakeUp 分享数据不完整，请重新分享" }
        val times = JSONArray(parts[1])
        val settings = JSONObject(parts[2])
        val bases = JSONArray(parts[3])
        val details = JSONArray(parts[4])
        val coursesById = (0 until bases.length()).associate { bases.getJSONObject(it).let { item -> item.getInt("id") to item } }
        val periods = (0 until times.length()).map { index ->
            val time = times.getJSONObject(index)
            val start = LocalTime.parse(time.getString("startTime"), DateTimeFormatter.ofPattern("H:mm"))
            val end = LocalTime.parse(time.getString("endTime"), DateTimeFormatter.ofPattern("H:mm"))
            PeriodDefinition(time.getInt("node"), start.toString(), ChronoUnit.MINUTES.between(start, end).toInt())
        }.sortedBy { it.index }
        val startDate = LocalDate.parse(settings.getString("startDate"), DateTimeFormatter.ofPattern("uuuu-M-d").withResolverStyle(ResolverStyle.STRICT))
        val config = SemesterConfig(name = settings.optString("tableName", "WakeUp 课表"), startDate = startDate.toString(),
            totalWeeks = settings.getInt("maxWeek"), periods = periods)
        val courses = (0 until details.length()).map { index ->
            val detail = details.getJSONObject(index)
            val base = coursesById[detail.getInt("id")] ?: error("WakeUp 课程名称数据缺失")
            CourseSession(name = base.getString("courseName"), teacher = detail.optString("teacher"), room = detail.optString("room"),
                note = base.optString("note"), dayOfWeek = detail.getInt("day"), startPeriod = detail.getInt("startNode"),
                periodSpan = detail.getInt("step"), startWeek = detail.getInt("startWeek"), endWeek = detail.getInt("endWeek"),
                weekPattern = when (detail.getInt("type")) { 0 -> WeekPattern.EVERY; 1 -> WeekPattern.ODD; 2 -> WeekPattern.EVEN; else -> error("WakeUp 单双周类型不支持") },
                colorIndex = Math.floorMod(base.getString("courseName").hashCode(), 12))
        }
        validate(ScheduleState(config, courses))
        return ParsedSchedule(courses, config = config.copy(startDate = config.monday().toString()))
    }

    private fun validate(state: ScheduleState) {
        val config = state.config
        LocalDate.parse(config.startDate)
        require(config.totalWeeks in 1..30 && config.periods.size in 1..30) { "课表周数或节数超出支持范围（1—30）" }
        require(config.periods.map { it.index } == (1..config.periods.size).toList()) { "上课节次不连续，请先修正源课表" }
        config.periods.forEach { LocalTime.parse(it.startTime); require(it.durationMinutes in 1..240) { "上课时间无效" } }
        require(state.courses.size <= 3000) { "课程数量过多" }
        state.courses.forEach {
            require(it.name.isNotBlank() && it.name.length <= 200 && it.dayOfWeek in 1..7 &&
                it.startPeriod in 1..config.periods.size && it.periodSpan >= 1 && it.startPeriod.toLong() + it.periodSpan - 1 <= config.periods.size &&
                it.startWeek in 1..config.totalWeeks && it.endWeek in it.startWeek..config.totalWeeks) { "课程“${it.name.take(30)}”的星期、节次或周数无效" }
        }
    }
}
