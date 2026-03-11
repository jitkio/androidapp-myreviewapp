package com.app.knowledgegraph.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index

@Entity(
    tableName = "question_folder_items",
    primaryKeys = ["folderId", "questionId"],
    foreignKeys = [
        ForeignKey(
            entity = QuestionFolder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = CASCADE
        ),
        ForeignKey(
            entity = ImportedQuestion::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = CASCADE
        )
    ],
    indices = [Index("questionId")]
)
data class QuestionFolderItem(
    val folderId: Long,
    val questionId: Long
)
