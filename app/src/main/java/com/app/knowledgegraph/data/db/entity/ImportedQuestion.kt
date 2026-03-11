package com.app.knowledgegraph.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class QuestionType {
    SINGLE_CHOICE, MULTI_CHOICE, FILL_BLANK, TRUE_FALSE
}

@Entity(
    tableName = "imported_questions",
    indices = [Index("source"), Index("batchId")]
)
data class ImportedQuestion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: QuestionType,
    val stem: String,
    val optionsJson: String = "",
    val answer: String,
    val explanation: String = "",
    val source: String = "",
    val batchId: Long = System.currentTimeMillis(),
    val attemptCount: Int = 0,
    val correctCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "") val figureSvg: String = ""
)
