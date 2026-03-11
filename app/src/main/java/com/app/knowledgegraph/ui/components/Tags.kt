package com.app.knowledgegraph.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.knowledgegraph.ui.theme.*

/**
 * Primary标签 - 蓝色标签
 * 用于主要分类、选中状态
 */
@Composable
fun PrimaryTag(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier
            .background(
                color = Primary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold,
        color = Primary
    )
}

/**
 * Secondary标签 - 青绿色标签
 * 用于辅助分类、成功状态
 */
@Composable
fun SecondaryTag(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier
            .background(
                color = Secondary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold,
        color = Secondary
    )
}

/**
 * Neutral标签 - 中性标签
 * 用于普通分类、默认状态
 */
@Composable
fun NeutralTag(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier
            .background(
                color = BgElevated,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary
    )
}

/**
 * Error标签 - 错误标签
 * 用于错误提示、警告状态
 */
@Composable
fun ErrorTag(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier
            .background(
                color = Error.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold,
        color = Error
    )
}

/**
 * Warning标签 - 警告标签
 * 用于警告提示、待处理状态
 */
@Composable
fun WarningTag(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier
            .background(
                color = Warning.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold,
        color = Warning
    )
}
