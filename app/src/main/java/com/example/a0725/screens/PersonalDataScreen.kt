@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.a0725.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.a0725.R
import com.example.a0725.auth.AuthViewModel
import com.example.a0725.model.ProfileViewModel
import com.example.a0725.navigation.Routes
import com.example.a0725.notif.Notif
import com.example.a0725.notif.NotifyGate
import com.example.a0725.notif.cancelEventEveAlarm
import com.example.a0725.repository.ScheduleRepo
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import java.io.File

// 色票定義
private val BgColor = Color(0xFFFAFAFA)       // 極淺灰白背景
private val CardBg = Color.White              // 卡片白
private val HeaderYellow = Color(0xFFFFF9C4)  // 純色淡鵝黃 (不漸層)
private val AccentYellow = Color(0xFFFFB74D)  // 按鈕重點色
private val TextDark = Color(0xFF333333)      // 深色文字
private val TextGray = Color(0xFF757575)      // 次要文字

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun PersonalDataScreen(nav: NavHostController) {
    val profileVm: ProfileViewModel = viewModel()
    val authVm: AuthViewModel = viewModel()
    val context = LocalContext.current

    val userProfile by profileVm.userProfile.collectAsStateWithLifecycle()
    val photoUrl   by profileVm.photoUrl.collectAsStateWithLifecycle()
    val loading    by profileVm.loading.collectAsStateWithLifecycle()
    val error      by profileVm.error.collectAsStateWithLifecycle()
    val pwState    by authVm.changePwState.collectAsStateWithLifecycle()

    var showChangePw by rememberSaveable { mutableStateOf(false) }
    var notifyEnabled by rememberSaveable { mutableStateOf(false) } // UI 預設顯示關閉

    LaunchedEffect(Unit) {
        notifyEnabled = NotifyGate.isEnabled(context)
    }

    var showAvatarSheet by rememberSaveable { mutableStateOf(false) }
    var showCtaaConfirm by rememberSaveable { mutableStateOf(false) }
    var pendingCtaaId by rememberSaveable { mutableStateOf<String?>(null) }

    val storage = FirebaseStorage.getInstance()
    val firestore = Firebase.firestore
    val uid = Firebase.auth.currentUser?.uid

    val askPostNotif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            Notif.ensureChannel(context)
            Toast.makeText(context, "已開啟通知", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "未授權通知，將無法收到提醒", Toast.LENGTH_SHORT).show()
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

    val readImagesPermission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
    fun hasPermission(p: String) = ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED

    val pickFromGallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) uploadAvatar(uri, uid, storage, firestore)
        else Toast.makeText(context, "未選取圖片", Toast.LENGTH_SHORT).show()
    }

    val tempCameraUri = remember { mutableStateOf<Uri?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) tempCameraUri.value?.let { uploadAvatar(it, uid, storage, firestore) }
    }

    val askReadPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pickFromGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        else Toast.makeText(context, "未取得讀取相片權限", Toast.LENGTH_SHORT).show()
    }

    val askCameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = createTempImageUri(context)
            tempCameraUri.value = uri
            takePicture.launch(uri)
        } else {
            Toast.makeText(context, "未取得相機權限", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
        containerColor = BgColor
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {

            // 1. 頂部背景 (純色、高度縮小、無圓角)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp) // ★ 高度縮小，不卡字
                    .background(HeaderYellow) // ★ 純色
            )

            // 2. 主要內容 (Column + Weight)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(), // 避開狀態列
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // --- 標題與頭像區 ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    // 標題 (往下加一點 padding 確保不卡到狀態列)
                    Text(
                        "個人資料",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5D4037),
                        modifier = Modifier.padding(top = 12.dp) // ★ 增加頂部間距
                    )

                    // 頭像 (稍微往上移，讓它壓在黃白交界處)
                    Box(
                        modifier = Modifier.padding(top = 50.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(photoUrl ?: "")
                                .crossfade(true)
                                .build(),
                            contentDescription = "Avatar",
                            placeholder = painterResource(R.drawable.user),
                            error = painterResource(R.drawable.user),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .border(4.dp, Color.White, CircleShape)
                        )
                        // 相機按鈕
                        Surface(
                            shape = CircleShape,
                            color = AccentYellow,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .size(34.dp)
                                .offset(x = 2.dp, y = 2.dp)
                                .clickable { showAvatarSheet = true }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.CameraAlt,
                                    contentDescription = "Edit",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // --- 卡片區 (自動填滿剩餘空間) ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // ★ 佔據所有剩餘空間
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.Top
                ) {

                    // 卡片 1: 帳號資訊
                    SettingsGroupCard(title = "基本資料") {
                        EditableSettingRowV2(
                            label = "名稱",
                            value = userProfile.name,
                            placeholder = "未設定",
                            icon = Icons.Filled.Person,
                            onSave = { profileVm.updateName(it.trim()) }
                        )
                        Divider(color = BgColor, thickness = 1.dp)

                        val currentCtaa = userProfile.ctaaId ?: ""
                        EditableSettingRowV2(
                            label = "系統編號",
                            value = currentCtaa,
                            placeholder = "未填",
                            icon = Icons.Filled.Badge,
                            keyboardType = KeyboardType.Number,
                            onSave = { newValue ->
                                val oldTrim = currentCtaa.trim()
                                val newTrim = newValue.trim()
                                if (newTrim == oldTrim || oldTrim.isBlank()) {
                                    profileVm.updateCtaaId(newTrim.ifBlank { null })
                                } else {
                                    pendingCtaaId = newTrim
                                    showCtaaConfirm = true
                                }
                            }
                        )
                        Divider(color = BgColor, thickness = 1.dp)

                        EditableSettingRowV2(
                            label = "信箱",
                            value = userProfile.email,
                            placeholder = "未綁定",
                            icon = Icons.Filled.Email,
                            keyboardType = KeyboardType.Email,
                            onSave = { profileVm.updateEmail(it.trim()) }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // 卡片 2: 安全設定
                    SettingsGroupCard(title = "設定") {
                        ClickableRowV2(
                            label = "更改密碼",
                            icon = Icons.Filled.Lock,
                            onClick = { showChangePw = true }
                        )
                        Divider(color = BgColor, thickness = 1.dp)

                        PreferenceSwitchRowV2(
                            label = "接收通知",
                            icon = Icons.Filled.Notifications,
                            checked = notifyEnabled,
                            onCheckedChange = { checked ->
                                notifyEnabled = checked
                                NotifyGate.setEnabled(context, checked)
                                if (checked) {
                                    Notif.ensureChannel(context)
                                    if (!NotifyGate.hasPostNotifPermission(context)) {
                                        askPostNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                } else {
                                    NotificationManagerCompat.from(context).cancelAll()
                                    ScheduleRepo.getEventsOnce(
                                        onSuccess = { list -> list.forEach { ev -> try { cancelEventEveAlarm(context, ev) } catch (_: Exception) {} } },
                                        onError = { }
                                    )
                                }
                                uid?.let {
                                    firestore.collection("users").document(it)
                                        .set(mapOf("settings" to mapOf("notifyEnabled" to checked)), SetOptions.merge())
                                }
                            }
                        )
                    }

                    // ★ 使用 Spacer + weight 將登出按鈕推到底部
                    Spacer(Modifier.weight(1f))

                    // ★ 登出按鈕 (改回淡紅色背景)
                    Button(
                        onClick = { nav.navigate(Routes.LOGIN) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFEBEE), // 淡紅色背景
                            contentColor = Color(0xFFD32F2F)    // 紅色文字
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Icon(Icons.Outlined.Logout, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("登出", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(32.dp)) // 底部留白
                }
            }

            // Dialogs & Loading
            if (showAvatarSheet) AvatarActionDialog(onDismiss = { showAvatarSheet = false }, onPickFromGallery = { showAvatarSheet = false; if (hasPermission(readImagesPermission)) pickFromGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) else askReadPermission.launch(readImagesPermission) }, onTakePhoto = { showAvatarSheet = false; if (hasPermission(Manifest.permission.CAMERA)) { val uri = createTempImageUri(context); tempCameraUri.value = uri; takePicture.launch(uri) } else askCameraPermission.launch(Manifest.permission.CAMERA) })
            if (showCtaaConfirm) ConfirmClearScoresDialog(onConfirm = { val target = pendingCtaaId?.trim(); profileVm.updateCtaaIdAndClearHistory(target); showCtaaConfirm = false }, onCancel = { showCtaaConfirm = false; pendingCtaaId = null })
            if (showChangePw) {
                ChangePasswordDialog(loading = pwState.isLoading, error = pwState.error, onDismiss = { authVm.resetChangePwState(); showChangePw = false }, onSave = { c, n -> authVm.changePassword(c, n) })
                if (pwState.success) { LaunchedEffect(Unit) { authVm.resetChangePwState(); showChangePw = false } }
            }
            AnimatedVisibility(visible = loading, modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            error?.let { msg ->
                AnimatedVisibility(visible = true, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                    Surface(color = Color.Red.copy(alpha = 0.9f), shape = RoundedCornerShape(8.dp)) { Text(msg, color = Color.White, modifier = Modifier.padding(16.dp)) }
                }
            }
        }
    }
}

// ───────────── UI 元件 (無變動，保持上次樣式) ─────────────

@Composable
private fun SettingsGroupCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 13.sp,
            color = TextGray,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CardBg,
            shadowElevation = 1.dp,
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
private fun EditableSettingRowV2(
    label: String,
    value: String,
    placeholder: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    onSave: (String) -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(value) }

    LaunchedEffect(value) { if (!editing) draft = value }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (!editing) { editing = true; draft = value } }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = AccentYellow, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, color = TextDark, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(70.dp))

        if (editing) {
            CompactOutlinedField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = placeholder,
                keyboardType = keyboardType,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { editing = false }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Close, null, tint = Color.Gray)
            }
            IconButton(onClick = { onSave(draft); editing = false }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Check, null, tint = AccentYellow)
            }
        } else {
            Text(
                text = if (value.isBlank()) placeholder else value,
                color = if (value.isBlank()) Color.LightGray else TextGray,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
                maxLines = 1
            )
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Outlined.Edit, null, tint = Color(0xFFEEEEEE), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun ClickableRowV2(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = AccentYellow, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, color = TextDark, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, null, tint = Color.LightGray)
    }
}

@Composable
private fun PreferenceSwitchRowV2(
    label: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = AccentYellow, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, color = TextDark, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AccentYellow,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE0E0E0),
                uncheckedBorderColor = Color.Transparent
            ),
            modifier = Modifier.scale(0.8f)
        )
    }
}

@Composable
private fun CompactOutlinedField(value: String, onValueChange: (String) -> Unit, placeholder: String, keyboardType: KeyboardType, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(8.dp)
    val borderColor = Color(0xFFE0E0E0)
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(shape)
            .border(1.dp, borderColor, shape)
            .background(Color(0xFFFAFAFA))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        var inner by remember { mutableStateOf(value) }
        BasicTextField(
            value = inner,
            onValueChange = { inner = it; onValueChange(it) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = LocalTextStyle.current.copy(color = TextDark, fontSize = 14.sp),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                if (inner.isEmpty()) Text(placeholder, color = Color.LightGray, fontSize = 14.sp)
                innerTextField()
            }
        )
    }
}

// Dialogs (保持不變)
@Composable private fun AvatarActionDialog(onDismiss: () -> Unit, onPickFromGallery: () -> Unit, onTakePhoto: () -> Unit) { Dialog(onDismissRequest = onDismiss) { Surface(shape = RoundedCornerShape(20.dp), color = Color.White) { Column(Modifier.padding(24.dp)) { Text("更換頭像", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark); Spacer(Modifier.height(20.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onPickFromGallery() }) { Icon(Icons.Outlined.Image, null, tint = AccentYellow, modifier = Modifier.size(40.dp)); Text("相簿", fontSize = 14.sp, color = TextGray) }; Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onTakePhoto() }) { Icon(Icons.Outlined.PhotoCamera, null, tint = AccentYellow, modifier = Modifier.size(40.dp)); Text("相機", fontSize = 14.sp, color = TextGray) } } } } } }
@Composable private fun ChangePasswordDialog(loading: Boolean, error: String?, onDismiss: () -> Unit, onSave: (String, String) -> Unit) { var current by remember { mutableStateOf("") }; var newPw by remember { mutableStateOf("") }; var confirm by remember { mutableStateOf("") }; var uiError by remember { mutableStateOf<String?>(null) }; val canEdit = !loading; Dialog(onDismissRequest = onDismiss) { Surface(shape = RoundedCornerShape(24.dp), color = Color.White) { Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { Text("更改密碼", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark); OutlinedTextField(value = current, onValueChange = { current = it }, label = { Text("原有密碼") }, singleLine = true, visualTransformation = PasswordVisualTransformation()); OutlinedTextField(value = newPw, onValueChange = { newPw = it }, label = { Text("新密碼") }, singleLine = true, visualTransformation = PasswordVisualTransformation()); OutlinedTextField(value = confirm, onValueChange = { confirm = it }, label = { Text("確認新密碼") }, singleLine = true, visualTransformation = PasswordVisualTransformation()); uiError?.let { Text(it, color = Color.Red, fontSize = 12.sp) }; Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) { TextButton(onClick = onDismiss) { Text("取消", color = TextGray) }; Button(onClick = { if (newPw != confirm) uiError = "密碼不一致" else onSave(current, newPw) }, colors = ButtonDefaults.buttonColors(containerColor = AccentYellow)) { Text("儲存") } } } } } }
@Composable private fun ConfirmClearScoresDialog(onConfirm: () -> Unit, onCancel: () -> Unit) { Dialog(onDismissRequest = onCancel) { Surface(shape = RoundedCornerShape(20.dp), color = Color.White) { Column(modifier = Modifier.padding(24.dp)) { Text("更換系統編號？", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark); Spacer(Modifier.height(8.dp)); Text("這將清空您的歷史成績紀錄，確定要執行嗎？", fontSize = 14.sp, color = TextGray); Spacer(Modifier.height(20.dp)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373)), modifier = Modifier.weight(1f)) { Text("更換並清空") }; OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("取消") } } } } } }
private fun createTempImageUri(context: Context): Uri { val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }; val file = File.createTempFile("avatar_", ".jpg", imagesDir); return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file) }
private fun uploadAvatar(uri: Uri, uid: String?, storage: FirebaseStorage, firestore: FirebaseFirestore) { if (uid == null) return; val ref = storage.reference.child("avatars/$uid"); ref.putFile(uri).addOnSuccessListener { ref.downloadUrl.addOnSuccessListener { downloadUri -> firestore.collection("users").document(uid).set(mapOf("photoUrl" to downloadUri.toString()), SetOptions.merge()) } } }