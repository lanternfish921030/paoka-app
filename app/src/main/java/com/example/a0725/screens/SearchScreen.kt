package com.example.a0725.screens




import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.a0725.R
import com.example.a0725.navigation.Routes
import com.example.a0725.navigation.navigateSingleTopTo
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.time.LocalDate




// --------- 共用色票 (白底 + 邊框風格) ----------
private val ScreenBg = Color.White
private val CardSurface = Color.White
private val CardBorder = Color(0xFFF0F0F0)




private val TitleText = Color(0xFF4A4036)
private val SubTitleText = Color(0xFF757575)
private val FilterGray = Color(0xFFE0E0E0)




// 狀態標籤顏色
private val StatusUpcomingBg = Color(0xFFEEEEEE)
private val StatusUpcomingText = Color(0xFF757575)




private val StatusPastBg = Color(0xFFFFF3C8)
private val StatusPastText = Color(0xFF8D6E63)




private val StatusOngoingBg = Color(0xFFC5E1A5)
private val StatusOngoingText = Color(0xFF33691E)




private enum class MeetStatus { PAST, ONGOING, UPCOMING, UNKNOWN }




data class MeetOption(
    val name: String,
    val url: String,
    val year: String, // 原始資料的學年度 (如 2026)
    val city: String,
    val startDate: String?, // 實際日期 (如 2025-11-26)
    val endDate: String?
)




// ★ 輔助函式：取得該賽事的「實際舉辦年份」
// 如果有 startDate 就抓前四碼，否則才用原本的 year
private fun getRealYear(meet: MeetOption): String {
    return meet.startDate?.take(4) ?: meet.year
}




@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SerchScreen(
    nav: NavHostController
) {
    val profileVm: com.example.a0725.model.ProfileViewModel = viewModel()
    val photoUrl by profileVm.photoUrl.collectAsStateWithLifecycle()
    val edgePadding = 20.dp
    val uid = Firebase.auth.currentUser?.uid
    val firestore = Firebase.firestore




    val today = remember { LocalDate.now() }




    val allMeets = remember {
        listOf(
            // --- 2025 (原本被歸在 2026 的部分) ---
            MeetOption("115年桃園市中小聯運八德區選拔賽", "http://sports.taoyuansport.org.tw/114/1141126bdm/Default.htm", "2026", "桃園市", "2025-11-26", "2025-11-26"),
            MeetOption("115年桃園市中小聯運桃園區選拔賽", "http://sports.taoyuansport.org.tw/114/1141203tyd/Default.htm", "2026", "桃園市", "2025-12-03", "2025-12-03"),
            MeetOption("115年桃園市中小聯運大溪區選拔賽", "http://sports.taoyuansport.org.tw/114/1141210tcd/Default.htm", "2026", "桃園市", "2025-12-10", "2025-12-10"),
            MeetOption("115年桃園市中小聯運龍潭區選拔賽", "http://sports.taoyuansport.org.tw/114/1141203ltd/Default.htm", "2026", "桃園市", "2025-12-03", "2025-12-03"),




            // --- 2025 其他 ---
            MeetOption("114年臺北市秋季全國田徑公開賽", "https://w3.athletics.org.tw/tpetf202509/score_query_adv.php", "2025", "臺北市", "2025-09-17", "2025-09-21"),
            MeetOption("114年新北市青年盃全國田徑公開賽", "http://210.59.162.39/ntptf202503/score_query_adv.php", "2025", "新北市", "2025-03-10", "2025-03-14"),
            MeetOption("2025年高雄市國民中小學田徑錦標賽", "https://w2.athletics.org.tw/khtfa/score_query.php?action=menu&htm=2&mode=1&ItNo=21&device=&ID=&minor=&debug=0", "2025", "高雄市", "2025-06-17", "2025-06-18"),
            MeetOption("114年屏東縣運暨公教聯合運動會", "http://ptctf.ptc.edu.tw/ptca2025/score_query_adv.php?action=menu&htm=2&mode=1&ItNo=21&device=&ID=&minor=", "2025", "屏東縣", "2025-11-07", "2025-11-09"),
            MeetOption("114年屏東縣區域性田徑對抗賽", "http://ptctf.ptc.edu.tw/ptcgame2025/score_query_adv.php", "2025", "屏東縣", "2025-09-27", "2025-09-27"),
            MeetOption("嘉義縣114年中小學師生田徑錦標賽", "https://sport.cyc.edu.tw/z7/ft114ts/pub/score/sco_ft.php", "2025", "嘉義縣", "2025-06-04", "2025-06-06"),
            MeetOption("嘉義縣114年全縣運動會田徑賽", "https://sport.cyc.edu.tw/z7/spt114ac/pub/score/sco_ft.php", "2025", "嘉義縣", "2025-03-04", "2025-03-06"),
            MeetOption("嘉義縣114年中小學運動會", "https://sport.cyc.edu.tw/z8/spt114hp/", "2025", "嘉義縣", "2025-10-28", "2025-10-30"),
            MeetOption("彰化縣114年縣民運動大會賽程表", "https://w69.tcctf.idv.tw/changhua2025/score_query.php?action=menu&htm=2&mode=1&ItNo=21&debug=1", "2025", "彰化縣", "2025-08-06", "2025-08-08"),
            MeetOption("114年彰化市運動大會", "https://mkez.tw/500/modules/xi_sport/schedule.php?gc=s114", "2025", "彰化市", "2025-06-14", "2025-06-15"),
            MeetOption("彰化縣114年適應體育田徑賽", "https://sport.mkez.tw/sport/modules/xi_sport/schedule.php?gc=C202503", "2025", "彰化縣", "2025-03-07", "2025-03-07"),
            MeetOption("114年彰化市第43屆國民小學田徑錦標賽", "https://sport.mkez.tw/sport/modules/xi_sport/schedule.php?gc=c43", "2025", "彰化市", "2025-11-03", "2025-11-03"),
            MeetOption("新竹市114年度市長盃田徑公開賽", "http://sport.nowforyou.com/sh5r/register/gameeventqry_b.asp", "2025", "新竹市", "2025-07-11", "2025-07-11"),
            MeetOption("新竹市114年中小學田徑錦標賽", "http://sport.nowforyou.com/sh5r/register/gameeventqry_b.asp", "2025", "新竹市", "2025-02-25", "2025-02-27"),
            MeetOption("新竹縣114年全縣運動會賽程表", "https://w71.tcctf.idv.tw/hctf2025/score_query_adv.php?action=menu&htm=2&mode=1&ItNo=21&device=&ID=&minor=&debug=0", "2025", "新竹縣", "2025-09-08", "2025-09-11"),
            MeetOption("南投縣第73屆全縣運動會", "https://www.ntsport.org.tw/73ntsp/Module/SportScore/Index_A.php?RacetypeID=SportA&LineItemID=101", "2025", "南投縣", "2025-11-08", "2025-11-11"),
            MeetOption("南投縣114年中區田徑賽賽程表", "https://w2.athletics.org.tw/ntag2025/score_query_adv.php?action=menu&htm=1&mode=2&ItNo=21&device=&ID=&minor=&debug=0", "2025", "南投縣", "2025-09-13", "2025-09-13"),
            MeetOption("苗栗縣114年度體育會主委盃田徑賽", "https://w2.athletics.org.tw/mltf4/score_query.php?action=menu&htm=2&mode=1&ItNo=21&device=&ID=&minor=", "2025", "苗栗縣", "2025-11-29", "2025-11-29"),
            MeetOption("114年桃園市體育總會理事長盃接力賽", "http://sports.taoyuansport.org.tw/114/1141116rn/Default.htm", "2025", "桃園市", "2025-11-16", "2025-11-16"),
            MeetOption("114年桃園市運動會", "http://sports.taoyuansport.org.tw/114/1140718sfspt/Default.htm", "2025", "桃園市", "2025-07-18", "2025-07-21"),
            MeetOption("114年桃園市中小學校聯合運動會", "http://sports.taoyuansport.org.tw/114/1140103md/Default.htm", "2025", "桃園市", "2025-01-03", "2025-01-06"),
            MeetOption("新北市114年中等學校運動會", "https://sports.spnet.tw/cenm114/CMprocess.php", "2025", "新北市", "2025-02-24", "2025-02-27"),
            MeetOption("114年臺北市中正盃田徑賽", "https://w68.tcctf.idv.tw/tpetf202510/score_query_adv.php?ItNo=21&ItNa=%A5%D0%AE|", "2025", "臺北市", "2025-11-29", "2025-11-30"),
            MeetOption("臺東縣114年全縣運動會田徑競賽", "https://w2.athletics.org.tw/ttcg2025/score_query_adv.php?action=menu&htm=2&mode=1&ItNo=21&device=&ID=&minor=&debug=0", "2025", "臺東縣", "2025-06-06", "2025-06-07"),
            MeetOption("114年臺東沙城盃全國中小學田徑對抗賽", "https://w2.athletics.org.tw/tttf202510/score_query_adv.php?action=menu&htm=2&mode=1&ItNo=21&device=&ID=&minor=&debug=0", "2025", "臺東縣", "2025-11-14", "2025-11-16"),
            MeetOption("2025年第二屆楊傳廣盃全民田徑聯賽-第一場賽程表", "https://w116.tcctf.idv.tw/2025cky/score_query.php", "2025", "花蓮縣", "2025-10-24", "2025-10-26"),




            // --- 2026 ---
            MeetOption("115年桃園市中小學校聯合運動會", "http://sports.taoyuansport.org.tw/115/1150129md/Default.htm", "2026", "桃園市", "2026-01-29", "2026-02-01"),




            // --- 2024 ---
            MeetOption("2024年港都盃全國田徑錦標賽", "https://w3.athletics.org.tw/ks2024/score_query_adv.php?action=menu&htm=2&mode=1&ItNo=21&device=&ID=&minor=", "2024", "高雄市", "2024-02-23", "2024-02-27"),
            MeetOption("113年台南市聯合運動大會", "http://163.26.179.2/sport11401c/news5/n5_score_view.php", "2024", "台南市", "2024-12-14", "2024-12-15"),
        )
    }




    var selectedYear by remember { mutableStateOf("所有年份") }
    var selectedCity by remember { mutableStateOf("所有縣市") }




    // ★ 修改 1：下拉選單的年份，改抓「實際年份」
    val yearOptions = remember(allMeets) {
        listOf("所有年份") + allMeets.map { getRealYear(it) }.distinct().sortedDescending()
    }




    val cityOptions = remember(allMeets) {
        listOf("所有縣市") + allMeets.map { it.city }.distinct()
    }




    // ★ 修改 2：篩選邏輯，也改為比對「實際年份」
    val filteredMeets = remember(selectedYear, selectedCity, allMeets) {
        allMeets.filter { meet ->
            val realYear = getRealYear(meet)




            (selectedYear == "所有年份" || realYear == selectedYear) &&
                    (selectedCity == "所有縣市" || meet.city == selectedCity)
        }
    }




    // 分組邏輯：維持使用實際年份
    val groupedByYear = remember(filteredMeets, today) {
        val byYear = filteredMeets.groupBy { getRealYear(it) }
        byYear.toSortedMap(compareByDescending { it }).mapValues { (_, meets) ->
            meets.sortedWith(
                compareBy<MeetOption> { it.statusOrder(today) }
                    .thenBy { it.startLocalDate() ?: LocalDate.MAX }
            )
        }
    }




    var currentMeet by remember { mutableStateOf<MeetOption?>(null) }




    DisposableEffect(uid) {
        if (uid == null) return@DisposableEffect onDispose {}
        val reg = firestore.collection("users").document(uid)
            .addSnapshotListener { snap, _ ->
                snap?.getString("photoUrl")?.let { profileVm.updatePhotoUrl(it) }
            }
        onDispose { reg.remove() }
    }




    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("工具", fontSize = 25.sp, color = TitleText) },
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
                            contentDescription = "User Avatar",
                            placeholder = painterResource(R.drawable.user),
                            error = painterResource(R.drawable.user),
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color(0xFFE0E0E0), CircleShape)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = ScreenBg
                )
            )
        },
        containerColor = ScreenBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "賽事成績查詢",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TitleText,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            )




            // 下拉選單
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterDropdown(
                    value = selectedYear,
                    options = yearOptions,
                    modifier = Modifier.weight(1f)
                ) { selectedYear = it }




                FilterDropdown(
                    value = selectedCity,
                    options = cityOptions,
                    modifier = Modifier.weight(1f)
                ) { selectedCity = it }
            }




            Spacer(modifier = Modifier.height(16.dp))




            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                if (groupedByYear.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("沒有符合條件的賽事", color = SubTitleText)
                        }
                    }
                } else {
                    groupedByYear.forEach { (year, meets) ->
                        item {
                            SearchSectionHeader("$year 年")
                        }
                        items(meets) { meet ->
                            MeetItemCard(
                                meet = meet,
                                status = meet.status(today),
                                onClick = { currentMeet = meet }
                            )
                        }
                    }
                }
            }
        }
    }




    if (currentMeet != null) {
        WebViewDialog(
            meet = currentMeet!!,
            onDismiss = { currentMeet = null }
        )
    }
}




// --- Helper Functions ---
private fun MeetOption.startLocalDate(): LocalDate? = startDate?.let { LocalDate.parse(it) }
private fun MeetOption.endLocalDate(): LocalDate? = endDate?.let { LocalDate.parse(it) }




private fun MeetOption.status(today: LocalDate): MeetStatus {
    val s = startLocalDate()
    val e = endLocalDate()
    if (s == null || e == null) return MeetStatus.UNKNOWN
    return when {
        today.isBefore(s) -> MeetStatus.UPCOMING
        today.isAfter(e) -> MeetStatus.PAST
        else -> MeetStatus.ONGOING
    }
}




private fun MeetOption.statusOrder(today: LocalDate): Int =
    when (status(today)) {
        MeetStatus.ONGOING -> 0
        MeetStatus.PAST -> 1
        MeetStatus.UPCOMING -> 2
        MeetStatus.UNKNOWN -> 3
    }




private fun MeetOption.displayDate(): String {
    return if (startDate != null && endDate != null) {
        if (startDate == endDate) startDate else "$startDate ~ $endDate"
    } else {
        "日期未設定"
    }
}




// --- UI 元件：賽事卡片 ---
@Composable
private fun MeetItemCard(
    meet: MeetOption,
    status: MeetStatus,
    onClick: () -> Unit
) {
    val (bgColor, textColor, textLabel) = when (status) {
        MeetStatus.UPCOMING -> Triple(StatusUpcomingBg, StatusUpcomingText, "尚未開始")
        MeetStatus.PAST -> Triple(StatusPastBg, StatusPastText, "已結束")
        MeetStatus.ONGOING -> Triple(StatusOngoingBg, StatusOngoingText, "進行中")
        MeetStatus.UNKNOWN -> Triple(FilterGray, Color.Gray, "未定")
    }




    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(3.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meet.name,
                    color = TitleText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(8.dp))




                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, null, tint = SubTitleText, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(meet.displayDate(), color = SubTitleText, fontSize = 13.sp)




                    Spacer(Modifier.width(12.dp))




                    Icon(Icons.Default.LocationOn, null, tint = SubTitleText, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(meet.city, color = SubTitleText, fontSize = 13.sp)
                }
            }




            Spacer(Modifier.width(8.dp))




            Surface(
                color = bgColor,
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    text = textLabel,
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}


// ★ 新增：帶有黃色裝飾線的標題元件 (與其他頁面風格統一)
@Composable
fun SearchSectionHeader(title: String) {
    Row(
        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 黃色圓角長條
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFFFFD54F))
        )
        Spacer(Modifier.width(8.dp))


        // 標題文字
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TitleText
        )
    }
}


// --- UI 元件：下拉選單 ---
@Composable
fun FilterDropdown(
    value: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }




    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = BorderStroke(1.dp, CardBorder),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth().height(44.dp).clickable { expanded = true }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Text(text = value, color = TitleText, fontSize = 14.sp)
                Icon(Icons.Filled.ArrowDropDown, null, tint = Color.Gray)
            }
        }




        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt, color = TitleText) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}




// --- WebView ---
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewDialog(
    meet: MeetOption,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isTaoyuanSite = remember(meet.url) {
        "sports.taoyuansport.org.tw" in meet.url
    }




    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ScreenBg)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {




                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = meet.name,
                        color = TitleText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, "Close", tint = TitleText)
                    }
                }




                Divider(color = Color(0xFFE0E0E0))




                Box(modifier = Modifier.weight(1f)) {
                    var isLoading by remember { mutableStateOf(true) }
                    var webView: WebView? by remember { mutableStateOf(null) }




                    if (isTaoyuanSite) {
                        LaunchedEffect(meet.url) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(meet.url))
                            context.startActivity(intent)
                            onDismiss()
                        }
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("已在瀏覽器開啟", color = TitleText, fontSize = 14.sp)
                        }
                    } else {
                        BackHandler {
                            if (webView?.canGoBack() == true) webView?.goBack() else onDismiss()
                        }




                        AndroidView(
                            factory = { context ->
                                WebView(context).apply {
                                    webView = this
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        databaseEnabled = true
                                        loadWithOverviewMode = true
                                        useWideViewPort = true
                                        builtInZoomControls = true
                                        displayZoomControls = false
                                        setSupportZoom(true)
                                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                        javaScriptCanOpenWindowsAutomatically = true
//                                        setSupportMultipleWindows(true)
                                        userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                                    }
                                    webChromeClient = android.webkit.WebChromeClient()
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false
                                        override fun onPageFinished(view: WebView?, url: String?) { isLoading = false }
                                        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) { handler?.proceed() }
                                    }
                                    loadUrl(meet.url)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )




                        if (isLoading) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                                color = StatusOngoingBg
                            )
                        }
                    }
                }
            }
        }
    }
}







