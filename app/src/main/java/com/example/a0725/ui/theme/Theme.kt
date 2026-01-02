package com.example.a0725.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// 深色主題（暫時簡單版）
private val DarkColorScheme = darkColorScheme(
    primary = BrandYellow,
    onPrimary = Color.Black,
    secondary = BrandTextMain,
    onSecondary = Color.White,

    background = Color(0xFF121212),
    onBackground = Color(0xFFF5F5F5),

    surface = Color(0xFF121212),
    onSurface = Color(0xFFF5F5F5),

    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = BrandSurfaceVariant,

    outline = BrandDivider
)

// 亮色主題：**把所有原本預設的紫色 container 全部改成白 / 灰 / 黃**
private val LightColorScheme = lightColorScheme(
    // 主要色
    primary = BrandYellow,
    onPrimary = Color.White,
    primaryContainer = BrandYellow,
    onPrimaryContainer = Color.Black,

    // 次要色
    secondary = BrandTextMain,
    onSecondary = Color.White,
    secondaryContainer = BrandSurfaceVariant,
    onSecondaryContainer = BrandTextMain,

    // 第三色（避免預設粉紅）
    tertiary = BrandTextMain,
    onTertiary = Color.White,
    tertiaryContainer = BrandSurfaceVariant,
    onTertiaryContainer = BrandTextMain,

    // 背景 & 一般 surface（AppBar、Card、Dialog…）
    background = Color.White,
    onBackground = BrandTextMain,
    surface = Color.White,               // ← AppBar / Card 底色回白
    onSurface = BrandTextMain,

    // 表格 / 列 等的灰底
    surfaceVariant = BrandSurfaceVariant,
    onSurfaceVariant = BrandTextMain,

    // 錯誤色（隨便給一組紅，用不到也無所謂）
    error = Color(0xFFB00020),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    // 邊框線
    outline = BrandDivider,
    outlineVariant = BrandDivider,

    // 反向色（偶爾少數元件會用到）
    inverseSurface = Color(0xFF2E3130),
    inverseOnSurface = Color(0xFFEFF1ED),
    inversePrimary = BrandYellow,

    // elevation 用的 tint，維持黃
    surfaceTint = BrandYellow,

    scrim = Color(0xFF000000)
)

/**
 * App 共用 Theme
 */
@Composable
fun _0725Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // 建議先關掉動態色，不然 12 以上會被系統配色覆蓋
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
