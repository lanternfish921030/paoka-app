// ToolScreen.kt
package com.example.a0725.screens


import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.a0725.R
import com.example.a0725.health.DailyDataPoint
import com.example.a0725.health.HealthConnectUtils
import com.example.a0725.health.HealthData
import com.example.a0725.health.HealthMetricType
import com.example.a0725.model.ProfileViewModel
import com.example.a0725.navigation.Routes
import com.example.a0725.navigation.navigateSingleTopTo
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale


// 擴充 MetricItem，加入 type 屬性
data class MetricItem(
    val title: String,
    val value: String,
    val unit: String = "",
    val icon: ImageVector,
    val iconTint: Color,
    val type: HealthMetricType = HealthMetricType.NONE // 對應 HealthConnect 的類型
)


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolScreen(nav: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profileVm: ProfileViewModel = viewModel()
    val photoUrl by profileVm.photoUrl.collectAsStateWithLifecycle()


    // UI 狀態
    var selectedItem by remember { mutableStateOf<MetricItem?>(null) }
    var healthState by remember { mutableStateOf(HealthData()) }
    var syncStatus by remember { mutableIntStateOf(0) }
    var isSyncing by remember { mutableStateOf(false) }


    // 歷史數據 (動態載入)
    var historicalData by remember { mutableStateOf<List<DailyDataPoint>>(emptyList()) }
    var isChartLoading by remember { mutableStateOf(false) }


    // 讀取當前數據
    fun refreshCurrentData() {
        scope.launch {
            isSyncing = true
            val startTime = System.currentTimeMillis()
            if (HealthConnectUtils.hasAllPermissions(context)) {
                healthState = HealthConnectUtils.readCurrentHealthData(context)
                syncStatus = 2
            } else {
                syncStatus = 1
            }
            val elapsedTime = System.currentTimeMillis() - startTime
            if (elapsedTime < 1000) delay(1000 - elapsedTime)
            isSyncing = false
        }
    }


    // 當點擊卡片時，讀取該卡片的歷史數據
    LaunchedEffect(selectedItem) {
        val item = selectedItem
        if (item != null && item.type != HealthMetricType.NONE) {
            isChartLoading = true
            historicalData = HealthConnectUtils.readHistoricalData(context, item.type)
            isChartLoading = false
        } else {
            historicalData = emptyList()
        }
    }


    val permissionLauncher = rememberLauncherForActivityResult(
        contract = HealthConnectUtils.createPermissionContract()
    ) { refreshCurrentData() }


    LaunchedEffect(Unit) {
        val status = HealthConnectClient.getSdkStatus(context, "com.google.android.apps.healthdata")
        if (status == HealthConnectClient.SDK_AVAILABLE) {
            if (HealthConnectUtils.hasAllPermissions(context)) {
                refreshCurrentData()
            } else {
                syncStatus = 1
            }
        }
    }


    // 格式化顯示
    val vo2Str = healthState.vo2Max?.let { String.format("%.1f", it) } ?: "--"
    val hrvStr = healthState.hrvRmssd?.let { String.format("%.0f", it) } ?: "--"
    val hrStr = healthState.heartRateBpm?.toString() ?: "--"
    val rhrStr = healthState.restingHeartRateBpm?.toString() ?: "--"
    val speedKmh = healthState.speed?.let { it * 3.6 }
    val speedStr = speedKmh?.let { String.format("%.1f", it) } ?: "--"
    val distKm = healthState.distanceMeters / 1000.0
    val distStr = if (healthState.distanceMeters > 0) String.format("%.2f", distKm) else "--"
    val stepsStr = if (healthState.steps > 0) "${healthState.steps}" else "--"


    val colorHeart = Color(0xFFE57373)
    val colorLung = Color(0xFF4DB6AC)
    val colorActivity = Color(0xFFFFB74D)
    val colorTech = Color(0xFF7986CB)


    // 定義所有卡片 (現在都有 type 了)
    val physio = listOf(
        MetricItem("最大攝氧量", vo2Str, "ml/kg/min", Icons.Rounded.Air, colorLung, HealthMetricType.VO2_MAX),
        MetricItem("心率變異度", hrvStr, "ms", Icons.Rounded.MonitorHeart, colorHeart, HealthMetricType.HRV),
        MetricItem("心率", hrStr, "bpm", Icons.Rounded.Favorite, colorHeart, HealthMetricType.HEART_RATE),
        MetricItem("靜止心率", rhrStr, "bpm", Icons.Rounded.FavoriteBorder, colorHeart, HealthMetricType.RESTING_HEART_RATE),
    )
    val others = listOf(
        MetricItem("步數", stepsStr, "步", Icons.Rounded.DirectionsWalk, colorActivity, HealthMetricType.STEPS),
        MetricItem("距離", distStr, "km", Icons.Rounded.Map, colorActivity, HealthMetricType.DISTANCE),
        MetricItem("速度", speedStr, "km/h", Icons.Rounded.Speed, colorTech, HealthMetricType.SPEED),
        MetricItem("垂直振幅", "--", "cm", Icons.Rounded.Height, colorTech, HealthMetricType.NONE),
    )


    Scaffold(
        containerColor = Color(0xFFF5F7FA),
        contentWindowInsets = WindowInsets(0.dp)
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding).fillMaxSize()) {


            // ── Header ──
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(photoUrl ?: "").crossfade(true).build(),
                        contentDescription = "Avatar",
                        placeholder = painterResource(R.drawable.user),
                        error = painterResource(R.drawable.user),
                        modifier = Modifier.size(60.dp).clip(CircleShape).border(1.dp, Color.LightGray, CircleShape).clickable { nav.navigateSingleTopTo(Routes.PERSONAL) },
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("健康概覽", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1C1E))
                        Text(LocalDate.now().format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.TAIWAN)), fontSize = 13.sp, color = Color.Gray)
                    }
                }
                val rotation by animateFloatAsState(targetValue = if (isSyncing) 360f else 0f, animationSpec = tween(1000, easing = LinearEasing), label = "sync")
                Surface(shape = CircleShape, color = Color.White, shadowElevation = 2.dp, modifier = Modifier.size(40.dp)) {
                    IconButton(onClick = { refreshCurrentData() }, enabled = !isSyncing) {
                        Icon(Icons.Rounded.Sync, "Sync", modifier = Modifier.rotate(rotation), tint = if (isSyncing) Color(0xFF2196F3) else Color(0xFF757575))
                    }
                }
            }


            // ── Grid ──
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f)
            ) {
                item(span = { GridItemSpan(2) }) { Text("生理指標", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF424242), modifier = Modifier.padding(vertical = 4.dp)) }
                items(physio) { metric -> ElegantMetricCard(metric) { selectedItem = metric } }


                item(span = { GridItemSpan(2) }) { Text("運動表現", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF424242), modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) }
                items(others) { metric -> ElegantMetricCard(metric) { selectedItem = metric } }


                item(span = { GridItemSpan(2) }) { Spacer(Modifier.height(80.dp)) }
            }
        }


        // 權限 SnackBar
        if (syncStatus == 1) {
            Box(Modifier.fillMaxSize().padding(bottom = 30.dp), contentAlignment = Alignment.BottomCenter) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F)), modifier = Modifier.padding(horizontal = 24.dp).clickable { try { permissionLauncher.launch(HealthConnectUtils.PERMISSIONS) } catch(e:Exception){} }) {
                    Text("請點擊授權 Health Connect 以讀取數據", color = Color.White, modifier = Modifier.padding(16.dp), fontSize = 14.sp)
                }
            }
        }


        // 詳細視窗
        selectedItem?.let { item ->
            MetricDetailSheet(
                item = item,
                historyData = historicalData,
                isLoading = isChartLoading,
                onDismiss = { selectedItem = null }
            )
        }
    }
}


@Composable
private fun ElegantMetricCard(item: MetricItem, onClick: () -> Unit) {
    val hasData = item.value != "--" && item.value != "0"
    Card(
        shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth().height(110.dp).clickable { onClick() }.padding(2.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(item.iconTint.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(item.icon, null, tint = item.iconTint, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(item.title, fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(item.value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (hasData) Color(0xFF1A1C1E) else Color.LightGray)
                if (hasData && item.unit.isNotEmpty()) {
                    Spacer(Modifier.width(4.dp))
                    Text(item.unit, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
                }
            }
        }
    }
}


// ── 詳細頁面 (含優化後的圖表) ──
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetricDetailSheet(
    item: MetricItem,
    historyData: List<DailyDataPoint>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)


    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color(0xFFF5F7FA)) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
            // Sheet Header
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(item.iconTint.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(item.icon, null, tint = item.iconTint, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(16.dp))
                Text(item.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1C1E))
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Close", tint = Color.Gray) }
            }


            Row(verticalAlignment = Alignment.Bottom) {
                Text(item.value, fontSize = 56.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1C1E), lineHeight = 56.sp)
                if(item.unit.isNotEmpty()){
                    Spacer(Modifier.width(8.dp))
                    Text(item.unit, fontSize = 20.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                }
            }


            Spacer(Modifier.height(32.dp))


            // 圖表區塊
            Text("過去 7 天趨勢", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(Modifier.height(16.dp))


            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth().height(250.dp).padding(bottom = 20.dp)
            ) {
                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = item.iconTint) }
                } else if (historyData.isEmpty() || historyData.all { it.value == 0.0 }) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("暫無歷史數據", color = Color.LightGray) }
                } else {
                    // 使用新的圖表元件
                    EnhancedChart(data = historyData, color = item.iconTint)
                }
            }
            Spacer(Modifier.height(48.dp))
        }
    }
}


// ── 專業級圖表繪製 (Canvas) ──
@Composable
fun EnhancedChart(data: List<DailyDataPoint>, color: Color) {
    Canvas(modifier = Modifier.fillMaxSize().padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)) {
        val maxVal = data.maxOfOrNull { it.value }?.toFloat() ?: 1f
        val safeMax = if (maxVal == 0f) 100f else maxVal * 1.1f // 留一點頭部空間


        val width = size.width
        val height = size.height
        val barWidth = (width / data.size) * 0.5f // 柱子寬度占 50%
        val spacing = width / data.size


        // 1. 繪製格線 (虛線)
        val gridLines = 3
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        for (i in 0..gridLines) {
            val y = height * (i.toFloat() / gridLines)
            drawLine(
                color = Color.LightGray.copy(alpha = 0.5f),
                start = Offset(0f, y),
                end = Offset(width, y),
                pathEffect = pathEffect,
                strokeWidth = 2f
            )
        }


        data.forEachIndexed { index, point ->
            val value = point.value.toFloat()
            val barHeight = (value / safeMax) * height
            val x = (spacing * index) + (spacing / 2)


            // 2. 繪製柱子
            if (value > 0) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x - barWidth / 2, height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                )


                // 3. 繪製柱子上的數值
                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        if (value >= 1000) String.format("%.1fk", value/1000) else String.format("%.0f", value),
                        x,
                        height - barHeight - 10f, // 數值在柱子上方
                        android.graphics.Paint().apply {
                            setColor(android.graphics.Color.DKGRAY)
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = 30f
                            isFakeBoldText = true
                        }
                    )
                }
            }


            // 4. 繪製底部日期標籤
            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    point.label, // "週一", "週二"
                    x,
                    height + 40f, // 顯示在底部外側
                    android.graphics.Paint().apply {
                        setColor(android.graphics.Color.GRAY)
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 28f
                    }
                )
            }
        }
    }
}



