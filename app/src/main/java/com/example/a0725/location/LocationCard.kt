package com.example.a0725.location

import android.Manifest
import android.content.pm.PackageManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch

@Composable
fun LocationCard(
    apiKey: String,
    modifier: Modifier = Modifier,
    onResolved: (city: String, district: String, lat: Double, lon: Double) -> Unit = { _,_,_,_ -> }
) {
    val context = LocalContext.current
    val fused = remember { LocationServices.getFusedLocationProviderClient(context) }
    val scope = rememberCoroutineScope()

    var label by remember { mutableStateOf("定位中…") }
    var fired by remember { mutableStateOf(false) }

    val callback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                if (fired) return

                // 先把當前的 LocationCallback 存下來
                val cb: LocationCallback = this

                scope.launch {
                    try {
                        // 保留您原本的 API 解析邏輯
                        val addr = getTwAddressFromLatLng(loc.latitude, loc.longitude, apiKey)
                        val cityN = addr.city.replace("台", "臺")
                        val districtN = addr.district.replace("台", "臺")

                        label = if (districtN.isNotEmpty()) districtN else cityN

                        // 保留您原本的 Firebase 寫入邏輯
                        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                            val db = FirebaseFirestore.getInstance()
                            db.collection("users").document(uid)
                                .set(mapOf("city" to cityN, "district" to districtN), SetOptions.merge())
                            db.collection("users").document(uid)
                                .collection("location").document("current")
                                .set(
                                    mapOf(
                                        "city" to cityN,
                                        "district" to districtN,
                                        "lat" to loc.latitude,
                                        "lon" to loc.longitude,
                                        "timestamp" to FieldValue.serverTimestamp()
                                    ),
                                    SetOptions.merge()
                                )
                        }

                        onResolved(cityN, districtN, loc.latitude, loc.longitude)

                        fired = true
                        try {
                            if (ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                fused.removeLocationUpdates(cb)
                            }
                        } catch (_: SecurityException) { /* ignore */ }

                    } catch (_: Exception) {
                        label = "定位失敗"
                    }
                }
            }
        }
    }

    // 權限請求
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startLocationUpdatesSafely(context, fused, callback)
            label = "定位中..."
        } else {
            label = "未授權"
        }
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) startLocationUpdatesSafely(context, fused, callback)
        else launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    DisposableEffect(Unit) {
        onDispose {
            try { fused.removeLocationUpdates(callback) } catch (_: SecurityException) {}
        }
    }

    // ─── UI 修改區 ───
    Surface(
        modifier = modifier.clickable {
            // 點擊重新定位
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fired = false
                startLocationUpdatesSafely(context, fused, callback)
                label = "更新中..."
            } else {
                launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        },
        shape = RoundedCornerShape(20.dp), // 圓角跟工具卡片一致
        color = Color(0xFFF3F4F6), // ★ 灰色背景
        tonalElevation = 6.dp,
        shadowElevation = 6.dp             // 扁平化風格
    ) {
        // 改用 Row 橫向排列：[Icon] [文字]
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // ★ 將原本的「現在位置」文字改為 Icon
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = Color(0xFF666666), // 深灰色圖示
                modifier = Modifier.size(22.dp)
            )

            Spacer(Modifier.width(8.dp))

            // 顯示抓到的地點 (label)
            Text(
                text = label,
                color = Color(0xFF333333),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun startLocationUpdatesSafely(
    context: android.content.Context,
    fused: FusedLocationProviderClient,
    callback: LocationCallback
) {
    if (ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) return

    try {
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000L)
            .setMinUpdateIntervalMillis(5_000L)
            .build()
        fused.requestLocationUpdates(req, callback, Looper.getMainLooper())
    } catch (_: SecurityException) {
        // ignore
    }
}