package com.app.knowledgegraph.data.repository

import com.app.knowledgegraph.data.db.dao.CardDao
import com.app.knowledgegraph.data.db.dao.ReviewDao
import com.app.knowledgegraph.data.db.entity.Card
import com.app.knowledgegraph.data.db.entity.CardType
import com.app.knowledgegraph.data.db.entity.ReviewSchedule
import kotlinx.coroutines.flow.Flow

class CardRepository(
    private val cardDao: CardDao,
    private val reviewDao: ReviewDao
) {

    fun observeAll(): Flow<List<Card>> = cardDao.observeAll()

    fun observeByChapter(chapter: String): Flow<List<Card>> =
        cardDao.observeByChapter(chapter)

    fun observeByType(type: CardType): Flow<List<Card>> =
        cardDao.observeByType(type)

    fun observeByChapterAndType(chapter: String, type: CardType): Flow<List<Card>> =
        cardDao.observeByChapterAndType(chapter, type)

    fun observeAllChapters(): Flow<List<String>> = cardDao.observeAllChapters()

    fun search(keyword: String): Flow<List<Card>> = cardDao.search(keyword)

    fun observeCount(): Flow<Int> = cardDao.observeCount()

    suspend fun getCount(): Int = cardDao.getCount()

    suspend fun getById(id: Long): Card? = cardDao.getById(id)

    suspend fun getByIds(ids: List<Long>): List<Card> = cardDao.getByIds(ids)

    fun observeById(id: Long): Flow<Card?> = cardDao.observeById(id)

    /**
     * 创建卡片并自动初始化 ReviewSchedule
     */
    suspend fun createCard(card: Card): Long {
        val cardId = cardDao.insert(card)
        // 自动创建复习排程
        reviewDao.insert(ReviewSchedule(cardId = cardId))
        return cardId
    }

    suspend fun updateCard(card: Card) {
        cardDao.update(card.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteCard(card: Card) {
        cardDao.delete(card)
        // ReviewSchedule 和 Edge 由 CASCADE 自动删除
    }

    suspend fun insertCard(card: Card): Long {
        return cardDao.insert(card)
    }

    suspend fun insertCards(cards: List<Card>): List<Long> {
        return cardDao.insertAll(cards)
    }

    suspend fun deleteByIds(ids: List<Long>) {
        ids.chunked(500).forEach { chunk ->
            cardDao.deleteByIds(chunk)
        }
    }

    fun observeAllTags(): Flow<List<String>> = cardDao.observeAllTags()
}
