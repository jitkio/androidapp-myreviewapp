package com.app.knowledgegraph.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.knowledgegraph.AppContainer
import com.app.knowledgegraph.data.db.entity.ImportedQuestion
import com.app.knowledgegraph.data.db.entity.QuestionType
import com.app.knowledgegraph.ui.components.MathText
import java.io.File

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    container: AppContainer,
    onNavigateBack: () -> Unit,
    onNavigateToQuiz: () -> Unit,
    viewModel: ScanViewModel = viewModel(factory = ScanViewModel.factory(container))
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var photoFilePath by rememberSaveable { mutableStateOf<String?>(null) }

    fun getImageDir(): File {
        val dir = File(context.cacheDir, "images")
        dir.mkdirs()
        return dir
    }

    fun createCameraFileAndUri(): Pair<String, Uri> {
        val file = File(getImageDir(), "scan_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
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
        } catch (e: Exception) { null }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            val path = photoFilePath
            if (path != null) {
                // 在 IO 线程解码 Bitmap，避免阻塞主线程
                scope.launch {
                    val bitmap = withContext(Dispatchers.IO) { decodeBitmapFromFile(path) }
                    if (bitmap != null) {
                        viewModel.processImage(bitmap)
                    } else {
                        val f = File(path)
                        viewModel.setError("拍照解码失败\n文件: ${f.exists()}, 大小: ${f.length()} bytes")
                    }
                }
            } else {
                viewModel.setError("拍照路径丢失，请重试")
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val (path, uri) = createCameraFileAndUri()
            photoFilePath = path
            cameraLauncher.launch(uri)
        } else {
            viewModel.setError("需要相机权限才能拍照，请在系统设置中授予权限")
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            // 在 IO 线程拷贝文件和解码 Bitmap
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    val localPath = copyUriToTempFile(uri)
                    if (localPath != null) decodeBitmapFromFile(localPath) to localPath
                    else null to null
                }
                val (bitmap, localPath) = result
                if (bitmap != null) {
                    viewModel.processImage(bitmap)
                } else if (localPath != null) {
                    viewModel.setError("图片解码失败\n文件大小: ${File(localPath).length()} bytes")
                } else {
                    viewModel.setError("无法读取图片，请尝试直接选择照片而非裁剪")
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("扫描导入") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        when (uiState.phase) {
            ScanPhase.IDLE -> IdlePhase(
                uiState = uiState,
                onSourceChange = viewModel::updateSourceLabel,
                onCamera = { launchCamera() },
                onGallery = { galleryLauncher.launch("image/*") },
                modifier = Modifier.padding(padding)
            )
            ScanPhase.PREVIEW -> PreviewPhase(
                uiState = uiState,
                onToggle = viewModel::toggleQuestion,
                onSelectAll = viewModel::selectAll,
                onSave = viewModel::saveSelected,
                onBack = viewModel::resetToIdle,
                modifier = Modifier.padding(padding)
            )
            ScanPhase.SAVED -> SavedPhase(
                savedCount = uiState.savedCount,
                onContinueScan = viewModel::resetToIdle,
                onGoToQuiz = onNavigateToQuiz,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun IdlePhase(
    uiState: ScanUiState,
    onSourceChange: (String) -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Top area: API key warning + source label
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!uiState.apiKeyPresent) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("请先在「Me → API Key 设置」中配置 API Key",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            OutlinedTextField(
                value = uiState.sourceLabel,
                onValueChange = onSourceChange,
                label = { Text("来源标签") },
                placeholder = { Text("例如：高数第三章、期中试卷") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        // Center area: camera illustration + hint + processing/error overlays
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
                    "拍照或从相册导入题目",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Processing overlay
            if (uiState.isProcessing) {
                Card(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text(uiState.progressMessage.ifBlank { "AI 正在识别题目..." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Error overlay
            uiState.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                ) {
                    Text(error, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }

        // Bottom bar: gallery + shutter + spacer
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery button (left)
                IconButton(
                    onClick = onGallery,
                    enabled = !uiState.isProcessing,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, "相册", modifier = Modifier.size(28.dp))
                }

                // Shutter button (center)
                Button(
                    onClick = onCamera,
                    enabled = !uiState.isProcessing,
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, "拍照", modifier = Modifier.size(32.dp))
                }

                // Spacer for symmetry (right)
                Box(modifier = Modifier.size(48.dp))
            }
        }
    }
}

@Composable
private fun PreviewPhase(
    uiState: ScanUiState,
    onToggle: (Int) -> Unit,
    onSelectAll: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("识别到 ${uiState.extractedQuestions.size} 道题",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onSelectAll) {
                    Text(if (uiState.selectedIndices.size == uiState.extractedQuestions.size) "取消全选" else "全选")
                }
                TextButton(onClick = onBack) { Text("重新扫描") }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(uiState.extractedQuestions, key = { index, _ -> index }) { index, question ->
                QuestionPreviewCard(
                    index = index,
                    question = question,
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
            Text("保存选中的 ${uiState.selectedIndices.size} 道题")
        }
    }
}

@Composable
private fun QuestionPreviewCard(
    index: Int,
    question: ImportedQuestion,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                when (question.type) {
                                    QuestionType.SINGLE_CHOICE -> "单选"
                                    QuestionType.MULTI_CHOICE -> "多选"
                                    QuestionType.FILL_BLANK -> "填空"
                                    QuestionType.TRUE_FALSE -> "判断"
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier.height(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("#${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(4.dp))
                MathText(
                    text = question.stem,
                    modifier = Modifier.fillMaxWidth()
                )
                if (question.answer.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text("答案: ${question.answer}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun SavedPhase(
    savedCount: Int,
    onContinueScan: () -> Unit,
    onGoToQuiz: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
            Text("成功保存 $savedCount 道题",
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Button(onClick = onGoToQuiz, modifier = Modifier.fillMaxWidth(0.7f)) {
                Icon(Icons.Default.Quiz, null)
                Spacer(Modifier.width(8.dp))
                Text("去刷题")
            }
            OutlinedButton(onClick = onContinueScan, modifier = Modifier.fillMaxWidth(0.7f)) {
                Icon(Icons.Default.CameraAlt, null)
                Spacer(Modifier.width(8.dp))
                Text("继续扫描")
            }
        }
    }
}
