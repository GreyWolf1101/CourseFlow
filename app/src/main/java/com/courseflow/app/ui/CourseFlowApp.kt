package com.courseflow.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.courseflow.app.BuildConfig
import com.courseflow.app.data.ScheduleRepository
import com.courseflow.app.importer.CourseImportService
import com.courseflow.app.importer.ImportResult
import com.courseflow.app.importer.ScheduleShareCodec
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.courseflow.app.model.CourseSession
import com.courseflow.app.model.PeriodDefinition
import com.courseflow.app.model.SemesterConfig
import com.courseflow.app.model.WeekPattern
import com.courseflow.app.ui.theme.Coral
import com.courseflow.app.ui.theme.Ink
import com.courseflow.app.ui.theme.Mint
import com.courseflow.app.update.AppUpdateManager
import com.courseflow.app.update.InstallLaunchResult
import com.courseflow.app.update.ReleaseInfo
import com.courseflow.app.update.UpdateCheckResult
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.runtime.snapshotFlow
import java.time.LocalDate
import java.time.Instant
import java.time.DayOfWeek
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import java.io.File

private val dayNames = listOf("一", "二", "三", "四", "五", "六", "日")
private data class CourseColorStyle(val name: String, val background: Color, val foreground: Color)

private val courseColors = listOf(
    CourseColorStyle("薄荷", Color(0xFFCDEFE7), Color(0xFF07574F)),
    CourseColorStyle("杏仁", Color(0xFFFFE1C7), Color(0xFF7A3B05)),
    CourseColorStyle("雾蓝", Color(0xFFDCE5FF), Color(0xFF244A8E)),
    CourseColorStyle("藤紫", Color(0xFFF4DCF9), Color(0xFF6D327B)),
    CourseColorStyle("青柠", Color(0xFFE9F4C5), Color(0xFF445F0A)),
    CourseColorStyle("珊瑚", Color(0xFFFFDCD6), Color(0xFF87392B)),
    CourseColorStyle("天空", Color(0xFFCFEAF7), Color(0xFF174A61)),
    CourseColorStyle("樱粉", Color(0xFFFCE0EA), Color(0xFF7C2F4A)),
    CourseColorStyle("沙金", Color(0xFFF2E2B9), Color(0xFF634B00)),
    CourseColorStyle("海盐", Color(0xFFDDF0F1), Color(0xFF164F52)),
    CourseColorStyle("薰衣草", Color(0xFFE6E0F6), Color(0xFF4B3B78)),
    CourseColorStyle("石青", Color(0xFFD6E8E0), Color(0xFF1D5143)),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseFlowApp(repository: ScheduleRepository) {
    val state by repository.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val importer = remember { CourseImportService(context) }
    val scope = rememberCoroutineScope()
    val currentWeek = state.config.weekFor()

    var selectedWeek by rememberSaveable { mutableIntStateOf(currentWeek) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var detailCourse by remember { mutableStateOf<CourseSession?>(null) }
    var editingCourse by remember { mutableStateOf<CourseSession?>(null) }
    var newCourse by remember { mutableStateOf<CourseSession?>(null) }
    var importMenuOpen by remember { mutableStateOf(false) }
    var passphraseOpen by remember { mutableStateOf(false) }
    var shareText by remember { mutableStateOf<String?>(null) }
    var deleteCourse by remember { mutableStateOf<CourseSession?>(null) }
    var courseDeletionOpen by remember { mutableStateOf(false) }
    var clearCoursesOpen by remember { mutableStateOf(false) }
    var importResult by remember { mutableStateOf<ImportResult?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    var isImporting by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            isImporting = true
            runCatching { importer.import(uri, state.config) }
                .onSuccess { importResult = it }
                .onFailure { importError = it.message ?: "导入失败，请检查文件内容" }
            isImporting = false
        }
    }

    if (settingsOpen) {
        SettingsPage(
            config = state.config,
            courseCount = state.courses.size,
            onSelectDelete = { courseDeletionOpen = true },
            onClearCourses = { clearCoursesOpen = true },
            onBack = { settingsOpen = false },
            onImport = { importMenuOpen = true },
            onShare = {
                runCatching { ScheduleShareCodec.encode(state) }
                    .onSuccess { shareText = it }.onFailure { importError = it.message }
            },
            onAddCourse = { newCourse = CourseSession(name = "", dayOfWeek = LocalDate.now().dayOfWeek.value,
                startPeriod = 1, startWeek = selectedWeek, endWeek = state.config.totalWeeks) },
            onSave = {
                repository.updateConfig(it)
                selectedWeek = it.weekFor()
                settingsOpen = false
            },
        )
    } else {
        Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
            ScheduleHome(
                modifier = Modifier.padding(padding),
                config = state.config,
                courses = state.courses,
                week = selectedWeek,
                currentWeek = currentWeek,
                onWeekChange = { selectedWeek = it.coerceIn(1, state.config.totalWeeks) },
                onCourseClick = { detailCourse = it },
                onSlotClick = { day, period, week ->
                    newCourse = CourseSession(name = "", dayOfWeek = day, startPeriod = period,
                        startWeek = week, endWeek = state.config.totalWeeks, colorIndex = state.courses.size % courseColors.size)
                },
                onSettings = { settingsOpen = true },
            )
        }
    }

    detailCourse?.let { course ->
        CourseDetailSheet(
            course = course,
            periods = state.config.periods,
            onDismiss = { detailCourse = null },
            onEdit = {
                detailCourse = null
                editingCourse = course
            },
            onDelete = {
                detailCourse = null
                deleteCourse = course
            },
        )
    }

    val courseToEdit = editingCourse ?: newCourse
    courseToEdit?.let { course ->
        CourseEditorDialog(
            original = course,
            totalPeriods = state.config.periods.size,
            totalWeeks = state.config.totalWeeks,
            config = state.config,
            onDismiss = {
                editingCourse = null
                newCourse = null
            },
            onSave = {
                repository.saveCourse(it)
                editingCourse = null
                newCourse = null
            },
        )
    }

    if (courseDeletionOpen) {
        CourseDeletionDialog(
            courses = state.courses,
            onDismiss = { courseDeletionOpen = false },
            onDelete = { ids ->
                repository.deleteCourses(ids)
                courseDeletionOpen = false
            },
        )
    }
    if (clearCoursesOpen) {
        AlertDialog(
            onDismissRequest = { clearCoursesOpen = false },
            icon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
            title = { Text("清空整张课表？") },
            text = { Text("将删除所有周的 ${state.courses.size} 条课程安排，保留学期日期和上课时间。删除后无法撤销。") },
            confirmButton = {
                Button(onClick = { repository.clearCourses(); clearCoursesOpen = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("确认清空") }
            },
            dismissButton = { TextButton(onClick = { clearCoursesOpen = false }) { Text("取消") } },
        )
    }
    deleteCourse?.let { course ->
        AlertDialog(
            onDismissRequest = { deleteCourse = null },
            icon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
            title = { Text("删除“${course.name}”？") },
            text = { Text("删除后将从所有周的课表中移除。") },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deleteCourse(course.id)
                        deleteCourse = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deleteCourse = null }) { Text("取消") } },
        )
    }

    importResult?.let { result ->
        ImportPreviewDialog(
            result = result,
            currentConfig = state.config,
            onChange = { importResult = it },
            onDismiss = { importResult = null },
            onImport = { replace ->
                repository.importCourses(result.parsed.courses, replace, if (replace) result.parsed.config else null)
                selectedWeek = repository.state.value.config.weekFor()
                importResult = null
            },
        )
    }
    if (importMenuOpen) {
        AlertDialog(
            onDismissRequest = { importMenuOpen = false },
            title = { Text("导入课表") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("当前第一周周一：${state.config.monday()}。文件中的周次会以此计算日期。")
                    Button(onClick = {
                        importMenuOpen = false
                        filePicker.launch(arrayOf(
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            "application/pdf", "text/csv", "text/plain", "text/tab-separated-values", "text/html", "application/xhtml+xml",
                        ))
                    }, modifier = Modifier.fillMaxWidth()) { Text("选择文件（含 HTML）") }
                    OutlinedButton(onClick = { importMenuOpen = false; passphraseOpen = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("口令导入 · 课序 / WakeUp")
                    }
                    Text("支持 DOCX、XLSX、PDF、HTML、CSV、TXT。导入后可逐条核对和修改。", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { importMenuOpen = false }) { Text("取消") } },
        )
    }
    if (passphraseOpen) {
        var phrase by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { passphraseOpen = false },
            title = { Text("口令导入") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("粘贴课序或 WakeUp 的完整分享文案。WakeUp 口令需要联网读取分享课表。")
                OutlinedTextField(phrase, { phrase = it }, label = { Text("分享口令") }, minLines = 3, maxLines = 6, modifier = Modifier.fillMaxWidth())
            } },
            confirmButton = { Button(enabled = phrase.isNotBlank() && !isImporting, onClick = {
                passphraseOpen = false
                scope.launch {
                    isImporting = true
                    runCatching { importer.importPassphrase(phrase, state.config) }.onSuccess { importResult = it }
                        .onFailure { importError = it.message ?: "口令解析失败，请重新复制" }
                    isImporting = false
                }
            }) { Text("解析并预览") } },
            dismissButton = { TextButton(onClick = { passphraseOpen = false }) { Text("取消") } },
        )
    }
    shareText?.let { text ->
        var copied by remember(text) { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { shareText = null },
            title = { Text("课序分享口令") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("口令包含完整课程、学期日期和上课时间，可在另一台设备中离线导入。")
                OutlinedTextField(text, {}, readOnly = true, label = { Text("完整口令") }, maxLines = 5)
            } },
            confirmButton = { Button(onClick = {
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("课序分享口令", text))
                copied = true
            }) { Text(if (copied) "已复制" else "复制口令") } },
            dismissButton = { TextButton(onClick = { shareText = null }) { Text("关闭") } },
        )
    }
    importError?.let { message ->
        AlertDialog(
            onDismissRequest = { importError = null },
            icon = { Icon(Icons.Rounded.WarningAmber, contentDescription = null) },
            title = { Text("没有导入成功") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { importError = null }) { Text("知道了") } },
        )
    }
    if (isImporting) LoadingDialog()
}

@Composable
private fun ScheduleHome(
    modifier: Modifier,
    config: SemesterConfig,
    courses: List<CourseSession>,
    week: Int,
    currentWeek: Int,
    onWeekChange: (Int) -> Unit,
    onCourseClick: (CourseSession) -> Unit,
    onSlotClick: (Int, Int, Int) -> Unit,
    onSettings: () -> Unit,
) {
    val today = LocalDate.now()
    var weekPickerOpen by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = (week - 1).coerceIn(0, config.totalWeeks - 1),
        pageCount = { config.totalWeeks },
    )
    LaunchedEffect(week, config.totalWeeks) {
        val targetPage = (week - 1).coerceIn(0, config.totalWeeks - 1)
        if (targetPage != pagerState.currentPage && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(targetPage)
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { onWeekChange(it + 1) }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = .72f), MaterialTheme.colorScheme.background),
                    endY = 620f,
                )
            )
            .verticalScroll(rememberScrollState()),
    ) {
        Header(
            week = week,
            today = today,
            onSelectWeek = { weekPickerOpen = true },
            onSettings = onSettings,
        )
        Spacer(Modifier.height(2.dp))
        Surface(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "第${week}周课表，左右滑动切换周次" },
        ) {
            Column(Modifier.padding(top = 2.dp, bottom = 40.dp)) {
                val timetableHeight = 60.dp + 80.dp * config.periods.size
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().height(timetableHeight),
                    beyondViewportPageCount = 1,
                    key = { it },
                ) { page ->
                    val pageWeek = page + 1
                    ScheduleGrid(
                        config = config,
                        week = pageWeek,
                        courses = courses.filter { it.occursInWeek(pageWeek) },
                        today = today,
                        onCourseClick = onCourseClick,
                        onSlotClick = { day, period -> onSlotClick(day, period, pageWeek) },
                    )
                }
            }
        }
    }
    if (weekPickerOpen) {
        WeekPickerDialog(
            selectedWeek = week,
            currentWeek = currentWeek,
            totalWeeks = config.totalWeeks,
            onDismiss = { weekPickerOpen = false },
            onSelect = {
                weekPickerOpen = false
                scope.launch { pagerState.animateScrollToPage(it - 1) }
            },
        )
    }
}

@Composable
private fun Header(
    week: Int,
    today: LocalDate,
    onSelectWeek: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            onClick = onSelectWeek,
            color = Color.Transparent,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.semantics { contentDescription = "选择周次，当前第${week}周" },
        ) {
            Row(Modifier.padding(start = 2.dp, end = 2.dp, top = 9.dp, bottom = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("第${week}周", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            today.format(DateTimeFormatter.ofPattern("M月d日 · EEEE", Locale.CHINA)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onSettings, modifier = Modifier.semantics { contentDescription = "课表设置" }) {
            Icon(Icons.Rounded.Settings, contentDescription = null)
        }
    }
}

@Composable
private fun WeekPickerDialog(
    selectedWeek: Int,
    currentWeek: Int,
    totalWeeks: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null) },
        title = { Text("选择要查看的周次") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                (1..totalWeeks).chunked(5).forEach { rowWeeks ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        rowWeeks.forEach { week ->
                            FilterChip(
                                selected = week == selectedWeek,
                                onClick = { onSelect(week) },
                                label = { Text(week.toString(), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(5 - rowWeeks.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
        dismissButton = if (selectedWeek != currentWeek) {
            { TextButton(onClick = { onSelect(currentWeek) }) { Text("回到本周") } }
        } else null,
    )
}

@Composable
private fun ScheduleGrid(
    config: SemesterConfig,
    week: Int,
    courses: List<CourseSession>,
    today: LocalDate,
    onCourseClick: (CourseSession) -> Unit,
    onSlotClick: (Int, Int) -> Unit,
) {
    val slotHeight = 80.dp
    val headerHeight = 60.dp
    val railWidth = 50.dp
    val totalHeight = slotHeight * config.periods.size
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val dayWidth = (maxWidth - railWidth) / 7f
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.width(railWidth)) {
                Box(Modifier.height(headerHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("节次", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TimeRail(config.periods, slotHeight)
            }
            Column {
                Row {
                    (1..7).forEach { day ->
                        val date = config.dateFor(week, day)
                        DayHeader(date, dayWidth, headerHeight, date == today)
                    }
                }
                Row(Modifier.requiredHeight(totalHeight)) {
                    (1..7).forEach { day ->
                        DayColumn(
                            width = dayWidth,
                            height = totalHeight,
                            slotHeight = slotHeight,
                            periodCount = config.periods.size,
                            courses = courses.filter { it.dayOfWeek == day },
                            isToday = config.dateFor(week, day) == today,
                            onCourseClick = onCourseClick,
                            onSlotClick = { period -> onSlotClick(day, period) },
                            day = day,
                            week = week,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHeader(date: LocalDate, width: Dp, height: Dp, isToday: Boolean) {
    Box(Modifier.width(width).height(height), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("周${dayNames[date.dayOfWeek.value - 1]}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Surface(
                color = if (isToday) Coral else Color.Transparent,
                shape = CircleShape,
                modifier = Modifier.height(28.dp).widthIn(min = 38.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("${date.monthValue}/${date.dayOfMonth}", fontSize = 10.sp, color = if (isToday) Ink else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TimeRail(periods: List<PeriodDefinition>, slotHeight: Dp) {
    Column {
        periods.forEach { period ->
            Column(
                Modifier.height(slotHeight).fillMaxWidth().padding(top = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(period.index.toString(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(period.startTime, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(period.endTime(), fontSize = 8.sp, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun DayColumn(
    width: Dp,
    height: Dp,
    slotHeight: Dp,
    periodCount: Int,
    courses: List<CourseSession>,
    isToday: Boolean,
    onCourseClick: (CourseSession) -> Unit,
    onSlotClick: (Int) -> Unit,
    day: Int,
    week: Int,
) {
    Box(
        Modifier.width(width).height(height)
            .background(if (isToday) Mint.copy(alpha = .32f) else Color.Transparent)
            .border(.5.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.fillMaxSize()) {
            repeat(periodCount) { index ->
                val occupied = courses.any { index + 1 in it.startPeriod until it.startPeriod + it.periodSpan }
                Box(Modifier.height(slotHeight).fillMaxWidth().border(.5.dp, MaterialTheme.colorScheme.outlineVariant)
                    .then(if (!occupied) Modifier.clickable(onClickLabel = "添加课程") { onSlotClick(index + 1) }
                        .semantics { contentDescription = "第${week}周，周${dayNames[day - 1]}第${index + 1}节，添加课程" } else Modifier))
            }
        }
        courses.forEach { course ->
            val style = courseColors[Math.floorMod(course.colorIndex, courseColors.size)]
            CourseCard(
                course = course,
                background = style.background,
                foreground = style.foreground,
                modifier = Modifier
                    .offset(y = slotHeight * (course.startPeriod - 1))
                    .padding(1.5.dp)
                    .fillMaxWidth()
                    .height((slotHeight * course.periodSpan) - 3.dp),
                onClick = { onCourseClick(course) },
            )
        }
    }
}

@Composable
private fun CourseCard(
    course: CourseSession,
    background: Color,
    foreground: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        color = background,
        contentColor = foreground,
        shape = RoundedCornerShape(9.dp),
        modifier = modifier
            .shadow(1.dp, RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "${course.name}，${course.room}，点击查看详情" },
    ) {
        Column(Modifier.padding(horizontal = 4.dp, vertical = 6.dp)) {
            Text(
                course.name,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = if (course.periodSpan == 1) 3 else 5,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            if (course.room.isNotBlank() && course.periodSpan >= 2) {
                Text(course.room, fontSize = 8.sp, lineHeight = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (course.weekPattern != WeekPattern.EVERY) {
                Text(course.weekPattern.label.take(1), fontSize = 8.sp, lineHeight = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseDetailSheet(
    course: CourseSession,
    periods: List<PeriodDefinition>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val first = periods.getOrNull(course.startPeriod - 1)
    val last = periods.getOrNull(course.startPeriod + course.periodSpan - 2)
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(course.name, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50)) {
                        Text(course.weekPattern.label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                    }
                }
                IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, contentDescription = "编辑课程") }
            }
            Spacer(Modifier.height(20.dp))
            DetailRow(Icons.Rounded.CalendarMonth, "周${dayNames[course.dayOfWeek - 1]} · 第${course.startPeriod}—${course.startPeriod + course.periodSpan - 1}节")
            DetailRow(Icons.Rounded.Schedule, "${first?.startTime ?: "--:--"}—${last?.endTime() ?: "--:--"}")
            DetailRow(Icons.Rounded.LocationOn, course.room.ifBlank { "未填写教室" })
            DetailRow(Icons.Rounded.Person, course.teacher.ifBlank { "未填写任课教师" })
            DetailRow(Icons.Rounded.Info, "第${course.startWeek}—${course.endWeek}周")
            if (course.note.isNotBlank()) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.AutoMirrored.Rounded.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("备注", style = MaterialTheme.typography.labelLarge)
                            Text(course.note, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null)
                    Spacer(Modifier.width(6.dp)); Text("删除")
                }
                Button(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Edit, contentDescription = null)
                    Spacer(Modifier.width(6.dp)); Text("编辑")
                }
            }
        }
    }
}

@Composable
private fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(14.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun CourseEditorDialog(
    original: CourseSession,
    totalPeriods: Int,
    totalWeeks: Int,
    config: SemesterConfig,
    onDismiss: () -> Unit,
    onSave: (CourseSession) -> Unit,
) {
    var name by remember(original.id) { mutableStateOf(original.name) }
    var teacher by remember(original.id) { mutableStateOf(original.teacher) }
    var room by remember(original.id) { mutableStateOf(original.room) }
    var note by remember(original.id) { mutableStateOf(original.note) }
    var day by remember(original.id) { mutableIntStateOf(original.dayOfWeek) }
    var startPeriod by remember(original.id) { mutableIntStateOf(original.startPeriod) }
    var span by remember(original.id) { mutableIntStateOf(original.periodSpan) }
    var startWeek by remember(original.id) { mutableIntStateOf(original.startWeek) }
    var endWeek by remember(original.id) { mutableIntStateOf(original.endWeek) }
    var pattern by remember(original.id) { mutableStateOf(original.weekPattern) }
    var color by remember(original.id) { mutableIntStateOf(original.colorIndex) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().fillMaxHeight(.92f)) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (original.name.isBlank()) "添加课程" else "编辑课程", style = MaterialTheme.typography.headlineSmall)
                        Text("课程、周次和节数都可以随时调整", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "关闭") }
                }
                HorizontalDivider()
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("课程名称 *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(teacher, { teacher = it }, label = { Text("任课教师") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(room, { room = it }, label = { Text("上课教室") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    FieldLabel("上课星期")
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        dayNames.forEachIndexed { index, label ->
                            FilterChip(selected = day == index + 1, onClick = { day = index + 1 }, label = { Text("周$label") })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StepperCard("开始节次", startPeriod, 1, totalPeriods, { startPeriod = it; span = span.coerceAtMost(totalPeriods - it + 1) }, Modifier.weight(1f))
                        StepperCard("连续节数", span, 1, totalPeriods - startPeriod + 1, { span = it }, Modifier.weight(1f))
                    }
                    Text("周${dayNames[day - 1]} · 第${startPeriod}—${startPeriod + span - 1}节 · ${config.dateFor(startWeek, day)} 起", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    Text("连续课程会合并成一张完整卡片显示。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FieldLabel("上课周数")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StepperCard("开始周", startWeek, 1, totalWeeks, { startWeek = it; endWeek = endWeek.coerceAtLeast(it) }, Modifier.weight(1f))
                        StepperCard("结束周", endWeek, startWeek, totalWeeks, { endWeek = it }, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WeekPattern.entries.forEach { value ->
                            FilterChip(selected = pattern == value, onClick = { pattern = value }, label = { Text(value.label) })
                        }
                    }
                    FieldLabel("课程颜色")
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        courseColors.chunked(6).forEachIndexed { rowIndex, rowColors ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                rowColors.forEachIndexed { itemIndex, style ->
                                    val index = rowIndex * 6 + itemIndex
                                    Surface(
                                        color = style.background,
                                        shape = CircleShape,
                                        border = if (color == index) androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clickable { color = index }
                                            .semantics { contentDescription = "课程颜色：${style.name}" },
                                    ) {
                                        if (color == index) Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Rounded.Check, null, tint = style.foreground, modifier = Modifier.size(19.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Text(
                        "已选择：${courseColors[Math.floorMod(color, courseColors.size)].name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(note, { note = it }, label = { Text("自由备注") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                }
                HorizontalDivider()
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(original.copy(
                                name = name.trim(), teacher = teacher.trim(), room = room.trim(), note = note.trim(),
                                dayOfWeek = day, startPeriod = startPeriod, periodSpan = span,
                                startWeek = startWeek, endWeek = endWeek, weekPattern = pattern, colorIndex = color,
                            ))
                        },
                        enabled = name.isNotBlank(),
                    ) { Text("保存课程") }
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun StepperCard(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp), modifier = modifier) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onChange((value - 1).coerceAtLeast(min)) }, enabled = value > min) { Text("−", fontSize = 22.sp) }
                Text(value.toString(), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.width(30.dp))
                IconButton(onClick = { onChange((value + 1).coerceAtMost(max)) }, enabled = value < max) { Text("+", fontSize = 22.sp) }
            }
        }
    }
}

@Composable
private fun UpdatePanel() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updateManager = remember(context) { AppUpdateManager(context) }
    var checking by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var release by remember { mutableStateOf<ReleaseInfo?>(null) }
    var downloadedApk by remember { mutableStateOf<File?>(null) }
    var status by remember { mutableStateOf("通过 GitHub Release 获取正式更新") }
    var isError by remember { mutableStateOf(false) }

    val buttonText = when {
        checking -> "检查中"
        downloading -> "$progress%"
        downloadedApk != null -> "安装更新"
        release != null -> "下载 v${release?.versionName}"
        else -> "检查更新"
    }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(42.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.SystemUpdateAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("课序", style = MaterialTheme.typography.titleMedium)
                    Text("当前版本 v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    enabled = !checking && !downloading,
                    onClick = {
                        when {
                            downloadedApk != null -> {
                                runCatching { updateManager.launchInstaller(downloadedApk!!) }
                                    .onSuccess { result ->
                                        isError = false
                                        status = if (result == InstallLaunchResult.PermissionRequired) {
                                            "请允许课序安装应用，然后返回再点一次安装更新"
                                        } else {
                                            "正在打开系统安装界面"
                                        }
                                    }
                                    .onFailure {
                                        isError = true
                                        status = it.message ?: "无法打开安装界面"
                                    }
                            }
                            release != null -> {
                                downloading = true
                                progress = 0
                                isError = false
                                status = "正在下载安装包，请保持网络连接"
                                scope.launch {
                                    runCatching { updateManager.downloadUpdate(release!!) { progress = it } }
                                        .onSuccess {
                                            downloadedApk = it
                                            status = "下载与安全校验完成，可以安装"
                                        }
                                        .onFailure {
                                            isError = true
                                            status = it.message ?: "更新下载失败"
                                        }
                                    downloading = false
                                }
                            }
                            else -> {
                                checking = true
                                isError = false
                                status = "正在连接 GitHub 检查新版本"
                                scope.launch {
                                    runCatching { updateManager.checkForUpdate() }
                                        .onSuccess { result ->
                                            when (result) {
                                                is UpdateCheckResult.Available -> {
                                                    release = result.release
                                                    status = "发现新版本 v${result.release.versionName}"
                                                }
                                                is UpdateCheckResult.UpToDate -> {
                                                    release = null
                                                    downloadedApk = null
                                                    status = "已经是最新版本 v${result.latestVersion}"
                                                }
                                            }
                                        }
                                        .onFailure {
                                            isError = true
                                            status = it.message ?: "检查更新失败，请稍后重试"
                                        }
                                    checking = false
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                ) { Text(buttonText) }
            }
            if (downloading) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "更新下载进度 $progress%" },
                )
            }
            Text(
                status,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            release?.releaseNotes?.takeIf { it.isNotBlank() }?.let { notes ->
                Text(notes, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsPage(
    config: SemesterConfig,
    courseCount: Int,
    onSelectDelete: () -> Unit,
    onClearCourses: () -> Unit,
    onBack: () -> Unit,
    onImport: () -> Unit,
    onShare: () -> Unit,
    onAddCourse: () -> Unit,
    onSave: (SemesterConfig) -> Unit,
) {
    BackHandler(onBack = onBack)
    val widgetContext = LocalContext.current
    var name by remember(config) { mutableStateOf(config.name) }
    var startDate by remember(config) { mutableStateOf(config.monday()) }
    var totalWeeks by remember(config) { mutableIntStateOf(config.totalWeeks) }
    var periods by remember(config) { mutableStateOf(config.periods) }
    var editingPeriod by remember { mutableStateOf<PeriodDefinition?>(null) }
    var calendarOpen by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            Surface(shadowElevation = 1.dp) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回") }
                    Column(Modifier.weight(1f)) {
                        Text("课表设置", style = MaterialTheme.typography.titleLarge)
                        Text("导入、课程、学期与上课时间", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { onSave(config.copy(name = name.trim(), startDate = startDate.toString(), totalWeeks = totalWeeks, periods = periods)) }, enabled = name.isNotBlank()) { Text("保存") }
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("快捷操作", style = MaterialTheme.typography.headlineSmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onImport,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(vertical = 13.dp),
                ) {
                    Icon(Icons.Rounded.FileUpload, contentDescription = null, Modifier.size(19.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("导入课表")
                }
                OutlinedButton(
                    onClick = onAddCourse,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(vertical = 13.dp),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, Modifier.size(19.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("添加课程")
                }
            }
            OutlinedButton(onClick = onShare, modifier = Modifier.fillMaxWidth()) { Text("生成课序分享口令") }
            OutlinedButton(onClick = {
                val manager = android.appwidget.AppWidgetManager.getInstance(widgetContext)
                if (android.os.Build.VERSION.SDK_INT >= 26 && manager.isRequestPinAppWidgetSupported) {
                    manager.requestPinAppWidget(android.content.ComponentName(widgetContext, com.courseflow.app.widget.ScheduleWidgetProvider::class.java), null, null)
                } else {
                    android.widget.Toast.makeText(widgetContext, "请长按桌面空白处，选择小组件 → 课序 → 今日课程", android.widget.Toast.LENGTH_LONG).show()
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("添加桌面小组件") }
            Text("也可以在首页点击空白时段，直接添加该时段的课程。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text("课程管理", style = MaterialTheme.typography.headlineSmall)
            Text(if (courseCount == 0) "课表为空，点击首页空白时段即可添加课程。" else "共 $courseCount 条课程安排，可选择删除或清空整张课表。",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onSelectDelete, enabled = courseCount > 0, modifier = Modifier.weight(1f)) { Text("选择课程删除") }
                OutlinedButton(onClick = onClearCourses, enabled = courseCount > 0, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("清空课表") }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text("关于与更新", style = MaterialTheme.typography.headlineSmall)
            UpdatePanel()
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text("学期信息", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(name, { name = it }, label = { Text("学期名称") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { calendarOpen = true }
                    .semantics { contentDescription = "选择第一周日期，当前为$startDate" },
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(42.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("第一周周一", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            startDate.format(DateTimeFormatter.ofPattern("yyyy年M月d日 · EEEE", Locale.CHINA)),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                }
            }
            Text("点击日历选择日期；选择一周中的任意一天都会自动定位到该周周一。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            StepperCard("学期总周数", totalWeeks, 1, 30, { totalWeeks = it }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Text("上课时间", style = MaterialTheme.typography.headlineSmall)
            Text("点击任意节次，可修改开始时间和一节课的分钟数。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            periods.forEach { period ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth().clickable { editingPeriod = period },
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(42.dp)) {
                            Box(contentAlignment = Alignment.Center) { Text(period.index.toString(), fontWeight = FontWeight.Bold) }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("第${period.index}节", style = MaterialTheme.typography.titleMedium)
                            Text("${period.startTime}—${period.endTime()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${period.durationMinutes}分钟", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
    editingPeriod?.let { period ->
        PeriodEditorDialog(
            period = period,
            onDismiss = { editingPeriod = null },
            onSave = { changed ->
                periods = periods.map { if (it.index == changed.index) changed else it }
                editingPeriod = null
            },
        )
    }
    if (calendarOpen) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            yearRange = 2020..2100,
        )
        DatePickerDialog(
            onDismissRequest = { calendarOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val selected = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        startDate = selected.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    }
                    calendarOpen = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { calendarOpen = false }) { Text("取消") } },
        ) {
            DatePicker(
                state = pickerState,
                showModeToggle = false,
                title = { Text("选择第一周日期", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 24.dp, top = 20.dp)) },
            )
        }
    }
}

@Composable
private fun PeriodEditorDialog(period: PeriodDefinition, onDismiss: () -> Unit, onSave: (PeriodDefinition) -> Unit) {
    var time by remember(period) { mutableStateOf(period.startTime) }
    var duration by remember(period) { mutableIntStateOf(period.durationMinutes) }
    val timeValid = Regex("(?:[01]\\d|2[0-3]):[0-5]\\d").matches(time)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("第${period.index}节时间") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(time, { time = it }, label = { Text("开始时间（HH:mm）") }, isError = !timeValid, singleLine = true)
                StepperCard("一节课时长（分钟）", duration, 20, 180, { duration = it }, Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { onSave(period.copy(startTime = time, durationMinutes = duration)) }, enabled = timeValid) { Text("应用") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun CourseDeletionDialog(courses: List<CourseSession>, onDismiss: () -> Unit, onDelete: (Set<String>) -> Unit) {
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var confirming by remember { mutableStateOf(false) }
    val selectedCourses = courses.filter { it.id in selected }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(.8f)) {
            Column(Modifier.padding(20.dp)) {
                Text("选择课程删除", style = MaterialTheme.typography.headlineSmall)
                Text("勾选要删除的课程安排，将从其所有上课周中移除。", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("已选择 ${selectedCourses.size} 条", modifier = Modifier.weight(1f))
                    TextButton(onClick = { selected = if (selectedCourses.size == courses.size) emptySet() else courses.map { it.id }.toSet() }) {
                        Text(if (selectedCourses.size == courses.size) "取消全选" else "全选")
                    }
                }
                LazyColumn(Modifier.weight(1f)) {
                    items(courses.sortedWith(compareBy<CourseSession> { it.name }.thenBy { it.dayOfWeek }.thenBy { it.startPeriod }), key = { it.id }) { course ->
                        Row(Modifier.fillMaxWidth()
                            .toggleable(value = course.id in selected, role = Role.Checkbox) {
                                selected = if (it) selected + course.id else selected - course.id
                            }
                            .semantics { contentDescription = "选择删除${course.name}，周${dayNames[course.dayOfWeek - 1]}第${course.startPeriod}节" }
                            .padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = course.id in selected, onCheckedChange = null)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(course.name, style = MaterialTheme.typography.titleSmall)
                                Text("周${dayNames[course.dayOfWeek - 1]} · 第${course.startPeriod}—${course.startPeriod + course.periodSpan - 1}节 · ${course.startWeek}—${course.endWeek}周 ${course.weekPattern.label}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (course.teacher.isNotBlank() || course.room.isNotBlank()) Text(
                                    listOf(course.teacher, course.room).filter { it.isNotBlank() }.joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider()
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Button(onClick = { confirming = true }, enabled = selectedCourses.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("删除所选（${selectedCourses.size}）") }
                }
            }
        }
    }
    if (confirming) AlertDialog(
        onDismissRequest = { confirming = false },
        title = { Text("删除选中的 ${selectedCourses.size} 条课程安排？") },
        text = { Text(selectedCourses.take(5).joinToString("、") { it.name } +
            (if (selectedCourses.size > 5) "等课程" else "") + "将从课表中移除，删除后无法撤销。") },
        confirmButton = { Button(onClick = { onDelete(selectedCourses.map { it.id }.toSet()) },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("确认删除") } },
        dismissButton = { TextButton(onClick = { confirming = false }) { Text("取消") } },
    )
}

@Composable
private fun ImportPreviewDialog(
    result: ImportResult,
    currentConfig: SemesterConfig,
    onChange: (ImportResult) -> Unit,
    onDismiss: () -> Unit,
    onImport: (Boolean) -> Unit,
) {
    var editing by remember { mutableStateOf<CourseSession?>(null) }
    val config = result.parsed.config ?: currentConfig
    var firstMonday by remember(config.startDate) { mutableStateOf(config.monday().toString()) }
    val enteredDate = runCatching { LocalDate.parse(firstMonday) }.getOrNull()
    val canAppend = config.monday() == currentConfig.monday() && config.totalWeeks == currentConfig.totalWeeks && config.periods == currentConfig.periods
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().fillMaxHeight(.9f)) {
            Column(Modifier.padding(20.dp)) {
                Text("导入预览", style = MaterialTheme.typography.headlineSmall)
                Text(result.fileName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(12.dp))
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Text("识别到 ${result.parsed.courses.size} 条课程安排", style = MaterialTheme.typography.titleMedium)
                        Text("第一周周一：${config.monday()} · 共${config.totalWeeks}周", style = MaterialTheme.typography.bodyMedium)
                        OutlinedTextField(firstMonday, { firstMonday = it }, label = { Text("第一周周一（yyyy-MM-dd）") }, singleLine = true,
                            isError = enteredDate == null || enteredDate.dayOfWeek != DayOfWeek.MONDAY, modifier = Modifier.fillMaxWidth())
                        TextButton(onClick = { enteredDate?.let { date -> onChange(result.copy(parsed = result.parsed.copy(config = config.copy(startDate = date.toString())))) } },
                            enabled = enteredDate?.dayOfWeek == DayOfWeek.MONDAY && enteredDate != config.monday()) { Text("应用日期") }
                        if (result.parsed.config != null) Text("覆盖导入将同时应用预览中的学期日期和上课时间。", style = MaterialTheme.typography.bodySmall)
                        if (!canAppend) Text("来源课表的日期或上课时间与当前课表不同，无法直接追加；可核对后覆盖导入。", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    items(result.parsed.warnings) { warning ->
                        Text(warning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    items(result.parsed.courses, key = { it.id }) { course ->
                        Column(Modifier.fillMaxWidth().clickable { editing = course }.padding(vertical = 6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(course.name, style = MaterialTheme.typography.titleSmall)
                                    Text("周${dayNames[course.dayOfWeek - 1]} · 第${course.startPeriod}—${course.startPeriod + course.periodSpan - 1}节", style = MaterialTheme.typography.bodySmall)
                                    Text("第${course.startWeek}—${course.endWeek}周 · ${course.weekPattern.label}", style = MaterialTheme.typography.bodySmall)
                                    val firstWeek = (course.startWeek..course.endWeek).firstOrNull { course.occursInWeek(it) }
                                    Text(if (firstWeek != null) "按学期推算首次上课：${config.dateFor(firstWeek, course.dayOfWeek)}" else "所选周数内没有符合单双周规则的课程", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    Text(listOf(course.teacher, course.room).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "教师 / 教室未识别，可点击补充" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { editing = course }) { Icon(Icons.Rounded.Edit, contentDescription = "修改${course.name}") }
                                IconButton(onClick = { onChange(result.copy(parsed = result.parsed.copy(courses = result.parsed.courses.filterNot { it.id == course.id }))) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "移除${course.name}")
                                }
                            }
                            HorizontalDivider(Modifier.padding(top = 8.dp))
                        }
                    }
                    if (result.parsed.courses.isEmpty()) item {
                        Text("识别文字预览", style = MaterialTheme.typography.labelLarge)
                        Text(result.rawPreview.ifBlank { "没有可导入的课程" }, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    OutlinedButton(onClick = { onImport(false) }, enabled = result.parsed.courses.isNotEmpty() && canAppend && firstMonday == config.monday().toString()) { Text("追加") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onImport(true) }, enabled = result.parsed.courses.isNotEmpty() && firstMonday == config.monday().toString()) { Text("覆盖导入") }
                }
            }
        }
    }
    editing?.let { course ->
        CourseEditorDialog(original = course, totalPeriods = config.periods.size, totalWeeks = config.totalWeeks, config = config,
            onDismiss = { editing = null }, onSave = { changed ->
                onChange(result.copy(parsed = result.parsed.copy(courses = result.parsed.courses.map { if (it.id == changed.id) changed else it })))
                editing = null
            })
    }
}

@Composable
private fun LoadingDialog() {
    Dialog(onDismissRequest = {}) {
        Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.widthIn(min = 260.dp)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("正在识别课表…", style = MaterialTheme.typography.titleMedium)
                Text("PDF OCR 可能需要一点时间", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
    }
}
