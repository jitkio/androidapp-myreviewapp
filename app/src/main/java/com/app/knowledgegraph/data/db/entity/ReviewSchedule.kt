package com.app.knowledgegraph.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ReviewStatus {
    NEW,          // 新卡片，未学习
    LEARNING,     // 学习中（短间隔）
    REVIEW,       // 正常复习（长间隔）
    RELEARNING    // 遗忘后重新学习
}

@Entity(
    tableName = "review_schedules",
    foreignKeys = [
        ForeignKey(
            entity = Card::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("cardId"), Index("nextReviewDate"), Index("status")]
)
data class ReviewSchedule(
    @PrimaryKey
    val cardId: Long,

    /** SM-2 难度因子 (≥1.3) */
    val easeFactor: Float = 2.5f,

    /** 当前间隔（天） */
    val interval: Int = 0,

    /** 连续正确次数 */
    val repetitions: Int = 0,

    /** 下次复习日期（毫秒时间戳） */
    val nextReviewDate: Long = System.currentTimeMillis(),

    /** 上次复习日期 */
    val lastReviewDate: Long? = null,

    /** 总复习次数 */
    val totalReviews: Int = 0,

    /** 正确次数 */
    val correctCount: Int = 0,

    /** 复习状态 */
    val status: ReviewStatus = ReviewStatus.NEW
)
