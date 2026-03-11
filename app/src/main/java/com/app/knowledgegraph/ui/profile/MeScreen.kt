package com.app.knowledgegraph.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.knowledgegraph.AppContainer
import com.app.knowledgegraph.ui.theme.*
import com.app.knowledgegraph.ui.components.StandardCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeScreen(
    container: AppContainer,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToApiKeySettings: () -> Unit = {}
) {
    val totalCards by container.cardRepository.observeCount().collectAsState(initial = 0)
    val totalReviews by container.reviewDao.observeTotalReviews().collectAsState(initial = 0)
    val totalCorrect by container.reviewDao.observeTotalCorrect().collectAsState(initial = 0)
    val practiceTotal by container.practiceRecordDao.observeTotalCount().collectAsState(initial = 0)
    val practiceCorrect by container.practiceRecordDao.observeCorrectCount().collectAsState(initial = 0)

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Me", style = MaterialTheme.typography.titleLarge) },
            windowInsets = WindowInsets(0)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.space4),
            verticalArrangement = Arrangement.spacedBy(Spacing.space4)
        ) {
            Text("学习概况", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.space3)
            ) {
                StatCard("总卡片", "$totalCards", Modifier.weight(1f))
                StatCard("总复习", "${totalReviews ?: 0}", Modifier.weight(1f))
                StatCard("正确率", if ((totalReviews ?: 0) > 0)
                    "${((totalCorrect ?: 0) * 100 / (totalReviews ?: 1))}%" else "--",
                    Modifier.weight(1f))
            }

            if (practiceTotal > 0) {
                Text("选法训练", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.space3)
                ) {
                    StatCard("练习题数", "$practiceTotal", Modifier.weight(1f))
                    StatCard("选法正确", if (practiceTotal > 0)
                        "${practiceCorrect * 100 / practiceTotal}%" else "--",
                        Modifier.weight(1f))
                }
            }

            HorizontalDivider()

            StandardCard(
                onClick = onNavigateToSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Settings, null, tint = Primary)
                    Spacer(modifier = Modifier.width(Spacing.space3))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("复习设置", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text("调整每日学习量、记忆曲线参数", style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = TextSecondary)
                }
            }

            StandardCard(
                onClick = onNavigateToApiKeySettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Key, null, tint = Primary)
                    Spacer(modifier = Modifier.width(Spacing.space3))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("API Key 设置", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text("配置 DeepSeek API Key 用于扫题识别", style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary)
                    }
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = BgCard),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(CornerRadius.card)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.space4),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold, color = Primary)
            Spacer(modifier = Modifier.height(Spacing.space1))
            Text(label, style = MaterialTheme.typography.bodySmall,
                color = TextSecondary)
        }
    }
}
