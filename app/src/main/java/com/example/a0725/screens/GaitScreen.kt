@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)




package com.example.a0725.screens




import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.a0725.R
import com.example.a0725.navigation.Routes
import com.example.a0725.navigation.navigateSingleTopTo
import com.example.a0725.repository.UserVideoEntry
import com.example.a0725.repository.VideoRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import com.example.a0725.repository.AnglesData
import com.example.a0725.repository.AngleFrame
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*


import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ListItem
import androidx.compose.material3.Divider
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun GaitScreen(nav: NavHostController) {




    val context = LocalContext.current
    val repo = remember { VideoRepository() }
    val scope = rememberCoroutineScope()




    // 🔹 個人頭像（跟其他頁面同一套）
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




    // 🔹 Firebase 影片清單 & 選取狀態
    val videoItems = remember { mutableStateListOf<UserVideoEntry>() }
    var selectedVideo by remember { mutableStateOf<UserVideoEntry?>(null) }


    // 🔹 角度 + 步態事件（從 video_analyses 讀）
    var anglesData by remember { mutableStateOf<AnglesData?>(null) }
    var anglesError by remember { mutableStateOf<String?>(null) }


    var isEditMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<String>() }




    // 選取來源 Dialog（跟 Skeleton 一樣）
    var showPickerSheet by remember { mutableStateOf(false) }




    // Gait: 哪個階段開角度 Dialog
    var activePhase by remember { mutableStateOf<GaitPhase?>(null) }




    // 原始影片全螢幕
    var fullScreenUri by remember { mutableStateOf<Uri?>(null) }




    /* ---------------- 進畫面先讀 Firestore 影片清單 ---------------- */
    LaunchedEffect(Unit) {
        try {
            val list = repo.listGaitVideos()
            videoItems.clear()
            videoItems.addAll(list)
            selectedVideo = list.firstOrNull()
        } catch (e: Exception) {
            Toast.makeText(context, "讀取影片失敗：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    // 當選中的影片變化，且有 analysisDocId，就讀取 video_analyses
    LaunchedEffect(selectedVideo?.analysisDocId) {
        val analysisId = selectedVideo?.analysisDocId
        if (analysisId.isNullOrEmpty()) {
            anglesData = null
            anglesError = null
            return@LaunchedEffect
        }
        try {
            anglesError = null
            anglesData = repo.loadAngles(analysisId)
        } catch (e: Exception) {
            anglesData = null
            anglesError = e.message
        }
    }


    /* ---------------- 新影片選取流程（跟 Skeleton 一樣） ---------------- */




    val onNewVideoPicked: (Uri) -> Unit = { uri ->
        scope.launch {
            try {
                val newId = repo.uploadOriginalAndRegisterForGait(
                    context = context,
                    uri = uri,
                    title = "跑步影片"
                )


                val list = repo.listGaitVideos()
                videoItems.clear()
                videoItems.addAll(list)
                selectedVideo = list.firstOrNull { it.id == newId } ?: list.firstOrNull()




            } catch (e: Exception) {
                Toast.makeText(context, "上傳影片失敗：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }




    // 錄影
    val recordVideoLauncher = rememberLauncherForActivityResult(
        contract = StartActivityForResult()
    ) { res ->
        val uri = res.data?.data
        if (uri != null) onNewVideoPicked(uri)
    }




    val startCamera: () -> Unit = {
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_DURATION_LIMIT, 30)
            putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
        }
        recordVideoLauncher.launch(intent)
    }




    val requestCameraPerms = rememberLauncherForActivityResult(
        contract = RequestMultiplePermissions()
    ) { grant ->
        val ok = grant[Manifest.permission.CAMERA] == true
        if (ok) startCamera()
    }




    // 從檔案管理 / 相簿選影片
    val pickFromGallery = rememberLauncherForActivityResult(
        contract = OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) onNewVideoPicked(uri)
    }




    /* ---------------- UI Scaffold ---------------- */




    Scaffold(
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
                actions = {
                    IconButton(onClick = { /* 說明 */ }) {
                        Icon(
                            imageVector = Icons.Filled.Help,
                            contentDescription = "Help"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->




        ConstraintLayout(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            val (modify, row, body) = createRefs()
            val line3 = createGuidelineFromTop(0f)
            val line5 = createGuidelineFromTop(0.055f)
            val line7 = createGuidelineFromTop(0.22f)




            // 上方「修改 / 取消 刪除」
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(20.dp)
                    .constrainAs(modify) {
                        top.linkTo(line3)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
            ) {
                if (!isEditMode) {
                    Text(
                        "修改",
                        color = Color(0xFFF3CE5A),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .clickable {
                                if (videoItems.isNotEmpty()) {
                                    isEditMode = true
                                    selectedIds.clear()
                                }
                            }
                    )
                } else {
                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "取消",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clickable {
                                    isEditMode = false
                                    selectedIds.clear()
                                }
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "刪除",
                            color = Color(0xFFFF5555),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                val toDelete = selectedIds.toList()
                                if (toDelete.isEmpty()) {
                                    isEditMode = false
                                    return@clickable
                                }




                                scope.launch {
                                    try {
                                        repo.deleteVideos(toDelete)
                                        val list = repo.listGaitVideos()
                                        videoItems.clear()
                                        videoItems.addAll(list)




                                        if (selectedVideo != null &&
                                            toDelete.contains(selectedVideo!!.id)
                                        ) {
                                            selectedVideo = list.firstOrNull()
                                        }




                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            "刪除失敗：${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } finally {
                                        selectedIds.clear()
                                        isEditMode = false
                                    }
                                }
                            }
                        )
                    }
                }
            }




            // ① 上方影片列：直接用 Skeleton 的 VideoRow（縮圖也一樣）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .constrainAs(row) {
                        top.linkTo(line5)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
            ) {
                VideoRow(
                    videos = videoItems,
                    isEditMode = isEditMode,
                    selectedIds = selectedIds,
                    onToggleSelect = { id ->
                        if (selectedIds.contains(id)) selectedIds.remove(id)
                        else selectedIds.add(id)
                    },
                    onAddClick = { showPickerSheet = true },
                    onItemClick = { video ->
                        if (!isEditMode) selectedVideo = video
                    }
                )
            }




            // ② 下方內容：原始影片 + 三個步態階段
            Column(
                modifier = Modifier
                    .constrainAs(body) {
                        top.linkTo(line7)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val playbackUri: Uri? =
                    selectedVideo?.originalDownloadUrl?.let { Uri.parse(it) }


                // 給 PhaseBox 用：每個階段自己的 frames 清單
                val icFrames = remember(anglesData) {
                    anglesData?.frames?.filter { it.phase == "IC" } ?: emptyList()
                }
                val msFrames = remember(anglesData) {
                    anglesData?.frames?.filter { it.phase == "MS" } ?: emptyList()
                }
                val toFrames = remember(anglesData) {
                    anglesData?.frames?.filter { it.phase == "TO" } ?: emptyList()
                }


                // 原始影片
                SectionTitle("原始影片")
                Spacer(Modifier.height(8.dp))
                MediaBox(
                    selected = playbackUri,
                    onClick = null
                ) { uri ->
                    Box(Modifier.fillMaxSize()) {
                        SimpleVideoBox(
                            uri = uri,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { fullScreenUri = uri },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Fullscreen,
                                contentDescription = "全螢幕播放",
                                tint = Color.White
                            )
                        }
                    }
                }




                // 著地
                Spacer(Modifier.height(16.dp))
                SectionChip(title = "著地 Initial Contact")
                Spacer(Modifier.height(12.dp))
                PhaseBox(
                    frames = icFrames,
                    onClick = {
                        when {
                            selectedVideo == null -> {
                                Toast.makeText(
                                    context,
                                    "請先從上方選擇一支影片或新增影片",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            selectedVideo?.analysisDocId.isNullOrEmpty() -> {
                                Toast.makeText(
                                    context,
                                    "這支影片尚未分析完成，請稍後再試",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            else -> {
                                activePhase = GaitPhase.INITIAL_CONTACT
                            }
                        }
                    }
                )


                // 中間站立
                Spacer(Modifier.height(16.dp))
                SectionChip(title = "中間站立 Mid Stance")
                Spacer(Modifier.height(12.dp))
                PhaseBox(
                    frames = msFrames,
                    onClick = {
                        when {
                            selectedVideo == null -> {
                                Toast.makeText(
                                    context,
                                    "請先從上方選擇一支影片或新增影片",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            selectedVideo?.analysisDocId.isNullOrEmpty() -> {
                                Toast.makeText(
                                    context,
                                    "這支影片尚未分析完成，請稍後再試",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            else -> {
                                activePhase = GaitPhase.MID_STANCE
                            }
                        }
                    }
                )


                // 離地
                Spacer(Modifier.height(16.dp))
                SectionChip(title = "離地 Toe Off")
                Spacer(Modifier.height(12.dp))
                PhaseBox(
                    frames = toFrames,
                    onClick = {
                        when {
                            selectedVideo == null -> {
                                Toast.makeText(
                                    context,
                                    "請先從上方選擇一支影片或新增影片",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            selectedVideo?.analysisDocId.isNullOrEmpty() -> {
                                Toast.makeText(
                                    context,
                                    "這支影片尚未分析完成，請稍後再試",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            else -> {
                                activePhase = GaitPhase.TOE_OFF
                            }
                        }
                    }
                )
                Spacer(Modifier.height(130.dp))
            }
        }




        // 在 GaitScreen Composable 裡面，請確保有這行 (通常在上面就有了)
        val scope = rememberCoroutineScope()
// 加入這個 state 來控制 sheet 的展開/收合動畫
        val sheetState = rememberModalBottomSheetState()


// 替換原本的 if (showPickerSheet) 區塊
        if (showPickerSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPickerSheet = false },
                sheetState = sheetState,
                containerColor = Color.White, // 背景色
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp) // 頂部圓角
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp) // 底部留白
                ) {
                    // 1. 標題
                    Text(
                        text = "選擇影片來源", // 改掉原本的「更換頭像」
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF333333)
                    )


                    Divider(color = Color(0xFFF0F0F0)) // 細分隔線


                    // 2. 選項 A: 從相簿
                    ListItem(
                        headlineContent = { Text("從相簿選擇影片") },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Outlined.PhotoLibrary,
                                contentDescription = null,
                                tint = Color(0xFFFFB74D) // 使用主題黃色
                            )
                        },
                        modifier = Modifier.clickable {
                            // 點擊後先收合 sheet，動畫結束再執行動作
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) showPickerSheet = false
                                pickFromGallery.launch(arrayOf("video/*"))
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.White)
                    )


                    // 3. 選項 B: 錄影
                    ListItem(
                        headlineContent = { Text("錄製新影片") },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Outlined.Videocam,
                                contentDescription = null,
                                tint = Color(0xFFFFB74D) // 使用主題黃色
                            )
                        },
                        modifier = Modifier.clickable {
                            // 點擊後先收合 sheet
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) showPickerSheet = false
                                // 執行錄影權限檢查與啟動邏輯
                                val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.CAMERA
                                )
                                if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    // ★★★ 如果這裡報錯，請換回您原本啟動相機的程式碼 ★★★
                                    // 例如: startCamera() 或 cameraLauncher.launch()
                                } else {
                                    requestCameraPerms.launch(arrayOf(Manifest.permission.CAMERA))
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.White)
                    )


                    Divider(color = Color(0xFFF0F0F0))
                    Spacer(modifier = Modifier.height(8.dp))


                    // 4. 取消按鈕
                    TextButton(
                        onClick = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) showPickerSheet = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text("取消", fontSize = 16.sp, color = Color.Gray)
                    }
                }
            }
        }




        /* ④ 關節角度 Dialog（Gait 三階段共用） */
        if (activePhase != null && selectedVideo != null) {
            AngleDialogForPhase(
                phase = activePhase!!,
                angles = anglesData,
                onDismiss = { activePhase = null }
            )
        }




        /* ⑤ 原始影片「全螢幕」Dialog（跟 Skeleton 一樣風格） */
        if (fullScreenUri != null) {
            Dialog(
                onDismissRequest = { fullScreenUri = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    SimpleVideoBox(
                        uri = fullScreenUri!!,
                        modifier = Modifier.fillMaxSize()
                    )




                    IconButton(
                        onClick = { fullScreenUri = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "關閉",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}




/* ----------------------- 資料模型（示意） ----------------------- */
private enum class GaitPhase { INITIAL_CONTACT, MID_STANCE, TOE_OFF }
/* ----------------------- Gait 專用角度 Dialog ----------------------- */
@Composable
private fun AngleDialogForPhase(
    phase: GaitPhase,
    angles: AnglesData?,
    onDismiss: () -> Unit,
) {
    val phaseKey = when (phase) {
        GaitPhase.INITIAL_CONTACT -> "IC"
        GaitPhase.MID_STANCE -> "MS"
        GaitPhase.TOE_OFF -> "TO"
    }


    // 從所有 frames 篩出對應這個 phase 的幀
    val framesForPhase = remember(phase, angles) {
        val all = angles?.frames ?: emptyList()
        all.filter { it.phase == phaseKey }
    }


    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .width(320.dp)
                    // 1. 修改高度：改用 fillMaxHeight 讓它變長 (例如佔螢幕 85%)
                    .fillMaxHeight(0.85f)
            ) {


                val phaseTitle = when (phase) {
                    GaitPhase.INITIAL_CONTACT -> "著地 Initial Contact"
                    GaitPhase.MID_STANCE -> "中間站立 Mid Stance"
                    GaitPhase.TOE_OFF -> "離地 Toe Off"
                }


                // --- 錯誤處理區塊 (保持不變) ---
                if (angles == null) {
                    Text(
                        text = "尚未取得分析資料，請稍後再試",
                        color = Color.DarkGray,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(phaseTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    return@Column
                }


                if (framesForPhase.isEmpty()) {
                    Text(
                        text = "目前沒有此階段（$phaseTitle）的偵測結果",
                        color = Color.DarkGray,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "請確認走路 / 跑步動作有包含完整步態。",
                        color = Color(0xFF6B7280),
                        fontSize = 12.sp
                    )
                    return@Column
                }


                // --- 資料準備 ---
                var index by remember(phase, angles) { mutableStateOf(0) }
                val safeIndex = index.coerceIn(0, framesForPhase.lastIndex)
                val current = framesForPhase[safeIndex]
                val frameNumber = (current.frameIndex ?: safeIndex) + 1




                // 2. 圖片區塊：放在最外層 Column，不參與下方滾動，因此會固定在頂部
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp) // 稍微加高一點讓圖片清楚
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE5E5E5)),
                    contentAlignment = Alignment.Center
                ) {
                    val imageUrl = current.imagePath


                    if (!imageUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "步態骨架影像",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Text("第 $frameNumber 張（骨架示意）", color = Color.DarkGray)
                    }


                    if (framesForPhase.size > 1) {
                        IconButton(
                            onClick = { if (index > 0) index-- },
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 8.dp)
                                .size(40.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowLeft,
                                    contentDescription = "上一張",
                                    tint = Color.DarkGray,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }


                        IconButton(
                            onClick = { if (index < framesForPhase.lastIndex) index++ },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 8.dp)
                                .size(40.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowRight,
                                    contentDescription = "下一張",
                                    tint = Color.DarkGray,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }


                Spacer(Modifier.height(12.dp))


                // 3. 滾動內容區塊：用 weight(1f) 佔滿剩餘空間，並加上 verticalScroll
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // 關鍵：佔據圖片下方所有剩餘高度
                        .verticalScroll(rememberScrollState()) // 關鍵：只有這裡面會滾動
                ) {
                    Text(phaseTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("關節角度", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))


                    fun Int?.orZero() = this ?: 0


                    // ========= 建議文字產生邏輯 (保持不變) =========
                    fun shoulderAdvice(phaseCode: String, left: Int, right: Int): String {
                        return when (phaseCode) {
                            "MS" -> {
                                if (right <= 80) "肩部擺動大致在理想範圍，維持現在這種自然放鬆的擺臂就很好。"
                                else "右肩擺動有一點偏大，可以之後試著讓手臂更靠近身體、讓擺臂再柔和一點。"
                            }
                            else -> "這個階段肩膀只要保持左右大致對稱、自然擺動即可，不需要刻意用力抬高。"
                        }
                    }


                    fun elbowAdvice(phaseCode: String, left: Int, right: Int): String {
                        return when (phaseCode) {
                            "MS" -> {
                                if (right <= 90) "肘部彎曲度看起來還不錯，雙側差不多就很好。"
                                else "右側手肘有一點打得比較開，可以嘗試讓手臂彎曲放鬆一些，讓動作更省力。"
                            }
                            else -> "肘部只要跟肩膀一起自然前後擺動，避免僵硬或晃動太大即可。"
                        }
                    }


                    fun hipAdvice(phaseCode: String, left: Int, right: Int): String {
                        return when (phaseCode) {
                            "IC" -> {
                                if (right <= 60) "著地瞬間髖關節控制得不錯，步伐看起來算是穩定。"
                                else "著地時右髖活動有一點偏大，可以之後試著把跨步略為縮小，讓上半身更穩。"
                            }
                            "MS" -> {
                                if (right <= 20) "中間站立時髖部的穩定度還可以，支撐腳看起來蠻穩的。"
                                else "中間站立時右髖晃動稍微多一點，可以練習讓軀幹保持穩定、步伐不要太大。"
                            }
                            "TO" -> "離地階段目前不特別用角度去評分髖關節，先以整體步伐的流暢度為主即可。"
                            else -> "髖部只要避免明顯左右晃動過大，整體穩定就算不錯。"
                        }
                    }


                    fun kneeAdvice(phaseCode: String, left: Int, right: Int): String {
                        return "目前膝關節在這個分析裡先不做數值上的評分，只要在動作過程中不感到不舒服，避免完全鎖死或過度彎曲就可以。"
                    }


                    val shoulderAdviceText = shoulderAdvice(phaseKey, current.shoulderL.orZero(), current.shoulderR.orZero())
                    val elbowAdviceText = elbowAdvice(phaseKey, current.elbowL.orZero(), current.elbowR.orZero())
                    val hipAdviceText = hipAdvice(phaseKey, current.hipL.orZero(), current.hipR.orZero())
                    val kneeAdviceText = kneeAdvice(phaseKey, current.kneeL.orZero(), current.kneeR.orZero())


                    // ========= 卡片顯示 =========
                    JointAngleCard(
                        label = "肩部",
                        left = current.shoulderL.orZero(),
                        right = current.shoulderR.orZero(),
                        advice = shoulderAdviceText
                    )
                    JointAngleCard(
                        label = "肘部",
                        left = current.elbowL.orZero(),
                        right = current.elbowR.orZero(),
                        advice = elbowAdviceText
                    )
                    JointAngleCard(
                        label = "髖部",
                        left = current.hipL.orZero(),
                        right = current.hipR.orZero(),
                        advice = hipAdviceText
                    )
                    JointAngleCard(
                        label = "膝部",
                        left = current.kneeL.orZero(),
                        right = current.kneeR.orZero(),
                        advice = kneeAdviceText
                    )


                    Spacer(Modifier.height(16.dp)) // 底部留白
                }
            }
        }
    }
}


@Composable
private fun JointAngleCard(
    label: String,
    left: Int,
    right: Int,
    advice: String
) {
    Text(label, color = Color.Black, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF3F4F6))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("左  ${left}°", color = Color(0xFF16A34A))
                Text("右  ${right}°", color = Color(0xFFEF4444))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                advice,
                fontSize = 12.sp,
                color = Color(0xFF6B7280)
            )
        }
    }
}




/* ----------------------- 小元件 ----------------------- */




@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Black
    )
}




@Composable
private fun PhaseBox(
    frames: List<AngleFrame>,
    onClick: () -> Unit
) {
    // 這個 state 只存在這一個 PhaseBox 裡
    var index by remember(frames) { mutableStateOf(0) }
    val safeIndex = if (frames.isNotEmpty()) {
        index.coerceIn(0, frames.lastIndex)
    } else 0
    val current = frames.getOrNull(safeIndex)
    val imageUrl = current?.imagePath


    Box(
        modifier = Modifier
            .size(300.dp, 200.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .dashedBorder(
                color = Color.Gray,
                shapeRadiusDp = 20f,
                strokeWidth = 3f,
                intervals = floatArrayOf(12f, 12f)
            )
            .clickable { onClick() },   // 點框框 → 開彈窗
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "步態預覽影像",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = "點此查看分析結果",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }


        // 有多張圖才顯示左右切換按鈕
        if (frames.size > 1) {
            IconButton(
                onClick = {
                    if (index > 0) index--
                },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
                    .size(40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowLeft,
                        contentDescription = "上一張",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }


            IconButton(
                onClick = {
                    if (index < frames.lastIndex) index++
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .size(40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        contentDescription = "下一張",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }


    }
}


@Composable
private fun SectionChip(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFEDEDED))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                title,
                fontSize = 14.sp,
                color = Color(0xFF555555),
                fontWeight = FontWeight.Medium
            )
        }
    }
}



