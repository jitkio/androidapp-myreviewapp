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
    val queue: List<Card> = emptyList(),       // 待复习队列
    val currentIndex: Int = 0,
    val isAnswerRevealed: Boolean = false,
    val sessionStats: SessionStats = SessionStats(),
    val isLoading: Boolean = true,
    val showErrorTagDialog: Boolean = false,
    val currentCardId: Long? = null            // 当前卡片ID（用于记录错因）
) {
    val currentCard: Card? get() = queue.getOrNull(currentIndex)
    val isComplete: Boolean get() = queue.isNotEmpty() && currentIndex >= queue.size
    val remaining: Int get() = (queue.size - currentIndex).coerceAtLeast(0)
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
            // 使用 JOIN 查询直接获取 Card，避免 N+1
            combine(
                reviewRepository.observeDueCardsJoin(),
                reviewRepository.observeNewCardsJoin(5),
                reviewRepository.observeRelearningCardsJoin()
            ) { due, new, relearning ->
                // 合并并去重
                (relearning + due + new).distinctBy { it.id }
            }.collect { cards ->
                _uiState.update {
                    it.copy(
                        queue = cards,
                        isLoading = false,
                        // 如果之前已经在复习中，保持进度
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
                // 评分 Again → 弹出错因选择
                _uiState.update {
                    it.copy(
                        showErrorTagDialog = true,
                        currentCardId = card.id,
                        sessionStats = newStats
                    )
                }
            } else {
                // 直接进入下一张
                _uiState.update {
                    it.copy(
                        currentIndex = state.currentIndex + 1,
                        isAnswerRevealed = false,
                        sessionStats = newStats
                    )
                }
            }
        }
    }

    fun submitErrorTag(errorType: ErrorType, description: String = "") {
        val cardId = _uiState.value.currentCardId ?: return
        viewModelScope.launch {
            reviewRepository.addErrorTag(cardId, errorType, description)
            _uiState.update {
                it.copy(
                    showErrorTagDialog = false,
                    currentCardId = null,
                    currentIndex = it.currentIndex + 1,
                    isAnswerRevealed = false
                )
            }
        }
    }

    fun dismissErrorTagDialog() {
        _uiState.update {
            it.copy(
                showErrorTagDialog = false,
                currentIndex = it.currentIndex + 1,
                isAnswerRevealed = false
            )
        }
    }

    fun skipCard() {
        _uiState.update {
            // 把当前卡片移到队列末尾
            val current = it.currentCard
            val newQueue = if (current != null) {
                it.queue.toMutableList().apply {
                    removeAt(it.currentIndex)
                    add(current)
                }
            } else it.queue

            it.copy(queue = newQueue, isAnswerRevealed = false)
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TodayViewModel(
                    container.cardRepository,
                    container.reviewRepository
                ) as T
            }
        }
    }
}
