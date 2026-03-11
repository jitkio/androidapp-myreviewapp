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

enum class IncomingFrom {
    LEFT, TOP, RIGHT, BOTTOM, NONE
}

private fun tweenOffset() =
    tween<IntOffset>(durationMillis = AnimationDuration.pageTransition, easing = AnimationEasing.easeOut)

private fun tweenFloat() =
    tween<Float>(durationMillis = AnimationDuration.pageTransition, easing = AnimationEasing.easeOut)

fun IncomingFrom.forwardEnter(): EnterTransition = when (this) {
    IncomingFrom.LEFT -> slideInHorizontally(tweenOffset()) { -it }
    IncomingFrom.RIGHT -> slideInHorizontally(tweenOffset()) { it }
    IncomingFrom.TOP -> slideInVertically(tweenOffset()) { -it }
    IncomingFrom.BOTTOM -> slideInVertically(tweenOffset()) { it }
    IncomingFrom.NONE -> EnterTransition.None
}

fun IncomingFrom.forwardExit(): ExitTransition = when (this) {
    IncomingFrom.NONE -> ExitTransition.None
    else -> fadeOut(animationSpec = tweenFloat(), targetAlpha = 0.9f)
}

fun IncomingFrom.popEnter(): EnterTransition = when (this) {
    IncomingFrom.NONE -> EnterTransition.None
    else -> fadeIn(animationSpec = tweenFloat(), initialAlpha = 0.9f)
}

fun IncomingFrom.popExit(): ExitTransition = when (this) {
    IncomingFrom.LEFT -> slideOutHorizontally(tweenOffset()) { -it }
    IncomingFrom.RIGHT -> slideOutHorizontally(tweenOffset()) { it }
    IncomingFrom.TOP -> slideOutVertically(tweenOffset()) { -it }
    IncomingFrom.BOTTOM -> slideOutVertically(tweenOffset()) { it }
    IncomingFrom.NONE -> ExitTransition.None
}

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
