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
import com.app.knowledgegraph.data.repository.ScanRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AddQuestionsViewModel(
    private val questionBankRepository: QuestionBankRepository,
    private val scanRepository: ScanRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val folderId: Long = savedStateHandle.get<Long>("folderId") ?: 0L

    private val _allQuestions = MutableStateFlow<List<ImportedQuestion>>(emptyList())

    private val _existingIds = MutableStateFlow<Set<Long>>(emptySet())
    val existingIds: StateFlow<Set<Long>> = _existingIds.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    val searchQuery = MutableStateFlow("")

    private val _selectedSource = MutableStateFlow<String?>(null)
    val selectedSource: StateFlow<String?> = _selectedSource.asStateFlow()

    private val _sources = MutableStateFlow<List<String>>(emptyList())
    val sources: StateFlow<List<String>> = _sources.asStateFlow()

    @OptIn(FlowPreview::class)
    val filteredQuestions: StateFlow<List<ImportedQuestion>> = combine(
        _allQuestions,
        searchQuery.debounce(300),
        _selectedSource
    ) { questions, query, source ->
        var list = questions
        if (source != null) {
            list = list.filter { it.source == source }
        }
        if (query.isNotBlank()) {
            list = list.filter {
                it.stem.contains(query, ignoreCase = true) ||
                        it.answer.contains(query, ignoreCase = true)
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            scanRepository.observeAll().collect { list ->
                _allQuestions.value = list
            }
        }
        viewModelScope.launch {
            scanRepository.observeAllSources().collect { list ->
                _sources.value = list
            }
        }
        viewModelScope.launch {
            val ids = questionBankRepository.getQuestionIdsInFolder(folderId)
            _existingIds.value = ids.toSet()
        }
    }

    fun setSelectedSource(source: String?) {
        _selectedSource.value = source
    }

    fun toggleQuestion(id: Long) {
        if (id in _existingIds.value) return
        val current = _selectedIds.value.toMutableSet()
        if (id in current) current.remove(id) else current.add(id)
        _selectedIds.value = current
    }

    fun selectAll() {
        val existing = _existingIds.value
        val filtered = filteredQuestions.value
        val newIds = filtered.map { it.id }.filter { it !in existing }.toSet()
        _selectedIds.value = _selectedIds.value + newIds
    }

    fun deselectAll() {
        _selectedIds.value = emptySet()
    }

    fun confirm(onDone: () -> Unit) {
        val ids = _selectedIds.value.toList()
        if (ids.isEmpty()) {
            onDone()
            return
        }
        viewModelScope.launch {
            questionBankRepository.addQuestionsToFolder(folderId, ids)
            onDone()
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val savedStateHandle = extras.createSavedStateHandle()
                return AddQuestionsViewModel(
                    container.questionBankRepository,
                    container.scanRepository,
                    savedStateHandle
                ) as T
            }
        }
    }
}
