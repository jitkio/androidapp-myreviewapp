package com.app.knowledgegraph.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.app.knowledgegraph.ui.theme.*

/**
 * 空状态页面组件 - 统一样式
 * 用于列表为空、无数据等场景
 */
@Composable
fun EmptyState(
    icon: @Composable () -> Unit = {
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = TextSecondary
        )
    },
    title: String,
    description: String? = null,
    primaryAction: (@Composable () -> Unit)? = null,
    secondaryAction: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.space6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 图标
        icon()

        Spacer(modifier = Modifier.height(Spacing.space4))

        // 标题
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        // 描述（可选）
        if (description != null) {
            Spacer(modifier = Modifier.height(Spacing.space2))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = Spacing.space4)
            )
        }

        // 主要操作按钮（可选）
        if (primaryAction != null) {
            Spacer(modifier = Modifier.height(Spacing.space6))
            primaryAction()
        }

        // 次要操作按钮（可选）
        if (secondaryAction != null) {
            Spacer(modifier = Modifier.height(Spacing.space3))
            secondaryAction()
        }
    }
}

/**
 * 加载状态组件 - 统一样式
 */
@Composable
fun LoadingState(
    message: String = "加载中...",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.space6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        androidx.compose.material3.CircularProgressIndicator(
            color = Primary,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(Spacing.space4))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )
    }
}

/**
 * 错误状态组件 - 统一样式
 */
@Composable
fun ErrorState(
    title: String = "出错了",
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.space6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 错误图标
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Error
        )

        Spacer(modifier = Modifier.height(Spacing.space4))

        // 标题
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Spacing.space2))

        // 错误信息
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Spacing.space4)
        )

        // 重试按钮（可选）
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(Spacing.space6))
            PrimaryButton(onClick = onRetry) {
                Text("重试")
            }
        }
    }
}
