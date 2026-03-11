package com.app.knowledgegraph.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.app.knowledgegraph.ui.theme.Primary

/**
 * 通用3D阴影修饰符 — 给任何组件加悬浮感
 * 按下时阴影缩小 + 下沉，松开弹回
 */
fun Modifier.shadow3d(
    defaultElevation: Dp = 6.dp,
    pressedElevation: Dp = 1.dp,
    cornerRadius: Dp = 16.dp,
    pressOffsetY: Float = 2f,
    shadowColor: Color = Color.Black,
    shadowAlpha: Float = 0.12f,
    interactionSource: MutableInteractionSource? = null
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()

    val elevation by animateDpAsState(
        targetValue = if (isPressed) pressedElevation else defaultElevation,
        animationSpec = tween(100),
        label = "3dElevation"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (isPressed) pressOffsetY else 0f,
        animationSpec = tween(100),
        label = "3dOffsetY"
    )

    this
        .shadow(
            elevation = elevation,
            shape = RoundedCornerShape(cornerRadius),
            ambientColor = shadowColor.copy(alpha = shadowAlpha * 0.6f),
            spotColor = shadowColor.copy(alpha = shadowAlpha)
        )
        .graphicsLayer { translationY = offsetY }
}

/** 卡片专用：柔和黑色阴影 */
fun Modifier.cardShadow3d(
    cornerRadius: Dp = 16.dp,
    interactionSource: MutableInteractionSource? = null
): Modifier = shadow3d(
    defaultElevation = 6.dp,
    pressedElevation = 1.dp,
    cornerRadius = cornerRadius,
    pressOffsetY = 2f,
    shadowColor = Color.Black,
    shadowAlpha = 0.12f,
    interactionSource = interactionSource
)

/** 按钮专用：主色调阴影，更明显 */
fun Modifier.buttonShadow3d(
    cornerRadius: Dp = 14.dp,
    interactionSource: MutableInteractionSource? = null
): Modifier = shadow3d(
    defaultElevation = 8.dp,
    pressedElevation = 2.dp,
    cornerRadius = cornerRadius,
    pressOffsetY = 2f,
    shadowColor = Primary,
    shadowAlpha = 0.35f,
    interactionSource = interactionSource
)

/** 轻量3D：用于小卡片、列表项 */
fun Modifier.lightShadow3d(
    cornerRadius: Dp = 12.dp
): Modifier = shadow3d(
    defaultElevation = 3.dp,
    pressedElevation = 1.dp,
    cornerRadius = cornerRadius,
    pressOffsetY = 1f,
    shadowColor = Color.Black,
    shadowAlpha = 0.08f
)

/** 静态阴影：不可点击的悬浮元素 */
fun Modifier.staticShadow3d(
    elevation: Dp = 6.dp,
    cornerRadius: Dp = 16.dp
): Modifier = this.shadow(
    elevation = elevation,
    shape = RoundedCornerShape(cornerRadius),
    ambientColor = Color.Black.copy(alpha = 0.06f),
    spotColor = Color.Black.copy(alpha = 0.10f)
)
