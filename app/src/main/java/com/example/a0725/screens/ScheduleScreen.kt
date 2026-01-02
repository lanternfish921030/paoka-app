package com.example.a0725.screens




import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.a0725.R
import com.example.a0725.model.DayDetail
import com.example.a0725.model.ProjectDetail
import com.example.a0725.model.ScheduleEvent
import com.example.a0725.navigation.Routes
import com.example.a0725.navigation.navigateSingleTopTo
import com.example.a0725.notif.CalendarLinkStore
import com.example.a0725.notif.CalendarUtils
import com.example.a0725.notif.Notif
import com.example.a0725.notif.NotifyGate
import com.example.a0725.notif.cancelEventEveAlarm
import com.example.a0725.notif.scheduleEventEveAlarm
import com.example.a0725.repository.ScheduleRepo
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import androidx.compose.ui.window.Dialog
import com.example.a0725.notif.sendImmediateTestNotification




// ------------------------ 馬卡龍配色 (回歸您原本的選擇) ------------------------
val MacaronColors = listOf(
    Color(0xFFE9AFA3), // Peach (粉)
    Color(0xFFFFED97), // Lemon (黃)
    Color(0xFF93B7BE), // Mint (藍綠)
    Color(0xFFD9DBBC), // Green-beige (米綠)
    Color(0xFFFCDDBC)  // Cream (奶油)
)




// 字體顏色與背景
private val SchTextMain = Color(0xFF333333)
private val SchTextSecondary = Color(0xFF757575)
private val SchBackground = Color.White




// 資料結構
data class ScheduleProjectInput(
    var project: String = "",
    var group: String = "",
    var category: String = "",
    var checkInTime: String = "",
    var lane: String = "",
    var matchTime: String = ""
)




data class ScheduleDayInput(
    var date: String = "",
    var note: String = "",
    var startTime: String = "",
    var projects: SnapshotStateList<ScheduleProjectInput> = mutableStateListOf(ScheduleProjectInput())
)




private fun Color.toHexStr(): String {
    val r = (red * 255).toInt().coerceIn(0, 255)
    val g = (green * 255).toInt().coerceIn(0, 255)
    val b = (blue * 255).toInt().coerceIn(0, 255)
    return "#%02X%02X%02X".format(r, g, b)
}




private fun hexToColorSafe(hex: String): Color =
    try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { MacaronColors.first() }




private val ISO = DateTimeFormatter.ISO_LOCAL_DATE




private fun parseEventRange(event: ScheduleEvent): Pair<LocalDate?, LocalDate?> {
    val parts = event.dateRange.split("~").map { it.trim() }
    if (parts.size != 2) return null to null
    fun parsePart(part: String): LocalDate? {
        try { return LocalDate.parse(part, DateTimeFormatter.ISO_LOCAL_DATE) } catch (_: Exception) {}
        try { return LocalDate.parse(part, DateTimeFormatter.ofPattern("yyyy/MM/dd")) } catch (_: Exception) {}
        return try {
            val year = event.year.filter { it.isDigit() }.toIntOrNull() ?: return null
            val (mm, dd) = part.split("-").map { it.padStart(2, '0') }
            LocalDate.parse("$year-$mm-$dd", DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (_: Exception) { null }
    }
    val start = parsePart(parts[0])
    val end = parsePart(parts[1])
    return start to end
}




private fun sortKeyForUpcoming(event: ScheduleEvent): LocalDate {
    val (s, _) = parseEventRange(event)
    return s ?: LocalDate.MAX
}




@Composable
private fun LabeledValueCentered(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 12.sp, color = Color(0xFF888888))
        Text(if (value.isBlank()) "—" else value, fontSize = 14.sp, color = SchTextMain)
    }
}




@Composable
private fun ThreeColLineCentered(
    label1: String, value1: String,
    label2: String, value2: String,
    label3: String, value3: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f)) { LabeledValueCentered(label1, value1) }
        Box(Modifier.weight(1f)) { LabeledValueCentered(label2, value2) }
        Box(Modifier.weight(1f)) { LabeledValueCentered(label3, value3) }
    }
}




@Composable
private fun ProjectTwoLineCentered(p: ProjectDetail) {
    ThreeColLineCentered("項目", p.project, "組次", p.group, "賽別", p.category)
    Spacer(Modifier.height(6.dp))
    ThreeColLineCentered("檢錄", p.checkInTime, "道次", p.lane, "比賽", p.matchTime)
}




// ------------------------ 主畫面 ------------------------
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(nav: NavHostController) {
    val scope = rememberCoroutineScope()
    val profileVm: com.example.a0725.model.ProfileViewModel = viewModel()
    val photoUrl by profileVm.photoUrl.collectAsStateWithLifecycle()




    val edgePadding = 20.dp
    val events = remember { mutableStateListOf<ScheduleEvent>() }
    val showDialog = rememberSaveable { mutableStateOf(false) }
    val editEvent = remember { mutableStateOf<ScheduleEvent?>(null) }
    val expandedMap = remember { mutableStateMapOf<String, Boolean>() }




    val uid = Firebase.auth.currentUser?.uid
    val firestore = Firebase.firestore
    val context = LocalContext.current




    val notifPerm = Manifest.permission.POST_NOTIFICATIONS
    val requestNotifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val calendarPermissions = arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
    val requestCalendarPermissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }




    DisposableEffect(uid) {
        if (uid == null) return@DisposableEffect onDispose {}
        val reg = firestore.collection("users").document(uid).addSnapshotListener { snap, _ -> snap?.getString("photoUrl")?.let { profileVm.updatePhotoUrl(it) } }
        onDispose { reg.remove() }
    }




    DisposableEffect(Unit) {
        val reg: ListenerRegistration = ScheduleRepo.listenEvents(
            onChange = { list ->
                events.clear()
                events.addAll(list)
                val today = LocalDate.now()
                list.forEach { ev ->
                    val (_, end) = parseEventRange(ev)
                    if (end != null && today.isAfter(end)) scope.launch { ScheduleRepo.deleteEvent(ev.id) }
                }
            },
            onError = { }
        )
        onDispose { reg.remove() }
    }




    fun deleteEvent(ev: ScheduleEvent) {
        cancelEventEveAlarm(context, ev)
        val linked = CalendarLinkStore.get(context, ev.id)
        if (linked != null) { CalendarUtils.deleteById(context, linked); CalendarLinkStore.remove(context, ev.id) }
        scope.launch { ScheduleRepo.deleteEvent(ev.id) }
    }
    fun openEditDialog(e: ScheduleEvent) { editEvent.value = e; showDialog.value = true }




    fun addNewEvent(
        name: String,
        startDate: String,
        endDate: String,
        colorHex: String,
        dayInputs: List<ScheduleDayInput>
    ) {
        val days = dayInputs.map { d ->
            DayDetail(
                date = d.date,
                note = "",
                startTime = d.startTime,
                projects = d.projects.map { p ->
                    ProjectDetail(
                        p.project,
                        p.group,
                        p.category,
                        p.checkInTime,
                        p.lane,
                        p.matchTime
                    )
                }
            )
        }


        // 先組一個沒有 id 的 event
        val base = ScheduleEvent(
            id = "",
            year = "2025年",
            name = name,
            dateRange = "$startDate ~ $endDate",
            colorHex = colorHex,
            days = days
        )


        scope.launch {
            // 1. 寫進 Firestore，拿到真正的 id
            val newId = ScheduleRepo.addEvent(base)
            val e = base.copy(id = newId)


            // 2. 重新排「前一天提醒」的鬧鐘（用真正的 id）
            try {
                cancelEventEveAlarm(context, e)
            } catch (_: SecurityException) {
            }
            if (NotifyGate.isEnabled(context)) {
                try {
                    scheduleEventEveAlarm(context, e)
                } catch (_: SecurityException) {
                }
            }


            // 3. 同步到 Google Calendar
            if (CalendarUtils.hasCalendarPermission(context)) {
                val calId = CalendarUtils.findWritableCalendarId(context)
                val s = CalendarUtils.parseIsoDate(startDate)
                val ed = CalendarUtils.parseIsoDate(endDate)


                if (calId != null && s != null && ed != null) {
                    val res = CalendarUtils.insertAllDayEvent(
                        context = context,
                        calendarId = calId,
                        title = e.name,
                        startDate = s,
                        endDateInclusive = ed,
                        description = "賽事：${e.name}",
                        location = null
                    )
                    if (res.success && res.eventId != null) {
                        CalendarLinkStore.save(context, newId, res.eventId)
                    }
                }
            } else {
                // 沒有日曆權限 → 觸發 request，等使用者同意後下次再建立
                requestCalendarPermissions.launch(calendarPermissions)
            }
        }
    }








    fun updateExistingEvent(
        originId: String,
        name: String,
        startDate: String,
        endDate: String,
        colorHex: String,
        dayInputs: List<ScheduleDayInput>
    ) {
        val days = dayInputs.map { d ->
            DayDetail(
                date = d.date,
                note = "",
                startTime = d.startTime,
                projects = d.projects.map { p ->
                    ProjectDetail(
                        p.project,
                        p.group,
                        p.category,
                        p.checkInTime,
                        p.lane,
                        p.matchTime
                    )
                }
            )
        }


        val e = ScheduleEvent(
            id = originId,
            year = "2025年",
            name = name,
            dateRange = "$startDate ~ $endDate",
            colorHex = colorHex,
            days = days
        )


        scope.launch {
            // 1. 更新 Firestore
            ScheduleRepo.updateEvent(e)


            // 2. 重新排「前一天提醒」鬧鐘
            try {
                cancelEventEveAlarm(context, e)
            } catch (_: SecurityException) {
            }
            if (NotifyGate.isEnabled(context)) {
                try {
                    scheduleEventEveAlarm(context, e)
                } catch (_: SecurityException) {
                }
            }


            // 3. 更新 Google Calendar 事件
            if (CalendarUtils.hasCalendarPermission(context)) {
                val s = CalendarUtils.parseIsoDate(startDate)
                val ed = CalendarUtils.parseIsoDate(endDate)


                if (s != null && ed != null) {
                    val linkedId = CalendarLinkStore.get(context, originId)


                    if (linkedId != null) {
                        // 已經有連結 → 試著更新原本那顆事件
                        val ok = CalendarUtils.updateAllDayEvent(
                            context = context,
                            eventId = linkedId,
                            title = e.name,
                            startDate = s,
                            endDateInclusive = ed,
                            description = "賽事：${e.name}",
                            location = null
                        )


                        // 如果更新失敗（被使用者刪掉之類的）就重新插入
                        if (!ok) {
                            val calId = CalendarUtils.findWritableCalendarId(context)
                            if (calId != null) {
                                val res = CalendarUtils.insertAllDayEvent(
                                    context = context,
                                    calendarId = calId,
                                    title = e.name,
                                    startDate = s,
                                    endDateInclusive = ed,
                                    description = "賽事：${e.name}",
                                    location = null
                                )
                                if (res.success && res.eventId != null) {
                                    CalendarLinkStore.save(context, originId, res.eventId)
                                }
                            }
                        }
                    } else {
                        // 舊資料沒有 link → 當作新插入
                        val calId = CalendarUtils.findWritableCalendarId(context)
                        if (calId != null) {
                            val res = CalendarUtils.insertAllDayEvent(
                                context = context,
                                calendarId = calId,
                                title = e.name,
                                startDate = s,
                                endDateInclusive = ed,
                                description = "賽事：${e.name}",
                                location = null
                            )
                            if (res.success && res.eventId != null) {
                                CalendarLinkStore.save(context, originId, res.eventId)
                            }
                        }
                    }
                }
            } else {
                // 沒權限就先請求
                requestCalendarPermissions.launch(calendarPermissions)
            }
        }
    }








    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("工具", fontSize = 25.sp, color = SchTextMain) },
                navigationIcon = {
                    val context = LocalContext.current
                    IconButton(
                        onClick = { nav.navigateSingleTopTo(Routes.PERSONAL) },
                        modifier = Modifier.padding(start = edgePadding)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(photoUrl ?: "").crossfade(true).build(),
                            contentDescription = "User Avatar",
                            placeholder = painterResource(R.drawable.user),
                            error = painterResource(R.drawable.user),
                            modifier = Modifier.size(48.dp).clip(CircleShape).border(1.dp, Color(0xFFE0E0E0), CircleShape)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = SchBackground)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog.value = true },
                containerColor = Color(0xFFFFD54F),
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) { Icon(Icons.Filled.Add, null, modifier = Modifier.size(28.dp)) }
        },
        containerColor = SchBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {




            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Text(
                    text = "我的賽程",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SchTextMain
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "管理與追蹤您的比賽計畫",
                    fontSize = 14.sp,
                    color = SchTextSecondary
                )
            }




            val unique by remember { derivedStateOf { events.distinctBy { it.id } } }
            val today = LocalDate.now()
            val ongoing by remember { derivedStateOf { unique.filter { ev -> val (s, e) = parseEventRange(ev); s != null && e != null && !today.isBefore(s) && !today.isAfter(e) }.sortedBy { parseEventRange(it).first ?: LocalDate.MAX } } }
            val upcoming by remember { derivedStateOf { unique.filter { ev -> val (s, _) = parseEventRange(ev); s != null && today.isBefore(s) }.sortedBy { sortKeyForUpcoming(it) } } }




            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                if (ongoing.isNotEmpty()) {
                    item { ScheduleSectionHeader("進行中") }
                    itemsIndexed(ongoing, key = { _, e -> e.id }) { _, event ->
                        val isExpanded = expandedMap[event.id] == true
                        ScheduleEventItem(event, isExpanded, { expandedMap[event.id] = !isExpanded }, { deleteEvent(event) }, { openEditDialog(event) })
                    }
                }




                if (upcoming.isNotEmpty()) {
                    item { ScheduleSectionHeader("即將到來") }
                    itemsIndexed(upcoming, key = { _, e -> e.id }) { _, event ->
                        val isExpanded = expandedMap[event.id] == true
                        ScheduleEventItem(event, isExpanded, { expandedMap[event.id] = !isExpanded }, { deleteEvent(event) }, { openEditDialog(event) })
                    }
                }




                if (ongoing.isEmpty() && upcoming.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                            Text("目前沒有賽程，點擊 + 新增", color = Color.LightGray)
                        }
                    }
                }
            }
        }
    }




    if (showDialog.value) {
        AddEventDialog(
            editing = editEvent.value,
            onDismiss = { showDialog.value = false; editEvent.value = null },
            onSubmit = { editingId, name, start, end, color, dayInputs ->
                if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, notifPerm) != PackageManager.PERMISSION_GRANTED) requestNotifPermission.launch(notifPerm)
                if (editingId == null) addNewEvent(name, start, end, color, dayInputs) else updateExistingEvent(editingId, name, start, end, color, dayInputs)
            }
        )
    }
}




// ------------------------ UI 元件 ------------------------




@Composable
fun ScheduleSectionHeader(title: String) {
    Row(
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFFFFD54F))
        )
        Spacer(Modifier.width(8.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SchTextSecondary)
    }
}




// ★ 全彩卡片設計 (胖胖版)
@Composable
private fun ScheduleEventItem(
    event: ScheduleEvent,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    // 取得該卡片的馬卡龍色
    val cardColor = remember(event.colorHex) { hexToColorSafe(event.colorHex) }


    // 卡片本體
    Card(
        onClick = onToggle,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor, // 上半部維持馬卡龍填滿色
            contentColor = SchTextMain
        ),
        // 去除陰影
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {


            // 1. 上半部：標題與時間 (填滿色)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 90.dp)
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = event.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = SchTextMain,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )


                Spacer(Modifier.width(8.dp))


                Text(
                    text = event.dateRange,
                    fontSize = 14.sp,
                    color = SchTextMain.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            }


            // 2. 展開後的詳細內容 (白色底 + 同色細邊框)
            if (isExpanded) {
                Surface(
                    color = Color.White,
                    contentColor = SchTextMain,
                    // 底部圓角跟卡片一致
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                    // ★★★ 修改重點：加上同色系的細邊框 ★★★
                    border = BorderStroke(0.1.dp, cardColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        // 稍微往內縮一點點 padding，避免邊框跟卡片邊緣重疊時被裁切，如果不喜歡可以拿掉
                        .padding(horizontal = 1.dp, vertical = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        if (event.days.isEmpty()) {
                            Text("尚未新增項目", color = Color.Gray, fontSize = 14.sp)
                        } else {
                            event.days.forEachIndexed { i, day ->
                                // 每日區塊
                                Surface(
                                    color = Color(0xFFFAFAFA),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFFF0F0F0)),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                ) {
                                    Column(Modifier.padding(14.dp)) {
                                        // Day 標題
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            // 小圓點跟隨卡片主色
                                            Surface(color = cardColor, shape = CircleShape, modifier = Modifier.size(10.dp)) {}
                                            Spacer(Modifier.width(8.dp))
                                            Text("Day ${i + 1}  ${day.date}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SchTextMain)
                                        }


                                        Spacer(Modifier.height(10.dp))


                                        if (day.projects.isEmpty()) {
                                            Text("（無項目）", color = Color.Gray, fontSize = 14.sp)
                                        } else {
                                            day.projects.forEachIndexed { idx, p ->
                                                if (idx > 0) Divider(color = Color(0xFFEEEEEE), modifier = Modifier.padding(vertical = 8.dp))
                                                ProjectTwoLineCentered(p)
                                            }
                                        }
                                    }
                                }
                            }
                        }


                        // 操作按鈕
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onEdit) {
                                Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("修改", color = SchTextSecondary, fontSize = 14.sp)
                            }
                            TextButton(onClick = onDelete) {
                                Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(18.dp), tint = Color(0xFFE57373))
                                Spacer(Modifier.width(4.dp))
                                Text("刪除", color = Color(0xFFE57373), fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}






// ------------------------ 新增賽程對話框 (自定義 Dialog 版) ------------------------
@Composable
fun AddEventDialog(
    editing: ScheduleEvent? = null,
    onDismiss: () -> Unit,
    onSubmit: (String?, String, String, String, String, List<ScheduleDayInput>) -> Unit
) {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    // 限制高度，避免鍵盤跳出時撐爆畫面
    val dialogMaxHeight = (config.screenHeightDp * 0.85f).dp


    var name by remember(editing) { mutableStateOf(editing?.name.orEmpty()) }
    var selectedColor by remember(editing) { mutableStateOf(editing?.colorHex?.let(::hexToColorSafe) ?: MacaronColors.first()) }
    val days = remember(editing) {
        if (editing != null && editing.days.isNotEmpty()) {
            mutableStateListOf(*editing.days.map { d -> ScheduleDayInput(date = d.date, startTime = d.startTime, projects = mutableStateListOf(*d.projects.map { p -> ScheduleProjectInput(p.project, p.group, p.category, p.checkInTime, p.lane, p.matchTime) }.toTypedArray())) }.toTypedArray())
        } else {
            mutableStateListOf(ScheduleDayInput(projects = mutableStateListOf(ScheduleProjectInput())))
        }
    }


    fun setStartDateAndShift(iso: String) {
        days[0] = days[0].copy(date = iso)
        if (days.size > 1) {
            val start = LocalDate.parse(iso, ISO)
            for (i in 1 until days.size) days[i] = days[i].copy(date = start.plusDays(i.toLong()).format(ISO))
        }
    }


    fun pickStartDateForDay1() {
        val cal = Calendar.getInstance()
        DatePickerDialog(context, { _, y, m, d -> setStartDateAndShift("%04d-%02d-%02d".format(y, m + 1, d)) }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }


    // ★★★ 改用 Dialog + Surface，完全掌控邊距 ★★★
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = dialogMaxHeight)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                // ★★★ 關鍵：這裡手動控制邊距 ★★★
                // 上、左、右給 24dp (標準大方)，底部只給 12dp (收緊留白)
                contentPadding = PaddingValues(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 12.dp)
            ) {
                // 標題
                item {
                    Text(
                        if (editing == null) "新增賽程" else "編輯賽程",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SchTextMain
                    )
                }


                // 顏色選擇
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MacaronColors.forEach { c ->
                            Box(
                                Modifier.size(32.dp).clip(CircleShape).background(c)
                                    .border(2.dp, if (c == selectedColor) SchTextMain else Color.Transparent, CircleShape)
                                    .clickable { selectedColor = c }
                            )
                        }
                    }
                }


                // 名稱輸入
                item { ScheduleCompactField(value = name, onValueChange = { name = it }, label = "賽事名稱") }


                // 日期選擇
                item {
                    Button(
                        onClick = { pickStartDateForDay1() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5F5F5), contentColor = SchTextMain),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Text(if (days.firstOrNull()?.date.isNullOrBlank()) "選擇起始日期" else "起始日期：${days.first().date}")
                    }
                }


                item { Divider(color = Color(0xFFEEEEEE)) }


                // 天數列表
                itemsIndexed(days) { idx, day ->
                    Surface(
                        color = Color(0xFFFAFAFA),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Day ${idx + 1}  ${day.date}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (days.size > 1) {
                                    Icon(Icons.Outlined.Delete, null, tint = Color.Gray, modifier = Modifier.clickable { days.removeAt(idx) })
                                }
                            }
                            day.projects.forEachIndexed { pIdx, p ->
                                if (pIdx > 0) Divider(color = Color(0xFFE0E0E0))
                                ScheduleProjectInputView(p, pIdx > 0, { days[idx].projects[pIdx] = it }, { days[idx].projects.removeAt(pIdx) })
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { days[idx].projects.add(ScheduleProjectInput()) }) { Text("+ 項目", color = Color(0xFFFFA000)) }
                            }
                        }
                    }
                    if(idx < days.lastIndex) Spacer(Modifier.height(8.dp))
                }


                // 新增天數按鈕
                item {
                    OutlinedButton(
                        onClick = {
                            val d0 = days.firstOrNull()?.date
                            if (d0.isNullOrBlank()) pickStartDateForDay1()
                            else days.add(ScheduleDayInput(date = LocalDate.parse(d0, ISO).plusDays(days.size.toLong()).format(ISO), projects = mutableStateListOf(ScheduleProjectInput())))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("+ 新增天數", color = Color(0xFFFFA000)) }
                }


                // 底部按鈕區 (取消 / 完成)
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("取消", color = SchTextSecondary) }
                        Button(
                            onClick = {
                                val start = days.firstOrNull()?.date.orEmpty()
                                val end = days.lastOrNull()?.date ?: start


                                if (name.isNotBlank() && start.isNotBlank()) {
                                    // 1. 呼叫外面的 onSubmit（裡面會做 新增/修改 + 排鬧鐘 + 日曆）
                                    onSubmit(
                                        editing?.id,
                                        name,
                                        start,
                                        end,
                                        selectedColor.toHexStr(),
                                        days.toList()
                                    )


                                    // 2. 立刻送一顆通知（區分新增 / 修改）
                                    val title = if (editing == null) "已新增賽程" else "已更新賽程"
                                    val text = if (name.isBlank()) "賽程已儲存" else name


                                    sendImmediateTestNotification(
                                        ctx = context,
                                        title = title,
                                        text = text
                                    )


                                    // 3. 關閉 Dialog
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "請填寫完整", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFD54F),
                                contentColor = SchTextMain
                            )
                        ) {
                            Text("完成")
                        }


                    }
                }


            }
        }
    }
}




// ------------------------ 輸入元件 ------------------------
@Composable
fun ScheduleCompactField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF5F5F5))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) Text(placeholder ?: label, color = Color(0xFFBDBDBD), fontSize = 14.sp)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = androidx.compose.ui.text.TextStyle(color = SchTextMain, fontSize = 14.sp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}




@Composable
fun ScheduleProjectInputView(
    project: ScheduleProjectInput,
    showRemove: Boolean,
    onChange: (ScheduleProjectInput) -> Unit,
    onRemove: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ScheduleCompactField(project.project, { onChange(project.copy(project = it)) }, "項目", Modifier.weight(1f))
            ScheduleCompactField(project.group, { onChange(project.copy(group = it)) }, "組次", Modifier.weight(1f))
            ScheduleCompactField(project.category, { onChange(project.copy(category = it)) }, "賽別", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ScheduleCompactField(project.checkInTime, { onChange(project.copy(checkInTime = it)) }, "檢錄", Modifier.weight(1f), "00:00")
            ScheduleCompactField(project.lane, { onChange(project.copy(lane = it)) }, "道次", Modifier.weight(1f))
            ScheduleCompactField(project.matchTime, { onChange(project.copy(matchTime = it)) }, "比賽", Modifier.weight(1f), "00:00")
        }
        if (showRemove) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TextButton(onClick = onRemove) { Text("刪除項目", color = Color(0xFFE57373), fontSize = 12.sp) }
            }
        }
    }
}









