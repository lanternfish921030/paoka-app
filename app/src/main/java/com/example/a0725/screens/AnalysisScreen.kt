package com.example.a0725.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@ExperimentalMaterial3Api
@Composable
fun AnalysisScreen(nav: NavHostController) {

    val backgroundColor = Color.White

    val profileVm: com.example.a0725.model.ProfileViewModel = viewModel()
    val photoUrl by profileVm.photoUrl.collectAsStateWithLifecycle()
    val uid = Firebase.auth.currentUser?.uid
    val firestore = Firebase.firestore

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
                title = {
                    Text(
                        "分析",
                        fontSize = 24.sp,

                        color = Color(0xFF333333)
                    )
                },
                navigationIcon = {
                    val context = LocalContext.current
                    IconButton(
                        onClick = { nav.navigateSingleTopTo(Routes.PERSONAL) },
                        modifier = Modifier.padding(start = 16.dp)
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
                                .size(50.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color.LightGray, CircleShape)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = backgroundColor)
            )
        }
    ) { innerPadding ->

        // ★ 修改：移除 verticalScroll，改用 Column + Weight 自動填滿
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp), // 底部留白
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 1. 頁面標題與引言 (固定高度區塊)
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    text = "選擇分析模式",
                    fontSize = 20.sp, // 稍微縮小一點點標題，留空間給卡片
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "透過影像辨識技術，精準檢測跑步姿態，提升跑步經濟性並預防傷害。",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    lineHeight = 20.sp
                )
            }

            // 2. 骨架追蹤卡片 (科技灰 - 代表結構)
            // ★ 使用 weight(1f) 自動佔據一半的剩餘空間
            AnalysisOptionCard(
                modifier = Modifier.weight(1f),
                title = "骨架追蹤",
                subtitle = "即時關節角度分析",
                description = "呈現並協助您追蹤及調整您的關節角度數據。",
                tags = listOf("MediaPipe", "即時關節角度"),
                icon = Icons.Filled.AccessibilityNew,

                // ★ 配色：溫暖的淺灰色底 + 深灰色主色
                backgroundColor = Color(0xFFF5F5F5), // 淺灰 (Grey 50)
                primaryColor = Color(0xFF616161),    // 深灰 (Grey 700)

                onClick = { nav.navigateSingleTopTo(Routes.SKELETON) }
            )

            // 3. 步態分析卡片 (品牌黃 - 代表動能)
            // ★ 使用 weight(1f) 自動佔據另一半的剩餘空間
            AnalysisOptionCard(
                modifier = Modifier.weight(1f),
                title = "步態分析",
                subtitle = "觸地期關鍵事件",
                description = "檢測初始觸地 (Initial Contact)、中間支撐 (Mid Stance) 與腳趾離地 (Toe Off) 三大事件。",
                tags = listOf("YOLO","MediaPipe","角度建議"),
                icon = Icons.Filled.DirectionsRun,

                // ★ 配色：淺黃色底 + 深黃/土黃主色
                backgroundColor = Color(0xFFFFFDE7), // 淺黃 (Yellow 50)
                primaryColor = Color(0xFFF9A825),    // 深黃 (Yellow 800)

                onClick = { nav.navigateSingleTopTo(Routes.GAIT) }
            )
        }
    }
}

// ───────────── 專業分析卡片元件 (支援 Weight) ─────────────
@Composable
fun AnalysisOptionCard(
    modifier: Modifier = Modifier, // 接收外部傳入的 weight
    title: String,
    subtitle: String,
    description: String,
    tags: List<String>,
    icon: ImageVector,
    backgroundColor: Color,
    primaryColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(32.dp),
        color = backgroundColor,
        shadowElevation = 0.dp // 扁平化風格
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // 背景裝飾 (浮水印圖示)
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = primaryColor.copy(alpha = 0.08f),
                modifier = Modifier
                    .size(160.dp) // 稍微縮小一點，避免小螢幕遮擋太多
                    .align(Alignment.BottomEnd)
                    .offset(x = 30.dp, y = 30.dp)
                    .rotate(-15f)
            )

            // 內容垂直排列，使用 Arrangement.SpaceBetween 讓內容均勻分布
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween // ★ 關鍵：讓頭尾對齊，撐開版面
            ) {
                // 上半部：標題區
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.size(52.dp),
                            shadowElevation = 2.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(16.dp))

                        Column {
                            Text(
                                text = title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF333333)
                            )
                            Text(
                                text = subtitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = primaryColor
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = description,
                        fontSize = 14.sp,
                        color = Color(0xFF555555),
                        lineHeight = 20.sp,
                        maxLines = 3, // 限制行數避免溢出
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                // 下半部：標籤 + 按鈕
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 標籤群組
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tags.forEach { tag ->
                            Surface(
                                color = primaryColor.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = tag,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor
                                )
                            }
                        }
                    }

                    // 開始按鈕
                    Surface(
                        color = primaryColor,
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp),
                        shadowElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Go",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}