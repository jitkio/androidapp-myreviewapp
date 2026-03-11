package com.app.knowledgegraph.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.knowledgegraph.AppContainer
import com.app.knowledgegraph.data.db.entity.Card
import com.app.knowledgegraph.data.db.entity.CardType
import com.app.knowledgegraph.data.repository.CardRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LibraryUiState(
    val cards: List<Card> = emptyList(),
    val chapters: List<String> = emptyList(),
    val searchQuery: String = "",
    val selectedChapter: String? = null,         // null = 全部
    val selectedType: CardType? = null,           // null = 全部
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class LibraryViewModel(
    private val cardRepository: CardRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedChapter = MutableStateFlow<String?>(null)
    private val _selectedType = MutableStateFlow<CardType?>(null)

    val uiState: StateFlow<LibraryUiState> = combine(
        _searchQuery.debounce(300),
        _selectedChapter,
        _selectedType,
        cardRepository.observeAllChapters()
    ) { query, chapter, type, chapters ->
        Triple(Triple(query, chapter, type), chapters, Unit)
    }.flatMapLatest { (filters, chapters, _) ->
        val (query, chapter, type) = filters

        val cardsFlow = when {
            query.isNotBlank() -> cardRepository.search(query)
            chapter != null && type != null -> cardRepository.observeByChapterAndType(chapter, type)
            chapter != null -> cardRepository.observeByChapter(chapter)
            type != null -> cardRepository.observeByType(type)
            else -> cardRepository.observeAll()
        }

        cardsFlow.map { cards ->
            LibraryUiState(
                cards = cards,
                chapters = chapters,
                searchQuery = query,
                selectedChapter = chapter,
                selectedType = type,
                isLoading = false
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        LibraryUiState()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onChapterSelected(chapter: String?) {
        _selectedChapter.value = chapter
    }

    fun onTypeSelected(type: CardType?) {
        _selectedType.value = type
    }

    fun deleteCard(card: Card) {
        viewModelScope.launch {
            cardRepository.deleteCard(card)
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LibraryViewModel(container.cardRepository) as T
            }
        }
    }
}
