package com.courseflow.app.importer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Xml
import com.courseflow.app.model.SemesterConfig
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import kotlin.math.min

data class ImportResult(
    val fileName: String,
    val parsed: ParsedSchedule,
    val rawPreview: String,
)

class CourseImportService(private val context: Context) {
    private val parser = StructuredScheduleParser()

    suspend fun import(uri: Uri, config: SemesterConfig): ImportResult = withContext(Dispatchers.IO) {
        val fileName = displayName(uri)
        val extension = fileName.substringAfterLast('.', "").lowercase()
        val (text, readWarnings) = when (extension) {
            "docx" -> extractDocx(uri) to emptyList()
            "xlsx" -> extractXlsx(uri) to emptyList()
            "pdf" -> extractPdf(uri)
            "csv", "txt", "tsv" -> readPlainText(uri) to emptyList()
            "html", "htm" -> context.contentResolver.openInputStream(uri)!!.use { HtmlScheduleReader.read(it) } to emptyList()
            "doc", "xls" -> throw IllegalArgumentException("暂不支持旧版 .$extension 二进制格式，请在 Office/WPS 中另存为 DOCX 或 XLSX 后导入")
            else -> throw IllegalArgumentException("无法识别 .$extension 文件，请选择 DOCX、XLSX、PDF、HTML、CSV 或 TXT")
        }
        val original = parser.parse(text, config.totalWeeks, config.monday())
        val fieldWarnings = original.courses.groupBy { it.name }.filterValues { sessions ->
            sessions.map { it.teacher }.filter { it.isNotBlank() }.distinct().size > 1
        }.keys.map { "“$it”在不同单元格中的教师姓名不一致，请对照原表核对是否存在错字。" }
        val sourceYear = Regex("(20\\d{2})[-—](?:20\\d{2})").find(fileName)?.groupValues?.get(1)?.toIntOrNull()
        val dateWarnings = if (extension == "pdf") listOf(
            "PDF 中的学年、打印日期不能用作开学日期。以下日期按当前学期推算，请核对第一周周一。"
        ) + if (sourceYear != null && config.monday().year !in sourceYear..sourceYear + 1)
            listOf("文件属于 $sourceYear 学年，与当前 ${config.monday().year} 年学期不符，请在导入预览中修改第一周日期。") else emptyList() else emptyList()
        val courses = original.courses.filter { it.startPeriod + it.periodSpan - 1 <= config.periods.size }
        val parsed = original.copy(courses = courses, warnings = original.warnings +
            if (courses.size < original.courses.size) listOf("部分课程超出当前上课节数，已跳过；请先核对设置中的上课时间") else emptyList())
        ImportResult(
            fileName = fileName,
            parsed = parsed.copy(warnings = readWarnings + dateWarnings + fieldWarnings + parsed.warnings),
            rawPreview = text.take(2400),
        )
    }

    suspend fun importPassphrase(text: String, config: SemesterConfig = SemesterConfig()): ImportResult = withContext(Dispatchers.IO) {
        val parsed = if (ScheduleShareCodec.isLocal(text)) ScheduleShareCodec.decode(text)
            else ScheduleShareCodec.decodeWakeUp(WakeUpShareClient().fetch(ScheduleShareCodec.wakeUpKey(text)), config.periods)
        ImportResult(if (ScheduleShareCodec.isLocal(text)) "课序分享口令" else "WakeUp 分享口令", parsed, "")
    }

    private fun displayName(uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0)
        }
        return uri.lastPathSegment ?: "课表文件"
    }

    private fun readPlainText(uri: Uri): String =
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()

    private fun extractDocx(uri: Uri): String {
        val entries = readZipEntries(uri) { it == "word/document.xml" }
        val xml = entries["word/document.xml"] ?: error("DOCX 中缺少 word/document.xml")
        val pull = Xml.newPullParser().apply { setInput(ByteArrayInputStream(xml), "UTF-8") }
        val out = StringBuilder()
        var cellDepth = 0
        var event = pull.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && pull.name == "tc") cellDepth++
            if (event == XmlPullParser.START_TAG && pull.name == "t") out.append(pull.nextText())
            if (event == XmlPullParser.START_TAG && pull.name == "br") out.append("  ")
            if (event == XmlPullParser.END_TAG) when (pull.name) {
                "tc" -> {
                    out.append('\t')
                    cellDepth = (cellDepth - 1).coerceAtLeast(0)
                }
                "tr" -> out.append('\n')
                "p" -> if (cellDepth > 0) out.append("  ") else out.append('\n')
            }
            event = pull.next()
        }
        return out.toString()
    }

    private fun extractXlsx(uri: Uri): String {
        val entries = readZipEntries(uri) {
            it == "xl/sharedStrings.xml" || it == "xl/worksheets/sheet1.xml"
        }
        val shared = entries["xl/sharedStrings.xml"]?.let(::parseSharedStrings).orEmpty()
        val sheet = entries["xl/worksheets/sheet1.xml"] ?: error("XLSX 中没有找到第一个工作表")
        return parseWorksheet(sheet, shared)
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val pull = Xml.newPullParser().apply { setInput(ByteArrayInputStream(bytes), "UTF-8") }
        val values = mutableListOf<String>()
        var current: StringBuilder? = null
        var event = pull.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && pull.name == "si") current = StringBuilder()
            if (event == XmlPullParser.START_TAG && pull.name == "t") current?.append(pull.nextText())
            if (event == XmlPullParser.END_TAG && pull.name == "si") {
                values += current?.toString().orEmpty()
                current = null
            }
            event = pull.next()
        }
        return values
    }

    private fun parseWorksheet(bytes: ByteArray, shared: List<String>): String {
        val pull = Xml.newPullParser().apply { setInput(ByteArrayInputStream(bytes), "UTF-8") }
        val out = StringBuilder()
        var cellType = ""
        var cellRef = ""
        var value = ""
        var previousColumn = -1
        var event = pull.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) when (pull.name) {
                "row" -> previousColumn = -1
                "c" -> {
                    cellType = pull.getAttributeValue(null, "t").orEmpty()
                    cellRef = pull.getAttributeValue(null, "r").orEmpty()
                    value = ""
                }
                "v", "t" -> value += pull.nextText()
            }
            if (event == XmlPullParser.END_TAG) when (pull.name) {
                "c" -> {
                    val column = columnIndex(cellRef)
                    repeat((if (previousColumn < 0) column else column - previousColumn).coerceAtLeast(0)) { out.append('\t') }
                    val display = if (cellType == "s") shared.getOrNull(value.toIntOrNull() ?: -1).orEmpty() else value
                    out.append(display.replace('\t', ' ').replace("\r\n", "  ").replace("\n", "  "))
                    previousColumn = column
                }
                "row" -> out.append('\n')
            }
            event = pull.next()
        }
        return out.toString()
    }

    private fun columnIndex(reference: String): Int {
        val letters = reference.takeWhile { it.isLetter() }.uppercase()
        return letters.fold(0) { acc, char -> acc * 26 + (char - 'A' + 1) } - 1
    }

    private suspend fun extractPdf(uri: Uri): Pair<String, List<String>> {
        val descriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: error("无法打开 PDF")
        val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
        val pages = mutableListOf<String>()
        var pageCount = 0
        try {
            PdfRenderer(descriptor).use { renderer ->
                pageCount = renderer.pageCount
                val limit = min(renderer.pageCount, 20)
                for (index in 0 until limit) {
                    renderer.openPage(index).use { page ->
                        val scale = min(4f, 3400f / page.width.coerceAtLeast(1))
                        val bitmap = Bitmap.createBitmap(
                            (page.width * scale).toInt().coerceAtLeast(1),
                            (page.height * scale).toInt().coerceAtLeast(1),
                            Bitmap.Config.ARGB_8888,
                        )
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val grid = PdfTableCells.find(bitmap)
                        val cellTexts = mutableListOf<Pair<Int, List<String>>>()
                        for ((day, rect) in grid) {
                            val crop = Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height())
                            try {
                                val cellText = recognizer.process(InputImage.fromBitmap(crop, 0)).await().asLayoutText()
                                cellTexts += day to cellText.lines()
                            } finally { crop.recycle() }
                        }
                        val corroboratedNames = cellTexts.mapNotNull { RegistrarCellParser.className(it.second) }
                            .groupingBy { it }.eachCount().filterValues { it > 1 }.keys
                        val cellRecords = cellTexts.mapNotNull { (day, lines) -> RegistrarCellParser.record(lines, day, corroboratedNames) }
                        val recognizedHeaders = cellTexts.mapNotNull { (day, lines) ->
                            lines.singleOrNull()?.takeIf { Regex("(?:星期|周)[一二三四五六日天]").matches(it.trim()) }
                                ?.let { day to parser.parseDay(it) }
                        }
                        if (cellRecords.isNotEmpty() && recognizedHeaders.size >= 3 && recognizedHeaders.all { it.first == it.second }) {
                            pages += cellRecords.joinToString("\n")
                            bitmap.recycle()
                            return@use
                        }
                        val result = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
                        val layout = OcrScheduleLayout.reconstruct(result.textBlocks.flatMap { it.lines }.mapNotNull { line ->
                            line.boundingBox?.let { box -> OcrLine(line.text, box.left, box.top, box.right, box.bottom) }
                        })
                        pages += layout ?: result.asLayoutText()
                        bitmap.recycle()
                    }
                }
            }
        } finally {
            recognizer.close()
            descriptor.close()
        }
        val warnings = (if (pages.any { it.contains("其他课程") }) listOf("原表包含未注明固定星期、节次的其他课程，未自动排入周课表，请手动补充。") else emptyList()) + if (pageCount > 20) listOf("PDF 共 $pageCount 页，为保证速度仅识别了前 20 页") else emptyList()
        return pages.joinToString("\n\n") to (warnings + "PDF 优先按单元格内明确的节次和周次识别，教师和场地按字段提取；请核对模糊文字和跨页内容")
    }

    private fun Text.asLayoutText(): String = textBlocks
        .flatMap { it.lines }
        .sortedWith(compareBy<Text.Line> { it.boundingBox?.top ?: 0 }.thenBy { it.boundingBox?.left ?: 0 })
        .joinToString("\n") { it.text }

    private fun readZipEntries(uri: Uri, accept: (String) -> Boolean): Map<String, ByteArray> {
        val result = mutableMapOf<String, ByteArray>()
        val stream = context.contentResolver.openInputStream(uri) ?: error("无法读取文件")
        ZipInputStream(stream.buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && accept(entry.name)) {
                    val out = ByteArrayOutputStream()
                    zip.copyTo(out)
                    result[entry.name] = out.toByteArray()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return result
    }
}
