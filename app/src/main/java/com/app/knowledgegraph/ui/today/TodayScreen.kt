package com.app.knowledgegraph.ui.today

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Column(modifier = Modifier.fillMaxSize().background(BgBase)) {
        // ★ 顶栏：带统计摘要
        Surface(
            color = BgCard,
            shadowElevation = 4.dp
        ) {
            Column {
                TopAppBar(
                    title = { Text("Today", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                    windowInsets = WindowInsets(0),
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    actions = {
                        IconButton(onClick = { viewModel.openAddCards() }) {
                            Icon(Icons.Default.PlaylistAdd, contentDescription = "添加复习卡片", tint = Primary)
                        }
                    }
                )
                // ★ 进度条摘要栏
                if (!uiState.isLoading && uiState.queue.isNotEmpty()) {
                    val total = uiState.queue.size
                    val done = uiState.sessionStats.reviewed
                    val progress = if (total > 0) done.toFloat() / total else 0f
                    Column(modifier = Modifier.padding(horizontal = Spacing.space4, vertical = Spacing.space2)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("已复习 $done / $total", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            Text("剩余 ${uiState.remaining}", style = MaterialTheme.typography.labelMedium, color = Primary, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp)
                                .shadow(2.dp, RoundedCornerShape(3.dp)),
                            color = Primary,
                            trackColor = Primary.copy(alpha = 0.10f),
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        Spacer(Modifier.height(Spacing.space2))
                    }
                }
            }
        }

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

/* ═══ 复习卡片 — 3D 风格 ═══ */

@Composable
private fun ReviewCard(
    card: Card, isAnswerRevealed: Boolean,
    onRevealAnswer: () -> Unit, onRate: (Int) -> Unit, onSkip: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.space4, vertical = Spacing.space3)
        ) {
            // 类型 & 章节
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Primary.copy(alpha = 0.12f),
                    shadowElevation = 2.dp
                ) {
                    Text(card.type.name, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium, color = Primary, fontWeight = FontWeight.SemiBold)
                }
                Text(card.chapter, style = MaterialTheme.typography.bodySmall, color = TextSecondary,
                    modifier = Modifier.align(Alignment.CenterVertically))
            }

            Spacer(Modifier.height(Spacing.space4))

            // ★ 问题卡片 — 3D 悬浮
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(CornerRadius.card),
                        ambientColor = Primary.copy(alpha = 0.08f),
                        spotColor = Primary.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(CornerRadius.card),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                // 顶部渐变色条
                Box(
                    modifier = Modifier.fillMaxWidth().height(4.dp)
                        .background(Brush.horizontalGradient(listOf(Primary, Secondary)))
                )
                MathView(
                    text = card.prompt,
                    modifier = Modifier.fillMaxWidth().padding(Spacing.space4),
                    baseFontSize = 22
                )
            }

            Spacer(Modifier.height(Spacing.space4))

            if (isAnswerRevealed) {
                // ★ 提示卡片
                if (card.hint.isNotBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(CornerRadius.card),
                                ambientColor = Warning.copy(alpha = 0.10f),
                                spotColor = Warning.copy(alpha = 0.15f)),
                        colors = CardDefaults.cardColors(containerColor = Warning.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(CornerRadius.card),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(modifier = Modifier.padding(Spacing.space3), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Lightbulb, null, tint = Warning, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                            Spacer(Modifier.width(Spacing.space2))
                            MathView(text = card.hint, modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(Spacing.space3))
                }

                // ★ 答案卡片
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(CornerRadius.card),
                            ambientColor = Color.Black.copy(alpha = 0.06f),
                            spotColor = Color.Black.copy(alpha = 0.10f)),
                    colors = CardDefaults.cardColors(containerColor = BgCard),
                    shape = RoundedCornerShape(CornerRadius.card),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(Spacing.space4)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircleOutline, null, tint = Secondary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(Spacing.space2))
                            Text("答案", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Secondary)
                        }
                        Spacer(Modifier.height(Spacing.space2))
                        MathView(text = card.answer, modifier = Modifier.fillMaxWidth())
                    }
                }
                Spacer(Modifier.height(Spacing.space4))
            }
        }

        // ★ 底部操作栏 — 3D 浮起
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    ambientColor = Color.Black.copy(alpha = 0.05f),
                    spotColor = Color.Black.copy(alpha = 0.08f)),
            color = BgCard,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            if (!isAnswerRevealed) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.space4, vertical = Spacing.space3),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
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

/* ═══ 评分按钮 — 3D 按压 ═══ */

@Composable
private fun RatingButtons(onRate: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
        RatingBtn("忘记", Error, 0, onRate, Modifier.weight(1f))
        RatingBtn("困难", Warning, 2, onRate, Modifier.weight(1f))
        RatingBtn("记得", Secondary, 3, onRate, Modifier.weight(1f))
        RatingBtn("简单", Primary, 5, onRate, Modifier.weight(1f))
    }
}

@Composable
private fun RatingBtn(label: String, color: Color, quality: Int, onRate: (Int) -> Unit, modifier: Modifier) {
    Button(
        onClick = { onRate(quality) },
        modifier = modifier
            .height(ButtonSize.secondaryHeight)
            .shadow(4.dp, RoundedCornerShape(CornerRadius.small),
                ambientColor = color.copy(alpha = 0.15f),
                spotColor = color.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(CornerRadius.small),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.12f),
            contentColor = color
        ),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Text(label, maxLines = 1, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

/* ═══ 添加卡片 BottomSheet ═══ */

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
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Text("添加复习卡片", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (selectedIds.isNotEmpty()) {
                    Button(onClick = { onAddCards(uiState.filteredAddCards.filter { it.id in selectedIds }) },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.shadow(4.dp, RoundedCornerShape(12.dp), ambientColor = Primary.copy(alpha = 0.2f), spotColor = Primary.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("添加 ${selectedIds.size} 张") }
                }
            }

            OutlinedTextField(
                value = uiState.addSearchQuery, onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.space4, vertical = Spacing.space1),
                placeholder = { Text("搜索卡片...") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true, shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Border, focusedBorderColor = Primary)
            )

            if (uiState.addChapters.isNotEmpty()) {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.space4, vertical = Spacing.space1),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                    FilterChip(selected = uiState.addSelectedChapter == null, onClick = { onChapterSelect(null) },
                        label = { Text("全部") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Primary.copy(alpha = 0.15f), selectedLabelColor = Primary))
                    uiState.addChapters.forEach { ch ->
                        FilterChip(selected = uiState.addSelectedChapter == ch, onClick = { onChapterSelect(ch) },
                            label = { Text(ch) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Primary.copy(alpha = 0.15f), selectedLabelColor = Primary))
                    }
                }
            }

            val allFilteredIds = uiState.filteredAddCards.map { it.id }.toSet()
            val allSelected = allFilteredIds.isNotEmpty() && allFilteredIds.all { it in selectedIds }
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.space4, vertical = Spacing.space1),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
                LazyColumn(modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = Spacing.space4, vertical = Spacing.space2),
                    verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                    items(uiState.filteredAddCards, key = { it.id }) { card ->
                        val isSelected = card.id in selectedIds
                        Card(
                            modifier = Modifier.fillMaxWidth()
                                .shadow(if (isSelected) 4.dp else 2.dp, RoundedCornerShape(CornerRadius.small),
                                    ambientColor = if (isSelected) Primary.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f),
                                    spotColor = if (isSelected) Primary.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f))
                                .clickable { selectedIds = if (isSelected) selectedIds - card.id else selectedIds + card.id },
                            colors = CardDefaults.cardColors(containerColor = if (isSelected) Primary.copy(alpha = 0.06f) else BgCard),
                            shape = RoundedCornerShape(CornerRadius.small),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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

/* ═══ 错因对话框 ═══ */

@Composable
private fun ErrorTagDialog(onConfirm: (ErrorType) -> Unit, onDismiss: () -> Unit) {
    var selectedType by remember { mutableStateOf<ErrorType?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("选择错因") },
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

/* ═══ 空状态 ═══ */

@Composable
private fun EmptyStateWithAdd(onAdd: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Card(
                modifier = Modifier.size(96.dp)
                    .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = Primary.copy(alpha = 0.08f), spotColor = Primary.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.08f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.DateRange, null, modifier = Modifier.size(48.dp), tint = Primary.copy(alpha = 0.5f))
                }
            }
            Spacer(Modifier.height(Spacing.space5))
            Text("暂无待复习卡片", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(Modifier.height(Spacing.space2))
            Text("今日任务已完成，或还没有添加卡片", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(Modifier.height(Spacing.space6))
            Button(
                onClick = onAdd,
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                modifier = Modifier.shadow(6.dp, RoundedCornerShape(CornerRadius.medium), ambientColor = Primary.copy(alpha = 0.2f), spotColor = Primary.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(CornerRadius.medium)
            ) {
                Icon(Icons.Default.PlaylistAdd, null)
                Spacer(Modifier.width(Spacing.space2))
                Text("手动添加复习")
            }
        }
    }
}

/* ═══ 完成状态 ═══ */

@Composable
private fun CompleteState(stats: SessionStats, onAddMore: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            // ★ 成功图标 3D
            Card(
                modifier = Modifier.size(96.dp)
                    .shadow(10.dp, RoundedCornerShape(24.dp), ambientColor = Secondary.copy(alpha = 0.15f), spotColor = Secondary.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Secondary.copy(alpha = 0.10f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CheckCircle, null, tint = Secondary, modifier = Modifier.size(52.dp))
                }
            }
            Spacer(Modifier.height(Spacing.space5))
            Text("今日复习完成！", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(Spacing.space4))

            // ★ 统计卡片 3D
            Card(
                modifier = Modifier
                    .shadow(8.dp, RoundedCornerShape(CornerRadius.card),
                        ambientColor = Color.Black.copy(alpha = 0.06f), spotColor = Color.Black.copy(alpha = 0.10f)),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                shape = RoundedCornerShape(CornerRadius.card),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(modifier = Modifier.padding(Spacing.space5), horizontalArrangement = Arrangement.spacedBy(Spacing.space8)) {
                    StatColumn("${stats.reviewed}", "已复习", Primary)
                    StatColumn("${stats.correct}", "正确", Secondary)
                    StatColumn("${stats.again}", "重学", Error)
                }
            }

            Spacer(Modifier.height(Spacing.space6))
            OutlinedButton(
                onClick = onAddMore,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                modifier = Modifier.shadow(3.dp, RoundedCornerShape(CornerRadius.medium), ambientColor = Primary.copy(alpha = 0.1f), spotColor = Primary.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(CornerRadius.medium)
            ) {
                Icon(Icons.Default.PlaylistAdd, null)
                Spacer(Modifier.width(Spacing.space2))
                Text("继续加练")
            }
        }
    }
}

@Composable
private fun StatColumn(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}
