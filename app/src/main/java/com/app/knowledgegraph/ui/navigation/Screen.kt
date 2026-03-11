package com.app.knowledgegraph.ui.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Today : Screen("today", "Today", Icons.Default.DateRange)
    data object Library : Screen("library", "Library", Icons.Default.MenuBook)
    data object Practice : Screen("practice", "Practice", Icons.Default.FlashOn)
    data object Me : Screen("me", "Me", Icons.Default.Person)

    companion object {
        val bottomTabs = listOf(Today, Library, Practice, Me)
    }
}

object DetailRoutes {
    const val CARD_DETAIL = "card_detail/{cardId}"
    const val CARD_EDIT = "card_edit/{cardId}"
    const val CARD_CREATE = "card_create"
    const val ADD_EDGE = "add_edge/{cardId}"
    const val REVIEW_SETTINGS = "review_settings"
    const val METHOD_TRAINING = "method_training"
    const val SCAN_SCREEN = "scan_screen"
    const val IMPORTED_QUIZ = "imported_quiz?count={count}&sources={sources}&types={types}&folderIds={folderIds}"
    const val API_KEY_SETTINGS = "api_key_settings"
    const val SMART_SCAN = "smart_scan"
    const val QUESTION_DETAIL = "question_detail/{questionId}"
    const val BULK_DELETE_CARDS = "bulk_delete_cards"
    const val BULK_DELETE_QUESTIONS = "bulk_delete_questions"
    const val QUESTION_BANK = "question_bank"
    const val FOLDER_DETAIL = "folder_detail/{folderId}"
    const val ADD_QUESTIONS_TO_FOLDER = "add_questions/{folderId}"
    const val MAP_SCREEN = "map_screen"
    const val SUBJECT_MANAGE = "subject_manage"

    fun cardDetail(cardId: Long) = "card_detail/$cardId"
    fun cardEdit(cardId: Long) = "card_edit/$cardId"
    fun addEdge(cardId: Long) = "add_edge/$cardId"
    fun questionDetail(questionId: Long) = "question_detail/$questionId"
    fun importedQuiz(count: Int = 0, sources: String = "", types: String = "", folderIds: String = "") =
        "imported_quiz?count=$count&sources=${Uri.encode(sources)}&types=${Uri.encode(types)}&folderIds=${Uri.encode(folderIds)}"
    fun folderDetail(folderId: Long) = "folder_detail/$folderId"
    fun addQuestionsToFolder(folderId: Long) = "add_questions/$folderId"
}
