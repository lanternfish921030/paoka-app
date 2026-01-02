@file:Suppress("UnsafeOptInUsageError")
@file:OptIn(androidx.media3.common.util.UnstableApi::class)




package com.example.a0725.screens




import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.a0725.R
import com.example.a0725.navigation.Routes
import com.example.a0725.navigation.navigateSingleTopTo
import com.example.a0725.repository.AngleFrame
import com.example.a0725.repository.AnglesData
import com.example.a0725.repository.UserVideoEntry
import com.example.a0725.repository.VideoRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.saveable.rememberSaveable
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkeletonScreen(nav: NavHostController) {
    val context = LocalContext.current
    val repo = remember { VideoRepository() }
    val scope = rememberCoroutineScope()




    // 🔹 個人頭像
    val profileVm: com.example.a0725.model.ProfileViewModel = viewModel()
    val photoUrl by profileVm.photoUrl.collectAsStateWithLifecycle()




    // 🔹 從 Firestore 讀出的影片清單 (改用監聽模式)
    val videoItems = remember { mutableStateListOf<UserVideoEntry>() }
    // 🔹 目前選中的影片
    var selectedVideo by remember { mutableStateOf<UserVideoEntry?>(null) }




    // 用來控制監聽器的生命週期
    DisposableEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) return@DisposableEffect onDispose { }




        // 🔥 關鍵修正：改用 observeUserVideos (監聽) 而不是 listSkeletonVideos (只讀一次)
        // 這樣後端處理完，列表會自動跳一下，縮圖就會跑出來
        val registration = repo.observeUserVideos(uid) { videos ->
            // 過濾掉 gait 的影片 (如果你需要區分的話)
            val skeletonVideos = videos.filter {
                // 簡單判斷：假設 originalStoragePath 不包含 "gait"
                !it.originalStoragePath.contains("videos/gait")
            }




            videoItems.clear()
            videoItems.addAll(skeletonVideos)




            // 如果目前沒有選中影片，預設選第一支
            if (selectedVideo == null) {
                selectedVideo = skeletonVideos.firstOrNull()
            }
        }




        onDispose {
            registration.remove()
        }
    }








    // 伺服器處理後的影片網址 & 角度資料
    var overlayUrl by remember(selectedVideo?.id) { mutableStateOf<String?>(null) }
    var skeletonUrl by remember(selectedVideo?.id) { mutableStateOf<String?>(null) }
    var anglesData by remember(selectedVideo?.id) { mutableStateOf<AnglesData?>(null) }
    var anglesError by remember(selectedVideo?.id) { mutableStateOf<String?>(null) }




    // 刪除模式 & 被勾選的 videoId
    var isEditMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<String>() }




    // 選擇來源 Dialog
    var showPickerSheet by remember { mutableStateOf(false) }




    // 點「骨架分析」虛線框 → 顯示關節角度 Dialog
//    var showAngleDialog by remember { mutableStateOf(false) }
// 或者更穩定的寫法 (即使螢幕旋轉也不會關掉)
    var showAngleDialog by rememberSaveable { mutableStateOf(false) }
    // 原始影片「全螢幕」Dialog 用
    var fullScreenUri by remember { mutableStateOf<Uri?>(null) }




    // 🔹 關節角度彈窗要用的影片 Uri
    var angleDialogVideoUri by remember { mutableStateOf<Uri?>(null) }




    /* ---------------- 2. 處理 Overlay URL (🔥 關鍵修改：優先用 downloadUrl) ---------------- */
    LaunchedEffect(selectedVideo) {
        overlayUrl = null
        val video = selectedVideo ?: return@LaunchedEffect




        // A. 優先使用後端產生的公開連結 (新版)
        if (!video.overlayDownloadUrl.isNullOrBlank()) {
            overlayUrl = video.overlayDownloadUrl
        }
        // B. 如果沒有連結但有路徑 (舊版相容)
        else if (!video.overlayStoragePath.isNullOrBlank()) {
            try {
                overlayUrl = repo.resolveDownloadUrl(video.overlayStoragePath!!)
            } catch (e: Exception) {
                Log.e("SkeletonScreen", "Overlay resolve failed", e)
            }
        }
    }




    /* ---------------- 3. 處理 Skeleton URL (🔥 關鍵修改：優先用 downloadUrl) ---------------- */
    LaunchedEffect(selectedVideo) {
        skeletonUrl = null
        val video = selectedVideo ?: return@LaunchedEffect




        if (!video.skeletonDownloadUrl.isNullOrBlank()) {
            skeletonUrl = video.skeletonDownloadUrl
        } else if (!video.skeletonStoragePath.isNullOrBlank()) {
            try {
                skeletonUrl = repo.resolveDownloadUrl(video.skeletonStoragePath!!)
            } catch (e: Exception) {
                Log.e("SkeletonScreen", "Skeleton resolve failed", e)
            }
        }
    }




    // 🔹 讀取關節角度資料
    LaunchedEffect(selectedVideo?.analysisDocId) {
        anglesData = null
        anglesError = null
        val id = selectedVideo?.analysisDocId
        if (!id.isNullOrBlank()) {
            try {
                anglesData = repo.loadAngles(id)
            } catch (e: Exception) {
                anglesError = e.message
            }
        }
    }




    // 🔹 監聽影片更新 (當後端處理好時，自動刷新)
    val authUid = FirebaseAuth.getInstance().currentUser?.uid
    // ⚠️ 這裡用來暫存監聽器，確保離開畫面時可以移除
    var videoListener by remember { mutableStateOf<ListenerRegistration?>(null) }




    // 🔥 關鍵修正：Key 只用 ID，不要用整個物件，避免無窮迴圈
    val videoId = selectedVideo?.id




    DisposableEffect(videoId, authUid) {
        val vid = videoId
        val uid = authUid




        if (vid != null && uid != null) {
            // 啟動監聽
            videoListener = repo.listenVideo(uid, vid) { updated ->
                if (updated != null) {
                    // ⭐️ 聰明判斷：只有當「網址」或「分析狀態」真的不一樣時，才更新 UI
                    // 這樣可以過濾掉只有時間戳記改變的無效更新
                    val old = selectedVideo




                    val isOverlayChanged = updated.overlayDownloadUrl != old?.overlayDownloadUrl
                    val isSkeletonChanged = updated.skeletonDownloadUrl != old?.skeletonDownloadUrl
                    val isAnalysisChanged = updated.analysisDocId != old?.analysisDocId




                    // 如果原本是 null，或者關鍵資料變了，才更新
                    if (old == null || isOverlayChanged || isSkeletonChanged || isAnalysisChanged) {
                        selectedVideo = updated
                    }
                }
            }
        }




        // 當離開畫面或 ID 改變時，移除舊的監聽器，避免記憶體洩漏
        onDispose {
            videoListener?.remove()
        }
    }




    /* ---------------- 新影片選取流程 ---------------- */
    val onNewVideoPicked: (Uri) -> Unit = { uri ->
        scope.launch {
            try {
                val newId = repo.uploadOriginalAndRegister(
                    context = context,
                    uri = uri,
                    title = "未命名影片"
                )
                val list = repo.listSkeletonVideos()
                videoItems.clear()
                videoItems.addAll(list)
                selectedVideo = list.firstOrNull { it.id == newId } ?: list.firstOrNull()
            } catch (e: Exception) {
                Toast.makeText(context, "上傳影片失敗：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }




    /* ---------------- Launchers ---------------- */
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
        if (grant[Manifest.permission.CAMERA] == true) startCamera()
    }




    val pickFromGallery = rememberLauncherForActivityResult(
        contract = OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) onNewVideoPicked(uri)
    }




    // 如果有任何 Dialog 開啟，主畫面的影片就應該暫停 (釋放資源)
//    val isMainScreenPlaying = !showAngleDialog && !showPickerSheet && fullScreenUri == null




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
                    Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "help")
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




            // 1. 上方「修改 / 取消 刪除」Box
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(20.dp)
                    .constrainAs(modify) {
                        top.linkTo(line3); start.linkTo(parent.start); end.linkTo(parent.end)
                    }
            ) {
                if (!isEditMode) {
                    Text(
                        "修改",
                        color = Color(0xFFF3CE5A),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .clickable {
                                isEditMode = true
                                selectedIds.clear()
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
                            modifier = Modifier.clickable {
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
                                        val list = repo.listSkeletonVideos()
                                        videoItems.clear()
                                        videoItems.addAll(list)
                                        if (selectedVideo != null && toDelete.contains(selectedVideo!!.id)) {
                                            selectedVideo = list.firstOrNull()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "刪除失敗：${e.message}", Toast.LENGTH_SHORT).show()
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




            // 2. 上方影片清單（VideoRow）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .constrainAs(row) {
                        top.linkTo(line5); start.linkTo(parent.start); end.linkTo(parent.end)
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




            // 3. 下方三個區塊：原始影片 / 骨架分析 / 分離畫面
            Column(
                modifier = Modifier
                    .constrainAs(body) {
                        top.linkTo(line7); start.linkTo(parent.start); end.linkTo(parent.end)
                    }
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val playbackUri: Uri? = selectedVideo?.originalDownloadUrl?.let { Uri.parse(it) }
                val overlayUri: Uri? = overlayUrl?.let { Uri.parse(it) }
                val skeletonUri: Uri? = skeletonUrl?.let { Uri.parse(it) }




                // --- 區塊 1: 原始影片 ---
                SectionTitle("原始影片")
                Spacer(Modifier.height(8.dp))
                MediaBox(
                    selected = playbackUri,
                    onClick = null // 點擊由內部 Box 處理
                ) { uri ->
                    Box(Modifier.fillMaxSize()) {
                        SimpleVideoBox(
                            uri = uri,
                            modifier = Modifier.fillMaxSize(),
                            showController = true,
                            isPlaying = false // 建議主畫面影片預設不自動播放，由使用者手動點擊
                            // 或者如果你希望它自動播，但彈窗時暫停，請用：
                            //isPlaying = isMainScreenPlaying
                            // (注意：這會導致每次 state 變更都觸發播放/暫停，若要簡單穩定，建議設為 false)
                        )
                        IconButton(
                            onClick = { fullScreenUri = uri },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                        ) {
                            Icon(Icons.Filled.Fullscreen, "全螢幕播放", tint = Color.White)
                        }
                    }
                }




                // --- 區塊 2: 骨架分析（右邊放「關節角度」按鈕） ---
                SectionTitleWithAction(
                    title = "骨架分析",
                    actionText = "關節角度",
                    onActionClick = {
                        when {
                            selectedVideo == null -> Toast.makeText(context, "請先選擇影片", Toast.LENGTH_SHORT).show()
                            anglesData == null -> Toast.makeText(context, "分析資料尚未準備好", Toast.LENGTH_SHORT).show()
                            else -> {
                                val baseUri = overlayUri ?: playbackUri
                                if (baseUri == null) {
                                    Toast.makeText(context, "無法取得影片", Toast.LENGTH_SHORT).show()
                                } else {
                                    angleDialogVideoUri = baseUri
                                    showAngleDialog = true
                                }
                            }
                        }
                    }
                )




                // --- 區塊 2: 骨架分析 ---
                Spacer(Modifier.height(8.dp))
                MediaBox(
                    selected = overlayUri,
                    onClick = null
                ) { uri ->
                    Box(Modifier.fillMaxSize()) {
                        SimpleVideoBox(
                            uri = uri,
                            modifier = Modifier.fillMaxSize(),
                            showController = true, // 建議開啟控制器讓使用者能自己按播放
                            isPlaying = false      // 🔥 修改這裡：預設不自動播放，節省資源
                            // 或者使用變數控制： isPlaying = isMainScreenPlaying
                        )
                        IconButton(
                            onClick = { fullScreenUri = uri },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                        ) {
                            Icon(
                                Icons.Filled.Fullscreen,
                                contentDescription = "全螢幕播放",
                                tint = Color.White
                            )
                        }
                    }
                }




                // --- 區塊 3: 分離畫面 ---
                SectionTitleWithAction(
                    title = "分離畫面",
                    actionText = "關節角度",
                    onActionClick = {
                        when {
                            selectedVideo == null -> Toast.makeText(context, "請先選擇影片", Toast.LENGTH_SHORT).show()
                            anglesData == null -> Toast.makeText(context, "分析資料尚未準備好", Toast.LENGTH_SHORT).show()
                            else -> {
                                // 🔥 關鍵修改：這裡指定 Dialog 要播放 skeletonUri (純黑底骨架影片)
                                if (skeletonUri != null) {
                                    angleDialogVideoUri = skeletonUri
                                    showAngleDialog = true
                                } else {
                                    Toast.makeText(context, "無法取得骨架影片", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
                MediaBox(
                    selected = skeletonUri,
                    onClick = null
                ) { uri ->
                    Box(Modifier.fillMaxSize()) {
                        SimpleVideoBox(
                            uri = uri,
                            modifier = Modifier.fillMaxSize(),
                            showController = true, // 建議開啟控制器
                            isPlaying = false      // 🔥 修改這裡：預設不自動播放
                        )
                        IconButton(
                            onClick = { fullScreenUri = uri },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                        ) {
                            Icon(
                                Icons.Filled.Fullscreen,
                                contentDescription = "全螢幕播放",
                                tint = Color.White
                            )
                        }
                    }
                }
                Spacer(Modifier.height(130.dp))
            }
        }




        // 在 SkeletonScreen Composable 裡面
        val scope = rememberCoroutineScope()
        val sheetState = rememberModalBottomSheetState()


// 替換原本的 if (showPickerSheet) 區塊
        if (showPickerSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPickerSheet = false },
                sheetState = sheetState,
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    // 1. 標題
                    Text(
                        text = "選擇影片來源",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF333333)
                    )


                    Divider(color = Color(0xFFF0F0F0))


                    // 2. 選項 A: 從相簿
                    ListItem(
                        headlineContent = { Text("從相簿選擇影片") },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Outlined.PhotoLibrary,
                                contentDescription = null,
                                tint = Color(0xFFFFB74D)
                            )
                        },
                        modifier = Modifier.clickable {
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
                                tint = Color(0xFFFFB74D)
                            )
                        },
                        modifier = Modifier.clickable {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) showPickerSheet = false
                                // ★★★ 這裡填入您原本的錄影邏輯 ★★★
                                val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.CAMERA
                                )
                                if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    // startCamera() 或是您的相機啟動程式碼
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




        /* 5. 關節角度 Dialog */
        if (showAngleDialog) {
            AngleDialog(
                videoUri = angleDialogVideoUri,
                anglesData = anglesData,
                onDismiss = { showAngleDialog = false }
            )
        }




        /* 6. 全螢幕 Dialog */
        if (fullScreenUri != null) {
            Dialog(
                onDismissRequest = { fullScreenUri = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    SimpleVideoBox(
                        uri = fullScreenUri!!,
                        modifier = Modifier.fillMaxSize(),
                        isPlaying = true // 🔥 全螢幕時自動播放
                    )
                    IconButton(
                        onClick = { fullScreenUri = null },
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                    ) {
                        Icon(Icons.Filled.Close, "關閉", tint = Color.White)
                    }
                }
            }
        }
    }
}




/* ---------------- 小元件 & Helper Composables ---------------- */




@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black
    )
}




@Composable
private fun SectionTitleWithAction(title: String, actionText: String, onActionClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.weight(1f))
        Text(
            actionText, color = Color(0xFFF3CE5A), fontSize = 14.sp, fontWeight = FontWeight.Bold,
            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
            modifier = Modifier.clickable { onActionClick() }
        )
    }
}




@Composable
fun MediaBox(selected: Uri?, onClick: (() -> Unit)? = null, content: @Composable (Uri) -> Unit) {
    Box(
        modifier = Modifier
            .size(300.dp, 200.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .dashedBorder(Color.Gray, 20f, 3f, floatArrayOf(12f, 12f))
        // 移除這裡的 clickable，改在下面處理
    ) {
        if (selected != null) {
            content(selected)
        }




        // 🔥 關鍵修正：在最上層蓋一個透明 Box 負責處理點擊
        // 這樣絕對不會被下面的影片擋住點擊事件
        if (onClick != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onClick() }
            )
        }
    }
}




@Composable
fun VideoRow(
    videos: List<UserVideoEntry>, isEditMode: Boolean, selectedIds: List<String>,
    onToggleSelect: (String) -> Unit, onAddClick: () -> Unit, onItemClick: (UserVideoEntry) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().height(90.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
    ) {
        item { UploadAddCard(onClick = onAddClick) }
        items(videos.size) { i ->
            val video = videos[i]
            val selected = selectedIds.contains(video.id)
            VideoItemCard(
                entry = video, isEditMode = isEditMode, isSelected = selected,
                onClick = { if (isEditMode) onToggleSelect(video.id) else onItemClick(video) }
            )
        }
    }
}




@Composable
private fun VideoItemCard(entry: UserVideoEntry, isEditMode: Boolean, isSelected: Boolean, onClick: () -> Unit) {
    var thumb by remember(entry.originalDownloadUrl) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(entry.originalDownloadUrl) {
        val bmp: Bitmap? = withContext(Dispatchers.IO) {
            runCatching {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(entry.originalDownloadUrl, HashMap())
                val frame = retriever.getFrameAtTime(0)
                retriever.release()
                frame
            }.getOrNull()
        }
        if (bmp != null) thumb = bmp
    }




    Box(
        modifier = Modifier.size(135.dp, 90.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFFFF4D6))
            .clickable { onClick() }
    ) {
        if (thumb != null) {
            Image(bitmap = thumb!!.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text("載入中…", fontSize = 12.sp, modifier = Modifier.align(Alignment.Center))
        }
        if (isEditMode) {
            Box(
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(20.dp).clip(CircleShape)
                    .background(if (isSelected) Color(0xFF3D8AFF) else Color.White)
                    .border(1.dp, if (isSelected) Color(0xFF3D8AFF) else Color(0xFFCCCCCC), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) Text("✓", fontSize = 12.sp, color = Color.White)
            }
        }
    }
}




fun Modifier.dashedBorder(color: Color, shapeRadiusDp: Float, strokeWidth: Float, intervals: FloatArray) = this.drawBehind {
    val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, pathEffect = PathEffect.dashPathEffect(intervals))
    val r = shapeRadiusDp.dp.toPx()
    drawRoundRect(color = color, style = stroke, cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r))
}




@Composable
private fun UploadAddCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(50.dp, 100.dp).clip(RoundedCornerShape(12.dp)).background(Color(248, 225, 161)).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text("+", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(62, 50, 42))
    }
}




/* ---------------- 關節角度相關元件 ---------------- */




@Composable
private fun AngleSheetContent(
    hasData: Boolean, shoulderL: Int?, shoulderR: Int?,
    elbowL: Int?, elbowR: Int?, hipL: Int?, hipR: Int?, kneeL: Int?, kneeR: Int?,
) {
    fun fmt(v: Int?): String = if (v == null) "--°" else "${v}°"




    Column(Modifier.fillMaxWidth()) {
        Text("關節角度", style = MaterialTheme.typography.titleLarge) // 改標題提示
        Spacer(Modifier.height(12.dp))




//        if (!hasData) {
//            Text("尚未完成分析或資料尚未同步。", color = Color(0xFF6B7280))
//            Spacer(Modifier.height(8.dp))
//        }




        // 🔥 如果你覺得左右相反，就在這裡把參數對調顯示即可
        // 例如：顯示 label "左 (畫面左側)" 但傳入 right 的數據
        // 但最標準的做法是保持原樣，並理解這是「受試者」的左右




        // 這裡維持標準定義：Left = 受試者的左手 (畫面右邊)
        AngleRow("肩部", left = fmt(shoulderL), right = fmt(shoulderR))
        AngleRow("肘部", left = fmt(elbowL), right = fmt(elbowR))
        AngleRow("髖部", left = fmt(hipL), right = fmt(hipR))
        AngleRow("膝部", left = fmt(kneeL), right = fmt(kneeR))




        Spacer(Modifier.height(8.dp))
    }
}




@Composable
private fun AngleRow(label: String, left: String, right: String) {
    Text(label, color = Color(0xFF6B7280))
    Card(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("左 $left", color = Color(0xFFFF8C00))
            Text("|", color = Color(0xFFCBD5E1))
            Text("右 $right", color = Color(0xFF0000FF))
        }
    }
}




@Composable
private fun AngleDialog(videoUri: Uri?, anglesData: AnglesData?, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f) // 寬度維持 95%
                .wrapContentHeight() // 高度自動，內容多少就多高
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
        ) {
            // 控制全螢幕狀態
            var showFullScreen by remember { mutableStateOf(false) }
            // 記錄影片播放進度
            var positionMs by remember { mutableStateOf(0L) }


            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("骨架分析與關節角度", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))


                // 1. 影片區塊
                if (videoUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                    ) {
                        SimpleVideoBox(
                            uri = videoUri,
                            modifier = Modifier.fillMaxSize(),
                            showController = true,
                            isPlaying = true,
                            onPositionChanged = { pos -> positionMs = pos }
                        )


                        // 全螢幕按鈕
                        IconButton(
                            onClick = { showFullScreen = true },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                        ) {
                            Icon(
                                Icons.Filled.Fullscreen,
                                contentDescription = "全螢幕播放",
                                tint = Color.White
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("無影片預覽", color = Color.White)
                    }
                }


                Spacer(Modifier.height(12.dp))


                // ================= 修改開始：防止數據閃爍的邏輯 =================


                // 1. 計算「當下這一刻」應有的 Frame (可能會是 null)
                val currentFrame = remember(positionMs, anglesData) {
                    val data = anglesData
                    if (data == null || data.frames.isEmpty()) null
                    else {
                        val step = data.stepMs
                        val idx = ((positionMs / step).toInt()).coerceIn(0, data.frames.size - 1)
                        data.frames.getOrNull(idx)
                    }
                }


                // 2. 緩存機制：記住「最後一次有效」的 Frame
                var cachedFrame by remember { mutableStateOf<AngleFrame?>(null) }


                // 如果當下算出來有東西，就更新緩存
                if (currentFrame != null) {
                    cachedFrame = currentFrame
                }


                // 3. 決定最終要顯示什麼：
                // 優先用當下的 -> 沒有的話用緩存的 -> 再沒有(剛開啟)就拿列表第一張 -> 真的都沒有才 null
                val frameDisplay = currentFrame ?: cachedFrame ?: anglesData?.frames?.firstOrNull()


                // ================= 修改結束 =================


                // 3. 表格區塊
                AngleSheetContent(
                    hasData = frameDisplay != null, // 只要找到任何一張圖，就不會顯示 --
                    shoulderL = frameDisplay?.shoulderL, shoulderR = frameDisplay?.shoulderR,
                    elbowL = frameDisplay?.elbowL, elbowR = frameDisplay?.elbowR,
                    hipL = frameDisplay?.hipL, hipR = frameDisplay?.hipR,
                    kneeL = frameDisplay?.kneeL, kneeR = frameDisplay?.kneeR
                )


                Spacer(Modifier.height(8.dp))


                // 底部按鈕
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("關閉") }
                }
            }


            // 全螢幕處理邏輯
            if (showFullScreen && videoUri != null) {
                Dialog(
                    onDismissRequest = { showFullScreen = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black)
                    ) {
                        SimpleVideoBox(
                            uri = videoUri,
                            modifier = Modifier.fillMaxSize(),
                            showController = true
                        )
                        IconButton(
                            onClick = { showFullScreen = false },
                            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "關閉", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}



