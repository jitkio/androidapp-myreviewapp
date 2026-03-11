package com.app.knowledgegraph.ui.smartscan

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.knowledgegraph.AppContainer
import com.app.knowledgegraph.data.db.entity.Card
import com.app.knowledgegraph.data.db.entity.CardType
import com.app.knowledgegraph.data.network.DeepSeekApi
import com.app.knowledgegraph.data.preferences.SettingsDataStore
import com.app.knowledgegraph.data.repository.CardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SmartScanPhase { IDLE, PROCESSING, PREVIEW, SAVED }

data class SmartScanUiState(
    val phase: SmartScanPhase = SmartScanPhase.IDLE,
    val subject: String = "",
    val generatedCards: List<Card> = emptyList(),
    val selectedIndices: Set<Int> = emptySet(),
    val isProcessing: Boolean = false,
    val progressMessage: String = "",
    val error: String? = null,
    val savedCount: Int = 0,
    val apiKeyPresent: Boolean = false
)

class SmartScanViewModel(
    private val cardRepository: CardRepository,
    private val deepSeekApi: DeepSeekApi,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(SmartScanUiState())
    val uiState: StateFlow<SmartScanUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsDataStore.apiKeyFlow.collect { key ->
                _uiState.update { it.copy(apiKeyPresent = key.isNotBlank()) }
            }
        }
    }

    fun setSubject(subject: String) {
        _uiState.update { it.copy(subject = subject) }
    }

    fun processImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.update { it.copy(phase = SmartScanPhase.PROCESSING, isProcessing = true, error = null) }

            val apiKey = settingsDataStore.apiKeyFlow.first()
            if (apiKey.isBlank()) {
                _uiState.update { it.copy(isProcessing = false, error = "\u8bf7\u5148\u914d\u7f6e API Key", phase = SmartScanPhase.IDLE) }
                return@launch
            }

            val baseUrl = settingsDataStore.apiBaseUrlFlow.first()
            val model = settingsDataStore.apiModelFlow.first()
            val subject = _uiState.value.subject

            val result = withContext(Dispatchers.IO) {
                deepSeekApi.smartScanToCards(apiKey, bitmap, subject, baseUrl, model) { msg ->
                    _uiState.update { it.copy(progressMessage = msg) }
                }
            }

            result.fold(
                onSuccess = { cardMaps ->
                    val cards = cardMaps.map { m ->
                        val cardType = try { CardType.valueOf(m["type"] ?: "CONCEPT") } catch (_: Exception) { CardType.CONCEPT }
                        Card(
                            type = cardType,
                            chapter = m["chapter"] ?: subject,
                            tags = m["tags"] ?: subject,
                            prompt = m["prompt"] ?: "",
                            hint = m["hint"] ?: "",
                            answer = m["answer"] ?: ""
                        )
                    }
                    _uiState.update {
                        it.copy(
                            phase = SmartScanPhase.PREVIEW,
                            generatedCards = cards,
                            selectedIndices = cards.indices.toSet(),
                            isProcessing = false
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isProcessing = false, error = e.message, phase = SmartScanPhase.IDLE) }
                }
            )
        }
    }

    fun toggleCard(index: Int) {
        _uiState.update { state ->
            val newSet = state.selectedIndices.toMutableSet()
            if (newSet.contains(index)) newSet.remove(index) else newSet.add(index)
            state.copy(selectedIndices = newSet)
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            if (state.selectedIndices.size == state.generatedCards.size)
                state.copy(selectedIndices = emptySet())
            else
                state.copy(selectedIndices = state.generatedCards.indices.toSet())
        }
    }

    fun saveSelected() {
        viewModelScope.launch {
            val state = _uiState.value
            val selected = state.selectedIndices.map { state.generatedCards[it] }
            if (selected.isEmpty()) return@launch
            cardRepository.insertCards(selected)
            _uiState.update { it.copy(phase = SmartScanPhase.SAVED, savedCount = selected.size) }
        }
    }

    fun resetToIdle() {
        _uiState.update { SmartScanUiState(subject = it.subject, apiKeyPresent = it.apiKeyPresent) }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SmartScanViewModel(container.cardRepository, DeepSeekApi(), container.settingsDataStore) as T
            }
        }
    }
}
