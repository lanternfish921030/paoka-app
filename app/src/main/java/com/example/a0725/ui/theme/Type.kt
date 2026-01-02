package com.example.a0725.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.a0725.R

val KaiuFontFamily = FontFamily(
    Font(R.font.kaiu, weight = FontWeight.Normal)
)

val KleeOneFontFamily = FontFamily(
    Font(R.font.kleeone, weight = FontWeight.Normal)
)

// ⭐ 讓整個 App 使用 Material3 預設字型（就是你說首頁那種）
// 如果你不介意 bodyLarge 的大小，就這樣最乾脆：
val Typography = Typography()

/* Other default text styles to override
titleLarge = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Normal,
    fontSize = 22.sp,
    lineHeight = 28.sp,
    letterSpacing = 0.sp
),
labelSmall = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.5.sp
)
*/

