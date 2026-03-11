package com.app.knowledgegraph.data.db.converter

import androidx.room.TypeConverter
import com.app.knowledgegraph.data.db.entity.*

class Converters {

    // CardType
    @TypeConverter
    fun fromCardType(value: CardType): String = value.name

    @TypeConverter
    fun toCardType(value: String): CardType = CardType.valueOf(value)

    // RelationType
    @TypeConverter
    fun fromRelationType(value: RelationType): String = value.name

    @TypeConverter
    fun toRelationType(value: String): RelationType = RelationType.valueOf(value)

    // ReviewStatus
    @TypeConverter
    fun fromReviewStatus(value: ReviewStatus): String = value.name

    @TypeConverter
    fun toReviewStatus(value: String): ReviewStatus = ReviewStatus.valueOf(value)

    // ErrorType
    @TypeConverter
    fun fromErrorType(value: ErrorType): String = value.name

    @TypeConverter
    fun toErrorType(value: String): ErrorType = ErrorType.valueOf(value)

    // QuestionType
    @TypeConverter
    fun fromQuestionType(value: QuestionType): String = value.name

    @TypeConverter
    fun toQuestionType(value: String): QuestionType = QuestionType.valueOf(value)
}
