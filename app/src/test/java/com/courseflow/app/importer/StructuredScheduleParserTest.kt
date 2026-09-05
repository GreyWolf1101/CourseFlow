package com.courseflow.app.importer

import com.courseflow.app.model.WeekPattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StructuredScheduleParserTest {
    private val parser = StructuredScheduleParser()

    @Test fun `metadata blocks never become independent courses`() {
        val text = "节次\t星期一\t星期二\n1-2节\t王老师\t教学楼A101\n3-4节\t张三\t东区田径场"
        assertTrue(parser.parse(text).courses.isEmpty())
        assertTrue(parser.parse("张三 星期一 第1-2节 1-16周").courses.isEmpty())
    }

    @Test fun `title can follow metadata lines within a cell`() {
        val course = parser.parse("节次\t星期一\t星期二\n1-2节\t王老师  A101  高等数学  1-16周\t").courses.single()
        assertEquals("高等数学", course.name)
        assertEquals("王老师", course.teacher)
        assertEquals("A101", course.room)
    }

    @Test fun `weeks dates times and rooms are not periods`() {
        listOf("高等数学 周一 1-16周", "高等数学 周一 2026-09-07", "高等数学 周一 08:00 教室A101").forEach {
            assertTrue(it, parser.parse(it).courses.isEmpty())
        }
        val course = parser.parse("第3-4节 操作系统 星期三 2-18周").courses.single()
        assertEquals("操作系统", course.name)
        assertEquals(3, course.startPeriod)
    }

    @Test fun `explicit calendar date is converted to correct week across year boundary`() {
        val course = parser.parse("机器学习 2027-01-04 星期一 第3-4节", 20, LocalDate.of(2026, 9, 7)).courses.single()
        assertEquals(18, course.startWeek)
        assertEquals(18, course.endWeek)
        assertEquals(1, course.dayOfWeek)
    }

    @Test fun `invalid conflicting and out of semester dates are rejected`() {
        listOf("2026-09-08 星期一", "2026-02-30 星期一", "2026-08-31 星期一", "2026-09-07 星期一 第2周", "2026-09-07 星期一 第1周（双）").forEach {
            val result = parser.parse("高等数学 $it 第1-2节", 20, LocalDate.of(2026, 9, 7))
            assertTrue(it, result.courses.isEmpty())
            assertTrue(result.warnings.isNotEmpty())
        }
    }

    @Test fun `discontinuous weeks and parity do not fill gaps`() {
        val courses = parser.parse("高等数学 周一 第1-2节 1-4,7,9-12周（单）").courses
        assertEquals(3, courses.size)
        assertEquals(setOf(1, 3, 7, 9, 11), (1..20).filter { week -> courses.any { it.occursInWeek(week) } }.toSet())
        assertTrue(parser.parse("高等数学 周一 第1-2节 18-35周").courses.isEmpty())
        assertTrue(parser.parse("高等数学 周一 第1-2节 16-1周").courses.isEmpty())
    }

    @Test fun `header calendar date does not narrow explicit recurring weeks`() {
        val course = parser.parse("节次\t星期一 2026-09-07\t星期二 2026-09-08\n1-2节\t高等数学 1-16周\t", 20, LocalDate.of(2026, 9, 7)).courses.single()
        assertEquals(16, course.endWeek)
    }

    @Test fun `month day headers resolve across year and validate weekday`() {
        val course = parser.parse("节次\t星期一 1/4\t星期二 1/5\n1-2节\t高等数学\t", 20, LocalDate.of(2026, 9, 7)).courses.single()
        assertEquals(18, course.startWeek)
        assertEquals(18, course.endWeek)
        assertTrue(parser.parse("节次\t星期一 1/5\t星期二 1/6\n1-2节\t高等数学\t", 20, LocalDate.of(2026, 9, 7)).courses.isEmpty())
    }

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
