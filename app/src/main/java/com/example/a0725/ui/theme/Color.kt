package com.example.a0725.ui.theme

import androidx.compose.ui.graphics.Color

// ===== 如果有舊程式還引用到 PurpleXX，就先保留這些顏色名稱，之後要刪再說 =====
//val Purple80     = Color(0xFFD0BCFF)
//val PurpleGrey80 = Color(0xFFCCC2DC)
//val Pink80       = Color(0xFFEFB8C8)
//
//val Purple40     = Color(0xFF6650A4)
//val PurpleGrey40 = Color(0xFF625B71)
//val Pink40       = Color(0xFF7D5260)

// ===== 新的白 / 灰 / 黃品牌色票 =====

// 主要品牌色（黃）
val BrandYellow      = Color(0xFFFFA800)   // 主要強調色：按鈕、Icon、分頁底線
val BrandYellowLight = Color(0xFFFFF3C8)   // 膠囊背景、Tag、Chip 等

// 文字 & 灰階
val BrandTextMain      = Color(0xFF444444) // 主要文字（深灰）
val BrandTextSecondary = Color(0xFF9E9E9E) // 次要文字（提示字、說明文字）

// 底色
val BrandSurface        = Color(0xFFFFFFFF) // App 整體背景、卡片、Dialog 等
val BrandSurfaceVariant = Color(0xFFF0F0F3) // 列背景、輸入框背景、表格灰底（**偏灰，不帶紫**）
val BrandDivider        = Color(0xFFE0E0E0) // 邊框線、分隔線
