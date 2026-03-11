package com.app.knowledgegraph.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ErrorType {
    DIRECTION_ERROR,     // 方向错（电流/电压参考方向）
    PORT_ERROR,          // 端口错（开路/短路搞混）
    CONDITION_ERROR,     // 适用条件错（非线性用了叠加）
    FORMULA_ERROR,       // 公式记错
    SIGN_ERROR,          // 正负号错
    METHOD_WRONG,        // 方法选错
    STEP_MISSING,        // 步骤遗漏
    OTHER                // 其他
}

@Entity(
    tableName = "error_tags",
    foreignKeys = [
        ForeignKey(
            entity = Card::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("cardId")]
)
data class ErrorTag(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val cardId: Long,

    /** 错因类型 */
    val errorType: ErrorType,

    /** 具体描述 */
    val description: String = "",

    /** 发生时间 */
    val occurredAt: Long = System.currentTimeMillis()
)
