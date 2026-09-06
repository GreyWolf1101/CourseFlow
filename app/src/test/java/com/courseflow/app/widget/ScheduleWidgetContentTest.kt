package com.courseflow.app.widget
import com.courseflow.app.model.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test
class ScheduleWidgetContentTest {
 private val monday = LocalDate.of(2026,9,7)
 private val state = ScheduleState(SemesterConfig(startDate="2026-09-07",totalWeeks=2), listOf(
  CourseSession(name="单周课",dayOfWeek=1,startPeriod=1,weekPattern=WeekPattern.ODD),
  CourseSession(name="双周课",dayOfWeek=1,startPeriod=3,weekPattern=WeekPattern.EVEN)))
 @Test fun `semester boundaries do not repeat first or last week`() {
  assertEquals("学期尚未开始",ScheduleWidgetContent.day(state,monday.minusDays(1),"今天",2))
  assertEquals("本学期已结束",ScheduleWidgetContent.day(state,monday.plusWeeks(2),"今天",2))
 }
 @Test fun `tomorrow uses its own week and weekday`() {
  val sunday=monday.plusDays(6)
  assertTrue(ScheduleWidgetContent.day(state,sunday.plusDays(1),"明天",2).contains("双周课"))
  assertFalse(ScheduleWidgetContent.day(state,sunday.plusDays(1),"明天",2).contains("单周课"))
 }
 @Test fun `empty days and overflow are explicit`() {
  assertTrue(ScheduleWidgetContent.day(state,monday.plusDays(1),"明天",1).contains("明天没有课啦"))
  val many=state.copy(courses=state.courses.map { it.copy(weekPattern=WeekPattern.EVERY) })
  assertTrue(ScheduleWidgetContent.day(many,monday,"今天",1).contains("还有1门"))
 }
}
