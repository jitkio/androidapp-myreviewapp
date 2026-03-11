package com.app.knowledgegraph.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class CardType {
    CONCEPT,    // 概念卡：定义、性质、适用条件
    METHOD,     // 方法卡：步骤、检查点、常见坑
    TEMPLATE,   // 模板卡：解题模板、公式速查
    BOUNDARY    // 边界卡：反例、易错点、适用边界
}

@Entity(
    tableName = "cards",
    indices = [
        Index("chapter"),
        Index("type"),
        Index("updatedAt"),
        Index("chapter", "type")
    ]
)
data class Card(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 卡片类型 */
    val type: CardType,

    /** 所属章节（如 "第4章-电路定理"） */
    val chapter: String,

    /** 标签（逗号分隔，如 "戴维南,等效电路,线性"） */
    val tags: String = "",

    /** Prompt（问题式标题） */
    val prompt: String,

    /** Hint（可选提示，折叠显示） */
    val hint: String = "",

    /** Answer（结构化回答，Markdown 格式） */
    val answer: String,

    /** 创建时间 */
    val createdAt: Long = System.currentTimeMillis(),

    /** 最后修改时间 */
    val updatedAt: Long = System.currentTimeMillis()
)
