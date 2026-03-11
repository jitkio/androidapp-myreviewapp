package com.app.knowledgegraph.domain.srs

import com.app.knowledgegraph.data.db.entity.ReviewStatus

/**
 * SM-2 改进版间隔重复算法
 *
 * 评分等级：
 * - 0 (Again)：完全不记得
 * - 2 (Hard)：想了很久才想起来
 * - 3 (Good)：稍作思考后回忆起来
 * - 5 (Easy)：立刻想起
 */
object SrsEngine {

    data class ReviewResult(
        val newEaseFactor: Float,
        val newInterval: Int,       // 天数
        val newRepetitions: Int,
        val newStatus: ReviewStatus
    )

    /**
     * 计算下次复习参数
     *
     * @param quality     评分 (0, 2, 3, 5)
     * @param easeFactor  当前难度因子
     * @param interval    当前间隔（天）
     * @param repetitions 连续正确次数
     */
    fun calculateNext(
        quality: Int,
        easeFactor: Float,
        interval: Int,
        repetitions: Int
    ): ReviewResult {

        // 评分 < 2 视为失败 → 重置
        if (quality < 2) {
            return ReviewResult(
                newEaseFactor = maxOf(1.3f, easeFactor - 0.2f),
                newInterval = 1,          // 重置为1天
                newRepetitions = 0,
                newStatus = ReviewStatus.RELEARNING
            )
        }

        // 计算新的 ease factor（SM-2 公式）
        val newEF = maxOf(
            1.3f,
            easeFactor + (0.1f - (5 - quality) * (0.08f + (5 - quality) * 0.02f))
        )

        // 计算新间隔
        val newInterval = when (repetitions) {
            0 -> 1                                    // 第一次: 1天
            1 -> 3                                    // 第二次: 3天（比标准SM-2更保守）
            else -> (interval * newEF).toInt()         // 后续: 间隔 × EF
        }

        // 确保间隔至少1天，最多180天
        val clampedInterval = newInterval.coerceIn(1, 180)

        return ReviewResult(
            newEaseFactor = newEF,
            newInterval = clampedInterval,
            newRepetitions = repetitions + 1,
            newStatus = if (repetitions == 0) ReviewStatus.LEARNING else ReviewStatus.REVIEW
        )
    }

    /**
     * 将 quality 值映射为用户可读标签
     */
    fun qualityLabel(quality: Int): String = when (quality) {
        0 -> "Again"
        2 -> "Hard"
        3 -> "Good"
        5 -> "Easy"
        else -> "Unknown"
    }

    /**
     * 预览下次复习间隔（供 UI 显示）
     */
    fun previewIntervals(
        easeFactor: Float,
        interval: Int,
        repetitions: Int
    ): Map<Int, Int> {
        return listOf(0, 2, 3, 5).associateWith { q ->
            calculateNext(q, easeFactor, interval, repetitions).newInterval
        }
    }
}
