package com.example.a0725.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

// ───────────── 工具函式區 ─────────────

// 支援 CWB 的時間格式：含 +08:00 / 不含時區…
private fun parseTwTime(s: String?): Date? {
    if (s.isNullOrBlank()) return null
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ssXXX", // 2025-11-30T15:00:00+08:00
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd"
    )
    val tz = TimeZone.getTimeZone("Asia/Taipei")
    for (fmt in formats) {
        try {
            val sdf = SimpleDateFormat(fmt, Locale.TAIWAN).apply { timeZone = tz }
            return sdf.parse(s)
        } catch (_: Exception) {}
    }
    return null
}

private fun anyToDate(any: Any?): Date? = when (any) {
    null -> null
    is com.google.firebase.Timestamp -> any.toDate()
    is Number -> Date(any.toLong())
    is String -> parseTwTime(any)
    else -> null
}

private fun floorToHourMillis(
    ms: Long,
    tz: TimeZone = TimeZone.getTimeZone("Asia/Taipei")
): Long {
    val cal = Calendar.getInstance(tz).apply {
        timeInMillis = ms
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

private fun ceilToNextHourMillis(
    ms: Long,
    tz: TimeZone = TimeZone.getTimeZone("Asia/Taipei")
): Long {
    val cal = Calendar.getInstance(tz).apply {
        timeInMillis = ms
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.HOUR_OF_DAY, 1)
    }
    return cal.timeInMillis
}

// ───────────── 資料結構 ─────────────
private data class HourSlot(
    val time: Date,
    val temp: Int?,
    val desc: String?,
    val pop: Int?,
    val humidity: Int?
)

// ───────────── 從 Map 取欄位 ─────────────
// 這邊完全對應你 weather_v2 裡的欄位名稱
private fun pickTemp(m: Map<String, Any?>): Int? =
    (m["temp"] as? Number)?.toInt()

private fun pickPop(m: Map<String, Any?>): Int? =
    (m["pop"] as? Number)?.toInt()

private fun pickHumidity(m: Map<String, Any?>): Int? =
    (m["humidity"] as? Number)?.toInt()

private fun pickDesc(m: Map<String, Any?>): String? =
    m["description"] as? String

// ───────────── 時間比對 ─────────────

private fun covers(target: Long, start: Long, end: Long?): Boolean {
    val e = end ?: (start + 3 * 60 * 60 * 1000L) // 沒 end 就預設 3 小時
    val tol = 5 * 60_000L // 5 分鐘容錯
    return target in (start - tol)..(e - 1 + tol)
}

/**
 * blocks：每一筆就是你 screenshot 裡的一個 doc（含 start_dt / end_dt / temp / humidity / pop / description）
 * targetMs：要查的時間（現在時間或未來某一小時）
 */
private fun slotFromBlocksAt(
    blocks: List<Map<String, Any?>>,
    targetMs: Long
): HourSlot {
    if (blocks.isEmpty()) return HourSlot(Date(targetMs), null, null, null, null)

    fun pickStartMs(m: Map<String, Any?>): Long? =
        anyToDate(m["start_dt"])?.time
            ?: anyToDate(m["start"])?.time
            ?: anyToDate(m["time_dt"])?.time // 萬一 start 沒寫，也用 time_dt 補一下

    fun pickEndMs(m: Map<String, Any?>): Long? =
        anyToDate(m["end_dt"])?.time
            ?: anyToDate(m["end"])?.time

    // 1. 優先找「有包含 targetMs」的區間
    var hit = blocks.firstOrNull { m ->
        val s = pickStartMs(m)
        val e = pickEndMs(m)
        if (s != null) covers(targetMs, s, e) else false
    }

    // 2. 如果沒有剛好覆蓋的，就找「開頭時間最接近」的一筆
    if (hit == null) {
        hit = blocks.minByOrNull { m ->
            val s = pickStartMs(m) ?: Long.MAX_VALUE
            abs(s - targetMs)
        }
    }

    val temp = hit?.let { pickTemp(it) }
    val desc = hit?.let { pickDesc(it) }
    val pop = hit?.let { pickPop(it) }
    val hum = hit?.let { pickHumidity(it) }

    val time = Date(targetMs)
    return HourSlot(time, temp, desc, pop, hum)
}

private fun buildNext8Hours(
    blocks: List<Map<String, Any?>>,
    nowMs: Long = System.currentTimeMillis()
): List<HourSlot> {
    val tz = TimeZone.getTimeZone("Asia/Taipei")
    val start = ceilToNextHourMillis(nowMs, tz)
    return (0 until 8).map { k ->
        val t = start + k * 60 * 60 * 1000L
        slotFromBlocksAt(blocks, t)
    }
}

// ───────────── UI ─────────────

@Composable
private fun WeatherIcon(
    desc: String?,
    size: Int = 24,
    isNight: Boolean = false,
    pop: Int? = null
) {
    val p = pop ?: 0
    val d = desc ?: ""

    val txt = when {
        // 先用機率決定，下大雨
        p >= 60 -> "🌧️"
        // 有點雨 / 局部雨
        p in 30..59 -> "🌦️"

        // 下面才看文字描述
        "雷" in d -> "⛈️"

        // 只有在機率不低的時候，看到「雨」才畫雨
        "雨" in d && p >= 30 -> "🌧️"

        "陰" in d -> "☁️"
        "多雲" in d -> if (isNight) "🌙" else "⛅️"

        else -> if (isNight) "🌙" else "☀️"
    }

    Text(text = txt, fontSize = size.sp)
}

@Composable
private fun MetricCard(
    title: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp,
        shadowElevation = 4.dp,
        color = Color.White,
        modifier = modifier
            .height(120.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                title,
                fontSize = 15.sp,
                color = Color(0xFF666666),
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                content()
            }
        }
    }
}

private val BigValueFontSize = 26.sp
private val BigValueFontWeight = FontWeight.Bold
private val BigValueColor = Color(0xFF333333)
private val SmallLabelFontSize = 12.sp
private val SmallLabelColor = Color.Gray

// =========================================
// ① 天氣（即時）
// =========================================
@Composable
fun WeatherTileRt(modifier: Modifier = Modifier) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val db = remember { FirebaseFirestore.getInstance() }
    var blocks by remember { mutableStateOf(emptyList<Map<String, Any?>>()) }
    var descNow by remember { mutableStateOf<String?>(null) }
    var show by remember { mutableStateOf(false) }
    var tempNow by remember { mutableStateOf<Int?>(null) }
    var popNow by remember { mutableStateOf<Int?>(null) }

    DisposableEffect(uid) {
        if (uid == null) return@DisposableEffect onDispose {}
        val reg = db.collection("weather_v2").document(uid)
            .collection("forecast")
            .orderBy("start_dt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.map { it.data ?: emptyMap() } ?: emptyList()
                blocks = list

                val nowMs = System.currentTimeMillis()
                val hit = slotFromBlocksAt(list, nowMs)

                tempNow = hit.temp
                descNow = hit.desc
                popNow = hit.pop
            }
        onDispose { reg.remove() }
    }

    MetricCard(
        title = "天氣",
        modifier = modifier,
        onClick = { if (blocks.isNotEmpty()) show = true }
    ) {
        Text(
            text = tempNow?.let { "$it°C" } ?: "--",
            fontSize = BigValueFontSize,
            fontWeight = BigValueFontWeight,
            color = BigValueColor
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            val isNight = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) !in 6..17
            WeatherIcon(descNow, size = 18, isNight = isNight, pop = popNow)
            Spacer(Modifier.width(4.dp))
            val displayDesc = descNow?.split("。")?.firstOrNull() ?: "概況"
            Text(
                displayDesc.take(6),
                fontSize = SmallLabelFontSize,
                color = SmallLabelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    if (show) {
        AlertDialog(
            onDismissRequest = { show = false },
            containerColor = Color.White,
            confirmButton = {
                TextButton(onClick = { show = false }) { Text("關閉") }
            },
            title = { Text("接下來的天氣", fontWeight = FontWeight.Bold, color = Color.Black) },
            text = {
                val hourly = remember(blocks) { buildNext8Hours(blocks) }
                val hh = remember {
                    SimpleDateFormat("HH:00", Locale.TAIWAN).apply {
                        timeZone = TimeZone.getTimeZone("Asia/Taipei")
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(hourly) { h ->
                        val hourLabel = hh.format(h.time)
                        val cal = Calendar.getInstance().apply { time = h.time }
                        val isNight = cal.get(Calendar.HOUR_OF_DAY) !in 6..17
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            tonalElevation = 2.dp,
                            color = Color(0xFFF7F7F7),
                            modifier = Modifier
                                .size(width = 84.dp, height = 120.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(6.dp)
                            ) {
                                Text(
                                    h.temp?.let { "$it°C" } ?: "--",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Black
                                )
                                Spacer(Modifier.height(6.dp))
                                WeatherIcon(h.desc, size = 22, isNight = isNight, pop = h.pop)
                                Spacer(Modifier.height(8.dp))
                                Text(hourLabel, fontSize = 12.sp, color = Color.Gray)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    h.pop?.let { "${it}%" } ?: "--",
                                    fontSize = 12.sp,
                                    color = Color(0xFF616161)
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}

// =========================================
// ② AQI（即時；沿用原本 aqi_data）
// =========================================
@Composable
fun AqiTileRt(modifier: Modifier = Modifier) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val db = remember { FirebaseFirestore.getInstance() }
    var details by remember { mutableStateOf<Map<String, Any?>?>(null) }
    var aqi by remember { mutableStateOf<Int?>(null) }
    var show by remember { mutableStateOf(false) }

    val millis = (details?.get("timestamp") as? Number)?.toLong()
    val sdf = remember {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.TAIWAN).apply {
            timeZone = TimeZone.getTimeZone("Asia/Taipei")
        }
    }
    val timeTW = millis?.let { sdf.format(Date(it)) } ?: "--"

    DisposableEffect(uid) {
        if (uid == null) return@DisposableEffect onDispose {}
        val reg = db.collection("aqi_data").document(uid)
            .collection("latest")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snap, _ ->
                val m = snap?.documents?.firstOrNull()?.data
                details = m
                aqi = (m?.get("aqi") as? Number)?.toInt()
            }
        onDispose { reg.remove() }
    }

    fun aqiColor(v: Int?): Color = when (v ?: -1) {
        in 0..50 -> Color(0xFF27C36F)
        in 51..100 -> Color(0xFFFFC107)
        in 101..150 -> Color(0xFFFF9800)
        in 151..200 -> Color(0xFFE53935)
        else -> Color(0xFF7E57C2)
    }

    fun aqiStatus(v: Int?): String = when (v ?: -1) {
        in 0..50 -> "良好"
        in 51..100 -> "普通"
        in 101..150 -> "敏感"
        in 151..200 -> "不健康"
        else -> "危險"
    }

    MetricCard(
        title = "AQI",
        modifier = modifier,
        onClick = { if (details != null) show = true }
    ) {
        Text(
            text = aqi?.toString() ?: "--",
            fontSize = BigValueFontSize,
            fontWeight = BigValueFontWeight,
            color = aqiColor(aqi)
        )
        Text("指數", fontSize = SmallLabelFontSize, color = SmallLabelColor)
    }

    if (show && details != null) {
        val pm25 = (details!!["pm2_5"] as? Number)?.toInt()
        val pm10 = (details!!["pm10"] as? Number)?.toInt()
        val site = details!!["sitename"] as? String ?: "未知測站"

        AlertDialog(
            onDismissRequest = { show = false },
            containerColor = Color.White,
            confirmButton = {},
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("空氣品質詳情", fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(Modifier.weight(1f))
                    Surface(
                        color = aqiColor(aqi).copy(alpha = 0.2f),
                        contentColor = aqiColor(aqi),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = aqiStatus(aqi),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(100.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = 1f,
                            color = aqiColor(aqi).copy(alpha = 0.2f),
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 8.dp
                        )
                        CircularProgressIndicator(
                            progress = ((aqi ?: 0).toFloat() / 200f).coerceIn(0f, 1f),
                            color = aqiColor(aqi),
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 8.dp
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${aqi ?: "--"}",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = aqiColor(aqi)
                            )
                            Text(text = "AQI", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                    Divider(color = Color(0xFFEEEEEE))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("PM2.5", color = Color.Gray, fontSize = 12.sp)
                            Text(
                                "${pm25 ?: "--"}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp,
                                color = Color.Black
                            )
                            Text("μg/m³", color = Color.LightGray, fontSize = 10.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("PM10", color = Color.Gray, fontSize = 12.sp)
                            Text(
                                "${pm10 ?: "--"}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp,
                                color = Color.Black
                            )
                            Text("μg/m³", color = Color.LightGray, fontSize = 10.sp)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("測站：$site", fontSize = 12.sp, color = Color.Gray)
                        Spacer(Modifier.width(12.dp))
                        Text(timeTW, fontSize = 12.sp, color = Color.LightGray)
                    }
                }
            }
        )
    }
}

// =========================================
// ③ 濕度（即時）
// =========================================
@Composable
fun HumidityTileRt(modifier: Modifier = Modifier) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val db = remember { FirebaseFirestore.getInstance() }
    var blocks by remember { mutableStateOf(emptyList<Map<String, Any?>>()) }
    var rhNow by remember { mutableStateOf<Int?>(null) }

    DisposableEffect(uid) {
        if (uid == null) return@DisposableEffect onDispose {}
        val reg = db.collection("weather_v2").document(uid)
            .collection("forecast")
            .orderBy("start_dt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.map { it.data ?: emptyMap() } ?: emptyList()
                blocks = list

                val nowMs = System.currentTimeMillis()
                val hit = slotFromBlocksAt(list, nowMs)
                rhNow = hit.humidity
            }
        onDispose { reg.remove() }
    }

    MetricCard(title = "濕度", modifier = modifier) {
        Text(
            text = rhNow?.let { "$it%" } ?: "--",
            fontSize = BigValueFontSize,
            fontWeight = BigValueFontWeight,
            color = BigValueColor
        )
        Text("相對", fontSize = SmallLabelFontSize, color = SmallLabelColor)
    }
}
