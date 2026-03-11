package com.app.knowledgegraph.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * 深色主题配色方案 - 基于设计规范 v1.0
 * 学习应用主要使用深色模式
 */
private val DarkColorScheme = darkColorScheme(
    // 主色
    primary = Primary,              // #4F6EF7 蓝色
    onPrimary = TextPrimary,        // 主色上的文字
    primaryContainer = Primary.copy(alpha = 0.15f),  // 主色容器（15%透明度）
    onPrimaryContainer = Primary,   // 主色容器上的文字

    // 辅助色
    secondary = Secondary,          // #1DCAAB 青绿色
    onSecondary = TextPrimary,
    secondaryContainer = Secondary.copy(alpha = 0.15f),
    onSecondaryContainer = Secondary,

    // 背景色
    background = BgBase,            // #0F1117 主背景
    onBackground = TextPrimary,     // 背景上的文字
    surface = BgCard,               // #1A1D27 卡片表面
    onSurface = TextPrimary,        // 表面上的文字
    surfaceVariant = BgElevated,    // #22263A 悬浮层
    onSurfaceVariant = TextSecondary,  // 表面变体上的文字

    // 功能色
    error = Error,                  // #FF5C5C 错误红色
    onError = TextPrimary,
    errorContainer = Error.copy(alpha = 0.15f),
    onErrorContainer = Error,

    // 边框和分隔线
    outline = Border,               // #2A2D3E 边框色
    outlineVariant = Border.copy(alpha = 0.5f),

    // 其他
    tertiary = Warning,             // #FFB84C 警告橙色
    onTertiary = TextPrimary
)

/**
 * 浅色主题配色方案 - 基于设计规范 v1.0
 */
private val LightColorScheme = lightColorScheme(
    // 主色
    primary = Primary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Primary.copy(alpha = 0.15f),
    onPrimaryContainer = Primary,

    // 辅助色
    secondary = Secondary,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Secondary.copy(alpha = 0.15f),
    onSecondaryContainer = Secondary,

    // 背景色
    background = BgBase,
    onBackground = TextPrimary,
    surface = BgCard,
    onSurface = TextPrimary,
    surfaceVariant = BgElevated,
    onSurfaceVariant = TextSecondary,

    // 功能色
    error = Error,
    onError = Color(0xFFFFFFFF),
    errorContainer = Error.copy(alpha = 0.15f),
    onErrorContainer = Error,

    // 边框和分隔线
    outline = Border,
    outlineVariant = Border.copy(alpha = 0.5f),

    // 其他
    tertiary = Warning,
    onTertiary = Color(0xFFFFFFFF)
)

@Composable
fun KnowledgeGraphTheme(
    darkTheme: Boolean = false,
    // Dynamic color is available on Android 12+
    // 注意：动态颜色会覆盖设计规范，建议关闭
    dynamicColor: Boolean = false,  // 默认关闭动态颜色以遵循设计规范
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