package com.app.knowledgegraph.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.knowledgegraph.data.preferences.SettingsDataStore
import com.app.knowledgegraph.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectManageScreen(
    settingsDataStore: SettingsDataStore,
    onNavigateBack: () -> Unit
) {
    val subjects by settingsDataStore.subjectListFlow.collectAsState(initial = SettingsDataStore.DEFAULT_SUBJECTS)
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    var newSubjectName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("学科管理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "添加学科")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (subjects.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("暂无学科", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("添加学科")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(Spacing.space4),
                    verticalArrangement = Arrangement.spacedBy(Spacing.space2)
                ) {
                    itemsIndexed(subjects) { index, subject ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = BgCard)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = subject,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    scope.launch {
                                        settingsDataStore.saveSubjectList(subjects.toMutableList().apply { removeAt(index) })
                                    }
                                }) {
                                    Icon(Icons.Default.Close, "删除", tint = Error)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false; newSubjectName = "" },
                title = { Text("添加学科") },
                text = {
                    OutlinedTextField(
                        value = newSubjectName,
                        onValueChange = { newSubjectName = it },
                        placeholder = { Text("输入学科名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val name = newSubjectName.trim()
                            if (name.isNotBlank() && name !in subjects) {
                                scope.launch {
                                    settingsDataStore.saveSubjectList(subjects + name)
                                }
                            }
                            newSubjectName = ""
                            showAddDialog = false
                        },
                        enabled = newSubjectName.trim().isNotBlank()
                    ) { Text("添加") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false; newSubjectName = "" }) { Text("取消") }
                }
            )
        }
    }
}
