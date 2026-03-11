package com.app.knowledgegraph.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.knowledgegraph.AppContainer
import com.app.knowledgegraph.ui.navigation.AppNavState
import com.app.knowledgegraph.ui.navigation.IncomingFrom
import com.app.knowledgegraph.data.db.entity.CardType
import com.app.knowledgegraph.data.db.entity.RelationType
import com.app.knowledgegraph.ui.navigation.DetailRoutes
import com.app.knowledgegraph.ui.theme.*
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    container: AppContainer,
    appNavState: AppNavState,
    viewModel: MapViewModel = viewModel(factory = MapViewModel.factory(container))
) {
    val uiState by viewModel.uiState.collectAsState()
    val density = LocalDensity.current

    // Cached Paint object to avoid GC pressure
    val labelPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.argb(220, 50, 55, 70)
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    LaunchedEffect(Unit) {
        var lastTime = System.nanoTime()
        while (true) {
            // 当物理已稳定时降频到 2fps 节省电量，交互时恢复 60fps
            if (viewModel.isSettled.value) {
                delay(500)
            }
            awaitFrame()
            val now = System.nanoTime()
            val dt = ((now - lastTime) / 1_000_000f).coerceAtMost(32f) / 16f
            lastTime = now
            viewModel.updatePhysics(dt)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(BgBase)) {
        TopAppBar(
            title = { Text("Map", style = MaterialTheme.typography.titleLarge, color = TextPrimary) },
            windowInsets = WindowInsets(0),
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BgBase)
        )

        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Spacing.space3, vertical = Spacing.space1),
            horizontalArrangement = Arrangement.spacedBy(Spacing.space2)
        ) {
            RelationType.entries.forEach { type ->
                FilterChip(
                    selected = type in uiState.edgeFilter,
                    onClick = { viewModel.toggleEdgeFilter(type) },
                    label = { Text(relationLabel(type), style = MaterialTheme.typography.bodySmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = BgElevated,
                        labelColor = TextSecondary,
                        selectedContainerColor = Primary.copy(alpha = 0.15f),
                        selectedLabelColor = Primary
                    )
                )
            }
        }

        if (uiState.isLoading) {
            com.app.knowledgegraph.ui.components.LoadingState(message = "加载知识图谱...")
        } else if (uiState.nodes.isEmpty()) {
            com.app.knowledgegraph.ui.components.EmptyState(
                title = "暂无图谱数据",
                description = "前往 Library 添加卡片并建立关系，构建你的知识网络"
            )
        } else {
            Box(modifier = Modifier.weight(1f)) {
                val selectedId = uiState.selectedNode?.card?.id
                val nodes = uiState.nodes
                val edges = uiState.edges
                val particles = uiState.particles
                val centerId = uiState.centerId

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BgBase)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                viewModel.onPanDelta(pan.x, pan.y)
                                if (zoom != 1f) viewModel.onZoom(zoom)
                            }
                        }
                        .pointerInput(nodes) {
                            detectTapGestures { tapOffset ->
                                val s = viewModel.scale.floatValue
                                val cx = viewModel.camX.floatValue
                                val cy = viewModel.camY.floatValue
                                val tapped = nodes.find { node ->
                                    val sx = node.x * s + cx
                                    val sy = node.y * s + cy
                                    val ddx = tapOffset.x - sx
                                    val ddy = tapOffset.y - sy
                                    sqrt(ddx * ddx + ddy * ddy) < 40f * s
                                }
                                viewModel.selectNode(tapped)
                            }
                        }
                ) {
                    // Reading Compose State here triggers only Canvas redraw
                    val scale = viewModel.scale.floatValue
                    val cx = viewModel.camX.floatValue
                    val cy = viewModel.camY.floatValue
                    val ft = viewModel.frameTime.longValue
                    val tick = viewModel.frameTick.longValue

                    drawGrid(scale, cx, cy)

                    // Draw edges
                    edges.forEach { de ->
                        val src = nodes[de.sourceIndex]
                        val tgt = nodes[de.targetIndex]
                        val x1 = src.x * scale + cx
                        val y1 = src.y * scale + cy
                        val x2 = tgt.x * scale + cx
                        val y2 = tgt.y * scale + cy
                        val edgeColor = relationGlowColor(de.edge.relation)

                        drawLine(color = edgeColor.copy(alpha = 0.12f), start = Offset(x1, y1), end = Offset(x2, y2), strokeWidth = 6f * scale)
                        drawLine(color = edgeColor.copy(alpha = 0.6f), start = Offset(x1, y1), end = Offset(x2, y2), strokeWidth = 1f * scale)
                    }

                    // Draw particles
                    particles.forEach { p ->
                        if (p.edgeIndex < edges.size) {
                            val de = edges[p.edgeIndex]
                            val src = nodes[de.sourceIndex]
                            val tgt = nodes[de.targetIndex]
                            val x1 = src.x * scale + cx
                            val y1 = src.y * scale + cy
                            val x2 = tgt.x * scale + cx
                            val y2 = tgt.y * scale + cy
                            val px = x1 + (x2 - x1) * p.progress
                            val py = y1 + (y2 - y1) * p.progress
                            val pColor = relationGlowColor(de.edge.relation)

                            drawCircle(color = pColor.copy(alpha = p.alpha * 0.3f), radius = p.size * 3f * scale, center = Offset(px, py))
                            drawCircle(color = pColor.copy(alpha = p.alpha), radius = p.size * scale, center = Offset(px, py))
                        }
                    }

                    // Draw nodes - set shadow/textSize once before loop
                    labelPaint.textSize = 10.5f * scale * density.density
                    labelPaint.setShadowLayer(4f * scale, 0f, 0f, android.graphics.Color.argb(40, 0, 0, 0))

                    nodes.forEach { node ->
                        val sx = node.x * scale + cx
                        val sy = node.y * scale + cy
                        val isCenter = node.card.id == centerId
                        val isSelected = node.card.id == selectedId
                        val baseRadius = (if (isCenter) 28f else 22f) * scale
                        val nodeColor = nodeGlowColor(node.card.type)

                        val pulsePhase = ((ft / 1500.0) + node.card.id * 0.3).let { kotlin.math.sin(it) }.toFloat()

                        // Simplified glow: 2 layers instead of 3
                        drawCircle(color = nodeColor.copy(alpha = 0.08f), radius = baseRadius * 2.5f, center = Offset(sx, sy))
                        drawCircle(color = nodeColor.copy(alpha = 0.15f), radius = baseRadius * 1.4f, center = Offset(sx, sy))

                        if (isSelected) {
                            drawCircle(color = Color.White.copy(alpha = 0.15f), radius = baseRadius * 2.5f, center = Offset(sx, sy))
                        }

                        drawCircle(color = BgBase.copy(alpha = 0.7f), radius = baseRadius, center = Offset(sx, sy))
                        drawCircle(color = nodeColor.copy(alpha = 0.8f), radius = baseRadius, center = Offset(sx, sy), style = Stroke(width = 2f * scale))
                        drawCircle(color = nodeColor.copy(alpha = 0.12f), radius = baseRadius, center = Offset(sx, sy))

                        val dotRadius = 3.5f * scale
                        drawCircle(color = nodeColor.copy(alpha = 0.7f + pulsePhase * 0.2f), radius = dotRadius, center = Offset(sx, sy))

                        // Reuse cached Paint (textSize/shadow already set before loop)
                        drawContext.canvas.nativeCanvas.apply {
                            val label = if (node.card.prompt.length > 8) node.card.prompt.take(8) + ".." else node.card.prompt
                            drawText(label, sx, sy + baseRadius + 16f * scale, labelPaint)
                        }
                    }
                }

                uiState.selectedNode?.let { node ->
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = BgCard),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text(node.card.type.name, color = TextPrimary) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = BgElevated)
                                )
                                Text(node.card.chapter, style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    modifier = Modifier.align(Alignment.CenterVertically))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(node.card.prompt, style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { viewModel.focusNode(node.card.id) }) {
                                    Text("\u4ee5\u6b64\u4e3a\u4e2d\u5fc3", color = TextSecondary)
                                }
                                Button(
                                    onClick = { appNavState.navigate(DetailRoutes.cardDetail(node.card.id), IncomingFrom.BOTTOM) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                                ) {
                                    Text("\u67e5\u770b\u8be6\u60c5")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawGrid(scale: Float, cx: Float, cy: Float) {
    val gridSize = 80f * scale
    val startX = (cx % gridSize) - gridSize
    val startY = (cy % gridSize) - gridSize
    var gx = startX
    while (gx < size.width + gridSize) {
        drawLine(BgElevated, Offset(gx, 0f), Offset(gx, size.height), 0.5f)
        gx += gridSize
    }
    var gy = startY
    while (gy < size.height + gridSize) {
        drawLine(BgElevated, Offset(0f, gy), Offset(size.width, gy), 0.5f)
        gy += gridSize
    }
}

private fun nodeGlowColor(type: CardType): Color = when (type) {
    CardType.CONCEPT -> Primary
    CardType.METHOD -> Secondary
    CardType.TEMPLATE -> Warning
    CardType.BOUNDARY -> Error
}

private fun relationGlowColor(type: RelationType): Color = when (type) {
    RelationType.REQUIRES -> Color(0xFF7B68EE)
    RelationType.EQUIVALENT -> Color(0xFF4ECDC4)
    RelationType.FAILS_WHEN -> Color(0xFFFF6B6B)
    RelationType.WORKFLOW -> Color(0xFFFFBE76)
    RelationType.CONTRADICTS -> Color(0xFFFF4757)
    RelationType.EXTENDS -> Color(0xFF6C7A89)
}

private fun relationLabel(type: RelationType): String = when (type) {
    RelationType.REQUIRES -> "\u524d\u7f6e"
    RelationType.EQUIVALENT -> "\u7b49\u4ef7"
    RelationType.FAILS_WHEN -> "\u5931\u6548"
    RelationType.WORKFLOW -> "\u6d41\u7a0b"
    RelationType.CONTRADICTS -> "\u4e92\u65a5"
    RelationType.EXTENDS -> "\u6269\u5c55"
}
