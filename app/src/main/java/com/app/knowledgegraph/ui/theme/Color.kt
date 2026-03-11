package com.app.knowledgegraph.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 颜色系统 - 基于设计规范 v1.0
 * 禁止使用规范外的颜色
 */

// ============ 主色和辅助色 ============
val Primary = Color(0xFF4F6EF7)      // 蓝色 - 主按钮、选中态、重点强调
val Secondary = Color(0xFF1DCAAB)    // 青绿色 - 辅助强调、成功状态

// ============ 背景色 ============
val BgBase = Color(0xFFF8F9FC)       // 浅蓝灰底色 - App主背景、页面底色
val BgCard = Color(0xFFFFFFFF)       // 白色卡片 - 所有卡片、底部导航背景
val BgElevated = Color(0xFFEEF0F5)   // 浅灰悬浮层 - 弹窗、菜单、Tooltip背景、网格线

// ============ 文字色 ============
val TextPrimary = Color(0xFF1A1D27)     // 主要文字 - 标题、正文、强调内容
val TextSecondary = Color(0xFF6B7280)   // 次要文字 - 辅助标题、说明、占位文本
val TextDisabled = Color(0xFF9CA3AF)    // 禁用文字 - 不可操作状态文字

// ============ 功能色 ============
val Error = Color(0xFFFF5C5C)        // 错误红色 - 错误提示、危险操作、删除按钮
val Warning = Color(0xFFFFB84C)      // 警告橙色 - 警告、待处理提示
val Success = Color(0xFF1DCAAB)      // 成功绿色 - 操作成功、完成（同辅助色）
val Border = Color(0xFFE5E7EB)       // 边框色 - 卡片边框、分隔线

// ============ 保留旧颜色（兼容性，逐步移除） ============
@Deprecated("使用 Primary 替代", ReplaceWith("Primary"))
val Purple80 = Color(0xFFD0BCFF)
@Deprecated("使用 TextSecondary 替代", ReplaceWith("TextSecondary"))
val PurpleGrey80 = Color(0xFFCCC2DC)
@Deprecated("使用 Error 替代", ReplaceWith("Error"))
val Pink80 = Color(0xFFEFB8C8)

@Deprecated("使用 Primary 替代", ReplaceWith("Primary"))
val Purple40 = Color(0xFF6650a4)
@Deprecated("使用 TextSecondary 替代", ReplaceWith("TextSecondary"))
val PurpleGrey40 = Color(0xFF625b71)
@Deprecated("使用 Error 替代", ReplaceWith("Error"))
val Pink40 = Color(0xFF7D5260)