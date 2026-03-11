package com.app.knowledgegraph.ui.scan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.knowledgegraph.AppContainer
import com.app.knowledgegraph.data.db.entity.ImportedQuestion
import com.app.knowledgegraph.data.db.entity.QuestionType
import com.app.knowledgegraph.ui.components.MathView
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportedQuizScreen(
    container: AppContainer,
    onNavigateBack: () -> Unit,
    presetCount: Int = 0,
    presetSources: String = "",
    presetTypes: String = "",
    presetFolderIds: String = "",
    viewModel: ImportedQuizViewModel = viewModel(factory = ImportedQuizViewModel.factory(container))
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        if (presetFolderIds.isNotBlank() && presetCount > 0) {
            val folderIds = presetFolderIds.split(",").mapNotNull { it.toLongOrNull() }.toSet()
            if (folderIds.isNotEmpty()) {
                viewModel.startWithFolders(folderIds, presetCount)
            } else if (presetCount > 0) {
                viewModel.startWithPreset(presetCount, presetSources, presetTypes)
            }
        } else if (presetCount > 0) {
            viewModel.startWithPreset(presetCount, presetSources, presetTypes)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(when (uiState.phase) {
                        QuizPhase.SETUP -> "刷题设置"
                        QuizPhase.ANSWERING, QuizPhase.FEEDBACK -> "${uiState.currentIndex + 1}/${uiState.questions.size}"
                        QuizPhase.COMPLETE -> "练习完成"
                    })
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    if (uiState.phase == QuizPhase.ANSWERING || uiState.phase == QuizPhase.FEEDBACK) {
                        Text("${uiState.sessionCorrect}/${uiState.sessionTotal}",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(end = 16.dp))
                    }
                }
            )
        }
    ) { padding ->
        when (uiState.phase) {
            QuizPhase.SETUP -> SetupPhase(
                uiState = uiState,
                onModeChange = viewModel::setQuizMode,
                onSourceChange = viewModel::setSelectedSource,
                onCountChange = viewModel::setQuestionCount,
                onStart = viewModel::startQuiz,
                modifier = Modifier.padding(padding)
            )
            QuizPhase.ANSWERING -> {
                uiState.currentQuestion?.let { question ->
                    AnsweringPhase(
                        question = question,
                        userAnswer = uiState.userAnswer,
                        userMultiAnswer = uiState.userMultiAnswer,
                        onAnswerChange = viewModel::updateAnswer,
                        onMultiToggle = viewModel::toggleMultiAnswer,
                        onSubmit = viewModel::submitAnswer,
                        progress = uiState.progress,
                        modifier = Modifier.padding(padding)
                    )
                }
            }
            QuizPhase.FEEDBACK -> {
                uiState.currentQuestion?.let { question ->
                    FeedbackPhase(
                        question = question,
                        userAnswer = uiState.userAnswer,
                        userMultiAnswer = uiState.userMultiAnswer,
                        isCorrect = uiState.isCorrect,
                        onNext = viewModel::nextQuestion,
                        modifier = Modifier.padding(padding)
                    )
                }
            }
            QuizPhase.COMPLETE -> CompletePhase(
                correct = uiState.sessionCorrect,
                total = uiState.sessionTotal,
                onRestart = viewModel::restartQuiz,
                onBack = onNavigateBack,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun SetupPhase(
    uiState: ImportedQuizUiState,
    onModeChange: (QuizMode) -> Unit,
    onSourceChange: (String) -> Unit,
    onCountChange: (Int) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("选择模式", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        QuizMode.entries.forEach { mode ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onModeChange(mode) },
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.quizMode == mode) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = uiState.quizMode == mode, onClick = { onModeChange(mode) })
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(when (mode) {
                            QuizMode.RANDOM -> "随机练习"
                            QuizMode.WEAK_FIRST -> "错题优先"
                            QuizMode.BY_SOURCE -> "按来源"
                        }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(when (mode) {
                            QuizMode.RANDOM -> "从所有题目中随机抽取"
                            QuizMode.WEAK_FIRST -> "优先练习正确率低于60%的题目"
                            QuizMode.BY_SOURCE -> "选择特定来源的题目"
                        }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (uiState.quizMode == QuizMode.BY_SOURCE && uiState.availableSources.isNotEmpty()) {
            Text("选择来源", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            uiState.availableSources.forEach { source ->
                FilterChip(selected = uiState.selectedSource == source, onClick = { onSourceChange(source) }, label = { Text(source) })
            }
        }

        Text("题目数量", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 10, 20, 50).forEach { count ->
                FilterChip(selected = uiState.questionCount == count, onClick = { onCountChange(count) }, label = { Text("$count") })
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = uiState.quizMode != QuizMode.BY_SOURCE || uiState.selectedSource.isNotBlank()
        ) { Text("开始刷题", style = MaterialTheme.typography.titleSmall) }
    }
}

@Composable
private fun AnsweringPhase(
    question: ImportedQuestion,
    userAnswer: String,
    userMultiAnswer: Set<String>,
    onAnswerChange: (String) -> Unit,
    onMultiToggle: (String) -> Unit,
    onSubmit: () -> Unit,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AssistChip(onClick = {}, label = {
                            Text(when (question.type) {
                                QuestionType.SINGLE_CHOICE -> "单选题"
                                QuestionType.MULTI_CHOICE -> "多选题"
                                QuestionType.FILL_BLANK -> "填空题"
                                QuestionType.TRUE_FALSE -> "判断题"
                            })
                        })
                    }
                    Spacer(Modifier.height(8.dp))
                    MathView(
                        text = question.stem,
                        figureSvg = question.figureSvg,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            when (question.type) {
                QuestionType.SINGLE_CHOICE -> SingleChoiceInput(parseOptions(question.optionsJson), userAnswer, onAnswerChange)
                QuestionType.MULTI_CHOICE -> MultiChoiceInput(parseOptions(question.optionsJson), userMultiAnswer, onMultiToggle)
                QuestionType.FILL_BLANK -> {
                    OutlinedTextField(value = userAnswer, onValueChange = onAnswerChange,
                        label = { Text("填写答案") }, modifier = Modifier.fillMaxWidth())
                }
                QuestionType.TRUE_FALSE -> TrueFalseInput(userAnswer, onAnswerChange)
            }
        }

        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
            enabled = when (question.type) {
                QuestionType.MULTI_CHOICE -> userMultiAnswer.isNotEmpty()
                else -> userAnswer.isNotBlank()
            }
        ) { Text("提交答案", style = MaterialTheme.typography.titleSmall) }
    }
}

@Composable
private fun SingleChoiceInput(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    options.forEach { option ->
        val letter = option.firstOrNull()?.toString() ?: ""
        val isSelected = selected == letter
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onSelect(letter) },
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = isSelected, onClick = { onSelect(letter) })
                Spacer(Modifier.width(8.dp))
                MathView(text = option, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MultiChoiceInput(options: List<String>, selected: Set<String>, onToggle: (String) -> Unit) {
    options.forEach { option ->
        val letter = option.firstOrNull()?.toString() ?: ""
        val isSelected = selected.contains(letter)
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onToggle(letter) },
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isSelected, onCheckedChange = { onToggle(letter) })
                Spacer(Modifier.width(8.dp))
                MathView(text = option, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TrueFalseInput(selected: String, onSelect: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = { onSelect("TRUE") },
            modifier = Modifier.weight(1f).height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selected == "TRUE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = if (selected == "TRUE") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(Icons.Default.Check, null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(8.dp))
            Text("正确", style = MaterialTheme.typography.titleMedium)
        }
        Button(
            onClick = { onSelect("FALSE") },
            modifier = Modifier.weight(1f).height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selected == "FALSE") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = if (selected == "FALSE") MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(Icons.Default.Close, null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(8.dp))
            Text("错误", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun FeedbackPhase(
    question: ImportedQuestion,
    userAnswer: String,
    userMultiAnswer: Set<String>,
    isCorrect: Boolean,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sections = remember(question.explanation) { parseExplanationSections(question.explanation) }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Result status card
            Card(colors = CardDefaults.cardColors(
                containerColor = if (isCorrect) Color(0xFF1B5E20).copy(alpha = 0.15f) else Color(0xFFB71C1C).copy(alpha = 0.15f)
            )) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel, null,
                        tint = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336), modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(if (isCorrect) "回答正确！" else "回答错误",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (!isCorrect) {
                            val displayAnswer = when (question.type) {
                                QuestionType.MULTI_CHOICE -> userMultiAnswer.sorted().joinToString(",")
                                else -> userAnswer
                            }
                            Text("你的答案: $displayAnswer", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Correct answer card
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("正确答案", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    MathView(text = question.answer, modifier = Modifier.fillMaxWidth())
                }
            }

            // Structured explanation sections
            if (sections.process.isNotBlank()) {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("解题过程", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        MathView(text = sections.process, figureSvg = question.figureSvg, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            if (sections.keywords.isNotBlank()) {
                Card(colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                )) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Key, null,
                                tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("解题关键词", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        MathView(text = sections.keywords, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            if (sections.pitfalls.isNotBlank()) {
                Card(colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                )) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null,
                                tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("常见坑", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        MathView(text = sections.pitfalls, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            if (sections.tips.isNotBlank()) {
                Card(colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                )) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lightbulb, null,
                                tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("技巧", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        MathView(text = sections.tips, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            // Fallback: if no sections were parsed, show raw explanation
            if (!sections.hasStructuredContent && question.explanation.isNotBlank()) {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("解析", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        MathView(text = question.explanation, figureSvg = question.figureSvg, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }

        // Fixed bottom button
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp)) {
            Text("下一题", style = MaterialTheme.typography.titleSmall)
        }
    }
}

private data class ExplanationSections(
    val process: String = "",
    val keywords: String = "",
    val pitfalls: String = "",
    val tips: String = ""
) {
    val hasStructuredContent: Boolean get() = process.isNotBlank() || keywords.isNotBlank() || pitfalls.isNotBlank() || tips.isNotBlank()
}

private fun parseExplanationSections(explanation: String): ExplanationSections {
    if (explanation.isBlank()) return ExplanationSections()

    val sectionPattern = Regex("""【(解题过程|解题关键词|常见坑|技巧)】""")
    val matches = sectionPattern.findAll(explanation).toList()

    if (matches.isEmpty()) return ExplanationSections()

    val sectionMap = mutableMapOf<String, String>()
    for (i in matches.indices) {
        val key = matches[i].groupValues[1]
        val start = matches[i].range.last + 1
        val end = if (i + 1 < matches.size) matches[i + 1].range.first else explanation.length
        sectionMap[key] = explanation.substring(start, end).trim()
    }

    return ExplanationSections(
        process = sectionMap["解题过程"] ?: "",
        keywords = sectionMap["解题关键词"] ?: "",
        pitfalls = sectionMap["常见坑"] ?: "",
        tips = sectionMap["技巧"] ?: ""
    )
}

@Composable
private fun CompletePhase(
    correct: Int, total: Int, onRestart: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier
) {
    val rate = if (total > 0) correct * 100 / total else 0
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(if (rate >= 80) Icons.Default.EmojiEvents else Icons.Default.School, null,
                tint = if (rate >= 80) Color(0xFFFFD700) else MaterialTheme.colorScheme.primary, modifier = Modifier.size(72.dp))
            Spacer(Modifier.height(16.dp))
            Text("练习完成！", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text("正确 $correct / $total ($rate%)", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(when {
                rate >= 90 -> "太棒了！掌握得很好"
                rate >= 70 -> "不错，继续练习巩固"
                rate >= 50 -> "还需加强，多多复习"
                else -> "建议重点复习这些题目"
            }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(32.dp))
            Button(onClick = onRestart, modifier = Modifier.fillMaxWidth(0.7f)) { Text("再来一轮") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(0.7f)) { Text("返回") }
        }
    }
}

private fun parseOptions(optionsJson: String): List<String> {
    if (optionsJson.isBlank() || optionsJson == "[]") return emptyList()
    return try {
        val array = JSONArray(optionsJson)
        (0 until array.length()).map { array.getString(it) }
    } catch (_: Exception) { emptyList() }
}
