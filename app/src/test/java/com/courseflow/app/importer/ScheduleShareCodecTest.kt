package com.courseflow.app.importer

import com.courseflow.app.model.*
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject

class ScheduleShareCodecTest {
    @Test fun `native protocol matches upstream known vectors`() {
        assertEquals("0c030a0b0f090808070d05080a0c0a0e020a030704010706060b020f010904020a020303020708000d0507080d0b0206",
            WakeUpProtocol.hexEncode(WakeUpProtocol.encrypt("CourseFlow 测试 123", "@fG2SuLA")))
        assertEquals("4c354e00d687de53fc4da6e5c3d8f6dd6a56d07d2d7a3b6118ea092c2901145466b58247815e65d635f336bcb043a46d31cd22941af1d9898bc5ba70303e7ccb",
            WakeUpProtocol.key("1234567890", "530"))
    }
    private val state = ScheduleState(SemesterConfig(startDate = "2026-09-07"), listOf(
        CourseSession(name = "数据结构", teacher = "张老师", room = "教学楼301", note = "实验\n带电脑", dayOfWeek = 3,
            startPeriod = 3, periodSpan = 2, startWeek = 2, endWeek = 18, weekPattern = WeekPattern.EVEN)))

    @Test fun `native share round trip preserves calendar periods and course fields`() {
        val decoded = ScheduleShareCodec.decode(ScheduleShareCodec.encode(state))
        assertEquals(state.config, decoded.config)
        val course = decoded.courses.single()
        assertEquals(state.courses.single().copy(id = course.id), course)
        assertNotEquals(state.courses.single().id, course.id)
    }

    @Test fun `corrupted or truncated native tokens fail without partial import`() {
        val token = ScheduleShareCodec.encode(state)
        assertTrue(runCatching { ScheduleShareCodec.decode(token.replace("CF1.", "CF1.X")) }.isFailure)
        assertTrue(runCatching { ScheduleShareCodec.decode(token.substringBeforeLast('.')) }.isFailure)
    }

    @Test fun `WakeUp supports full share prose and raw key`() {
        val key = "abcdef0123456789abcdef0123456789"
        assertEquals(key, ScheduleShareCodec.wakeUpKey("这是来自WakeUp课程表的课表分享，分享口令为「${key.uppercase()}」"))
        assertEquals(key, ScheduleShareCodec.wakeUpKey(key))
        assertTrue(runCatching { ScheduleShareCodec.wakeUpKey("invalid") }.isFailure)
    }

    @Test fun `WakeUp imports source dates times ids and parity`() {
        val parsed = ScheduleShareCodec.decodeWakeUp(wakeData)
        assertEquals("2026-09-07", parsed.config!!.startDate)
        assertEquals("08:10", parsed.config!!.periods.first().startTime)
        assertEquals(50, parsed.config!!.periods.first().durationMinutes)
        val course = parsed.courses.single()
        assertEquals("高等数学", course.name)
        assertEquals(3, course.dayOfWeek)
        assertEquals(WeekPattern.EVEN, course.weekPattern)
        assertEquals(2, course.periodSpan)
        assertFalse(course.occursInWeek(3))
        assertTrue(course.occursInWeek(4))
    }

    @Test fun `WakeUp invalid course fields reject whole import`() {
        assertTrue(runCatching { ScheduleShareCodec.decodeWakeUp(wakeData.replace("\"day\":3", "\"day\":8")) }.isFailure)
        assertTrue(runCatching { ScheduleShareCodec.decodeWakeUp(wakeData.replace("\"id\":42,\"day\"", "\"id\":99,\"day\"")) }.isFailure)
    }

    @Test fun `WakeUp full protocol exchanges validate nonce and decode shared payload`() {
        var calls = 0
        val key = "abcdef0123456789abcdef0123456789"
        val client = WakeUpShareClient { path, body, _ ->
            calls++
            val params = body.split('&').filter { it.contains('=') }.associate { it.substringBefore('=') to java.net.URLDecoder.decode(it.substringAfter('='), "UTF-8") }
            if (path.endsWith("antispam")) {
                val plain = String(WakeUpProtocol.decrypt(WakeUpProtocol.hexDecode(params.getValue("data")), "@fG2SuLA"))
                val nonce = plain.split("##")[1]
                JSONObject().put("data", WakeUpProtocol.hexEncode(WakeUpProtocol.encrypt("$nonce##1234567890", nonce.take(5) + "#G4")))
            } else {
                assertEquals("/share_schedule/getv2", path)
                val rcKey = WakeUpProtocol.key("1234567890", "530")
                assertEquals("key=$key", String(WakeUpProtocol.rc4(java.util.Base64.getDecoder().decode(params.getValue("data")), rcKey)))
                val encoded = java.util.Base64.getEncoder().encodeToString(WakeUpProtocol.rc4(JSONObject().put("shareData", wakeData).toString().toByteArray(), rcKey))
                JSONObject().put("errNo", 0).put("data", JSONObject().put("data", encoded))
            }
        }
        assertEquals(wakeData, client.fetch(key))
        assertEquals(2, calls)
    }

    @Test fun `WakeUp expired response does not return courses`() {
        val client = WakeUpShareClient { _, _, _ -> JSONObject().put("errNo", 1) }
        assertTrue(runCatching { client.fetch("abcdef0123456789abcdef0123456789") }.isFailure)
    }

    private val wakeData = """
        {"name":"默认"}
        [{"node":1,"startTime":"8:10","endTime":"9:00"},{"node":2,"startTime":"9:10","endTime":"10:00"}]
        {"tableName":"秋季课表","startDate":"2026-9-7","maxWeek":20}
        [{"id":42,"courseName":"高等数学"}]
        [{"id":42,"day":3,"startNode":1,"step":2,"startWeek":2,"endWeek":16,"type":2,"teacher":"周老师","room":"A101"}]
    """.trimIndent()
}
