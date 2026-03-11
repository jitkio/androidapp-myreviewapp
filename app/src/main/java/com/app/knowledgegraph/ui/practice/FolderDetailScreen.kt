package com.app.knowledgegraph.ui.practice

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.app.knowledgegraph.data.db.entity.ImportedQuestion
import com.app.knowledgegraph.ui.components.cardShadow3d

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    folderId: Long,
    container: AppContainer,
    onNavigateBack: () -> Unit,
    onNavigateToAddQuestions: (Long) -> Unit
) {
    val viewModel: FolderDetailViewModel = viewModel(factory = FolderDetailViewModel.factory(container))
    val questions by viewModel.questions.collectAsState()

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSelectionMode) {
                        Text("已选 ${selectedIds.size} 项")
                    } else {
                        Text("题库详情")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSelectionMode) {
                            isSelectionMode = false
                            selectedIds = emptySet()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            if (isSelectionMode) Icons.Default.Close else Icons.Default.ArrowBack,
                            "返回"
                        )
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(
                            onClick = {
                                viewModel.removeQuestions(selectedIds.toList())
                                isSelectionMode = false
                                selectedIds = emptySet()
                            },
                            enabled = selectedIds.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Delete, "移除选中", tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        IconButton(onClick = { isSelectionMode = true }) {
                            Icon(Icons.Default.CheckBox, "多选")
                        }
                        IconButton(onClick = { onNavigateToAddQuestions(folderId) }) {
                            Icon(Icons.Default.Add, "添加题目")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (questions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Assignment,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "暂无题目",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = { onNavigateToAddQuestions(folderId) }) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("添加题目")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(questions, key = { it.id }) { question ->
                    FolderQuestionItem(
                        question = question,
                        isSelectionMode = isSelectionMode,
                        isSelected = question.id in selectedIds,
                        onToggle = {
                            if (isSelectionMode) {
                                selectedIds = if (question.id in selectedIds) {
                                    selectedIds - question.id
                                } else {
                                    selectedIds + question.id
                                }
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                isSelectionMode = true
                                selectedIds = setOf(question.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderQuestionItem(
    question: ImportedQuestion,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier.cardShadow3d()
            .fillMaxWidth()
            .animateContentSize()
            .then(
                if (isSelectionMode) {
                    Modifier.clickable(onClick = onToggle)
                } else {
                    Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = onLongClick
                    )
                }
            )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                questionTypeLabelForFolder(question.type),
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier.height(24.dp)
                    )
                    if (question.source.isNotBlank()) {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(question.source, style = MaterialTheme.typography.labelSmall)
                            },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    question.stem,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun questionTypeLabelForFolder(type: com.app.knowledgegraph.data.db.entity.QuestionType): String =
    when (type) {
        com.app.knowledgegraph.data.db.entity.QuestionType.SINGLE_CHOICE -> "单选"
        com.app.knowledgegraph.data.db.entity.QuestionType.MULTI_CHOICE -> "多选"
        com.app.knowledgegraph.data.db.entity.QuestionType.FILL_BLANK -> "填空"
        com.app.knowledgegraph.data.db.entity.QuestionType.TRUE_FALSE -> "判断"
    }
