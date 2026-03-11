package com.app.knowledgegraph.ui.practice

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddQuestionsScreen(
    folderId: Long,
    container: AppContainer,
    onNavigateBack: () -> Unit
) {
    val viewModel: AddQuestionsViewModel = viewModel(factory = AddQuestionsViewModel.factory(container))
    val filteredQuestions by viewModel.filteredQuestions.collectAsState()
    val existingIds by viewModel.existingIds.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val selectedSource by viewModel.selectedSource.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加题目") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.confirm(onNavigateBack) },
                        enabled = selectedIds.isNotEmpty()
                    ) {
                        Text(
                            if (selectedIds.isEmpty()) "确定" else "确定 (${selectedIds.size})",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("搜索题目...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )

            // Source filter chips
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedSource == null,
                    onClick = { viewModel.setSelectedSource(null) },
                    label = { Text("全部") }
                )
                sources.forEach { src ->
                    FilterChip(
                        selected = selectedSource == src,
                        onClick = {
                            viewModel.setSelectedSource(if (selectedSource == src) null else src)
                        },
                        label = { Text(src) }
                    )
                }
            }

            // Select all / deselect all
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = { viewModel.selectAll() }) {
                    Text("全选")
                }
                TextButton(onClick = { viewModel.deselectAll() }) {
                    Text("取消全选")
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "${filteredQuestions.size} 道题目",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }

            if (filteredQuestions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "暂无题目",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredQuestions, key = { it.id }) { question ->
                        val isExisting = question.id in existingIds
                        val isChecked = question.id in selectedIds

                        AddQuestionItem(
                            question = question,
                            isExisting = isExisting,
                            isChecked = isChecked || isExisting,
                            onToggle = { viewModel.toggleQuestion(question.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddQuestionItem(
    question: ImportedQuestion,
    isExisting: Boolean,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isExisting, onClick = onToggle),
        colors = if (isExisting) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (isExisting) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "已添加",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(end = 8.dp, top = 4.dp)
                        .size(24.dp)
                )
            } else {
                Checkbox(
                    checked = isChecked,
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
                                addQuestionTypeLabel(question.type),
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
                    if (isExisting) {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text("已添加", style = MaterialTheme.typography.labelSmall)
                            },
                            modifier = Modifier.height(24.dp),
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    question.stem,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isExisting) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private fun addQuestionTypeLabel(type: com.app.knowledgegraph.data.db.entity.QuestionType): String =
    when (type) {
        com.app.knowledgegraph.data.db.entity.QuestionType.SINGLE_CHOICE -> "单选"
        com.app.knowledgegraph.data.db.entity.QuestionType.MULTI_CHOICE -> "多选"
        com.app.knowledgegraph.data.db.entity.QuestionType.FILL_BLANK -> "填空"
        com.app.knowledgegraph.data.db.entity.QuestionType.TRUE_FALSE -> "判断"
    }
