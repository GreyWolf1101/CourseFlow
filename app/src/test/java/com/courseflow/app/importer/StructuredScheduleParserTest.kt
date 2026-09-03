package com.courseflow.app.importer

import com.courseflow.app.model.WeekPattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StructuredScheduleParserTest {
    private val parser = StructuredScheduleParser()

    @Test
    fun `parses a timetable matrix`() {
        val text = """
            节次\t星期一\t星期二
            1-2节\t高等数学  教师：王老师  教室：A101  第1-16周\t大学英语  教师：Mia  教室：B203  第1-18周（双）
            3-4节\t\t数据结构  教师：周老师  教室：C305  第2-17周
        """.trimIndent().replace("\\t", "\t")

        val result = parser.parse(text, 20)
        assertEquals(3, result.courses.size)
        val english = result.courses.first { it.name == "大学英语" }
        assertEquals(2, english.dayOfWeek)
        assertEquals(2, english.periodSpan)
        assertEquals(WeekPattern.EVEN, english.weekPattern)
        assertEquals("Mia", english.teacher)
        assertEquals("B203", english.room)
    }

    @Test
    fun `parses labeled OCR line`() {
        val result = parser.parse("操作系统 星期三 第3-4节 第2-18周（单） 教师：李老师 教室：知行楼301", 20)
        assertEquals(1, result.courses.size)
        result.courses.single().also {
            assertEquals("操作系统", it.name)
            assertEquals(3, it.dayOfWeek)
            assertEquals(3, it.startPeriod)
            assertEquals(2, it.periodSpan)
            assertEquals(WeekPattern.ODD, it.weekPattern)
            assertEquals("李老师", it.teacher)
            assertTrue(it.room.contains("知行楼301"))
        }
    }
}
