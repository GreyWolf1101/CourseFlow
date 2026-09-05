package com.courseflow.app

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.courseflow.app.data.ScheduleRepository
import com.courseflow.app.model.*
import com.courseflow.app.ui.CourseFlowApp
import com.courseflow.app.ui.theme.CourseFlowTheme
import com.courseflow.app.importer.ScheduleShareCodec
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class CourseEntryTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var repository: ScheduleRepository

    @Before fun prepare() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("courseflow_schedule", Context.MODE_PRIVATE).edit().clear().commit()
        repository = ScheduleRepository(context)
        repository.importCourses(emptyList(), true, SemesterConfig(startDate = LocalDate.now().toString()))
        compose.setContent { CourseFlowTheme { CourseFlowApp(repository) } }
    }

    @Test fun tappingSlotUsesViewedWeekDayAndPeriodAndPersistsWithoutSettings() {
        compose.onAllNodesWithText("+").assertCountEquals(0)
        compose.onNodeWithContentDescription("选择周次，当前第1周").performClick()
        compose.onNode(hasText("5") and hasAnyAncestor(isDialog())).performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("第5周，周三第3节，添加课程").performClick()
        compose.onNodeWithText("添加课程").assertIsDisplayed()
        compose.onNodeWithText("课程名称 *").performTextInput("点击添加测试")
        compose.onNodeWithText("保存课程").performClick()
        compose.runOnIdle {
            val course = repository.state.value.courses.single()
            assertEquals("点击添加测试", course.name)
            assertEquals(3, course.dayOfWeek)
            assertEquals(3, course.startPeriod)
            assertEquals(5, course.startWeek)
            assertEquals(20, course.endWeek)
        }
    }

    @Test fun homeCourseCanBeDeletedAndEmptySlotCanBeAddedAgain() {
        compose.runOnIdle { repository.saveCourse(CourseSession(name = "待删除课程", dayOfWeek = 3, startPeriod = 3)) }
        compose.onAllNodesWithText("待删除课程")[0].performClick()
        compose.onNodeWithText("删除", substring = false).performClick()
        compose.onNodeWithText("取消").performClick()
        compose.runOnIdle { assertEquals(1, repository.state.value.courses.size) }
        compose.onAllNodesWithText("待删除课程")[0].performClick()
        compose.onNodeWithText("删除", substring = false).performClick()
        compose.onNodeWithText("删除", substring = false).performClick()
        compose.runOnIdle { assertTrue(repository.state.value.courses.isEmpty()) }
        compose.onNodeWithContentDescription("第1周，周三第3节，添加课程").performClick()
        compose.onNodeWithText("添加课程").assertIsDisplayed()
    }

    @Test fun settingsDeletesOnlySelectedCoursesThenClearsWithoutChangingSemester() {
        compose.runOnIdle {
            repository.saveCourse(CourseSession(name = "高等数学", dayOfWeek = 1, startPeriod = 1))
            repository.saveCourse(CourseSession(name = "大学英语", dayOfWeek = 2, startPeriod = 3))
        }
        val config = repository.state.value.config
        compose.onNodeWithContentDescription("课表设置").performClick()
        compose.onNodeWithText("选择课程删除").performScrollTo().performClick()
        compose.onNodeWithContentDescription("选择删除高等数学，周一第1节").performClick()
        compose.onNodeWithText("删除所选（1）").performClick()
        compose.onNodeWithText("确认删除").performClick()
        compose.runOnIdle { assertEquals("大学英语", repository.state.value.courses.single().name) }
        compose.onNodeWithText("清空课表").performScrollTo().performClick()
        compose.onNodeWithText("取消").performClick()
        compose.runOnIdle { assertEquals(1, repository.state.value.courses.size) }
        compose.onNodeWithText("清空课表").performClick()
        compose.onNodeWithText("确认清空").performClick()
        compose.runOnIdle {
            val reloaded = ScheduleRepository(ApplicationProvider.getApplicationContext<Context>()).state.value
            assertTrue(reloaded.courses.isEmpty())
            assertEquals(config, reloaded.config)
        }
        compose.onNodeWithText("选择课程删除").assertIsNotEnabled()
        compose.onNodeWithText("清空课表").assertIsNotEnabled()
    }

    @Test fun nativePassphraseCanBePreviewedEditedAndImportedWithSourceCalendar() {
        val source = ScheduleState(SemesterConfig(startDate = "2026-09-07", periods = listOf(PeriodDefinition(1, "08:10", 50), PeriodDefinition(2, "09:10", 50))),
            listOf(CourseSession(name = "原课程", dayOfWeek = 1, startPeriod = 1)))
        compose.onNodeWithContentDescription("课表设置").performClick()
        compose.onNodeWithText("导入课表").performClick()
        compose.onNodeWithText("选择文件（含 HTML）").assertIsDisplayed()
        compose.onNodeWithText("口令导入 · 课序 / WakeUp").performClick()
        compose.onNodeWithText("分享口令").performTextInput(ScheduleShareCodec.encode(source))
        compose.onNodeWithText("解析并预览").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("导入预览").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("修改原课程").performClick()
        compose.onNodeWithText("课程名称 *").performTextReplacement("已修正课程")
        compose.onNodeWithText("保存课程").performClick()
        compose.onNodeWithText("已修正课程").assertIsDisplayed()
        compose.onNodeWithText("第一周周一（yyyy-MM-dd）").performScrollTo().performTextReplacement("2023-09-04")
        compose.onNodeWithText("覆盖导入").assertIsNotEnabled()
        compose.onNodeWithText("应用日期").performScrollTo().performClick()
        compose.onNodeWithText("覆盖导入").performClick()
        compose.runOnIdle {
            assertEquals("已修正课程", repository.state.value.courses.single().name)
            assertEquals("2023-09-04", repository.state.value.config.startDate)
            val reloaded = ScheduleRepository(ApplicationProvider.getApplicationContext<Context>()).state.value
            assertEquals(source.config.copy(startDate = "2023-09-04"), reloaded.config)
            assertEquals("已修正课程", reloaded.courses.single().name)
        }
    }
}
