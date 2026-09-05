package com.courseflow.app.importer
import org.junit.Assert.*
import org.junit.Test
class RegistrarCellParserTest {
 @Test fun `wrapped fields tolerate lost punctuation without becoming courses`() {
  val lines = listOf("离教数学★", "(1-2节)1-14周/校区:西校区", "|/场地XGD楼-303阶教师:王", "老师/教学班:离散数学-", "0005/教学班组成:软件工程")
  val course = StructuredScheduleParser().parse(RegistrarCellParser.record(lines, 3, setOf("离散数学"))!!).courses.single()
  assertEquals("离散数学", course.name)
  assertEquals("王老师", course.teacher)
  assertEquals("XGD楼-303阶", course.room)
  assertEquals(3, course.dayOfWeek)
  assertEquals(2, course.periodSpan)
 }
 @Test fun `uncorroborated class spelling does not override heading`() {
  val lines = listOf("大学体育(三)★", "(1-2节)1-16周/场地体育馆/教师", "李老师教学班:大学体有(三)-0038")
  val course = StructuredScheduleParser().parse(RegistrarCellParser.record(lines, 2)!!).courses.single()
  assertEquals("大学体育(三)", course.name)
  assertEquals("李老师", course.teacher)
  assertEquals("体育馆", course.room)
 }
}
