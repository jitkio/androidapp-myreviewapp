package com.app.knowledgegraph.data.db.dao

import androidx.room.*
import com.app.knowledgegraph.data.db.entity.ErrorTag
import com.app.knowledgegraph.data.db.entity.ErrorType
import kotlinx.coroutines.flow.Flow

@Dao
interface ErrorTagDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(errorTag: ErrorTag): Long

    @Delete
    suspend fun delete(errorTag: ErrorTag)

    @Query("SELECT * FROM error_tags WHERE cardId = :cardId ORDER BY occurredAt DESC")
    fun observeByCard(cardId: Long): Flow<List<ErrorTag>>

    @Query("SELECT * FROM error_tags ORDER BY occurredAt DESC")
    fun observeAll(): Flow<List<ErrorTag>>

    /** 按错因类型统计数量 */
    @Query("SELECT errorType, COUNT(*) as count FROM error_tags GROUP BY errorType ORDER BY count DESC")
    fun observeErrorDistribution(): Flow<List<ErrorDistribution>>

    /** 获取某卡片的错误次数 */
    @Query("SELECT COUNT(*) FROM error_tags WHERE cardId = :cardId")
    suspend fun getErrorCountByCard(cardId: Long): Int

    /** 获取错误次数最多的卡片ID */
    @Query("""
        SELECT cardId, COUNT(*) as count 
        FROM error_tags 
        GROUP BY cardId 
        ORDER BY count DESC 
        LIMIT :limit
    """)
    fun observeTopErrorCards(limit: Int = 10): Flow<List<CardErrorCount>>
}

data class ErrorDistribution(
    val errorType: ErrorType,
    val count: Int
)

data class CardErrorCount(
    val cardId: Long,
    val count: Int
)
