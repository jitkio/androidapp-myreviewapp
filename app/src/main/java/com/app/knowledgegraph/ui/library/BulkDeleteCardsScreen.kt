package com.app.knowledgegraph.ui.library

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.knowledgegraph.AppContainer
import com.app.knowledgegraph.data.db.entity.Card
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class DeleteMode { ALL, BY_DATE, BY_TAG }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkDeleteCardsScreen(
    container: AppContainer,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val allCards by container.cardRepository.observeAll().collectAsState(initial = emptyList())
    val allTagsRaw by container.cardRepository.observeAllTags().collectAsState(initial = emptyList())
    var deleteMode by remember { mutableStateOf(DeleteMode.ALL) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    // Date filter state
    var startDate by remember { mutableLongStateOf(defaultStartDate()) }
    var endDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    // Tag filter state
    var selectedTags by remember { mutableStateOf(setOf<String>()) }

    // Parse distinct tags from raw tag strings
    val allTags = remember(allTagsRaw) {
        allTagsRaw.flatMap { it.split(",").map { t -> t.trim() } }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    val matchingCards = remember(allCards, deleteMode, startDate, endDate, selectedTags) {
        when (deleteMode) {
            DeleteMode.ALL -> allCards
            DeleteMode.BY_DATE -> allCards.filter { it.createdAt in startDate..endDate }
            DeleteMode.BY_TAG -> {
                if (selectedTags.isEmpty()) emptyList()
                else allCards.filter { card ->
                    val cardTags = card.tags.split(",").map { it.trim() }.toSet()
                    selectedTags.any { it in cardTags }
                }
            }
        }
    }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("批量删除卡片") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "将删除 ${matchingCards.size} 张卡片",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (matchingCards.isNotEmpty()) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { showConfirmDialog = true },
                        enabled = matchingCards.isNotEmpty() && !isDeleting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("删除")
                    }
                }
            }
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
            Spacer(Modifier.height(4.dp))

            // Mode selector
            Text("选择模式", style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = deleteMode == DeleteMode.ALL,
                    onClick = { deleteMode = DeleteMode.ALL },
                    shape = SegmentedButtonDefaults.itemShape(0, 3)
                ) { Text("全部") }
                SegmentedButton(
                    selected = deleteMode == DeleteMode.BY_DATE,
                    onClick = { deleteMode = DeleteMode.BY_DATE },
                    shape = SegmentedButtonDefaults.itemShape(1, 3)
                ) { Text("按日期") }
                SegmentedButton(
                    selected = deleteMode == DeleteMode.BY_TAG,
                    onClick = { deleteMode = DeleteMode.BY_TAG },
                    shape = SegmentedButtonDefaults.itemShape(2, 3)
                ) { Text("按标签") }
            }

            HorizontalDivider()

            when (deleteMode) {
                DeleteMode.ALL -> {
                    Card {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "删除全部 ${allCards.size} 张卡片",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "此操作将删除所有卡片及其关联的复习记录和连接关系。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                DeleteMode.BY_DATE -> {
                    Text("起始日期", style = MaterialTheme.typography.labelLarge)
                    OutlinedButton(onClick = { showStartPicker = true }) {
                        Text(dateFormat.format(Date(startDate)))
                    }
                    Text("截止日期", style = MaterialTheme.typography.labelLarge)
                    OutlinedButton(onClick = { showEndPicker = true }) {
                        Text(dateFormat.format(Date(endDate)))
                    }
                    Text(
                        "匹配 ${matchingCards.size} 张卡片",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DeleteMode.BY_TAG -> {
                    if (allTags.isEmpty()) {
                        Text(
                            "暂无标签",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text("选择标签", style = MaterialTheme.typography.labelLarge)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            allTags.forEach { tag ->
                                FilterChip(
                                    selected = tag in selectedTags,
                                    onClick = {
                                        selectedTags = if (tag in selectedTags) selectedTags - tag else selectedTags + tag
                                    },
                                    label = { Text(tag) }
                                )
                            }
                        }
                        Text(
                            "匹配 ${matchingCards.size} 张卡片",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // Date pickers
    if (showStartPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = startDate)
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { startDate = it }
                    showStartPicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text("取消") }
            }
        ) { DatePicker(state = state) }
    }
    if (showEndPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = endDate)
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { endDate = it + 86400000L - 1 } // end of day
                    showEndPicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) { Text("取消") }
            }
        ) { DatePicker(state = state) }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除 ${matchingCards.size} 张卡片吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    isDeleting = true
                    scope.launch {
                        try {
                            container.cardRepository.deleteByIds(matchingCards.map { it.id })
                            // 等待数据库操作完全完成
                            kotlinx.coroutines.delay(100)
                            onNavigateBack()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            isDeleting = false
                        }
                    }
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("取消") }
            }
        )
    }
}

private fun defaultStartDate(): Long {
    val cal = Calendar.getInstance()
    cal.add(Calendar.MONTH, -1)
    return cal.timeInMillis
}
