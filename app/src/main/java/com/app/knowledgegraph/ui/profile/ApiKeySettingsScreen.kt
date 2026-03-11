package com.app.knowledgegraph.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.app.knowledgegraph.data.preferences.SettingsDataStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeySettingsScreen(
    settingsDataStore: SettingsDataStore,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val savedKey by settingsDataStore.apiKeyFlow.collectAsState(initial = "")
    val savedBaseUrl by settingsDataStore.apiBaseUrlFlow.collectAsState(initial = SettingsDataStore.DEFAULT_BASE_URL)
    val savedModel by settingsDataStore.apiModelFlow.collectAsState(initial = SettingsDataStore.DEFAULT_MODEL)

    var keyInput by remember(savedKey) { mutableStateOf(savedKey) }
    var baseUrlInput by remember(savedBaseUrl) { mutableStateOf(savedBaseUrl) }
    var modelInput by remember(savedModel) { mutableStateOf(savedModel) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showSnackbar by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API 设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "使用说明",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "扫题功能需要支持图片识别的 AI API。\n" +
                        "推荐使用硅基流动 (SiliconFlow)，注册即送额度：\n" +
                        "1. 前往 siliconflow.cn 注册并获取 API Key\n" +
                        "2. 下方地址和模型已填好默认值，直接填 Key 即可",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Text("API Key", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                label = { Text("API Key") },
                placeholder = { Text("sk-...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                            if (passwordVisible) "隐藏" else "显示"
                        )
                    }
                }
            )

            HorizontalDivider()

            Text("API 地址", style = MaterialTheme.typography.titleMedium)
            Text(
                "OpenAI 兼容格式的 Base URL（不含 /chat/completions）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = baseUrlInput,
                onValueChange = { baseUrlInput = it },
                label = { Text("Base URL") },
                placeholder = { Text(SettingsDataStore.DEFAULT_BASE_URL) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text("模型名称", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = modelInput,
                onValueChange = { modelInput = it },
                label = { Text("Model") },
                placeholder = { Text(SettingsDataStore.DEFAULT_MODEL) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text(
                "常用视觉模型：\n" +
                "  硅基流动: Qwen/Qwen2.5-VL-72B-Instruct\n" +
                "  OpenAI: gpt-4o\n" +
                "  注意：DeepSeek 官方 API 暂不支持图片",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = {
                    scope.launch {
                        settingsDataStore.saveApiKey(keyInput.trim())
                        settingsDataStore.saveApiBaseUrl(baseUrlInput.trim().ifBlank { SettingsDataStore.DEFAULT_BASE_URL })
                        settingsDataStore.saveApiModel(modelInput.trim().ifBlank { SettingsDataStore.DEFAULT_MODEL })
                        showSnackbar = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = keyInput.isNotBlank()
            ) {
                Text("保存")
            }

            if (showSnackbar) {
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000)
                    showSnackbar = false
                }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        "设置已保存",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
