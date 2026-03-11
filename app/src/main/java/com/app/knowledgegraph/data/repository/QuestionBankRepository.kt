package com.app.knowledgegraph.data.repository

import com.app.knowledgegraph.data.db.dao.QuestionFolderDao
import com.app.knowledgegraph.data.db.entity.FolderWithCount
import com.app.knowledgegraph.data.db.entity.ImportedQuestion
import com.app.knowledgegraph.data.db.entity.QuestionFolder
import com.app.knowledgegraph.data.db.entity.QuestionFolderItem
import kotlinx.coroutines.flow.Flow

class QuestionBankRepository(
    private val dao: QuestionFolderDao
) {

    fun observeAllWithCount(): Flow<List<FolderWithCount>> = dao.observeAllWithCount()

    suspend fun createFolder(name: String): Long {
        return dao.insert(QuestionFolder(name = name))
    }

    suspend fun renameFolder(id: Long, name: String) {
        val folder = dao.getById(id) ?: return
        dao.update(folder.copy(name = name))
    }

    suspend fun deleteFolder(folder: QuestionFolder) {
        dao.delete(folder)
    }

    suspend fun deleteFolderById(id: Long) {
        val folder = dao.getById(id) ?: return
        dao.delete(folder)
    }

    suspend fun getFolderCount(): Int = dao.getCount()

    fun observeItemsByFolder(folderId: Long): Flow<List<ImportedQuestion>> =
        dao.observeItemsByFolder(folderId)

    suspend fun addQuestionsToFolder(folderId: Long, questionIds: List<Long>) {
        val items = questionIds.map { QuestionFolderItem(folderId, it) }
        items.chunked(500).forEach { chunk ->
            dao.insertItems(chunk)
        }
    }

    suspend fun removeQuestionsFromFolder(folderId: Long, questionIds: List<Long>) {
        questionIds.chunked(500).forEach { chunk ->
            dao.removeItems(folderId, chunk)
        }
    }

    suspend fun getQuestionIdsInFolder(folderId: Long): List<Long> =
        dao.getQuestionIdsInFolder(folderId)

    suspend fun getTrainingQuestions(folderIds: List<Long>, limit: Int): List<ImportedQuestion> {
        return if (folderIds.isEmpty()) emptyList()
        else dao.getQuestionsByFolderIds(folderIds, limit)
    }
}
