package com.hanwentian.courseschedule

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.hanwentian.courseschedule.data.local.AppDatabase
import com.hanwentian.courseschedule.data.local.CourseEntity
import com.hanwentian.courseschedule.data.local.CourseLocalRepository
import com.hanwentian.courseschedule.ui.theme.CourseScheduleAppTheme
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val ImportScheduleUrl =
    "https://authserver-cjlu-edu-cn.video.cjlu.edu.cn:8118/authserver/login?service=https%3A%2F%2Fwebvpn.video.cjlu.edu.cn%2Fauth%2Fcas_validate%3Fentry_id%3D1"
private const val ImportHtmlLogTag = "ImportScheduleHtml"
private const val ImportCourseLogTag = "ImportParsedCourse"
private const val HtmlBridgeName = "HtmlBridge"

private val ScheduleTimeAxisWidth = 34.dp
private val ScheduleTimeAxisOffsetX = (-4).dp
private val ScheduleAxisToCourseGap = 4.dp
private val SchedulePeriodHeight = 88.dp
private val SchedulePeriodGap = 8.dp
private val ScheduleCourseInset = 2.dp
private val CourseWeekRegex =
    Regex("""第?\s*\d{1,2}(?:\s*[-~到]\s*\d{1,2})?(?:\s*,\s*\d{1,2}(?:\s*[-~到]\s*\d{1,2})?)*\s*周(?:[（(][单双全][)）])?""")
private val CoursePeriodRegex = Regex("""(?:第\s*)?\d{1,2}(?:\s*[-~到]\s*\d{1,2})?\s*节""")
private val CourseDayRegex = Regex("""(?:星期|周)([一二三四五六日天])""")
private val CourseDayMetadataRegex =
    Regex("""(?i)(?:xqj|weekday|dayOfWeek)\s*[:=]\s*['"]?([1-7])""")
private val CoursePeriodMetadataRegex =
    Regex("""(?i)(?:jcs|jc|period|startUnit|startPeriod)\s*[:=]\s*['"]?(\d{1,2})(?:\D+(\d{1,2}))?""")
private val CourseWeekMetadataRegex =
    Regex("""(?i)(?:zcd|week|weekText)\s*[:=]\s*['"]([^'"]*周(?:[（(][单双全][)）])?)['"]?""")
private val TeacherInfoRegex = Regex("""[\u4e00-\u9fa5A-Za-z]{2,20}(?:老师|教授|讲师|助教|教师)""")
private val HtmlBreakRegex = Regex("""(?i)<br\s*/?>""")
private val HtmlBlockEndRegex = Regex("""(?i)</(p|div|li|tr|td|section)>""")
private val WhitespaceRegex = Regex("""\s+""")
private val RoomCodeRegex = Regex("""[A-Z]{1,3}-?\d{2,4}""")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CourseScheduleAppTheme {
                CourseScheduleScreen()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CourseScheduleScreen() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val coroutineScope = rememberCoroutineScope()
    val courseRepository = remember(appContext) {
        CourseLocalRepository(
            AppDatabase.getInstance(appContext).courseDao()
        )
    }
    val today = remember { LocalDate.now() }
    val semesterStartDate = remember(today) {
        today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(5)
    }
    val baseWeek = remember(semesterStartDate, today) {
        calculateCurrentWeek(semesterStartDate, today)
    }
    val initialPagerPage = remember(baseWeek) { baseWeek - 1 }
    val totalWeekCount = remember(baseWeek) { maxOf(baseWeek + 20, 30) }
    val pagerState = rememberPagerState(
        initialPage = initialPagerPage,
        pageCount = { totalWeekCount }
    )
    val currentWeek by remember {
        derivedStateOf { pagerState.currentPage + 1 }
    }

    val timeSlots = listOf(
        TimeSlot(1, "08:00", "08:45"),
        TimeSlot(2, "08:50", "09:35"),
        TimeSlot(3, "09:55", "10:40"),
        TimeSlot(4, "10:45", "11:30"),
        TimeSlot(5, "11:35", "12:20"),
        TimeSlot(6, "13:30", "14:15"),
        TimeSlot(7, "14:20", "15:05"),
        TimeSlot(8, "15:15", "16:00"),
        TimeSlot(9, "16:05", "16:50"),
        TimeSlot(10, "18:00", "18:45"),
        TimeSlot(11, "18:50", "19:35"),
        TimeSlot(12, "19:40", "20:25")
    )

    val courses = remember {
        mutableStateListOf(
            CourseBlockInfo(1L, 0, 1, 2, "\u9ad8\u7b49\u6570\u5b66", "\u7b2c1-12\u5468", "\u5f20\u8001\u5e08", "A201", Color(0xFFE3F2FD)),
            CourseBlockInfo(2L, 1, 1, 2, "\u5927\u5b66\u82f1\u8bed", "\u7b2c1-16\u5468", "Lily", "B305", Color(0xFFE8F5E9)),
            CourseBlockInfo(3L, 2, 3, 2, "\u6570\u636e\u7ed3\u6784", "\u7b2c3-16\u5468", "\u738b\u8001\u5e08", "\u673a\u623f402", Color(0xFFFFF3E0)),
            CourseBlockInfo(4L, 3, 3, 2, "\u8ba1\u7b97\u673a\u7f51\u7edc", "\u7b2c5-16\u5468", "\u674e\u6559\u6388", "C406", Color(0xFFF3E5F5)),
            CourseBlockInfo(5L, 4, 5, 1, "\u4f53\u80b2", "\u7b2c1-18\u5468", "\u8d75\u8001\u5e08", "\u64cd\u573a", Color(0xFFF1F8E9)),
            CourseBlockInfo(6L, 0, 6, 2, "\u7ebf\u6027\u4ee3\u6570", "\u7b2c1-12\u5468", "\u5b59\u8001\u5e08", "A105", Color(0xFFE0F7FA)),
            CourseBlockInfo(7L, 1, 6, 2, "Android", "\u7b2c6-16\u5468", "\u9648\u8001\u5e08", "\u673a\u623f503", Color(0xFFE8EAF6)),
            CourseBlockInfo(8L, 2, 8, 2, "\u8f6f\u4ef6\u5de5\u7a0b", "\u7b2c1-16\u5468", "\u5468\u8001\u5e08", "B201", Color(0xFFFFF8E1)),
            CourseBlockInfo(9L, 3, 8, 2, "\u6570\u636e\u5e93\u539f\u7406", "\u7b2c1-16\u5468", "\u5434\u6559\u6388", "C302", Color(0xFFE0F2F1)),
            CourseBlockInfo(10L, 4, 10, 2, "\u64cd\u4f5c\u7cfb\u7edf", "\u7b2c8-16\u5468", "\u90d1\u8001\u5e08", "D208", Color(0xFFFFEBEE)),
            CourseBlockInfo(11L, 5, 10, 2, "\u9009\u4fee\u8bfe", "\u7b2c3-12\u5468", "\u6797\u8001\u5e08", "E102", Color(0xFFFCE4EC)),
            CourseBlockInfo(12L, 6, 11, 2, "\u81ea\u4e60", "\u7b2c1-18\u5468", "\u8f85\u5bfc\u5458", "\u56fe\u4e66\u9986", Color(0xFFEDE7F6))
        )
    }
    var selectedCourse by remember { mutableStateOf<CourseBlockInfo?>(null) }
    var showCourseSheet by remember { mutableStateOf(false) }
    var nextCourseId by remember { mutableStateOf(100L) }
    var showImportWebPage by remember { mutableStateOf(false) }

    LaunchedEffect(courseRepository) {
        val savedCourseBlocks = withContext(Dispatchers.IO) {
            courseRepository.getAllCourses().toStoredCourseBlockInfoList()
        }
        if (savedCourseBlocks.isNotEmpty()) {
            courses.clear()
            courses.addAll(savedCourseBlocks)
            nextCourseId = (savedCourseBlocks.maxOfOrNull { course -> course.id } ?: 0L) + 1L
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFF5F7FB)
    ) { innerPadding ->
        if (showImportWebPage) {
            ImportScheduleWebScreen(
                url = ImportScheduleUrl,
                onClose = { showImportWebPage = false },
                onCoursesParsed = { parsedCourses ->
                    val importedCourseBlocks = parsedCourses.toCourseBlockInfoList()
                    if (importedCourseBlocks.isEmpty()) {
                        Toast.makeText(
                            context,
                            "\u672a\u89e3\u6790\u5230\u8bfe\u7a0b\uff0c\u8bf7\u5148\u6253\u5f00\u6559\u52a1\u7cfb\u7edf\u4e2d\u7684\u8bfe\u8868\u9875\u9762",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@ImportScheduleWebScreen
                    }
                    val importedCourseEntities = parsedCourses.toCourseEntities()

                    coroutineScope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) {
                                courseRepository.replaceAllCourses(importedCourseEntities)
                            }
                        }.onSuccess {
                            courses.clear()
                            courses.addAll(importedCourseBlocks)
                            selectedCourse = null
                            showCourseSheet = false
                            nextCourseId = (importedCourseBlocks.maxOfOrNull { course -> course.id } ?: 0L) + 1L
                            showImportWebPage = false
                            Toast.makeText(
                                context,
                                "\u5df2\u4fdd\u5b58\u5e76\u5bfc\u5165 ${importedCourseBlocks.size} \u95e8\u8bfe\u7a0b",
                                Toast.LENGTH_SHORT
                            ).show()
                        }.onFailure { throwable ->
                            Log.e(ImportCourseLogTag, "Failed to save imported courses", throwable)
                            Toast.makeText(
                                context,
                                "\u8bfe\u8868\u4fdd\u5b58\u5931\u8d25\uff0c\u8bf7\u91cd\u8bd5",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
            return@Scaffold
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF5F7FB))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                SemesterTimeHeader(
                    currentWeek = currentWeek,
                    systemCurrentWeek = baseWeek,
                    today = today,
                    onImportClick = { showImportWebPage = true }
                )
                Spacer(modifier = Modifier.height(14.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDE3F0))
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val pageWeek = page + 1
                        val weekDates = remember(semesterStartDate, pageWeek) {
                            calculateWeekDates(
                                semesterStartDate = semesterStartDate,
                                currentWeek = pageWeek
                            )
                        }
                        val weekCourses = courses.filter { course ->
                            course.isVisibleInWeek(pageWeek)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 12.dp)
                        ) {
                            ScheduleWeekHeader(
                                weekDates = weekDates,
                                today = today
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                WakeUpScheduleBoard(
                                    dayCount = weekDates.size,
                                    timeSlots = timeSlots,
                                    courses = weekCourses,
                                    modifier = Modifier.fillMaxWidth(),
                                    onCourseClick = { course ->
                                        selectedCourse = course
                                        showCourseSheet = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showCourseSheet && selectedCourse != null) {
            CourseDetailBottomSheet(
                course = selectedCourse!!,
                timeSlots = timeSlots,
                onDismiss = {
                    showCourseSheet = false
                    selectedCourse = null
                },
                onDelete = {
                    val deletingCourse = selectedCourse ?: return@CourseDetailBottomSheet
                    courses.removeAll { it.id == deletingCourse.id }
                    showCourseSheet = false
                    selectedCourse = null
                },
                onCopy = {
                    val sourceCourse = selectedCourse ?: return@CourseDetailBottomSheet
                    val copiedCourse = sourceCourse.copy(
                        id = nextCourseId++,
                        dayIndex = if (sourceCourse.dayIndex < 6) {
                            sourceCourse.dayIndex + 1
                        } else {
                            sourceCourse.dayIndex
                        },
                        name = if (sourceCourse.name.contains("\u526f\u672c")) {
                            sourceCourse.name
                        } else {
                            "${sourceCourse.name}\u526f\u672c"
                        }
                    )
                    courses.add(copiedCourse)
                    selectedCourse = copiedCourse
                },
                onEdit = {
                    val editingCourse = selectedCourse ?: return@CourseDetailBottomSheet
                    val updatedCourse = editingCourse.copy(
                        name = if (editingCourse.name.contains("\uff08\u5df2\u7f16\u8f91\uff09")) {
                            editingCourse.name
                        } else {
                            "${editingCourse.name}\uff08\u5df2\u7f16\u8f91\uff09"
                        }
                    )
                    val index = courses.indexOfFirst { it.id == updatedCourse.id }
                    if (index >= 0) {
                        courses[index] = updatedCourse
                        selectedCourse = updatedCourse
                    }
                }
            )
        }
    }
}

@Composable
fun SemesterTimeHeader(
    currentWeek: Int,
    systemCurrentWeek: Int,
    today: LocalDate,
    modifier: Modifier = Modifier,
    onImportClick: () -> Unit = {},
    onClassTimeClick: () -> Unit = {},
    onAboutClick: () -> Unit = {}
) {
    var showMoreMenu by remember { mutableStateOf(false) }
    val weekTitle = if (currentWeek == systemCurrentWeek) {
        "\u7b2c${currentWeek}\u5468  ${today.toChineseWeekText()}"
    } else {
        "\u7b2c${currentWeek}\u5468\uff08\u975e\u672c\u5468\uff09"
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = weekTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = today.toSlashDateText(),
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF334155)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onImportClick) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = "Import",
                    tint = Color(0xFF0F172A)
                )
            }
            Box {
                IconButton(onClick = { showMoreMenu = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More",
                        tint = Color(0xFF0F172A)
                    )
                }

                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false },
                    offset = DpOffset(x = 0.dp, y = 4.dp),
                    modifier = Modifier
                        .width(168.dp)
                        .background(Color.White, RoundedCornerShape(16.dp))
                ) {
                    HeaderMenuItem(
                        icon = Icons.Filled.Schedule,
                        text = "\u4e0a\u8bfe\u65f6\u95f4",
                        onClick = {
                            showMoreMenu = false
                            onClassTimeClick()
                        }
                    )
                    HeaderMenuItem(
                        icon = Icons.Filled.Info,
                        text = "\u5173\u4e8e",
                        onClick = {
                            showMoreMenu = false
                            onAboutClick()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ScheduleWeekHeader(
    weekDates: List<LocalDate>,
    today: LocalDate,
    modifier: Modifier = Modifier
) {
    val displayMonth = resolveDisplayMonth(weekDates, today)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(ScheduleTimeAxisWidth + ScheduleAxisToCourseGap),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "${displayMonth}\u6708",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        }

        weekDates.forEach { date ->
            WeekDateItem(
                date = date,
                isToday = date == today,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ImportScheduleWebScreen(
    url: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onCoursesParsed: (List<Course>) -> Unit = {}
) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var shouldParseOnNextCapture by remember { mutableStateOf(false) }
    val onCoursesParsedState = rememberUpdatedState(onCoursesParsed)
    val htmlBridge = remember {
        HtmlCaptureJavascriptBridge { html ->
            logLargeText(
                tag = ImportHtmlLogTag,
                text = html
            )
            if (!shouldParseOnNextCapture) {
                return@HtmlCaptureJavascriptBridge
            }

            val parsedCourses = parseCoursesFromHtml(html)
            logParsedCourses(parsedCourses)
            onCoursesParsedState.value(parsedCourses)
            shouldParseOnNextCapture = false
        }
    }

    BackHandler(onBack = onClose)

    DisposableEffect(Unit) {
        onDispose {
            webView?.removeJavascriptInterface(HtmlBridgeName)
            webView?.destroy()
            webView = null
        }
    }

    Box(
        modifier = modifier
            .background(Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF0F172A)
                    )
                }
                Text(
                    text = "\u5bfc\u5165\u8bfe\u8868",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A)
                )
            }

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        webView = this
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadsImagesAutomatically = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        addJavascriptInterface(htmlBridge, HtmlBridgeName)
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                injectHtmlCaptureScript(view)
                            }
                        }
                        webChromeClient = WebChromeClient()
                        loadUrl(url)
                    }
                },
                update = { view ->
                    webView = view
                    if (view.url.isNullOrBlank()) {
                        view.loadUrl(url)
                    }
                }
            )
        }

        FloatingActionButton(
            onClick = { showImportDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = Color(0xFF1E293B),
            contentColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = "Import Schedule"
            )
        }

        if (showImportDialog) {
            ImportConfirmDialog(
                onImportReplace = {
                    showImportDialog = false
                    if (webView == null) {
                        Toast.makeText(
                            context,
                            "\u5f53\u524d\u9875\u9762\u8fd8\u6ca1\u52a0\u8f7d\u5b8c\u6210",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.d(ImportCourseLogTag, "WebView is not ready, skip import request")
                        return@ImportConfirmDialog
                    }
                    Toast.makeText(
                        context,
                        "\u6b63\u5728\u89e3\u6790\u8bfe\u8868...",
                        Toast.LENGTH_SHORT
                    ).show()
                    shouldParseOnNextCapture = true
                    requestCurrentPageHtml(webView)
                },
                onCancelImport = {
                    showImportDialog = false
                },
                onDismiss = {
                    showImportDialog = false
                }
            )
        }
    }
}

private fun injectHtmlCaptureScript(webView: WebView?) {
    webView?.evaluateJavascript(
        """
        (function() {
            window.__captureScheduleHtml = function() {
                if (window.HtmlBridge && window.HtmlBridge.captureHtml) {
                    window.HtmlBridge.captureHtml(document.documentElement.outerHTML);
                }
            };
        })();
        """.trimIndent(),
        null
    )
}

private fun requestCurrentPageHtml(webView: WebView?) {
    webView?.evaluateJavascript(
        """
        (function() {
            if (window.__captureScheduleHtml) {
                window.__captureScheduleHtml();
            } else if (window.HtmlBridge && window.HtmlBridge.captureHtml) {
                window.HtmlBridge.captureHtml(document.documentElement.outerHTML);
            }
        })();
        """.trimIndent(),
        null
    )
}

@Composable
fun ImportConfirmDialog(
    onImportReplace: () -> Unit,
    onCancelImport: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(24.dp))
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "\u8bf7\u9009\u62e9\u5bfc\u5165\u8bfe\u8868\u7684\u65b9\u5f0f",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Text(
                    text = "\u5bfc\u5165\u540e\u5c06\u5904\u7406\u5f53\u524d\u8bfe\u8868\u6570\u636e",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onImportReplace,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp)),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Text(
                            text = "\u8986\u76d6\u5f53\u524d\u8bfe\u8868",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155)
                        )
                    }

                    TextButton(
                        onClick = onCancelImport,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text(
                            text = "\u53d6\u6d88\u5bfc\u5165",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFDC2626)
                        )
                    }
                }
            }
        }
    }
}

private class HtmlCaptureJavascriptBridge(
    private val onHtmlCaptured: (String) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun captureHtml(html: String) {
        mainHandler.post {
            onHtmlCaptured(html)
        }
    }
}

private fun logLargeText(tag: String, text: String) {
    if (text.isBlank()) {
        Log.d(tag, "HTML is empty")
        return
    }

    val chunkSize = 3000
    var start = 0
    while (start < text.length) {
        val end = (start + chunkSize).coerceAtMost(text.length)
        Log.d(tag, text.substring(start, end))
        start = end
    }
}

private fun logParsedCourses(courses: List<Course>) {
    if (courses.isEmpty()) {
        Log.d(ImportCourseLogTag, "No course data was parsed from the current HTML")
        return
    }

    Log.d(ImportCourseLogTag, "Parsed ${courses.size} courses")
    courses.forEachIndexed { index, course ->
        Log.d(ImportCourseLogTag, "${index + 1}. $course")
    }
}

private fun parseCoursesFromHtml(html: String): List<Course> {
    if (html.isBlank()) {
        return emptyList()
    }

    val document = Jsoup.parse(html)
    return parseCoursesFromScheduleTables(document)
}

private fun selectCourseCandidateElements(document: Document): List<Element> {
    val selectors = listOf(
        "[class*=kbcontent]",
        "[class*=course]",
        "[class*=lesson]",
        "[class*=task]",
        "[class*=schedule]",
        "[data-role*=course]",
        "td[title]",
        "td"
    )
    val candidates = linkedSetOf<Element>()

    selectors.forEach { selector ->
        document.select(selector).forEach { element ->
            if (looksLikeCourseElement(element)) {
                candidates += element
            }
        }
    }

    if (candidates.isEmpty()) {
        document.select("div, li, td").forEach { element ->
            if (looksLikeCourseElement(element)) {
                candidates += element
            }
        }
    }

    return candidates.toList()
}

private fun looksLikeCourseElement(element: Element): Boolean {
    val lines = extractElementLines(element)
    if (lines.isEmpty()) {
        return false
    }

    val combinedText = lines.joinToString(" ")
    if (combinedText.length !in 4..220) {
        return false
    }

    val signalCount = listOf(
        extractWeekText(combinedText, element) != null,
        extractPeriodText(combinedText, element) != null,
        extractDayOfWeek(combinedText, element) != null,
        extractClassroom(lines, combinedText) != null
    ).count { it }

    val classSignal = element.classNames().any { className ->
        val lowerCaseClass = className.lowercase()
        lowerCaseClass.contains("course") ||
            lowerCaseClass.contains("lesson") ||
            lowerCaseClass.contains("kb") ||
            lowerCaseClass.contains("task")
    }

    return classSignal || signalCount >= 2
}

private fun parseCourseElement(element: Element): Course? {
    val lines = extractElementLines(element)
    if (lines.isEmpty()) {
        return null
    }

    val combinedText = lines.joinToString(" ")
    val weekText = extractWeekText(combinedText, element).orEmpty()
    val periodText = extractPeriodText(combinedText, element).orEmpty()
    val dayOfWeek = extractDayOfWeek(combinedText, element).orEmpty()
    val classroom = extractClassroom(lines, combinedText).orEmpty()
    val courseName = extractCourseName(
        lines = lines,
        combinedText = combinedText,
        weekText = weekText,
        periodText = periodText,
        dayOfWeek = dayOfWeek,
        classroom = classroom
    )

    if (courseName.isBlank()) {
        return null
    }
    if (weekText.isBlank() && periodText.isBlank() && dayOfWeek.isBlank() && classroom.isBlank()) {
        return null
    }

    return Course(
        name = courseName,
        periodText = periodText,
        dayOfWeek = dayOfWeek,
        classroom = classroom,
        weekText = weekText
    )
}

private fun extractElementLines(element: Element): List<String> {
    val rawSources = buildList {
        addTextSource(element.attr("title"))
        addTextSource(element.attr("data-original-title"))
        addTextSource(element.attr("aria-label"))
        addTextSource(element.wholeText())
        val htmlWithLineBreaks = element.html()
            .replace(HtmlBreakRegex, "\n")
            .replace(HtmlBlockEndRegex, "\n")
        addTextSource(Jsoup.parseBodyFragment(htmlWithLineBreaks).wholeText())
        addTextSource(element.text())
    }

    return rawSources
        .flatMap { source -> source.split(Regex("[\\n\\r]+")) }
        .map { line -> line.normalizeForParsing() }
        .filter { line -> line.isNotBlank() }
        .distinct()
}

private fun MutableList<String>.addTextSource(text: String) {
    val normalized = text.normalizeForParsing()
    if (normalized.isNotBlank()) {
        add(normalized)
    }
}

private fun extractWeekText(text: String, element: Element): String? {
    return CourseWeekRegex.find(text)?.value?.compactCourseField()
        ?: CourseWeekMetadataRegex.find(element.outerHtml())
            ?.groupValues
            ?.getOrNull(1)
            ?.compactCourseField()
}

private fun extractPeriodText(text: String, element: Element): String? {
    return CoursePeriodRegex.find(text)?.value
        ?.compactCourseField()
        ?.removePrefix("第")
        ?: inferPeriodFromMetadata(element)
}

private fun inferPeriodFromMetadata(element: Element): String? {
    val match = CoursePeriodMetadataRegex.find(element.outerHtml()) ?: return null
    val startPeriod = match.groupValues[1]
    val endPeriod = match.groupValues.getOrNull(2).orEmpty()
    return when {
        startPeriod.isBlank() -> null
        endPeriod.isBlank() || endPeriod == startPeriod -> "${startPeriod}节"
        else -> "${startPeriod}-${endPeriod}节"
    }
}

private fun extractDayOfWeek(text: String, element: Element): String? {
    return extractDayOfWeekFromText(text)
        ?: inferDayOfWeekFromMetadata(element)
        ?: inferDayOfWeekFromTableColumn(element)
}

private fun extractDayOfWeekFromText(text: String): String? {
    val dayChar = CourseDayRegex.find(text)?.groupValues?.getOrNull(1) ?: return null
    return if (dayChar == "天") {
        "周日"
    } else {
        "周$dayChar"
    }
}

private fun inferDayOfWeekFromMetadata(element: Element): String? {
    CourseDayMetadataRegex.find(element.outerHtml())
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
        ?.let { dayNumber ->
            return mapDayNumberToWeekText(dayNumber)
        }

    element.classNames().forEach { className ->
        Regex("""(?i)(?:xq|day|weekday)([1-7])""")
            .find(className)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?.let { dayNumber ->
                return mapDayNumberToWeekText(dayNumber)
            }
    }

    return null
}

private fun inferDayOfWeekFromTableColumn(element: Element): String? {
    if (element.tagName() != "td") {
        return null
    }

    val rowCells = element.parent()
        ?.children()
        ?.filter { cell -> cell.tagName() == "td" || cell.tagName() == "th" }
        ?: return null
    val cellIndex = rowCells.indexOf(element)
    if (cellIndex < 0) {
        return null
    }

    val dayNumber = when {
        rowCells.size >= 8 && cellIndex in 1..7 -> cellIndex
        rowCells.size == 7 && cellIndex in 0..6 -> cellIndex + 1
        else -> return null
    }
    return mapDayNumberToWeekText(dayNumber)
}

private fun mapDayNumberToWeekText(dayNumber: Int): String? {
    return when (dayNumber) {
        1 -> "周一"
        2 -> "周二"
        3 -> "周三"
        4 -> "周四"
        5 -> "周五"
        6 -> "周六"
        7 -> "周日"
        else -> null
    }
}

private fun extractClassroom(lines: List<String>, combinedText: String): String? {
    lines.forEach { line ->
        if (line.contains("@")) {
            val afterAt = line.substringAfter("@").normalizeForParsing()
            if (looksLikeClassroom(afterAt)) {
                return afterAt.compactCourseField()
            }
        }
    }

    lines.forEach { line ->
        val normalizedLine = line
            .replace("教室：", "")
            .replace("教室:", "")
            .trim()
        if (looksLikeClassroom(normalizedLine)) {
            return normalizedLine.compactCourseField()
        }
    }

    return RoomCodeRegex.find(combinedText)?.value?.compactCourseField()
}

private fun looksLikeClassroom(text: String): Boolean {
    val normalized = text.normalizeForParsing()
    if (normalized.isBlank()) {
        return false
    }
    if (CourseWeekRegex.matches(normalized) || CoursePeriodRegex.matches(normalized)) {
        return false
    }
    if (TeacherInfoRegex.matches(normalized)) {
        return false
    }
    return normalized.contains("教室") ||
        normalized.contains("机房") ||
        normalized.contains("实验室") ||
        normalized.contains("教学楼") ||
        normalized.contains("校区") ||
        normalized.contains("操场") ||
        normalized.contains("体育馆") ||
        normalized.contains("楼") ||
        normalized.contains("室") ||
        RoomCodeRegex.containsMatchIn(normalized)
}

private fun extractCourseName(
    lines: List<String>,
    combinedText: String,
    weekText: String,
    periodText: String,
    dayOfWeek: String,
    classroom: String
): String {
    lines.forEach { line ->
        val candidate = line
            .substringBefore("@")
            .replace(TeacherInfoRegex, " ")
            .replace(classroom, " ")
            .replace("课程名：", "")
            .replace("课程名:", "")
            .normalizeForParsing()

        if (
            candidate.isNotBlank() &&
            candidate != weekText &&
            candidate != periodText &&
            candidate != dayOfWeek &&
            candidate != classroom &&
            !CourseWeekRegex.matches(candidate) &&
            !CoursePeriodRegex.matches(candidate) &&
            extractDayOfWeekFromText(candidate) == null &&
            !looksLikeClassroom(candidate)
        ) {
            return candidate
        }
    }

    return combinedText
        .replace(weekText, " ")
        .replace(periodText, " ")
        .replace(dayOfWeek, " ")
        .replace(classroom, " ")
        .replace(TeacherInfoRegex, " ")
        .replace("课程名：", " ")
        .replace("课程名:", " ")
        .substringBefore("@")
        .normalizeForParsing()
}

private fun String.normalizeForParsing(): String {
    return trim()
        .replace('：', ':')
        .replace(WhitespaceRegex, " ")
}

private fun String.compactCourseField(): String {
    return normalizeForParsing()
        .replace(" ", "")
        .replace("到", "-")
        .replace("~", "-")
}

private val ScheduleWeekRegexLegacyV2 =
    Regex("""第?\s*\d{1,2}(?:\s*[-~到]\s*\d{1,2})?(?:\s*[,，]\s*\d{1,2}(?:\s*[-~到]\s*\d{1,2})?)*\s*周(?:[（(][单双全][)）])?""")
private val SchedulePeriodRegexLegacyV2 =
    Regex("""第?\s*(\d{1,2})(?:\s*[-~到]\s*(\d{1,2}))?\s*节""")
private val ScheduleCreditRegexLegacyV2 =
    Regex("""(?:(\d+(?:\.\d+)?)\s*学分|学分[:：]?\s*(\d+(?:\.\d+)?))""", RegexOption.IGNORE_CASE)
private val HeaderPeriodRegexV2 = Regex("""^第?\s*(\d{1,2})(?:\s*节)?$""")
private val HtmlLineBreakRegexV2 = Regex("""(?i)<br\s*/?>""")
private val TeacherKeywordRegexV2 = Regex("""(教师|老师|教授|讲师|助教|实验师|导师)""")
private val TimeRangeRegexV2 = Regex("""\d{1,2}:\d{2}""")
private val RoomTokenRegexV2 = Regex("""[A-Z]{1,4}-?\d{2,4}""")
private val ScheduleWeekRegexV2 =
    Regex("(\\d{1,2})(?:\\s*-\\s*(\\d{1,2}))?\\u5468(?:[（(](\\u5355|\\u53CC)[)）])?")
private val SchedulePeriodRegexV2 =
    Regex("(\\d{1,2})\\s*-\\s*(\\d{1,2})\\u8282")
private val ScheduleCreditRegexV2 =
    Regex("(?:(\\d+(?:\\.\\d+)?)\\s*\\u5B66\\u5206|\\u5B66\\u5206[:：]?\\s*(\\d+(?:\\.\\d+)?))", RegexOption.IGNORE_CASE)
private val TeacherKeywordStrictRegexV2 =
    Regex("(?:\\u6559\\u5E08|\\u8001\\u5E08|\\u6559\\u6388|\\u8BB2\\u5E08|\\u52A9\\u6559|\\u5BFC\\u5E08)")
private val CreditDecimalOnlyRegexV2 = Regex("""^\d+\.\d+$""")
private val ShortNumericTextRegexV2 = Regex("""^\d{1,2}$""")
private val WeekParityOddRegexV2 = Regex("\\u5355\\u5468")
private val WeekParityEvenRegexV2 = Regex("\\u53CC\\u5468")
private val TeacherLineRegexV2 =
    Regex("^[\\u4E00-\\u9FA5]{2,4}(?:/[\\u4E00-\\u9FA5]{2,4})*$")
private val ClassroomKeywordRegexV2 = Regex("(?:\\u697C|\\u6559\\u5BA4|\\u5BA4)")
private val ClassroomLineRegexV2 =
    Regex("^(?:[A-Z]\\d{3}|\\d{3,}|.*\\u697C.*|.*\\u6559\\u5BA4.*|.*\\u5BA4.*)$")
private val ClassroomCodeRegexV2 =
    Regex("(?<![A-Za-z0-9])(?:[A-Z]\\d{3}|\\d{3,})(?![A-Za-z0-9])")
private val ClassroomAlphaCodeRegexV2 = Regex("""[A-Z]\d{3}""")
private val ClassroomNumericCodeRegexV2 = Regex("""(?<!\d)\d{3,}(?!\d)""")
private val CourseTitleKeywordRegexV2 =
    Regex("(?:\\u5B66|\\u7406|\\u8BBA|\\u6CD5|\\u53F2|\\u8BED|\\u6587|\\u6570|\\u82F1|\\u4F53|\\u8BA1|\\u673A|\\u6982|\\u57FA\\u7840|\\u539F\\u7406|\\u8BBE\\u8BA1|\\u5B9E\\u9A8C|\\u601D\\u60F3|\\u653F\\u6CBB|\\u7F16\\u7A0B|\\u5DE5\\u7A0B)")

private fun parseCoursesFromScheduleTables(document: Document): List<Course> {
    val tableAnalyses = document.select("table")
        .mapIndexedNotNull { index, table ->
            buildScheduleTableAnalysisV2(table, index)
        }

    if (tableAnalyses.isEmpty()) {
        Log.d(ImportCourseLogTag, "No table structure matched the schedule parser")
        return emptyList()
    }

    tableAnalyses.forEach { analysis ->
        logScheduleTableAnalysisV2(analysis)
    }

    val selectedTable = tableAnalyses.maxByOrNull { it.score } ?: return emptyList()
    Log.d(
        ImportCourseLogTag,
        "Using schedule table #${selectedTable.tableIndex}, score=${selectedTable.score}"
    )

    return parseCoursesFromScheduleTable(selectedTable)
        .distinctBy { course ->
            listOf(
                course.name,
                course.periodText,
                course.dayOfWeek,
                course.classroom,
                course.weekText,
                course.teacher,
                course.credit
            )
        }
}

private fun buildScheduleTableAnalysisV2(
    table: Element,
    tableIndex: Int
): ScheduleTableAnalysisV2? {
    val rowElements = extractTableRowsV2(table)
    if (rowElements.isEmpty()) {
        return null
    }

    val activeRowSpans = mutableMapOf<Int, Int>()
    val logicalRows = mutableListOf<TableRowSnapshotV2>()
    var columnCount = 0

    rowElements.forEachIndexed { rowIndex, rowElement ->
        val placements = mutableListOf<TableCellPlacementV2>()
        var columnIndex = 0

        rowElement.children()
            .filter { child -> child.tagName() == "td" || child.tagName() == "th" }
            .forEach { cell ->
                while ((activeRowSpans[columnIndex] ?: -1) >= rowIndex) {
                    columnIndex++
                }

                val columnSpan = cell.attr("colspan").toIntOrNull()?.coerceAtLeast(1) ?: 1
                val rowSpan = cell.attr("rowspan").toIntOrNull()?.coerceAtLeast(1) ?: 1
                placements += TableCellPlacementV2(
                    element = cell,
                    rowIndex = rowIndex,
                    startColumn = columnIndex,
                    columnSpan = columnSpan,
                    rowSpan = rowSpan,
                    text = cellStructuredTextV2(cell)
                )

                repeat(columnSpan) { offset ->
                    if (rowSpan > 1) {
                        activeRowSpans[columnIndex + offset] = rowIndex + rowSpan - 1
                    }
                }
                columnIndex += columnSpan
            }

        columnCount = maxOf(columnCount, columnIndex)
        logicalRows += TableRowSnapshotV2(
            rowIndex = rowIndex,
            placements = placements
        )
    }

    val headerCandidate = logicalRows.take(minOf(4, logicalRows.size)).maxByOrNull { row ->
        row.placements.mapNotNull { placement ->
            extractHeaderWeekdayV2(placement.text)
        }.distinct().size
    } ?: return null

    val weekdayColumns = linkedMapOf<Int, Int>()
    headerCandidate.placements.forEach { placement ->
        val dayNumber = extractHeaderWeekdayV2(placement.text) ?: return@forEach
        repeat(placement.columnSpan) { offset ->
            weekdayColumns[placement.startColumn + offset] = dayNumber
        }
    }

    if (weekdayColumns.values.distinct().size < 5) {
        return null
    }

    val bodyStartRowIndex = findBodyStartRowIndexV2(
        rows = logicalRows,
        weekdayHeaderRowIndex = headerCandidate.rowIndex,
        weekdayColumns = weekdayColumns.keys.toSet()
    )

    return ScheduleTableAnalysisV2(
        tableIndex = tableIndex,
        rows = logicalRows,
        columnCount = columnCount,
        weekdayHeaderRowIndex = headerCandidate.rowIndex,
        bodyStartRowIndex = bodyStartRowIndex,
        weekdayColumns = weekdayColumns
    )
}

private fun extractTableRowsV2(table: Element): List<Element> {
    return table.children().flatMap { child ->
        when (child.tagName()) {
            "tr" -> listOf(child)
            "thead", "tbody", "tfoot" -> child.children().filter { it.tagName() == "tr" }
            else -> emptyList()
        }
    }
}

private fun logScheduleTableAnalysisV2(analysis: ScheduleTableAnalysisV2) {
    val weekdayPreview = analysis.weekdayColumns.entries
        .sortedBy { it.key }
        .joinToString(", ") { (column, day) ->
            "$column=${mapDayNumberToWeekTextV2(day)}"
        }
    val headerPreview = analysis.rows
        .filter { it.rowIndex <= analysis.weekdayHeaderRowIndex }
        .takeLast(2)
        .joinToString(" || ") { row ->
            row.placements.joinToString(" | ") { placement ->
                "[${placement.startColumn}]${placement.text.take(10)}"
            }
        }

    Log.d(
        ImportCourseLogTag,
        "Table #${analysis.tableIndex}: rows=${analysis.rows.size}, cols=${analysis.columnCount}, bodyStart=${analysis.bodyStartRowIndex}, weekdays=[$weekdayPreview], header=$headerPreview"
    )
}

private fun findBodyStartRowIndexV2(
    rows: List<TableRowSnapshotV2>,
    weekdayHeaderRowIndex: Int,
    weekdayColumns: Set<Int>
): Int {
    rows.filter { it.rowIndex > weekdayHeaderRowIndex }.forEach { row ->
        if (extractPeriodNumberFromRowV2(row, weekdayColumns) != null) {
            return row.rowIndex
        }

        val hasCourseContent = row.placements.any { placement ->
            placement.overlapsAny(weekdayColumns) && looksLikeCourseContentV2(placement.text)
        }
        if (hasCourseContent) {
            return row.rowIndex
        }
    }
    return weekdayHeaderRowIndex + 1
}

private fun parseCoursesFromScheduleTable(analysis: ScheduleTableAnalysisV2): List<Course> {
    val courses = mutableListOf<Course>()
    var fallbackPeriod = 1

    analysis.rows
        .filter { it.rowIndex >= analysis.bodyStartRowIndex }
        .forEach { row ->
            val rowPeriod = extractPeriodNumberFromRowV2(row, analysis.weekdayColumns.keys) ?: fallbackPeriod
            fallbackPeriod = maxOf(fallbackPeriod, rowPeriod + 1)

            row.placements.forEach { placement ->
                val coveredDays = placement.coveredColumns()
                    .mapNotNull { column -> analysis.weekdayColumns[column] }
                    .distinct()
                if (coveredDays.isEmpty()) {
                    return@forEach
                }

                val structuredCourses = parseStructuredCourseCellsResolved(placement.element)
                if (structuredCourses.isEmpty()) {
                    return@forEach
                }

                structuredCourses.forEach { structuredCourse ->
                    val startPeriod = structuredCourse.periodRange?.first ?: rowPeriod
                val duration = structuredCourse.periodRange
                    ?.let { range -> range.last - range.first + 1 }
                    ?: placement.rowSpan.coerceAtLeast(1)
                val endPeriod = startPeriod + duration - 1
                val periodText = if (startPeriod == endPeriod) {
                    "${startPeriod}节"
                } else {
                    "${startPeriod}-${endPeriod}节"
                }
                    coveredDays.forEach { dayNumber ->
                    val dayOfWeek = mapDayNumberToWeekTextV2(dayNumber).orEmpty()
                    if (dayOfWeek.isBlank()) {
                        return@forEach
                    }

                    courses += Course(
                        name = structuredCourse.name,
                        periodText = periodText,
                        dayOfWeek = dayOfWeek,
                        classroom = structuredCourse.classroom,
                        weekText = structuredCourse.weekText,
                        teacher = structuredCourse.teacher,
                        credit = structuredCourse.credit,
                        weekRanges = structuredCourse.weekRule.ranges,
                        weekParityMode = structuredCourse.weekRule.parityMode
                    )
                }
            }
        }
        }

    return courses
}

private fun parseStructuredCourseCellV2(cell: Element): StructuredCourseCellV2? {
    val lines = extractStructuredCellLinesV2(cell)
    if (lines.isEmpty()) {
        return null
    }

    val combinedText = lines.joinToString(" ")
    if (!looksLikeCourseContentV2(combinedText)) {
        return null
    }

    val weekText = extractWeekTextFromCellV2(lines, combinedText)
    val classroom = extractClassroomFromCellV2(lines, combinedText)
    val credit = extractCreditFromCellV2(combinedText)
    val periodRange = extractPeriodRangeFromCellV2(combinedText)
    val filteredLines = lines.mapNotNull { rawLine ->
        rawLine
            .replace("课程名：", "")
            .replace("课程名:", "")
            .replace("教师：", "")
            .replace("教师:", "")
            .replace("教室：", "")
            .replace("教室:", "")
            .replace("学分：", "")
            .replace("学分:", "")
            .normalizeForParsing()
            .takeIf { it.isNotBlank() }
    }

    val name = extractCourseNameFromCellV2(
        lines = filteredLines,
        classroom = classroom,
        weekText = weekText,
        credit = credit
    )
    if (name.isBlank()) {
        return null
    }

    val teacher = extractTeacherFromCellV2(
        lines = filteredLines,
        courseName = name,
        classroom = classroom,
        weekText = weekText,
        credit = credit
    )

    return StructuredCourseCellV2(
        name = name,
        teacher = teacher,
        classroom = classroom,
        weekText = weekText,
        credit = credit,
        periodRange = periodRange
    )
}

private fun extractStructuredCellLinesV2(cell: Element): List<String> {
    val fragments = mutableListOf<String>()

    listOf("title", "data-original-title", "aria-label").forEach { attributeName ->
        val attributeText = cell.attr(attributeName)
        if (attributeText.isNotBlank()) {
            fragments += attributeText
                .replace(HtmlLineBreakRegexV2, "\n")
                .replace("&#10;", "\n")
        }
    }

    val clone = cell.clone()
    clone.select("br").forEach { br ->
        br.after("\n")
    }
    clone.select("p, div, li").forEach { block ->
        block.appendText("\n")
    }
    fragments += clone.wholeText()

    return fragments
        .flatMap { fragment -> fragment.split(Regex("[\\n\\r]+")) }
        .map { line -> line.normalizeForParsing() }
        .filter { line -> line.isNotBlank() }
        .distinct()
}

private fun cellStructuredTextV2(cell: Element): String {
    return extractStructuredCellLinesV2(cell).joinToString(" ")
}

private fun extractHeaderWeekdayV2(text: String): Int? {
    val normalized = text.normalizeForParsing()
        .replace("星期", "周")
        .replace("礼拜", "周")

    return when {
        normalized.contains("周一") || normalized == "一" -> 1
        normalized.contains("周二") || normalized == "二" -> 2
        normalized.contains("周三") || normalized == "三" -> 3
        normalized.contains("周四") || normalized == "四" -> 4
        normalized.contains("周五") || normalized == "五" -> 5
        normalized.contains("周六") || normalized == "六" -> 6
        normalized.contains("周日") || normalized.contains("周天") || normalized == "日" || normalized == "天" -> 7
        else -> null
    }
}

private fun extractPeriodNumberFromRowV2(
    row: TableRowSnapshotV2,
    weekdayColumns: Set<Int>
): Int? {
    row.placements
        .filterNot { placement -> placement.overlapsAny(weekdayColumns) }
        .map { placement -> placement.text.normalizeForParsing() }
        .forEach { text ->
            if (TimeRangeRegexV2.containsMatchIn(text)) {
                return@forEach
            }

            HeaderPeriodRegexV2.find(text)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
                ?.takeIf { it in 1..30 }
                ?.let { return it }
        }
    return null
}

private fun looksLikeCourseContentV2(text: String): Boolean {
    val normalized = text.normalizeForParsing()
    if (normalized.isBlank()) {
        return false
    }
    if (normalized.all { it.isDigit() }) {
        return false
    }
    if (extractHeaderWeekdayV2(normalized) != null) {
        return false
    }
    if (TimeRangeRegexV2.containsMatchIn(normalized) && normalized.length <= 13) {
        return false
    }
    return normalized.any { character ->
        character.isLetter() || Character.UnicodeScript.of(character.code) == Character.UnicodeScript.HAN
    }
}

private fun extractWeekTextFromCellV2(lines: List<String>, combinedText: String): String {
    return lines.firstNotNullOfOrNull { line ->
        ScheduleWeekRegexV2.find(line)?.value?.compactCourseField()
    } ?: ScheduleWeekRegexV2.find(combinedText)?.value?.compactCourseField().orEmpty()
}

private fun extractPeriodRangeFromCellV2(text: String): IntRange? {
    val match = SchedulePeriodRegexV2.find(text) ?: return null
    val startPeriod = match.groupValues[1].toIntOrNull() ?: return null
    val endPeriod = match.groupValues.getOrNull(2).orEmpty().toIntOrNull() ?: startPeriod
    return startPeriod..endPeriod.coerceAtLeast(startPeriod)
}

private fun extractCreditFromCellV2(text: String): String {
    val match = ScheduleCreditRegexV2.find(text) ?: return ""
    val value = match.groupValues[1].ifBlank { match.groupValues[2] }
    return if (value.isBlank()) "" else "${value}学分"
}

private fun extractClassroomFromCellV2(lines: List<String>, combinedText: String): String {
    lines.forEach { line ->
        if (line.contains("@")) {
            val afterAt = line.substringAfter("@").normalizeForParsing()
            if (looksLikeClassroomV2(afterAt)) {
                return afterAt.compactCourseField()
            }
        }
    }

    lines.forEach { line ->
        val normalized = line
            .replace("教室：", "")
            .replace("教室:", "")
            .replace("地点：", "")
            .replace("地点:", "")
            .normalizeForParsing()
        if (looksLikeClassroomV2(normalized)) {
            return normalized.compactCourseField()
        }
    }

    return RoomTokenRegexV2.find(combinedText)?.value?.compactCourseField().orEmpty()
}

private fun looksLikeClassroomV2(text: String): Boolean {
    val normalized = text.normalizeForParsing()
    if (normalized.isBlank()) {
        return false
    }
    if (ScheduleWeekRegexV2.matches(normalized) || SchedulePeriodRegexV2.matches(normalized)) {
        return false
    }
    if (TeacherKeywordRegexV2.containsMatchIn(normalized)) {
        return false
    }
    return normalized.contains("教室") ||
        normalized.contains("机房") ||
        normalized.contains("实验室") ||
        normalized.contains("教学楼") ||
        normalized.contains("校区") ||
        normalized.contains("操场") ||
        normalized.contains("体育馆") ||
        normalized.contains("楼") ||
        normalized.contains("室") ||
        RoomTokenRegexV2.containsMatchIn(normalized)
}

private fun extractCourseNameFromCellV2(
    lines: List<String>,
    classroom: String,
    weekText: String,
    credit: String
): String {
    lines.forEach { line ->
        val candidate = line
            .substringBefore("@")
            .replace(classroom, " ")
            .replace(weekText, " ")
            .replace(credit, " ")
            .normalizeForParsing()
        if (
            candidate.isNotBlank() &&
            !looksLikeTeacherLineV2(candidate) &&
            !looksLikeClassroomV2(candidate) &&
            ScheduleWeekRegexV2.find(candidate) == null &&
            SchedulePeriodRegexV2.find(candidate) == null
        ) {
            return candidate
        }
    }
    return ""
}

private fun extractTeacherFromCellV2(
    lines: List<String>,
    courseName: String,
    classroom: String,
    weekText: String,
    credit: String
): String {
    return lines.mapNotNull { line ->
        line
            .replace(courseName, " ")
            .replace(classroom, " ")
            .replace(weekText, " ")
            .replace(credit, " ")
            .normalizeForParsing()
            .takeIf { it.isNotBlank() && looksLikeTeacherLineV2(it) }
    }
        .distinct()
        .joinToString(" / ")
}

private fun looksLikeTeacherLineV2(text: String): Boolean {
    val normalized = text.normalizeForParsing()
    if (normalized.isBlank()) {
        return false
    }
    if (looksLikeClassroomV2(normalized) || ScheduleWeekRegexV2.containsMatchIn(normalized)) {
        return false
    }
    if (SchedulePeriodRegexV2.containsMatchIn(normalized) || TimeRangeRegexV2.containsMatchIn(normalized)) {
        return false
    }
    if (TeacherKeywordRegexV2.containsMatchIn(normalized)) {
        return true
    }
    if (normalized.any { it.isDigit() }) {
        return false
    }
    val tokens = normalized.split(Regex("[、,，/ ]+"))
        .map { token -> token.trim() }
        .filter { token -> token.isNotBlank() }
    return tokens.isNotEmpty() && tokens.all { token -> token.length in 2..8 }
}

private fun mapDayNumberToWeekTextV2(dayNumber: Int): String? {
    return when (dayNumber) {
        1 -> "周一"
        2 -> "周二"
        3 -> "周三"
        4 -> "周四"
        5 -> "周五"
        6 -> "周六"
        7 -> "周日"
        else -> null
    }
}

private data class ScheduleTableAnalysisV2(
    val tableIndex: Int,
    val rows: List<TableRowSnapshotV2>,
    val columnCount: Int,
    val weekdayHeaderRowIndex: Int,
    val bodyStartRowIndex: Int,
    val weekdayColumns: Map<Int, Int>
) {
    val score: Int
        get() = weekdayColumns.values.distinct().size * 100 + (rows.size - bodyStartRowIndex)
}

private data class TableRowSnapshotV2(
    val rowIndex: Int,
    val placements: List<TableCellPlacementV2>
)

private data class TableCellPlacementV2(
    val element: Element,
    val rowIndex: Int,
    val startColumn: Int,
    val columnSpan: Int,
    val rowSpan: Int,
    val text: String
) {
    fun coveredColumns(): IntRange {
        return startColumn until (startColumn + columnSpan)
    }

    fun overlapsAny(columns: Set<Int>): Boolean {
        return coveredColumns().any { column -> column in columns }
    }
}

private data class StructuredCourseCellV2(
    val name: String,
    val teacher: String,
    val classroom: String,
    val weekText: String,
    val credit: String,
    val periodRange: IntRange?
)

private const val FieldBoundaryPatternV2 =
    """(?=\s*(?:课程名|课程名称|教师|老师|任课教师|授课教师|教室|上课地点|地点|教室地点|周次|周数|节次|学分)\s*[:：]|$)"""
private val CourseNameLabelRegexV2 =
    Regex("""(?:课程名|课程名称)\s*[:：]?\s*(.+?)$FieldBoundaryPatternV2""")
private val TeacherLabelRegexV2 =
    Regex("""(?:教师|老师|任课教师|授课教师)\s*[:：]?\s*(.+?)$FieldBoundaryPatternV2""")
private val ClassroomLabelRegexV2 =
    Regex("""(?:教室|上课地点|地点|教室地点)\s*[:：]?\s*(.+?)$FieldBoundaryPatternV2""")
private val CourseNameMarkerRegexV2 =
    Regex("""★\s*(.+?)$FieldBoundaryPatternV2""")
private val TeacherNameRegexV2 = Regex("^[\\u4E00-\\u9FFF]{2,4}$")
private val ClassroomHintRegexV2 =
    Regex("(?:\\u697C|\\u6559\\u5BA4|[A-Z]?\\d{3,4}|\\u6559[\\u4E00-\\u9FFFA-Z]?\\d{2,4})")

private fun parseStructuredCourseCellResolved(cell: Element): StructuredCourseCellResolved? {
    val segments = extractDomSegmentsFromCellV2(cell)
    if (segments.isEmpty()) {
        return null
    }

    val combinedText = segments.joinToString(" ")
    if (!looksLikeCourseContentV2(combinedText)) {
        return null
    }

    val structuredFieldGroups = extractStructuredFieldGroupsFromCellV2(cell)
    val parsedFields = extractCourseFieldsStrictV2(
        segments = segments,
        structuredFields = structuredFieldGroups.firstOrNull()
    )
    val resolvedCourseName = resolveCourseNameFromHtmlV2(
        cell = cell,
        lines = extractCourseBlockLinesFromCellV2(cell),
        groupIndex = 0,
        groupCount = 1
    )
    if (resolvedCourseName.isBlank()) {
        return null
    }
    val displayCourseName = appendCreditToCourseNameV2(
        name = resolvedCourseName,
        credit = parsedFields.credit
    )

    return StructuredCourseCellResolved(
        name = displayCourseName,
        teacher = parsedFields.teacher,
        classroom = parsedFields.classroom,
        weekText = parsedFields.weekText,
        credit = parsedFields.credit,
        periodRange = parsedFields.periodRange,
        weekRule = parsedFields.weekRule
    )
}

private fun parseStructuredCourseCellsResolved(cell: Element): List<StructuredCourseCellResolved> {
    val lines = extractCourseBlockLinesFromCellV2(cell)
    if (lines.isEmpty()) {
        return listOfNotNull(parseStructuredCourseCellResolved(cell))
    }

    val groupedLines = splitCourseGroupsByWeekV2(lines)
    val boldCourseNames = extractBoldCourseNamesFromCellV2(cell)
    val structuredFieldGroups = extractStructuredFieldGroupsFromCellV2(cell)
    val parsedCourses = groupedLines.mapIndexedNotNull { index, groupLines ->
        parseStructuredCourseGroupResolvedV2(
            cell = cell,
            lines = groupLines,
            groupIndex = index,
            groupCount = groupedLines.size,
            boldCourseNames = boldCourseNames,
            structuredFields = structuredFieldGroups.getOrNull(index)
        )?.also { parsedCourse ->
            Log.d(
                ImportCourseLogTag,
                "Cell group #${index + 1}: lines=$groupLines, " +
                    "name=${parsedCourse.name}, week=${parsedCourse.weekText}, " +
                    "teacher=${parsedCourse.teacher}, classroom=${parsedCourse.classroom}, " +
                    "period=${parsedCourse.periodRange}, credit=${parsedCourse.credit}"
            )
        }
    }

    return if (parsedCourses.isNotEmpty()) {
        parsedCourses
    } else {
        listOfNotNull(parseStructuredCourseCellResolved(cell))
    }
}

private fun parseStructuredCourseGroupResolvedV2(
    cell: Element,
    lines: List<String>,
    groupIndex: Int,
    groupCount: Int,
    boldCourseNames: List<String>,
    structuredFields: StructuredFieldGroupV2?
): StructuredCourseCellResolved? {
    val segments = lines
        .map { line -> line.normalizeDomSegmentV2() }
        .filter { segment -> segment.isNotBlank() && !segment.isIgnorableDomSegmentV2() }
    if (segments.isEmpty()) {
        return null
    }

    val combinedText = segments.joinToString(" ")
    if (!looksLikeCourseContentV2(combinedText)) {
        return null
    }

    val parsedFields = extractCourseFieldsStrictV2(
        segments = segments,
        structuredFields = structuredFields
    )
    val resolvedCourseName = resolveCourseNameFromHtmlV2(
        cell = cell,
        lines = lines,
        groupIndex = groupIndex,
        groupCount = groupCount,
        boldCourseNames = boldCourseNames
    )
    if (resolvedCourseName.isBlank()) {
        return null
    }
    val displayCourseName = appendCreditToCourseNameV2(
        name = resolvedCourseName,
        credit = parsedFields.credit
    )

    return StructuredCourseCellResolved(
        name = displayCourseName,
        teacher = parsedFields.teacher,
        classroom = parsedFields.classroom,
        weekText = parsedFields.weekText,
        credit = parsedFields.credit,
        periodRange = parsedFields.periodRange,
        weekRule = parsedFields.weekRule
    )
}

private fun extractCourseBlockLinesFromCellV2(cell: Element): List<String> {
    val lines = mutableListOf<String>()
    val currentLine = StringBuilder()
    val blockTags = setOf("div", "p", "li", "tr")

    fun appendText(rawText: String) {
        val text = rawText.normalizeForParsing()
        if (text.isBlank()) {
            return
        }
        if (currentLine.isNotEmpty()) {
            currentLine.append(' ')
        }
        currentLine.append(text)
    }

    fun flushLine() {
        val normalized = currentLine.toString().normalizeDomSegmentV2()
        if (normalized.isNotBlank() && !normalized.isIgnorableDomSegmentV2()) {
            lines += normalized
        }
        currentLine.clear()
    }

    fun visit(node: Node) {
        when (node) {
            is TextNode -> appendText(node.text())
            is Element -> {
                val tagName = node.normalName()
                if (tagName == "br") {
                    flushLine()
                    return
                }

                val isBlock = tagName in blockTags
                if (isBlock && currentLine.isNotEmpty()) {
                    flushLine()
                }

                appendText(node.ownText())
                node.children().forEach { child ->
                    visit(child)
                }

                if (isBlock && currentLine.isNotEmpty()) {
                    flushLine()
                }
            }
        }
    }

    cell.childNodes().forEach { childNode ->
        visit(childNode)
    }
    flushLine()

    return lines
}

private fun splitCourseGroupsByWeekV2(lines: List<String>): List<List<String>> {
    val normalizedLines = lines
        .map { line -> line.normalizeDomSegmentV2() }
        .filter { line -> line.isNotBlank() && !line.isIgnorableDomSegmentV2() }
    if (normalizedLines.isEmpty()) {
        return emptyList()
    }

    val weekIndices = normalizedLines.indices.filter { index ->
        extractWeekTextStrictV2(normalizedLines[index]).isNotBlank()
    }
    if (weekIndices.size <= 1) {
        return listOf(normalizedLines)
    }

    val groupStarts = mutableListOf<Int>()
    var lowerBound = 0
    weekIndices.forEach { weekIndex ->
        val startIndex = findCourseGroupStartIndexV2(
            lines = normalizedLines,
            lowerBound = lowerBound,
            weekIndex = weekIndex
        )
        groupStarts += startIndex
        lowerBound = weekIndex + 1
    }

    val distinctStarts = groupStarts.distinct().sorted()
    return distinctStarts.mapIndexedNotNull { index, startIndex ->
        val endExclusive = distinctStarts.getOrNull(index + 1) ?: normalizedLines.size
        normalizedLines.subList(startIndex, endExclusive)
            .filter { line -> line.isNotBlank() && !line.isIgnorableDomSegmentV2() }
            .takeIf { groupLines -> groupLines.isNotEmpty() }
    }
}

private fun findCourseGroupStartIndexV2(
    lines: List<String>,
    lowerBound: Int,
    weekIndex: Int
): Int {
    val searchStart = lowerBound.coerceAtLeast(0)
    for (index in (weekIndex - 1) downTo searchStart) {
        if (looksLikeCourseGroupStartLineV2(lines[index])) {
            return index
        }
    }
    return searchStart
}

private fun looksLikeCourseGroupStartLineV2(line: String): Boolean {
    val normalized = line.normalizeDomSegmentV2()
    if (
        normalized.isBlank() ||
        normalized.isIgnorableDomSegmentV2() ||
        extractWeekTextStrictV2(normalized).isNotBlank() ||
        extractPeriodRangeFromCellV2(normalized) != null ||
        ScheduleCreditRegexV2.containsMatchIn(normalized) ||
        extractClassroomRegexStrictV3(normalized).isNotBlank() ||
        looksLikeTeacherCandidateRegexStrictV3(normalized)
    ) {
        return false
    }

    return cleanupCourseNameRegexStrictV3(normalized).isNotBlank()
}

private const val StructuredWeekIconV2 = "\uD83D\uDCC5"
private const val StructuredPeriodIconV2 = "\u23F0"
private const val StructuredTeacherIconV2 = "\uD83D\uDC64"
private const val StructuredClassroomIconV2 = "\uD83D\uDCCD"
private const val StructuredCreditIconV2 = "\uD83C\uDF93"
private const val StructuredHouseBoundaryIconV2 = "\uD83C\uDFE0"

private val StructuredFieldBoundaryIconRegexV2 = Regex(
    listOf(
        StructuredWeekIconV2,
        StructuredPeriodIconV2,
        StructuredTeacherIconV2,
        StructuredClassroomIconV2,
        StructuredCreditIconV2,
        StructuredHouseBoundaryIconV2
    ).joinToString("|") { icon -> Regex.escape(icon) }
)
private const val UnspecifiedWeekTextV2 = "未标注周数"

private enum class StructuredFieldTypeV2 {
    WEEK,
    PERIOD,
    TEACHER,
    CLASSROOM,
    CREDIT
}

private data class StructuredFieldTokenV2(
    val fieldType: StructuredFieldTypeV2? = null,
    val text: String = "",
    val isBoundary: Boolean = false
)

private data class StructuredFieldGroupV2(
    val weekText: String = "",
    val periodText: String = "",
    val teacher: String = "",
    val classroom: String = "",
    val credit: String = ""
)

private fun extractStructuredFieldGroupsFromCellV2(cell: Element): List<StructuredFieldGroupV2> {
    val rowCandidates = findStructuredFieldRowsV2(cell)
    val entries = rowCandidates.flatMap { row ->
        extractStructuredFieldEntriesFromRowV2(row)
    }

    if (entries.isEmpty()) {
        return emptyList()
    }

    val groups = mutableListOf<StructuredFieldGroupV2>()
    var currentGroup = StructuredFieldGroupV2()
    var hasContent = false

    entries.forEach { (fieldType, fieldText) ->
        if (shouldStartStructuredFieldGroupV2(fieldType, currentGroup, hasContent)) {
            groups += currentGroup
            currentGroup = StructuredFieldGroupV2()
            hasContent = false
        }

        currentGroup = when (fieldType) {
            StructuredFieldTypeV2.WEEK -> currentGroup.copy(weekText = fieldText)
            StructuredFieldTypeV2.PERIOD -> currentGroup.copy(periodText = fieldText)
            StructuredFieldTypeV2.TEACHER -> currentGroup.copy(teacher = fieldText)
            StructuredFieldTypeV2.CLASSROOM -> currentGroup.copy(classroom = fieldText)
            StructuredFieldTypeV2.CREDIT -> currentGroup.copy(credit = fieldText)
        }
        hasContent = true
    }

    if (hasContent) {
        groups += currentGroup
    }

    groups.forEachIndexed { index, group ->
        Log.d(
            ImportCourseLogTag,
            "Structured field group #${index + 1}: week=${group.weekText}, " +
                "period=${group.periodText}, teacher=${group.teacher}, classroom=${group.classroom}, credit=${group.credit}"
        )
    }

    return groups
}

private fun shouldStartStructuredFieldGroupV2(
    fieldType: StructuredFieldTypeV2,
    currentGroup: StructuredFieldGroupV2,
    hasContent: Boolean
): Boolean {
    if (!hasContent) {
        return false
    }

    return when (fieldType) {
        StructuredFieldTypeV2.WEEK -> currentGroup.weekText.isNotBlank()
        StructuredFieldTypeV2.PERIOD -> currentGroup.periodText.isNotBlank()
        StructuredFieldTypeV2.TEACHER -> currentGroup.teacher.isNotBlank()
        StructuredFieldTypeV2.CLASSROOM -> currentGroup.classroom.isNotBlank()
        StructuredFieldTypeV2.CREDIT -> currentGroup.credit.isNotBlank()
    }
}

private fun findStructuredFieldRowsV2(cell: Element): List<Element> {
    val descendants = cell.select("div, li, p, section, article, tr, td, span")
    val minimalRows = descendants.filter { row ->
        isStructuredFieldRowCandidateV2(row) && !hasNestedStructuredFieldRowV2(row)
    }

    if (minimalRows.isNotEmpty()) {
        return minimalRows
    }

    return cell.children()
        .filter { child -> isStructuredFieldRowCandidateV2(child) }
}

private fun isStructuredFieldRowCandidateV2(row: Element): Boolean {
    return extractStructuredFieldEntriesFromRowV2(row).isNotEmpty()
}

private fun hasNestedStructuredFieldRowV2(row: Element): Boolean {
    return row.children().any { child ->
        isStructuredFieldRowCandidateV2(child) || hasNestedStructuredFieldRowV2(child)
    }
}

private fun extractStructuredFieldEntriesFromRowV2(row: Element): List<Pair<StructuredFieldTypeV2, String>> {
    val tokens = mutableListOf<StructuredFieldTokenV2>()

    fun visit(node: Node) {
        when (node) {
            is TextNode -> appendStructuredFieldTokensFromTextV2(node.text(), tokens)
            is Element -> {
                if (node.normalName() == "br") {
                    tokens += StructuredFieldTokenV2(text = "\n")
                    return
                }

                val iconType = detectStructuredFieldTypeFromInlineElementV2(node)
                if (iconType != null) {
                    tokens += StructuredFieldTokenV2(
                        fieldType = iconType,
                        isBoundary = true
                    )
                    return
                }

                node.childNodes().forEach(::visit)
            }
        }
    }

    row.childNodes().forEach(::visit)
    return buildStructuredFieldEntriesFromTokensV2(tokens)
}

private fun detectStructuredFieldTypeFromInlineElementV2(element: Element): StructuredFieldTypeV2? {
    val iconType = classifyStructuredFieldTypeFromElementV2(element) ?: return null
    val hasMeaningfulOwnText = element.ownText().normalizeForParsing().isNotBlank()
    val children = element.children()
    val hasOnlyIconLikeChildren = children.isNotEmpty() && children.all { child ->
        child.ownText().normalizeForParsing().isBlank() &&
            child.children().all { grandChild ->
                grandChild.ownText().normalizeForParsing().isBlank()
            }
    }

    return if (hasMeaningfulOwnText || children.isEmpty() || hasOnlyIconLikeChildren) {
        iconType
    } else {
        null
    }
}

private fun appendStructuredFieldTokensFromTextV2(
    rawText: String,
    tokens: MutableList<StructuredFieldTokenV2>
) {
    val normalizedText = rawText.replace("\uFE0F", "")
    if (normalizedText.isBlank()) {
        return
    }

    var cursor = 0
    StructuredFieldBoundaryIconRegexV2.findAll(normalizedText).forEach { match ->
        if (match.range.first > cursor) {
            tokens += StructuredFieldTokenV2(
                text = normalizedText.substring(cursor, match.range.first)
            )
        }

        tokens += StructuredFieldTokenV2(
            fieldType = classifyStructuredFieldTypeFromTextIconV2(match.value),
            isBoundary = true
        )
        cursor = match.range.last + 1
    }

    if (cursor < normalizedText.length) {
        tokens += StructuredFieldTokenV2(text = normalizedText.substring(cursor))
    }
}

private fun classifyStructuredFieldTypeFromTextIconV2(iconMarker: String): StructuredFieldTypeV2? {
    return when (iconMarker) {
        StructuredWeekIconV2 -> StructuredFieldTypeV2.WEEK
        StructuredPeriodIconV2 -> StructuredFieldTypeV2.PERIOD
        StructuredTeacherIconV2 -> StructuredFieldTypeV2.TEACHER
        StructuredClassroomIconV2 -> StructuredFieldTypeV2.CLASSROOM
        StructuredCreditIconV2 -> StructuredFieldTypeV2.CREDIT
        else -> null
    }
}

private fun buildStructuredFieldEntriesFromTokensV2(
    tokens: List<StructuredFieldTokenV2>
): List<Pair<StructuredFieldTypeV2, String>> {
    val entries = mutableListOf<Pair<StructuredFieldTypeV2, String>>()
    var currentType: StructuredFieldTypeV2? = null
    val currentText = StringBuilder()

    fun flush() {
        val fieldType = currentType ?: run {
            currentText.clear()
            return
        }

        val fieldText = extractMinimalStructuredFieldTextV2(currentText.toString())
        if (fieldText.isNotBlank()) {
            entries += fieldType to fieldText
        }
        currentText.clear()
    }

    tokens.forEach { token ->
        if (token.isBoundary) {
            flush()
            currentType = token.fieldType
            return@forEach
        }

        if (currentType == null) {
            return@forEach
        }

        if (currentText.isNotEmpty() && token.text.isNotBlank()) {
            currentText.append(' ')
        }
        currentText.append(token.text)
    }

    flush()
    return entries
}

private fun detectStructuredFieldTypeFromRowV2(row: Element): StructuredFieldTypeV2? {
    val iconElement = row.children()
        .firstOrNull { child -> classifyStructuredFieldTypeFromElementV2(child) != null }

    return classifyStructuredFieldTypeFromElementV2(iconElement ?: row)
}

private fun classifyStructuredFieldTypeFromElementV2(element: Element): StructuredFieldTypeV2? {
    val markerSource = buildString {
        append(element.classNames().joinToString(" "))
        append(' ')
        append(element.attr("aria-label"))
        append(' ')
        append(element.attr("data-icon"))
        append(' ')
        append(element.attr("title"))
        append(' ')
        append(element.ownText())
        append(' ')
        append(element.outerHtml())
    }.lowercase()

    return when {
        markerSource.contains("📅") ||
            markerSource.contains("calendar") ||
            markerSource.contains("date") -> StructuredFieldTypeV2.WEEK
        markerSource.contains("⏰") ||
            markerSource.contains("clock") ||
            markerSource.contains("time") ||
            markerSource.contains("schedule") -> StructuredFieldTypeV2.PERIOD
        markerSource.contains("👤") ||
            markerSource.contains("person") ||
            markerSource.contains("user") ||
            markerSource.contains("teacher") -> StructuredFieldTypeV2.TEACHER
        markerSource.contains("📍") ||
            markerSource.contains("location_on") ||
            markerSource.contains("location-on") ||
            markerSource.contains("locationon") ||
            markerSource.contains("location") ||
            markerSource.contains("place") ||
            markerSource.contains("pin-drop") ||
            markerSource.contains("mappin") ||
            markerSource.contains("marker") ||
            markerSource.contains("room") ||
            markerSource.contains("address") ||
            markerSource.contains("environment") -> StructuredFieldTypeV2.CLASSROOM
        markerSource.contains("school") ||
            markerSource.contains("graduation") ||
            markerSource.contains("mortarboard") ||
            markerSource.contains("cap") ||
            markerSource.contains("credit") -> StructuredFieldTypeV2.CREDIT
        else -> null
    }
}

private fun extractStructuredFieldTextFromRowV2(row: Element): String {
    val children = row.children().toList()
    if (children.isEmpty()) {
        return ""
    }

    val iconIndex = children.indexOfFirst { child ->
        classifyStructuredFieldTypeFromElementV2(child) != null
    }

    val text = when {
        iconIndex >= 0 && iconIndex + 1 < children.size -> {
            children.drop(iconIndex + 1).joinToString(" ") { child -> child.text() }
        }
        iconIndex >= 0 -> {
            row.text().replace(children[iconIndex].text(), " ")
        }
        else -> row.text()
    }

    return text.normalizeForParsing()
}

private fun extractBoldCourseNamesFromCellV2(cell: Element): List<String> {
    return cell.select("b, strong")
        .mapNotNull { element ->
            cleanupCourseNameFromHtmlCandidateV2(element.text())
                .takeIf { candidate -> candidate.isNotBlank() }
        }
        .distinct()
}

private fun resolveCourseNameFromHtmlV2(
    cell: Element,
    lines: List<String>,
    groupIndex: Int,
    groupCount: Int,
    boldCourseNames: List<String> = extractBoldCourseNamesFromCellV2(cell)
): String {
    val normalizedLines = lines
        .map { line -> line.normalizeDomSegmentV2() }
        .filter { line -> line.isNotBlank() && !line.isIgnorableDomSegmentV2() }
    val combinedText = normalizedLines.joinToString(" ")
    val starBasedName = normalizedLines.firstNotNullOfOrNull { line ->
        extractCourseNameBeforeStarV2(line)
    }

    val htmlBoldName = boldCourseNames.firstOrNull { candidate ->
        combinedText.contains(candidate) ||
            starBasedName?.let { name ->
                candidate.contains(name) || name.contains(candidate)
            } == true
    } ?: boldCourseNames.getOrNull(groupIndex)
        ?.takeIf { candidate ->
            groupCount == 1 || boldCourseNames.size == groupCount
        }

    return htmlBoldName?.takeIf { candidate -> candidate.isNotBlank() }
        ?: starBasedName.orEmpty()
}

private fun extractCourseNameBeforeStarV2(text: String): String? {
    val normalized = text.normalizeDomSegmentV2()
    val starIndex = normalized.indexOf('★')
    if (starIndex <= 0) {
        return null
    }

    return cleanupCourseNameFromHtmlCandidateV2(
        normalized.substring(0, starIndex)
    ).takeIf { candidate -> candidate.isNotBlank() }
}

private fun cleanupCourseNameFromHtmlCandidateV2(value: String): String {
    return value.normalizeDomSegmentV2()
        .substringBefore('★')
        .replace(Regex("""[()（）]"""), " ")
        .replace(Regex("""\s+"""), "")
        .trim()
}

private fun appendCreditToCourseNameV2(name: String, credit: String): String {
    val normalizedName = name.normalizeDomSegmentV2()
    val normalizedCredit = credit.normalizeDomSegmentV2()
    if (normalizedName.isBlank() || normalizedCredit.isBlank()) {
        return normalizedName
    }
    if (normalizedName.contains(normalizedCredit)) {
        return normalizedName
    }
    return "$normalizedName ($normalizedCredit)"
}

private fun extractMinimalStructuredFieldTextV2(text: String): String {
    return text.normalizeForParsing()
        .replace(Regex("""[\r\n]+"""), " ")
        .trim()
}

private fun extractWeekTextFromTimeFieldV2(text: String): String {
    val normalized = extractMinimalStructuredFieldTextV2(text)
    if (normalized.isBlank()) {
        return UnspecifiedWeekTextV2
    }

    return ScheduleWeekRegexV2.find(normalized)
        ?.value
        ?.compactCourseField()
        ?: UnspecifiedWeekTextV2
}

private fun extractCreditValueFromIconFieldV2(text: String): String {
    val normalized = extractMinimalStructuredFieldTextV2(text)
    if (normalized.isBlank()) {
        return ""
    }

    return Regex("""\d+(?:\.\d+)?""")
        .find(normalized)
        ?.value
        .orEmpty()
}

private fun extractCourseFieldsV2(
    lines: List<String>,
    combinedText: String
): ParsedCourseFieldsV2 {
    var weekText = extractLabeledFieldValueV2(lines, ScheduleWeekRegexV2)
        ?: extractWeekTextFromCellV2(lines, combinedText).takeIf { it.isNotBlank() }
        ?: ""
    var periodRange = extractPeriodRangeFromCellV2(combinedText)
    var classroom = extractLabeledLineValueV2(lines, ClassroomLabelRegexV2)
        ?.let { value -> cleanupClassroomValueV2(value) }
        .orEmpty()
    var credit = extractLabeledFieldValueV2(lines, ScheduleCreditRegexV2)
        ?: extractCreditFromCellV2(combinedText).takeIf { it.isNotBlank() }
        ?: ""

    val preferredNameCandidates = linkedSetOf<String>()
    val fallbackNameCandidates = linkedSetOf<String>()
    val teacherCandidates = linkedSetOf<String>()

    lines.forEach { originalLine ->
        val normalizedLine = originalLine.normalizeForParsing()
        if (normalizedLine.isBlank()) {
            return@forEach
        }

        if (weekText.isBlank() && ScheduleWeekRegexV2.containsMatchIn(normalizedLine)) {
            weekText = ScheduleWeekRegexV2.find(normalizedLine)?.value?.compactCourseField().orEmpty()
        }
        if (periodRange == null && SchedulePeriodRegexV2.containsMatchIn(normalizedLine)) {
            periodRange = extractPeriodRangeFromCellV2(normalizedLine)
        }
        if (credit.isBlank() && ScheduleCreditRegexV2.containsMatchIn(normalizedLine)) {
            credit = extractCreditFromCellV2(normalizedLine)
        }

        extractLabeledLineValueV2(listOf(normalizedLine), TeacherLabelRegexV2)
            ?.let { teacherValue ->
                teacherCandidates += splitTeacherNamesV2(teacherValue)
                return@forEach
            }

        extractLabeledLineValueV2(listOf(normalizedLine), ClassroomLabelRegexV2)
            ?.let { classroomValue ->
                if (classroom.isBlank()) {
                    classroom = cleanupClassroomValueV2(classroomValue)
                }
                return@forEach
            }

        extractLabeledLineValueV2(listOf(normalizedLine), CourseNameLabelRegexV2)
            ?.let { courseName ->
                preferredNameCandidates += cleanupCourseNameValueV2(courseName)
                return@forEach
            }

        extractLabeledLineValueV2(listOf(normalizedLine), CourseNameMarkerRegexV2)
            ?.let { courseName ->
                preferredNameCandidates += cleanupCourseNameValueV2(courseName)
                return@forEach
            }

        if (classroom.isBlank() && looksLikeClassroomFieldV2(normalizedLine)) {
            classroom = cleanupClassroomValueV2(normalizedLine)
            return@forEach
        }

        if (looksLikeTeacherFieldV2(normalizedLine)) {
            teacherCandidates += splitTeacherNamesV2(normalizedLine)
            return@forEach
        }

        if (looksLikeCourseNameFieldV2(normalizedLine)) {
            fallbackNameCandidates += cleanupCourseNameValueV2(normalizedLine)
        }
    }

    val teacher = teacherCandidates
        .map { value -> value.normalizeForParsing() }
        .filter { value -> value.isNotBlank() }
        .distinct()
        .joinToString(" / ")

    val name = preferredNameCandidates.firstOrNull { it.isNotBlank() }
        ?: fallbackNameCandidates.firstOrNull { it.isNotBlank() }
        ?: ""

    if (classroom.isBlank()) {
        classroom = extractClassroomFromCellV2(lines, combinedText)
    }

    val normalizedWeekText = weekText.ifBlank {
        extractWeekTextFromCellV2(lines, combinedText)
    }

    return ParsedCourseFieldsV2(
        name = name,
        teacher = teacher,
        classroom = classroom,
        weekText = normalizedWeekText,
        credit = credit,
        periodRange = periodRange,
        weekRule = parseWeekRuleFromText(normalizedWeekText)
    )
}

private fun extractLabeledFieldValueV2(
    lines: List<String>,
    regex: Regex
): String? {
    return lines.firstNotNullOfOrNull { line ->
        regex.find(line.normalizeForParsing())
            ?.let { match ->
                match.groupValues
                    .drop(1)
                    .lastOrNull { groupValue -> groupValue.isNotBlank() }
                    ?.normalizeForParsing()
                    ?.takeIf { value -> value.isNotBlank() }
                    ?: match.value.normalizeForParsing().takeIf { value -> value.isNotBlank() }
            }
    }
}

private fun extractLabeledLineValueV2(
    lines: List<String>,
    regex: Regex
): String? {
    return extractLabeledFieldValueV2(lines, regex)
}

private fun splitTeacherNamesV2(value: String): List<String> {
    return value.normalizeForParsing()
        .replace("教师", "")
        .replace("老师", "")
        .replace("任课教师", "")
        .replace("授课教师", "")
        .split(Regex("[、/,， ]+"))
        .map { token -> token.trim() }
        .filter { token -> TeacherNameRegexV2.matches(token) }
}

private fun cleanupCourseNameValueV2(value: String): String {
    return value.normalizeForParsing()
        .replace("课程名", "")
        .replace("课程名称", "")
        .replace("★", "")
        .replace("教师", "")
        .replace("教室", "")
        .substringBefore("周")
        .substringBefore("节")
        .normalizeForParsing()
}

private fun cleanupClassroomValueV2(value: String): String {
    return value.normalizeForParsing()
        .replace("教室", "")
        .replace("上课地点", "")
        .replace("地点", "")
        .normalizeForParsing()
}

private fun looksLikeTeacherFieldV2(text: String): Boolean {
    val normalized = text.normalizeForParsing()
    if (normalized.isBlank() || looksLikeClassroomFieldV2(normalized)) {
        return false
    }
    if (TeacherLabelRegexV2.containsMatchIn(normalized)) {
        return true
    }
    return TeacherNameRegexV2.matches(normalized)
}

private fun looksLikeClassroomFieldV2(text: String): Boolean {
    val normalized = text.normalizeForParsing()
    if (normalized.isBlank()) {
        return false
    }
    if (TeacherLabelRegexV2.containsMatchIn(normalized) || TeacherNameRegexV2.matches(normalized)) {
        return false
    }
    return ClassroomLabelRegexV2.containsMatchIn(normalized) || ClassroomHintRegexV2.containsMatchIn(normalized)
}

private fun looksLikeCourseNameFieldV2(text: String): Boolean {
    val normalized = text.normalizeForParsing()
    if (normalized.isBlank()) {
        return false
    }
    if (
        looksLikeTeacherFieldV2(normalized) ||
        looksLikeClassroomFieldV2(normalized) ||
        ScheduleWeekRegexV2.containsMatchIn(normalized) ||
        SchedulePeriodRegexV2.containsMatchIn(normalized) ||
        ScheduleCreditRegexV2.containsMatchIn(normalized)
    ) {
        return false
    }
    if (CourseNameLabelRegexV2.containsMatchIn(normalized) || CourseNameMarkerRegexV2.containsMatchIn(normalized)) {
        return true
    }
    return normalized.length >= 3
}

private data class ParsedCourseFieldsV2(
    val name: String,
    val teacher: String,
    val classroom: String,
    val weekText: String,
    val credit: String,
    val periodRange: IntRange?,
    val weekRule: WeekRule
)

private data class StructuredCourseCellResolved(
    val name: String,
    val teacher: String,
    val classroom: String,
    val weekText: String,
    val credit: String,
    val periodRange: IntRange?,
    val weekRule: WeekRule
)

private fun extractDomSegmentsFromCellV2(cell: Element): List<String> {
    val segments = linkedSetOf<String>()

    fun addSegment(rawText: String) {
        val normalized = rawText.normalizeDomSegmentV2()
        if (normalized.isNotBlank() && !normalized.isIgnorableDomSegmentV2()) {
            segments += normalized
        }
    }

    addSegment(cell.ownText())
    cell.children().forEach { child ->
        collectDomSegmentsFromNodeV2(child, ::addSegment)
    }

    if (segments.isEmpty()) {
        cell.childNodes().forEach { childNode ->
            collectDomSegmentsFromNodeV2(childNode, ::addSegment)
        }
    }

    return segments.toList()
}

private fun collectDomSegmentsFromNodeV2(
    node: Node,
    addSegment: (String) -> Unit
) {
    when (node) {
        is TextNode -> addSegment(node.text())
        is Element -> {
            addSegment(node.ownText())
            node.children().forEach { child ->
                collectDomSegmentsFromNodeV2(child, addSegment)
            }
        }
    }
}

private fun extractCourseFieldsFromDomSegmentsV2(
    segments: List<String>
): ParsedCourseFieldsV2 {
    var weekText = ""
    var credit = ""
    var classroom = ""
    var periodRange: IntRange? = null
    val nameCandidates = linkedSetOf<String>()
    val teacherCandidates = linkedSetOf<String>()

    segments.forEach { segment ->
        val normalized = segment.normalizeDomSegmentV2()
        if (normalized.isBlank() || normalized.isIgnorableDomSegmentV2()) {
            return@forEach
        }

        extractLabeledValueFromSegmentV2(normalized, CourseNameLabelRegexV2)
            ?.let { value ->
                cleanupCourseNameDomValueV2(value)
                    .takeIf { candidate -> candidate.isNotBlank() }
                    ?.let { candidate -> nameCandidates += candidate }
                return@forEach
            }

        extractLabeledValueFromSegmentV2(normalized, CourseNameMarkerRegexV2)
            ?.let { value ->
                cleanupCourseNameDomValueV2(value)
                    .takeIf { candidate -> candidate.isNotBlank() }
                    ?.let { candidate -> nameCandidates += candidate }
                return@forEach
            }

        extractLabeledValueFromSegmentV2(normalized, TeacherLabelRegexV2)
            ?.let { value ->
                teacherCandidates += extractTeacherNamesFromSegmentV2(value)
                return@forEach
            }

        extractLabeledValueFromSegmentV2(normalized, ClassroomLabelRegexV2)
            ?.let { value ->
                cleanupClassroomDomValueV2(value)
                    .takeIf { candidate -> candidate.isNotBlank() }
                    ?.let { candidate -> classroom = candidate }
                return@forEach
            }

        if (credit.isBlank() && ScheduleCreditRegexV2.containsMatchIn(normalized)) {
            credit = extractCreditFromCellV2(normalized)
            if (credit.isNotBlank()) {
                return@forEach
            }
        }

        if (periodRange == null && normalized.contains("节")) {
            periodRange = extractPeriodRangeFromCellV2(normalized)
            if (periodRange != null) {
                return@forEach
            }
        }

        if (weekText.isBlank() && normalized.contains("周")) {
            weekText = ScheduleWeekRegexV2.find(normalized)?.value?.compactCourseField()
                ?: normalized
            return@forEach
        }

        if (classroom.isBlank() && looksLikeClassroomDomSegmentV2(normalized)) {
            classroom = cleanupClassroomDomValueV2(normalized)
            return@forEach
        }

        if (looksLikeTeacherDomSegmentV2(normalized)) {
            teacherCandidates += extractTeacherNamesFromSegmentV2(normalized)
            return@forEach
        }

        if (looksLikeCourseNameDomSegmentV2(normalized)) {
            cleanupCourseNameDomValueV2(normalized)
                .takeIf { candidate -> candidate.isNotBlank() }
                ?.let { candidate -> nameCandidates += candidate }
        }
    }

    val resolvedWeekText = weekText.takeIf { value -> value.isNotBlank() }.orEmpty()
    val resolvedTeacher = teacherCandidates
        .map { value -> value.normalizeDomSegmentV2() }
        .filter { value -> value.isNotBlank() }
        .distinct()
        .joinToString(" / ")
    val resolvedName = nameCandidates.firstOrNull { value -> value.isNotBlank() }.orEmpty()

    return ParsedCourseFieldsV2(
        name = resolvedName,
        teacher = resolvedTeacher,
        classroom = classroom,
        weekText = resolvedWeekText,
        credit = credit,
        periodRange = periodRange,
        weekRule = parseWeekRuleFromText(resolvedWeekText)
    )
}

private fun extractLabeledValueFromSegmentV2(
    segment: String,
    regex: Regex
): String? {
    return regex.find(segment)
        ?.groupValues
        ?.drop(1)
        ?.lastOrNull { value -> value.isNotBlank() }
        ?.normalizeDomSegmentV2()
        ?.takeIf { value -> value.isNotBlank() }
}

private fun extractTeacherNamesFromSegmentV2(segment: String): List<String> {
    return segment.normalizeDomSegmentV2()
        .replace(TeacherKeywordRegexV2, " ")
        .replace(Regex("""[:：]"""), " ")
        .split(Regex("""[、/,， ]+"""))
        .map { token -> token.normalizeDomSegmentV2() }
        .filter { token -> TeacherNameRegexV2.matches(token) }
}

private fun cleanupCourseNameDomValueV2(value: String): String {
    return value.normalizeDomSegmentV2()
        .replace(CourseNameLabelRegexV2, "$1")
        .replace(CourseNameMarkerRegexV2, "$1")
        .replace(TeacherLabelRegexV2, "")
        .replace(ClassroomLabelRegexV2, "")
        .replace(ScheduleWeekRegexV2, "")
        .replace(SchedulePeriodRegexV2, "")
        .replace(ScheduleCreditRegexV2, "")
        .replace(Regex("""^[()（）【】\[\]\s]+|[()（）【】\[\]\s]+$"""), "")
        .normalizeDomSegmentV2()
}

private fun cleanupClassroomDomValueV2(value: String): String {
    return value.normalizeDomSegmentV2()
        .replace(ClassroomLabelRegexV2, "$1")
        .replace(Regex("""^[()（）【】\[\]\s]+|[()（）【】\[\]\s]+$"""), "")
        .normalizeDomSegmentV2()
}

private fun looksLikeTeacherDomSegmentV2(segment: String): Boolean {
    val normalized = segment.normalizeDomSegmentV2()
    if (normalized.isBlank() || looksLikeClassroomDomSegmentV2(normalized)) {
        return false
    }
    if (TeacherLabelRegexV2.containsMatchIn(normalized)) {
        return true
    }
    return TeacherNameRegexV2.matches(normalized)
}

private fun looksLikeClassroomDomSegmentV2(segment: String): Boolean {
    val normalized = segment.normalizeDomSegmentV2()
    if (normalized.isBlank() || normalized.isIgnorableDomSegmentV2()) {
        return false
    }
    if (TeacherNameRegexV2.matches(normalized)) {
        return false
    }
    return ClassroomLabelRegexV2.containsMatchIn(normalized) ||
        ClassroomHintRegexV2.containsMatchIn(normalized)
}

private fun looksLikeCourseNameDomSegmentV2(segment: String): Boolean {
    val normalized = segment.normalizeDomSegmentV2()
    if (
        normalized.isBlank() ||
        normalized.isIgnorableDomSegmentV2() ||
        normalized.contains("周") ||
        normalized.contains("节") ||
        looksLikeTeacherDomSegmentV2(normalized) ||
        looksLikeClassroomDomSegmentV2(normalized) ||
        ScheduleCreditRegexV2.containsMatchIn(normalized)
    ) {
        return false
    }

    if (CourseNameLabelRegexV2.containsMatchIn(normalized) || CourseNameMarkerRegexV2.containsMatchIn(normalized)) {
        return true
    }

    return normalized.any { character ->
        character.isLetter() || Character.UnicodeScript.of(character.code) == Character.UnicodeScript.HAN
    }
}

private fun extractCourseFieldsStrictV2(
    segments: List<String>,
    structuredFields: StructuredFieldGroupV2? = null
): ParsedCourseFieldsV2 {
    val timeFieldText = extractMinimalStructuredFieldTextV2(structuredFields?.periodText.orEmpty())
    val weekText = extractWeekTextFromTimeFieldV2(timeFieldText)
    val credit = extractCreditValueFromIconFieldV2(structuredFields?.credit.orEmpty())
    val classroom = extractMinimalStructuredFieldTextV2(structuredFields?.classroom.orEmpty())
    val periodRange = extractPeriodRangeFromCellV2(timeFieldText)
    val teacher = extractMinimalStructuredFieldTextV2(structuredFields?.teacher.orEmpty())

    return ParsedCourseFieldsV2(
        name = "",
        teacher = teacher,
        classroom = classroom,
        weekText = weekText,
        credit = credit,
        periodRange = periodRange,
        weekRule = parseWeekRuleStrictFromTextV2(weekText)
    )
}

private fun extractWeekTextStrictV2(segment: String): String {
    val normalized = segment.normalizeDomSegmentV2()
    val ranges = ScheduleWeekRegexV2.findAll(normalized)
        .map { match -> buildWeekTextFromMatchV2(match) }
        .distinct()
        .toList()

    return ranges.joinToString("/")
}

private fun extractTeacherNamesStrictV2(segment: String): List<String> {
    return segment.normalizeDomSegmentV2()
        .replace(TeacherKeywordStrictRegexV2, " ")
        .replace(Regex("""[:：]"""), " ")
        .split(Regex("""[、/,，\s]+"""))
        .map { token -> token.normalizeDomSegmentV2() }
        .filter { token ->
            token.isNotBlank() &&
                TeacherNameRegexV2.matches(token) &&
                extractWeekTextStrictV2(token).isBlank() &&
                extractPeriodRangeFromCellV2(token) == null &&
                extractClassroomFromDomSegmentStrictV2(token).isBlank()
        }
}

private fun extractClassroomFromDomSegmentStrictV2(segment: String): String {
    val normalized = segment.normalizeDomSegmentV2()
        .replace(ClassroomLabelRegexV2, "$1")
        .replace(Regex("""^[()（）\[\]\s-]+|[()（）\[\]\s-]+$"""), "")
        .normalizeDomSegmentV2()
    if (normalized.isBlank() || normalized.isIgnorableDomSegmentV2()) {
        return ""
    }
    if (extractWeekTextStrictV2(normalized).isNotBlank() || extractPeriodRangeFromCellV2(normalized) != null) {
        return ""
    }
    if (ClassroomKeywordRegexV2.containsMatchIn(normalized)) {
        return normalized
    }

    val classroomCode = ClassroomCodeRegexV2.find(normalized)?.value
        ?.normalizeDomSegmentV2()
        .orEmpty()
    if (classroomCode.isBlank()) {
        return ""
    }

    val compactNormalized = normalized.replace(" ", "")
    val compactCode = classroomCode.replace(" ", "")
    return if (compactNormalized == compactCode) classroomCode else ""
}

private fun cleanupCourseNameStrictV2(value: String): String {
    return value.normalizeDomSegmentV2()
        .replace(CourseNameLabelRegexV2, "$1")
        .replace(CourseNameMarkerRegexV2, "$1")
        .replace(TeacherLabelRegexV2, "")
        .replace(ClassroomLabelRegexV2, "")
        .replace("\u2605", "")
        .replace(ScheduleWeekRegexV2, "")
        .replace(SchedulePeriodRegexV2, "")
        .replace(ScheduleCreditRegexV2, "")
        .replace(Regex("""^[()（）\[\]\s-]+|[()（）\[\]\s-]+$"""), "")
        .normalizeDomSegmentV2()
}

private fun looksLikeTeacherCandidateStrictV2(value: String): Boolean {
    val normalized = value.normalizeDomSegmentV2()
    if (
        normalized.isBlank() ||
        normalized.isIgnorableDomSegmentV2() ||
        extractWeekTextStrictV2(normalized).isNotBlank() ||
        extractPeriodRangeFromCellV2(normalized) != null ||
        extractClassroomFromDomSegmentStrictV2(normalized).isNotBlank()
    ) {
        return false
    }

    val tokens = normalized.split(Regex("""[、/,，\s]+"""))
        .map { token -> token.normalizeDomSegmentV2() }
        .filter { token -> token.isNotBlank() }

    return tokens.isNotEmpty() && tokens.all { token -> TeacherNameRegexV2.matches(token) }
}

private fun looksLikeStrongCourseNameStrictV2(value: String): Boolean {
    val normalized = cleanupCourseNameStrictV2(value)
    if (normalized.isBlank()) {
        return false
    }

    return normalized.length > 4 ||
        normalized.any { character ->
            character.isDigit() || character in 'A'..'Z' || character in 'a'..'z'
        } ||
        CourseTitleKeywordRegexV2.containsMatchIn(normalized)
}

private fun buildWeekTextFromMatchV2(match: MatchResult): String {
    val startWeek = match.groupValues.getOrNull(1).orEmpty()
    val endWeek = match.groupValues.getOrNull(2).orEmpty()
    val parity = match.groupValues.getOrNull(3).orEmpty()

    return buildString {
        append(startWeek)
        if (endWeek.isNotBlank()) {
            append("-")
            append(endWeek)
        }
        append("\u5468")
        if (parity.isNotBlank()) {
            append("(")
            append(parity)
            append(")")
        }
    }
}

private fun extractTeacherNamesRegexStrictV3(segment: String): List<String> {
    return segment.normalizeDomSegmentV2()
        .replace(TeacherKeywordStrictRegexV2, " ")
        .replace(Regex("""[:：]"""), " ")
        .split(Regex("""[\s,，、]+"""))
        .map { token -> token.normalizeDomSegmentV2() }
        .filter { token ->
            token.isNotBlank() &&
                TeacherLineRegexV2.matches(token) &&
                !ScheduleWeekRegexV2.containsMatchIn(token) &&
                extractPeriodRangeFromCellV2(token) == null &&
                extractClassroomRegexStrictV3(token).isBlank() &&
                !token.contains("\u5468")
        }
}

private fun extractClassroomRegexStrictV3(segment: String): String {
    val normalized = segment.normalizeDomSegmentV2()
        .replace(ClassroomLabelRegexV2, "$1")
        .replace(Regex("""^[()（）\[\]\s]+|[()（）\[\]\s]+$"""), "")
        .normalizeDomSegmentV2()
    if (normalized.isBlank() || normalized.isIgnorableDomSegmentV2()) {
        return ""
    }
    if (ScheduleWeekRegexV2.containsMatchIn(normalized) || extractPeriodRangeFromCellV2(normalized) != null) {
        return ""
    }
    if (!ClassroomLineRegexV2.matches(normalized)) {
        return ""
    }

    return when {
        ClassroomKeywordRegexV2.containsMatchIn(normalized) -> normalized
        Regex("""^[A-Z]\d{3}$""").matches(normalized) -> normalized
        Regex("""^\d{3,}$""").matches(normalized) -> normalized
        else -> ClassroomCodeRegexV2.find(normalized)?.value.orEmpty()
    }
}

private fun extractValidatedClassroomFromLocationFieldV2(text: String): String {
    val normalized = text.normalizeDomSegmentV2()
    if (normalized.isBlank()) {
        return ""
    }

    val candidates = normalized.split(Regex("""[，,、/;；]"""))
        .map { segment -> segment.normalizeDomSegmentV2() }
        .filter { segment -> segment.isNotBlank() }
        .ifEmpty { listOf(normalized) }

    candidates.forEach { candidate ->
        if (CreditDecimalOnlyRegexV2.matches(candidate) || ShortNumericTextRegexV2.matches(candidate)) {
            return@forEach
        }

        if (ClassroomKeywordRegexV2.containsMatchIn(candidate)) {
            return candidate
        }

        ClassroomAlphaCodeRegexV2.find(candidate)?.value?.let { classroomCode ->
            return classroomCode
        }

        ClassroomNumericCodeRegexV2.find(candidate)?.value?.let { classroomCode ->
            return classroomCode
        }
    }

    return ""
}

private fun cleanupCourseNameRegexStrictV3(value: String): String {
    return value.normalizeDomSegmentV2()
        .replace(CourseNameLabelRegexV2, "$1")
        .replace(CourseNameMarkerRegexV2, "$1")
        .replace(TeacherLabelRegexV2, "")
        .replace(ClassroomLabelRegexV2, "")
        .replace(TeacherKeywordStrictRegexV2, "")
        .replace("\u2605", "")
        .replace(ScheduleWeekRegexV2, "")
        .replace(SchedulePeriodRegexV2, "")
        .replace(ScheduleCreditRegexV2, "")
        .replace(Regex("""[()（）\s]"""), "")
        .trim()
}

private fun looksLikeTeacherCandidateRegexStrictV3(value: String): Boolean {
    val normalized = value.normalizeDomSegmentV2()
    if (
        normalized.isBlank() ||
        normalized.isIgnorableDomSegmentV2() ||
        ScheduleWeekRegexV2.containsMatchIn(normalized) ||
        extractPeriodRangeFromCellV2(normalized) != null ||
        extractClassroomRegexStrictV3(normalized).isNotBlank()
    ) {
        return false
    }

    return normalized.split("/")
        .map { token -> token.normalizeDomSegmentV2() }
        .filter { token -> token.isNotBlank() }
        .all { token -> TeacherLineRegexV2.matches(token) && !token.contains("\u5468") }
}

private fun looksLikeCourseNameCandidateStrictV2(value: String): Boolean {
    val normalized = cleanupCourseNameRegexStrictV3(value)
    if (normalized.isBlank()) {
        return false
    }

    if (
        ScheduleWeekRegexV2.containsMatchIn(normalized) ||
        extractPeriodRangeFromCellV2(normalized) != null ||
        extractClassroomRegexStrictV3(normalized).isNotBlank() ||
        looksLikeTeacherCandidateRegexStrictV3(normalized)
    ) {
        return false
    }

    return normalized.any { character ->
        character.isLetter() || Character.UnicodeScript.of(character.code) == Character.UnicodeScript.HAN
    }
}

private fun parseWeekRuleStrictFromTextV2(weekText: String): WeekRule {
    val normalizedWeekText = weekText.normalizeForParsing()
    if (normalizedWeekText.isBlank()) {
        return WeekRule()
    }

    val ranges = ScheduleWeekRegexV2.findAll(normalizedWeekText)
        .mapNotNull { match ->
            val startWeek = match.groupValues.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
            val endWeek = match.groupValues.getOrNull(2)?.toIntOrNull() ?: startWeek
            startWeek..endWeek.coerceAtLeast(startWeek)
        }
        .toList()

    val parityMode = when {
        ScheduleWeekRegexV2.findAll(normalizedWeekText).any { match ->
            match.groupValues.getOrNull(3) == "\u5355"
        } -> WeekParityMode.ODD
        ScheduleWeekRegexV2.findAll(normalizedWeekText).any { match ->
            match.groupValues.getOrNull(3) == "\u53CC"
        } -> WeekParityMode.EVEN
        else -> WeekParityMode.ALL
    }

    return WeekRule(
        ranges = ranges,
        parityMode = parityMode
    )
}

private fun String.normalizeDomSegmentV2(): String {
    return normalizeForParsing()
        .replace(Regex("""\s+"""), " ")
        .trim()
}

private fun String.isIgnorableDomSegmentV2(): Boolean {
    val normalized = trim()
    return normalized.isBlank() ||
        normalized == "(" ||
        normalized == ")" ||
        normalized == "（" ||
        normalized == "）" ||
        Regex("""^[()（）【】\[\]\-]+$""").matches(normalized)
}

@Composable
fun HeaderMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF0F172A)
                )
            }
        },
        onClick = onClick
    )
}

@Composable
fun WeekDateItem(
    date: LocalDate,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = date.toShortWeekText(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
            color = if (isToday) Color(0xFF0F172A) else Color(0xFF94A3B8)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .background(
                    color = if (isToday) Color(0xFF1E293B) else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 10.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isToday) Color.White else Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
fun WakeUpScheduleBoard(
    dayCount: Int,
    timeSlots: List<TimeSlot>,
    courses: List<CourseBlockInfo>,
    modifier: Modifier = Modifier,
    onCourseClick: (CourseBlockInfo) -> Unit
) {
    val courseAreaStart = ScheduleTimeAxisWidth + ScheduleAxisToCourseGap
    val boardHeight = calculateBodyHeight(
        periodCount = timeSlots.size,
        periodHeight = SchedulePeriodHeight,
        periodGap = SchedulePeriodGap
    )

    BoxWithConstraints(
        modifier = modifier.height(boardHeight)
    ) {
        val dayColumnWidth = (maxWidth - courseAreaStart) / dayCount

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC), RoundedCornerShape(22.dp))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(22.dp))
                .padding(6.dp)
        ) {
            repeat(dayCount) { index ->
                Box(
                    modifier = Modifier
                        .offset(
                            x = courseAreaStart + dayColumnWidth * index,
                            y = 0.dp
                        )
                        .width(dayColumnWidth - 4.dp)
                        .height(boardHeight - 6.dp)
                        .background(Color.White, RoundedCornerShape(18.dp))
                )
            }

            timeSlots.forEachIndexed { index, slot ->
                ScheduleTimeAxisItem(
                    slot = slot,
                    modifier = Modifier
                        .offset(
                            x = ScheduleTimeAxisOffsetX,
                            y = calculatePeriodOffset(
                                periodIndex = index,
                                periodHeight = SchedulePeriodHeight,
                                periodGap = SchedulePeriodGap
                            )
                        )
                        .width(ScheduleTimeAxisWidth)
                        .height(SchedulePeriodHeight)
                )
            }

            courses.forEach { course ->
                CourseBlock(
                    course = course,
                    modifier = Modifier
                        .offset(
                            x = courseAreaStart + dayColumnWidth * course.dayIndex + ScheduleCourseInset,
                            y = calculatePeriodOffset(
                                periodIndex = course.startPeriod - 1,
                                periodHeight = SchedulePeriodHeight,
                                periodGap = SchedulePeriodGap
                            ) + ScheduleCourseInset
                        )
                        .width(dayColumnWidth - ScheduleCourseInset * 2 - 2.dp)
                        .height(
                            calculateCourseHeight(
                                duration = course.duration,
                                periodHeight = SchedulePeriodHeight,
                                periodGap = SchedulePeriodGap
                            ) - ScheduleCourseInset * 2
                        ),
                    onClick = { onCourseClick(course) }
                )
            }
        }
    }
}

@Composable
fun ScheduleTimeAxisItem(slot: TimeSlot, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(top = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = slot.period.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = slot.startTime,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = slot.endTime,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun CourseBlock(
    course: CourseBlockInfo,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val locationText = course.location.toCourseCardLocationText()

    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(course.backgroundColor, RoundedCornerShape(18.dp))
            .border(1.dp, course.backgroundColor.copy(alpha = 0.9f), RoundedCornerShape(18.dp))
            .padding(horizontal = 4.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = course.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = locationText,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF334155),
                maxLines = 2,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailBottomSheet(
    course: CourseBlockInfo,
    timeSlots: List<TimeSlot>,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        shape = RoundedCornerShape(0.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(
                            topStart = 24.dp,
                            topEnd = 24.dp,
                            bottomStart = 28.dp,
                            bottomEnd = 28.dp
                        )
                    )
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(42.dp)
                            .height(5.dp)
                            .background(Color(0xFFD4D9E3), RoundedCornerShape(999.dp))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = course.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.weight(1f)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                            CourseActionButton(
                                text = "\u5220\u9664",
                                textColor = Color(0xFFDC2626),
                                onClick = onDelete
                            )
                            CourseActionButton(
                                text = "\u590d\u5236",
                                textColor = Color(0xFF64748B),
                                onClick = onCopy
                            )
                            CourseActionButton(
                                text = "\u7f16\u8f91",
                                textColor = Color(0xFF64748B),
                                onClick = onEdit
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CourseDetailRow(
                            icon = Icons.Filled.DateRange,
                            iconTint = Color(0xFF6366F1),
                            label = "\u5468\u6570",
                            value = course.weekText
                        )
                        CourseDetailRow(
                            icon = Icons.Filled.AccessTime,
                            iconTint = Color(0xFF10B981),
                            label = "\u8282\u6b21",
                            value = buildCoursePeriodText(course, timeSlots)
                        )
                        CourseDetailRow(
                            icon = Icons.Filled.Person,
                            iconTint = Color(0xFFF59E0B),
                            label = "\u6559\u5e08",
                            value = course.teacher
                        )
                        CourseDetailRow(
                            icon = Icons.Filled.LocationOn,
                            iconTint = Color(0xFFEF4444),
                            label = "\u6559\u5ba4",
                            value = course.location
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun CourseActionButton(
    text: String,
    textColor: Color,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun CourseDetailRow(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(iconTint.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF64748B)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun calculateCurrentWeek(
    semesterStartDate: LocalDate,
    today: LocalDate
): Int {
    val semesterMonday = semesterStartDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val currentMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return (ChronoUnit.WEEKS.between(semesterMonday, currentMonday) + 1L)
        .toInt()
        .coerceAtLeast(1)
}

// Week 1 is anchored to the Monday of `semesterStartDate`.
private fun calculateWeekDates(
    semesterStartDate: LocalDate,
    currentWeek: Int
): List<LocalDate> {
    val semesterMonday = semesterStartDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val targetWeekMonday = semesterMonday.plusWeeks((currentWeek - 1).coerceAtLeast(0).toLong())
    return List(7) { index ->
        targetWeekMonday.plusDays(index.toLong())
    }
}

private fun resolveDisplayMonth(
    weekDates: List<LocalDate>,
    today: LocalDate
): Int {
    return weekDates.firstOrNull { it == today }?.monthValue ?: weekDates.first().monthValue
}

private fun LocalDate.toChineseWeekText(): String {
    return when (dayOfWeek) {
        DayOfWeek.MONDAY -> "\u5468\u4e00"
        DayOfWeek.TUESDAY -> "\u5468\u4e8c"
        DayOfWeek.WEDNESDAY -> "\u5468\u4e09"
        DayOfWeek.THURSDAY -> "\u5468\u56db"
        DayOfWeek.FRIDAY -> "\u5468\u4e94"
        DayOfWeek.SATURDAY -> "\u5468\u516d"
        DayOfWeek.SUNDAY -> "\u5468\u65e5"
    }
}

private fun LocalDate.toShortWeekText(): String {
    return when (dayOfWeek) {
        DayOfWeek.MONDAY -> "\u4e00"
        DayOfWeek.TUESDAY -> "\u4e8c"
        DayOfWeek.WEDNESDAY -> "\u4e09"
        DayOfWeek.THURSDAY -> "\u56db"
        DayOfWeek.FRIDAY -> "\u4e94"
        DayOfWeek.SATURDAY -> "\u516d"
        DayOfWeek.SUNDAY -> "\u65e5"
    }
}

private fun LocalDate.toSlashDateText(): String {
    return "$year/$monthValue/$dayOfMonth"
}

private fun calculateBodyHeight(periodCount: Int, periodHeight: Dp, periodGap: Dp): Dp {
    return periodHeight * periodCount + periodGap * (periodCount - 1)
}

private fun calculatePeriodOffset(periodIndex: Int, periodHeight: Dp, periodGap: Dp): Dp {
    return (periodHeight + periodGap) * periodIndex
}

private fun calculateCourseHeight(duration: Int, periodHeight: Dp, periodGap: Dp): Dp {
    return periodHeight * duration + periodGap * (duration - 1)
}

private fun buildCoursePeriodText(
    course: CourseBlockInfo,
    timeSlots: List<TimeSlot>
): String {
    val endPeriod = course.startPeriod + course.duration - 1
    val startSlot = timeSlots.firstOrNull { it.period == course.startPeriod }
    val endSlot = timeSlots.firstOrNull { it.period == endPeriod }
    val periodText = if (course.duration == 1) {
        "${course.startPeriod}\u8282"
    } else {
        "${course.startPeriod}-${endPeriod}\u8282"
    }
    val timeText = if (startSlot != null && endSlot != null) {
        "${startSlot.startTime}-${endSlot.endTime}"
    } else {
        ""
    }
    return if (timeText.isBlank()) periodText else "$periodText  $timeText"
}

private fun List<Course>.toCourseBlockInfoList(): List<CourseBlockInfo> {
    return mapIndexedNotNull { index, course ->
        val dayIndex = course.dayOfWeek.toDayIndex() ?: return@mapIndexedNotNull null
        val periodRange = course.periodText.toPeriodRange() ?: return@mapIndexedNotNull null
        val duration = (periodRange.last - periodRange.first + 1).coerceAtLeast(1)

        CourseBlockInfo(
            id = index + 1L,
            dayIndex = dayIndex,
            startPeriod = periodRange.first,
            duration = duration,
            name = course.name.ifBlank { "\u672a\u547d\u540d\u8bfe\u7a0b" },
            weekText = course.weekText.ifBlank { "\u672a\u6807\u6ce8\u5468\u6570" },
            teacher = course.teacher.ifBlank { "\u5f85\u8865\u5145" },
            location = course.classroom.ifBlank { "\u5f85\u5b89\u6392" },
            weekRanges = course.weekRanges,
            weekParityMode = course.weekParityMode,
            backgroundColor = ImportedCourseBlockColors[index % ImportedCourseBlockColors.size]
        )
    }
}

private fun List<Course>.toCourseEntities(): List<CourseEntity> {
    return map { course ->
        CourseEntity(
            name = course.name,
            teacher = course.teacher,
            classroom = course.classroom,
            weekText = course.weekText,
            periodText = course.periodText,
            dayOfWeek = course.dayOfWeek,
            credit = course.credit
        )
    }
}

private fun List<CourseEntity>.toStoredCourseBlockInfoList(): List<CourseBlockInfo> {
    return map { entity ->
        entity.toCourse()
    }.toCourseBlockInfoList()
}

private fun CourseEntity.toCourse(): Course {
    val weekRule = parseWeekRuleFromText(weekText)
    return Course(
        name = name,
        periodText = periodText,
        dayOfWeek = dayOfWeek,
        classroom = classroom,
        weekText = weekText,
        teacher = teacher,
        credit = credit,
        weekRanges = weekRule.ranges,
        weekParityMode = weekRule.parityMode
    )
}

private fun String.toCourseCardLocationText(): String {
    return normalizeForParsing()
        .replace("下沙", "")
        .trim()
        .ifBlank { "--" }
}

private fun String.toDayIndex(): Int? {
    return when (normalizeForParsing()) {
        "\u5468\u4e00", "\u661f\u671f\u4e00" -> 0
        "\u5468\u4e8c", "\u661f\u671f\u4e8c" -> 1
        "\u5468\u4e09", "\u661f\u671f\u4e09" -> 2
        "\u5468\u56db", "\u661f\u671f\u56db" -> 3
        "\u5468\u4e94", "\u661f\u671f\u4e94" -> 4
        "\u5468\u516d", "\u661f\u671f\u516d" -> 5
        "\u5468\u65e5", "\u661f\u671f\u65e5", "\u661f\u671f\u5929" -> 6
        else -> null
    }
}

private fun String.toPeriodRange(): IntRange? {
    val periodMatch = Regex("""(\d{1,2})(?:\D+(\d{1,2}))?""").find(this) ?: return null
    val startPeriod = periodMatch.groupValues[1].toIntOrNull() ?: return null
    val endPeriod = periodMatch.groupValues[2].toIntOrNull() ?: startPeriod
    return startPeriod..endPeriod.coerceAtLeast(startPeriod)
}

private fun CourseBlockInfo.isVisibleInWeek(currentWeek: Int): Boolean {
    if (weekRanges.isNotEmpty() || weekParityMode != WeekParityMode.ALL) {
        return WeekRule(
            ranges = weekRanges,
            parityMode = weekParityMode
        ).contains(currentWeek)
    }

    val fallbackRule = parseWeekRuleFromText(weekText)
    return if (fallbackRule.isEmpty()) {
        true
    } else {
        fallbackRule.contains(currentWeek)
    }
}

private fun CourseBlockInfo.isActiveInWeek(currentWeek: Int): Boolean {
    val normalizedWeekText = weekText.normalizeForParsing()
    if (normalizedWeekText.isBlank()) {
        return true
    }

    val hasMatchedRange = Regex("""(\d{1,2})(?:\s*[-~到]\s*(\d{1,2}))?""")
        .findAll(normalizedWeekText)
        .mapNotNull { match ->
            val startWeek = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
            val endWeek = match.groupValues[2].toIntOrNull() ?: startWeek
            startWeek..endWeek.coerceAtLeast(startWeek)
        }
        .any { weekRange -> currentWeek in weekRange }

    if (!hasMatchedRange) {
        return true
    }

    return when {
        normalizedWeekText.contains("单") -> currentWeek % 2 == 1
        normalizedWeekText.contains("双") -> currentWeek % 2 == 0
        else -> true
    }
}

data class TimeSlot(
    val period: Int,
    val startTime: String,
    val endTime: String
)

data class Course(
    val name: String,
    val periodText: String,
    val dayOfWeek: String,
    val classroom: String,
    val weekText: String,
    val teacher: String = "",
    val credit: String = "",
    val weekRanges: List<IntRange> = emptyList(),
    val weekParityMode: WeekParityMode = WeekParityMode.ALL
)

data class CourseBlockInfo(
    val id: Long,
    val dayIndex: Int,
    val startPeriod: Int,
    val duration: Int,
    val name: String,
    val weekText: String,
    val teacher: String,
    val location: String,
    val backgroundColor: Color,
    val weekRanges: List<IntRange> = emptyList(),
    val weekParityMode: WeekParityMode = WeekParityMode.ALL
)

private data class WeekRule(
    val ranges: List<IntRange> = emptyList(),
    val parityMode: WeekParityMode = WeekParityMode.ALL
) {
    fun contains(currentWeek: Int): Boolean {
        val inRange = ranges.isEmpty() || ranges.any { weekRange -> currentWeek in weekRange }
        val parityMatched = when (parityMode) {
            WeekParityMode.ALL -> true
            WeekParityMode.ODD -> currentWeek % 2 == 1
            WeekParityMode.EVEN -> currentWeek % 2 == 0
        }
        return inRange && parityMatched
    }

    fun isEmpty(): Boolean {
        return ranges.isEmpty() && parityMode == WeekParityMode.ALL
    }
}

enum class WeekParityMode {
    ALL,
    ODD,
    EVEN
}

private fun parseWeekRuleFromText(weekText: String): WeekRule {
    val normalizedWeekText = weekText.normalizeForParsing()
    if (normalizedWeekText.isBlank()) {
        return WeekRule()
    }

    val ranges = Regex("""(\d{1,2})(?:\s*[-~到]\s*(\d{1,2}))?""")
        .findAll(normalizedWeekText)
        .mapNotNull { match ->
            val startWeek = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
            val endWeek = match.groupValues[2].toIntOrNull() ?: startWeek
            startWeek..endWeek.coerceAtLeast(startWeek)
        }
        .toList()

    val parityMode = when {
        normalizedWeekText.contains("单") -> WeekParityMode.ODD
        normalizedWeekText.contains("双") -> WeekParityMode.EVEN
        else -> WeekParityMode.ALL
    }

    return WeekRule(
        ranges = ranges,
        parityMode = parityMode
    )
}

private val ImportedCourseBlockColors = listOf(
    Color(0xFFE3F2FD),
    Color(0xFFE8F5E9),
    Color(0xFFFFF3E0),
    Color(0xFFF3E5F5),
    Color(0xFFFFEBEE),
    Color(0xFFE0F7FA),
    Color(0xFFFFF8E1),
    Color(0xFFFCE4EC)
)

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CourseSchedulePreview() {
    CourseScheduleAppTheme {
        CourseScheduleScreen()
    }
}
