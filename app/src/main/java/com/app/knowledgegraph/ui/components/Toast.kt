package com.app.knowledgegraph.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.app.knowledgegraph.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Toast类型
 */
enum class ToastType {
    SUCCESS,
    ERROR,
    WARNING,
    INFO
}

/**
 * Toast数据类
 */
data class ToastData(
    val message: String,
    val type: ToastType = ToastType.INFO,
    val duration: Long = 2000L
)

/**
 * Toast组件 - 统一样式
 * 用于显示临时提示信息
 */
@Composable
fun Toast(
    data: ToastData,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(data) {
        visible = true
        delay(data.duration)
        visible = false
        delay(AnimationDuration.pageTransition.toLong())
        onDismiss()
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(AnimationDuration.pageTransition)
        ) + fadeIn(animationSpec = tween(AnimationDuration.pageTransition)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(AnimationDuration.pageTransition)
        ) + fadeOut(animationSpec = tween(AnimationDuration.pageTransition))
    ) {
        Surface(
            modifier = modifier
                .padding(Spacing.space4)
                .fillMaxWidth(),
            shape = RoundedCornerShape(CornerRadius.card),
            color = when (data.type) {
                ToastType.SUCCESS -> Secondary.copy(alpha = 0.15f)
                ToastType.ERROR -> Error.copy(alpha = 0.15f)
                ToastType.WARNING -> Warning.copy(alpha = 0.15f)
                ToastType.INFO -> Primary.copy(alpha = 0.15f)
            },
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(Spacing.space4),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.space3)
            ) {
                // 图标
                Icon(
                    imageVector = when (data.type) {
                        ToastType.SUCCESS -> Icons.Default.CheckCircle
                        ToastType.ERROR -> Icons.Default.Error
                        ToastType.WARNING -> Icons.Default.Warning
                        ToastType.INFO -> Icons.Default.Info
                    },
                    contentDescription = null,
                    tint = when (data.type) {
                        ToastType.SUCCESS -> Secondary
                        ToastType.ERROR -> Error
                        ToastType.WARNING -> Warning
                        ToastType.INFO -> Primary
                    },
                    modifier = Modifier.size(24.dp)
                )

                // 消息
                Text(
                    text = data.message,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * 进度指示器 - 带百分比显示
 */
@Composable
fun ProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    showPercentage: Boolean = true
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.space2)
    ) {
        // 进度条
        androidx.compose.material3.LinearProgressIndicator(
            progress = progress.coerceIn(0f, 1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = Primary,
            trackColor = Primary.copy(alpha = 0.15f)
        )

        // 百分比文字
        if (showPercentage) {
            Text(
                text = "${(progress * 100).toInt()}%",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

/**
 * 骨架屏 - Shimmer效果
 */
@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(AnimationDuration.shimmer),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    Box(
        modifier = modifier
            .background(
                color = TextSecondary.copy(alpha = alpha),
                shape = RoundedCornerShape(CornerRadius.small)
            )
    )
}

/**
 * 骨架屏卡片 - 用于列表加载
 */
@Composable
fun ShimmerCard(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BgCard, RoundedCornerShape(CornerRadius.card))
            .padding(Spacing.space4),
        verticalArrangement = Arrangement.spacedBy(Spacing.space2)
    ) {
        // 标题
        ShimmerEffect(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(20.dp)
        )

        // 内容行1
        ShimmerEffect(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
        )

        // 内容行2
        ShimmerEffect(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(16.dp)
        )
    }
}
