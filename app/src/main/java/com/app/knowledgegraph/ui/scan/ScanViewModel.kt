package com.app.knowledgegraph.ui.scan

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.knowledgegraph.AppContainer
import com.app.knowledgegraph.data.db.entity.ImportedQuestion
import com.app.knowledgegraph.data.preferences.SettingsDataStore
import com.app.knowledgegraph.data.repository.ScanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ScanPhase { IDLE, PREVIEW, SAVED }

data class ScanUiState(
    val phase: ScanPhase = ScanPhase.IDLE,
    val sourceLabel: String = "",
    val extractedQuestions: List<ImportedQuestion> = emptyList(),
    val selectedIndices: Set<Int> = emptySet(),
    val isProcessing: Boolean = false,
    val progressMessage: String = "",
    val error: String? = null,
    val savedCount: Int = 0,
    val apiKeyPresent: Boolean = false
)

class ScanViewModel(
    private val scanRepository: ScanRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsDataStore.apiKeyFlow.collect { key ->
                _uiState.update { it.copy(apiKeyPresent = key.isNotBlank()) }
            }
        }
    }

    fun updateSourceLabel(label: String) {
        _uiState.update { it.copy(sourceLabel = label) }
    }

    fun processImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null, progressMessage = "准备中...") }

            val apiKey = settingsDataStore.apiKeyFlow.first()
            if (apiKey.isBlank()) {
                _uiState.update { it.copy(isProcessing = false, error = "请先在「Me → API Key 设置」中配置 API Key") }
                return@launch
            }

            val baseUrl = settingsDataStore.apiBaseUrlFlow.first()
            val model = settingsDataStore.apiModelFlow.first()

            val result = withContext(Dispatchers.IO) {
                scanRepository.extractFromImage(
                    apiKey, bitmap, _uiState.value.sourceLabel, baseUrl, model
                ) { message ->
                    // MutableStateFlow is thread-safe, can update from IO thread
                    _uiState.update { it.copy(progressMessage = message) }
                }
            }

            result.fold(
                onSuccess = { questions ->
                    _uiState.update {
                        it.copy(
                            phase = ScanPhase.PREVIEW,
                            extractedQuestions = questions,
                            selectedIndices = questions.indices.toSet(),
                            isProcessing = false,
                            error = null
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isProcessing = false, error = e.message ?: "识别失败")
                    }
                }
            )
        }
    }

    fun toggleQuestion(index: Int) {
        _uiState.update { state ->
            val newSet = state.selectedIndices.toMutableSet()
            if (newSet.contains(index)) newSet.remove(index) else newSet.add(index)
            state.copy(selectedIndices = newSet)
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            if (state.selectedIndices.size == state.extractedQuestions.size) {
                state.copy(selectedIndices = emptySet())
            } else {
                state.copy(selectedIndices = state.extractedQuestions.indices.toSet())
            }
        }
    }

    fun saveSelected() {
        viewModelScope.launch {
            val state = _uiState.value
            val selected = state.selectedIndices.map { state.extractedQuestions[it] }
            if (selected.isEmpty()) return@launch

            scanRepository.saveQuestions(selected)
            _uiState.update {
                it.copy(
                    phase = ScanPhase.SAVED,
                    savedCount = selected.size
                )
            }
        }
    }

    fun setError(msg: String) {
        _uiState.update { it.copy(error = msg, isProcessing = false) }
    }

    fun resetToIdle() {
        _uiState.update {
            ScanUiState(
                sourceLabel = it.sourceLabel,
                apiKeyPresent = it.apiKeyPresent
            )
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ScanViewModel(container.scanRepository, container.settingsDataStore) as T
            }
        }
    }
}
