package com.app.knowledgegraph.data.db.dao

import androidx.room.*
import com.app.knowledgegraph.data.db.entity.Edge
import com.app.knowledgegraph.data.db.entity.RelationType
import kotlinx.coroutines.flow.Flow

@Dao
interface EdgeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(edge: Edge): Long

    @Update
    suspend fun update(edge: Edge)

    @Delete
    suspend fun delete(edge: Edge)

    @Query("SELECT * FROM edges WHERE id = :id")
    suspend fun getById(id: Long): Edge?

    /** 获取与某节点相关的所有边（作为 source 或 target） */
    @Query("""
        SELECT * FROM edges 
        WHERE sourceId = :cardId OR targetId = :cardId
    """)
    suspend fun getNeighbors(cardId: Long): List<Edge>

    /** 观察与某节点相关的所有边 */
    @Query("""
        SELECT * FROM edges 
        WHERE sourceId = :cardId OR targetId = :cardId
    """)
    fun observeNeighbors(cardId: Long): Flow<List<Edge>>

    /** 获取两个节点之间的边 */
    @Query("""
        SELECT * FROM edges 
        WHERE (sourceId = :cardA AND targetId = :cardB)
           OR (sourceId = :cardB AND targetId = :cardA)
    """)
    suspend fun getEdgeBetween(cardA: Long, cardB: Long): List<Edge>

    /** 按关系类型筛选 */
    @Query("""
        SELECT * FROM edges 
        WHERE (sourceId = :cardId OR targetId = :cardId)
          AND relation = :relation
    """)
    suspend fun getNeighborsByRelation(cardId: Long, relation: RelationType): List<Edge>

    @Query("SELECT * FROM edges")
    fun observeAll(): Flow<List<Edge>>

    @Query("SELECT * FROM edges")
    suspend fun getAll(): List<Edge>

    @Query("SELECT COUNT(*) FROM edges")
    fun observeCount(): Flow<Int>

    /** 删除某卡片相关的所有边 */
    @Query("DELETE FROM edges WHERE sourceId = :cardId OR targetId = :cardId")
    suspend fun deleteByCard(cardId: Long)
}
