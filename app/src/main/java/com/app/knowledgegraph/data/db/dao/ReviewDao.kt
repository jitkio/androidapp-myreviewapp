package com.app.knowledgegraph.data.db.dao

import androidx.room.*
import com.app.knowledgegraph.data.db.entity.Card
import com.app.knowledgegraph.data.db.entity.ReviewSchedule
import com.app.knowledgegraph.data.db.entity.ReviewStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: ReviewSchedule)

    @Update
    suspend fun update(schedule: ReviewSchedule)

    @Query("SELECT * FROM review_schedules WHERE cardId = :cardId")
    suspend fun getByCardId(cardId: Long): ReviewSchedule?

    @Query("SELECT * FROM review_schedules WHERE cardId = :cardId")
    fun observeByCardId(cardId: Long): Flow<ReviewSchedule?>

    /** 获取到期卡片（nextReviewDate ≤ 今天） */
    @Query("""
        SELECT * FROM review_schedules 
        WHERE nextReviewDate <= :todayMillis
          AND status != 'NEW'
        ORDER BY nextReviewDate ASC
    """)
    fun observeDueCards(todayMillis: Long): Flow<List<ReviewSchedule>>

    /** 获取到期卡片（一次性） */
    @Query("""
        SELECT * FROM review_schedules 
        WHERE nextReviewDate <= :todayMillis
          AND status != 'NEW'
        ORDER BY nextReviewDate ASC
    """)
    suspend fun getDueCards(todayMillis: Long): List<ReviewSchedule>

    /** 获取新卡片 */
    @Query("""
        SELECT * FROM review_schedules 
        WHERE status = 'NEW'
        ORDER BY cardId ASC
        LIMIT :limit
    """)
    fun observeNewCards(limit: Int = 10): Flow<List<ReviewSchedule>>

    /** 获取弱点卡片（ease factor 较低） */
    @Query("""
        SELECT * FROM review_schedules 
        WHERE easeFactor < 1.8
          AND totalReviews > 0
        ORDER BY easeFactor ASC
    """)
    fun observeWeakCards(): Flow<List<ReviewSchedule>>

    /** 获取弱点卡片（连续失败重学的） */
    @Query("""
        SELECT * FROM review_schedules 
        WHERE status = 'RELEARNING'
        ORDER BY nextReviewDate ASC
    """)
    fun observeRelearningCards(): Flow<List<ReviewSchedule>>

    /** 统计各状态卡片数量 */
    @Query("SELECT COUNT(*) FROM review_schedules WHERE status = :status")
    fun observeCountByStatus(status: ReviewStatus): Flow<Int>

    /** 总复习统计 */
    @Query("SELECT SUM(totalReviews) FROM review_schedules")
    fun observeTotalReviews(): Flow<Int?>

    @Query("SELECT SUM(correctCount) FROM review_schedules")
    fun observeTotalCorrect(): Flow<Int?>

    @Query("DELETE FROM review_schedules WHERE cardId = :cardId")
    suspend fun deleteByCardId(cardId: Long)

    /** 通过 JOIN 直接获取到期的卡片（避免 N+1 查询） */
    @Query("""
        SELECT c.* FROM cards c
        INNER JOIN review_schedules r ON c.id = r.cardId
        WHERE r.nextReviewDate <= :todayMillis AND r.status != 'NEW'
        ORDER BY r.nextReviewDate ASC
    """)
    fun observeDueCardsJoin(todayMillis: Long): Flow<List<Card>>

    /** 通过 JOIN 获取新卡片 */
    @Query("""
        SELECT c.* FROM cards c
        INNER JOIN review_schedules r ON c.id = r.cardId
        WHERE r.status = 'NEW'
        ORDER BY r.cardId ASC
        LIMIT :limit
    """)
    fun observeNewCardsJoin(limit: Int = 10): Flow<List<Card>>

    /** 通过 JOIN 获取重学卡片 */
    @Query("""
        SELECT c.* FROM cards c
        INNER JOIN review_schedules r ON c.id = r.cardId
        WHERE r.status = 'RELEARNING'
        ORDER BY r.nextReviewDate ASC
    """)
    fun observeRelearningCardsJoin(): Flow<List<Card>>
}
