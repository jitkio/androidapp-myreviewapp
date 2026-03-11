package com.app.knowledgegraph.ui.library

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

private data class SubjectOption(val label: String, val color: Color)

private val subjectOptions = listOf(
    SubjectOption("英语", Color(0xFF43A047)),
    SubjectOption("数学", Color(0xFF1E88E5)),
    SubjectOption("政治", Color(0xFFFF8F00)),
    SubjectOption("专业课", Color(0xFF8E24AA))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    container: AppContainer,
    appNavState: AppNavState,
    viewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.factory(container))
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSubjectPicker by remember { mutableStateOf(false) }
    var rootRect by remember { mutableStateOf<Rect?>(null) }
    var bulkDeleteRect by remember { mutableStateOf<Rect?>(null) }
    var addCardRect by remember { mutableStateOf<Rect?>(null) }

    Box(modifier = Modifier.fillMaxSize().onGloballyPositioned { rootRect = it.boundsInRoot() }) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Library", style = MaterialTheme.typography.titleLarge) },
                windowInsets = WindowInsets(0),
                actions = {
                    // 批量删除按钮
                    IconButton(
                        onClick = {
                            appNavState.navigateFromButton(DetailRoutes.BULK_DELETE_CARDS, rootRect, bulkDeleteRect)
                        },
                        modifier = Modifier.onGloballyPositioned { bulkDeleteRect = it.boundsInRoot() }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "批量删除")
                    }
                    // 摄像头按钮（冰糖葫芦触发器）
                    IconButton(onClick = { showSubjectPicker = !showSubjectPicker }) {
                        Surface(
                            shape = CircleShape,
                            color = if (showSubjectPicker) Primary else Primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(ButtonSize.iconSize)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "智能扫描",
                                    modifier = Modifier.size(20.dp),
                                    tint = if (showSubjectPicker) TextPrimary else Primary
                                )
                            }
                        }
                    }
                    // 添加卡片按钮
                    IconButton(
                        onClick = {
                            appNavState.navigateFromButton(DetailRoutes.CARD_CREATE, rootRect, addCardRect)
                        },
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
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(animationSpec = tween(AnimationDuration.pageTransition)) +
                                    expandVertically(animationSpec = tween(AnimationDuration.pageTransition))
                        ) {
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

        // 半透明遮罩层
        AnimatedVisibility(
            visible = showSubjectPicker,
            enter = fadeIn(animationSpec = tween(AnimationDuration.bottomSheetHide)),
            exit = fadeOut(animationSpec = tween(AnimationDuration.bottomSheetHide))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showSubjectPicker = false }
            )
        }

        // 冰糖葫芦学科选择球
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 64.dp, end = 52.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            subjectOptions.forEachIndexed { index, option ->
                AnimatedVisibility(
                    visible = showSubjectPicker,
                    enter = scaleIn(
                        animationSpec = tween(250, delayMillis = index * 80),
                        initialScale = 0.3f,
                        transformOrigin = TransformOrigin(0.5f, 0f)
                    ) + fadeIn(
                        animationSpec = tween(250, delayMillis = index * 80)
                    ),
                    exit = scaleOut(
                        animationSpec = tween(200, delayMillis = (subjectOptions.size - 1 - index) * 50),
                        targetScale = 0.3f,
                        transformOrigin = TransformOrigin(0.5f, 0f)
                    ) + fadeOut(
                        animationSpec = tween(200, delayMillis = (subjectOptions.size - 1 - index) * 50)
                    )
                ) {
                    Surface(
                        onClick = {
                            showSubjectPicker = false
                            appNavState.navigate(DetailRoutes.smartScan(option.label), IncomingFrom.BOTTOM)
                        },
                        shape = CircleShape,
                        color = option.color,
                        shadowElevation = 6.dp,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = option.label,
                                color = Color.White,
                                fontSize = if (option.label.length > 2) 10.sp else 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CardListItem(
    card: CardEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 类型标签
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = cardTypeLabel(card.type),
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    modifier = Modifier.height(24.dp)
                )
                // 章节
                Text(
                    text = card.chapter,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            MathText(
                text = card.prompt,
                modifier = Modifier.fillMaxWidth()
            )

            if (card.tags.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = card.tags.split(",").joinToString("  #") { it.trim() }.let { "#$it" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
