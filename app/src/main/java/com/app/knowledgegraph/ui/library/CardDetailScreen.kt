package com.app.knowledgegraph.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.knowledgegraph.AppContainer
import com.app.knowledgegraph.data.db.entity.Card
import com.app.knowledgegraph.data.db.entity.CardType
import com.app.knowledgegraph.data.db.entity.ReviewStatus
import com.app.knowledgegraph.ui.components.MathView
import com.app.knowledgegraph.data.db.entity.Edge
import com.app.knowledgegraph.ui.theme.*
import com.app.knowledgegraph.ui.components.TtsButton
import com.app.knowledgegraph.ui.components.shouldShowTts
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    cardId: Long,
    container: AppContainer,
    onNavigateBack: () -> Unit,
    onNavigateToAddEdge: (Long) -> Unit = {},
    onNavigateToCard: (Long) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val card by container.cardRepository.observeById(cardId).collectAsState(initial = null)
    val edges by container.graphRepository.observeNeighborEdges(cardId).collectAsState(initial = emptyList())
    val schedule by container.reviewRepository.observeScheduleByCard(cardId).collectAsState(initial = null)
    var showDeleteDialog by remember { mutableStateOf(false) }
    var edgeToDelete by remember { mutableStateOf<Edge?>(null) }
    var isEditMode by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    var editType by remember { mutableStateOf(CardType.CONCEPT) }
    var editChapter by remember { mutableStateOf("") }
    var editTags by remember { mutableStateOf("") }
    var editPrompt by remember { mutableStateOf("") }
    var editHint by remember { mutableStateOf("") }
    var editAnswer by remember { mutableStateOf("") }

    LaunchedEffect(isEditMode) {
        if (isEditMode) {
            card?.let { c ->
                editType = c.type
                editChapter = c.chapter
                editTags = c.tags
                editPrompt = c.prompt
                editHint = c.hint
                editAnswer = c.answer
            }
        }
    }

    BackHandler(enabled = isEditMode) { isEditMode = false }

    val linkedCards = remember { mutableStateMapOf<Long, Card?>() }
    LaunchedEffect(edges) {
        edges.forEach { edge ->
            val otherId = if (edge.sourceId == cardId) edge.targetId else edge.sourceId
            if (otherId !in linkedCards) {
                linkedCards[otherId] = container.cardRepository.getById(otherId)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "编辑卡片" else "卡片详情") },
                navigationIcon = {
                    IconButton(onClick = { if (isEditMode) isEditMode = false else onNavigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    if (isEditMode) {
                        TextButton(
                            onClick = {
                                card?.let { c ->
                                    isSaving = true
                                    scope.launch {
                                        container.cardRepository.updateCard(c.copy(
                                            type = editType, chapter = editChapter, tags = editTags,
                                            prompt = editPrompt, hint = editHint, answer = editAnswer
                                        ))
                                        isSaving = false; isEditMode = false
                                    }
                                }
                            },
                            enabled = !isSaving
                        ) { Text("保存") }
                    } else {
                        IconButton(onClick = { isEditMode = true }) { Icon(Icons.Default.Edit, "编辑") }
                        IconButton(onClick = { showDeleteDialog = true }) { Icon(Icons.Default.Delete, "删除") }
                    }
                }
            )
        }
    ) { innerPadding ->
        val currentCard = card
        if (currentCard == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.space4),
            verticalArrangement = Arrangement.spacedBy(Spacing.space4)
        ) {
            Spacer(Modifier.height(Spacing.space1))

            /* ═══ 类型 & 章节 & 标签 ═══ */
            if (isEditMode) {
                Text("卡片类型", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CardType.entries.forEach { type ->
                        FilterChip(selected = editType == type, onClick = { editType = type },
                            label = { Text(cardTypeLabel(type)) })
                    }
                }
                OutlinedTextField(value = editChapter, onValueChange = { editChapter = it },
                    label = { Text("章节") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = editTags, onValueChange = { editTags = it },
                    label = { Text("标签（逗号分隔）") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.space2),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = cardTypeColor(currentCard.type).copy(alpha = 0.12f)
                    ) {
                        Text(
                            cardTypeLabel(currentCard.type),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = cardTypeColor(currentCard.type),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(currentCard.chapter, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                if (currentCard.tags.isNotBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        currentCard.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Primary.copy(alpha = 0.08f)
                            ) {
                                Text("#$tag", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall, color = Primary)
                            }
                        }
                    }
                }
            }

            /* ═══ 问题（Prompt）— 醒目卡片 ═══ */
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                SectionHeader(icon = Icons.Default.HelpOutline, title = "问题")
                if (shouldShowTts(currentCard.chapter, currentCard.prompt)) {
                    TtsButton(text = currentCard.prompt, iconSize = 22.dp)
                }
            }
            if (isEditMode) {
                OutlinedTextField(value = editPrompt, onValueChange = { editPrompt = it },
                    label = { Text("问题 (Prompt)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(CornerRadius.card)
                ) {
                    MathView(
                        text = currentCard.prompt,
                        modifier = Modifier.fillMaxWidth().padding(Spacing.space4),
                        baseFontSize = 20
                    )
                }
            }

            /* ═══ 提示（Hint）═══ */
            if (isEditMode || currentCard.hint.isNotBlank()) {
                SectionHeader(icon = Icons.Default.Lightbulb, title = "提示")
                if (isEditMode) {
                    OutlinedTextField(value = editHint, onValueChange = { editHint = it },
                        label = { Text("提示 (Hint)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Warning.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(CornerRadius.card)
                    ) {
                        MathView(
                            text = currentCard.hint,
                            modifier = Modifier.fillMaxWidth().padding(Spacing.space4)
                        )
                    }
                }
            }

            /* ═══ 答案（Answer）— 最重要的区域 ═══ */
            SectionHeader(icon = Icons.Default.CheckCircleOutline, title = "答案")
            if (isEditMode) {
                OutlinedTextField(value = editAnswer, onValueChange = { editAnswer = it },
                    label = { Text("答案 (Answer)") }, modifier = Modifier.fillMaxWidth(), minLines = 5)
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BgCard),
                    shape = RoundedCornerShape(CornerRadius.card),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    MathView(
                        text = currentCard.answer,
                        modifier = Modifier.fillMaxWidth().padding(Spacing.space4),
                        baseFontSize = 17
                    )
                }
            }

            // 以下只在查看模式显示
            if (!isEditMode) {
                /* ═══ 复习状态 ═══ */
                schedule?.let { sch ->
                    SectionHeader(icon = Icons.Default.Timeline, title = "复习状态")
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BgCard),
                        shape = RoundedCornerShape(CornerRadius.card)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(Spacing.space4),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ReviewStatItem(
                                label = "状态",
                                value = reviewStatusLabel(sch.status),
                                color = reviewStatusColor(sch.status)
                            )
                            ReviewStatItem(
                                label = "间隔",
                                value = "${sch.interval}天",
                                color = Primary
                            )
                            ReviewStatItem(
                                label = "难度",
                                value = "%.1f".format(sch.easeFactor),
                                color = TextPrimary
                            )
                            ReviewStatItem(
                                label = "正确率",
                                value = if (sch.totalReviews > 0) "${sch.correctCount * 100 / sch.totalReviews}%" else "--",
                                color = Secondary
                            )
                        }
                    }
                }

                /* ═══ 连接关系 ═══ */
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(icon = Icons.Default.Hub, title = "连接关系 (${edges.size})")
                    IconButton(onClick = { onNavigateToAddEdge(cardId) }) {
                        Icon(Icons.Default.Add, "添加连接", tint = Primary)
                    }
                }

                if (edges.isEmpty()) {
                    Text("暂无连接，点击 + 添加",
                        style = MaterialTheme.typography.bodyMedium, color = TextSecondary,
                        modifier = Modifier.padding(start = Spacing.space2))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                        edges.forEach { edge ->
                            val otherId = if (edge.sourceId == cardId) edge.targetId else edge.sourceId
                            val otherCard = linkedCards[otherId]
                            val isSource = edge.sourceId == cardId

                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { onNavigateToCard(otherId) },
                                colors = CardDefaults.cardColors(containerColor = BgCard),
                                shape = RoundedCornerShape(CornerRadius.small)
                            ) {
                                Row(
                                    modifier = Modifier.padding(Spacing.space3),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 关系方向指示
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Primary.copy(alpha = 0.1f)
                                    ) {
                                        Text(
                                            text = "${relationTypeLabel(edge.relation)} ${if (isSource) "→" else "←"}",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Spacer(Modifier.width(Spacing.space3))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            otherCard?.prompt ?: "加载中...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 2
                                        )
                                        if (edge.description.isNotBlank()) {
                                            Text(edge.description, style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary, maxLines = 1)
                                        }
                                    }
                                    IconButton(onClick = { edgeToDelete = edge }) {
                                        Icon(Icons.Default.Close, "删除", tint = Error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Dialogs
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除卡片") },
            text = { Text("确定要删除这张卡片吗？相关的复习记录和连接关系也会被删除。") },
            confirmButton = {
                TextButton(onClick = {
                    card?.let { scope.launch { container.cardRepository.deleteCard(it); onNavigateBack() } }
                }) { Text("删除", color = Error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("取消") } }
        )
    }

    edgeToDelete?.let { edge ->
        AlertDialog(
            onDismissRequest = { edgeToDelete = null },
            title = { Text("删除连接") },
            text = { Text("确定要删除这条连接关系吗？") },
            confirmButton = {
                TextButton(onClick = { scope.launch { container.graphRepository.deleteEdge(edge); edgeToDelete = null } }) {
                    Text("删除", color = Error)
                }
            },
            dismissButton = { TextButton(onClick = { edgeToDelete = null }) { Text("取消") } }
        )
    }
}

/* ── 小组件 ────────────────────────────────────────── */

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.space2)
    ) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = TextSecondary)
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}

@Composable
private fun ReviewStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

private fun cardTypeColor(type: CardType): Color = when (type) {
    CardType.CONCEPT -> Primary
    CardType.METHOD -> Secondary
    CardType.TEMPLATE -> Warning
    CardType.BOUNDARY -> Error
}

private fun reviewStatusLabel(status: ReviewStatus): String = when (status) {
    ReviewStatus.NEW -> "新卡"
    ReviewStatus.LEARNING -> "学习中"
    ReviewStatus.REVIEW -> "复习中"
    ReviewStatus.RELEARNING -> "重学"
}

private fun reviewStatusColor(status: ReviewStatus): Color = when (status) {
    ReviewStatus.NEW -> Primary
    ReviewStatus.LEARNING -> Warning
    ReviewStatus.REVIEW -> Secondary
    ReviewStatus.RELEARNING -> Error
}

