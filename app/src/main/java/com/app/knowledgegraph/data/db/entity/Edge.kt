package com.app.knowledgegraph.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class RelationType {
    REQUIRES,      // A 是 B 的前置知识
    EQUIVALENT,    // A 和 B 等价/可互换
    FAILS_WHEN,    // A 在某条件下失效，需要 B
    WORKFLOW,      // A 之后通常接 B（解题流程）
    CONTRADICTS,   // A 和 B 互斥（不能同时用）
    EXTENDS        // B 是 A 的扩展/推广
}

@Entity(
    tableName = "edges",
    foreignKeys = [
        ForeignKey(
            entity = Card::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Card::class,
            parentColumns = ["id"],
            childColumns = ["targetId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sourceId"), Index("targetId")]
)
data class Edge(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 起点卡片 ID */
    val sourceId: Long,

    /** 终点卡片 ID */
    val targetId: Long,

    /** 关系类型 */
    val relation: RelationType,

    /** 关系说明 */
    val description: String = ""
)
