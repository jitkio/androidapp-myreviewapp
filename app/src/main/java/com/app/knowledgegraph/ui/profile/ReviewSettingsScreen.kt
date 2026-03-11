package com.app.knowledgegraph.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class ReviewSettings(
    val dailyNewCards: Int = 10,
    val dailyReviewLimit: Int = 50,
    val easyBonus: Float = 1.3f,
    val hardPenalty: Float = 0.8f,
    val learningSteps: List<Int> = listOf(1, 10, 30),
    val maxInterval: Int = 180,
    val newCardOrder: String = "sequential",
    val reviewOrder: String = "due_first",
    val autoPlayNext: Boolean = true,
    val showHintFirst: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewSettingsScreen(onNavigateBack: () -> Unit) {
    var dailyNew by remember { mutableFloatStateOf(10f) }
    var dailyReview by remember { mutableFloatStateOf(50f) }
    var maxInterval by remember { mutableFloatStateOf(180f) }
    var easyBonus by remember { mutableFloatStateOf(1.3f) }
    var hardPenalty by remember { mutableFloatStateOf(0.8f) }
    var showHintFirst by remember { mutableStateOf(false) }
    var autoNext by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("复习设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            SettingsSection("每日学习量") {
                SliderSetting(
                    label = "每日新卡片",
                    value = dailyNew,
                    onValueChange = { dailyNew = it },
                    range = 1f..30f,
                    steps = 29,
                    display = "${dailyNew.toInt()} 张"
                )
                SliderSetting(
                    label = "每日复习上限",
                    value = dailyReview,
                    onValueChange = { dailyReview = it },
                    range = 10f..200f,
                    steps = 19,
                    display = "${dailyReview.toInt()} 张"
                )
            }

            SettingsSection("间隔参数（记忆曲线调整）") {
                SliderSetting(
                    label = "最大间隔天数",
                    value = maxInterval,
                    onValueChange = { maxInterval = it },
                    range = 30f..365f,
                    steps = 13,
                    display = "${maxInterval.toInt()} 天",
                    description = "到达此间隔后不再增长。考前可以调小（如60天）确保考前能复习到。"
                )
                SliderSetting(
                    label = "Easy加速系数",
                    value = easyBonus,
                    onValueChange = { easyBonus = it },
                    range = 1.0f..2.0f,
                    steps = 9,
                    display = "x${"%.1f".format(easyBonus)}",
                    description = "按Easy时额外乘以此系数。觉得简单的卡片会更快拉长间隔。"
                )
                SliderSetting(
                    label = "Hard惩罚系数",
                    value = hardPenalty,
                    onValueChange = { hardPenalty = it },
                    range = 0.5f..1.0f,
                    steps = 9,
                    display = "x${"%.1f".format(hardPenalty)}",
                    description = "按Hard时间隔乘以此系数。难的卡片更频繁复习。"
                )
            }

            SettingsSection("学习策略") {
                Text(
                    "学习步骤（分钟）：1 -> 10 -> 30",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "新卡片先经过短间隔学习（1分钟、10分钟、30分钟），全部通过后进入正式复习队列。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsSection("界面偏好") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("先显示提示", style = MaterialTheme.typography.bodyLarge)
                        Text("显示答案前先展开Hint", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = showHintFirst, onCheckedChange = { showHintFirst = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("自动下一张", style = MaterialTheme.typography.bodyLarge)
                        Text("评分后自动跳转到下一张", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = autoNext, onCheckedChange = { autoNext = it })
                }
            }

            SettingsSection("记忆曲线说明") {
                Text(
                    "本应用使用SM-2改进版算法：\n\n" +
                    "- Again（忘了）：重置间隔为1天，难度因子下降\n" +
                    "- Hard（不熟）：间隔缩短，难度因子略降\n" +
                    "- Good（记住）：正常增长间隔\n" +
                    "- Easy（秒答）：间隔加速增长\n\n" +
                    "难度因子越低的卡片复习越频繁。\n" +
                    "连续答对间隔逐渐拉长：1天->3天->7天->16天->...\n" +
                    "答错立即重置，确保薄弱点得到强化。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) { content() }
        }
    }
}

@Composable
fun SliderSetting(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    display: String,
    description: String = ""
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(display, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = range, steps = steps)
        if (description.isNotBlank()) {
            Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
