package com.app.knowledgegraph.ui.scan

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.knowledgegraph.AppContainer
import com.app.knowledgegraph.data.db.entity.QuestionType
import com.app.knowledgegraph.ui.components.MathView
import com.app.knowledgegraph.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    var editStem by remember { mutableStateOf("") }
    var editAnswer by remember { mutableStateOf("") }
    var editExplanation by remember { mutableStateOf("") }
    var editOptionsJson by remember { mutableStateOf("") }
    var editSource by remember { mutableStateOf("") }
    var editType by remember { mutableStateOf(QuestionType.SINGLE_CHOICE) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(isEditMode) {
        if (isEditMode) {
            question?.let { q ->
                editStem = q.stem; editAnswer = q.answer; editExplanation = q.explanation
                editOptionsJson = q.optionsJson; editSource = q.source; editType = q.type
            }
        }
    }

    BackHandler(enabled = isEditMode) { isEditMode = false }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "编辑题目" else "题目详情") },
                navigationIcon = {
                    IconButton(onClick = { if (isEditMode) isEditMode = false else onNavigateBack() }) {
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
                                        container.scanRepository.updateQuestion(q.copy(
                                            stem = editStem, answer = editAnswer, explanation = editExplanation,
                                            optionsJson = editOptionsJson, source = editSource, type = editType
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
        val currentQuestion = question
        if (currentQuestion == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.space4),
            verticalArrangement = Arrangement.spacedBy(Spacing.space4)
        ) {
            Spacer(Modifier.height(Spacing.space1))

            /* ═══ 类型 & 来源 ═══ */
            if (isEditMode) {
                Text("题型", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuestionType.entries.forEach { type ->
                        FilterChip(selected = editType == type, onClick = { editType = type },
                            label = { Text(questionTypeLabel(type)) })
                    }
                }
                OutlinedTextField(value = editSource, onValueChange = { editSource = it },
                    label = { Text("来源") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space2), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(8.dp), color = questionTypeColor(currentQuestion.type).copy(alpha = 0.12f)) {
                        Text(questionTypeLabel(currentQuestion.type),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = questionTypeColor(currentQuestion.type), fontWeight = FontWeight.SemiBold)
                    }
                    if (currentQuestion.source.isNotBlank()) {
                        Surface(shape = RoundedCornerShape(8.dp), color = BgElevated) {
                            Text(currentQuestion.source,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        }
                    }
                }
            }

            /* ═══ 题干 ═══ */
            QSectionHeader(icon = Icons.Default.Description, title = "题干")
            if (isEditMode) {
                OutlinedTextField(value = editStem, onValueChange = { editStem = it },
                    modifier = Modifier.fillMaxWidth(), minLines = 3)
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(CornerRadius.card)
                ) {
                    MathView(text = currentQuestion.stem, figureSvg = currentQuestion.figureSvg,
                        modifier = Modifier.fillMaxWidth().padding(Spacing.space4), baseFontSize = 18)
                }
            }

            /* ═══ 选项 ═══ */
            if (currentQuestion.optionsJson.isNotBlank() || (isEditMode && editOptionsJson.isNotBlank())) {
                QSectionHeader(icon = Icons.Default.List, title = "选项")
                if (isEditMode) {
                    OutlinedTextField(value = editOptionsJson, onValueChange = { editOptionsJson = it },
                        label = { Text("选项 (JSON)") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                } else {
                    val options = parseOptions(currentQuestion.optionsJson)
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                        options.forEachIndexed { index, option ->
                            val label = ('A' + index).toString()
                            val isCorrect = currentQuestion.answer.contains(label)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCorrect) Secondary.copy(alpha = 0.10f) else BgCard
                                ),
                                shape = RoundedCornerShape(CornerRadius.small)
                            ) {
                                Row(
                                    modifier = Modifier.padding(Spacing.space3),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 选项字母圆圈
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isCorrect) Secondary.copy(alpha = 0.2f) else BgElevated,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(label, fontWeight = FontWeight.Bold,
                                                color = if (isCorrect) Secondary else TextSecondary,
                                                style = MaterialTheme.typography.labelLarge)
                                        }
                                    }
                                    Spacer(Modifier.width(Spacing.space3))
                                    MathView(text = option.removePrefix("$label.").removePrefix("$label. ").trim(),
                                        modifier = Modifier.weight(1f))
                                    // 正确项绿色勾
                                    if (isCorrect) {
                                        Spacer(Modifier.width(Spacing.space2))
                                        Icon(Icons.Default.CheckCircle, "正确",
                                            tint = Secondary, modifier = Modifier.size(22.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            /* ═══ 答案 ═══ */
            QSectionHeader(icon = Icons.Default.CheckCircleOutline, title = "答案")
            if (isEditMode) {
                OutlinedTextField(value = editAnswer, onValueChange = { editAnswer = it },
                    modifier = Modifier.fillMaxWidth(), minLines = 2)
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Secondary.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(CornerRadius.card)
                ) {
                    MathView(text = currentQuestion.answer,
                        modifier = Modifier.fillMaxWidth().padding(Spacing.space4), baseFontSize = 18)
                }
            }

            /* ═══ 解析 ═══ */
            if (currentQuestion.explanation.isNotBlank() || (isEditMode && editExplanation.isNotBlank())) {
                QSectionHeader(icon = Icons.Default.Lightbulb, title = "解析")
                if (isEditMode) {
                    OutlinedTextField(value = editExplanation, onValueChange = { editExplanation = it },
                        modifier = Modifier.fillMaxWidth(), minLines = 3)
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Warning.copy(alpha = 0.06f)),
                        shape = RoundedCornerShape(CornerRadius.card)
                    ) {
                        MathView(text = currentQuestion.explanation, figureSvg = currentQuestion.figureSvg,
                            modifier = Modifier.fillMaxWidth().padding(Spacing.space4))
                    }
                }
            }

            /* ═══ 练习统计 ═══ */
            if (!isEditMode) {
                QSectionHeader(icon = Icons.Default.BarChart, title = "练习统计")
                Card(
                    colors = CardDefaults.cardColors(containerColor = BgCard),
                    shape = RoundedCornerShape(CornerRadius.card)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(Spacing.space4),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        QStatItem("创建", dateFormat.format(Date(currentQuestion.createdAt)).substringBefore(" "), TextSecondary)
                        QStatItem("练习", "${currentQuestion.attemptCount}次", Primary)
                        QStatItem("正确率",
                            if (currentQuestion.attemptCount > 0) "${currentQuestion.correctCount * 100 / currentQuestion.attemptCount}%"
                            else "--",
                            Secondary)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除题目") },
            text = { Text("确定要删除这道题目吗？删除后不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    question?.let { scope.launch { container.scanRepository.deleteQuestion(it); onNavigateBack() } }
                }) { Text("删除", color = Error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun QSectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = TextSecondary)
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}

@Composable
private fun QStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

private fun questionTypeLabel(type: QuestionType): String = when (type) {
    QuestionType.SINGLE_CHOICE -> "单选"
    QuestionType.MULTI_CHOICE -> "多选"
    QuestionType.FILL_BLANK -> "填空"
    QuestionType.TRUE_FALSE -> "判断"
}

private fun questionTypeColor(type: QuestionType): Color = when (type) {
    QuestionType.SINGLE_CHOICE -> Primary
    QuestionType.MULTI_CHOICE -> Color(0xFF8E24AA)
    QuestionType.FILL_BLANK -> Warning
    QuestionType.TRUE_FALSE -> Secondary
}

private fun parseOptions(optionsJson: String): List<String> {
    if (optionsJson.isBlank()) return emptyList()
    return try {
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
                    c == ',' && !inString -> { result.add(current.toString().trim()); current = StringBuilder() }
                    else -> current.append(c)
                }
            }
            if (current.isNotEmpty()) result.add(current.toString().trim())
            result
        } else { listOf(optionsJson) }
    } catch (_: Exception) { listOf(optionsJson) }
}
