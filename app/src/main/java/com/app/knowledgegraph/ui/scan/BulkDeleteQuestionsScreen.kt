package com.app.knowledgegraph.ui.scan

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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.app.knowledgegraph.ui.components.buttonShadow3d

private enum class DeleteMode { ALL, BY_DATE, BY_SOURCE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkDeleteQuestionsScreen(
    container: AppContainer,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val allQuestions by container.scanRepository.observeAll().collectAsState(initial = emptyList())
    val allSources by container.scanRepository.observeAllSources().collectAsState(initial = emptyList())
    var deleteMode by remember { mutableStateOf(DeleteMode.ALL) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    // Date filter state
    var startDate by remember { mutableLongStateOf(defaultStartDate()) }
    var endDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    // Source filter state
    var selectedSources by remember { mutableStateOf(setOf<String>()) }

    val matchingQuestions = remember(allQuestions, deleteMode, startDate, endDate, selectedSources) {
        when (deleteMode) {
            DeleteMode.ALL -> allQuestions
            DeleteMode.BY_DATE -> allQuestions.filter { it.createdAt in startDate..endDate }
            DeleteMode.BY_SOURCE -> {
                if (selectedSources.isEmpty()) emptyList()
                else allQuestions.filter { it.source in selectedSources }
            }
        }
    }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("批量删除题目") },
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
                        "将删除 ${matchingQuestions.size} 道题目",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (matchingQuestions.isNotEmpty()) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { showConfirmDialog = true },
                        enabled = matchingQuestions.isNotEmpty() && !isDeleting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.buttonShadow3d().fillMaxWidth()
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
                    selected = deleteMode == DeleteMode.BY_SOURCE,
                    onClick = { deleteMode = DeleteMode.BY_SOURCE },
                    shape = SegmentedButtonDefaults.itemShape(2, 3)
                ) { Text("按来源") }
            }

            HorizontalDivider()

            when (deleteMode) {
                DeleteMode.ALL -> {
                    Card {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "删除全部 ${allQuestions.size} 道题目",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "此操作将删除所有导入的题目及其练习记录。",
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
                        "匹配 ${matchingQuestions.size} 道题目",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DeleteMode.BY_SOURCE -> {
                    if (allSources.isEmpty()) {
                        Text(
                            "暂无来源",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text("选择来源", style = MaterialTheme.typography.labelLarge)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            allSources.forEach { source ->
                                FilterChip(
                                    selected = source in selectedSources,
                                    onClick = {
                                        selectedSources = if (source in selectedSources) selectedSources - source else selectedSources + source
                                    },
                                    label = { Text(source) }
                                )
                            }
                        }
                        Text(
                            "匹配 ${matchingQuestions.size} 道题目",
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
                    state.selectedDateMillis?.let { endDate = it + 86400000L - 1 }
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
            text = { Text("确定要删除 ${matchingQuestions.size} 道题目吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    isDeleting = true
                    scope.launch {
                        container.scanRepository.deleteByIds(matchingQuestions.map { it.id })
                        onNavigateBack()
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
