package com.app.knowledgegraph.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.knowledgegraph.AppContainer
import com.app.knowledgegraph.data.db.entity.FolderWithCount
import com.app.knowledgegraph.data.preferences.SettingsDataStore
import com.app.knowledgegraph.data.repository.QuestionBankRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class QuestionBankViewModel(
    private val repository: QuestionBankRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _folders = MutableStateFlow<List<FolderWithCount>>(emptyList())
    val folders: StateFlow<List<FolderWithCount>> = _folders.asStateFlow()

    private val _selectedFolderIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedFolderIds: StateFlow<Set<Long>> = _selectedFolderIds.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeAllWithCount().collect { list ->
                _folders.value = list
            }
        }
        viewModelScope.launch {
            settingsDataStore.selectedFolderIdsFlow.collect { ids ->
                _selectedFolderIds.value = ids
            }
        }
    }

    fun toggleFolderSelection(id: Long) {
        val current = _selectedFolderIds.value.toMutableSet()
        if (id in current) current.remove(id) else current.add(id)
        _selectedFolderIds.value = current
        viewModelScope.launch {
            settingsDataStore.saveSelectedFolderIds(current)
        }
    }

    fun createFolder(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createFolder(name.trim())
        }
    }

    fun renameFolder(id: Long, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.renameFolder(id, name.trim())
        }
    }

    fun deleteFolder(id: Long) {
        viewModelScope.launch {
            repository.deleteFolderById(id)
            // Also remove from selection
            val current = _selectedFolderIds.value.toMutableSet()
            if (current.remove(id)) {
                _selectedFolderIds.value = current
                settingsDataStore.saveSelectedFolderIds(current)
            }
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return QuestionBankViewModel(
                    container.questionBankRepository,
                    container.settingsDataStore
                ) as T
            }
        }
    }
}
