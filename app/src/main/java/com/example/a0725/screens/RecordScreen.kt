@file:OptIn(ExperimentalMaterial3Api::class)




package com.example.a0725.screens




import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.a0725.R
import com.example.a0725.model.PbItem
import com.example.a0725.model.RecordViewModel
import com.example.a0725.model.ScoreItem
import com.example.a0725.navigation.Routes
import com.example.a0725.navigation.navigateSingleTopTo
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Calendar // ★ 補上這個 Import 解決紅字
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.sqrt
import androidx.compose.ui.graphics.drawscope.translate


/* -------------------------- 共用色票 & Firestore 常數 -------------------------- */




private val ScreenBg      = Color.White
private val CardBg        = Color.White
private val RowGrayBg     = Color(0xFFF7F7F9)
private val LightGrayBg   = Color(0xFFF5F5F7)
private val AccentOrange  = Color(0xFFFFA800)
private val AccentPillBg  = Color(0xFFFFF3C8)
private val DividerGray   = Color(0xFFE0E0E0)




// 進步=黃色(主題色)，退步=灰色
private val TrendGood     = Color(0xFFFFB300)
private val TrendBad      = Color(0xFFBDBDBD)
private val TrendNeutral  = Color(0xFFE0E0E0)




val TextMain      = Color(0xFF444444)
val TextSecondary = Color(0xFF9E9E9E)




private const val FIRESTORE_SCORES_ROOT = "scores"
private const val FIRESTORE_HISTORY_SUB = "history"




/* -------------------------- Screen -------------------------- */




@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun RecordScreen(nav: NavHostController) {
    val vm: RecordViewModel = viewModel()
    val profileVm: com.example.a0725.model.ProfileViewModel = viewModel()
    val photoUrl by profileVm.photoUrl.collectAsStateWithLifecycle()




    val edgePadding = 20.dp




    val ui by vm.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val uid = Firebase.auth.currentUser?.uid
    val firestore = Firebase.firestore




    var selectedScoreItem by remember { mutableStateOf<ScoreItem?>(null) }




    DisposableEffect(uid) {
        if (uid == null) return@DisposableEffect onDispose {}
        val reg = firestore.collection("users").document(uid)
            .addSnapshotListener { snap, _ ->
                snap?.getString("photoUrl")?.let { profileVm.updatePhotoUrl(it) }
            }
        onDispose { reg.remove() }
    }




    LaunchedEffect(Unit) {
        vm.attachListenersOnce()
        vm.ensureFreshOnEnter()
    }
    LaunchedEffect(ui.message) {
        ui.message?.let { snackbarHostState.showSnackbar(it); vm.consumeMessage() }
    }




    val screenH = LocalConfiguration.current.screenHeightDp.dp
    val halfCardHeight = screenH * 0.48f




    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("工具", fontSize = 25.sp, color = TextMain) },
                navigationIcon = {
                    val context = LocalContext.current
                    IconButton(
                        onClick = { nav.navigateSingleTopTo(Routes.PERSONAL) },
                        modifier = Modifier.padding(start = edgePadding)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(photoUrl ?: "")
                                .crossfade(true)
                                .build(),
                            contentDescription = "avatar",
                            placeholder = rememberAsyncImagePainter(R.drawable.user),
                            error = rememberAsyncImagePainter(R.drawable.user),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color(0xFFE3E5E8), CircleShape)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = ScreenBg
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("成績紀錄", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextMain)
                Spacer(Modifier.height(6.dp))
            }




            item {
                // ★ 修改：換成新的標題元件
                RecordSectionHeader("趨勢圖表")
                // ★ 修改：高度加大到 480dp
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = CardBg), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                    Box(Modifier.fillMaxWidth().height(480.dp)) {
                        PerformanceTrendCard(ui.historyIndividual, ui.historyTeam)
                    }
                }
            }




            item {
                RecordSectionHeader("最佳成績（PB）")
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 260.dp).height(halfCardHeight)
                ) {
                    PBCard(
                        pbsIndividual = ui.pbsIndividual,
                        pbsTeam = ui.pbsTeam,
                        onSync = { vm.manualRefresh() },
                        onItemClick = { pb ->
                            selectedScoreItem = ScoreItem(
                                id = pb.id,
                                event = pb.event,
                                performance = pb.best_text,
                                perf_seconds = pb.best_sec,
                                date = pb.date,
                                meet = pb.meet,
                                note = pb.note,
                                team = pb.team
                            )
                        },
                        fillMax = true
                    )
                }
            }




            item {
                RecordSectionHeader("歷年成績")
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 260.dp).height(450.dp)
                ) {
                    HistoryCardContent(
                        historyIndividual = ui.historyIndividual,
                        historyTeam = ui.historyTeam,
                        onRefresh = { vm.manualRefresh() },
                        onItemClick = { score -> selectedScoreItem = score }
                    )
                }
            }
        }




        if (ui.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }




    if (selectedScoreItem != null) {
        ScoreDetailDialog(
            item = selectedScoreItem!!,
            onDismiss = { selectedScoreItem = null },
            onSaveNote = { id, note -> vm.updateScoreNote(id, note) }
        )
    }
}




@Composable
fun ScoreDetailDialog(item: ScoreItem, onDismiss: () -> Unit, onSaveNote: (String, String) -> Unit) {
    var editingNote by remember { mutableStateOf(item.note) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = item.event, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextMain)
                        Text(text = item.date.ifBlank { "未知日期" }, fontSize = 14.sp, color = TextSecondary)
                    }
                    Text(text = item.performance, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AccentOrange, fontFamily = FontFamily.Monospace)
                }
                Spacer(Modifier.height(12.dp))
                Text(text = "賽事", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                Text(text = item.meet.ifBlank { "（無賽事名稱）" }, fontSize = 16.sp, color = TextMain, modifier = Modifier.padding(top = 2.dp))
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Edit, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(text = "賽後記錄 / 心得", fontSize = 14.sp, color = AccentOrange, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = editingNote, onValueChange = { editingNote = it }, modifier = Modifier.fillMaxWidth().height(120.dp),
                    placeholder = { Text("寫下這場比賽的狀況、檢討或是心情...", color = Color.LightGray, fontSize = 14.sp) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentOrange, unfocusedBorderColor = LightGrayBg, focusedContainerColor = LightGrayBg.copy(alpha = 0.5f), unfocusedContainerColor = LightGrayBg.copy(alpha = 0.5f)),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, color = TextMain)
                )
                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("關閉", color = TextSecondary) }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { if (editingNote != item.note) { onSaveNote(item.id, editingNote) }; onDismiss() }) { Text("儲存", color = AccentOrange, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}




private fun getEventSortIndex(event: String): Int {
    val digits = event.filter { it.isDigit() }
    return if (digits.isNotEmpty()) digits.toInt() else Int.MAX_VALUE
}




@Composable
private fun PBCard(pbsIndividual: List<PbItem>, pbsTeam: List<PbItem>, onSync: () -> Unit, onItemClick: (PbItem) -> Unit, fillMax: Boolean = false) {
    var tab by remember { mutableStateOf(0) }
    Column((if (fillMax) Modifier.fillMaxSize() else Modifier.fillMaxWidth()).padding(top = 12.dp, bottom = 10.dp)) {
        TabsHeader(left = "個人項目", right = "團體項目", selectedIndex = tab, onSelect = { tab = it })
        TableHeader(headers = listOf("項目", "成績", "賽事"))




        val rawData = if (tab == 0) pbsIndividual else pbsTeam
        val data = remember(rawData) {
            rawData.sortedBy { getEventSortIndex(it.event) }
        }




        LazyColumn(modifier = Modifier.weight(1f, fill = true).padding(horizontal = 12.dp, vertical = 6.dp)) {
            if (data.isEmpty()) { item { Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) { Text("還沒有資料，點「同步」鍵試試！", color = TextSecondary) } } }
            else {
                items(data.size) { i ->
                    val pb = data[i]
                    TableRow(col1 = pb.event, col2 = pb.best_text, col3 = pb.meet, hint = pb.date, onClick = { onItemClick(pb) })
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}




@Composable
private fun HistoryCardContent(historyIndividual: List<ScoreItem>, historyTeam: List<ScoreItem>, onRefresh: () -> Unit, onItemClick: (ScoreItem) -> Unit) {
    var tab by remember { mutableStateOf(0) }
    var editMode by remember { mutableStateOf(false) }
    val selectedMap = remember { mutableStateMapOf<String, Boolean>() }
    var showAddDialog by remember { mutableStateOf(false) }
    var newEvent by remember { mutableStateOf("") }
    var newTime by remember { mutableStateOf("") }
    var newMeet by remember { mutableStateOf("") }
    var newDate by remember { mutableStateOf("") }
    var useCustomEvent by remember { mutableStateOf(false) }
    val uid = Firebase.auth.currentUser?.uid
    val db = Firebase.firestore
    val context = LocalContext.current




    val data = if (tab == 0) historyIndividual else historyTeam
    val allEvents = remember(historyIndividual, historyTeam) { (historyIndividual + historyTeam).map { it.event }.filter { it.isNotBlank() }.distinct().sortedBy { getEventSortIndex(it) } }
    val grouped = remember(data) { data.groupBy { it.event }.toSortedMap(compareBy<String> { getEventSortIndex(it) }.thenBy { it }) }




    LaunchedEffect(tab, historyIndividual, historyTeam) { selectedMap.clear(); editMode = false }




    Column(Modifier.fillMaxSize().padding(top = 12.dp, bottom = 4.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            TabsHeader(left = "個人項目", right = "團體項目", selectedIndex = tab, onSelect = { tab = it }, modifier = Modifier.weight(1f))
            if (!editMode) {
                Text(text = "修改", color = AccentOrange, fontSize = 14.sp, modifier = Modifier.clickable(enabled = data.isNotEmpty()) { if (data.isNotEmpty()) editMode = true }.padding(start = 4.dp))
            } else {
                Text(text = "取消", color = TextSecondary, fontSize = 14.sp, modifier = Modifier.clickable { selectedMap.clear(); editMode = false }.padding(end = 4.dp))
                Spacer(Modifier.width(4.dp))
                FilterPill(text = "全選") { data.forEach { selectedMap[it.id] = true } }
                Spacer(Modifier.width(4.dp))
                FilterPill(text = "刪除", background = Color(0xFFFFE0E0), textColor = Color(0xFFD32F2F)) {
                    if (uid != null) {
                        val colRef = db.collection(FIRESTORE_SCORES_ROOT).document(uid).collection(FIRESTORE_HISTORY_SUB)
                        val toDelete = data.filter { selectedMap[it.id] == true }
                        toDelete.forEach { s -> if (s.id.isNotBlank()) colRef.document(s.id).delete() }
                    }
                    selectedMap.clear(); editMode = false
                }
            }
        }




        Box(Modifier.fillMaxSize()) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp).padding(bottom = 48.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (data.isEmpty()) { item { Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) { Text("還沒有資料，先同步或新增一筆吧！", color = TextSecondary) } } }
                else {
                    grouped.forEach { (event, rows) ->
                        stickyHeader { Surface(color = CardBg) { Text(text = event, fontWeight = FontWeight.SemiBold, color = TextSecondary, modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp, start = 4.dp)) } }
                        items(rows, key = { r -> r.id }) { r ->
                            HistoryRowEditable(
                                score = r,
                                showCheckbox = editMode,
                                checked = selectedMap[r.id] == true,
                                onCheckedChange = { c -> selectedMap[r.id] = c },
                                onClick = { if (!editMode) onItemClick(r) }
                            )
                        }
                    }
                }
            }
            if (editMode && uid != null) {
                FloatingActionButton(onClick = {
                    val defaultEvent = data.firstOrNull()?.event ?: allEvents.firstOrNull() ?: ""
                    newEvent = defaultEvent; useCustomEvent = defaultEvent.isBlank(); newTime = ""; newMeet = ""; newDate = ""; showAddDialog = true
                }, containerColor = AccentOrange, contentColor = Color.White, shape = CircleShape, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)) { Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold) }
            }
            AssistChip(onClick = onRefresh, label = { Text("同步") }, leadingIcon = { Icon(Icons.Outlined.Refresh, null, Modifier.size(16.dp)) }, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 4.dp))
        }
    }




    if (showAddDialog && uid != null) {
        val displayFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
        val storeFormatter = remember { DateTimeFormatter.ofPattern("yyyyMMdd") }
        var displayDate by remember { mutableStateOf("") }
        val calendar = remember { Calendar.getInstance() }
        val datePickerDialog = remember {
            android.app.DatePickerDialog(
                context,
                { _, y, m, d ->
                    val ld = LocalDate.of(y, m + 1, d)
                    displayDate = ld.format(displayFormatter)
                    newDate = ld.format(storeFormatter)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
        }




        Dialog(onDismissRequest = { showAddDialog = false }) {
            Surface(shape = RoundedCornerShape(26.dp), color = Color.White, tonalElevation = 6.dp) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp).widthIn(min = 260.dp, max = 360.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("新增成績", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = TextMain)
                    var eventDropdownExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = eventDropdownExpanded, onExpandedChange = { eventDropdownExpanded = !eventDropdownExpanded }) {
                        OutlinedTextField(value = if (!useCustomEvent && newEvent.isNotBlank()) newEvent else "自訂項目", onValueChange = {}, readOnly = true, label = { Text("項目", fontSize = 12.sp) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = eventDropdownExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LightGrayBg, unfocusedBorderColor = LightGrayBg, focusedContainerColor = LightGrayBg, unfocusedContainerColor = LightGrayBg, cursorColor = AccentOrange, focusedTextColor = TextMain, unfocusedTextColor = TextMain))
                        MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(surface = LightGrayBg, surfaceVariant = LightGrayBg)) {
                            ExposedDropdownMenu(expanded = eventDropdownExpanded, onDismissRequest = { eventDropdownExpanded = false }) {
                                allEvents.forEach { ev -> DropdownMenuItem(text = { Text(ev, color = TextMain) }, onClick = { newEvent = ev; useCustomEvent = false; eventDropdownExpanded = false }, modifier = Modifier.background(LightGrayBg), colors = MenuDefaults.itemColors(textColor = TextMain)) }
                                DropdownMenuItem(text = { Text("＋ 新增新項目", color = AccentOrange) }, onClick = { useCustomEvent = true; newEvent = ""; eventDropdownExpanded = false }, modifier = Modifier.background(LightGrayBg), colors = MenuDefaults.itemColors(textColor = AccentOrange))
                            }
                        }
                    }
                    if (useCustomEvent || newEvent.isBlank()) {
                        OutlinedTextField(value = newEvent, onValueChange = { newEvent = it }, singleLine = true, modifier = Modifier.fillMaxWidth(), placeholder = { Text("請輸入項目名稱（例如 1500公尺）", color = TextSecondary.copy(alpha = 0.7f)) }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LightGrayBg, unfocusedBorderColor = LightGrayBg, focusedContainerColor = LightGrayBg, unfocusedContainerColor = LightGrayBg, cursorColor = AccentOrange, focusedTextColor = TextMain, unfocusedTextColor = TextMain))
                    }
                    OutlinedTextField(value = newTime, onValueChange = { newTime = it }, singleLine = true, modifier = Modifier.fillMaxWidth(), placeholder = { Text("秒數（4:33.9 或 273.9）", color = TextSecondary.copy(alpha = 0.7f)) }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LightGrayBg, unfocusedBorderColor = LightGrayBg, focusedContainerColor = LightGrayBg, unfocusedContainerColor = LightGrayBg, cursorColor = AccentOrange, focusedTextColor = TextMain, unfocusedTextColor = TextMain))
                    OutlinedTextField(value = newMeet, onValueChange = { newMeet = it }, singleLine = true, modifier = Modifier.fillMaxWidth(), placeholder = { Text("賽事 / 練習說明", color = TextSecondary.copy(alpha = 0.7f)) }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LightGrayBg, unfocusedBorderColor = LightGrayBg, focusedContainerColor = LightGrayBg, unfocusedContainerColor = LightGrayBg, cursorColor = AccentOrange, focusedTextColor = TextMain, unfocusedTextColor = TextMain))
                    Box {
                        OutlinedTextField(value = displayDate, onValueChange = {}, singleLine = true, enabled = false, modifier = Modifier.fillMaxWidth(), placeholder = { Text("日期（點擊選擇）", color = TextSecondary.copy(alpha = 0.7f)) }, colors = OutlinedTextFieldDefaults.colors(disabledBorderColor = LightGrayBg, disabledContainerColor = LightGrayBg, disabledTextColor = TextMain))
                        Box(modifier = Modifier.matchParentSize().clickable { datePickerDialog.show() })
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddDialog = false }) { Text("取消", color = TextSecondary) }
                        Spacer(Modifier.width(4.dp))
                        TextButton(onClick = {
                            val event = newEvent.trim(); val time = newTime.trim(); val meet = newMeet.trim(); val date = newDate.trim()
                            if (event.isBlank() || time.isBlank()) { Toast.makeText(context, "項目與秒數不可空白", Toast.LENGTH_SHORT).show() }
                            else if (uid != null) {
                                val colRef = db.collection(FIRESTORE_SCORES_ROOT).document(uid).collection(FIRESTORE_HISTORY_SUB)
                                val t = time.trim().lowercase(); var perfSec = 0.0
                                if (t.isNotBlank() && t !in setOf("dq", "dns", "dnf", "—", "-")) {
                                    val cleaned = t.removeSuffix("s").removeSuffix("sec").trim()
                                    perfSec = try { if (":" in cleaned) { val p = cleaned.split(":"); (p.dropLast(1).joinToString(":").toDoubleOrNull()?:p[0].toDouble()) * 60 + p.last().toDouble() } else cleaned.toDouble() } catch(_:Throwable){0.0}
                                }
                                val dataMap = hashMapOf("event" to event, "performance" to time, "perf_seconds" to perfSec, "meet" to meet, "date" to date, "team" to (tab == 1), "time_dt" to date, "note" to "")
                                colRef.add(dataMap).addOnSuccessListener { Toast.makeText(context, "已新增", Toast.LENGTH_SHORT).show() }.addOnFailureListener { e -> Toast.makeText(context, "新增失敗：${e.message}", Toast.LENGTH_SHORT).show() }
                            }
                            showAddDialog = false
                        }) { Text("儲存", color = AccentOrange) }
                    }
                }
            }
        }
    }
}




@Composable
private fun TabsHeader(left: String, right: String, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        @Composable fun RowScope.Tab(text: String, selected: Boolean, onClick: () -> Unit) {
            Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                TextButton(onClick = onClick, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(text, fontSize = 16.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = if (selected) AccentOrange else TextSecondary)
                }
                Box(Modifier.height(3.dp).fillMaxWidth(0.6f).align(Alignment.CenterHorizontally).background(if (selected) AccentOrange else Color.Transparent, RoundedCornerShape(2.dp)))
            }
        }
        Tab(left, selectedIndex == 0) { onSelect(0) }
        Tab(right, selectedIndex == 1) { onSelect(1) }
    }
}




@Composable
private fun TableHeader(headers: List<String>) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(headers.getOrElse(0) { "" }, Modifier.weight(1.1f), color = TextSecondary, fontSize = 13.sp)
        Text(headers.getOrElse(1) { "" }, Modifier.weight(0.9f), color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
        Text(headers.getOrElse(2) { "" }, Modifier.weight(1.1f), color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.End)
    }
}




@Composable
private fun TableRow(col1: String, col2: String, col3: String, hint: String? = null, onClick: (() -> Unit)? = null) {
    Surface(shape = RoundedCornerShape(12.dp), color = RowGrayBg, modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1.1f)) {
                Text(col1, fontWeight = FontWeight.Medium, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!hint.isNullOrBlank()) { Text(hint, fontSize = 12.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            Text(col2.ifBlank { "—" }, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center, color = TextMain, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Clip)
            Text(col3, modifier = Modifier.weight(1.1f), textAlign = TextAlign.End, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}




@Composable
private fun HistoryRowEditable(score: ScoreItem, showCheckbox: Boolean, checked: Boolean, onCheckedChange: (Boolean) -> Unit, onClick: (() -> Unit)? = null) {
    Surface(shape = RoundedCornerShape(12.dp), color = RowGrayBg, modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (showCheckbox) {
                Checkbox(checked = checked, onCheckedChange = { onCheckedChange(it) }, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Column(Modifier.weight(1.2f)) {
                Text(score.meet, fontWeight = FontWeight.Medium, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (score.date.isNotBlank()) { Text(score.date, fontSize = 12.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            Text(score.performance.ifBlank { "—" }, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End, color = TextMain, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Clip)
        }
    }
}




@Composable
private fun FilterPill(text: String, background: Color = AccentPillBg, textColor: Color = TextMain, onClick: () -> Unit) {
    Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(background).clickable { onClick() }.padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(text = text, color = textColor, fontSize = 13.sp)
    }
}


// ★ 新增：與 ScheduleScreen 風格一致的標題元件
@Composable
fun RecordSectionHeader(title: String) {
    Row(
        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp), //稍微調整間距讓畫面更舒適
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFFFFD54F)) // 馬卡龍黃
        )
        Spacer(Modifier.width(8.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
    }
}


/* --------------------- 趨勢圖 & 工具 --------------------- */




private data class ChartData(
    val date: LocalDate?,
    val dateLabel: String,
    val timeValue: Double, // 原始秒數
    val plotValue: Double, // 繪圖用：速度
    val meet: String?
)




// ★ 輔助：把秒數格式化回 "分:秒" 或 "秒"
private fun formatTimeDisplay(totalSeconds: Double): String {
    if (totalSeconds < 60) {
        return "%.2f".format(totalSeconds) // 短跑顯示 10.23
    }
    val m = (totalSeconds / 60).toInt()
    val s = totalSeconds % 60
    return "%d:%04.1f".format(m, s) // 長跑顯示 4:33.9
}




private fun extractDistance(eventName: String): Int? {
    val digits = eventName.filter { it.isDigit() || it == '.' }
    return digits.toIntOrNull()
}




@Composable
private fun PerformanceTrendCard(
    historyIndividual: List<ScoreItem>,
    historyTeam: List<ScoreItem>
) {
    var source by remember { mutableStateOf(0) }
    val raw = if (source == 0) historyIndividual else historyTeam




    val availableEvents by remember(raw) {
        mutableStateOf(
            raw.filter { it.performance.isNotBlank() && parseToSeconds(it.performance) != null }
                .map { it.event }
                .distinct()
                .sorted()
        )
    }
    var selectedEvent by remember(availableEvents) { mutableStateOf(availableEvents.firstOrNull() ?: "") }




    LaunchedEffect(availableEvents, source) {
        if (selectedEvent !in availableEvents) {
            selectedEvent = availableEvents.firstOrNull() ?: ""
        }
    }




    val distance = remember(selectedEvent) { extractDistance(selectedEvent) }
    val dateOutFmt = remember { DateTimeFormatter.ofPattern("yy.MM.dd") }




    val currentData = remember(selectedEvent, raw) {
        raw.filter { it.event == selectedEvent }
            .mapNotNull { s ->
                val sec = parseToSeconds(s.performance) ?: return@mapNotNull null
                val d = parseDateSmart(s.date)




                // ★ 關鍵邏輯：速度 = 距離 / 秒數。 秒數越小，速度越大 -> 圖形往上跑！
                val plotVal = if (distance != null && distance > 0) {
                    distance.toDouble() / sec
                } else {
                    sec // 沒有距離資訊 (如跳高)，維持原樣
                }




                ChartData(
                    date = d,
                    dateLabel = d?.format(dateOutFmt) ?: s.date,
                    timeValue = sec,
                    plotValue = plotVal,
                    meet = s.meet
                )
            }
            .sortedBy { it.date ?: LocalDate.MIN }
    }




    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 12.dp, end = 12.dp, top = 20.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { source = 0 },
                    label = { Text("個人") },
                    colors = AssistChipDefaults.assistChipColors(containerColor = if (source == 0) AccentPillBg else LightGrayBg)
                )
                AssistChip(
                    onClick = { source = 1 },
                    label = { Text("團體") },
                    colors = AssistChipDefaults.assistChipColors(containerColor = if (source == 1) AccentPillBg else LightGrayBg)
                )
            }




            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(12.dp))




            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedEvent.ifBlank { "無資料" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("項目", fontSize = 12.sp) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    enabled = availableEvents.isNotEmpty(),
                    modifier = Modifier.menuAnchor().widthIn(min = 130.dp).height(60.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DividerGray,
                        unfocusedBorderColor = DividerGray,
                        disabledBorderColor = DividerGray,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        focusedLabelColor = TextSecondary,
                        unfocusedLabelColor = TextSecondary
                    )
                )
                MaterialTheme(
                    colorScheme = MaterialTheme.colorScheme.copy(
                        primary = LightGrayBg,     // 想徹底去掉紫色，可以順便把 primary 改灰
                        onPrimary = TextMain,
                        surface = LightGrayBg,
                        surfaceVariant = LightGrayBg
                    )
                ) {
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        containerColor = LightGrayBg      // ★ 整個選單底色改灰色
                    ) {
                        availableEvents.forEach { ev ->
                            DropdownMenuItem(
                                text = { Text(ev, color = TextMain) },
                                onClick = {
                                    selectedEvent = ev
                                    expanded = false
                                },
                                // ★ 這裡只改字色，不能放 containerColor
                                colors = MenuDefaults.itemColors(
                                    textColor = TextMain,
                                    disabledTextColor = TextMain.copy(alpha = 0.4f)
                                )
                            )
                        }
                    }
                }


            }
        }




        // ★ 新增圖例 (Legend)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = TrendGood, shape = CircleShape, modifier = Modifier.size(10.dp)) {}
            Spacer(Modifier.width(6.dp))
            Text("進步", fontSize = 12.sp, color = TextSecondary)




            Spacer(Modifier.width(20.dp))




            Surface(color = TrendBad, shape = CircleShape, modifier = Modifier.size(10.dp)) {}
            Spacer(Modifier.width(6.dp))
            Text("退步", fontSize = 12.sp, color = TextSecondary)
        }




        Spacer(Modifier.height(8.dp))




        if (currentData.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("此來源/項目目前沒有可用數據", color = TextSecondary, fontStyle = FontStyle.Italic)
            }
            return@Column
        }




        // ★ 呼叫圖表 (傳入是否為速度模式)
        LineChartWithTrend(
            data = currentData,
            isSpeedMode = (distance != null && distance > 0),
            modifier = Modifier.fillMaxWidth(),
            chartHeight = 240.dp,
            labelAreaHeight = 100.dp, // ★ 增加到底部 100dp，保證不切到
            yAmplify = 1.15f,
            yTicks = 2,
            minPointGap = 64.dp,
            showGuideLines = true
        )




        Spacer(Modifier.height(12.dp))
    }
}




@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun LineChartWithTrend(
    data: List<ChartData>,
    isSpeedMode: Boolean,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 180.dp,
    labelAreaHeight: Dp = 100.dp,
    yAmplify: Float = 1.15f,
    yTicks: Int = 2,
    minPointGap: Dp = 64.dp,
    showGuideLines: Boolean = true
) {
    if (data.isEmpty()) return


    var selectedIndex by remember(data) { mutableStateOf<Int?>(null) }


    val rawMax = data.maxOf { it.plotValue }
    val rawMin = data.minOf { it.plotValue }
    val margin = (rawMax - rawMin).coerceAtLeast(0.1) * 0.15
    val maxValue = rawMax + margin
    val minValue = rawMin - margin
    val range = (maxValue - minValue).coerceAtLeast(0.1)


    // PB 判斷
    val bestRecordIndex = remember(data) {
        if (isSpeedMode) {
            data.withIndex().maxByOrNull { it.value.plotValue }?.index
        } else {
            data.withIndex().minByOrNull { it.value.plotValue }?.index
        }
    }


    // Paint 物件設定 (維持原樣)
    val valueLabelPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#444444")
            textSize = 28f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }
    }
    val xDatePaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 26f
            textAlign = android.graphics.Paint.Align.RIGHT
            isAntiAlias = true
        }
    }
    val tooltipTextPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 36f
            textAlign = android.graphics.Paint.Align.LEFT
            isAntiAlias = true
        }
    }
    val tooltipBgPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            isAntiAlias = true
        }
    }
    val tooltipBorderPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.LTGRAY
            strokeWidth = 2f
            style = android.graphics.Paint.Style.STROKE
            isAntiAlias = true
        }
    }


    // ★ 修改：加大左側邊距，讓旋轉的日期文字不會被切掉
    val leftPadding = 45.dp
    val rightPadding = 40.dp
    val topPadding = 40.dp
    val scroll = rememberScrollState()


    BoxWithConstraints(modifier = modifier) {
        val n = data.size
        val viewportWidth = maxWidth
        val baseNeeded = (n - 1).coerceAtLeast(1) * minPointGap
        val usableViewport = viewportWidth - (leftPadding + rightPadding)
        val contentWidth = if (baseNeeded > usableViewport) baseNeeded else usableViewport


        Column(
            modifier = Modifier
                .horizontalScroll(scroll)
                .width(contentWidth + leftPadding + rightPadding)
        ) {
            Canvas(
                modifier = Modifier
                    .padding(start = leftPadding, end = rightPadding, top = topPadding, bottom = 0.dp)
                    .width(contentWidth)
                    .height(chartHeight)
                    .pointerInput(n, minValue, maxValue, range, data) {
                        detectTapGestures { pos ->
                            val chartWidth = size.width.toFloat()
                            val chartHeightPx = size.height.toFloat()
                            val stepX = if (n > 1) chartWidth / (n - 1) else chartWidth
                            val stepY = (chartHeightPx / range.toFloat()) * yAmplify


                            var bestIdx = -1
                            var bestDist = Float.MAX_VALUE
                            for (i in 0 until n) {
                                val x = i * stepX
                                val y = chartHeightPx - ((data[i].plotValue - minValue).toFloat() * stepY)
                                val d = sqrt((pos.x - x).pow(2) + (pos.y - y).pow(2))
                                if (d < bestDist) {
                                    bestDist = d
                                    bestIdx = i
                                }
                            }
                            selectedIndex = if (bestDist <= 64f) bestIdx else null
                        }
                    }
            ) {
                val chartWidth = size.width
                val chartHeightPx = size.height
                val stepX = if (n > 1) chartWidth / (n - 1) else chartWidth
                val stepY = (chartHeightPx / range.toFloat()) * yAmplify


                // Grid
                drawLine(Color(0xFFE0E0E0), Offset(0f, 0f), Offset(0f, chartHeightPx), 2f)
                drawLine(Color(0xFFE0E0E0), Offset(0f, chartHeightPx), Offset(chartWidth, chartHeightPx), 2f)


                val tickCount = (yTicks + 2).coerceAtLeast(3)
                for (t in 0 until tickCount) {
                    val ratio = t / (tickCount - 1f)
                    val value = maxValue - ratio * range
                    val y = chartHeightPx - ((value - minValue).toFloat() * stepY)
                    drawLine(
                        color = Color(0xFFF0F0F0),
                        start = Offset(0f, y),
                        end = Offset(chartWidth, y),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )
                }


                // 區域漸層
                val fillPath = Path()
                fillPath.moveTo(0f, chartHeightPx)
                for (i in 0 until n) {
                    val x = i * stepX
                    val y = chartHeightPx - ((data[i].plotValue - minValue).toFloat() * stepY)
                    fillPath.lineTo(x, y)
                }
                fillPath.lineTo((n - 1) * stepX, chartHeightPx)
                fillPath.close()


                val gradientColor = if(isSpeedMode) Color(0xFF66BB6A) else AccentOrange
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(gradientColor.copy(alpha = 0.15f), Color.Transparent),
                        startY = 0f,
                        endY = chartHeightPx
                    )
                )


                val guidePaint = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))


                for (i in 0 until data.lastIndex) {
                    val sv = data[i].plotValue
                    val ev = data[i + 1].plotValue
                    val start = Offset(i * stepX, chartHeightPx - ((sv - minValue).toFloat() * stepY))
                    val end = Offset((i + 1) * stepX, chartHeightPx - ((ev - minValue).toFloat() * stepY))


                    if (showGuideLines) {
                        drawLine(Color(0x22000000), Offset(start.x, chartHeightPx), start, 1f, pathEffect = guidePaint)
                    }


                    val diff = ev - sv
                    val segColor = if (isSpeedMode) {
                        if (diff > 0) TrendGood else TrendBad
                    } else {
                        if (diff < 0) TrendGood else TrendBad
                    }


                    drawLine(segColor, start, end, 6f)
                    drawCircle(segColor, 8f, start)


                    // 標示秒數
                    val secVal = data[i].timeValue
                    val text = formatTimeDisplay(secVal)
                    drawContext.canvas.nativeCanvas.drawText(text, start.x, start.y - 25f, valueLabelPaint)
                }


                // 最後一點
                val lastIdx = data.lastIndex
                val last = data[lastIdx]
                val lastOffset = Offset(lastIdx * stepX, chartHeightPx - ((last.plotValue - minValue).toFloat() * stepY))
                drawCircle(Color(0xFFFF9800), 8f, lastOffset)
                drawContext.canvas.nativeCanvas.drawText(formatTimeDisplay(last.timeValue), lastOffset.x, lastOffset.y - 25f, valueLabelPaint)


                if (showGuideLines) {
                    drawLine(Color(0x22000000), Offset(lastOffset.x, chartHeightPx), lastOffset, 1f, pathEffect = guidePaint)
                }


                // ★ 修改：繪製可愛的幾何皇冠 (取代 emoji)
                if (bestRecordIndex != null) {
                    val bestVal = data[bestRecordIndex].plotValue
                    val bx = bestRecordIndex * stepX
                    val by = chartHeightPx - ((bestVal - minValue).toFloat() * stepY)


                    // 皇冠的寬高
                    val crownW = 36f
                    val crownH = 24f


                    // 皇冠的高度
                    val offsetY = 75f


                    // 將畫筆移動到該點上方
                    translate(left = bx - crownW / 2, top = by - offsetY) {
                        val path = Path().apply {
                            // 起點左下
                            moveTo(0f, crownH)
                            // 底部直線
                            lineTo(crownW, crownH)
                            // 右邊稍微往外斜
                            lineTo(crownW, crownH * 0.4f)
                            // 右邊尖角
                            lineTo(crownW * 0.75f, crownH * 0.7f)
                            lineTo(crownW * 0.5f, 0f) // 中間尖角 (最高)
                            lineTo(crownW * 0.25f, crownH * 0.7f)
                            // 左邊尖角
                            lineTo(0f, crownH * 0.4f)
                            close()
                        }


                        // 填滿金色
                        drawPath(path, color = Color(0xFFFFD700))


                        // 在三個尖角畫上小圓點 (寶石)
                        drawCircle(Color(0xFFFF5252), radius = 3f, center = Offset(0f, crownH * 0.4f))
                        drawCircle(Color(0xFF448AFF), radius = 3f, center = Offset(crownW * 0.5f, 0f))
                        drawCircle(Color(0xFFFF5252), radius = 3f, center = Offset(crownW, crownH * 0.4f))
                    }
                }


                // Tooltip (維持原樣)
                selectedIndex?.let { idx ->
                    if (idx in data.indices) {
                        val x = idx * stepX
                        val y = chartHeightPx - ((data[idx].plotValue - minValue).toFloat() * stepY)
                        val content = data[idx].meet?.takeIf { it.isNotBlank() } ?: data[idx].dateLabel


                        val padding = 16f
                        val txtW = tooltipTextPaint.measureText(content)
                        val txtH = tooltipTextPaint.fontMetrics.let { it.descent - it.ascent }
                        val bw = txtW + padding * 2
                        val bh = txtH + padding * 2


                        var bx = x + 18f
                        var by = y - bh - 18f
                        if (bx + bw > chartWidth) bx = x - bw - 18f
                        if (bx < 0f) bx = 6f
                        if (by < 0f) by = y + 18f


                        val rect = android.graphics.RectF(bx, by, bx + bw, by + bh)
                        val r = 18f
                        val canvas = drawContext.canvas.nativeCanvas
                        canvas.drawRoundRect(rect, r, r, tooltipBgPaint)
                        canvas.drawRoundRect(rect, r, r, tooltipBorderPaint)
                        canvas.drawText(content, bx + padding, by + padding - tooltipTextPaint.fontMetrics.ascent, tooltipTextPaint)
                    }
                }
            }


            // X 軸 (旋轉日期)
            Canvas(
                modifier = Modifier
                    .padding(start = leftPadding, end = rightPadding)
                    .width(contentWidth)
                    .height(labelAreaHeight)
            ) {
                val n = data.size
                val stepX = if (n > 1) size.width / (n - 1) else size.width


                for (i in 0 until n) {
                    val x = i * stepX
                    drawContext.canvas.nativeCanvas.save()
                    drawContext.canvas.nativeCanvas.rotate(-45f, x, 0f)
                    // 文字起始點往下移
                    drawContext.canvas.nativeCanvas.drawText(data[i].dateLabel, x, 60f, xDatePaint)
                    drawContext.canvas.nativeCanvas.restore()
                }
            }
        }
    }
}


private fun parseToSeconds(s: String): Double? {
    val t = s.trim().lowercase()
    if (t.isBlank()) return null
    if (t in setOf("dq", "dns", "dnf", "—", "-")) return null
    val cleaned = t.removeSuffix("s").removeSuffix("sec").trim()
    return try {
        if (":" in cleaned) {
            val parts = cleaned.split(":")
            val mins = parts.dropLast(1).joinToString(":").toDoubleOrNull() ?: parts[0].toDouble()
            val secs = parts.last().toDouble()
            mins * 60 + secs
        } else cleaned.toDouble()
    } catch (_: Throwable) {
        null
    }
}




private fun parseDateSmart(s: String?): LocalDate? {
    if (s.isNullOrBlank()) return null
    val patterns = listOf("yyyy-MM-dd", "yyyy/M/d", "yyyy.MM.dd", "yyyyMMdd")
    for (p in patterns) {
        try { return LocalDate.parse(s.trim(), DateTimeFormatter.ofPattern(p)) } catch (_: DateTimeParseException) {}
    }
    return null
}









