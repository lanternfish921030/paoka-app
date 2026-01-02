package com.example.a0725.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.constraintlayout.compose.ConstraintLayout
import com.example.a0725.navigation.Routes
// 8/15
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.focus.onFocusChanged
import com.example.a0725.auth.AuthViewModel
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
// 8/17
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignScreen(nav: NavController) {
    // 8/15 ViewModel + 狀態
    val vm: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val state by vm.registerState.collectAsState()
    //

    // 欄位狀態
    var username by remember { mutableStateOf("") }
    var account by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var ctaaId by remember { mutableStateOf("") } // 協會系統編號（選填）8/17

    // 8/15 UI 錯誤與顯示/隱藏密碼
    var uiError by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }

    // 聚焦控制 → 顯示 supportingText
    var focusedField by remember { mutableStateOf<String?>(null) }

    // 在 Composable 裡統一欄位高度
    val FIELD_HEIGHT = 65.dp
    val fieldModifier = Modifier
        .fillMaxWidth()
        .heightIn(min = FIELD_HEIGHT)   // 用 heightIn 比 height 穩

    // 8/15 註冊成功 → 回登入或你要的頁面
    LaunchedEffect(state.success) {
        if (state.success) {
            nav.navigate(Routes.LOGIN) {
                popUpTo(Routes.LOGIN) { inclusive = true }
            }
        }
    }
    //

    // 8/16
    var showVerifyDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.success) {
        if (state.success) showVerifyDialog = true
    }

    if (showVerifyDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("請完成信箱驗證") },
            text = { Text("已寄出驗證信到：$email\n請到信箱點擊驗證連結後再登入。") },
            confirmButton = {
                TextButton(onClick = {
                    showVerifyDialog = false
                    nav.navigate(Routes.LOGIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }) { Text("去登入") }
            }
        )
    }
    //

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "註冊", fontSize = 40.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Start,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 15.dp, top = 15.dp)
                    )
                }
            )
        }
    ) { inner ->
        ConstraintLayout(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
        ) {
            val (fieldsColumn, buttonsColumn) = createRefs()
            val line1 = createGuidelineFromTop(0.1f)

            // 第一個 Column 表單區
            Column(
                modifier = Modifier
                    .constrainAs(fieldsColumn) {
                        top.linkTo(line1)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // 真實姓名（與其它欄位同高，允許中文) 8/17
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },  // 千萬不要在這裡 trim/filter
                    label = { Text("用戶名稱（真實姓名）") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,           // 允許中英文
                        imeAction = ImeAction.Next,
                        capitalization = KeyboardCapitalization.Words, // 看需求，可拿掉
                        autoCorrect = true
                    ),
                    modifier = fieldModifier
                        .onFocusChanged { focusedField = if (it.isFocused) "name" else null }
                )

                // 說明文字改成欄位外部顯示（不影響欄位高度）
                AnimatedVisibility(visible = focusedField == "name") {
                    Text(
                        "若要使用成績查詢功能，務必填寫「真實姓名」。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                }

                OutlinedTextField(
                    value = account,
                    onValueChange = { account = it },
                    label = { Text("帳號") },
                    singleLine = true,
                    modifier = fieldModifier
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密碼") },
                    singleLine = true,
                    // 8/15
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    //
                    modifier = fieldModifier
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("再次輸入密碼") },
                    singleLine = true,
                    // 8/15
                    visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { confirmVisible = !confirmVisible }) {
                            Icon(
                                imageVector = if (confirmVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    //
                    modifier = fieldModifier
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("電子信箱") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), // 8/15
                    modifier = fieldModifier
                )

                // 協會系統編號（選填）— 這個可以保留只允許數字的 filter，因為不是中文欄位 8/17
                OutlinedTextField(
                    value = ctaaId,
                    onValueChange = { new ->
                        // 只留數字，避免干擾中文組字（這欄本來就只要數字）
                        ctaaId = new.filter { it.isDigit() }
                    },
                    label = { Text("中華民國田徑協會系統編號（選填）") },
                    singleLine = true,
                    modifier = fieldModifier
                        .onFocusChanged { focusedField = if (it.isFocused) "ctaa" else null }
                )
                AnimatedVisibility(visible = focusedField == "ctaa") {
                    Text(
                        "用途：用姓名 + 系統編號自動查詢協會成績，並同步至 App。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                }
            }

            // 第二個 Column
            Column(
                modifier = Modifier
                    .constrainAs(buttonsColumn) {
                        top.linkTo(fieldsColumn.bottom, margin = 22.dp)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                    .padding(horizontal = 16.dp)
                    .height(180.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // 8/15
                // 顯示錯誤（若 ViewModel 還沒換成 mapRegisterError，做個 fallback 轉換）
                val displayError = remember(uiError, state.error) {
                    uiError ?: state.error?.let { raw ->
                        if (raw.contains("ERROR_EMAIL_ALREADY_IN_USE", ignoreCase = true))
                            "此電子信箱已被註冊，請直接登入或重設密碼。"
                        else raw
                    }
                }
                if (displayError != null) {
                    Text(
                        text = displayError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 15.sp
                    )
                    if (displayError.contains("已被註冊")) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { nav.navigate(Routes.LOGIN) }) {
                                Text("改用登入")
                            }
                            // 若已在 AuthRepository 實作 sendPasswordReset(email)，可改成直接呼叫
                            OutlinedButton(onClick = { nav.navigate(Routes.LOGIN) }) {
                                Text("重設密碼")
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        when {
                            username.isBlank() || account.isBlank() || email.isBlank()
                                    || password.isBlank() || confirmPassword.isBlank() ->
                                uiError = "請完整填寫所有欄位（系統編號為選填）"
                            password != confirmPassword ->
                                uiError = "密碼不一致請再次輸入"
                            else -> {
                                uiError = null
                                vm.register(
                                    username = username.trim(),
                                    account = account.trim(),
                                    email = email.trim(),
                                    password = password,
                                    ctaaId = ctaaId.trim().ifBlank { null } // 允許空值
                                )
                            }
                        }
                    },
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD361))
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (state.isLoading) "註冊中…" else "註冊", fontSize = 18.sp)
                } //

                OutlinedButton(
                    onClick = { nav.navigate(Routes.LOGIN) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                ) {
                    Text("返回登入介面", color = Color(0xFFFF9223), fontSize = 18.sp) // 8/15
                }
            }
        }
    }
}
