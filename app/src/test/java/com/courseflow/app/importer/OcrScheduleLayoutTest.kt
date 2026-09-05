package com.courseflow.app.importer

import org.junit.Assert.*
import org.junit.Test

class OcrScheduleLayoutTest {
    @Test fun `separate OCR blocks for teacher and room stay in same course cell`() {
        val lines = listOf(
            OcrLine("星期一", 100, 10, 180, 30), OcrLine("星期二", 300, 10, 380, 30), OcrLine("星期三", 500, 10, 580, 30),
            OcrLine("1-2节", 0, 140, 35, 160), OcrLine("3-4节", 0, 340, 35, 360),
            OcrLine("高等数学", 100, 90, 180, 110), OcrLine("王老师", 100, 120, 160, 140),
            OcrLine("教学楼A101", 100, 150, 190, 170), OcrLine("1-16周", 100, 180, 180, 200),
            OcrLine("周老师", 300, 90, 360, 110), OcrLine("B202", 300, 120, 360, 140))
        val text = OcrScheduleLayout.reconstruct(lines)!!
        val course = StructuredScheduleParser().parse(text).courses.single()
        assertEquals("高等数学", course.name)
        assertEquals("王老师", course.teacher)
        assertEquals("教学楼A101", course.room)
        assertEquals(2, course.periodSpan)
        assertEquals(1, course.dayOfWeek)
    }

    @Test fun `missing grid anchors returns no invented geometry`() {
        assertNull(OcrScheduleLayout.reconstruct(listOf(OcrLine("高等数学", 10, 10, 100, 50))))
    }
    @Test fun `registrar merged cells keep wrapped metadata and explicit periods`() {
        val lines = mutableListOf(
            OcrLine("星期一", 100, 10, 180, 30), OcrLine("星期二", 300, 10, 380, 30), OcrLine("星期三", 500, 10, 580, 30),
            OcrLine("1", 0, 50, 35, 70), OcrLine("2", 0, 130, 35, 150))
        listOf("大学体育(三)★", "(1-2节)1-16周/校区:西校区", "/场地:西校健美操408/教师", ":王老师/教学班:大学体育", "(三)-0038/考核方式:考查", "大学英语(三)★", "(3-4节)1-15周(单)/校区:西", "校区/场地:XGA楼-106/教师:李", "老师/教学班:大学英语/学分:3.0").forEachIndexed { index, value ->
            lines += OcrLine(value, 280, 50 + index * 25, 440, 70 + index * 25)
        }
        val courses = StructuredScheduleParser().parse(OcrScheduleLayout.reconstruct(lines)!!).courses
        assertEquals(2, courses.size)
        val sport = courses.single { it.name == "大学体育(三)★" }
        assertEquals(2, sport.dayOfWeek)
        assertEquals(2, sport.periodSpan)
        assertEquals(16, sport.endWeek)
        assertEquals("王老师", sport.teacher)
        assertEquals("西校健美操408", sport.room)
        val english = courses.single { it.name == "大学英语(三)★" }
        assertEquals(3, english.startPeriod)
        assertEquals(2, english.periodSpan)
        assertEquals("李老师", english.teacher)
        assertEquals(com.courseflow.app.model.WeekPattern.ODD, english.weekPattern)
    }
}
