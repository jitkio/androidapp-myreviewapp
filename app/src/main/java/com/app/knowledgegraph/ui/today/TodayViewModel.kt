package com.app.knowledgegraph.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.knowledgegraph.AppContainer
import com.app.knowledgegraph.data.db.entity.*
import com.app.knowledgegraph.data.repository.CardRepository
import com.app.knowledgegraph.data.repository.ReviewRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SessionStats(
    val reviewed: Int = 0,
    val correct: Int = 0,
    val again: Int = 0
)

data class TodayUiState(
    val queue: List<Card> = emptyList(),
    val currentIndex: Int = 0,
    val isAnswerRevealed: Boolean = false,
    val sessionStats: SessionStats = SessionStats(),
    val isLoading: Boolean = true,
    val showErrorTagDialog: Boolean = false,
    val currentCardId: Long? = null,
    val showAddCards: Boolean = false,
    val allCards: List<Card> = emptyList(),
    val addSearchQuery: String = "",
    val addSelectedChapter: String? = null
) {
    val currentCard: Card? get() = queue.getOrNull(currentIndex)
    val isComplete: Boolean get() = queue.isNotEmpty() && currentIndex >= queue.size
    val remaining: Int get() = (queue.size - currentIndex).coerceAtLeast(0)

    /** 添加面板中过滤后的卡片（排除已在队列中的） */
    val filteredAddCards: List<Card> get() {
        val queueIds = queue.map { it.id }.toSet()
        return allCards.filter { card ->
            card.id !in queueIds &&
            (addSearchQuery.isBlank() || card.prompt.contains(addSearchQuery, true) ||
                card.tags.contains(addSearchQuery, true) || card.chapter.contains(addSearchQuery, true)) &&
            (addSelectedChapter == null || card.chapter == addSelectedChapter)
        }
    }

    val addChapters: List<String> get() = allCards.map { it.chapter }.distinct().sorted()
}

class TodayViewModel(
    private val cardRepository: CardRepository,
    private val reviewRepository: ReviewRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    init {
        loadReviewQueue()
    }

    private fun loadReviewQueue() {
        viewModelScope.launch {
            combine(
                reviewRepository.observeDueCardsJoin(),
                reviewRepository.observeNewCardsJoin(5),
                reviewRepository.observeRelearningCardsJoin()
            ) { due, new, relearning ->
                (relearning + due + new).distinctBy { it.id }
            }.collect { cards ->
                _uiState.update {
                    it.copy(
                        queue = cards,
                        isLoading = false,
                        currentIndex = if (it.queue.isEmpty()) 0 else it.currentIndex
                    )
                }
            }
        }
    }

    fun revealAnswer() {
        _uiState.update { it.copy(isAnswerRevealed = true) }
    }

    fun submitRating(quality: Int) {
        val state = _uiState.value
        val card = state.currentCard ?: return

        viewModelScope.launch {
            reviewRepository.submitReview(card.id, quality)

            val newStats = state.sessionStats.copy(
                reviewed = state.sessionStats.reviewed + 1,
                correct = if (quality >= 2) state.sessionStats.correct + 1 else state.sessionStats.correct,
                again = if (quality < 2) state.sessionStats.again + 1 else state.sessionStats.again
            )

            if (quality < 2) {
                _uiState.update {
                    it.copy(showErrorTagDialog = true, currentCardId = card.id, sessionStats = newStats)
                }
            } else {
                _uiState.update {
                    it.copy(currentIndex = state.currentIndex + 1, isAnswerRevealed = false, sessionStats = newStats)
                }
            }
        }
    }

    fun submitErrorTag(errorType: ErrorType, description: String = "") {
        val cardId = _uiState.value.currentCardId ?: return
        viewModelScope.launch {
            reviewRepository.addErrorTag(cardId, errorType, description)
            _uiState.update {
                it.copy(showErrorTagDialog = false, currentCardId = null,
                    currentIndex = it.currentIndex + 1, isAnswerRevealed = false)
            }
        }
    }

    fun dismissErrorTagDialog() {
        _uiState.update {
            it.copy(showErrorTagDialog = false, currentIndex = it.currentIndex + 1, isAnswerRevealed = false)
        }
    }

    fun skipCard() {
        _uiState.update {
            val current = it.currentCard
            val newQueue = if (current != null) {
                it.queue.toMutableList().apply { removeAt(it.currentIndex); add(current) }
            } else it.queue
            it.copy(queue = newQueue, isAnswerRevealed = false)
        }
    }

    /* ═══ 手动添加卡片 ═══ */

    fun openAddCards() {
        viewModelScope.launch {
            val all = cardRepository.observeAll().first()
            _uiState.update { it.copy(showAddCards = true, allCards = all, addSearchQuery = "", addSelectedChapter = null) }
        }
    }

    fun closeAddCards() {
        _uiState.update { it.copy(showAddCards = false) }
    }

    fun updateAddSearch(query: String) {
        _uiState.update { it.copy(addSearchQuery = query) }
    }

    fun selectAddChapter(chapter: String?) {
        _uiState.update { it.copy(addSelectedChapter = chapter) }
    }

    /** 把选中的卡片加入队列尾部 */
    fun addCardsToQueue(cards: List<Card>) {
        _uiState.update { state ->
            val existingIds = state.queue.map { it.id }.toSet()
            val newCards = cards.filter { it.id !in existingIds }
            state.copy(
                queue = state.queue + newCards,
                showAddCards = false,
                // 如果之前已完成，重置到新加的第一张
                currentIndex = if (state.isComplete) state.queue.size else state.currentIndex
            )
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TodayViewModel(container.cardRepository, container.reviewRepository) as T
            }
        }
    }
}
