package com.app.knowledgegraph.ui.library

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.app.knowledgegraph.AppContainer
import com.app.knowledgegraph.ui.navigation.AppNavState
import com.app.knowledgegraph.ui.navigation.IncomingFrom
import com.app.knowledgegraph.data.db.entity.Card as CardEntity
import com.app.knowledgegraph.data.db.entity.CardType
import com.app.knowledgegraph.ui.components.MathText
import com.app.knowledgegraph.ui.navigation.DetailRoutes
import com.app.knowledgegraph.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    container: AppContainer,
    appNavState: AppNavState,
    viewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.factory(container))
) {
    val uiState by viewModel.uiState.collectAsState()
    var rootRect by remember { mutableStateOf<Rect?>(null) }
    var bulkDeleteRect by remember { mutableStateOf<Rect?>(null) }
    var addCardRect by remember { mutableStateOf<Rect?>(null) }
    var mapRect by remember { mutableStateOf<Rect?>(null) }
    var cameraRect by remember { mutableStateOf<Rect?>(null) }

    Column(modifier = Modifier.fillMaxSize().onGloballyPositioned { rootRect = it.boundsInRoot() }) {
        TopAppBar(
            title = { Text("Library", style = MaterialTheme.typography.titleLarge) },
            windowInsets = WindowInsets(0),
            actions = {
                // 知识图谱
                IconButton(
                    onClick = { appNavState.navigateFromButton(DetailRoutes.MAP_SCREEN, rootRect, mapRect) },
                    modifier = Modifier.onGloballyPositioned { mapRect = it.boundsInRoot() }
                ) {
                    Icon(Icons.Default.Hub, contentDescription = "知识图谱")
                }
                // 批量删除
                IconButton(
                    onClick = { appNavState.navigateFromButton(DetailRoutes.BULK_DELETE_CARDS, rootRect, bulkDeleteRect) },
                    modifier = Modifier.onGloballyPositioned { bulkDeleteRect = it.boundsInRoot() }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "批量删除")
                }
                // ★ 摄像头 — 直接进入 SmartScan 页面
                IconButton(
                    onClick = { appNavState.navigateFromButton(DetailRoutes.SMART_SCAN, rootRect, cameraRect) },
                    modifier = Modifier.onGloballyPositioned { cameraRect = it.boundsInRoot() }
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "智能扫描")
                }
                // 添加卡片
                IconButton(
                    onClick = { appNavState.navigateFromButton(DetailRoutes.CARD_CREATE, rootRect, addCardRect) },
                    modifier = Modifier.onGloballyPositioned { addCardRect = it.boundsInRoot() }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "添加卡片")
                }
            }
        )

        // 搜索栏
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onSearchQueryChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.space4, vertical = Spacing.space2),
            placeholder = { Text("搜索卡片...", style = MaterialTheme.typography.bodyLarge) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(InputField.radius),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Border,
                focusedBorderColor = Primary
            )
        )

        // 类型筛选 Chips
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Spacing.space4, vertical = Spacing.space1),
            horizontalArrangement = Arrangement.spacedBy(Spacing.space2)
        ) {
            FilterChip(
                selected = uiState.selectedType == null,
                onClick = { viewModel.onTypeSelected(null) },
                label = { Text("全部") }
            )
            CardType.entries.forEach { type ->
                FilterChip(
                    selected = uiState.selectedType == type,
                    onClick = { viewModel.onTypeSelected(type) },
                    label = { Text(cardTypeLabel(type)) }
                )
            }
        }

        // 章节筛选 Chips
        if (uiState.chapters.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.space4, vertical = Spacing.space1),
                horizontalArrangement = Arrangement.spacedBy(Spacing.space2)
            ) {
                FilterChip(
                    selected = uiState.selectedChapter == null,
                    onClick = { viewModel.onChapterSelected(null) },
                    label = { Text("全部章节") }
                )
                uiState.chapters.forEach { chapter ->
                    FilterChip(
                        selected = uiState.selectedChapter == chapter,
                        onClick = { viewModel.onChapterSelected(chapter) },
                        label = { Text(chapter) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.space1))

        // 卡片列表
        if (uiState.isLoading) {
            com.app.knowledgegraph.ui.components.LoadingState(message = "加载卡片...")
        } else if (uiState.cards.isEmpty()) {
            com.app.knowledgegraph.ui.components.EmptyState(
                title = "暂无卡片",
                description = "点击右上角 + 添加新卡片，开始构建你的知识库"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = Spacing.space4, vertical = Spacing.space2),
                verticalArrangement = Arrangement.spacedBy(Spacing.space3)
            ) {
                items(uiState.cards, key = { it.id }, contentType = { "card" }) { card ->
                    CardListItem(
                        card = card,
                        onClick = {
                            appNavState.navigate(DetailRoutes.cardDetail(card.id), IncomingFrom.BOTTOM)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CardListItem(card: CardEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text(cardTypeLabel(card.type), style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.height(24.dp)
                )
                Text(card.chapter, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(8.dp))
            MathText(text = card.prompt, modifier = Modifier.fillMaxWidth())
            if (card.tags.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = card.tags.split(",").joinToString("  #") { it.trim() }.let { "#$it" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

fun cardTypeLabel(type: CardType): String = when (type) {
    CardType.CONCEPT -> "概念"
    CardType.METHOD -> "方法"
    CardType.TEMPLATE -> "模板"
    CardType.BOUNDARY -> "边界"
}
