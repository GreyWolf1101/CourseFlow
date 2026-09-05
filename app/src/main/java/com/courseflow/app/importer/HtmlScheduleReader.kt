package com.courseflow.app.importer

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.io.InputStream

/** Reads text only: scripts, network resources and event handlers are never executed. */
object HtmlScheduleReader {
    fun read(html: String): String = extract(Jsoup.parse(html))
    fun read(stream: InputStream): String = extract(Jsoup.parse(stream, null, ""))

    private fun extract(document: Element): String {
        document.select("script, style, noscript, template, [hidden]").remove()
        val tables = document.select("table").filter { table -> table.parents().none { it.tagName() == "table" } }
        if (tables.isEmpty()) return text(document).replace("  ", "\n")
        return tables.joinToString("\n\n") { table ->
            val grid = mutableMapOf<Pair<Int, Int>, String>()
            val rows = table.select("tr").filter { it.parents().firstOrNull { p -> p.tagName() == "table" } == table }
            require(rows.size <= 1000) { "HTML 表格行数过多，请只导出课表区域" }
            rows.forEachIndexed { rowIndex, row ->
                var column = 0
                row.children().filter { it.tagName() in listOf("td", "th") }.forEach { cell ->
                    while (grid.containsKey(rowIndex to column)) column++
                    val rowSpan = (cell.attr("rowspan").toIntOrNull() ?: 1).let { if (it == 0) rows.size - rowIndex else it }
                    val colSpan = cell.attr("colspan").toIntOrNull() ?: 1
                    require(rowSpan in 1..1000 && colSpan in 1..100 && column + colSpan <= 100) { "HTML 合并单元格范围过大" }
                    val value = text(cell).trim()
                    repeat(rowSpan.coerceAtMost(rows.size - rowIndex)) { dy ->
                        repeat(colSpan) { dx -> grid[(rowIndex + dy) to (column + dx)] = value }
                    }
                    column += colSpan
                }
            }
            val width = (grid.keys.maxOfOrNull { it.second } ?: 0) + 1
            rows.indices.joinToString("\n") { row -> (0 until width).joinToString("\t") { column -> grid[row to column].orEmpty() } }
        }
    }

    private fun text(node: Node): String = when (node) {
        is TextNode -> node.wholeText.replace(Regex("\\s+"), " ").replace('\u00A0', ' ')
        is Element -> {
            if (node.tagName() == "br") "  " else {
                val content = node.childNodes().joinToString("") { text(it) }
                // Separate multiple courses in one cell when the page marks them with <hr>.
                if (node.tagName() == "hr") " || " else if (node.isBlock) "  $content  " else content
            }
        }
        else -> ""
    }
}
