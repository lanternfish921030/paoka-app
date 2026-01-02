package com.example.a0725.screens


import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.a0725.R
import com.example.a0725.health.HealthConnectUtils
import com.example.a0725.health.HealthData
import com.example.a0725.location.LocationCard
import com.example.a0725.model.HomeViewModel
import com.example.a0725.navigation.Routes
import com.example.a0725.navigation.navigateSingleTopTo
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


// ─── 1. 定義 8 種健康數據類型 ───
enum class HomeMetric(
    val label: String,
    val unit: String,
    val icon: ImageVector,
    val color: Color
) {
    STEPS("步數", "步", Icons.Rounded.DirectionsWalk, Color(0xFFFFA726)),
    HEART_RATE("心率", "bpm", Icons.Rounded.Favorite, Color(0xFFEF5350)),
    DISTANCE("距離", "km", Icons.Rounded.Map, Color(0xFF42A5F5)),
    SPEED("速度", "km/h", Icons.Rounded.Speed, Color(0xFF7E57C2)),
    RESTING_HR("靜止心率", "bpm", Icons.Rounded.FavoriteBorder, Color(0xFF8D6E63)),
    VO2_MAX("最大攝氧", "ml/kg", Icons.Rounded.Air, Color(0xFF26A69A)),
    HRV("心率變異", "ms", Icons.Rounded.MonitorHeart, Color(0xFFEC407A)),
    VERT_OSC("垂直振幅", "cm", Icons.Rounded.Height, Color(0xFF5C6BC0));


    fun getValueString(data: HealthData): String {
        return when (this) {
            STEPS -> if (data.steps > 0) "${data.steps}" else "--"
            HEART_RATE -> data.heartRateBpm?.toString() ?: "--"
            DISTANCE -> if (data.distanceMeters > 0) String.format("%.1f", data.distanceMeters / 1000) else "--"
            SPEED -> data.speed?.let { String.format("%.1f", it * 3.6) } ?: "--"
            RESTING_HR -> data.restingHeartRateBpm?.toString() ?: "--"
            VO2_MAX -> data.vo2Max?.let { String.format("%.1f", it) } ?: "--"
            HRV -> data.hrvRmssd?.let { String.format("%.0f", it) } ?: "--"
            VERT_OSC -> "--"
        }
    }
}


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@ExperimentalMaterial3Api
@Composable
fun MainScreen(nav: NavController) {


    val backgroundColor = Color.White
    val profileVm: com.example.a0725.model.ProfileViewModel = viewModel()
    val photoUrl by profileVm.photoUrl.collectAsStateWithLifecycle()
    val uid = Firebase.auth.currentUser?.uid
    val firestore = Firebase.firestore
    val homeVm: HomeViewModel = viewModel()


    var healthState by remember { mutableStateOf(HealthData()) }
    val context = LocalContext.current
    var metricOrder by remember { mutableStateOf(HomeMetric.values().toList()) }
    var showReorderDialog by remember { mutableStateOf(false) }


    LaunchedEffect(Unit) {
        val status = androidx.health.connect.client.HealthConnectClient.getSdkStatus(context, "com.google.android.apps.healthdata")
        if (status == androidx.health.connect.client.HealthConnectClient.SDK_AVAILABLE &&
            HealthConnectUtils.hasAllPermissions(context)) {
            healthState = HealthConnectUtils.readCurrentHealthData(context)
        }
    }


    DisposableEffect(uid) {
        if (uid == null) return@DisposableEffect onDispose {}
        val reg = firestore.collection("users").document(uid)
            .addSnapshotListener { snap, _ ->
                snap?.getString("photoUrl")?.let { profileVm.updatePhotoUrl(it) }
            }
        onDispose { reg.remove() }
    }


    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        containerColor = backgroundColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("首頁", fontSize = 22.sp, color = Color(0xFF333333)) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = backgroundColor),
                navigationIcon = {
                    IconButton(
                        onClick = { nav.navigateSingleTopTo(Routes.PERSONAL) },
                        modifier = Modifier.padding(start = 16.dp)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(photoUrl ?: "")
                                .crossfade(true)
                                .build(),
                            contentDescription = "User Avatar",
                            placeholder = painterResource(R.drawable.user),
                            error = painterResource(R.drawable.user),
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color.LightGray, CircleShape)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->


        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(Modifier.height(2.dp))


            // 1. 天氣區塊
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WeatherTileRt(modifier = Modifier.weight(1f))
                AqiTileRt(modifier = Modifier.weight(1f))
                HumidityTileRt(modifier = Modifier.weight(1f))
            }


            // 2. 位置與工具卡片
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 左邊：位置 (0.6f)
                Box(modifier = Modifier.weight(0.6f)) {
                    LocationCard(
                        apiKey = "AIzaSyBlPNO8HfgvVP0J4032KvALkCnotaDVqGg",
                        modifier = Modifier.fillMaxSize(),
                        onResolved = { city, district, lat, lon ->
                            homeVm.bootstrapIfNeeded(city, district, lat, lon)
                        }
                    )
                }


                // 右邊：工具 (白色底)
                val pagerState = rememberPagerState(pageCount = { metricOrder.size })


                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                        .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(20.dp))
                        .clickable { nav.navigateSingleTopTo(Routes.TOOL) }
                ) {
                    Box(Modifier.fillMaxSize()) {
                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                            val metric = metricOrder[page]
                            val valueText = metric.getValueString(healthState)


                            Box(modifier = Modifier.fillMaxSize()) {
                                // 背景裝飾
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawCircle(
                                        color = metric.color.copy(alpha = 0.08f),
                                        radius = size.height * 0.9f,
                                        center = Offset(size.width * 1.1f, size.height * 0.5f)
                                    )
                                }


                                // 1. 頂部列：左標題 + 右設定 (垂直對齊)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.TopCenter)
                                        .padding(start = 10.dp, end = 6.dp, top = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 左：圖示 + 名稱
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(metric.color.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = metric.icon,
                                                contentDescription = null,
                                                tint = metric.color,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = metric.label,
                                            color = Color.Gray,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                    }


                                    // 右：設定按鈕
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clickable { showReorderDialog = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "Edit Order",
                                            tint = Color.LightGray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }


                                // 2. 中央數據：改為 Row 讓單位在右邊
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 20.dp), // ★ 數值區域整體置底
                                    verticalAlignment = Alignment.Bottom, // ★ 底部對齊 (數值與單位)
                                    horizontalArrangement = Arrangement.Center // ★ 水平置中
                                ) {
                                    Text(
                                        text = valueText,
                                        color = metric.color,
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 26.sp
                                    )
                                    Spacer(Modifier.width(4.dp)) // 數值與單位的間距
                                    Text(
                                        text = metric.unit,
                                        color = Color.Gray.copy(alpha = 0.8f),
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(bottom = 3.dp) // 微調讓單位對齊數字底部
                                    )
                                }
                            }
                        }


                        // 底部指示點
                        Box(modifier = Modifier.fillMaxSize().padding(bottom = 6.dp), contentAlignment = Alignment.BottomCenter) {
                            Row(horizontalArrangement = Arrangement.Center) {
                                repeat(metricOrder.size) { iteration ->
                                    val color = if (pagerState.currentPage == iteration) metricOrder[pagerState.currentPage].color else Color.LightGray.copy(alpha = 0.5f)
                                    Box(modifier = Modifier.padding(1.5.dp).clip(CircleShape).background(color).size(3.dp))
                                }
                            }
                        }
                    }
                }
            }


            // 3. 標題
            Text(
                text = "工具列",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
            )


            // 4. 下方卡片區
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HomeFeatureCard(
                    title = "我的賽程",
                    subtitle = "個人賽事排程管理",
                    icon = Icons.Outlined.Timer,
                    startColor = Color(0xFFEBC08D),
                    endColor = Color(0xFFD69E55),
                    onClick = { nav.navigateSingleTopTo(Routes.SCHEDULE) },
                    modifier = Modifier.weight(1f)
                )
                HomeFeatureCard(
                    title = "賽事成績查詢",
                    subtitle = "歷年田徑賽事數據",
                    icon = Icons.Outlined.EmojiEvents,
                    startColor = Color(0xFF90B4CE),
                    endColor = Color(0xFF638CA6),
                    onClick = { nav.navigateSingleTopTo(Routes.SEARCH) },
                    modifier = Modifier.weight(1f)
                )
                HomeFeatureCard(
                    title = "成績紀錄",
                    subtitle = "追蹤您的進步軌跡",
                    icon = Icons.Outlined.Timeline,
                    startColor = Color(0xFFA5C9A7),
                    endColor = Color(0xFF759C78),
                    onClick = { nav.navigateSingleTopTo(Routes.RECORD) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }


    if (showReorderDialog) {
        MetricReorderDialog(
            currentOrder = metricOrder,
            onConfirm = { newOrder ->
                metricOrder = newOrder
                showReorderDialog = false
            },
            onDismiss = { showReorderDialog = false }
        )
    }
}


// 卡片元件 (保持不變)
@Composable
fun HomeFeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    startColor: Color,
    endColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 3.dp,
        tonalElevation = 3.dp,
        color = Color.White
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(startColor, endColor)))
        ) {
            Icon(
                imageVector = icon, contentDescription = null,
                tint = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(100.dp).align(Alignment.CenterEnd).offset(x = 10.dp, y = 10.dp)
            )
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color.White.copy(alpha = 0.85f), shape = CircleShape, modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = icon, contentDescription = null, tint = endColor, modifier = Modifier.size(26.dp))
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(verticalArrangement = Arrangement.Center) {
                    Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 0.5.sp)
                    Text(text = subtitle, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.9f))
                }
            }
        }
    }
}


// 排序視窗 (已修改按鈕顏色)
// ─── 排序視窗 (已加高，讓 8 個項目不用滑動就能看完) ───
@Composable
fun MetricReorderDialog(
    currentOrder: List<HomeMetric>,
    onConfirm: (List<HomeMetric>) -> Unit,
    onDismiss: () -> Unit
) {
    var tempList by remember { mutableStateOf(currentOrder.toMutableList()) }


    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            // ★ 修改處：原本 max = 500.dp 改為 700.dp
            // 這樣高度足夠容納 8 個項目，就不會出現卷軸了
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 700.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "調整數據顯示順序",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )


                Spacer(modifier = Modifier.height(16.dp))


                // 列表區
                LazyColumn(
                    // weight(1f, fill = false) 讓它根據內容高度長大，但最高不超過 Surface 設定的 700dp
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(tempList) { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp), // 稍微調整內距讓整體更緊湊一點點
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(item.icon, null, tint = item.color, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(item.label, fontSize = 16.sp, color = Color.Black)
                            }
                            Row {
                                if (index > 0) {
                                    IconButton(
                                        onClick = {
                                            val newList = tempList.toMutableList()
                                            val prev = newList[index - 1]
                                            newList[index - 1] = item
                                            newList[index] = prev
                                            tempList = newList
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) { Icon(Icons.Default.ArrowUpward, null, tint = Color.Gray) }
                                }
                                Spacer(Modifier.width(8.dp))
                                if (index < tempList.size - 1) {
                                    IconButton(
                                        onClick = {
                                            val newList = tempList.toMutableList()
                                            val next = newList[index + 1]
                                            newList[index + 1] = item
                                            newList[index] = next
                                            tempList = newList
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) { Icon(Icons.Default.ArrowDownward, null, tint = Color.Gray) }
                                }
                            }
                        }
                    }
                }


                Spacer(modifier = Modifier.height(24.dp))


                // 按鈕區
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = Color.Gray)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(tempList) },
                        // 這是您之前指定的橘色
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA726))
                    ) {
                        Text("儲存")
                    }
                }
            }
        }
    }
}



