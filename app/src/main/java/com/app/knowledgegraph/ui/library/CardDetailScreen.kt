package com.app.knowledgegraph.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.knowledgegraph.AppContainer
import com.app.knowledgegraph.data.db.entity.Card
import com.app.knowledgegraph.data.db.entity.CardType
import com.app.knowledgegraph.ui.components.MathView
import com.app.knowledgegraph.data.db.entity.Edge
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

    // Edit fields
    var editType by remember { mutableStateOf(CardType.CONCEPT) }
    var editChapter by remember { mutableStateOf("") }
    var editTags by remember { mutableStateOf("") }
    var editPrompt by remember { mutableStateOf("") }
    var editHint by remember { mutableStateOf("") }
    var editAnswer by remember { mutableStateOf("") }

    // Initialize edit fields when entering edit mode
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

    BackHandler(enabled = isEditMode) {
        isEditMode = false
    }

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
                    IconButton(onClick = {
                        if (isEditMode) isEditMode = false else onNavigateBack()
                    }) {
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
                                        container.cardRepository.updateCard(
                                            c.copy(
                                                type = editType,
                                                chapter = editChapter,
                                                tags = editTags,
                                                prompt = editPrompt,
                                                hint = editHint,
                                                answer = editAnswer
                                            )
                                        )
                                        isSaving = false
                                        isEditMode = false
                                    }
                                }
                            },
                            enabled = !isSaving
                        ) { Text("保存") }
                    } else {
                        IconButton(onClick = { isEditMode = true }) {
                            Icon(Icons.Default.Edit, "编辑")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, "删除")
                        }
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Type & Chapter
            if (isEditMode) {
                Text("卡片类型", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CardType.entries.forEach { type ->
                        FilterChip(
                            selected = editType == type,
                            onClick = { editType = type },
                            label = { Text(cardTypeLabel(type)) }
                        )
                    }
                }
                OutlinedTextField(
                    value = editChapter,
                    onValueChange = { editChapter = it },
                    label = { Text("章节") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = editTags,
                    onValueChange = { editTags = it },
                    label = { Text("标签（逗号分隔）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text(cardTypeLabel(currentCard.type)) })
                    Text(currentCard.chapter, style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.CenterVertically))
                }

                if (currentCard.tags.isNotBlank()) {
                    Text(
                        currentCard.tags.split(",").joinToString("  #") { it.trim() }.let { "#$it" },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            HorizontalDivider()

            // Prompt
            if (isEditMode) {
                OutlinedTextField(
                    value = editPrompt,
                    onValueChange = { editPrompt = it },
                    label = { Text("问题 (Prompt)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            } else {
                MathView(text = currentCard.prompt, modifier = Modifier.fillMaxWidth(), baseFontSize = 22)
            }

            HorizontalDivider()

            // Hint
            Text("提示", style = MaterialTheme.typography.labelLarge)
            if (isEditMode) {
                OutlinedTextField(
                    value = editHint,
                    onValueChange = { editHint = it },
                    label = { Text("提示 (Hint)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            } else {
                if (currentCard.hint.isNotBlank()) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                        MathView(text = currentCard.hint, modifier = Modifier.fillMaxWidth().padding(12.dp))
                    }
                } else {
                    Text("无", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Answer
            Text("答案", style = MaterialTheme.typography.labelLarge)
            if (isEditMode) {
                OutlinedTextField(
                    value = editAnswer,
                    onValueChange = { editAnswer = it },
                    label = { Text("答案 (Answer)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5
                )
            } else {
                MathView(text = currentCard.answer, modifier = Modifier.fillMaxWidth())
            }

            HorizontalDivider()

            // Only show review status and edges in display mode
            if (!isEditMode) {
                schedule?.let { sch ->
                    Text("复习状态", style = MaterialTheme.typography.labelLarge)
                    Card {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("状态: ${sch.status.name}")
                            Text("间隔: ${sch.interval} 天")
                            Text("难度因子: ${"%.2f".format(sch.easeFactor)}")
                            Text("复习次数: ${sch.totalReviews} (正确 ${sch.correctCount})")
                        }
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("连接关系 (${edges.size})", style = MaterialTheme.typography.labelLarge)
                    IconButton(onClick = { onNavigateToAddEdge(cardId) }) {
                        Icon(Icons.Default.Add, "添加连接")
                    }
                }

                if (edges.isEmpty()) {
                    Text("暂无连接，点击 + 添加", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    edges.forEach { edge ->
                        val otherId = if (edge.sourceId == cardId) edge.targetId else edge.sourceId
                        val otherCard = linkedCards[otherId]
                        val isSource = edge.sourceId == cardId
                        val dirLabel = if (isSource) "→" else "←"

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToCard(otherId) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        AssistChip(onClick = {}, label = {
                                            Text(relationTypeLabel(edge.relation), style = MaterialTheme.typography.labelSmall)
                                        }, modifier = Modifier.height(24.dp))
                                        Text(dirLabel, style = MaterialTheme.typography.labelMedium)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        otherCard?.prompt ?: "加载中...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (edge.description.isNotBlank()) {
                                        Text(edge.description, style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                IconButton(onClick = { edgeToDelete = edge }) {
                                    Icon(Icons.Default.Close, "删除连接", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除卡片") },
            text = { Text("确定要删除这张卡片吗？相关的复习记录和连接关系也会被删除。") },
            confirmButton = {
                TextButton(onClick = {
                    card?.let {
                        scope.launch {
                            container.cardRepository.deleteCard(it)
                            onNavigateBack()
                        }
                    }
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }

    edgeToDelete?.let { edge ->
        AlertDialog(
            onDismissRequest = { edgeToDelete = null },
            title = { Text("删除连接") },
            text = { Text("确定要删除这条连接关系吗？") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        container.graphRepository.deleteEdge(edge)
                        edgeToDelete = null
                    }
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { edgeToDelete = null }) { Text("取消") }
            }
        )
    }
}
