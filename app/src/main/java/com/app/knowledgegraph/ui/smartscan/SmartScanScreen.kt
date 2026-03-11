package com.app.knowledgegraph.ui.smartscan

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.knowledgegraph.AppContainer
import com.app.knowledgegraph.data.db.entity.Card
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartScanScreen(
    container: AppContainer,
    subject: String,
    onNavigateBack: () -> Unit,
    viewModel: SmartScanViewModel = viewModel(factory = SmartScanViewModel.factory(container))
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(subject) { viewModel.setSubject(subject) }

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

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            val path = photoFilePath
            if (path != null) {
                val bitmap = decodeBitmapFromFile(path)
                if (bitmap != null) viewModel.processImage(bitmap)
                else viewModel.resetToIdle()
            }
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val (path, uri) = createCameraFileAndUri()
            photoFilePath = path
            cameraLauncher.launch(uri)
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

    // Auto-launch camera on first open
    var cameraLaunched by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!cameraLaunched && uiState.phase == SmartScanPhase.IDLE) {
            cameraLaunched = true
            launchCamera()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("\u667a\u80fd\u626b\u63cf - " + subject) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "\u8fd4\u56de")
                    }
                }
            )
        }
    ) { padding ->
        when (uiState.phase) {
            SmartScanPhase.IDLE -> {
                IdleContent(
                    uiState = uiState,
                    onCamera = { launchCamera() },
                    modifier = Modifier.padding(padding)
                )
            }
            SmartScanPhase.PROCESSING -> {
                ProcessingContent(uiState = uiState, modifier = Modifier.padding(padding))
            }
            SmartScanPhase.PREVIEW -> {
                PreviewContent(
                    uiState = uiState,
                    onToggle = viewModel::toggleCard,
                    onSelectAll = viewModel::selectAll,
                    onSave = viewModel::saveSelected,
                    onBack = viewModel::resetToIdle,
                    modifier = Modifier.padding(padding)
                )
            }
            SmartScanPhase.SAVED -> {
                SavedContent(
                    savedCount = uiState.savedCount,
                    onContinue = { cameraLaunched = false; viewModel.resetToIdle() },
                    onDone = onNavigateBack,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun IdleContent(uiState: SmartScanUiState, onCamera: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (uiState.error != null) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(uiState.error, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
            Spacer(Modifier.height(16.dp))
        }
        Button(onClick = onCamera, modifier = Modifier.height(56.dp)) {
            Icon(Icons.Default.CameraAlt, null)
            Spacer(Modifier.width(8.dp))
            Text("\u62cd\u7167\u8bc6\u522b")
        }
    }
}

@Composable
private fun ProcessingContent(uiState: SmartScanUiState, modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator()
            Text(uiState.progressMessage.ifBlank { "AI \u6b63\u5728\u8bc6\u522b..." },
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
    onBack: () -> Unit,
    modifier: Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("\u751f\u6210 " + uiState.generatedCards.size + " \u5f20\u5361\u7247",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onSelectAll) {
                    Text(if (uiState.selectedIndices.size == uiState.generatedCards.size) "\u53d6\u6d88\u5168\u9009" else "\u5168\u9009")
                }
                TextButton(onClick = onBack) { Text("\u91cd\u65b0\u626b\u63cf") }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(uiState.generatedCards) { index, card ->
                CardPreviewItem(
                    index = index,
                    card = card,
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
            Text("\u4fdd\u5b58\u9009\u4e2d\u7684 " + uiState.selectedIndices.size + " \u5f20\u5361\u7247")
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
                    Text("#" + (index + 1), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun SavedContent(savedCount: Int, onContinue: () -> Unit, onDone: () -> Unit, modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
            Text("\u6210\u529f\u4fdd\u5b58 " + savedCount + " \u5f20\u5361\u7247", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth(0.7f)) {
                Icon(Icons.Default.CameraAlt, null)
                Spacer(Modifier.width(8.dp))
                Text("\u7ee7\u7eed\u626b\u63cf")
            }
            OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth(0.7f)) {
                Text("\u8fd4\u56de Library")
            }
        }
    }
}
