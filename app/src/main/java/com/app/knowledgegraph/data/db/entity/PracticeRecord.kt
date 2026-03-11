package com.app.knowledgegraph.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "practice_records")
data class PracticeRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 题干摘要 */
    val questionSummary: String,

    /** 正确方法 */
    val correctMethod: String,

    /** 用户选择的方法 */
    val userChoice: String,

    /** 是否正确 */
    val isCorrect: Boolean,

    /** 触发词命中情况 */
    val triggerWordsHit: String = "",

    /** 时间戳 */
    val timestamp: Long = System.currentTimeMillis()
)
