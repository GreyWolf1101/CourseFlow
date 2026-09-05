package com.courseflow.app.importer

import org.junit.Assert.*
import org.junit.Test

class HtmlScheduleReaderTest {
    @Test fun `merged cells preserve weekday positions and continuous periods`() {
        val html = """<table><tr><th>节次</th><th>星期一</th><th>星期二</th></tr>
            <tr><td>1</td><td rowspan="2">高等数学<br>教师：王老师<br>教室：A101<br>1-16周</td><td></td></tr>
            <tr><td>2</td><td>大学英语<br>1-18周（双）</td></tr></table>"""
        val courses = StructuredScheduleParser().parse(HtmlScheduleReader.read(html)).courses
        assertEquals(2, courses.size)
        val math = courses.single { it.name == "高等数学" }
        assertEquals(2, math.periodSpan)
        assertEquals("A101", math.room)
        val english = courses.single { it.name == "大学英语" }
        assertEquals(2, english.dayOfWeek)
        assertEquals(2, english.startPeriod)
    }

    @Test fun `HTML ignores scripts and decodes entities`() {
        val text = HtmlScheduleReader.read("<script>fake course</script><style>hidden</style><p>设计&amp;实践 星期一 第1-2节 1-16周</p>")
        assertFalse(text.contains("fake"))
        assertFalse(text.contains("hidden"))
        assertEquals("设计&实践", StructuredScheduleParser().parse(text).courses.single().name)
    }

    @Test fun `record shaped HTML uses named columns`() {
        val text = HtmlScheduleReader.read("<table><tr><th>课程名称</th><th>星期</th><th>节次</th><th>教师</th><th>教室</th><th>周次</th></tr><tr><td>计算机网络</td><td>3</td><td>5-6</td><td>周老师</td><td>B201</td><td>2-16</td></tr></table>")
        val course = StructuredScheduleParser().parse(text).courses.single()
        assertEquals("计算机网络", course.name)
        assertEquals("周老师", course.teacher)
        assertEquals(3, course.dayOfWeek)
        assertEquals(5, course.startPeriod)
        assertEquals(2, course.startWeek)
    }
}
