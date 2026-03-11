package com.app.knowledgegraph.ui.scan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.knowledgegraph.AppContainer
import com.app.knowledgegraph.data.db.entity.QuestionType
import com.app.knowledgegraph.ui.components.MathView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.app.knowledgegraph.ui.components.cardShadow3d

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionDetailScreen(
    questionId: Long,
    container: AppContainer,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val question by container.scanRepository.observeById(questionId).collectAsState(initial = null)
    var isEditMode by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Edit fields
    var editStem by remember { mutableStateOf("") }
    var editAnswer by remember { mutableStateOf("") }
    var editExplanation by remember { mutableStateOf("") }
    var editOptionsJson by remember { mutableStateOf("") }
    var editSource by remember { mutableStateOf("") }
    var editType by remember { mutableStateOf(QuestionType.SINGLE_CHOICE) }
    var isSaving by remember { mutableStateOf(false) }

    // Initialize edit fields when entering edit mode
    LaunchedEffect(isEditMode) {
        if (isEditMode) {
            question?.let { q ->
                editStem = q.stem
                editAnswer = q.answer
                editExplanation = q.explanation
                editOptionsJson = q.optionsJson
                editSource = q.source
                editType = q.type
            }
        }
    }

    BackHandler(enabled = isEditMode) {
        isEditMode = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "编辑题目" else "题目详情") },
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
                                question?.let { q ->
                                    isSaving = true
                                    scope.launch {
                                        container.scanRepository.updateQuestion(
                                            q.copy(
                                                stem = editStem,
                                                answer = editAnswer,
                                                explanation = editExplanation,
                                                optionsJson = editOptionsJson,
                                                source = editSource,
                                                type = editType
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
        val currentQuestion = question
        if (currentQuestion == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding), contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Type & Source chips
            if (isEditMode) {
                Text("题型", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuestionType.entries.forEach { type ->
                        FilterChip(
                            selected = editType == type,
                            onClick = { editType = type },
                            label = { Text(questionTypeLabel(type)) }
                        )
                    }
                }
                OutlinedTextField(
                    value = editSource,
                    onValueChange = { editSource = it },
                    label = { Text("来源") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = {},
                        label = { Text(questionTypeLabel(currentQuestion.type)) }
                    )
                    if (currentQuestion.source.isNotBlank()) {
                        AssistChip(
                            onClick = {},
                            label = { Text(currentQuestion.source) }
                        )
                    }
                }
            }

            HorizontalDivider()

            // Stem
            Text("题干", style = MaterialTheme.typography.labelLarge)
            if (isEditMode) {
                OutlinedTextField(
                    value = editStem,
                    onValueChange = { editStem = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            } else {
                MathView(
                    text = currentQuestion.stem,
                    modifier = Modifier.fillMaxWidth(),
                    baseFontSize = 16
                )
            }

            // Options
            if (currentQuestion.optionsJson.isNotBlank() || (isEditMode && editOptionsJson.isNotBlank())) {
                HorizontalDivider()
                Text("选项", style = MaterialTheme.typography.labelLarge)
                if (isEditMode) {
                    OutlinedTextField(
                        value = editOptionsJson,
                        onValueChange = { editOptionsJson = it },
                        label = { Text("选项 (JSON)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                } else {
                    val options = parseOptions(currentQuestion.optionsJson)
                    options.forEachIndexed { index, option ->
                        val label = ('A' + index).toString()
                        Card(
                            modifier = Modifier.cardShadow3d().fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (currentQuestion.answer.contains(label))
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Row(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "$label. ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                MathView(text = option, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // Answer
            Text("答案", style = MaterialTheme.typography.labelLarge)
            if (isEditMode) {
                OutlinedTextField(
                    value = editAnswer,
                    onValueChange = { editAnswer = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            } else {
                MathView(
                    text = currentQuestion.answer,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Explanation
            if (currentQuestion.explanation.isNotBlank() || (isEditMode && editExplanation.isNotBlank())) {
                HorizontalDivider()
                Text("解析", style = MaterialTheme.typography.labelLarge)
                if (isEditMode) {
                    OutlinedTextField(
                        value = editExplanation,
                        onValueChange = { editExplanation = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                } else {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        MathView(
                            text = currentQuestion.explanation,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        )
                    }
                }
            }

            HorizontalDivider()

            // Stats
            Text("练习统计", style = MaterialTheme.typography.labelLarge)
            Card {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("创建时间: ${dateFormat.format(Date(currentQuestion.createdAt))}")
                    Text("练习次数: ${currentQuestion.attemptCount}")
                    if (currentQuestion.attemptCount > 0) {
                        val rate = currentQuestion.correctCount * 100 / currentQuestion.attemptCount
                        Text("正确率: $rate% (${currentQuestion.correctCount}/${currentQuestion.attemptCount})")
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除题目") },
            text = { Text("确定要删除这道题目吗？删除后不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    question?.let {
                        scope.launch {
                            container.scanRepository.deleteQuestion(it)
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
}

private fun questionTypeLabel(type: QuestionType): String = when (type) {
    QuestionType.SINGLE_CHOICE -> "单选"
    QuestionType.MULTI_CHOICE -> "多选"
    QuestionType.FILL_BLANK -> "填空"
    QuestionType.TRUE_FALSE -> "判断"
}

private fun parseOptions(optionsJson: String): List<String> {
    if (optionsJson.isBlank()) return emptyList()
    return try {
        // Simple JSON array parsing: ["opt1","opt2",...]
        val trimmed = optionsJson.trim()
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            val inner = trimmed.substring(1, trimmed.length - 1)
            val result = mutableListOf<String>()
            var current = StringBuilder()
            var inString = false
            var escaped = false
            for (c in inner) {
                when {
                    escaped -> { current.append(c); escaped = false }
                    c == '\\' -> { escaped = true }
                    c == '"' -> inString = !inString
                    c == ',' && !inString -> {
                        result.add(current.toString().trim())
                        current = StringBuilder()
                    }
                    else -> current.append(c)
                }
            }
            if (current.isNotEmpty()) result.add(current.toString().trim())
            result
        } else {
            listOf(optionsJson)
        }
    } catch (_: Exception) {
        listOf(optionsJson)
    }
}
