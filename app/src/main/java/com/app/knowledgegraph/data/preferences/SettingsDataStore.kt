package com.app.knowledgegraph.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    private val API_KEY = stringPreferencesKey("deepseek_api_key")
    private val API_BASE_URL = stringPreferencesKey("api_base_url")
    private val API_MODEL = stringPreferencesKey("api_model")
    private val SELECTED_FOLDER_IDS = stringPreferencesKey("selected_folder_ids")

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

    suspend fun saveApiKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[API_KEY] = key
        }
    }

    suspend fun saveApiBaseUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[API_BASE_URL] = url
        }
    }

    suspend fun saveApiModel(model: String) {
        context.dataStore.edit { prefs ->
            prefs[API_MODEL] = model
        }
    }

    suspend fun saveSelectedFolderIds(ids: Set<Long>) {
        context.dataStore.edit { prefs ->
            prefs[SELECTED_FOLDER_IDS] = ids.joinToString(",")
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://api.siliconflow.cn/v1"
        const val DEFAULT_MODEL = "Qwen/Qwen2.5-VL-72B-Instruct"
    }
}
