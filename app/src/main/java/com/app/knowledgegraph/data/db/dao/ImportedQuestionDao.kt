package com.app.knowledgegraph.data.db.dao

import androidx.room.*
import com.app.knowledgegraph.data.db.entity.ImportedQuestion
import com.app.knowledgegraph.data.db.entity.QuestionType
import kotlinx.coroutines.flow.Flow

@Dao
interface ImportedQuestionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<ImportedQuestion>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(question: ImportedQuestion): Long

    @Update
    suspend fun update(question: ImportedQuestion)

    @Delete
    suspend fun delete(question: ImportedQuestion)

    @Query("DELETE FROM imported_questions WHERE batchId = :batchId")
    suspend fun deleteByBatch(batchId: Long)

    @Query("SELECT * FROM imported_questions ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ImportedQuestion>>

    @Query("SELECT * FROM imported_questions WHERE source = :source ORDER BY createdAt DESC")
    fun observeBySource(source: String): Flow<List<ImportedQuestion>>

    @Query("SELECT * FROM imported_questions WHERE type = :type ORDER BY createdAt DESC")
    fun observeByType(type: QuestionType): Flow<List<ImportedQuestion>>

    @Query("SELECT DISTINCT source FROM imported_questions WHERE source != '' ORDER BY source")
    fun observeAllSources(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM imported_questions")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT * FROM imported_questions ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomQuestions(limit: Int): List<ImportedQuestion>

    @Query("SELECT * FROM imported_questions WHERE attemptCount > 0 AND (correctCount * 1.0 / attemptCount) < 0.6 ORDER BY RANDOM() LIMIT :limit")
    suspend fun getWeakQuestions(limit: Int): List<ImportedQuestion>

    @Query("SELECT * FROM imported_questions WHERE source = :source ORDER BY RANDOM() LIMIT :limit")
    suspend fun getQuestionsBySource(source: String, limit: Int): List<ImportedQuestion>

    @Query("SELECT * FROM imported_questions WHERE source IN (:sources) ORDER BY RANDOM() LIMIT :limit")
    suspend fun getBySourcesRandom(sources: List<String>, limit: Int): List<ImportedQuestion>

    @Query("SELECT * FROM imported_questions WHERE type IN (:types) ORDER BY RANDOM() LIMIT :limit")
    suspend fun getByTypesRandom(types: List<String>, limit: Int): List<ImportedQuestion>

    @Query("SELECT * FROM imported_questions WHERE source IN (:sources) AND type IN (:types) ORDER BY RANDOM() LIMIT :limit")
    suspend fun getBySourcesAndTypesRandom(sources: List<String>, types: List<String>, limit: Int): List<ImportedQuestion>

    @Query("SELECT * FROM imported_questions")
    suspend fun getAll(): List<ImportedQuestion>

    @Query("SELECT * FROM imported_questions WHERE id = :id")
    suspend fun getById(id: Long): ImportedQuestion?

    @Query("SELECT * FROM imported_questions WHERE id = :id")
    fun observeById(id: Long): Flow<ImportedQuestion?>

    @Query("DELETE FROM imported_questions WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM imported_questions WHERE source = :source")
    suspend fun deleteBySource(source: String)
}
