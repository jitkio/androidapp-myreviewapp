package com.app.knowledgegraph.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring

/**
 * 动画规范 - 基于设计规范 v1.0
 * 统一管理动画时长和缓动函数
 */

/**
 * 动画时长（毫秒）
 * P0优先级：点击反馈必须在100ms内
 */
object AnimationDuration {
    const val clickFeedback = 100      // 点击反馈 - P0优先级
    const val tabSwitch = 150          // Tab切换
    const val bottomSheetHide = 200    // 底部弹窗隐藏
    const val pageTransition = 700     // 页面切换
    const val modalShow = 250          // Modal弹出
    const val bottomSheetShow = 320    // 底部弹窗显示
    const val successAnimation = 400   // 成功动画（checkmark）
    const val shimmer = 1500           // 骨架屏循环
}

/**
 * 缓动函数
 * 推荐使用 ease-out 和 ease-in-out，避免使用 linear
 */
object AnimationEasing {
    // 标准缓动
    val easeOut = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    val easeInOut = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    val easeIn = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)

    // 特殊缓动
    val emphasized = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1.0f)  // 强调效果（有回弹）
}

/**
 * 预定义的动画规格
 * 直接使用这些规格确保一致性
 */
object AnimationSpec {
    // 点击反馈动画 - P0优先级
    val clickFeedback = tween<Float>(
        durationMillis = AnimationDuration.clickFeedback,
        easing = AnimationEasing.easeOut
    )

    // Tab切换动画
    val tabSwitch = tween<Float>(
        durationMillis = AnimationDuration.tabSwitch,
        easing = AnimationEasing.easeOut
    )

    // 页面切换动画
    val pageTransition = tween<Float>(
        durationMillis = AnimationDuration.pageTransition,
        easing = AnimationEasing.easeOut
    )

    // 底部弹窗显示（带弹性）
    val bottomSheetShow = spring<Float>(
        dampingRatio = 0.85f,
        stiffness = Spring.StiffnessMediumLow
    )

    // Modal弹出
    val modalShow = tween<Float>(
        durationMillis = AnimationDuration.modalShow,
        easing = AnimationEasing.easeOut
    )
}

/**
 * 动画参数
 */
object AnimationParams {
    // 按钮按压缩放
    const val buttonPressScale = 0.97f

    // 卡片按压缩放
    const val cardPressScale = 0.99f

    // Tab图标弹跳缩放
    const val tabIconBounceScale = 1.15f

    // 按压时背景变暗透明度
    const val pressedAlpha = 0.15f

    // 页面切换偏移量
    const val pageTransitionOffset = 0.3f  // 30%
}
