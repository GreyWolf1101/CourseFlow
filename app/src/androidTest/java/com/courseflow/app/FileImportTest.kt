package com.courseflow.app

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.courseflow.app.importer.CourseImportService
import com.courseflow.app.model.SemesterConfig
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FileImportTest {
    @Test fun xlsxAdjacentAndEmptyColumnsPreserveWeekdayAndMetadata() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.cacheDir, "column-regression.xlsx")
        try {
            ZipOutputStream(file.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
                zip.write("""<worksheet><sheetData>
                    <row><c r="A1" t="inlineStr"><is><t>节次</t></is></c><c r="B1" t="inlineStr"><is><t>星期一</t></is></c><c r="C1" t="inlineStr"><is><t>星期二</t></is></c><c r="D1" t="inlineStr"><is><t>星期三</t></is></c></row>
                    <row><c r="A2" t="inlineStr"><is><t>1-2节</t></is></c><c r="B2" t="inlineStr"><is><t>高等数学
                    教师：王老师
                    教室：A101
                    1-16周</t></is></c><c r="D2" t="inlineStr"><is><t>大学英语  1-18周</t></is></c></row>
                    </sheetData></worksheet>""".trimIndent().toByteArray())
                zip.closeEntry()
            }
            val parsed = CourseImportService(context).import(Uri.fromFile(file), SemesterConfig(startDate = "2026-09-07")).parsed
            assertEquals(2, parsed.courses.size)
            assertEquals(1, parsed.courses.single { it.name == "高等数学" }.dayOfWeek)
            assertEquals("王老师", parsed.courses.single { it.name == "高等数学" }.teacher)
            assertEquals(3, parsed.courses.single { it.name == "大学英语" }.dayOfWeek)
        } finally { file.delete() }
    }

    @Test fun htmlImportHonorsDeclaredChineseEncoding() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.cacheDir, "html-regression.html")
        try {
            file.writeBytes("<meta charset='gbk'><table><tr><th>节次</th><th>星期一</th><th>星期二</th></tr><tr><td>1-2节</td><td>高等数学<br>教师：王老师<br>1-16周</td><td></td></tr></table>".toByteArray(charset("GBK")))
            val parsed = CourseImportService(context).import(Uri.fromFile(file), SemesterConfig(startDate = "2026-09-07")).parsed
            assertEquals("高等数学", parsed.courses.single().name)
            assertEquals("王老师", parsed.courses.single().teacher)
        } finally { file.delete() }
    }
}
