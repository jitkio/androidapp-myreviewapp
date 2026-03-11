package com.app.knowledgegraph.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    private val API_KEY = stringPreferencesKey("deepseek_api_key")
    private val API_BASE_URL = stringPreferencesKey("api_base_url")
    private val API_MODEL = stringPreferencesKey("api_model")
    private val SELECTED_FOLDER_IDS = stringPreferencesKey("selected_folder_ids")
    private val SUBJECT_LIST = stringPreferencesKey("subject_list")
    private val LAST_SUBJECT = stringPreferencesKey("last_subject")
    private val HAS_SEEDED = booleanPreferencesKey("has_seeded")

    val apiKeyFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[API_KEY] ?: ""
    }

    val apiBaseUrlFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[API_BASE_URL] ?: DEFAULT_BASE_URL
    }

    val apiModelFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[API_MODEL] ?: DEFAULT_MODEL
    }

    val selectedFolderIdsFlow: Flow<Set<Long>> = context.dataStore.data.map { prefs ->
        val raw = prefs[SELECTED_FOLDER_IDS] ?: ""
        if (raw.isBlank()) emptySet()
        else raw.split(",").mapNotNull { it.toLongOrNull() }.toSet()
    }

    val subjectListFlow: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[SUBJECT_LIST] ?: ""
        if (raw.isBlank()) DEFAULT_SUBJECTS
        else raw.split(SEPARATOR).filter { it.isNotBlank() }
    }

    val lastSubjectFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[LAST_SUBJECT] ?: ""
    }

    suspend fun hasSeeded(): Boolean {
        return context.dataStore.data.first()[HAS_SEEDED] ?: false
    }

    suspend fun markSeeded() {
        context.dataStore.edit { it[HAS_SEEDED] = true }
    }

    suspend fun saveApiKey(key: String) {
        context.dataStore.edit { it[API_KEY] = key }
    }

    suspend fun saveApiBaseUrl(url: String) {
        context.dataStore.edit { it[API_BASE_URL] = url }
    }

    suspend fun saveApiModel(model: String) {
        context.dataStore.edit { it[API_MODEL] = model }
    }

    suspend fun saveSelectedFolderIds(ids: Set<Long>) {
        context.dataStore.edit { it[SELECTED_FOLDER_IDS] = ids.joinToString(",") }
    }

    suspend fun saveSubjectList(subjects: List<String>) {
        context.dataStore.edit { it[SUBJECT_LIST] = subjects.joinToString(SEPARATOR) }
    }

    suspend fun saveLastSubject(subject: String) {
        context.dataStore.edit { it[LAST_SUBJECT] = subject }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://api.siliconflow.cn/v1"
        const val DEFAULT_MODEL = "Qwen/Qwen2.5-VL-72B-Instruct"
        private const val SEPARATOR = "||"
        val DEFAULT_SUBJECTS = listOf("英语", "数学", "政治", "专业课")
    }
}
