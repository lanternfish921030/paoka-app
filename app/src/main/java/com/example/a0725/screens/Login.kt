package com.example.a0725.screens




import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.navigation.NavHostController
import com.example.a0725.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.a0725.navigation.Routes
import com.example.a0725.navigation.navigateSingleTopTo
import com.example.a0725.screens.*
import com.example.a0725.auth.AuthViewModel
import androidx.compose.runtime.*
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.OutlinedTextFieldDefaults


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(nav: NavHostController) {


    val vm: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val state by vm.loginState.collectAsState()




    var email by remember { mutableStateOf("") }   // 原本是 account；Firebase 用 email




    // 8/23
    val resetState by vm.resetPwState.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }




    // 登入成功 → 進首頁並清掉 back stack
    LaunchedEffect(state.success) {
        if (state.success) {
            nav.navigate(Routes.HOME) {
                popUpTo(Routes.LOGIN) { inclusive = true }
            }
        }
    }


    Scaffold(
        // 1. 拿掉 topBar，讓畫面更乾淨，我們自己寫標題
        containerColor = Color.White // 背景設為白色
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxSize()
                // 這裡的 padding 會同時影響標題和輸入框，確保它們左右對齊
                .padding(horizontal = 32.dp)
        ) {
            // 2. 宣告三個參考點：標題(header)、表單(form)、底部(footer)
            val (header, form, footer) = createRefs()


            // --- A. 標題區塊 ---
            Column(
                modifier = Modifier.constrainAs(header) {
                    // 3. 控制標題距離頂部的高度 (這裡設 100.dp，你可以隨意調整讓它更往下)
                    top.linkTo(parent.top, margin = 100.dp)
                    start.linkTo(parent.start)
                }
            ) {
                Text(
                    text = "登入",
                    fontSize = 40.sp, // 大標題
                    color = Color(0xFF333333),
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Start
                )
            }




            Column(
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .constrainAs(form) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
            ) {
                var password by remember { mutableStateOf("") }
                var passwordVisible by remember { mutableStateOf(false) }


                Column {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("電子信箱", fontSize = 18.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp), // 圓角
                        leadingIcon = {
                            Icon(Icons.Filled.Email, contentDescription = null, tint = Color.Gray)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                }
                Column {
                    // 置於密碼輸入框的上方
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End // <--- 加入這行，內容就會靠右
                    ) {
                        Text(
                            text = "忘記密碼？",
                            color = Color(0xFF2196F3),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { showResetDialog = true }
                        )
                    }




                    // 密碼輸入框
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("密碼", fontSize = 18.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp), // 圓角
                        leadingIcon = {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = Color.Gray)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image =
                                if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = null)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )








                }
                // 8/15
                // 錯誤訊息 + 驗證信動作
                if (state.error != null) {
                    Text(
                        text = state.error!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth()
                    )




                    if (state.error!!.contains("尚未完成信箱驗證")) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = {
                                vm.refreshVerificationAndLogin(email.trim(), password)
                            }) {
                                Text("我已驗證，重新登入")
                            }
                            OutlinedButton(onClick = { vm.resendVerification() }) {
                                Text("重新寄送驗證信")
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
                // ---- 忘記密碼對話框 ---- \
                if (showResetDialog) {
                    ForgotPasswordDialog(
                        defaultEmail = email,
                        loading = resetState.isLoading,
                        error = resetState.error,
                        success = resetState.success,    // <— 新增
                        onDismiss = {
                            vm.clearResetPwState()
                            showResetDialog = false
                        },
                        onSend = { mail ->
                            vm.resetPassword(mail.trim())
                        }
                    )
                }




                // 登入按鈕 8/15
                Button(
                    onClick = { vm.login(email.trim(), password) },
                    enabled = !state.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD361)),
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().height(70.dp).padding(top = 10.dp)
                ) {
                    Text(
                        if (state.isLoading) "登入中…" else "登入",
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
                //
            }




            Row(
                modifier = Modifier
                    .constrainAs(footer) {
                        bottom.linkTo(parent.bottom, margin = 50.dp)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    },
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "還沒有帳號?    ", color = Color.Gray, fontSize = 22.sp)
                Text(
                    text = "註冊",
                    color = Color(0xFF2196F3),
                    fontSize = 22.sp,
                    modifier = Modifier.clickable {
                        nav.navigate(Routes.SIGN)
                    }
                )
            }
        }
    }
}




// 8/23
@Composable
private fun ForgotPasswordDialog(
    defaultEmail: String,
    loading: Boolean,
    error: String?,
    success: Boolean,                    // <— 新增
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    var input by remember { mutableStateOf(defaultEmail) }
    var localError by remember { mutableStateOf<String?>(null) }




    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 6.dp,
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("重設密碼", style = MaterialTheme.typography.titleLarge)




                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("註冊用的電子信箱") },
                    singleLine = true,
                    enabled = !loading && !success,          // 成功後鎖住輸入
                    modifier = Modifier.fillMaxWidth()
                )




                // 錯誤
                val displayErr = localError ?: error
                if (!displayErr.isNullOrBlank() && !success) {
                    Text(
                        text = displayErr,
                        color = MaterialTheme.colorScheme.error
                    )
                }




                // 成功提示（不自動關）
                if (success) {
                    Text(
                        text = "已寄出重設密碼郵件至：$input\n" +
                                "請前往信箱點擊郵件中的連結完成重設。\n若找不到，請檢查垃圾郵件或促銷匣。",
                        color = MaterialTheme.colorScheme.primary
                    )
                }




                // Loading
                if (loading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("寄送中…")
                    }
                }




                // 底部按鈕
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, enabled = !loading) { Text("關閉") }
                    Spacer(Modifier.size(8.dp))
                    if (!success) { // 成功後就不顯示「寄出」
                        TextButton(
                            onClick = {
                                if (input.isBlank()) {
                                    localError = "請輸入電子信箱"
                                } else {
                                    localError = null
                                    onSend(input)
                                }
                            },
                            enabled = !loading
                        ) { Text("寄出", color = Color(0xFF2196F3)) }
                    }
                }
            }
        }
    }
}













