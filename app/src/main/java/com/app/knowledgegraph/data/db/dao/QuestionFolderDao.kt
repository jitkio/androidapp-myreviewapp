package com.app.knowledgegraph.data.db.dao

import androidx.room.*
import com.app.knowledgegraph.data.db.entity.FolderWithCount
import com.app.knowledgegraph.data.db.entity.ImportedQuestion
import com.app.knowledgegraph.data.db.entity.QuestionFolder
import com.app.knowledgegraph.data.db.entity.QuestionFolderItem
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionFolderDao {

    @Query("""
        SELECT f.id, f.name, f.createdAt, COUNT(i.questionId) AS questionCount
        FROM question_folders f
        LEFT JOIN question_folder_items i ON f.id = i.folderId
        GROUP BY f.id
        ORDER BY f.createdAt ASC
    """)
    fun observeAllWithCount(): Flow<List<FolderWithCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: QuestionFolder): Long

    @Update
    suspend fun update(folder: QuestionFolder)

    @Delete
    suspend fun delete(folder: QuestionFolder)

    @Query("SELECT * FROM question_folders WHERE id = :id")
    suspend fun getById(id: Long): QuestionFolder?

    @Query("SELECT COUNT(*) FROM question_folders")
    suspend fun getCount(): Int

    @Query("""
        SELECT q.* FROM imported_questions q
        INNER JOIN question_folder_items i ON q.id = i.questionId
        WHERE i.folderId = :folderId
        ORDER BY q.createdAt DESC
    """)
    fun observeItemsByFolder(folderId: Long): Flow<List<ImportedQuestion>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItems(items: List<QuestionFolderItem>)

    @Query("DELETE FROM question_folder_items WHERE folderId = :folderId AND questionId IN (:questionIds)")
    suspend fun removeItems(folderId: Long, questionIds: List<Long>)

    @Query("SELECT questionId FROM question_folder_items WHERE folderId = :folderId")
    suspend fun getQuestionIdsInFolder(folderId: Long): List<Long>

    @Query("""
        SELECT q.* FROM imported_questions q
        INNER JOIN question_folder_items i ON q.id = i.questionId
        WHERE i.folderId IN (:folderIds)
        ORDER BY RANDOM()
        LIMIT :limit
    """)
    suspend fun getQuestionsByFolderIds(folderIds: List<Long>, limit: Int): List<ImportedQuestion>
}
