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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.knowledgegraph.AppContainer
import com.app.knowledgegraph.data.db.entity.ImportedQuestion
import com.app.knowledgegraph.data.db.entity.QuestionType
import com.app.knowledgegraph.ui.components.MathView
import com.app.knowledgegraph.ui.theme.*
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
            if (folderIds.isNotEmpty()) viewModel.startWithFolders(folderIds, presetCount)
            else if (presetCount > 0) viewModel.startWithPreset(presetCount, presetSources, presetTypes)
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
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
                actions = {
                    if (uiState.phase == QuizPhase.ANSWERING || uiState.phase == QuizPhase.FEEDBACK) {
                        Surface(shape = RoundedCornerShape(8.dp), color = Primary.copy(alpha = 0.1f),
                            modifier = Modifier.padding(end = 12.dp)) {
                            Text("${uiState.sessionCorrect}/${uiState.sessionTotal}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelLarge, color = Primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { padding ->
        when (uiState.phase) {
            QuizPhase.SETUP -> SetupPhase(uiState, viewModel::setQuizMode, viewModel::setSelectedSource,
                viewModel::setQuestionCount, viewModel::startQuiz, Modifier.padding(padding))
            QuizPhase.ANSWERING -> uiState.currentQuestion?.let { q ->
                AnsweringPhase(q, uiState.userAnswer, uiState.userMultiAnswer,
                    viewModel::updateAnswer, viewModel::toggleMultiAnswer, viewModel::submitAnswer,
                    uiState.progress, Modifier.padding(padding))
            }
            QuizPhase.FEEDBACK -> uiState.currentQuestion?.let { q ->
                FeedbackPhase(q, uiState.userAnswer, uiState.userMultiAnswer,
                    uiState.isCorrect, viewModel::nextQuestion, Modifier.padding(padding))
            }
            QuizPhase.COMPLETE -> CompletePhase(uiState.sessionCorrect, uiState.sessionTotal,
                viewModel::restartQuiz, onNavigateBack, Modifier.padding(padding))
        }
    }
}

@Composable
private fun SetupPhase(
    uiState: ImportedQuizUiState, onModeChange: (QuizMode) -> Unit, onSourceChange: (String) -> Unit,
    onCountChange: (Int) -> Unit, onStart: () -> Unit, modifier: Modifier
) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.space4),
        verticalArrangement = Arrangement.spacedBy(Spacing.space4)) {

        Text("选择模式", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        QuizMode.entries.forEach { mode ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onModeChange(mode) },
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.quizMode == mode) Primary.copy(alpha = 0.08f) else BgCard
                ),
                shape = RoundedCornerShape(CornerRadius.card)
            ) {
                Row(modifier = Modifier.padding(Spacing.space4), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = uiState.quizMode == mode, onClick = { onModeChange(mode) },
                        colors = RadioButtonDefaults.colors(selectedColor = Primary))
                    Spacer(Modifier.width(Spacing.space2))
                    Column {
                        Text(when (mode) {
                            QuizMode.RANDOM -> "随机练习"; QuizMode.WEAK_FIRST -> "错题优先"; QuizMode.BY_SOURCE -> "按来源"
                        }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(when (mode) {
                            QuizMode.RANDOM -> "从所有题目中随机抽取"
                            QuizMode.WEAK_FIRST -> "优先练习正确率低于60%的题目"
                            QuizMode.BY_SOURCE -> "选择特定来源的题目"
                        }, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }
        }

        if (uiState.quizMode == QuizMode.BY_SOURCE && uiState.availableSources.isNotEmpty()) {
            Text("选择来源", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space2),
                modifier = Modifier.fillMaxWidth()) {
                uiState.availableSources.forEach { source ->
                    FilterChip(selected = uiState.selectedSource == source, onClick = { onSourceChange(source) },
                        label = { Text(source) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary.copy(alpha = 0.15f), selectedLabelColor = Primary))
                }
            }
        }

        Text("题目数量", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            listOf(5, 10, 20, 50).forEach { count ->
                FilterChip(selected = uiState.questionCount == count, onClick = { onCountChange(count) },
                    label = { Text("$count") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primary.copy(alpha = 0.15f), selectedLabelColor = Primary))
            }
        }

        Spacer(Modifier.weight(1f))

        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = uiState.quizMode != QuizMode.BY_SOURCE || uiState.selectedSource.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) { Text("开始刷题", style = MaterialTheme.typography.titleSmall) }
    }
}

@Composable
private fun AnsweringPhase(
    question: ImportedQuestion, userAnswer: String, userMultiAnswer: Set<String>,
    onAnswerChange: (String) -> Unit, onMultiToggle: (String) -> Unit, onSubmit: () -> Unit,
    progress: Float, modifier: Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        LinearProgressIndicator(progress = { progress },
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.space4),
            color = Primary, trackColor = Primary.copy(alpha = 0.12f))

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Spacing.space4),
            verticalArrangement = Arrangement.spacedBy(Spacing.space3)
        ) {
            // 题干卡片
            Card(colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(CornerRadius.card)) {
                Column(modifier = Modifier.padding(Spacing.space4)) {
                    Surface(shape = RoundedCornerShape(8.dp), color = Primary.copy(alpha = 0.12f)) {
                        Text(when (question.type) {
                            QuestionType.SINGLE_CHOICE -> "单选题"; QuestionType.MULTI_CHOICE -> "多选题"
                            QuestionType.FILL_BLANK -> "填空题"; QuestionType.TRUE_FALSE -> "判断题"
                        }, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium, color = Primary, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(Spacing.space3))
                    MathView(text = question.stem, figureSvg = question.figureSvg,
                        modifier = Modifier.fillMaxWidth(), baseFontSize = 17)
                }
            }

            when (question.type) {
                QuestionType.SINGLE_CHOICE -> SingleChoiceInput(parseOptions(question.optionsJson), userAnswer, onAnswerChange)
                QuestionType.MULTI_CHOICE -> MultiChoiceInput(parseOptions(question.optionsJson), userMultiAnswer, onMultiToggle)
                QuestionType.FILL_BLANK -> OutlinedTextField(value = userAnswer, onValueChange = onAnswerChange,
                    label = { Text("填写答案") }, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary))
                QuestionType.TRUE_FALSE -> TrueFalseInput(userAnswer, onAnswerChange)
            }
        }

        Button(onClick = onSubmit, modifier = Modifier.fillMaxWidth().padding(Spacing.space4).height(52.dp),
            enabled = when (question.type) { QuestionType.MULTI_CHOICE -> userMultiAnswer.isNotEmpty(); else -> userAnswer.isNotBlank() },
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) { Text("提交答案", style = MaterialTheme.typography.titleSmall) }
    }
}

@Composable
private fun SingleChoiceInput(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
        options.forEach { option ->
            val letter = option.firstOrNull()?.toString() ?: ""
            val isSelected = selected == letter
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(letter) },
                colors = CardDefaults.cardColors(containerColor = if (isSelected) Primary.copy(alpha = 0.10f) else BgCard),
                shape = RoundedCornerShape(CornerRadius.small)
            ) {
                Row(modifier = Modifier.padding(Spacing.space3), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = isSelected, onClick = { onSelect(letter) },
                        colors = RadioButtonDefaults.colors(selectedColor = Primary))
                    Spacer(Modifier.width(Spacing.space2))
                    MathView(text = option, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MultiChoiceInput(options: List<String>, selected: Set<String>, onToggle: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
        options.forEach { option ->
            val letter = option.firstOrNull()?.toString() ?: ""
            val isSelected = selected.contains(letter)
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onToggle(letter) },
                colors = CardDefaults.cardColors(containerColor = if (isSelected) Primary.copy(alpha = 0.10f) else BgCard),
                shape = RoundedCornerShape(CornerRadius.small)
            ) {
                Row(modifier = Modifier.padding(Spacing.space3), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isSelected, onCheckedChange = { onToggle(letter) },
                        colors = CheckboxDefaults.colors(checkedColor = Primary))
                    Spacer(Modifier.width(Spacing.space2))
                    MathView(text = option, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TrueFalseInput(selected: String, onSelect: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.space3)) {
        Button(onClick = { onSelect("TRUE") }, modifier = Modifier.weight(1f).height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selected == "TRUE") Secondary else BgElevated,
                contentColor = if (selected == "TRUE") Color.White else TextPrimary),
            shape = RoundedCornerShape(CornerRadius.card)
        ) {
            Icon(Icons.Default.Check, null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(8.dp))
            Text("正确", style = MaterialTheme.typography.titleMedium)
        }
        Button(onClick = { onSelect("FALSE") }, modifier = Modifier.weight(1f).height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selected == "FALSE") Error else BgElevated,
                contentColor = if (selected == "FALSE") Color.White else TextPrimary),
            shape = RoundedCornerShape(CornerRadius.card)
        ) {
            Icon(Icons.Default.Close, null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(8.dp))
            Text("错误", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun FeedbackPhase(
    question: ImportedQuestion, userAnswer: String, userMultiAnswer: Set<String>,
    isCorrect: Boolean, onNext: () -> Unit, modifier: Modifier
) {
    val sections = remember(question.explanation) { parseExplanationSections(question.explanation) }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Spacing.space4),
            verticalArrangement = Arrangement.spacedBy(Spacing.space3)
        ) {
            // 结果状态卡片
            Card(colors = CardDefaults.cardColors(
                containerColor = if (isCorrect) Secondary.copy(alpha = 0.10f) else Error.copy(alpha = 0.10f)),
                shape = RoundedCornerShape(CornerRadius.card)
            ) {
                Row(modifier = Modifier.padding(Spacing.space4), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel, null,
                        tint = if (isCorrect) Secondary else Error, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(Spacing.space3))
                    Column {
                        Text(if (isCorrect) "回答正确！" else "回答错误",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                            color = if (isCorrect) Secondary else Error)
                        if (!isCorrect) {
                            val displayAnswer = when (question.type) {
                                QuestionType.MULTI_CHOICE -> userMultiAnswer.sorted().joinToString(",")
                                else -> userAnswer
                            }
                            Text("你的答案: $displayAnswer", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                }
            }

            // 正确答案
            Card(colors = CardDefaults.cardColors(containerColor = Secondary.copy(alpha = 0.06f)),
                shape = RoundedCornerShape(CornerRadius.card)) {
                Column(modifier = Modifier.padding(Spacing.space4)) {
                    Text("正确答案", style = MaterialTheme.typography.labelMedium, color = Secondary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(Spacing.space2))
                    MathView(text = question.answer, modifier = Modifier.fillMaxWidth(), baseFontSize = 18)
                }
            }

            // 解析分区
            if (sections.process.isNotBlank()) {
                ExplanationCard(Icons.AutoMirrored.Filled.MenuBook, "解题过程", Primary,
                    sections.process, question.figureSvg)
            }
            if (sections.keywords.isNotBlank()) {
                ExplanationCard(Icons.Default.Key, "解题关键词", Secondary, sections.keywords)
            }
            if (sections.pitfalls.isNotBlank()) {
                ExplanationCard(Icons.Default.Warning, "常见坑", Error, sections.pitfalls)
            }
            if (sections.tips.isNotBlank()) {
                ExplanationCard(Icons.Default.Lightbulb, "技巧", Warning, sections.tips)
            }

            // 无结构化时显示原文
            if (!sections.hasStructuredContent && question.explanation.isNotBlank()) {
                ExplanationCard(Icons.Default.Article, "解析", Primary,
                    question.explanation, question.figureSvg)
            }
        }

        Button(onClick = onNext, modifier = Modifier.fillMaxWidth().padding(Spacing.space4).height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) { Text("下一题", style = MaterialTheme.typography.titleSmall) }
    }
}

@Composable
private fun ExplanationCard(icon: ImageVector, title: String, color: Color, content: String, figureSvg: String = "") {
    Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.06f)),
        shape = RoundedCornerShape(CornerRadius.card)) {
        Column(modifier = Modifier.padding(Spacing.space4)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(title, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(Spacing.space2))
            MathView(text = content, figureSvg = figureSvg, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun CompletePhase(correct: Int, total: Int, onRestart: () -> Unit, onBack: () -> Unit, modifier: Modifier) {
    val rate = if (total > 0) correct * 100 / total else 0
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(if (rate >= 80) Icons.Default.EmojiEvents else Icons.Default.School, null,
                tint = if (rate >= 80) Warning else Primary, modifier = Modifier.size(72.dp))
            Spacer(Modifier.height(Spacing.space4))
            Text("练习完成！", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(Spacing.space3))

            // 横排统计
            Card(colors = CardDefaults.cardColors(containerColor = BgCard),
                shape = RoundedCornerShape(CornerRadius.card)) {
                Row(modifier = Modifier.fillMaxWidth(0.8f).padding(Spacing.space4),
                    horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$correct", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Secondary)
                        Text("正确", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$total", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Primary)
                        Text("总题", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$rate%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                            color = when { rate >= 80 -> Secondary; rate >= 50 -> Warning; else -> Error })
                        Text("正确率", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
            }

            Spacer(Modifier.height(Spacing.space2))
            Text(when {
                rate >= 90 -> "太棒了！掌握得很好"
                rate >= 70 -> "不错，继续练习巩固"
                rate >= 50 -> "还需加强，多多复习"
                else -> "建议重点复习这些题目"
            }, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)

            Spacer(Modifier.height(Spacing.space8))
            Button(onClick = onRestart, modifier = Modifier.fillMaxWidth(0.7f),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text("再来一轮") }
            Spacer(Modifier.height(Spacing.space2))
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(0.7f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)) { Text("返回") }
        }
    }
}

private data class ExplanationSections(
    val process: String = "", val keywords: String = "", val pitfalls: String = "", val tips: String = ""
) { val hasStructuredContent get() = process.isNotBlank() || keywords.isNotBlank() || pitfalls.isNotBlank() || tips.isNotBlank() }

private fun parseExplanationSections(explanation: String): ExplanationSections {
    if (explanation.isBlank()) return ExplanationSections()
    val pattern = Regex("""【(解题过程|解题关键词|常见坑|技巧)】""")
    val matches = pattern.findAll(explanation).toList()
    if (matches.isEmpty()) return ExplanationSections()
    val map = mutableMapOf<String, String>()
    for (i in matches.indices) {
        val key = matches[i].groupValues[1]
        val start = matches[i].range.last + 1
        val end = if (i + 1 < matches.size) matches[i + 1].range.first else explanation.length
        map[key] = explanation.substring(start, end).trim()
    }
    return ExplanationSections(map["解题过程"] ?: "", map["解题关键词"] ?: "", map["常见坑"] ?: "", map["技巧"] ?: "")
}

private fun parseOptions(optionsJson: String): List<String> {
    if (optionsJson.isBlank() || optionsJson == "[]") return emptyList()
    return try { val a = JSONArray(optionsJson); (0 until a.length()).map { a.getString(it) } }
    catch (_: Exception) { emptyList() }
}

