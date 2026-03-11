package com.app.knowledgegraph.ui.practice

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.app.knowledgegraph.AppContainer
import com.app.knowledgegraph.data.db.entity.ImportedQuestion
import com.app.knowledgegraph.data.repository.QuestionBankRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FolderDetailViewModel(
    private val repository: QuestionBankRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val folderId: Long = savedStateHandle.get<Long>("folderId") ?: 0L

    private val _questions = MutableStateFlow<List<ImportedQuestion>>(emptyList())
    val questions: StateFlow<List<ImportedQuestion>> = _questions.asStateFlow()

    private val _folderName = MutableStateFlow("")
    val folderName: StateFlow<String> = _folderName.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeItemsByFolder(folderId).collect { list ->
                _questions.value = list
            }
        }
    }

    fun removeQuestions(ids: List<Long>) {
        viewModelScope.launch {
            repository.removeQuestionsFromFolder(folderId, ids)
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val savedStateHandle = extras.createSavedStateHandle()
                return FolderDetailViewModel(
                    container.questionBankRepository,
                    savedStateHandle
                ) as T
            }
        }
    }
}
