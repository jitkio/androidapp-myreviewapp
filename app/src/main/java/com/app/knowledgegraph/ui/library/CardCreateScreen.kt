package com.app.knowledgegraph.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.knowledgegraph.AppContainer
import com.app.knowledgegraph.data.db.entity.Card
import com.app.knowledgegraph.data.db.entity.CardType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardCreateScreen(
    container: AppContainer,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedType by remember { mutableStateOf(CardType.CONCEPT) }
    var chapter by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("新建卡片") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (prompt.isNotBlank() && answer.isNotBlank()) {
                                isSaving = true
                                scope.launch {
                                    container.cardRepository.createCard(
                                        Card(
                                            type = selectedType,
                                            chapter = chapter.ifBlank { "未分类" },
                                            tags = tags,
                                            prompt = prompt,
                                            hint = hint,
                                            answer = answer
                                        )
                                    )
                                    onNavigateBack()
                                }
                            }
                        },
                        enabled = prompt.isNotBlank() && answer.isNotBlank() && !isSaving
                    ) {
                        Text("保存")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 卡片类型选择
            Text("卡片类型", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CardType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(cardTypeLabel(type)) }
                    )
                }
            }

            // 章节
            OutlinedTextField(
                value = chapter,
                onValueChange = { chapter = it },
                label = { Text("章节") },
                placeholder = { Text("如：第4章-电路定理") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // 标签
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text("标签（逗号分隔）") },
                placeholder = { Text("如：戴维南,等效电路,线性") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Prompt
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("问题 (Prompt) *") },
                placeholder = { Text("什么条件下用戴维南定理？步骤？") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            // Hint
            OutlinedTextField(
                value = hint,
                onValueChange = { hint = it },
                label = { Text("提示 (Hint)") },
                placeholder = { Text("可选的提示信息") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            // Answer
            OutlinedTextField(
                value = answer,
                onValueChange = { answer = it },
                label = { Text("答案 (Answer) *") },
                placeholder = { Text("详细的回答内容...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 5
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
