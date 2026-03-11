package com.app.knowledgegraph.ui.practice

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.knowledgegraph.AppContainer
import com.app.knowledgegraph.domain.practice.ConditionCheck
import com.app.knowledgegraph.domain.practice.PracticeQuestion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    container: AppContainer,
    viewModel: PracticeViewModel = viewModel(factory = PracticeViewModel.factory(container))
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Practice") },
            actions = {
                if (uiState.sessionTotal > 0) {
                    Text(
                        "${uiState.sessionCorrect}/${uiState.sessionTotal}",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            }
        )

        if (uiState.questions.isNotEmpty() && uiState.phase != PracticePhase.COMPLETE) {
            LinearProgressIndicator(
                progress = { uiState.progress },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
        }

        when (uiState.phase) {
            PracticePhase.QUESTION -> {
                uiState.currentQuestion?.let { question ->
                    QuestionPhase(
                        question = question,
                        onSubmit = { viewModel.submitChoice(it) }
                    )
                }
            }
            PracticePhase.FEEDBACK -> {
                uiState.currentQuestion?.let { question ->
                    FeedbackPhase(
                        question = question,
                        userChoice = uiState.userChoice ?: "",
                        isCorrect = uiState.isCorrect,
                        boundaryCardCreated = uiState.boundaryCardCreated,
                        onNext = { viewModel.nextQuestion() }
                    )
                }
            }
            PracticePhase.COMPLETE -> {
                CompletePhase(
                    correct = uiState.sessionCorrect,
                    total = uiState.sessionTotal,
                    onRestart = { viewModel.startSession() }
                )
            }
        }
    }
}

@Composable
fun QuestionPhase(question: PracticeQuestion, onSubmit: (String) -> Unit) {
    var selected by remember(question.id) { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("题干", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(question.stem, style = MaterialTheme.typography.bodyLarge)
            }
        }

        Text("选择最佳方法：", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

        question.methods.forEach { method ->
            val isSelected = selected == method
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selected = method }
                    .then(
                        if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                        else Modifier
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = isSelected, onClick = { selected = method })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(method, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { selected?.let { onSubmit(it) } },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = selected != null
        ) { Text("确认选择", style = MaterialTheme.typography.titleSmall) }
    }
}

@Composable
fun FeedbackPhase(
    question: PracticeQuestion,
    userChoice: String,
    isCorrect: Boolean,
    boundaryCardCreated: Boolean,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isCorrect) Color(0xFF1B5E20).copy(alpha = 0.15f)
                else Color(0xFFB71C1C).copy(alpha = 0.15f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    null,
                    tint = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        if (isCorrect) "回答正确！" else "回答错误",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (!isCorrect) {
                        Text("你选了: $userChoice", style = MaterialTheme.typography.bodyMedium)
                        Text("正确: ${question.correctMethod}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50))
                    }
                }
            }
        }

        if (boundaryCardCreated) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NoteAdd, null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("已自动生成边界卡，加入Today复习队列",
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Text("触发词分析", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Card {
            Column(modifier = Modifier.padding(12.dp)) {
                val annotated = buildAnnotatedString {
                    var text = question.stem
                    val sortedTriggers = question.triggerWords.keys.sortedByDescending { it.length }
                    val highlights = mutableListOf<Triple<Int, Int, String>>()

                    for (trigger in sortedTriggers) {
                        var idx = text.indexOf(trigger)
                        while (idx >= 0) {
                            val overlaps = highlights.any { (s, e, _) ->
                                idx < e && idx + trigger.length > s
                            }
                            if (!overlaps) {
                                highlights.add(Triple(idx, idx + trigger.length, trigger))
                            }
                            idx = text.indexOf(trigger, idx + 1)
                        }
                    }
                    highlights.sortBy { it.first }

                    var lastEnd = 0
                    for ((start, end, _) in highlights) {
                        if (start > lastEnd) append(text.substring(lastEnd, start))
                        withStyle(SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            background = MaterialTheme.colorScheme.primaryContainer
                        )) {
                            append(text.substring(start, end))
                        }
                        lastEnd = end
                    }
                    if (lastEnd < text.length) append(text.substring(lastEnd))
                }
                Text(annotated, style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                question.triggerWords.forEach { (word, reason) ->
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text("[$word]",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(reason, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Text("适用条件检查", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Card {
            Column(modifier = Modifier.padding(12.dp)) {
                question.conditions.forEach { check ->
                    ConditionRow(check)
                }
            }
        }

        Text("解题思路", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Card {
            Text(question.explanation, modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium)
        }

        if (question.commonTraps.isNotBlank()) {
            Text("常见坑", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
            ) {
                Row(modifier = Modifier.padding(12.dp)) {
                    Icon(Icons.Default.Warning, null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(question.commonTraps, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("下一题", style = MaterialTheme.typography.titleSmall) }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ConditionRow(check: ConditionCheck) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (check.value) Icons.Default.CheckCircle else Icons.Default.Cancel,
            null,
            tint = if (check.value) Color(0xFF4CAF50) else Color(0xFFF44336),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(check.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(check.hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CompletePhase(correct: Int, total: Int, onRestart: () -> Unit) {
    val rate = if (total > 0) correct * 100 / total else 0

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                if (rate >= 80) Icons.Default.EmojiEvents else Icons.Default.School,
                null,
                tint = if (rate >= 80) Color(0xFFFFD700) else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("训练完成！", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Text("正确 $correct / $total ($rate%)",
                style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                when {
                    rate >= 90 -> "太棒了！方法选择已经很熟练"
                    rate >= 70 -> "不错，继续练习巩固"
                    rate >= 50 -> "还需加强，注意看触发词"
                    else -> "建议先复习方法选择速查表"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (total - correct > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("已为 ${total - correct} 道错题生成边界卡",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary)
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onRestart, modifier = Modifier.fillMaxWidth(0.6f)) {
                Text("再来一轮")
            }
        }
    }
}
