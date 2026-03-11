package com.app.knowledgegraph

import android.content.Context
import androidx.compose.runtime.Stable
import com.app.knowledgegraph.data.db.AppDatabase
import com.app.knowledgegraph.data.network.DeepSeekApi
import com.app.knowledgegraph.data.preferences.SettingsDataStore
import com.app.knowledgegraph.data.repository.CardRepository
import com.app.knowledgegraph.data.repository.GraphRepository
import com.app.knowledgegraph.data.repository.QuestionBankRepository
import com.app.knowledgegraph.data.repository.ReviewRepository
import com.app.knowledgegraph.data.repository.ScanRepository

/**
 * 手动依赖注入容器
 * （AGP 9.0.1 不兼容 Hilt，改用手动注入）
 */
@Stable
class AppContainer(context: Context) {

    private val database = AppDatabase.getInstance(context)

    // DAOs
    val cardDao = database.cardDao()
    val edgeDao = database.edgeDao()
    val reviewDao = database.reviewDao()
    val errorTagDao = database.errorTagDao()
    val practiceRecordDao = database.practiceRecordDao()
    val importedQuestionDao = database.importedQuestionDao()
    val questionFolderDao = database.questionFolderDao()

    // DataStore
    val settingsDataStore = SettingsDataStore(context)

    // Network
    private val deepSeekApi = DeepSeekApi()

    // Repositories
    val cardRepository = CardRepository(cardDao, reviewDao)
    val reviewRepository = ReviewRepository(reviewDao, cardDao, errorTagDao)
    val graphRepository = GraphRepository(cardDao, edgeDao)
    val scanRepository = ScanRepository(importedQuestionDao, deepSeekApi)
    val questionBankRepository = QuestionBankRepository(questionFolderDao)
}
