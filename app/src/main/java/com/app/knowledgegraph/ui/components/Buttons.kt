package com.app.knowledgegraph.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.app.knowledgegraph.ui.theme.*

/**
 * Primary按钮 - 主要操作按钮
 * 3D效果：浮起状态有阴影，按下时阴影缩小+按钮下沉
 */
@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) AnimationParams.buttonPressScale else 1f,
        animationSpec = AnimationSpec.clickFeedback,
        label = "buttonScale"
    )
    val shadowElevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 8.dp,
        animationSpec = tween(100),
        label = "buttonShadow"
    )
    val translationY by animateFloatAsState(
        targetValue = if (isPressed) 2f else 0f,
        animationSpec = tween(100),
        label = "buttonTransY"
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .height(ButtonSize.primaryHeight)
            .shadow(
                elevation = shadowElevation,
                shape = RoundedCornerShape(CornerRadius.medium),
                ambientColor = Primary.copy(alpha = 0.3f),
                spotColor = Primary.copy(alpha = 0.4f)
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.translationY = translationY
            },
        enabled = enabled,
        shape = RoundedCornerShape(CornerRadius.medium),
        colors = ButtonDefaults.buttonColors(
            containerColor = Primary,
            contentColor = TextPrimary
        ),
        interactionSource = interactionSource,
        contentPadding = PaddingValues(horizontal = Spacing.space4),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        )
    ) {
        content()
    }
}

/**
 * Secondary按钮 - 次要操作按钮
 * 3D效果：轻微阴影，按下缩小
 */
@Composable
fun SecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) AnimationParams.buttonPressScale else 1f,
        animationSpec = AnimationSpec.clickFeedback,
        label = "buttonScale"
    )
    val shadowElevation by animateDpAsState(
        targetValue = if (isPressed) 1.dp else 4.dp,
        animationSpec = tween(100),
        label = "buttonShadow"
    )
    val translationY by animateFloatAsState(
        targetValue = if (isPressed) 1.5f else 0f,
        animationSpec = tween(100),
        label = "buttonTransY"
    )

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(ButtonSize.secondaryHeight)
            .shadow(
                elevation = shadowElevation,
                shape = RoundedCornerShape(CornerRadius.small),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.12f)
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.translationY = translationY
            },
        enabled = enabled,
        shape = RoundedCornerShape(CornerRadius.small),
        border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Primary,
            containerColor = BgCard
        ),
        interactionSource = interactionSource,
        contentPadding = PaddingValues(horizontal = Spacing.space4)
    ) {
        content()
    }
}

/**
 * Ghost按钮 - 文字按钮
 * 3D效果：只有按压缩放，不加阴影
 */
@Composable
fun GhostButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) AnimationParams.buttonPressScale else 1f,
        animationSpec = AnimationSpec.clickFeedback,
        label = "buttonScale"
    )

    TextButton(
        onClick = onClick,
        modifier = modifier
            .height(ButtonSize.ghostHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = TextSecondary
        ),
        interactionSource = interactionSource,
        contentPadding = PaddingValues(horizontal = Spacing.space3)
    ) {
        content()
    }
}
