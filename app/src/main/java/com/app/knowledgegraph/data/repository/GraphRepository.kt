package com.app.knowledgegraph.data.repository

import com.app.knowledgegraph.data.db.dao.CardDao
import com.app.knowledgegraph.data.db.dao.EdgeDao
import com.app.knowledgegraph.data.db.entity.Card
import com.app.knowledgegraph.data.db.entity.Edge
import com.app.knowledgegraph.data.db.entity.RelationType
import kotlinx.coroutines.flow.Flow
import java.util.LinkedList

/** 图谱中的节点（附带深度信息） */
data class GraphNode(
    val card: Card,
    val depth: Int
)

/** 局部图谱数据 */
data class LocalGraph(
    val nodes: List<GraphNode>,
    val edges: List<Edge>
)

class GraphRepository(
    private val cardDao: CardDao,
    private val edgeDao: EdgeDao
) {

    fun observeAllEdges(): Flow<List<Edge>> = edgeDao.observeAll()

    fun observeNeighborEdges(cardId: Long): Flow<List<Edge>> =
        edgeDao.observeNeighbors(cardId)

    suspend fun addEdge(
        sourceId: Long,
        targetId: Long,
        relation: RelationType,
        description: String = ""
    ): Long {
        return edgeDao.insert(
            Edge(
                sourceId = sourceId,
                targetId = targetId,
                relation = relation,
                description = description
            )
        )
    }

    suspend fun deleteEdge(edge: Edge) = edgeDao.delete(edge)

    /**
     * BFS 获取局部图谱（优化版：批量加载，避免 N+1 查询）
     * @param centerId 中心节点ID
     * @param maxHops 最大跳数
     */
    suspend fun getLocalGraph(centerId: Long, maxHops: Int = 2): LocalGraph {
        // 一次性加载所有边到内存，避免逐节点查询
        val allEdges = edgeDao.getAll()

        // 构建邻接表
        val adjacency = mutableMapOf<Long, MutableList<Edge>>()
        for (edge in allEdges) {
            adjacency.getOrPut(edge.sourceId) { mutableListOf() }.add(edge)
            adjacency.getOrPut(edge.targetId) { mutableListOf() }.add(edge)
        }

        // BFS 遍历
        val visited = mutableSetOf<Long>()
        val queue: LinkedList<Pair<Long, Int>> = LinkedList()
        val nodeDepths = mutableMapOf<Long, Int>()
        val resultEdges = mutableListOf<Edge>()
        val addedEdgeIds = mutableSetOf<Long>()

        queue.add(centerId to 0)
        visited.add(centerId)

        while (queue.isNotEmpty()) {
            val (nodeId, depth) = queue.poll()
            nodeDepths[nodeId] = depth

            if (depth >= maxHops) continue

            val neighborEdges = adjacency[nodeId] ?: continue
            for (edge in neighborEdges) {
                if (edge.id !in addedEdgeIds) {
                    resultEdges.add(edge)
                    addedEdgeIds.add(edge.id)
                }

                val neighborId = if (edge.sourceId == nodeId) edge.targetId else edge.sourceId
                if (neighborId !in visited) {
                    visited.add(neighborId)
                    queue.add(neighborId to depth + 1)
                }
            }
        }

        // 批量加载所有需要的 Card
        val cards = cardDao.getByIds(visited.toList()).associateBy { it.id }
        val nodes = nodeDepths.mapNotNull { (id, depth) ->
            val card = cards[id] ?: return@mapNotNull null
            GraphNode(card, depth)
        }

        return LocalGraph(nodes, resultEdges)
    }

    /**
     * BFS 最短路径查找
     */
    suspend fun findShortestPath(fromId: Long, toId: Long): List<Long>? {
        val visited = mutableSetOf<Long>()
        val queue: LinkedList<List<Long>> = LinkedList()

        queue.add(listOf(fromId))
        visited.add(fromId)

        while (queue.isNotEmpty()) {
            val path = queue.poll()
            val current = path.last()

            if (current == toId) return path

            val neighborEdges = edgeDao.getNeighbors(current)
            for (edge in neighborEdges) {
                val neighborId = if (edge.sourceId == current) edge.targetId else edge.sourceId
                if (neighborId !in visited) {
                    visited.add(neighborId)
                    queue.add(path + neighborId)
                }
            }
        }

        return null // 无法到达
    }
}
