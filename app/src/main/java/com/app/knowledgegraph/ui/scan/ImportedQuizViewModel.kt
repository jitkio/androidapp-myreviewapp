package com.app.knowledgegraph.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.knowledgegraph.AppContainer
import com.app.knowledgegraph.data.db.entity.ImportedQuestion
import com.app.knowledgegraph.data.db.entity.QuestionType
import com.app.knowledgegraph.data.repository.QuestionBankRepository
import com.app.knowledgegraph.data.repository.ScanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class QuizPhase { SETUP, ANSWERING, FEEDBACK, COMPLETE }
enum class QuizMode { RANDOM, WEAK_FIRST, BY_SOURCE }

data class ImportedQuizUiState(
    val phase: QuizPhase = QuizPhase.SETUP,
    val questions: List<ImportedQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val userAnswer: String = "",
    val userMultiAnswer: Set<String> = emptySet(),
    val isSubmitted: Boolean = false,
    val isCorrect: Boolean = false,
    val sessionCorrect: Int = 0,
    val sessionTotal: Int = 0,
    val availableSources: List<String> = emptyList(),
    val selectedSource: String = "",
    val quizMode: QuizMode = QuizMode.RANDOM,
    val questionCount: Int = 10
) {
    val currentQuestion: ImportedQuestion? get() = questions.getOrNull(currentIndex)
    val progress: Float get() = if (questions.isEmpty()) 0f else currentIndex.toFloat() / questions.size
}

class ImportedQuizViewModel(
    private val scanRepository: ScanRepository,
    private val questionBankRepository: QuestionBankRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportedQuizUiState())
    val uiState: StateFlow<ImportedQuizUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            scanRepository.observeAllSources().collect { sources ->
                _uiState.update { it.copy(availableSources = sources) }
            }
        }
    }

    fun setQuizMode(mode: QuizMode) {
        _uiState.update { it.copy(quizMode = mode) }
    }

    fun setSelectedSource(source: String) {
        _uiState.update { it.copy(selectedSource = source) }
    }

    fun setQuestionCount(count: Int) {
        _uiState.update { it.copy(questionCount = count) }
    }

    fun startQuiz() {
        viewModelScope.launch {
            val state = _uiState.value
            val questions = when (state.quizMode) {
                QuizMode.RANDOM -> scanRepository.getQuizQuestions(state.questionCount)
                QuizMode.WEAK_FIRST -> scanRepository.getWeakQuestions(state.questionCount)
                QuizMode.BY_SOURCE -> scanRepository.getQuestionsBySource(state.selectedSource, state.questionCount)
            }

            if (questions.isEmpty()) return@launch

            _uiState.update {
                it.copy(
                    phase = QuizPhase.ANSWERING,
                    questions = questions,
                    currentIndex = 0,
                    userAnswer = "",
                    userMultiAnswer = emptySet(),
                    isSubmitted = false,
                    sessionCorrect = 0,
                    sessionTotal = 0
                )
            }
        }
    }

    fun startWithPreset(count: Int, sourcesStr: String, typesStr: String) {
        viewModelScope.launch {
            val sources = if (sourcesStr.isBlank()) emptySet()
                          else sourcesStr.split(",").toSet()
            val types = if (typesStr.isBlank()) emptySet()
                        else typesStr.split(",").mapNotNull {
                            try { QuestionType.valueOf(it) } catch (_: Exception) { null }
                        }.toSet()

            val questions = scanRepository.getFilteredQuestions(sources, types, count)
            if (questions.isEmpty()) return@launch

            _uiState.update {
                it.copy(
                    phase = QuizPhase.ANSWERING,
                    questions = questions,
                    currentIndex = 0,
                    userAnswer = "",
                    userMultiAnswer = emptySet(),
                    isSubmitted = false,
                    sessionCorrect = 0,
                    sessionTotal = 0,
                    questionCount = count
                )
            }
        }
    }

    fun startWithFolders(folderIds: Set<Long>, limit: Int) {
        viewModelScope.launch {
            val questions = questionBankRepository.getTrainingQuestions(folderIds.toList(), limit)
            if (questions.isEmpty()) return@launch

            _uiState.update {
                it.copy(
                    phase = QuizPhase.ANSWERING,
                    questions = questions,
                    currentIndex = 0,
                    userAnswer = "",
                    userMultiAnswer = emptySet(),
                    isSubmitted = false,
                    sessionCorrect = 0,
                    sessionTotal = 0,
                    questionCount = limit
                )
            }
        }
    }

    fun updateAnswer(answer: String) {
        _uiState.update { it.copy(userAnswer = answer) }
    }

    fun toggleMultiAnswer(option: String) {
        _uiState.update { state ->
            val newSet = state.userMultiAnswer.toMutableSet()
            if (newSet.contains(option)) newSet.remove(option) else newSet.add(option)
            state.copy(userMultiAnswer = newSet)
        }
    }

    fun submitAnswer() {
        val state = _uiState.value
        val question = state.currentQuestion ?: return

        val correct = checkAnswer(question, state.userAnswer, state.userMultiAnswer)

        viewModelScope.launch {
            scanRepository.recordAttempt(question, correct)
        }

        _uiState.update {
            it.copy(
                phase = QuizPhase.FEEDBACK,
                isSubmitted = true,
                isCorrect = correct,
                sessionCorrect = if (correct) it.sessionCorrect + 1 else it.sessionCorrect,
                sessionTotal = it.sessionTotal + 1
            )
        }
    }

    fun nextQuestion() {
        _uiState.update { state ->
            val nextIndex = state.currentIndex + 1
            if (nextIndex >= state.questions.size) {
                state.copy(phase = QuizPhase.COMPLETE)
            } else {
                state.copy(
                    phase = QuizPhase.ANSWERING,
                    currentIndex = nextIndex,
                    userAnswer = "",
                    userMultiAnswer = emptySet(),
                    isSubmitted = false
                )
            }
        }
    }

    fun restartQuiz() {
        _uiState.update {
            ImportedQuizUiState(
                availableSources = it.availableSources,
                quizMode = it.quizMode,
                selectedSource = it.selectedSource,
                questionCount = it.questionCount
            )
        }
    }

    private fun checkAnswer(
        question: ImportedQuestion,
        userAnswer: String,
        userMultiAnswer: Set<String>
    ): Boolean {
        return when (question.type) {
            QuestionType.SINGLE_CHOICE -> {
                userAnswer.trim().equals(question.answer.trim(), ignoreCase = true)
            }
            QuestionType.MULTI_CHOICE -> {
                val correctSet = question.answer.split(",").map { it.trim().uppercase() }.toSet()
                val userSet = userMultiAnswer.map { it.trim().uppercase() }.toSet()
                correctSet == userSet
            }
            QuestionType.FILL_BLANK -> {
                userAnswer.trim().equals(question.answer.trim(), ignoreCase = true)
            }
            QuestionType.TRUE_FALSE -> {
                userAnswer.trim().equals(question.answer.trim(), ignoreCase = true)
            }
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ImportedQuizViewModel(
                    container.scanRepository,
                    container.questionBankRepository
                ) as T
            }
        }
    }
}
