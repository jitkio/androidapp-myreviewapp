package com.app.knowledgegraph.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.knowledgegraph.AppContainer
import com.app.knowledgegraph.data.db.entity.PracticeRecord
import com.app.knowledgegraph.data.repository.CardRepository
import com.app.knowledgegraph.domain.practice.PracticeEngine
import com.app.knowledgegraph.domain.practice.PracticeQuestion
import com.app.knowledgegraph.domain.practice.QuestionBank
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PracticePhase { QUESTION, FEEDBACK, COMPLETE }

data class PracticeUiState(
    val phase: PracticePhase = PracticePhase.QUESTION,
    val questions: List<PracticeQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val userChoice: String? = null,
    val isCorrect: Boolean = false,
    val sessionCorrect: Int = 0,
    val sessionTotal: Int = 0,
    val boundaryCardCreated: Boolean = false
) {
    val currentQuestion: PracticeQuestion? get() = questions.getOrNull(currentIndex)
    val progress: Float get() = if (questions.isEmpty()) 0f else currentIndex.toFloat() / questions.size
}

class PracticeViewModel(
    private val cardRepository: CardRepository,
    private val practiceRecordDao: com.app.knowledgegraph.data.db.dao.PracticeRecordDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(PracticeUiState())
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    init { startSession() }

    fun startSession() {
        val questions = QuestionBank.fetchAllQuestions().shuffled()
        _uiState.value = PracticeUiState(
            phase = PracticePhase.QUESTION,
            questions = questions
        )
    }

    fun submitChoice(method: String) {
        val question = _uiState.value.currentQuestion ?: return
        val correct = method == question.correctMethod

        viewModelScope.launch {
            practiceRecordDao.insert(
                PracticeRecord(
                    questionSummary = question.stem.take(80),
                    correctMethod = question.correctMethod,
                    userChoice = method,
                    isCorrect = correct,
                    triggerWordsHit = question.triggerWords.keys.joinToString(",")
                )
            )

            var boundaryCreated = false
            if (!correct) {
                PracticeEngine.generateBoundaryCard(question, method, cardRepository)
                boundaryCreated = true
            }

            _uiState.update {
                it.copy(
                    phase = PracticePhase.FEEDBACK,
                    userChoice = method,
                    isCorrect = correct,
                    sessionCorrect = if (correct) it.sessionCorrect + 1 else it.sessionCorrect,
                    sessionTotal = it.sessionTotal + 1,
                    boundaryCardCreated = boundaryCreated
                )
            }
        }
    }

    fun nextQuestion() {
        _uiState.update {
            val nextIndex = it.currentIndex + 1
            if (nextIndex >= it.questions.size) {
                it.copy(phase = PracticePhase.COMPLETE)
            } else {
                it.copy(
                    phase = PracticePhase.QUESTION,
                    currentIndex = nextIndex,
                    userChoice = null,
                    boundaryCardCreated = false
                )
            }
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PracticeViewModel(container.cardRepository, container.practiceRecordDao) as T
            }
        }
    }
}
