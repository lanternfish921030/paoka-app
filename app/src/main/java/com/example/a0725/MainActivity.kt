package com.example.a0725


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.navigation.compose.rememberNavController
import com.example.a0725.navigation.AppRoot
import com.example.a0725.ui.theme._0725Theme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // 這層決定全 app 的配色
            _0725Theme(
                dynamicColor = false,   // 關掉動態色（不吃桌布顏色）
                darkTheme = false       // 或用 isSystemInDarkTheme() 跟隨系統
            ) {
                AppRoot()
            }
        }
    }
}

