package com.app.knowledgegraph.ui.practice

import androidx.compose.animation.*
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.knowledgegraph.AppContainer
import com.app.knowledgegraph.data.db.entity.ImportedQuestion
import com.app.knowledgegraph.data.db.entity.QuestionType
import com.app.knowledgegraph.ui.theme.*
import com.app.knowledgegraph.ui.components.PrimaryButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PracticeTab { SETTINGS, QUESTIONS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeHubScreen(
    container: AppContainer,
    onNavigateToMethodTraining: () -> Unit,
    onNavigateToScan: () -> Unit,
    onNavigateToQuiz: (count: Int, sources: String, types: String, folderIds: String) -> Unit,
    onNavigateToQuestionDetail: (Long) -> Unit = {},
    onNavigateToBulkDelete: () -> Unit = {},
    onNavigateToQuestionBank: () -> Unit = {}
) {
    val allQuestions by container.scanRepository.observeAll().collectAsState(initial = emptyList())
    val allSources by container.scanRepository.observeAllSources().collectAsState(initial = emptyList())
    val importedCount by container.scanRepository.observeTotalCount().collectAsState(initial = 0)
    val selectedFolderIds by container.settingsDataStore.selectedFolderIdsFlow.collectAsState(initial = emptySet())
    val folders by container.questionBankRepository.observeAllWithCount().collectAsState(initial = emptyList())

    var selectedTab by remember { mutableStateOf(PracticeTab.SETTINGS) }

    // Settings state
    var selectedSources by remember { mutableStateOf(setOf<String>()) }
    var selectedTypes by remember { mutableStateOf(setOf<QuestionType>()) }
    var questionCount by remember { mutableIntStateOf(10) }

    // Question list state
    var searchQuery by remember { mutableStateOf("") }
    var sortByDate by remember { mutableStateOf(true) } // true=newest first
    var filterSource by remember { mutableStateOf<String?>(null) }

    val filteredQuestions = remember(allQuestions, searchQuery, filterSource, sortByDate) {
        var list = allQuestions
        if (searchQuery.isNotBlank()) {
            list = list.filter { it.stem.contains(searchQuery, ignoreCase = true) || it.answer.contains(searchQuery, ignoreCase = true) }
        }
        if (filterSource != null) {
            list = list.filter { it.source == filterSource }
        }
        if (sortByDate) list.sortedByDescending { it.createdAt } else list.sortedBy { it.createdAt }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(bottom = BottomNavigation.height)) {
            TopAppBar(
                title = { Text("Practice", style = MaterialTheme.typography.titleLarge) },
                windowInsets = WindowInsets(0),
                actions = {
                    IconButton(onClick = onNavigateToBulkDelete) {
                        Icon(Icons.Default.Delete, "批量删除")
                    }
                    IconButton(onClick = onNavigateToScan) {
                        Icon(Icons.Default.CameraAlt, "拍照导入")
                    }
                }
            )

            // Tab Row
            TabRow(
                selectedTabIndex = if (selectedTab == PracticeTab.SETTINGS) 0 else 1,
                modifier = Modifier.padding(horizontal = Spacing.space4)
            ) {
                Tab(
                    selected = selectedTab == PracticeTab.SETTINGS,
                    onClick = { selectedTab = PracticeTab.SETTINGS },
                    text = { Text("\u8bad\u7ec3\u8bbe\u7f6e") },
                    icon = { Icon(Icons.Default.Settings, null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == PracticeTab.QUESTIONS,
                    onClick = { selectedTab = PracticeTab.QUESTIONS },
                    text = { Text("\u9898\u76ee\u5217\u8868 ($importedCount)") },
                    icon = { Icon(Icons.Default.List, null, modifier = Modifier.size(18.dp)) }
                )
            }

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    slideInHorizontally { fullWidth -> direction * fullWidth } + fadeIn() togetherWith
                            slideOutHorizontally { fullWidth -> -direction * fullWidth } + fadeOut()
                },
                label = "tabContent"
            ) { tab ->
                when (tab) {
                    PracticeTab.SETTINGS -> SettingsTab(
                        allSources = allSources,
                        selectedSources = selectedSources,
                        onToggleSource = { src ->
                            selectedSources = if (src in selectedSources) selectedSources - src else selectedSources + src
                        },
                        selectedTypes = selectedTypes,
                        onToggleType = { t ->
                            selectedTypes = if (t in selectedTypes) selectedTypes - t else selectedTypes + t
                        },
                        questionCount = questionCount,
                        onCountChange = { questionCount = it },
                        onMethodTraining = onNavigateToMethodTraining,
                        selectedFolderCount = selectedFolderIds.size,
                        totalFolderCount = folders.size,
                        onQuestionBank = onNavigateToQuestionBank
                    )
                    PracticeTab.QUESTIONS -> QuestionsTab(
                        questions = filteredQuestions,
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        sortByDate = sortByDate,
                        onToggleSort = { sortByDate = !sortByDate },
                        allSources = allSources,
                        filterSource = filterSource,
                        onFilterSource = { src ->
                            filterSource = if (src.isEmpty() || filterSource == src) null else src
                        },
                        onQuestionClick = onNavigateToQuestionDetail
                    )
                }
            }
        }

        // Bottom start training button - semicircle
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-Spacing.space2)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                onClick = {
                    val sourcesStr = selectedSources.joinToString(",")
                    val typesStr = selectedTypes.joinToString(",") { it.name }
                    val folderIdsStr = selectedFolderIds.joinToString(",")
                    onNavigateToQuiz(questionCount, sourcesStr, typesStr, folderIdsStr)
                },
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
                color = Primary,
                shadowElevation = 8.dp,
                modifier = Modifier.size(width = 140.dp, height = 48.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = TextPrimary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(Spacing.space1))
                    Text("开始训练", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun SettingsTab(
    allSources: List<String>,
    selectedSources: Set<String>,
    onToggleSource: (String) -> Unit,
    selectedTypes: Set<QuestionType>,
    onToggleType: (QuestionType) -> Unit,
    questionCount: Int,
    onCountChange: (Int) -> Unit,
    onMethodTraining: () -> Unit,
    selectedFolderCount: Int = 0,
    totalFolderCount: Int = 0,
    onQuestionBank: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Question bank entry
        item {
            Card(modifier = Modifier.fillMaxWidth().clickable { onQuestionBank() }) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("\u9898\u5e93\u7ba1\u7406", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            if (selectedFolderCount > 0) "\u5df2\u9009 $selectedFolderCount / $totalFolderCount \u4e2a\u9898\u5e93" else "\u7ba1\u7406\u9898\u5e93\u6587\u4ef6\u5939",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
        }

        // Source filter
        item {
            Text("\u6765\u6e90\u7b5b\u9009", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (allSources.isEmpty()) {
                Text("\u6682\u65e0\u6765\u6e90\uff0c\u8bf7\u5148\u5bfc\u5165\u9898\u76ee", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    allSources.forEach { src ->
                        FilterChip(
                            selected = src in selectedSources,
                            onClick = { onToggleSource(src) },
                            label = { Text(src) }
                        )
                    }
                }
            }
        }

        // Type filter
        item {
            Text("\u9898\u578b\u7b5b\u9009", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuestionType.entries.forEach { type ->
                    FilterChip(
                        selected = type in selectedTypes,
                        onClick = { onToggleType(type) },
                        label = { Text(questionTypeLabel(type)) }
                    )
                }
            }
        }

        // Question count
        item {
            Text("\u9898\u76ee\u6570\u91cf", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 20, 50).forEach { count ->
                    FilterChip(
                        selected = questionCount == count,
                        onClick = { onCountChange(count) },
                        label = { Text("$count") }
                    )
                }
            }
        }

        // Method training entry
        item {
            Spacer(Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth().clickable { onMethodTraining() }) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FlashOn, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("\u9009\u6cd5\u8bad\u7ec3", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("\u7535\u8def\u7406\u8bba\u65b9\u6cd5\u9009\u62e9\u7ec3\u4e60", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
        }
    }
}

@Composable
private fun QuestionsTab(
    questions: List<ImportedQuestion>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    sortByDate: Boolean,
    onToggleSort: () -> Unit,
    allSources: List<String>,
    filterSource: String?,
    onFilterSource: (String) -> Unit,
    onQuestionClick: (Long) -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("\u641c\u7d22\u9898\u76ee...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true
        )

        // Filter row
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sort toggle
            FilterChip(
                selected = true,
                onClick = onToggleSort,
                label = { Text(if (sortByDate) "\u6700\u65b0\u4f18\u5148" else "\u6700\u65e9\u4f18\u5148") },
                leadingIcon = { Icon(if (sortByDate) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, null, modifier = Modifier.size(16.dp)) }
            )
            // Source filters
            FilterChip(
                selected = filterSource == null,
                onClick = { onFilterSource("") },
                label = { Text("\u5168\u90e8") }
            )
            allSources.forEach { src ->
                FilterChip(
                    selected = filterSource == src,
                    onClick = { onFilterSource(src) },
                    label = { Text(src) }
                )
            }
        }

        if (questions.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("\u6682\u65e0\u9898\u76ee", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(questions, key = { it.id }) { question ->
                    QuestionCard(question, onClick = { onQuestionClick(question.id) })
                }
            }
        }
    }
}

@Composable
private fun QuestionCard(question: ImportedQuestion, onClick: () -> Unit = {}) {
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }
    Card(modifier = Modifier.fillMaxWidth().animateContentSize().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssistChip(
                        onClick = {},
                        label = { Text(questionTypeLabel(question.type), style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(24.dp)
                    )
                    if (question.source.isNotBlank()) {
                        AssistChip(
                            onClick = {},
                            label = { Text(question.source, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
                Text(dateFormat.format(Date(question.createdAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
            Text(question.stem, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (question.attemptCount > 0) {
                Spacer(Modifier.height(4.dp))
                val rate = if (question.attemptCount > 0) question.correctCount * 100 / question.attemptCount else 0
                Text("\u7ec3\u4e60 ${question.attemptCount} \u6b21 | \u6b63\u786e\u7387 $rate%",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (rate >= 60) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun questionTypeLabel(type: QuestionType): String = when (type) {
    QuestionType.SINGLE_CHOICE -> "\u5355\u9009"
    QuestionType.MULTI_CHOICE -> "\u591a\u9009"
    QuestionType.FILL_BLANK -> "\u586b\u7a7a"
    QuestionType.TRUE_FALSE -> "\u5224\u65ad"
}
