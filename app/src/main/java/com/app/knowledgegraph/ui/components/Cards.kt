package com.app.knowledgegraph.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
 * 标准卡片 - 3D悬浮效果
 * 默认有阴影，按下时阴影缩小+微微下沉
 */
@Composable
fun StandardCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val shadowElevation by animateDpAsState(
        targetValue = if (isPressed && onClick != null) 2.dp else 6.dp,
        animationSpec = tween(120),
        label = "cardShadow"
    )
    val translationY by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 2f else 0f,
        animationSpec = tween(120),
        label = "cardTransY"
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) AnimationParams.cardPressScale else 1f,
        animationSpec = tween(120),
        label = "cardScale"
    )

    Card(
        onClick = onClick ?: {},
        modifier = modifier
            .shadow(
                elevation = shadowElevation,
                shape = RoundedCornerShape(CornerRadius.card),
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.10f)
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.translationY = translationY
            },
        enabled = onClick != null,
        shape = RoundedCornerShape(CornerRadius.card),
        colors = CardDefaults.cardColors(
            containerColor = BgCard
        ),
        interactionSource = interactionSource,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(CardSpec.padding)
        ) {
            content()
        }
    }
}

/**
 * 紧凑卡片 - 轻度3D效果
 * 边框 + 轻阴影，按下有按压反馈
 */
@Composable
fun CompactCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val shadowElevation by animateDpAsState(
        targetValue = if (isPressed && onClick != null) 1.dp else 3.dp,
        animationSpec = tween(120),
        label = "compactShadow"
    )
    val translationY by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 1f else 0f,
        animationSpec = tween(120),
        label = "compactTransY"
    )

    Card(
        onClick = onClick ?: {},
        modifier = modifier
            .shadow(
                elevation = shadowElevation,
                shape = RoundedCornerShape(CardSpec.compactRadius),
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .graphicsLayer {
                this.translationY = translationY
            }
            .border(
                width = 1.dp,
                color = Border,
                shape = RoundedCornerShape(CardSpec.compactRadius)
            ),
        enabled = onClick != null,
        shape = RoundedCornerShape(CardSpec.compactRadius),
        colors = CardDefaults.cardColors(
            containerColor = BgCard
        ),
        interactionSource = interactionSource,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(Spacing.space3)
        ) {
            content()
        }
    }
}

/**
 * 悬浮卡片 - 强3D效果
 * 大阴影，明显悬浮感
 */
@Composable
fun ElevatedCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(CornerRadius.card),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.15f)
            ),
        shape = RoundedCornerShape(CornerRadius.card),
        colors = CardDefaults.cardColors(
            containerColor = BgElevated
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(Spacing.space5)
        ) {
            content()
        }
    }
}
