package com.app.knowledgegraph.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "question_folders")
data class QuestionFolder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
