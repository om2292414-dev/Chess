package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.audio.ChessSound
import com.example.chess.audio.ChessSoundEngine
import com.example.chess.data.ChessRepository
import com.example.chess.data.SavedGameEntity
import com.example.chess.engine.ChessAi
import com.example.chess.engine.ChessEngine
import com.example.chess.model.BoardState
import com.example.chess.model.BotProfile
import com.example.chess.model.EngineEvaluation
import com.example.chess.model.GameMode
import com.example.chess.model.GameStatus
import com.example.chess.model.Move
import com.example.chess.model.PieceColor
import com.example.chess.model.SidePreference
import com.example.chess.model.TimeControl
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

data class GameUiState(
    val inActiveMatch: Boolean = false,
    val selectedMode: GameMode = GameMode.VS_BOT,
    val selectedBot: BotProfile = BotProfile.ROSTER[2], // Leo 800 ELO default
    val selectedSide: SidePreference = SidePreference.WHITE,
    val selectedTimeControl: TimeControl = TimeControl.PRESETS[6], // 10 min Rapid default
    val boardState: BoardState = ChessEngine.initialState(),
    val viewingPly: Int = 0,
    val playerColor: PieceColor = PieceColor.WHITE,
    val boardFlipped: Boolean = false,
    val whiteClockMs: Long? = null,
    val blackClockMs: Long? = null,
    val isBotThinking: Boolean = false,
    val liveEvaluation: EngineEvaluation = EngineEvaluation(0),
    val isEvaluating: Boolean = false,
    val hintMove: Move? = null,
    val coachFeedback: String? = null,
    val showGameOverDialog: Boolean = false,
    val lastSavedGameId: Long? = null
) {
    val displayedState: BoardState
        get() {
            if (viewingPly >= boardState.moveHistory.size) return boardState
            if (viewingPly <= 0) {
                return ChessEngine.fromFen(boardState.initialFen) ?: ChessEngine.initialState()
            }
            val targetFen = boardState.moveHistory[viewingPly - 1].fenAfter
            val parsed = ChessEngine.fromFen(targetFen) ?: boardState
            return parsed.copy(moveHistory = boardState.moveHistory.take(viewingPly))
        }

    val isViewingLatest: Boolean
        get() = viewingPly == boardState.moveHistory.size

    val canUserInteract: Boolean
        get() {
            if (!inActiveMatch || !isViewingLatest || boardState.status.isOver || isBotThinking) return false
            return if (selectedMode == GameMode.PASS_AND_PLAY) {
                true
            } else {
                boardState.turn == playerColor
            }
        }
}

class GameViewModel(
    private val repository: ChessRepository,
    private val soundEngine: ChessSoundEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var clockJob: Job? = null
    private var botJob: Job? = null
    private var evalJob: Job? = null
    private var lastEvalBeforeUserMoveCp: Int = 0
    private var hasSavedCurrentGame: Boolean = false

    fun selectMode(mode: GameMode) {
        _uiState.update { it.copy(selectedMode = mode) }
    }

    fun selectBot(bot: BotProfile) {
        _uiState.update { it.copy(selectedBot = bot) }
    }

    fun selectSide(side: SidePreference) {
        _uiState.update { it.copy(selectedSide = side) }
    }

    fun selectTimeControl(tc: TimeControl) {
        _uiState.update { it.copy(selectedTimeControl = tc) }
    }

    fun openNewGameLobby() {
        cancelBackgroundJobs()
        _uiState.update {
            it.copy(
                inActiveMatch = false,
                showGameOverDialog = false,
                isBotThinking = false,
                hintMove = null,
                coachFeedback = null
            )
        }
    }

    fun startQuickBotGame(bot: BotProfile = _uiState.value.selectedBot) {
        _uiState.update { it.copy(selectedMode = GameMode.VS_BOT, selectedBot = bot) }
        startGame()
    }

    fun startQuickPassAndPlay() {
        _uiState.update { it.copy(selectedMode = GameMode.PASS_AND_PLAY) }
        startGame()
    }

    fun startGame() {
        cancelBackgroundJobs()
        hasSavedCurrentGame = false
        lastEvalBeforeUserMoveCp = 0

        val current = _uiState.value
        val resolvedPlayerColor = when {
            current.selectedMode == GameMode.PASS_AND_PLAY -> PieceColor.WHITE
            current.selectedSide == SidePreference.WHITE -> PieceColor.WHITE
            current.selectedSide == SidePreference.BLACK -> PieceColor.BLACK
            else -> if (Random.nextBoolean()) PieceColor.WHITE else PieceColor.BLACK
        }
        val initialFlipped = current.selectedMode == GameMode.VS_BOT &&
            resolvedPlayerColor == PieceColor.BLACK
        val tc = current.selectedTimeControl
        val startClock = if (tc.isTimed) tc.initialMillis else null

        val freshState = ChessEngine.initialState()
        _uiState.update {
            it.copy(
                inActiveMatch = true,
                boardState = freshState,
                viewingPly = 0,
                playerColor = resolvedPlayerColor,
                boardFlipped = initialFlipped,
                whiteClockMs = startClock,
                blackClockMs = startClock,
                isBotThinking = false,
                liveEvaluation = EngineEvaluation(0),
                isEvaluating = false,
                hintMove = null,
                coachFeedback = if (current.selectedMode == GameMode.VS_BOT) {
                    "Playing ${current.selectedBot.name} (${current.selectedBot.elo} ELO). Good luck!"
                } else {
                    "Pass & Play — board stays fixed unless you tap Flip."
                },
                showGameOverDialog = false,
                lastSavedGameId = null
            )
        }

        if (tc.isTimed) {
            startClockTicker()
        }
        triggerLiveEval(freshState)

        // If playing Black vs Bot, let the bot make the opening move
        if (current.selectedMode == GameMode.VS_BOT && resolvedPlayerColor == PieceColor.BLACK) {
            triggerBotMove()
        }
    }

    fun onUserMove(move: Move) {
        val current = _uiState.value
        if (!current.canUserInteract) return
        val mover = current.boardState.turn
        lastEvalBeforeUserMoveCp = current.liveEvaluation.scoreCp

        val next = ChessEngine.makeMove(current.boardState, move) ?: return
        applyMoveTransition(next, mover, isFromBot = false)
    }

    private fun applyMoveTransition(next: BoardState, mover: PieceColor, isFromBot: Boolean) {
        val current = _uiState.value
        val tc = current.selectedTimeControl
        val newWhiteClock = if (tc.isTimed && mover == PieceColor.WHITE && current.whiteClockMs != null) {
            current.whiteClockMs + tc.incrementMillis
        } else current.whiteClockMs

        val newBlackClock = if (tc.isTimed && mover == PieceColor.BLACK && current.blackClockMs != null) {
            current.blackClockMs + tc.incrementMillis
        } else current.blackClockMs

        playSoundForMove(next)

        // IMPORTANT: Board orientation stays FIXED in Pass & Play (never auto-flips!)
        _uiState.update {
            it.copy(
                boardState = next,
                viewingPly = next.moveHistory.size,
                whiteClockMs = newWhiteClock,
                blackClockMs = newBlackClock,
                hintMove = null,
                isBotThinking = false
            )
        }

        if (next.status.isOver) {
            handleGameEnded(next)
            return
        }

        triggerLiveEval(next, provideCoachFeedback = !isFromBot && current.selectedMode == GameMode.VS_BOT, moverColor = mover)

        if (current.selectedMode == GameMode.VS_BOT && next.turn != current.playerColor) {
            triggerBotMove()
        }
    }

    private fun triggerBotMove() {
        botJob?.cancel()
        botJob = viewModelScope.launch {
            _uiState.update { it.copy(isBotThinking = true) }
            val snap = _uiState.value
            val startMs = System.currentTimeMillis()
            val chosen = ChessAi.selectBotMove(snap.boardState, snap.selectedBot)
            // Keep a natural short cadence so moves feel crisp and human
            val elapsed = System.currentTimeMillis() - startMs
            if (elapsed < 320L) delay(320L - elapsed)

            val latest = _uiState.value
            if (!latest.inActiveMatch || latest.boardState.status.isOver || chosen == null) {
                _uiState.update { it.copy(isBotThinking = false) }
                return@launch
            }
            val mover = latest.boardState.turn
            val next = ChessEngine.makeMove(latest.boardState, chosen)
            if (next != null) {
                applyMoveTransition(next, mover, isFromBot = true)
            } else {
                _uiState.update { it.copy(isBotThinking = false) }
            }
        }
    }

    private fun triggerLiveEval(
        state: BoardState,
        provideCoachFeedback: Boolean = false,
        moverColor: PieceColor? = null
    ) {
        evalJob?.cancel()
        evalJob = viewModelScope.launch {
            _uiState.update { it.copy(isEvaluating = true) }
            val eval = ChessAi.evaluatePosition(state, depth = 2, timeBudgetMs = 450L)
            val feedback = if (provideCoachFeedback && moverColor != null) {
                buildCoachTip(lastEvalBeforeUserMoveCp, eval.scoreCp, moverColor)
            } else null

            _uiState.update {
                it.copy(
                    liveEvaluation = eval,
                    isEvaluating = false,
                    coachFeedback = feedback ?: it.coachFeedback
                )
            }
        }
    }

    private fun buildCoachTip(beforeWhiteCp: Int, afterWhiteCp: Int, mover: PieceColor): String {
        val loss = if (mover == PieceColor.WHITE) {
            beforeWhiteCp - afterWhiteCp
        } else {
            afterWhiteCp - beforeWhiteCp
        }
        return when {
            loss <= 15 -> "Coach: Strong move! Keeps full control of the position."
            loss <= 55 -> "Coach: Solid choice. Stay alert for tactics."
            loss <= 130 -> "Coach: Slight inaccuracy — a sharper continuation was available."
            loss <= 250 -> "Coach: Mistake! That gave your opponent an opening."
            else -> "Coach: Blunder! Check for undefended pieces or tactical threats."
        }
    }

    fun requestHint() {
        val current = _uiState.value
        if (!current.canUserInteract) return
        viewModelScope.launch {
            _uiState.update { it.copy(coachFeedback = "Coach: Calculating best move...") }
            val eval = ChessAi.evaluatePosition(current.boardState, depth = 3, timeBudgetMs = 900L)
            val best = eval.bestMove
            if (best != null) {
                _uiState.update {
                    it.copy(
                        hintMove = best,
                        liveEvaluation = eval,
                        coachFeedback = "Coach Hint: Try moving ${best.from.algebraic} to ${best.to.algebraic}."
                    )
                }
            }
        }
    }

    fun takebackMove() {
        val current = _uiState.value
        if (!current.inActiveMatch || current.boardState.moveHistory.isEmpty()) return
        botJob?.cancel()
        evalJob?.cancel()

        val pliesToPop = if (current.selectedMode == GameMode.VS_BOT && !current.isBotThinking &&
            current.boardState.turn == current.playerColor && current.boardState.moveHistory.size >= 2
        ) {
            2
        } else {
            1
        }

        val targetPlies = (current.boardState.moveHistory.size - pliesToPop).coerceAtLeast(0)
        var rebuilt = ChessEngine.fromFen(current.boardState.initialFen) ?: ChessEngine.initialState()
        for (i in 0 until targetPlies) {
            rebuilt = ChessEngine.makeMove(rebuilt, current.boardState.moveHistory[i].move) ?: break
        }

        hasSavedCurrentGame = false
        _uiState.update {
            it.copy(
                boardState = rebuilt,
                viewingPly = rebuilt.moveHistory.size,
                isBotThinking = false,
                hintMove = null,
                showGameOverDialog = false,
                coachFeedback = "Move taken back."
            )
        }
        triggerLiveEval(rebuilt)
        if (current.selectedMode == GameMode.VS_BOT && rebuilt.turn != current.playerColor) {
            triggerBotMove()
        }
    }

    fun flipBoard() {
        _uiState.update { it.copy(boardFlipped = !it.boardFlipped) }
    }

    fun selectViewingPly(ply: Int) {
        val max = _uiState.value.boardState.moveHistory.size
        _uiState.update { it.copy(viewingPly = ply.coerceIn(0, max)) }
    }

    fun resignGame() {
        val current = _uiState.value
        if (!current.inActiveMatch || current.boardState.status.isOver) return
        val resigningColor = if (current.selectedMode == GameMode.VS_BOT) {
            current.playerColor
        } else {
            current.boardState.turn
        }
        val status = if (resigningColor == PieceColor.WHITE) {
            GameStatus.RESIGNED_WHITE
        } else {
            GameStatus.RESIGNED_BLACK
        }
        val ended = current.boardState.copy(status = status)
        _uiState.update { it.copy(boardState = ended, isBotThinking = false) }
        handleGameEnded(ended)
    }

    fun agreeDraw() {
        val current = _uiState.value
        if (!current.inActiveMatch || current.boardState.status.isOver) return
        val ended = current.boardState.copy(status = GameStatus.DRAW_AGREEMENT)
        _uiState.update { it.copy(boardState = ended, isBotThinking = false) }
        handleGameEnded(ended)
    }

    fun dismissGameOverDialog() {
        _uiState.update { it.copy(showGameOverDialog = false) }
    }

    private fun startClockTicker() {
        clockJob?.cancel()
        clockJob = viewModelScope.launch {
            var lastTick = System.currentTimeMillis()
            while (true) {
                delay(100L)
                val now = System.currentTimeMillis()
                val dt = (now - lastTick).coerceAtLeast(0L)
                lastTick = now

                val snap = _uiState.value
                if (!snap.inActiveMatch || snap.boardState.status.isOver) break
                // Only start ticking after the first move is made (standard chess behavior)
                if (snap.boardState.moveHistory.isEmpty()) continue

                if (snap.boardState.turn == PieceColor.WHITE && snap.whiteClockMs != null) {
                    val remaining = (snap.whiteClockMs - dt).coerceAtLeast(0L)
                    if (remaining == 0L) {
                        val ended = snap.boardState.copy(status = GameStatus.TIMEOUT_BLACK_WINS)
                        _uiState.update { it.copy(whiteClockMs = 0L, boardState = ended, isBotThinking = false) }
                        handleGameEnded(ended)
                        break
                    } else {
                        _uiState.update { it.copy(whiteClockMs = remaining) }
                    }
                } else if (snap.boardState.turn == PieceColor.BLACK && snap.blackClockMs != null) {
                    val remaining = (snap.blackClockMs - dt).coerceAtLeast(0L)
                    if (remaining == 0L) {
                        val ended = snap.boardState.copy(status = GameStatus.TIMEOUT_WHITE_WINS)
                        _uiState.update { it.copy(blackClockMs = 0L, boardState = ended, isBotThinking = false) }
                        handleGameEnded(ended)
                        break
                    } else {
                        _uiState.update { it.copy(blackClockMs = remaining) }
                    }
                }
            }
        }
    }

    private fun handleGameEnded(endedState: BoardState) {
        clockJob?.cancel()
        botJob?.cancel()
        soundEngine.play(ChessSound.GAME_OVER)
        _uiState.update { it.copy(showGameOverDialog = true, isBotThinking = false) }
        saveCompletedGame(endedState)
    }

    private fun saveCompletedGame(endedState: BoardState) {
        if (hasSavedCurrentGame) return
        hasSavedCurrentGame = true
        val snap = _uiState.value
        val whiteName = when {
            snap.selectedMode == GameMode.PASS_AND_PLAY -> "Player 1 (White)"
            snap.playerColor == PieceColor.WHITE -> "You"
            else -> "${snap.selectedBot.name} (${snap.selectedBot.elo})"
        }
        val blackName = when {
            snap.selectedMode == GameMode.PASS_AND_PLAY -> "Player 2 (Black)"
            snap.playerColor == PieceColor.BLACK -> "You"
            else -> "${snap.selectedBot.name} (${snap.selectedBot.elo})"
        }
        val pgnResult = endedState.status.pgnResult()
        val userOutcome = if (snap.selectedMode == GameMode.PASS_AND_PLAY) {
            "LOCAL"
        } else {
            when (pgnResult) {
                "1-0" -> if (snap.playerColor == PieceColor.WHITE) "WIN" else "LOSS"
                "0-1" -> if (snap.playerColor == PieceColor.BLACK) "WIN" else "LOSS"
                else -> "DRAW"
            }
        }
        val pgn = ChessEngine.toPgn(
            state = endedState,
            whiteName = whiteName,
            blackName = blackName,
            event = if (snap.selectedMode == GameMode.VS_BOT) "Play vs Bot" else "Pass & Play"
        )

        val entity = SavedGameEntity(
            mode = snap.selectedMode.name,
            whiteName = whiteName,
            blackName = blackName,
            botId = if (snap.selectedMode == GameMode.VS_BOT) snap.selectedBot.id else null,
            botElo = if (snap.selectedMode == GameMode.VS_BOT) snap.selectedBot.elo else null,
            userColor = if (snap.selectedMode == GameMode.VS_BOT) snap.playerColor.name else null,
            result = pgnResult,
            userOutcome = userOutcome,
            termination = endedState.status.terminationReason(),
            timeControlLabel = snap.selectedTimeControl.label,
            pgn = pgn,
            finalFen = ChessEngine.toFen(endedState),
            moveCount = endedState.moveHistory.size
        )

        viewModelScope.launch {
            val savedId = repository.saveGame(entity)
            _uiState.update { it.copy(lastSavedGameId = savedId) }
        }
    }

    private fun playSoundForMove(next: BoardState) {
        val last = next.moveHistory.lastOrNull() ?: return
        val sound = when {
            next.status.isOver -> ChessSound.GAME_OVER
            last.isCheck -> ChessSound.CHECK
            last.move.promotion != null -> ChessSound.PROMOTION
            last.move.isCastleKingside || last.move.isCastleQueenside -> ChessSound.CASTLE
            last.capturedPiece != null -> ChessSound.CAPTURE
            else -> ChessSound.MOVE
        }
        soundEngine.play(sound)
    }

    private fun cancelBackgroundJobs() {
        clockJob?.cancel()
        botJob?.cancel()
        evalJob?.cancel()
    }

    override fun onCleared() {
        cancelBackgroundJobs()
        super.onCleared()
    }
}
