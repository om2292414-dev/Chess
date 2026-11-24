package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.audio.ChessSound
import com.example.chess.audio.ChessSoundEngine
import com.example.chess.data.ChessRepository
import com.example.chess.data.SavedGameEntity
import com.example.chess.engine.ChessAi
import com.example.chess.engine.ChessEngine
import com.example.chess.model.AnalyzedMove
import com.example.chess.model.BoardState
import com.example.chess.model.EngineEvaluation
import com.example.chess.model.GameAnalysisResult
import com.example.chess.model.Move
import com.example.chess.model.MoveClassification
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AnalysisUiState(
    val fullGameState: BoardState = ChessEngine.initialState(),
    val viewingPly: Int = 0,
    val whiteLabel: String = "White",
    val blackLabel: String = "Black",
    val loadedGameId: Long? = null,
    val boardFlipped: Boolean = false,
    val currentEval: EngineEvaluation = EngineEvaluation(0),
    val isEvaluating: Boolean = false,
    val showBestMoveArrow: Boolean = true,
    val isRunningReview: Boolean = false,
    val reviewProgress: Float = 0f,
    val analysisResult: GameAnalysisResult? = null,
    val statusBanner: String? = null
) {
    val displayedState: BoardState
        get() {
            if (viewingPly >= fullGameState.moveHistory.size) return fullGameState
            if (viewingPly <= 0) {
                return ChessEngine.fromFen(fullGameState.initialFen) ?: ChessEngine.initialState()
            }
            val targetFen = fullGameState.moveHistory[viewingPly - 1].fenAfter
            val parsed = ChessEngine.fromFen(targetFen) ?: fullGameState
            return parsed.copy(moveHistory = fullGameState.moveHistory.take(viewingPly))
        }

    val currentAnalyzedMove: AnalyzedMove?
        get() = if (viewingPly > 0) {
            analysisResult?.moves?.getOrNull(viewingPly - 1)
        } else null

    val classificationMap: Map<Int, MoveClassification>
        get() = analysisResult?.moves?.associate { it.plyIndex to it.classification } ?: emptyMap()
}

class AnalysisViewModel(
    private val repository: ChessRepository,
    private val soundEngine: ChessSoundEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    private var evalJob: Job? = null
    private var reviewJob: Job? = null

    init {
        evaluateDisplayedPosition()
    }

    fun loadFromBoardState(
        state: BoardState,
        whiteName: String,
        blackName: String,
        savedGameId: Long? = null,
        autoRunReview: Boolean = false
    ) {
        reviewJob?.cancel()
        evalJob?.cancel()
        _uiState.update {
            it.copy(
                fullGameState = state,
                viewingPly = state.moveHistory.size,
                whiteLabel = whiteName,
                blackLabel = blackName,
                loadedGameId = savedGameId,
                analysisResult = null,
                isRunningReview = false,
                reviewProgress = 0f,
                statusBanner = "Loaded ${state.moveHistory.size} plies."
            )
        }
        evaluateDisplayedPosition()
        if (autoRunReview && state.moveHistory.isNotEmpty()) {
            runFullGameReview()
        }
    }

    fun loadSavedGame(entity: SavedGameEntity, autoRunReview: Boolean = true) {
        val parsed = ChessEngine.fromPgn(entity.pgn)
            ?: ChessEngine.fromFen(entity.finalFen)
            ?: ChessEngine.initialState()
        loadFromBoardState(
            state = parsed,
            whiteName = entity.whiteName,
            blackName = entity.blackName,
            savedGameId = entity.id,
            autoRunReview = autoRunReview
        )
    }

    fun onPlayMoveOnBoard(move: Move) {
        val snap = _uiState.value
        // Truncate future moves if exploring a variation from an earlier ply
        val baseState = if (snap.viewingPly < snap.fullGameState.moveHistory.size) {
            var rebuilt = ChessEngine.fromFen(snap.fullGameState.initialFen) ?: ChessEngine.initialState()
            for (i in 0 until snap.viewingPly) {
                rebuilt = ChessEngine.makeMove(rebuilt, snap.fullGameState.moveHistory[i].move) ?: break
            }
            rebuilt
        } else {
            snap.fullGameState
        }

        val next = ChessEngine.makeMove(baseState, move) ?: return
        soundEngine.play(if (next.moveHistory.lastOrNull()?.capturedPiece != null) ChessSound.CAPTURE else ChessSound.MOVE)
        _uiState.update {
            it.copy(
                fullGameState = next,
                viewingPly = next.moveHistory.size,
                analysisResult = null,
                statusBanner = null
            )
        }
        evaluateDisplayedPosition()
    }

    fun selectViewingPly(ply: Int) {
        val max = _uiState.value.fullGameState.moveHistory.size
        val clamped = ply.coerceIn(0, max)
        _uiState.update { it.copy(viewingPly = clamped) }
        evaluateDisplayedPosition()
    }

    fun flipBoard() {
        _uiState.update { it.copy(boardFlipped = !it.boardFlipped) }
    }

    fun toggleBestMoveArrow() {
        _uiState.update { it.copy(showBestMoveArrow = !it.showBestMoveArrow) }
    }

    fun resetBoard() {
        reviewJob?.cancel()
        _uiState.update {
            it.copy(
                fullGameState = ChessEngine.initialState(),
                viewingPly = 0,
                whiteLabel = "White",
                blackLabel = "Black",
                loadedGameId = null,
                analysisResult = null,
                isRunningReview = false,
                reviewProgress = 0f,
                statusBanner = "Reset to starting position."
            )
        }
        evaluateDisplayedPosition()
    }

    fun importFen(fen: String): Boolean {
        val parsed = ChessEngine.fromFen(fen.trim()) ?: run {
            _uiState.update { it.copy(statusBanner = "Invalid FEN string.") }
            return false
        }
        reviewJob?.cancel()
        _uiState.update {
            it.copy(
                fullGameState = parsed,
                viewingPly = 0,
                whiteLabel = "White",
                blackLabel = "Black",
                loadedGameId = null,
                analysisResult = null,
                isRunningReview = false,
                statusBanner = "FEN position imported!"
            )
        }
        evaluateDisplayedPosition()
        return true
    }

    fun importPgn(pgn: String): Boolean {
        val parsed = ChessEngine.fromPgn(pgn) ?: run {
            _uiState.update { it.copy(statusBanner = "Could not parse PGN moves.") }
            return false
        }
        val white = Regex("""\[White\s+"([^"]+)"\]""").find(pgn)?.groupValues?.getOrNull(1) ?: "White"
        val black = Regex("""\[Black\s+"([^"]+)"\]""").find(pgn)?.groupValues?.getOrNull(1) ?: "Black"
        loadFromBoardState(parsed, white, black, savedGameId = null, autoRunReview = false)
        _uiState.update { it.copy(statusBanner = "PGN imported (${parsed.moveHistory.size} plies)!") }
        return true
    }

    fun exportCurrentFen(): String = ChessEngine.toFen(_uiState.value.displayedState)

    fun exportCurrentPgn(): String {
        val snap = _uiState.value
        return ChessEngine.toPgn(snap.fullGameState, snap.whiteLabel, snap.blackLabel)
    }

    fun runFullGameReview() {
        val snap = _uiState.value
        if (snap.fullGameState.moveHistory.isEmpty() || snap.isRunningReview) return
        reviewJob?.cancel()
        reviewJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRunningReview = true,
                    reviewProgress = 0f,
                    statusBanner = "Running engine game review..."
                )
            }
            val result = ChessAi.analyzeGame(
                initialFen = snap.fullGameState.initialFen,
                history = snap.fullGameState.moveHistory,
                onProgress = { p -> _uiState.update { it.copy(reviewProgress = p) } }
            )
            _uiState.update {
                it.copy(
                    isRunningReview = false,
                    reviewProgress = 1f,
                    analysisResult = result,
                    statusBanner = "Game Review complete! Tap any move to inspect."
                )
            }
            val gameId = snap.loadedGameId
            if (gameId != null) {
                repository.updateGameAccuracy(gameId, result.whiteAccuracy, result.blackAccuracy)
            }
        }
    }

    private fun evaluateDisplayedPosition() {
        evalJob?.cancel()
        evalJob = viewModelScope.launch {
            val state = _uiState.value.displayedState
            _uiState.update { it.copy(isEvaluating = true) }
            val eval = ChessAi.evaluatePosition(state, depth = 3, timeBudgetMs = 750L)
            _uiState.update { it.copy(currentEval = eval, isEvaluating = false) }
        }
    }

    override fun onCleared() {
        evalJob?.cancel()
        reviewJob?.cancel()
        super.onCleared()
    }
}
