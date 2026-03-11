package com.app.knowledgegraph.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.app.knowledgegraph.ui.theme.AnimationDuration
import com.app.knowledgegraph.ui.theme.AnimationEasing
import com.app.knowledgegraph.ui.theme.AnimationParams

enum class IncomingFrom {
    LEFT, TOP, RIGHT, BOTTOM, NONE
}

/* ── tween 工具 ───────────────────────────────────── */

private fun slideEnter() = tween<IntOffset>(
    durationMillis = AnimationDuration.pageTransition,
    easing = AnimationEasing.emphasizedDecelerate
)

private fun slideExit() = tween<IntOffset>(
    durationMillis = AnimationDuration.pageTransition,
    easing = AnimationEasing.emphasizedAccelerate
)

private fun tabSlideEnter() = tween<IntOffset>(
    durationMillis = AnimationDuration.tabSwitch,
    easing = AnimationEasing.emphasizedDecelerate
)

private fun tabSlideExit() = tween<IntOffset>(
    durationMillis = AnimationDuration.tabSwitch,
    easing = AnimationEasing.emphasizedAccelerate
)

private fun fadeSpec(durationMs: Int) = tween<Float>(
    durationMillis = durationMs,
    easing = AnimationEasing.easeOut
)

/* ─────────────────────────────────────────────────────
   核心思路：
   - 新页面完整滑入（100%），覆盖旧页面
   - 旧页面只做 30% 的反向视差偏移，产生"被推开"的感觉
   - 返回时反过来：当前页面完整滑出，底下的页面从 30% 偏移恢复
   ───────────────────────────────────────────────────── */

/* ── 前进：新页面进入 ─────────────────────────────── */

fun IncomingFrom.forwardEnter(): EnterTransition = when (this) {
    IncomingFrom.LEFT -> slideInHorizontally(tabSlideEnter()) { -it } +
            fadeIn(fadeSpec(150), initialAlpha = 0.85f)
    IncomingFrom.RIGHT -> slideInHorizontally(tabSlideEnter()) { it } +
            fadeIn(fadeSpec(150), initialAlpha = 0.85f)
    IncomingFrom.TOP -> slideInVertically(slideEnter()) { -it } +
            fadeIn(fadeSpec(150), initialAlpha = 0.85f)
    IncomingFrom.BOTTOM -> slideInVertically(slideEnter()) { it } +
            fadeIn(fadeSpec(150), initialAlpha = 0.85f)
    IncomingFrom.NONE -> EnterTransition.None
}

/* ── 前进：旧页面退出（视差，只移 30%）──────────── */

fun IncomingFrom.forwardExit(): ExitTransition = when (this) {
    IncomingFrom.LEFT -> slideOutHorizontally(tabSlideExit()) { (it * AnimationParams.parallaxExitRatio).toInt() } +
            fadeOut(fadeSpec(AnimationDuration.tabSwitch), targetAlpha = 0.7f)
    IncomingFrom.RIGHT -> slideOutHorizontally(tabSlideExit()) { (-it * AnimationParams.parallaxExitRatio).toInt() } +
            fadeOut(fadeSpec(AnimationDuration.tabSwitch), targetAlpha = 0.7f)
    IncomingFrom.TOP -> slideOutVertically(slideExit()) { (it * AnimationParams.parallaxExitRatio).toInt() } +
            fadeOut(fadeSpec(AnimationDuration.pageTransition), targetAlpha = 0.7f)
    IncomingFrom.BOTTOM -> slideOutVertically(slideExit()) { (-it * AnimationParams.parallaxExitRatio).toInt() } +
            fadeOut(fadeSpec(AnimationDuration.pageTransition), targetAlpha = 0.7f)
    IncomingFrom.NONE -> ExitTransition.None
}

/* ── 返回：底层页面恢复（从 30% 偏移滑回来）─────── */

fun IncomingFrom.popEnter(): EnterTransition = when (this) {
    IncomingFrom.LEFT -> slideInHorizontally(tabSlideEnter()) { (it * AnimationParams.parallaxExitRatio).toInt() } +
            fadeIn(fadeSpec(AnimationDuration.tabSwitch), initialAlpha = 0.7f)
    IncomingFrom.RIGHT -> slideInHorizontally(tabSlideEnter()) { (-it * AnimationParams.parallaxExitRatio).toInt() } +
            fadeIn(fadeSpec(AnimationDuration.tabSwitch), initialAlpha = 0.7f)
    IncomingFrom.TOP -> slideInVertically(slideEnter()) { (it * AnimationParams.parallaxExitRatio).toInt() } +
            fadeIn(fadeSpec(AnimationDuration.pageTransition), initialAlpha = 0.7f)
    IncomingFrom.BOTTOM -> slideInVertically(slideEnter()) { (-it * AnimationParams.parallaxExitRatio).toInt() } +
            fadeIn(fadeSpec(AnimationDuration.pageTransition), initialAlpha = 0.7f)
    IncomingFrom.NONE -> EnterTransition.None
}

/* ── 返回：当前页面完整滑出 ──────────────────────── */

fun IncomingFrom.popExit(): ExitTransition = when (this) {
    IncomingFrom.LEFT -> slideOutHorizontally(tabSlideExit()) { -it } +
            fadeOut(fadeSpec(200), targetAlpha = 0.85f)
    IncomingFrom.RIGHT -> slideOutHorizontally(tabSlideExit()) { it } +
            fadeOut(fadeSpec(200), targetAlpha = 0.85f)
    IncomingFrom.TOP -> slideOutVertically(slideExit()) { -it } +
            fadeOut(fadeSpec(200), targetAlpha = 0.85f)
    IncomingFrom.BOTTOM -> slideOutVertically(slideExit()) { it } +
            fadeOut(fadeSpec(200), targetAlpha = 0.85f)
    IncomingFrom.NONE -> ExitTransition.None
}

/* ── 页面边缘阴影 ─────────────────────────────────── */

fun Modifier.pageEdgeShadow(direction: IncomingFrom): Modifier = this.drawWithContent {
    drawContent()
    val shadowSize = 24.dp.toPx()
    val dark = Color.Black.copy(alpha = 0.08f)
    val transparent = Color.Transparent
    when (direction) {
        IncomingFrom.BOTTOM -> drawRect(
            brush = Brush.verticalGradient(listOf(dark, transparent), startY = 0f, endY = shadowSize),
            size = Size(size.width, shadowSize)
        )
        IncomingFrom.TOP -> drawRect(
            brush = Brush.verticalGradient(listOf(transparent, dark), startY = size.height - shadowSize, endY = size.height),
            topLeft = Offset(0f, size.height - shadowSize),
            size = Size(size.width, shadowSize)
        )
        IncomingFrom.RIGHT -> drawRect(
            brush = Brush.horizontalGradient(listOf(dark, transparent), startX = 0f, endX = shadowSize),
            size = Size(shadowSize, size.height)
        )
        IncomingFrom.LEFT -> drawRect(
            brush = Brush.horizontalGradient(listOf(transparent, dark), startX = size.width - shadowSize, endX = size.width),
            topLeft = Offset(size.width - shadowSize, 0f),
            size = Size(shadowSize, size.height)
        )
        IncomingFrom.NONE -> { }
    }
}
