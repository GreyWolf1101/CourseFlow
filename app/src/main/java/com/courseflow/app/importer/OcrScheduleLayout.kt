package com.courseflow.app.importer

import kotlin.math.abs

data class OcrLine(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val x: Int get() = (left + right) / 2
    val y: Int get() = (top + bottom) / 2
}

/** Group OCR lines by weekday and period, never treating a text block as a course. */
object OcrScheduleLayout {
    fun reconstruct(lines: List<OcrLine>): String? {
        val parser = StructuredScheduleParser()
        val dayHeader = Regex("^(?:星期|周)[一二三四五六日天](?:\\s*(?:\\d{4}[-/.年])?\\d{1,2}[/月.-]\\d{1,2}日?)?$")
        val headers = lines.filter { dayHeader.matches(it.text.trim()) }.sortedBy { it.top }
        if (headers.size < 3) return null
        // Use one header row only. Subsequent pages are processed independently.
        val firstY = headers.first().y
        val row = headers.filter { abs(it.y - firstY) <= (it.bottom - it.top) * 2 }
            .distinctBy { parser.parseDay(it.text) }.sortedBy { it.x }
        if (row.size < 3) return null
        val columnWidth = row.zipWithNext { a, b -> b.x - a.x }.sorted().let { it[it.size / 2] }
        val leftEdge = row.first().x - columnWidth / 2
        val rightEdge = row.last().x + columnWidth / 2
        val headerBottom = row.maxOf { it.bottom }
        // Dense registrar tables carry explicit periods inside merged cells. Their wrapped
        // metadata can extend past several period labels, so nearest-row assignment is unsafe.
        val columns = row.indices.map { column ->
            lines.filter { it.top > headerBottom && it.x in leftEdge..rightEdge &&
                it.right - it.left <= columnWidth * 1.35 &&
                row.indices.minBy { index -> abs(row[index].x - it.x) } == column }
                .sortedWith(compareBy<OcrLine> { it.top }.thenBy { it.left })
        }
        if (columns.flatten().any { it.text.contains("校区") || it.text.contains("教学班") }) {
            val records = columns.flatMapIndexed { column, content ->
                val anchors = content.indices.filter { parser.parsePeriod(content[it].text) != null }
                anchors.mapNotNull { index ->
                    val anchor = content[index]
                    val before = anchor.text.substringBefore(Regex("[（(]?\\d+\\s*[-—~～]?\\s*\\d*\\s*节").find(anchor.text)?.value ?: anchor.text)
                    val title = before.trim().ifBlank { content.getOrNull(index - 1)?.text.orEmpty().trim() }
                    if (!parser.isCourseName(title)) return@mapNotNull null
                    val next = anchors.firstOrNull { it > index }
                    val end = if (next != null) (next - 1).coerceAtLeast(index + 1) else content.size
                    val detail = content.subList(index, end).joinToString("") { it.text }.replace(Regex("\\s+"), "")
                    if (!Regex("\\d+周").containsMatchIn(detail)) return@mapNotNull null
                    fun field(label: String) = Regex("(?:^|[/／])$label[:：]([^/／]*)").find(detail)?.groupValues?.get(1).orEmpty()
                    val schedule = detail.substringBefore('/').substringBefore('／')
                    "${row[column].text}  课程名称：$title  $schedule  教师：${field("教师")}  教室：${field("场地").ifBlank { field("教室") }}"
                }
            }
            if (records.isNotEmpty()) return records.joinToString("\n")
        }
        val marks = lines.filter { it.right < leftEdge && it.top > headerBottom }
            .mapNotNull { line -> parser.parsePeriod(line.text, true)?.let { it to line } }
            .distinctBy { it.first.first }.sortedBy { it.second.y }
        if (marks.size < 2 || marks.zipWithNext().any { (a, b) -> a.first.first >= b.first.first }) return null
        val cells = mutableMapOf<Pair<Int, Int>, MutableList<OcrLine>>()
        val spacing = marks.zipWithNext { a, b -> b.second.y - a.second.y }.sorted().let { it[it.size / 2] }
        lines.filter { it.top > headerBottom && it.x in leftEdge..rightEdge && it.y <= marks.last().second.y + spacing / 2 }.forEach { line ->
            val column = row.indices.minBy { abs(row[it].x - line.x) }
            // Exclude OCR lines straddling multiple weekday columns instead of assigning arbitrarily.
            if (line.right - line.left > columnWidth * 1.35) return@forEach
            val mark = marks.indices.minBy { abs(marks[it].second.y - line.y) }
            cells.getOrPut(mark to column) { mutableListOf() } += line
        }
        return buildList {
            add("节次\t" + row.joinToString("\t") { it.text })
            marks.forEachIndexed { index, (period, _) ->
                add("第${period.first}-${period.first + period.second - 1}节\t" + row.indices.joinToString("\t") { column ->
                    cells[index to column].orEmpty().sortedWith(compareBy<OcrLine> { it.top }.thenBy { it.left }).joinToString("  ") { it.text }
                })
            }
        }.joinToString("\n")
    }
}
