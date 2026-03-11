package com.app.knowledgegraph.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.app.knowledgegraph.ui.theme.*

/**
 * 标准卡片 - 16dp圆角
 * 用于主要内容展示
 */
@Composable
fun StandardCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        onClick = onClick ?: {},
        modifier = modifier,
        enabled = onClick != null,
        shape = RoundedCornerShape(CornerRadius.card),
        colors = CardDefaults.cardColors(
            containerColor = BgCard
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
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
 * 紧凑卡片 - 12dp圆角
 * 用于列表项、小卡片
 */
@Composable
fun CompactCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        onClick = onClick ?: {},
        modifier = modifier
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
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
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
 * 悬浮卡片 - 用于弹窗、对话框
 */
@Composable
fun ElevatedCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(CornerRadius.card),
        colors = CardDefaults.cardColors(
            containerColor = BgElevated
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(Spacing.space5)
        ) {
            content()
        }
    }
}
