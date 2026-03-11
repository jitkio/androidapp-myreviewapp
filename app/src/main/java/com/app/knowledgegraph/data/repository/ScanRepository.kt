package com.app.knowledgegraph.data.repository

import android.graphics.Bitmap
import com.app.knowledgegraph.data.db.dao.ImportedQuestionDao
import com.app.knowledgegraph.data.db.entity.ImportedQuestion
import com.app.knowledgegraph.data.db.entity.QuestionType
import com.app.knowledgegraph.data.network.DeepSeekApi
import kotlinx.coroutines.flow.Flow

class ScanRepository(
    private val dao: ImportedQuestionDao,
    private val deepSeekApi: DeepSeekApi
) {

    suspend fun extractFromImage(
        apiKey: String,
        bitmap: Bitmap,
        sourceLabel: String,
        baseUrl: String,
        model: String,
        onProgress: (String) -> Unit = {}
    ): Result<List<ImportedQuestion>> {
        return deepSeekApi.extractQuestions(apiKey, bitmap, sourceLabel, baseUrl, model, onProgress)
    }

    suspend fun saveQuestions(questions: List<ImportedQuestion>) {
        dao.insertAll(questions)
    }

    suspend fun recordAttempt(question: ImportedQuestion, isCorrect: Boolean) {
        dao.update(
            question.copy(
                attemptCount = question.attemptCount + 1,
                correctCount = if (isCorrect) question.correctCount + 1 else question.correctCount
            )
        )
    }

    suspend fun getQuizQuestions(limit: Int): List<ImportedQuestion> {
        return dao.getRandomQuestions(limit)
    }

    suspend fun getWeakQuestions(limit: Int): List<ImportedQuestion> {
        return dao.getWeakQuestions(limit)
    }

    suspend fun getQuestionsBySource(source: String, limit: Int): List<ImportedQuestion> {
        return dao.getQuestionsBySource(source, limit)
    }

    fun observeAll(): Flow<List<ImportedQuestion>> = dao.observeAll()

    fun observeAllSources(): Flow<List<String>> = dao.observeAllSources()

    fun observeTotalCount(): Flow<Int> = dao.observeTotalCount()

    suspend fun deleteQuestion(question: ImportedQuestion) = dao.delete(question)

    suspend fun deleteBatch(batchId: Long) = dao.deleteByBatch(batchId)

    suspend fun getById(id: Long): ImportedQuestion? = dao.getById(id)

    fun observeById(id: Long): Flow<ImportedQuestion?> = dao.observeById(id)

    suspend fun updateQuestion(question: ImportedQuestion) = dao.update(question)

    suspend fun deleteByIds(ids: List<Long>) {
        ids.chunked(500).forEach { chunk ->
            dao.deleteByIds(chunk)
        }
    }

    suspend fun deleteBySource(source: String) = dao.deleteBySource(source)

    suspend fun getFilteredQuestions(
        sources: Set<String>,
        types: Set<QuestionType>,
        limit: Int
    ): List<ImportedQuestion> {
        // 在数据库层过滤，避免加载全表到内存
        return when {
            sources.isNotEmpty() && types.isNotEmpty() ->
                dao.getBySourcesAndTypesRandom(
                    sources.toList(),
                    types.map { it.name },
                    limit
                )
            sources.isNotEmpty() ->
                dao.getBySourcesRandom(sources.toList(), limit)
            types.isNotEmpty() ->
                dao.getByTypesRandom(types.map { it.name }, limit)
            else ->
                dao.getRandomQuestions(limit)
        }
    }
}
