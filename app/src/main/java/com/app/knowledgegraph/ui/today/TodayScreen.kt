package com.app.knowledgegraph.ui.today

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.knowledgegraph.AppContainer
import com.app.knowledgegraph.ui.navigation.AppNavState
import com.app.knowledgegraph.data.db.entity.Card
import com.app.knowledgegraph.ui.components.MathView
import com.app.knowledgegraph.ui.components.PrimaryButton
import com.app.knowledgegraph.ui.components.GhostButton
import com.app.knowledgegraph.ui.components.NeutralTag
import com.app.knowledgegraph.data.db.entity.ErrorType
import com.app.knowledgegraph.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    container: AppContainer,
    appNavState: AppNavState,
    viewModel: TodayViewModel = viewModel(factory = TodayViewModel.factory(container))
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Today", style = MaterialTheme.typography.titleLarge) },
            windowInsets = WindowInsets(0),
            actions = {
                Text(
                    text = "${uiState.sessionStats.reviewed} done | ${uiState.remaining} left",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(end = Spacing.space4)
                )
            }
        )

        when {
            uiState.isLoading -> {
                com.app.knowledgegraph.ui.components.LoadingState(message = "加载复习卡片...")
            }
            uiState.queue.isEmpty() -> { EmptyState() }
            uiState.isComplete -> { CompleteState(uiState.sessionStats) }
            else -> {
                ReviewCard(
                    card = uiState.currentCard!!,
                    isAnswerRevealed = uiState.isAnswerRevealed,
                    onRevealAnswer = { viewModel.revealAnswer() },
                    onRate = { quality -> viewModel.submitRating(quality) },
                    onSkip = { viewModel.skipCard() }
                )
            }
        }
    }

    if (uiState.showErrorTagDialog) {
        ErrorTagDialog(
            onConfirm = { errorType -> viewModel.submitErrorTag(errorType) },
            onDismiss = { viewModel.dismissErrorTagDialog() }
        )
    }
}

@Composable
fun ReviewCard(
    card: Card,
    isAnswerRevealed: Boolean,
    onRevealAnswer: () -> Unit,
    onRate: (Int) -> Unit,
    onSkip: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 可滚动的内容区域
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.pageHorizontal)
        ) {
            Row(
                modifier = Modifier.padding(top = Spacing.space2),
                horizontalArrangement = Arrangement.spacedBy(Spacing.space2)
            ) {
                NeutralTag(text = card.type.name)
                Text(
                    text = card.chapter,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.space6))

            MathView(
                text = card.prompt,
                modifier = Modifier.fillMaxWidth(),
                baseFontSize = 22
            )

            Spacer(modifier = Modifier.height(Spacing.space5))

            if (isAnswerRevealed) {
                if (card.hint.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Warning.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MathView(
                            text = card.hint,
                            modifier = Modifier.fillMaxWidth().padding(Spacing.space3)
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.space3))
                }

                MathView(
                    text = card.answer,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(Spacing.space6))
            }
        }

        // 底部固定按钮区域
        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!isAnswerRevealed) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.pageHorizontal, vertical = Spacing.space3),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    GhostButton(onClick = onSkip) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Skip")
                        Spacer(modifier = Modifier.width(Spacing.space1))
                        Text("跳过")
                    }
                    PrimaryButton(onClick = onRevealAnswer) {
                        Text("显示答案")
                    }
                }
            } else {
                RatingButtons(
                    onRate = onRate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.space4, vertical = Spacing.space3)
                )
            }
        }
    }
}

@Composable
fun RatingButtons(
    onRate: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
        FilledTonalButton(
            onClick = { onRate(0) },
            modifier = Modifier.weight(1f).height(ButtonSize.secondaryHeight),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = Error.copy(alpha = 0.15f),
                contentColor = Error
            )
        ) { Text("\u5fd8\u8bb0", maxLines = 1) }

        FilledTonalButton(
            onClick = { onRate(2) },
            modifier = Modifier.weight(1f).height(ButtonSize.secondaryHeight),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = Warning.copy(alpha = 0.15f),
                contentColor = Warning
            )
        ) { Text("\u56f0\u96be", maxLines = 1) }

        FilledTonalButton(
            onClick = { onRate(3) },
            modifier = Modifier.weight(1f).height(ButtonSize.secondaryHeight),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = Secondary.copy(alpha = 0.15f),
                contentColor = Secondary
            )
        ) { Text("\u8bb0\u5f97", maxLines = 1) }

        FilledTonalButton(
            onClick = { onRate(5) },
            modifier = Modifier.weight(1f).height(ButtonSize.secondaryHeight),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = Primary.copy(alpha = 0.15f),
                contentColor = Primary
            )
        ) { Text("\u7b80\u5355", maxLines = 1) }
    }
}

@Composable
fun ErrorTagDialog(
    onConfirm: (ErrorType) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf<ErrorType?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择错因") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ErrorType.entries.forEach { type ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedType == type,
                            onClick = { selectedType = type }
                        )
                        Text(
                            text = errorTypeLabel(type),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedType?.let { onConfirm(it) } },
                enabled = selectedType != null
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("跳过") }
        }
    )
}

private fun errorTypeLabel(type: ErrorType): String = when (type) {
    ErrorType.DIRECTION_ERROR -> "方向错（参考方向）"
    ErrorType.PORT_ERROR -> "端口错（开路/短路）"
    ErrorType.CONDITION_ERROR -> "适用条件错"
    ErrorType.FORMULA_ERROR -> "公式记错"
    ErrorType.SIGN_ERROR -> "正负号错"
    ErrorType.METHOD_WRONG -> "方法选错"
    ErrorType.STEP_MISSING -> "步骤遗漏"
    ErrorType.OTHER -> "其他"
}

@Composable
fun EmptyState() {
    com.app.knowledgegraph.ui.components.EmptyState(
        title = "暂无复习卡片",
        description = "前往 Library 添加新卡片，开始你的学习之旅"
    )
}

@Composable
fun CompleteState(stats: SessionStats) {
    com.app.knowledgegraph.ui.components.EmptyState(
        icon = {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Secondary,
                modifier = Modifier.size(64.dp)
            )
        },
        title = "今日复习完成！",
        description = "复习 ${stats.reviewed} 张 | 正确 ${stats.correct} | 重学 ${stats.again}"
    )
}
