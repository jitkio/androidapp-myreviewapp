package com.app.knowledgegraph.ui.smartscan

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.knowledgegraph.AppContainer
import com.app.knowledgegraph.data.db.entity.Card
import com.app.knowledgegraph.data.preferences.SettingsDataStore
import com.app.knowledgegraph.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartScanScreen(
    container: AppContainer,
    onNavigateBack: () -> Unit,
    viewModel: SmartScanViewModel = viewModel(factory = SmartScanViewModel.factory(container))
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val subjects by container.settingsDataStore.subjectListFlow.collectAsState(initial = SettingsDataStore.DEFAULT_SUBJECTS)
    val lastSubject by container.settingsDataStore.lastSubjectFlow.collectAsState(initial = "")
    var selectedSubject by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(subjects, lastSubject) {
        if (selectedSubject.isBlank()) {
            selectedSubject = if (lastSubject.isNotBlank() && lastSubject in subjects) lastSubject
            else subjects.firstOrNull() ?: ""
        }
    }

    LaunchedEffect(selectedSubject) {
        if (selectedSubject.isNotBlank()) {
            viewModel.setSubject(selectedSubject)
            scope.launch { container.settingsDataStore.saveLastSubject(selectedSubject) }
        }
    }

    var photoFilePath by rememberSaveable { mutableStateOf<String?>(null) }

    fun getImageDir(): File {
        val dir = File(context.cacheDir, "smart_images")
        dir.mkdirs()
        return dir
    }

    fun createCameraFileAndUri(): Pair<String, Uri> {
        val file = File(getImageDir(), "smart_" + System.currentTimeMillis() + ".jpg")
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        return file.absolutePath to uri
    }

    fun decodeBitmapFromFile(path: String): Bitmap? {
        val file = File(path)
        if (!file.exists() || file.length() == 0L) return null
        return try {
            BitmapFactory.decodeFile(path)
                ?: BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = 2 })
        } catch (_: OutOfMemoryError) {
            BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = 4 })
        }
    }

    fun copyUriToTempFile(uri: Uri): String? {
        return try {
            val tempFile = File(getImageDir(), "gallery_${System.currentTimeMillis()}.jpg")
            val input = context.contentResolver.openInputStream(uri) ?: return null
            input.use { src -> tempFile.outputStream().use { dst -> src.copyTo(dst) } }
            if (tempFile.exists() && tempFile.length() > 0) tempFile.absolutePath else null
        } catch (_: Exception) { null }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            val path = photoFilePath
            if (path != null) {
                scope.launch {
                    val bitmap = withContext(Dispatchers.IO) { decodeBitmapFromFile(path) }
                    if (bitmap != null) viewModel.processImage(bitmap)
                    else viewModel.resetToIdle()
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val (path, uri) = createCameraFileAndUri()
            photoFilePath = path
            cameraLauncher.launch(uri)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    val localPath = copyUriToTempFile(uri)
                    if (localPath != null) decodeBitmapFromFile(localPath) else null
                }
                if (result != null) {
                    viewModel.processImage(result)
                } else {
                    viewModel.resetToIdle()
                }
            }
        }
    }

    fun launchCamera() {
        val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            val (path, uri) = createCameraFileAndUri()
            photoFilePath = path
            cameraLauncher.launch(uri)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // 不再自动启动相机，让用户自己选择拍照或相册

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("智能扫描") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // 学科选择 Chip 栏
            if (subjects.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.space4, vertical = Spacing.space2),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.space2)
                ) {
                    subjects.forEach { subject ->
                        FilterChip(
                            selected = selectedSubject == subject,
                            onClick = { selectedSubject = subject },
                            label = { Text(subject) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary.copy(alpha = 0.15f),
                                selectedLabelColor = Primary
                            )
                        )
                    }
                }
            }

            when (uiState.phase) {
                SmartScanPhase.IDLE -> {
                    IdleContent(
                        uiState = uiState,
                        onCamera = { launchCamera() },
                        onGallery = { galleryLauncher.launch("image/*") }
                    )
                }
                SmartScanPhase.PROCESSING -> {
                    ProcessingContent(uiState = uiState)
                }
                SmartScanPhase.PREVIEW -> {
                    PreviewContent(
                        uiState = uiState,
                        onToggle = viewModel::toggleCard,
                        onSelectAll = viewModel::selectAll,
                        onSave = viewModel::saveSelected,
                        onBack = viewModel::resetToIdle
                    )
                }
                SmartScanPhase.SAVED -> {
                    SavedContent(
                        savedCount = uiState.savedCount,
                        onContinue = { viewModel.resetToIdle() },
                        onDone = onNavigateBack
                    )
                }
            }
        }
    }
}

@Composable
private fun IdleContent(uiState: SmartScanUiState, onCamera: () -> Unit, onGallery: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 中间区域：提示 + 错误
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.CameraAlt, null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "拍照或从相册导入\n自动提取知识卡片",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            if (uiState.isProcessing) {
                Card(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text(uiState.progressMessage.ifBlank { "AI 正在识别..." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            uiState.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                ) {
                    Text(error, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }

        // ★ 底栏：相册 + 快门 + 占位（和 ScanScreen 一样的布局）
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 相册按钮（左）
                IconButton(
                    onClick = onGallery,
                    enabled = !uiState.isProcessing,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, "相册", modifier = Modifier.size(28.dp))
                }

                // 快门按钮（中）
                Button(
                    onClick = onCamera,
                    enabled = !uiState.isProcessing,
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, "拍照", modifier = Modifier.size(32.dp))
                }

                // 占位（右，保持对称）
                Box(modifier = Modifier.size(48.dp))
            }
        }
    }
}

@Composable
private fun ProcessingContent(uiState: SmartScanUiState) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator()
            Text(uiState.progressMessage.ifBlank { "AI 正在识别..." },
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PreviewContent(
    uiState: SmartScanUiState,
    onToggle: (Int) -> Unit,
    onSelectAll: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("生成 ${uiState.generatedCards.size} 张卡片",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onSelectAll) {
                    Text(if (uiState.selectedIndices.size == uiState.generatedCards.size) "取消全选" else "全选")
                }
                TextButton(onClick = onBack) { Text("重新扫描") }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(uiState.generatedCards) { index, card ->
                CardPreviewItem(
                    index = index, card = card,
                    isSelected = uiState.selectedIndices.contains(index),
                    onToggle = { onToggle(index) }
                )
            }
        }

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
            enabled = uiState.selectedIndices.isNotEmpty()
        ) {
            Text("保存选中的 ${uiState.selectedIndices.size} 张卡片")
        }
    }
}

@Composable
private fun CardPreviewItem(index: Int, card: Card, isSelected: Boolean, onToggle: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Checkbox(checked = isSelected, onCheckedChange = { onToggle() })
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text(card.type.name, style = MaterialTheme.typography.labelSmall) }, modifier = Modifier.height(24.dp))
                    Text("#${index + 1}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(4.dp))
                Text(card.prompt, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (card.answer.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(card.answer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun SavedContent(savedCount: Int, onContinue: () -> Unit, onDone: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
            Text("成功保存 $savedCount 张卡片", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth(0.7f)) {
                Icon(Icons.Default.CameraAlt, null)
                Spacer(Modifier.width(8.dp))
                Text("继续扫描")
            }
            OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth(0.7f)) {
                Text("返回 Library")
            }
        }
    }
}


