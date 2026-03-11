package com.app.knowledgegraph.ui.map

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.knowledgegraph.AppContainer
import com.app.knowledgegraph.data.db.entity.Card
import com.app.knowledgegraph.data.db.entity.Edge
import com.app.knowledgegraph.data.db.entity.RelationType
import com.app.knowledgegraph.data.repository.GraphRepository
import com.app.knowledgegraph.data.repository.CardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

class DisplayNode(
    val card: Card,
    val depth: Int,
    var restX: Float,
    var restY: Float,
    var x: Float = restX,
    var y: Float = restY,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var offsetX: Float = 0f,
    var offsetY: Float = 0f,
    var brownVx: Float = 0f,
    var brownVy: Float = 0f
)

data class DisplayEdge(
    val edge: Edge,
    val sourceIndex: Int,
    val targetIndex: Int
)

class EdgeParticle(
    val edgeIndex: Int,
    var progress: Float = Random.nextFloat(),
    val speed: Float = 0.003f + Random.nextFloat() * 0.004f,
    val size: Float = 1.5f + Random.nextFloat() * 2f,
    val alpha: Float = 0.4f + Random.nextFloat() * 0.5f
)

data class MapUiState(
    val nodes: List<DisplayNode> = emptyList(),
    val edges: List<DisplayEdge> = emptyList(),
    val particles: List<EdgeParticle> = emptyList(),
    val centerId: Long? = null,
    val selectedNode: DisplayNode? = null,
    val allCards: List<Card> = emptyList(),
    val isLoading: Boolean = true,
    val edgeFilter: Set<RelationType> = RelationType.entries.toSet(),
    val hops: Int = 2
)

class MapViewModel(
    private val graphRepository: GraphRepository,
    private val cardRepository: CardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    // Animation state as Compose State - only triggers Canvas redraw, not full recomposition
    var scale = mutableFloatStateOf(1f)
    var camX = mutableFloatStateOf(0f)
    var camY = mutableFloatStateOf(0f)
    var frameTick = mutableLongStateOf(0L)
    var frameTime = mutableLongStateOf(0L)

    // 物理模拟是否已稳定
    var isSettled = mutableStateOf(false)

    var panVelocityX: Float = 0f
    var panVelocityY: Float = 0f

    private val springK = 0.06f
    private val damping = 0.85f
    private val velocityInfluence = 0.6f
    private val maxOffset = 60f

    private val brownForce = 0.35f
    private val brownDamping = 0.92f
    private val brownSpringK = 0.03f

    init {
        viewModelScope.launch {
            cardRepository.observeAll().collect { cards ->
                _uiState.update { it.copy(allCards = cards) }
                if (cards.isNotEmpty() && _uiState.value.centerId == null) {
                    loadGraph(cards.first().id)
                }
            }
        }
    }

    fun loadGraph(centerId: Long, width: Float = 1000f, height: Float = 1000f) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, centerId = centerId) }

            val localGraph = withContext(Dispatchers.IO) {
                graphRepository.getLocalGraph(centerId, _uiState.value.hops)
            }

            if (localGraph.nodes.isEmpty()) {
                _uiState.update { it.copy(isLoading = false, nodes = emptyList(), edges = emptyList(), particles = emptyList()) }
                return@launch
            }

            val cardIdToIndex = mutableMapOf<Long, Int>()
            val displayNodes = localGraph.nodes.mapIndexed { index, node ->
                cardIdToIndex[node.card.id] = index
                DisplayNode(
                    card = node.card,
                    depth = node.depth,
                    restX = width / 2f + Random.nextFloat() * 200f - 100f,
                    restY = height / 2f + Random.nextFloat() * 200f - 100f
                )
            }

            val filter = _uiState.value.edgeFilter
            val displayEdges = localGraph.edges.mapNotNull { edge ->
                if (edge.relation !in filter) return@mapNotNull null
                val si = cardIdToIndex[edge.sourceId] ?: return@mapNotNull null
                val ti = cardIdToIndex[edge.targetId] ?: return@mapNotNull null
                DisplayEdge(edge, si, ti)
            }

            // 力导向布局在 Default 线程执行，避免阻塞主线程
            withContext(Dispatchers.Default) {
                forceDirectedLayout(displayNodes, displayEdges, width, height)
            }

            displayNodes.forEach { n ->
                n.restX = n.x
                n.restY = n.y
                n.offsetX = 0f
                n.offsetY = 0f
                n.vx = 0f
                n.vy = 0f
                n.brownVx = (Random.nextFloat() - 0.5f) * 0.5f
                n.brownVy = (Random.nextFloat() - 0.5f) * 0.5f
            }

            val particles = mutableListOf<EdgeParticle>()
            displayEdges.forEachIndexed { idx, _ ->
                val count = 1 + Random.nextInt(2) // Reduced particle count
                repeat(count) { particles.add(EdgeParticle(edgeIndex = idx)) }
            }

            scale.floatValue = 1f
            camX.floatValue = 0f
            camY.floatValue = 0f

            _uiState.update {
                it.copy(
                    nodes = displayNodes,
                    edges = displayEdges,
                    particles = particles,
                    isLoading = false,
                    selectedNode = null
                )
            }
        }
    }

    fun updatePhysics(dt: Float) {
        val state = _uiState.value
        val nodes = state.nodes
        if (nodes.isEmpty()) return

        val friction = 0.92f
        panVelocityX *= friction
        panVelocityY *= friction

        var maxMovement = 0f

        nodes.forEach { node ->
            val depthFactor = 1f - node.depth * 0.15f
            val jitter = 0.9f + Random.nextFloat() * 0.2f

            node.brownVx += (Random.nextFloat() - 0.5f) * brownForce * dt
            node.brownVy += (Random.nextFloat() - 0.5f) * brownForce * dt
            node.brownVx += -brownSpringK * node.offsetX
            node.brownVy += -brownSpringK * node.offsetY
            node.brownVx *= brownDamping
            node.brownVy *= brownDamping

            node.vx += panVelocityX * velocityInfluence * depthFactor * jitter
            node.vy += panVelocityY * velocityInfluence * depthFactor * jitter
            node.vx += -springK * node.offsetX
            node.vy += -springK * node.offsetY
            node.vx *= damping
            node.vy *= damping

            val totalVx = node.vx + node.brownVx
            val totalVy = node.vy + node.brownVy
            node.offsetX = (node.offsetX + totalVx * dt).coerceIn(-maxOffset, maxOffset)
            node.offsetY = (node.offsetY + totalVy * dt).coerceIn(-maxOffset, maxOffset)
            node.x = node.restX + node.offsetX
            node.y = node.restY + node.offsetY

            maxMovement = maxOf(maxMovement, abs(totalVx) + abs(totalVy))
        }

        state.particles.forEach { p ->
            p.progress += p.speed * dt
            if (p.progress > 1f) p.progress -= 1f
        }

        // 当所有节点运动量极小且无用户拖拽时，标记为稳定
        val panActive = abs(panVelocityX) > 0.01f || abs(panVelocityY) > 0.01f
        isSettled.value = maxMovement < 0.05f && !panActive

        // Only update the tick to trigger Canvas invalidation, not full recomposition
        frameTime.longValue = System.currentTimeMillis()
        frameTick.longValue++
    }

    fun onPanDelta(dx: Float, dy: Float) {
        panVelocityX = dx * 0.3f
        panVelocityY = dy * 0.3f
        camX.floatValue += dx
        camY.floatValue += dy
        isSettled.value = false
    }

    fun onZoom(zoomDelta: Float) {
        scale.floatValue = (scale.floatValue * zoomDelta).coerceIn(0.3f, 3f)
        isSettled.value = false
    }

    fun selectNode(node: DisplayNode?) {
        _uiState.update { it.copy(selectedNode = node) }
    }

    fun focusNode(cardId: Long) { loadGraph(cardId) }

    fun toggleEdgeFilter(type: RelationType) {
        _uiState.update { state ->
            val newFilter = state.edgeFilter.toMutableSet()
            if (type in newFilter) newFilter.remove(type) else newFilter.add(type)
            state.copy(edgeFilter = newFilter)
        }
        _uiState.value.centerId?.let { loadGraph(it) }
    }

    private fun forceDirectedLayout(
        nodes: List<DisplayNode>, edges: List<DisplayEdge>,
        width: Float, height: Float, iterations: Int = 120
    ) {
        if (nodes.size <= 1) return
        val centerX = width / 2f
        val centerY = height / 2f
        val k = sqrt(width * height / nodes.size.toFloat()) * 0.6f
        var temperature = width / 4f

        nodes.forEachIndexed { i, node ->
            val angle = 2.0 * Math.PI * i / nodes.size
            node.x = centerX + (k * Math.cos(angle)).toFloat()
            node.y = centerY + (k * Math.sin(angle)).toFloat()
        }

        for (iter in 0 until iterations) {
            val dx = FloatArray(nodes.size)
            val dy = FloatArray(nodes.size)
            for (i in nodes.indices) {
                for (j in i + 1 until nodes.size) {
                    val diffX = nodes[i].x - nodes[j].x
                    val diffY = nodes[i].y - nodes[j].y
                    val dist = sqrt(diffX * diffX + diffY * diffY).coerceAtLeast(1f)
                    val force = k * k / dist
                    val fx = diffX / dist * force
                    val fy = diffY / dist * force
                    dx[i] += fx; dy[i] += fy; dx[j] -= fx; dy[j] -= fy
                }
            }
            for (e in edges) {
                val diffX = nodes[e.sourceIndex].x - nodes[e.targetIndex].x
                val diffY = nodes[e.sourceIndex].y - nodes[e.targetIndex].y
                val dist = sqrt(diffX * diffX + diffY * diffY).coerceAtLeast(1f)
                val force = dist * dist / k * 0.3f
                val fx = diffX / dist * force
                val fy = diffY / dist * force
                dx[e.sourceIndex] -= fx; dy[e.sourceIndex] -= fy
                dx[e.targetIndex] += fx; dy[e.targetIndex] += fy
            }
            for (i in nodes.indices) {
                dx[i] += (centerX - nodes[i].x) * 0.01f
                dy[i] += (centerY - nodes[i].y) * 0.01f
            }
            for (i in nodes.indices) {
                val disp = sqrt(dx[i] * dx[i] + dy[i] * dy[i]).coerceAtLeast(1f)
                val limitedDisp = minOf(disp, temperature)
                nodes[i].x += dx[i] / disp * limitedDisp
                nodes[i].y += dy[i] / disp * limitedDisp
                nodes[i].x = nodes[i].x.coerceIn(80f, width - 80f)
                nodes[i].y = nodes[i].y.coerceIn(80f, height - 80f)
            }
            temperature *= 0.95f
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MapViewModel(container.graphRepository, container.cardRepository) as T
            }
        }
    }
}
