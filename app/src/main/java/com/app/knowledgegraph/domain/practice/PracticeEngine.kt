package com.app.knowledgegraph.domain.practice

import com.app.knowledgegraph.data.db.entity.*
import com.app.knowledgegraph.data.repository.CardRepository

object PracticeEngine {

    suspend fun generateBoundaryCard(
        question: PracticeQuestion,
        userChoice: String,
        cardRepository: CardRepository
    ): Long {
        val card = Card(
            type = CardType.BOUNDARY,
            chapter = question.chapter,
            tags = "选法训练,易错,$userChoice,${question.correctMethod}",
            prompt = "为什么这道题不能用${userChoice}，而要用${question.correctMethod}？",
            hint = "题干：${question.stem.take(40)}...",
            answer = "【题干】\n${question.stem}\n\n" +
                "【你选了】${userChoice}\n" +
                "【正确】${question.correctMethod}\n\n" +
                "【为什么】\n${question.explanation}\n\n" +
                "【触发词】\n${question.triggerWords.entries.joinToString("\n") { "- ${it.key} -> ${it.value}" }}\n\n" +
                "【常见坑】\n${question.commonTraps}"
        )
        return cardRepository.createCard(card)
    }
}
