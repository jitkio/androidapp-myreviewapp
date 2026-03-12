package com.app.knowledgegraph.ui.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
                    modifier = Modifier.padding(end = Spacing.space2)
                )
                IconButton(onClick = { viewModel.openAddCards() }) {
                    Icon(Icons.Default.PlaylistAdd, contentDescription = "添加复习卡片", tint = Primary)
                }
            }
        )

        when {
            uiState.isLoading -> {
                com.app.knowledgegraph.ui.components.LoadingState(message = "加载复习卡片...")
            }
            uiState.queue.isEmpty() -> {
                EmptyStateWithAdd(onAdd = { viewModel.openAddCards() })
            }
            uiState.isComplete -> {
                CompleteState(uiState.sessionStats, onAddMore = { viewModel.openAddCards() })
            }
            uiState.currentCard != null -> {
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

    if (uiState.showAddCards) {
        AddCardsSheet(
            uiState = uiState,
            onSearchChange = viewModel::updateAddSearch,
            onChapterSelect = viewModel::selectAddChapter,
            onAddCards = viewModel::addCardsToQueue,
            onDismiss = viewModel::closeAddCards
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCardsSheet(
    uiState: TodayUiState,
    onSearchChange: (String) -> Unit,
    onChapterSelect: (String?) -> Unit,
    onAddCards: (List<Card>) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = BgCard) {
        Column(modifier = Modifier.fillMaxHeight(0.85f)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.space4, vertical = Spacing.space2),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("添加复习卡片", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (selectedIds.isNotEmpty()) {
                    Button(
                        onClick = { onAddCards(uiState.filteredAddCards.filter { it.id in selectedIds }) },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) { Text("添加 ${selectedIds.size} 张") }
                }
            }

            OutlinedTextField(
                value = uiState.addSearchQuery, onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.space4, vertical = Spacing.space1),
                placeholder = { Text("搜索卡片...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true, shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Border, focusedBorderColor = Primary)
            )

            if (uiState.addChapters.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.space4, vertical = Spacing.space1),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.space2)
                ) {
                    FilterChip(selected = uiState.addSelectedChapter == null, onClick = { onChapterSelect(null) },
                        label = { Text("全部") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Primary.copy(alpha = 0.15f), selectedLabelColor = Primary))
                    uiState.addChapters.forEach { chapter ->
                        FilterChip(selected = uiState.addSelectedChapter == chapter, onClick = { onChapterSelect(chapter) },
                            label = { Text(chapter) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Primary.copy(alpha = 0.15f), selectedLabelColor = Primary))
                    }
                }
            }

            val allFilteredIds = uiState.filteredAddCards.map { it.id }.toSet()
            val allSelected = allFilteredIds.isNotEmpty() && allFilteredIds.all { it in selectedIds }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.space4, vertical = Spacing.space1),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${uiState.filteredAddCards.size} 张可添加", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                TextButton(onClick = {
                    selectedIds = if (allSelected) selectedIds - allFilteredIds else selectedIds + allFilteredIds
                }) { Text(if (allSelected) "取消全选" else "全选", color = Primary) }
            }

            if (uiState.filteredAddCards.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("没有可添加的卡片", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = Spacing.space4, vertical = Spacing.space2),
                    verticalArrangement = Arrangement.spacedBy(Spacing.space2)
                ) {
                    items(uiState.filteredAddCards, key = { it.id }) { card ->
                        val isSelected = card.id in selectedIds
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedIds = if (isSelected) selectedIds - card.id else selectedIds + card.id
                            },
                            colors = CardDefaults.cardColors(containerColor = if (isSelected) Primary.copy(alpha = 0.08f) else BgCard),
                            shape = RoundedCornerShape(CornerRadius.small)
                        ) {
                            Row(modifier = Modifier.padding(Spacing.space3), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = isSelected,
                                    onCheckedChange = { selectedIds = if (isSelected) selectedIds - card.id else selectedIds + card.id },
                                    colors = CheckboxDefaults.colors(checkedColor = Primary))
                                Spacer(Modifier.width(Spacing.space2))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                                        Surface(shape = RoundedCornerShape(6.dp), color = Primary.copy(alpha = 0.1f)) {
                                            Text(card.type.name, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall, color = Primary)
                                        }
                                        Text(card.chapter, style = MaterialTheme.typography.labelSmall, color = TextSecondary,
                                            modifier = Modifier.align(Alignment.CenterVertically))
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(card.prompt.replace(Regex("\\$[^$]+\\$"), "[公式]"),
                                        style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewCard(
    card: Card, isAnswerRevealed: Boolean,
    onRevealAnswer: () -> Unit, onRate: (Int) -> Unit, onSkip: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.pageHorizontal)) {
            Row(modifier = Modifier.padding(top = Spacing.space2), horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                NeutralTag(text = card.type.name)
                Text(card.chapter, style = MaterialTheme.typography.bodySmall, color = TextSecondary,
                    modifier = Modifier.align(Alignment.CenterVertically))
            }
            Spacer(modifier = Modifier.height(Spacing.space6))
            MathView(text = card.prompt, modifier = Modifier.fillMaxWidth(), baseFontSize = 22)
            Spacer(modifier = Modifier.height(Spacing.space5))
            if (isAnswerRevealed) {
                if (card.hint.isNotBlank()) {
                    Card(colors = CardDefaults.cardColors(containerColor = Warning.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()) {
                        MathView(text = card.hint, modifier = Modifier.fillMaxWidth().padding(Spacing.space3))
                    }
                    Spacer(modifier = Modifier.height(Spacing.space3))
                }
                MathView(text = card.answer, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(Spacing.space6))
            }
        }
        Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            if (!isAnswerRevealed) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.pageHorizontal, vertical = Spacing.space3),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    GhostButton(onClick = onSkip) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Skip")
                        Spacer(modifier = Modifier.width(Spacing.space1))
                        Text("跳过")
                    }
                    PrimaryButton(onClick = onRevealAnswer) { Text("显示答案") }
                }
            } else {
                RatingButtons(onRate = onRate,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.space4, vertical = Spacing.space3))
            }
        }
    }
}

@Composable
private fun RatingButtons(onRate: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
        FilledTonalButton(onClick = { onRate(0) }, modifier = Modifier.weight(1f).height(ButtonSize.secondaryHeight),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Error.copy(alpha = 0.15f), contentColor = Error)
        ) { Text("忘记", maxLines = 1) }
        FilledTonalButton(onClick = { onRate(2) }, modifier = Modifier.weight(1f).height(ButtonSize.secondaryHeight),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Warning.copy(alpha = 0.15f), contentColor = Warning)
        ) { Text("困难", maxLines = 1) }
        FilledTonalButton(onClick = { onRate(3) }, modifier = Modifier.weight(1f).height(ButtonSize.secondaryHeight),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Secondary.copy(alpha = 0.15f), contentColor = Secondary)
        ) { Text("记得", maxLines = 1) }
        FilledTonalButton(onClick = { onRate(5) }, modifier = Modifier.weight(1f).height(ButtonSize.secondaryHeight),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Primary.copy(alpha = 0.15f), contentColor = Primary)
        ) { Text("简单", maxLines = 1) }
    }
}

@Composable
private fun ErrorTagDialog(onConfirm: (ErrorType) -> Unit, onDismiss: () -> Unit) {
    var selectedType by remember { mutableStateOf<ErrorType?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择错因") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ErrorType.entries.forEach { type ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedType == type, onClick = { selectedType = type })
                        Text(errorTypeLabel(type), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { selectedType?.let { onConfirm(it) } }, enabled = selectedType != null) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("跳过") } }
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
private fun EmptyStateWithAdd(onAdd: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.DateRange, null, modifier = Modifier.size(64.dp), tint = TextSecondary.copy(alpha = 0.4f))
            Spacer(Modifier.height(Spacing.space4))
            Text("暂无待复习卡片", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
            Spacer(Modifier.height(Spacing.space2))
            Text("今日任务已完成，或还没有添加卡片", style = MaterialTheme.typography.bodyMedium, color = TextDisabled)
            Spacer(Modifier.height(Spacing.space6))
            Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                Icon(Icons.Default.PlaylistAdd, null)
                Spacer(Modifier.width(Spacing.space2))
                Text("手动添加复习")
            }
        }
    }
}

@Composable
private fun CompleteState(stats: SessionStats, onAddMore: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Default.CheckCircle, null, tint = Secondary, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(Spacing.space4))
            Text("今日复习完成！", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(Spacing.space3))
            Card(colors = CardDefaults.cardColors(containerColor = BgCard), shape = RoundedCornerShape(CornerRadius.card)) {
                Row(modifier = Modifier.padding(Spacing.space4), horizontalArrangement = Arrangement.spacedBy(Spacing.space8)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${stats.reviewed}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Primary)
                        Text("已复习", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${stats.correct}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Secondary)
                        Text("正确", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${stats.again}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Error)
                        Text("重学", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
            }
            Spacer(Modifier.height(Spacing.space6))
            OutlinedButton(onClick = onAddMore, colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)) {
                Icon(Icons.Default.PlaylistAdd, null)
                Spacer(Modifier.width(Spacing.space2))
                Text("继续加练")
            }
        }
    }
}
