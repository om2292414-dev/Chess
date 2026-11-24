package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.audio.ChessSound
import com.example.chess.audio.ChessSoundEngine
import com.example.chess.data.ChessRepository
import com.example.chess.data.PuzzleRepository
import com.example.chess.engine.ChessEngine
import com.example.chess.model.BoardState
import com.example.chess.model.ChessPuzzle
import com.example.chess.model.Move
import com.example.chess.model.PieceColor
import com.example.chess.model.PuzzleRushVariant
import com.example.chess.model.Square
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PuzzleTabMode {
    RATED,
    RUSH
}

enum class PuzzleStatus {
    SOLVING,
    WRONG_MOVE,
    SOLVED
}

data class RushResultItem(
    val puzzle: ChessPuzzle,
    val solved: Boolean
)

data class PuzzleUiState(
    val tabMode: PuzzleTabMode = PuzzleTabMode.RATED,
    val currentPuzzleIndex: Int = 0,
    val currentPuzzle: ChessPuzzle = PuzzleRepository.byDifficulty().first(),
    val boardState: BoardState = ChessEngine.fromFen(PuzzleRepository.byDifficulty().first().fen)!!,
    val solutionStep: Int = 0,
    val status: PuzzleStatus = PuzzleStatus.SOLVING,
    val boardFlipped: Boolean = false,
    val hintArrow: Pair<Square, Square>? = null,
    val showHintText: Boolean = false,
    val feedbackMessage: String = "Find the winning move!",
    val currentStreak: Int = 0,
    val ratingDelta: Int? = null,
    val attemptsOnCurrent: Int = 0,
    // Puzzle Rush state
    val rushVariant: PuzzleRushVariant = PuzzleRushVariant.THREE_MIN,
    val rushActive: Boolean = false,
    val rushFinished: Boolean = false,
    val rushScore: Int = 0,
    val rushStrikes: Int = 0,
    val rushSecondsLeft: Int = 180,
    val rushHistory: List<RushResultItem> = emptyList()
)

class PuzzleViewModel(
    private val repository: ChessRepository,
    private val soundEngine: ChessSoundEngine
) : ViewModel() {

    private val puzzles = PuzzleRepository.byDifficulty()
    private val _uiState = MutableStateFlow(PuzzleUiState())
    val uiState: StateFlow<PuzzleUiState> = _uiState.asStateFlow()

    private var rushTimerJob: Job? = null
    private var rushQueue: List<ChessPuzzle> = puzzles
    private var rushQueuePos: Int = 0

    init {
        loadRatedPuzzle(0)
    }

    fun selectTabMode(mode: PuzzleTabMode) {
        if (mode == PuzzleTabMode.RATED && _uiState.value.rushActive) {
            stopRush(saveScore = false)
        }
        _uiState.update { it.copy(tabMode = mode) }
        if (mode == PuzzleTabMode.RATED) {
            loadRatedPuzzle(_uiState.value.currentPuzzleIndex)
        }
    }

    fun openDailyPuzzle() {
        val daily = PuzzleRepository.dailyPuzzle()
        val idx = puzzles.indexOfFirst { it.id == daily.id }.coerceAtLeast(0)
        stopRush(saveScore = false)
        _uiState.update { it.copy(tabMode = PuzzleTabMode.RATED) }
        loadRatedPuzzle(idx)
    }

    fun selectRushVariant(variant: PuzzleRushVariant) {
        _uiState.update { it.copy(rushVariant = variant) }
    }

    fun startPuzzleRush(variant: PuzzleRushVariant = _uiState.value.rushVariant) {
        rushTimerJob?.cancel()
        rushQueue = puzzles // Progressive difficulty just like Chess.com Puzzle Rush!
        rushQueuePos = 0
        val firstPuzzle = rushQueue.first()
        val state = ChessEngine.fromFen(firstPuzzle.fen) ?: ChessEngine.initialState()

        _uiState.update {
            it.copy(
                tabMode = PuzzleTabMode.RUSH,
                rushVariant = variant,
                rushActive = true,
                rushFinished = false,
                rushScore = 0,
                rushStrikes = 0,
                rushSecondsLeft = variant.durationSeconds,
                rushHistory = emptyList(),
                currentPuzzle = firstPuzzle,
                boardState = state,
                solutionStep = 0,
                status = PuzzleStatus.SOLVING,
                boardFlipped = state.turn == PieceColor.BLACK,
                hintArrow = null,
                showHintText = false,
                feedbackMessage = "Puzzle Rush! ${if (state.turn == PieceColor.WHITE) "White" else "Black"} to move."
            )
        }

        if (variant.durationSeconds > 0) {
            rushTimerJob = viewModelScope.launch {
                while (true) {
                    delay(1000L)
                    val cur = _uiState.value
                    if (!cur.rushActive) break
                    val nextSec = (cur.rushSecondsLeft - 1).coerceAtLeast(0)
                    _uiState.update { it.copy(rushSecondsLeft = nextSec) }
                    if (nextSec == 0) {
                        stopRush(saveScore = true)
                        break
                    }
                }
            }
        }
    }

    fun stopRush(saveScore: Boolean = true) {
        rushTimerJob?.cancel()
        val snap = _uiState.value
        if (saveScore && snap.rushActive) {
            viewModelScope.launch {
                repository.recordPuzzleRushScore(snap.rushVariant, snap.rushScore)
            }
        }
        _uiState.update {
            it.copy(
                rushActive = false,
                rushFinished = saveScore && (snap.rushActive || snap.rushFinished)
            )
        }
    }

    fun loadRatedPuzzle(index: Int) {
        val safeIdx = index.mod(puzzles.size)
        val puzzle = puzzles[safeIdx]
        val state = ChessEngine.fromFen(puzzle.fen) ?: ChessEngine.initialState()
        val toMove = if (state.turn == PieceColor.WHITE) "White" else "Black"
        _uiState.update {
            it.copy(
                currentPuzzleIndex = safeIdx,
                currentPuzzle = puzzle,
                boardState = state,
                solutionStep = 0,
                status = PuzzleStatus.SOLVING,
                boardFlipped = state.turn == PieceColor.BLACK,
                hintArrow = null,
                showHintText = false,
                ratingDelta = null,
                attemptsOnCurrent = 0,
                feedbackMessage = "$toMove to move — ${puzzle.theme}"
            )
        }
    }

    fun nextRatedPuzzle() {
        loadRatedPuzzle(_uiState.value.currentPuzzleIndex + 1)
    }

    fun retryCurrentPuzzle() {
        val puzzle = _uiState.value.currentPuzzle
        val state = ChessEngine.fromFen(puzzle.fen) ?: ChessEngine.initialState()
        _uiState.update {
            it.copy(
                boardState = state,
                solutionStep = 0,
                status = PuzzleStatus.SOLVING,
                hintArrow = null,
                feedbackMessage = "Try again — ${puzzle.theme}"
            )
        }
    }

    fun requestHint() {
        val snap = _uiState.value
        if (snap.status == PuzzleStatus.SOLVED || snap.rushActive) return
        val expectedUci = snap.currentPuzzle.solutionUci.getOrNull(snap.solutionStep) ?: return
        val from = Square.fromAlgebraic(expectedUci.substring(0, 2))
        val to = Square.fromAlgebraic(expectedUci.substring(2, 4))
        _uiState.update {
            it.copy(
                hintArrow = if (from != null && to != null) from to to else null,
                showHintText = true,
                feedbackMessage = snap.currentPuzzle.hintText
            )
        }
    }

    fun onPuzzleMove(move: Move) {
        val snap = _uiState.value
        if (snap.status == PuzzleStatus.SOLVED) return
        if (snap.tabMode == PuzzleTabMode.RUSH && !snap.rushActive) return

        val expectedUci = snap.currentPuzzle.solutionUci.getOrNull(snap.solutionStep) ?: return
        val playedUci = move.uci()

        // Also accept any immediate checkmate move as valid!
        val candidateState = ChessEngine.makeMove(snap.boardState, move) ?: return
        val isMate = candidateState.status.name.startsWith("CHECKMATE")
        val isCorrect = playedUci == expectedUci || (isMate && snap.solutionStep == snap.currentPuzzle.solutionUci.lastIndex)

        if (isCorrect) {
            val nextStep = snap.solutionStep + 1
            if (nextStep >= snap.currentPuzzle.solutionUci.size) {
                // Puzzle Solved!
                soundEngine.play(ChessSound.PUZZLE_CORRECT)
                if (snap.tabMode == PuzzleTabMode.RUSH && snap.rushActive) {
                    val newScore = snap.rushScore + 1
                    val updatedHist = snap.rushHistory + RushResultItem(snap.currentPuzzle, true)
                    _uiState.update {
                        it.copy(
                            boardState = candidateState,
                            solutionStep = nextStep,
                            status = PuzzleStatus.SOLVED,
                            rushScore = newScore,
                            rushHistory = updatedHist,
                            feedbackMessage = "Correct! +1"
                        )
                    }
                    advanceRushQueueAfterDelay()
                } else {
                    val newStreak = snap.currentStreak + 1
                    val delta = if (snap.attemptsOnCurrent == 0 && !snap.showHintText) +15 else +6
                    _uiState.update {
                        it.copy(
                            boardState = candidateState,
                            solutionStep = nextStep,
                            status = PuzzleStatus.SOLVED,
                            hintArrow = null,
                            currentStreak = newStreak,
                            ratingDelta = delta,
                            feedbackMessage = "Solved! Excellent tactical vision (+$delta rating)."
                        )
                    }
                    viewModelScope.launch {
                        repository.markPuzzleSolved(snap.currentPuzzle.id, snap.attemptsOnCurrent + 1)
                        val curSettings = repository.settings.first()
                        repository.updatePuzzleRating(curSettings.puzzleRating + delta, newStreak)
                    }
                }
            } else {
                // Intermediate correct move -> auto-play opponent reply
                soundEngine.play(ChessSound.MOVE)
                _uiState.update {
                    it.copy(
                        boardState = candidateState,
                        solutionStep = nextStep,
                        hintArrow = null,
                        feedbackMessage = "Great move! Keep going..."
                    )
                }
                viewModelScope.launch {
                    delay(340L)
                    val cur = _uiState.value
                    val replyUci = cur.currentPuzzle.solutionUci.getOrNull(nextStep) ?: return@launch
                    val afterReply = ChessEngine.makeMoveByUci(cur.boardState, replyUci) ?: return@launch
                    soundEngine.play(ChessSound.MOVE)
                    _uiState.update {
                        it.copy(
                            boardState = afterReply,
                            solutionStep = nextStep + 1,
                            feedbackMessage = "Your turn — finish the tactic!"
                        )
                    }
                }
            }
        } else {
            // Wrong move!
            soundEngine.play(ChessSound.PUZZLE_WRONG)
            if (snap.tabMode == PuzzleTabMode.RUSH && snap.rushActive) {
                val newStrikes = snap.rushStrikes + 1
                val updatedHist = snap.rushHistory + RushResultItem(snap.currentPuzzle, false)
                _uiState.update {
                    it.copy(
                        rushStrikes = newStrikes,
                        rushHistory = updatedHist,
                        status = PuzzleStatus.WRONG_MOVE,
                        feedbackMessage = "Strike $newStrikes / 3!"
                    )
                }
                if (newStrikes >= 3) {
                    stopRush(saveScore = true)
                } else {
                    advanceRushQueueAfterDelay()
                }
            } else {
                val delta = -10
                _uiState.update {
                    it.copy(
                        status = PuzzleStatus.WRONG_MOVE,
                        currentStreak = 0,
                        attemptsOnCurrent = it.attemptsOnCurrent + 1,
                        ratingDelta = delta,
                        feedbackMessage = "Not quite! Try another move or tap Hint."
                    )
                }
                viewModelScope.launch {
                    val curSettings = repository.settings.first()
                    repository.updatePuzzleRating(curSettings.puzzleRating + delta, 0)
                }
            }
        }
    }

    private fun advanceRushQueueAfterDelay() {
        viewModelScope.launch {
            delay(480L)
            val snap = _uiState.value
            if (!snap.rushActive) return@launch
            rushQueuePos++
            if (rushQueuePos >= rushQueue.size) {
                // Looped all puzzles cleanly — shuffle for endless Survival/Rush!
                rushQueue = puzzles.shuffled()
                rushQueuePos = 0
            }
            val nextPuzzle = rushQueue[rushQueuePos]
            val nextState = ChessEngine.fromFen(nextPuzzle.fen) ?: ChessEngine.initialState()
            _uiState.update {
                it.copy(
                    currentPuzzle = nextPuzzle,
                    boardState = nextState,
                    solutionStep = 0,
                    status = PuzzleStatus.SOLVING,
                    boardFlipped = nextState.turn == PieceColor.BLACK,
                    hintArrow = null,
                    showHintText = false,
                    feedbackMessage = "${if (nextState.turn == PieceColor.WHITE) "White" else "Black"} to move (${nextPuzzle.rating})"
                )
            }
        }
    }

    override fun onCleared() {
        rushTimerJob?.cancel()
        super.onCleared()
    }
}
