package com.example.chess.engine

import com.example.chess.model.AnalyzedMove
import com.example.chess.model.BoardState
import com.example.chess.model.BotProfile
import com.example.chess.model.EngineEvaluation
import com.example.chess.model.GameAnalysisResult
import com.example.chess.model.GameStatus
import com.example.chess.model.Move
import com.example.chess.model.MoveClassification
import com.example.chess.model.MoveRecord
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.abs
import kotlin.math.exp
import kotlin.random.Random

/**
 * Pure Kotlin Chess AI using Negamax + Alpha-Beta pruning, Quiescence Search,
 * MVV-LVA move ordering, and Piece-Square Tables (PSTs).
 * Runs off the main thread on Dispatchers.Default.
 */
object ChessAi {

    private const val MATE_SCORE = 100_000
    private const val INF = 1_000_000

    // Piece-Square Tables oriented from White's perspective (index 0 = a1, index 63 = h8).
    // Note: Rows below are written top-down (Rank 8 down to Rank 1), so we flip rank when indexing!
    private val PAWN_PST = intArrayOf(
        0,  0,  0,  0,  0,  0,  0,  0,
        50, 50, 50, 50, 50, 50, 50, 50,
        10, 10, 20, 30, 30, 20, 10, 10,
        5,  5, 10, 27, 27, 10,  5,  5,
        0,  0,  0, 24, 24,  0,  0,  0,
        5, -5,-10,  0,  0,-10, -5,  5,
        5, 10, 10,-22,-22, 10, 10,  5,
        0,  0,  0,  0,  0,  0,  0,  0
    )

    private val KNIGHT_PST = intArrayOf(
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  0,  0,  0,-20,-40,
        -30,  0, 12, 16, 16, 12,  0,-30,
        -30,  5, 16, 22, 22, 16,  5,-30,
        -30,  0, 16, 22, 22, 16,  0,-30,
        -30,  5, 12, 16, 16, 12,  5,-30,
        -40,-20,  0,  5,  5,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50
    )

    private val BISHOP_PST = intArrayOf(
        -20,-10,-10,-10,-10,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  6, 12, 12,  6,  0,-10,
        -10,  6,  6, 12, 12,  6,  6,-10,
        -10,  0, 12, 12, 12, 12,  0,-10,
        -10, 10, 10, 10, 10, 10, 10,-10,
        -10,  6,  0,  0,  0,  0,  6,-10,
        -20,-10,-10,-10,-10,-10,-10,-20
    )

    private val ROOK_PST = intArrayOf(
        0,  0,  0,  0,  0,  0,  0,  0,
        6, 12, 12, 12, 12, 12, 12,  6,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        0,  0,  2,  8,  8,  2,  0,  0
    )

    private val QUEEN_PST = intArrayOf(
        -20,-10,-10, -5, -5,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5,  5,  5,  5,  0,-10,
        -5,  0,  5,  5,  5,  5,  0, -5,
        0,  0,  5,  5,  5,  5,  0, -5,
        -10,  5,  5,  5,  5,  5,  0,-10,
        -10,  0,  5,  0,  0,  0,  0,-10,
        -20,-10,-10, -5, -5,-10,-10,-20
    )

    private val KING_MID_PST = intArrayOf(
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -20,-30,-30,-40,-40,-30,-30,-20,
        -10,-20,-20,-20,-20,-20,-20,-10,
        20, 20,  0,  0,  0,  0, 20, 20,
        20, 32, 10,  0,  0, 10, 32, 20
    )

    private val KING_END_PST = intArrayOf(
        -50,-40,-30,-20,-20,-30,-40,-50,
        -30,-20,-10,  0,  0,-10,-20,-30,
        -30,-10, 20, 30, 30, 20,-10,-30,
        -30,-10, 30, 40, 40, 30,-10,-30,
        -30,-10, 30, 40, 40, 30,-10,-30,
        -30,-10, 20, 30, 30, 20,-10,-30,
        -30,-30,  0,  0,  0,  0,-30,-30,
        -50,-30,-30,-30,-30,-30,-30,-50
    )

    private class SearchContext(
        val deadlineMs: Long,
        var nodes: Int = 0,
        var aborted: Boolean = false
    )

    suspend fun evaluatePosition(
        state: BoardState,
        depth: Int = 3,
        timeBudgetMs: Long = 1200L
    ): EngineEvaluation = withContext(Dispatchers.Default) {
        if (state.status.isOver) {
            return@withContext when (state.status) {
                GameStatus.CHECKMATE_WHITE_WINS -> EngineEvaluation(MATE_SCORE, mateIn = 0)
                GameStatus.CHECKMATE_BLACK_WINS -> EngineEvaluation(-MATE_SCORE, mateIn = 0)
                else -> EngineEvaluation(0, mateIn = null)
            }
        }

        val legalMoves = ChessEngine.generateLegalMoves(state)
        if (legalMoves.isEmpty()) {
            return@withContext EngineEvaluation(0)
        }

        val orderedRoot = orderMoves(state, legalMoves)
        val ctx = SearchContext(deadlineMs = System.currentTimeMillis() + timeBudgetMs)

        var bestMove: Move = orderedRoot.first()
        var bestScoreSideToMove = -INF

        // Iterative deepening from depth 1 up to target depth for responsiveness and move ordering
        var currentOrder = orderedRoot
        for (d in 1..depth) {
            coroutineContext.ensureActive()
            var alpha = -INF
            val beta = INF
            var iterBestMove = currentOrder.first()
            var iterBestScore = -INF
            val moveScores = ArrayList<Pair<Move, Int>>(currentOrder.size)

            for (move in currentOrder) {
                val next = fastStep(state, move)
                val score = -negamax(next, d - 1, -beta, -alpha, 1, ctx)
                if (ctx.aborted && d > 1) break
                moveScores.add(move to score)
                if (score > iterBestScore) {
                    iterBestScore = score
                    iterBestMove = move
                }
                if (score > alpha) {
                    alpha = score
                }
            }

            if (ctx.aborted && d > 1) break
            bestMove = iterBestMove
            bestScoreSideToMove = iterBestScore
            currentOrder = moveScores.sortedByDescending { it.second }.map { it.first }
            if (abs(bestScoreSideToMove) > MATE_SCORE - 500) break
        }

        val whiteScore = if (state.turn == PieceColor.WHITE) bestScoreSideToMove else -bestScoreSideToMove
        val mateIn = extractMateIn(whiteScore)
        EngineEvaluation(scoreCp = whiteScore, mateIn = mateIn, bestMove = bestMove)
    }

    suspend fun selectBotMove(
        state: BoardState,
        bot: BotProfile
    ): Move? = withContext(Dispatchers.Default) {
        val legalMoves = ChessEngine.generateLegalMoves(state)
        if (legalMoves.isEmpty()) return@withContext null
        if (legalMoves.size == 1) return@withContext legalMoves.first()

        // Lower-rated bots occasionally make a genuine human-like blunder
        if (bot.blunderRate > 0.0 && Random.nextDouble() < bot.blunderRate) {
            // Prefer non-obviously-suicidal random move for mid-tier, completely random for beginner
            val pool = if (bot.elo < 600) legalMoves else legalMoves.shuffled().take(5)
            return@withContext pool.random()
        }

        val timeBudget = when {
            bot.searchDepth >= 4 -> 1400L
            bot.searchDepth == 3 -> 850L
            else -> 400L
        }

        val ordered = orderMoves(state, legalMoves)
        val ctx = SearchContext(deadlineMs = System.currentTimeMillis() + timeBudget)

        val scoredMoves = mutableListOf<Pair<Move, Int>>()
        var alpha = -INF
        for (move in ordered) {
            coroutineContext.ensureActive()
            val next = fastStep(state, move)
            val baseScore = -negamax(next, (bot.searchDepth - 1).coerceAtLeast(0), -INF, -alpha, 1, ctx)
            val noise = if (bot.evalNoiseCp > 0) {
                Random.nextInt(-bot.evalNoiseCp, bot.evalNoiseCp + 1)
            } else {
                0
            }
            scoredMoves.add(move to (baseScore + noise))
            if (baseScore > alpha && bot.evalNoiseCp == 0) {
                alpha = baseScore
            }
            if (ctx.aborted && scoredMoves.isNotEmpty()) break
        }

        scoredMoves.maxByOrNull { it.second }?.first ?: legalMoves.first()
    }

    suspend fun analyzeGame(
        initialFen: String,
        history: List<MoveRecord>,
        onProgress: (Float) -> Unit
    ): GameAnalysisResult = withContext(Dispatchers.Default) {
        if (history.isEmpty()) {
            return@withContext GameAnalysisResult(
                moves = emptyList(),
                whiteAccuracy = 100.0,
                blackAccuracy = 100.0,
                whiteCounts = emptyMap(),
                blackCounts = emptyMap()
            )
        }

        var state = ChessEngine.fromFen(initialFen) ?: ChessEngine.initialState()
        val analyzed = mutableListOf<AnalyzedMove>()
        val whiteLosses = mutableListOf<Int>()
        val blackLosses = mutableListOf<Int>()
        val whiteCounts = mutableMapOf<MoveClassification, Int>()
        val blackCounts = mutableMapOf<MoveClassification, Int>()

        var currentEval = evaluatePosition(state, depth = 2, timeBudgetMs = 240L)

        for (i in history.indices) {
            coroutineContext.ensureActive()
            val record = history[i]
            val mover = state.turn
            val bestMove = currentEval.bestMove
            val bestSan = if (bestMove != null) {
                val legal = ChessEngine.generateLegalMoves(state)
                val mp = state.pieceAt(bestMove.from)
                if (mp != null) {
                    val cap = bestMove.isEnPassant || state.pieceAt(bestMove.to) != null
                    ChessEngine.buildSan(state, bestMove, mp, cap, legal, false, false)
                } else null
            } else null

            val evalBeforeWhite = clampForAccuracy(currentEval.scoreCp)
            val nextState = ChessEngine.makeMove(state, record.move) ?: break
            val nextEval = evaluatePosition(nextState, depth = 2, timeBudgetMs = 240L)
            val evalAfterWhite = clampForAccuracy(nextEval.scoreCp)

            // Centipawn loss from the perspective of the player who just moved
            val rawLoss = if (mover == PieceColor.WHITE) {
                evalBeforeWhite - evalAfterWhite
            } else {
                evalAfterWhite - evalBeforeWhite
            }
            val cpLoss = if (bestMove != null && bestMove.from == record.move.from &&
                bestMove.to == record.move.to && bestMove.promotion == record.move.promotion
            ) {
                0
            } else {
                rawLoss.coerceAtLeast(0)
            }

            val classification = when {
                cpLoss <= 20 -> MoveClassification.BEST
                cpLoss <= 55 -> MoveClassification.GOOD
                cpLoss <= 115 -> MoveClassification.INACCURACY
                cpLoss <= 240 -> MoveClassification.MISTAKE
                else -> MoveClassification.BLUNDER
            }

            if (mover == PieceColor.WHITE) {
                whiteLosses.add(cpLoss)
                whiteCounts[classification] = (whiteCounts[classification] ?: 0) + 1
            } else {
                blackLosses.add(cpLoss)
                blackCounts[classification] = (blackCounts[classification] ?: 0) + 1
            }

            analyzed.add(
                AnalyzedMove(
                    plyIndex = i,
                    san = record.san,
                    playedMove = record.move,
                    bestMove = bestMove,
                    bestMoveSan = bestSan,
                    evalBeforeCp = currentEval.scoreCp,
                    evalAfterCp = nextEval.scoreCp,
                    mateAfter = nextEval.mateIn,
                    centipawnLoss = cpLoss,
                    classification = classification,
                    moverColor = mover
                )
            )

            state = nextState
            currentEval = nextEval
            onProgress((i + 1).toFloat() / history.size.toFloat())
        }

        GameAnalysisResult(
            moves = analyzed,
            whiteAccuracy = computeAccuracy(whiteLosses),
            blackAccuracy = computeAccuracy(blackLosses),
            whiteCounts = whiteCounts,
            blackCounts = blackCounts
        )
    }

    private fun clampForAccuracy(scoreCp: Int): Int = scoreCp.coerceIn(-1200, 1200)

    private fun computeAccuracy(losses: List<Int>): Double {
        if (losses.isEmpty()) return 100.0
        val perMoveAcc = losses.map { loss ->
            val capped = loss.coerceAtMost(600).toDouble()
            (103.1668 * exp(-0.004354 * capped) - 3.1668).coerceIn(0.0, 100.0)
        }
        return perMoveAcc.average().coerceIn(0.0, 100.0)
    }

    private fun extractMateIn(whiteScore: Int): Int? {
        val threshold = MATE_SCORE - 1000
        if (abs(whiteScore) < threshold) return null
        val plies = MATE_SCORE - abs(whiteScore)
        val moves = ((plies + 1) / 2).coerceAtLeast(1)
        return if (whiteScore > 0) moves else -moves
    }

    private fun negamax(
        state: BoardState,
        depth: Int,
        alphaInit: Int,
        beta: Int,
        ply: Int,
        ctx: SearchContext
    ): Int {
        ctx.nodes++
        if ((ctx.nodes and 255) == 0 && System.currentTimeMillis() >= ctx.deadlineMs) {
            ctx.aborted = true
            return 0
        }

        if (state.halfMoveClock >= 100 || ChessEngine.isInsufficientMaterial(state.board)) {
            return 0
        }

        val legalMoves = ChessEngine.generateLegalMoves(state)
        if (legalMoves.isEmpty()) {
            return if (ChessEngine.isInCheck(state.board, state.turn)) {
                -(MATE_SCORE - ply)
            } else {
                0 // Stalemate
            }
        }

        if (depth <= 0) {
            return quiescence(state, alphaInit, beta, 0, ctx)
        }

        var alpha = alphaInit
        var best = -INF
        val ordered = orderMoves(state, legalMoves)

        for (move in ordered) {
            val next = fastStep(state, move)
            val score = -negamax(next, depth - 1, -beta, -alpha, ply + 1, ctx)
            if (ctx.aborted) return 0
            if (score > best) best = score
            if (score > alpha) alpha = score
            if (alpha >= beta) break
        }
        return best
    }

    private fun quiescence(
        state: BoardState,
        alphaInit: Int,
        beta: Int,
        qDepth: Int,
        ctx: SearchContext
    ): Int {
        ctx.nodes++
        if ((ctx.nodes and 255) == 0 && System.currentTimeMillis() >= ctx.deadlineMs) {
            ctx.aborted = true
            return 0
        }

        val standPat = staticEvalSideToMove(state)
        if (standPat >= beta) return beta
        if (qDepth >= 4) return standPat

        var alpha = maxOf(alphaInit, standPat)
        val tactical = ChessEngine.generateLegalMoves(state).filter { move ->
            move.isEnPassant || move.promotion != null || state.pieceAt(move.to) != null
        }
        if (tactical.isEmpty()) return alpha

        val ordered = orderMoves(state, tactical)
        for (move in ordered) {
            val next = fastStep(state, move)
            val score = -quiescence(next, -beta, -alpha, qDepth + 1, ctx)
            if (ctx.aborted) return alpha
            if (score >= beta) return beta
            if (score > alpha) alpha = score
        }
        return alpha
    }

    private fun orderMoves(state: BoardState, moves: List<Move>): List<Move> {
        return moves.sortedByDescending { move ->
            var score = 0
            val victim = if (move.isEnPassant) PieceType.PAWN else state.pieceAt(move.to)?.type
            val attacker = state.pieceAt(move.from)?.type
            if (victim != null && attacker != null) {
                // MVV-LVA: capture high-value pieces with low-value attackers first
                score += 10_000 + victim.centipawnValue * 10 - attacker.centipawnValue
            }
            if (move.promotion != null) {
                score += 8_000 + move.promotion.centipawnValue
            }
            if (move.isCastleKingside || move.isCastleQueenside) {
                score += 400
            }
            score
        }
    }

    private fun fastStep(state: BoardState, move: Move): BoardState {
        val moving = state.pieceAt(move.from)!!
        val isCapture = move.isEnPassant || state.pieceAt(move.to) != null
        val nextBoard = ChessEngine.applyMoveToBoard(state.board, move)

        var wk = state.castlingRights.whiteKingside
        var wq = state.castlingRights.whiteQueenside
        var bk = state.castlingRights.blackKingside
        var bq = state.castlingRights.blackQueenside
        if (moving.type == PieceType.KING) {
            if (moving.color == PieceColor.WHITE) { wk = false; wq = false }
            else { bk = false; bq = false }
        }
        val fIdx = move.from.index
        val tIdx = move.to.index
        if (fIdx == 0 || tIdx == 0) wq = false
        if (fIdx == 7 || tIdx == 7) wk = false
        if (fIdx == 56 || tIdx == 56) bq = false
        if (fIdx == 63 || tIdx == 63) bk = false

        val ep = if (moving.type == PieceType.PAWN && abs(move.to.rank - move.from.rank) == 2) {
            com.example.chess.model.Square((move.from.rank + move.to.rank) / 2, move.from.file)
        } else null

        return BoardState(
            board = nextBoard,
            turn = state.turn.opposite(),
            castlingRights = com.example.chess.model.CastlingRights(wk, wq, bk, bq),
            enPassantTarget = ep,
            halfMoveClock = if (moving.type == PieceType.PAWN || isCapture) 0 else state.halfMoveClock + 1,
            fullMoveNumber = if (state.turn == PieceColor.BLACK) state.fullMoveNumber + 1 else state.fullMoveNumber
        )
    }

    private fun staticEvalSideToMove(state: BoardState): Int {
        val whiteScore = staticEvalWhite(state.board)
        return if (state.turn == PieceColor.WHITE) whiteScore else -whiteScore
    }

    fun staticEvalWhite(board: List<com.example.chess.model.Piece?>): Int {
        var totalNonPawnMaterial = 0
        var whiteBishops = 0
        var blackBishops = 0

        for (i in 0 until 64) {
            val p = board[i] ?: continue
            if (p.type != PieceType.PAWN && p.type != PieceType.KING) {
                totalNonPawnMaterial += p.type.centipawnValue
            }
            if (p.type == PieceType.BISHOP) {
                if (p.color == PieceColor.WHITE) whiteBishops++ else blackBishops++
            }
        }
        val isEndgame = totalNonPawnMaterial <= 2600

        var score = 0
        for (idx in 0 until 64) {
            val p = board[idx] ?: continue
            val rank = idx / 8
            val file = idx % 8
            val tableIndex = if (p.color == PieceColor.WHITE) {
                (7 - rank) * 8 + file
            } else {
                rank * 8 + file
            }
            val pstBonus = when (p.type) {
                PieceType.PAWN -> PAWN_PST[tableIndex]
                PieceType.KNIGHT -> KNIGHT_PST[tableIndex]
                PieceType.BISHOP -> BISHOP_PST[tableIndex]
                PieceType.ROOK -> ROOK_PST[tableIndex]
                PieceType.QUEEN -> QUEEN_PST[tableIndex]
                PieceType.KING -> if (isEndgame) KING_END_PST[tableIndex] else KING_MID_PST[tableIndex]
            }
            val value = p.type.centipawnValue + pstBonus
            if (p.color == PieceColor.WHITE) score += value else score -= value
        }

        // Bishop pair bonus
        if (whiteBishops >= 2) score += 32
        if (blackBishops >= 2) score -= 32

        return score
    }
}
