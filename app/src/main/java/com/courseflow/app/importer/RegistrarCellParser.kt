package com.courseflow.app.importer

/** Registrar exports repeat the course name in the teaching-class identifier. */
internal object RegistrarCellParser {
    fun className(lines: List<String>): String? = Regex("教学班[:：,，]?(.+?)[-—]\\d{4}")
        .find(lines.drop(1).joinToString("").replace(Regex("[\\s|]+"), ""))?.groupValues?.get(1)

    fun record(lines: List<String>, day: Int, corroboratedNames: Set<String> = emptySet()): String? {
        if (lines.isEmpty()) return null
        val compact = lines.drop(1).joinToString("").replace(Regex("[\\s|]+"), "")
        if (StructuredScheduleParser().parsePeriod(compact) == null || !Regex("\\d+周").containsMatchIn(compact)) return null
        val labels = "教学班组成|教学班|教师|救师|场地|教室|校区|考核方式|选课备注|课程学时组成|周学时|总学时|学分"
        fun field(label: String): String = Regex("(?:$label)[:：,，.]?(.+?)(?=[/／]|(?:$labels)[:：,，.]?|$)")
            .find(compact)?.groupValues?.get(1).orEmpty().trim(':', '：', ',', '，', '.')
        val className = className(lines)
        val title = lines.first().trim().trimEnd('★', '☆', '○', 'O', '〇')
        val name = className?.takeIf { it in corroboratedNames && StructuredScheduleParser().isCourseName(it) } ?: title
        if (!StructuredScheduleParser().isCourseName(name)) return null
        return "周${"一二三四五六日"[day - 1]}  课程名称：$name  ${compact.substringBefore('/').substringBefore('／')}  教师：${field("教师|救师")}  教室：${field("场地|教室")}"
    }
}
