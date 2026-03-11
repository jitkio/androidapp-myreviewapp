package com.app.knowledgegraph.ui.components

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.app.knowledgegraph.ui.theme.*

/**
 * Primary按钮 - 主要操作按钮
 * 高度52dp，圆角14dp，实心蓝色背景
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

    Button(
        onClick = onClick,
        modifier = modifier
            .height(ButtonSize.primaryHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        enabled = enabled,
        shape = RoundedCornerShape(CornerRadius.medium),
        colors = ButtonDefaults.buttonColors(
            containerColor = Primary,
            contentColor = TextPrimary
        ),
        interactionSource = interactionSource,
        contentPadding = PaddingValues(horizontal = Spacing.space4)
    ) {
        content()
    }
}

/**
 * Secondary按钮 - 次要操作按钮
 * 高度44dp，圆角12dp，白底+蓝色边框
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

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(ButtonSize.secondaryHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        enabled = enabled,
        shape = RoundedCornerShape(CornerRadius.small),
        border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Primary
        ),
        interactionSource = interactionSource,
        contentPadding = PaddingValues(horizontal = Spacing.space4)
    ) {
        content()
    }
}

/**
 * Ghost按钮 - 文字按钮
 * 高度36dp，圆角10dp，无边框背景透明
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
