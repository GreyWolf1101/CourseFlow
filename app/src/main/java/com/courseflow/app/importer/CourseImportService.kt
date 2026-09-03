package com.courseflow.app.importer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Xml
import com.google.android.gms.tasks.Task
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
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

data class ImportResult(
    val fileName: String,
    val parsed: ParsedSchedule,
    val rawPreview: String,
)

class CourseImportService(private val context: Context) {
    private val parser = StructuredScheduleParser()

    suspend fun import(uri: Uri, totalWeeks: Int): ImportResult = withContext(Dispatchers.IO) {
        val fileName = displayName(uri)
        val extension = fileName.substringAfterLast('.', "").lowercase()
        val (text, readWarnings) = when (extension) {
            "docx" -> extractDocx(uri) to emptyList()
            "xlsx" -> extractXlsx(uri) to emptyList()
            "pdf" -> extractPdf(uri)
            "csv", "txt", "tsv" -> readPlainText(uri) to emptyList()
            "doc", "xls" -> throw IllegalArgumentException("暂不支持旧版 .$extension 二进制格式，请在 Office/WPS 中另存为 DOCX 或 XLSX 后导入")
            else -> throw IllegalArgumentException("无法识别 .$extension 文件，请选择 DOCX、XLSX、PDF、CSV 或 TXT")
        }
        val parsed = parser.parse(text, totalWeeks)
        ImportResult(
            fileName = fileName,
            parsed = parsed.copy(warnings = readWarnings + parsed.warnings),
            rawPreview = text.take(2400),
        )
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
                    repeat((column - previousColumn - 1).coerceAtLeast(0)) { out.append('\t') }
                    val display = if (cellType == "s") shared.getOrNull(value.toIntOrNull() ?: -1).orEmpty() else value
                    out.append(display.replace('\t', ' ').replace("\r\n", "  ").replace('\n', ' '))
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
                        val scale = min(2f, 2200f / page.width.coerceAtLeast(1))
                        val bitmap = Bitmap.createBitmap(
                            (page.width * scale).toInt().coerceAtLeast(1),
                            (page.height * scale).toInt().coerceAtLeast(1),
                            Bitmap.Config.ARGB_8888,
                        )
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val result = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
                        pages += listOf(result.asSpatialRecords(), result.asLayoutText())
                            .filter { it.isNotBlank() }
                            .joinToString("\n")
                        bitmap.recycle()
                    }
                }
            }
        } finally {
            recognizer.close()
            descriptor.close()
        }
        val warnings = if (pageCount > 20) listOf("PDF 共 $pageCount 页，为保证速度仅识别了前 20 页") else emptyList()
        return pages.joinToString("\n\n") to warnings
    }

    private fun Text.asLayoutText(): String = textBlocks
        .flatMap { it.lines }
        .sortedWith(compareBy<Text.Line> { it.boundingBox?.top ?: 0 }.thenBy { it.boundingBox?.left ?: 0 })
        .joinToString("\n") { it.text }

    /**
     * Rebuilds simple timetable records from OCR geometry. This is the PDF fallback for
     * grid documents where the weekday is represented by the column rather than repeated
     * inside every course cell.
     */
    private fun Text.asSpatialRecords(): String {
        val exactDay = Regex("^(?:星期|周)([一二三四五六日天])$")
        val allLines = textBlocks.flatMap { it.lines }.filter { it.boundingBox != null }
        val headers = allLines.mapNotNull { line ->
            val token = exactDay.find(line.text.trim())?.groupValues?.get(1) ?: return@mapNotNull null
            val day = when (token) {
                "一" -> 1; "二" -> 2; "三" -> 3; "四" -> 4; "五" -> 5; "六" -> 6; else -> 7
            }
            Triple(day, line.boundingBox!!.centerX(), line.boundingBox!!.bottom)
        }.distinctBy { it.first }.sortedBy { it.second }
        if (headers.size < 3) return ""

        val headerBottom = headers.maxOf { it.third }
        val leftEdge = headers.minOf { it.second }
        val periodRegex = Regex("^(?:第)?(\\d{1,2})(?:节)?$")
        val periodMarks = allLines.mapNotNull { line ->
            val box = line.boundingBox ?: return@mapNotNull null
            val index = periodRegex.find(line.text.trim())?.groupValues?.get(1)?.toIntOrNull() ?: return@mapNotNull null
            if (box.centerX() >= leftEdge || box.top <= headerBottom || index !in 1..30) return@mapNotNull null
            index to box.top
        }.distinctBy { it.first }.sortedBy { it.first }
        if (periodMarks.size < 2) return ""

        val spacing = periodMarks.zipWithNext { first, second -> (second.second - first.second).coerceAtLeast(1) }
            .sorted().let { it[it.size / 2] }
        val dayChars = listOf("", "一", "二", "三", "四", "五", "六", "日")
        return textBlocks.mapNotNull { block ->
            val box = block.boundingBox ?: return@mapNotNull null
            val centerX = box.centerX()
            if (box.top <= headerBottom || centerX < leftEdge - spacing / 2) return@mapNotNull null
            if (block.lines.any { exactDay.matches(it.text.trim()) }) return@mapNotNull null
            val firstLine = block.lines.firstOrNull()?.text?.trim().orEmpty()
            if (firstLine.length < 2 || firstLine.matches(Regex("[\\d:：./-]+"))) return@mapNotNull null

            val day = headers.minByOrNull { abs(it.second - centerX) }?.first ?: return@mapNotNull null
            val start = periodMarks.minByOrNull { abs(it.second - box.top) }?.first ?: return@mapNotNull null
            val span = (box.height().toFloat() / spacing).roundToInt().coerceIn(1, 4)
            val metadata = block.text.replace('\n', ' ')
            "$firstLine 星期${dayChars[day]} 第$start-${start + span - 1}节 $metadata"
        }.distinct().joinToString("\n")
    }

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
