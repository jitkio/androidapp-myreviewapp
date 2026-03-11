package com.app.knowledgegraph.ui.theme

import androidx.compose.ui.unit.dp

/**
 * 设计令牌 - 统一管理设计规范
 * 基于 App_UI改版方案 v1.0
 */

/**
 * 间距系统 - 基于8pt标准
 * 只能使用这些预定义的间距值
 */
object Spacing {
    val space1 = 4.dp
    val space2 = 8.dp
    val space3 = 12.dp
    val space4 = 16.dp
    val space5 = 20.dp
    val space6 = 24.dp   // 页面水平边距（标准）
    val space8 = 32.dp
    val space12 = 48.dp

    // 特殊用途
    val pageHorizontal = space6  // 页面水平padding: 24dp
    val cardPadding = space4     // 卡片内边距: 16dp
    val itemSpacing = space3     // 列表项间距: 12dp
}

/**
 * 圆角规范
 * 统一的圆角定义，禁止使用其他值
 */
object CornerRadius {
    val small = 12.dp      // 小标签/小按钮
    val medium = 14.dp     // 普通按钮
    val card = 16.dp       // 标准卡片
    val cardLarge = 20.dp  // 大卡片
    val bottomNav = 24.dp  // 底部导航
}

/**
 * 按钮尺寸规范
 */
object ButtonSize {
    val primaryHeight = 52.dp    // Primary按钮高度
    val secondaryHeight = 44.dp  // Secondary按钮高度
    val ghostHeight = 36.dp      // Ghost按钮高度

    val iconSize = 24.dp         // 图标尺寸
    val minWidth = 80.dp         // 最小宽度
}

/**
 * 底部导航栏规范
 */
object BottomNavigation {
    val height = 56.dp           // 固定高度
    val iconSize = 24.dp         // 图标尺寸 24x24
    val indicatorHeight = 2.dp   // 选中指示条高度
    val indicatorRadius = 1.dp   // 指示条圆角
}

/**
 * 输入框规范
 */
object InputField {
    val height = 48.dp           // 标准高度
    val radius = 12.dp           // 圆角
    val horizontalPadding = 16.dp
    val verticalPadding = 12.dp
}

/**
 * 卡片规范
 */
object CardSpec {
    val standardRadius = 16.dp   // 标准卡片圆角
    val compactRadius = 12.dp    // 紧凑卡片圆角
    val padding = 16.dp          // 内边距
    val elevation = 2.dp         // 阴影高度
}
