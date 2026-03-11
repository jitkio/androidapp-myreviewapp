package com.app.knowledgegraph.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.knowledgegraph.AppContainer
import com.app.knowledgegraph.data.db.entity.Card
import com.app.knowledgegraph.data.db.entity.RelationType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEdgeScreen(
    sourceCardId: Long,
    container: AppContainer,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val allCards by container.cardRepository.observeAll().collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var selectedTarget by remember { mutableStateOf<Card?>(null) }
    var selectedRelation by remember { mutableStateOf(RelationType.REQUIRES) }
    var description by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    val filteredCards = allCards.filter { card ->
        card.id != sourceCardId && (
            searchQuery.isBlank() ||
            card.prompt.contains(searchQuery, ignoreCase = true) ||
            card.tags.contains(searchQuery, ignoreCase = true) ||
            card.chapter.contains(searchQuery, ignoreCase = true)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加连接") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val target = selectedTarget ?: return@TextButton
                            isSaving = true
                            scope.launch {
                                container.graphRepository.addEdge(
                                    sourceId = sourceCardId,
                                    targetId = target.id,
                                    relation = selectedRelation,
                                    description = description
                                )
                                onNavigateBack()
                            }
                        },
                        enabled = selectedTarget != null && !isSaving
                    ) { Text("保存") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            Text("关系类型", style = MaterialTheme.typography.labelLarge)
            Column {
                RelationType.entries.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { type ->
                            FilterChip(
                                selected = selectedRelation == type,
                                onClick = { selectedRelation = type },
                                label = { Text(relationTypeLabel(type)) }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("关系说明（可选）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            HorizontalDivider()

            Text(
                if (selectedTarget != null) "已选: ${selectedTarget!!.prompt}"
                else "选择目标卡片",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selectedTarget != null) FontWeight.Bold else FontWeight.Normal
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("搜索卡片...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredCards, key = { it.id }) { card ->
                    val isSelected = selectedTarget?.id == card.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTarget = card },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AssistChip(onClick = {}, label = { Text(card.type.name, style = MaterialTheme.typography.labelSmall) }, modifier = Modifier.height(24.dp))
                                Text(card.chapter, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterVertically))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(card.prompt, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

fun relationTypeLabel(type: RelationType): String = when (type) {
    RelationType.REQUIRES -> "前置知识"
    RelationType.EQUIVALENT -> "等价互换"
    RelationType.FAILS_WHEN -> "失效条件"
    RelationType.WORKFLOW -> "解题流程"
    RelationType.CONTRADICTS -> "互斥关系"
    RelationType.EXTENDS -> "扩展推广"
}
