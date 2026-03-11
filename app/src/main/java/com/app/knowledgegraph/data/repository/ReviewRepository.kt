package com.app.knowledgegraph.data.repository

import com.app.knowledgegraph.data.db.dao.CardDao
import com.app.knowledgegraph.data.db.dao.ErrorTagDao
import com.app.knowledgegraph.data.db.dao.ReviewDao
import com.app.knowledgegraph.data.db.entity.*
import com.app.knowledgegraph.domain.srs.SrsEngine
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class ReviewRepository(
    private val reviewDao: ReviewDao,
    private val cardDao: CardDao,
    private val errorTagDao: ErrorTagDao
) {

    /** 获取今日到期卡片对应的 Card */
    fun observeDueCards(): Flow<List<ReviewSchedule>> {
        val todayEnd = getTodayEndMillis()
        return reviewDao.observeDueCards(todayEnd)
    }

    /** 通过 JOIN 直接获取到期的 Card（避免 N+1 查询） */
    fun observeDueCardsJoin(): Flow<List<Card>> {
        val todayEnd = getTodayEndMillis()
        return reviewDao.observeDueCardsJoin(todayEnd)
    }

    /** 通过 JOIN 获取新卡片 */
    fun observeNewCardsJoin(limit: Int = 10): Flow<List<Card>> =
        reviewDao.observeNewCardsJoin(limit)

    /** 通过 JOIN 获取重学卡片 */
    fun observeRelearningCardsJoin(): Flow<List<Card>> =
        reviewDao.observeRelearningCardsJoin()

    /** 获取新卡片 */
    fun observeNewCards(limit: Int = 10): Flow<List<ReviewSchedule>> =
        reviewDao.observeNewCards(limit)

    /** 获取弱点卡片 */
    fun observeWeakCards(): Flow<List<ReviewSchedule>> =
        reviewDao.observeWeakCards()

    /** 获取重学卡片 */
    fun observeRelearningCards(): Flow<List<ReviewSchedule>> =
        reviewDao.observeRelearningCards()

    fun observeScheduleByCard(cardId: Long): Flow<ReviewSchedule?> =
        reviewDao.observeByCardId(cardId)

    /**
     * 处理复习评分
     * @param cardId 卡片ID
     * @param quality 评分：0=Again, 2=Hard, 3=Good, 5=Easy
     */
    suspend fun submitReview(cardId: Long, quality: Int) {
        val schedule = reviewDao.getByCardId(cardId) ?: return

        val result = SrsEngine.calculateNext(
            quality = quality,
            easeFactor = schedule.easeFactor,
            interval = schedule.interval,
            repetitions = schedule.repetitions
        )

        val now = System.currentTimeMillis()
        val nextDate = now + result.newInterval * 24L * 60 * 60 * 1000

        reviewDao.update(
            schedule.copy(
                easeFactor = result.newEaseFactor,
                interval = result.newInterval,
                repetitions = result.newRepetitions,
                nextReviewDate = nextDate,
                lastReviewDate = now,
                totalReviews = schedule.totalReviews + 1,
                correctCount = if (quality >= 2) schedule.correctCount + 1 else schedule.correctCount,
                status = result.newStatus
            )
        )
    }

    /** 记录错因 */
    suspend fun addErrorTag(cardId: Long, errorType: ErrorType, description: String = "") {
        errorTagDao.insert(
            ErrorTag(
                cardId = cardId,
                errorType = errorType,
                description = description
            )
        )
    }

    /** 获取今日结束时间戳 */
    private fun getTodayEndMillis(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }
}
