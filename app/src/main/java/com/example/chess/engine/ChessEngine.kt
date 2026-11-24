package com.example.chess.engine

import com.example.chess.model.BoardState
import com.example.chess.model.CastlingRights
import com.example.chess.model.GameStatus
import com.example.chess.model.MaterialSummary
import com.example.chess.model.Move
import com.example.chess.model.MoveRecord
import com.example.chess.model.Piece
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import com.example.chess.model.Square
import kotlin.math.abs

/**
 * Pure Kotlin FIDE-compliant chess rules engine.
 * Supports full legal move generation, castling, en passant, promotion,
 * check/checkmate, stalemate, insufficient material, threefold repetition,
 * 50-move rule, FEN parsing/generation, SAN notation, and PGN import/export.
 */
object ChessEngine {

    private val KNIGHT_OFFSETS = arrayOf(
        -2 to -1, -2 to 1, -1 to -2, -1 to 2,
        1 to -2, 1 to 2, 2 to -1, 2 to 1
    )

    private val KING_OFFSETS = arrayOf(
        -1 to -1, -1 to 0, -1 to 1,
        0 to -1, 0 to 1,
        1 to -1, 1 to 0, 1 to 1
    )

    private val BISHOP_DIRS = arrayOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
    private val ROOK_DIRS = arrayOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
    private val QUEEN_DIRS = BISHOP_DIRS + ROOK_DIRS

    fun initialState(): BoardState = fromFen(BoardState.INITIAL_FEN)!!

    fun fromFen(fen: String): BoardState? {
        val parts = fen.trim().split(Regex("\\s+"))
        if (parts.size < 4) return null

        val rows = parts[0].split("/")
        if (rows.size != 8) return null

        val board = MutableList<Piece?>(64) { null }
        for (fenRowIndex in 0..7) {
            val rank = 7 - fenRowIndex
            var file = 0
            for (ch in rows[fenRowIndex]) {
                if (ch in '1'..'8') {
                    file += (ch - '0')
                } else {
                    val pieceType = PieceType.fromFenChar(ch) ?: return null
                    val color = if (ch.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
                    if (file > 7) return null
                    board[rank * 8 + file] = Piece(pieceType, color)
                    file++
                }
            }
            if (file != 8) return null
        }

        val turn = when (parts[1]) {
            "w" -> PieceColor.WHITE
            "b" -> PieceColor.BLACK
            else -> return null
        }

        val castlingStr = parts[2]
        val castlingRights = CastlingRights(
            whiteKingside = 'K' in castlingStr,
            whiteQueenside = 'Q' in castlingStr,
            blackKingside = 'k' in castlingStr,
            blackQueenside = 'q' in castlingStr
        )

        val enPassantTarget = if (parts[3] == "-") null else Square.fromAlgebraic(parts[3])
        val halfMoveClock = parts.getOrNull(4)?.toIntOrNull() ?: 0
        val fullMoveNumber = parts.getOrNull(5)?.toIntOrNull() ?: 1

        val baseState = BoardState(
            board = board.toList(),
            turn = turn,
            castlingRights = castlingRights,
            enPassantTarget = enPassantTarget,
            halfMoveClock = halfMoveClock,
            fullMoveNumber = fullMoveNumber,
            initialFen = fen.trim()
        )
        val key = positionKey(baseState)
        val withCount = baseState.copy(positionCounts = mapOf(key to 1))
        val status = evaluateStatus(withCount)
        return withCount.copy(status = status)
    }

    fun toFen(state: BoardState): String {
        val sb = StringBuilder()
        for (rank in 7 downTo 0) {
            var emptyCount = 0
            for (file in 0..7) {
                val piece = state.board[rank * 8 + file]
                if (piece == null) {
                    emptyCount++
                } else {
                    if (emptyCount > 0) {
                        sb.append(emptyCount)
                        emptyCount = 0
                    }
                    sb.append(piece.fenChar)
                }
            }
            if (emptyCount > 0) sb.append(emptyCount)
            if (rank > 0) sb.append('/')
        }
        sb.append(' ')
        sb.append(if (state.turn == PieceColor.WHITE) 'w' else 'b')
        sb.append(' ')
        sb.append(state.castlingRights.toFenPart())
        sb.append(' ')
        sb.append(state.enPassantTarget?.algebraic ?: "-")
        sb.append(' ')
        sb.append(state.halfMoveClock)
        sb.append(' ')
        sb.append(state.fullMoveNumber)
        return sb.toString()
    }

    fun positionKey(state: BoardState): String {
        val sb = StringBuilder()
        for (rank in 7 downTo 0) {
            var empty = 0
            for (file in 0..7) {
                val p = state.board[rank * 8 + file]
                if (p == null) {
                    empty++
                } else {
                    if (empty > 0) {
                        sb.append(empty)
                        empty = 0
                    }
                    sb.append(p.fenChar)
                }
            }
            if (empty > 0) sb.append(empty)
            if (rank > 0) sb.append('/')
        }
        sb.append(' ')
        sb.append(if (state.turn == PieceColor.WHITE) 'w' else 'b')
        sb.append(' ')
        sb.append(state.castlingRights.toFenPart())
        sb.append(' ')
        // Include en passant target only if an enemy pawn can actually capture en passant
        val ep = state.enPassantTarget
        val epValid = ep != null && canCaptureEnPassant(state.board, state.turn, ep)
        sb.append(if (epValid) ep!!.algebraic else "-")
        return sb.toString()
    }

    private fun canCaptureEnPassant(board: List<Piece?>, turn: PieceColor, ep: Square): Boolean {
        val pawnRank = if (turn == PieceColor.WHITE) ep.rank - 1 else ep.rank + 1
        if (pawnRank !in 0..7) return false
        for (df in intArrayOf(-1, 1)) {
            val f = ep.file + df
            if (f in 0..7) {
                val p = board[pawnRank * 8 + f]
                if (p?.type == PieceType.PAWN && p.color == turn) return true
            }
        }
        return false
    }

    fun findKingSquare(board: List<Piece?>, color: PieceColor): Square? {
        for (i in 0 until 64) {
            val p = board[i]
            if (p != null && p.type == PieceType.KING && p.color == color) {
                return Square.fromIndex(i)
            }
        }
        return null
    }

    fun isInCheck(board: List<Piece?>, color: PieceColor): Boolean {
        val kingSq = findKingSquare(board, color) ?: return false
        return isSquareAttacked(board, kingSq, color.opposite())
    }

    fun isInCheck(state: BoardState, color: PieceColor = state.turn): Boolean =
        isInCheck(state.board, color)

    fun isSquareAttacked(board: List<Piece?>, target: Square, byColor: PieceColor): Boolean {
        val r = target.rank
        val f = target.file

        // Pawn attacks
        val pawnSourceRank = if (byColor == PieceColor.WHITE) r - 1 else r + 1
        if (pawnSourceRank in 0..7) {
            if (f - 1 >= 0) {
                val p = board[pawnSourceRank * 8 + (f - 1)]
                if (p != null && p.color == byColor && p.type == PieceType.PAWN) return true
            }
            if (f + 1 <= 7) {
                val p = board[pawnSourceRank * 8 + (f + 1)]
                if (p != null && p.color == byColor && p.type == PieceType.PAWN) return true
            }
        }

        // Knight attacks
        for ((dr, df) in KNIGHT_OFFSETS) {
            val nr = r + dr
            val nf = f + df
            if (nr in 0..7 && nf in 0..7) {
                val p = board[nr * 8 + nf]
                if (p != null && p.color == byColor && p.type == PieceType.KNIGHT) return true
            }
        }

        // King attacks
        for ((dr, df) in KING_OFFSETS) {
            val nr = r + dr
            val nf = f + df
            if (nr in 0..7 && nf in 0..7) {
                val p = board[nr * 8 + nf]
                if (p != null && p.color == byColor && p.type == PieceType.KING) return true
            }
        }

        // Bishop / Queen diagonal rays
        for ((dr, df) in BISHOP_DIRS) {
            var nr = r + dr
            var nf = f + df
            while (nr in 0..7 && nf in 0..7) {
                val p = board[nr * 8 + nf]
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                nr += dr
                nf += df
            }
        }

        // Rook / Queen orthogonal rays
        for ((dr, df) in ROOK_DIRS) {
            var nr = r + dr
            var nf = f + df
            while (nr in 0..7 && nf in 0..7) {
                val p = board[nr * 8 + nf]
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                nr += dr
                nf += df
            }
        }

        return false
    }

    fun generateLegalMoves(state: BoardState): List<Move> {
        if (state.status.isOver) return emptyList()
        val pseudo = generatePseudoLegalMoves(state)
        val legal = ArrayList<Move>(pseudo.size)
        for (move in pseudo) {
            val nextBoard = applyMoveToBoard(state.board, move)
            if (!isInCheck(nextBoard, state.turn)) {
                legal.add(move)
            }
        }
        return legal
    }

    fun generateLegalMovesForSquare(state: BoardState, square: Square): List<Move> {
        if (state.status.isOver) return emptyList()
        val piece = state.pieceAt(square) ?: return emptyList()
        if (piece.color != state.turn) return emptyList()
        val pseudo = ArrayList<Move>(16)
        generatePieceMoves(state, square, piece, pseudo)
        return pseudo.filter { move ->
            val nextBoard = applyMoveToBoard(state.board, move)
            !isInCheck(nextBoard, state.turn)
        }
    }

    private fun generatePseudoLegalMoves(state: BoardState): List<Move> {
        val moves = ArrayList<Move>(48)
        for (idx in 0 until 64) {
            val piece = state.board[idx] ?: continue
            if (piece.color != state.turn) continue
            generatePieceMoves(state, Square.fromIndex(idx), piece, moves)
        }
        return moves
    }

    private fun generatePieceMoves(
        state: BoardState,
        from: Square,
        piece: Piece,
        out: MutableList<Move>
    ) {
        val board = state.board
        when (piece.type) {
            PieceType.PAWN -> {
                val dir = if (piece.color == PieceColor.WHITE) 1 else -1
                val startRank = if (piece.color == PieceColor.WHITE) 1 else 6
                val promoRank = if (piece.color == PieceColor.WHITE) 7 else 0

                // Single push
                val oneRank = from.rank + dir
                if (oneRank in 0..7 && board[oneRank * 8 + from.file] == null) {
                    val to = Square(oneRank, from.file)
                    if (oneRank == promoRank) {
                        for (promo in PieceType.PROMOTION_OPTIONS) {
                            out.add(Move(from, to, promotion = promo))
                        }
                    } else {
                        out.add(Move(from, to))
                        // Double push from starting rank
                        val twoRank = from.rank + 2 * dir
                        if (from.rank == startRank && board[twoRank * 8 + from.file] == null) {
                            out.add(Move(from, Square(twoRank, from.file)))
                        }
                    }
                }

                // Captures + En Passant
                for (df in intArrayOf(-1, 1)) {
                    val nf = from.file + df
                    if (oneRank in 0..7 && nf in 0..7) {
                        val target = Square(oneRank, nf)
                        val occupant = board[target.index]
                        if (occupant != null && occupant.color != piece.color) {
                            if (oneRank == promoRank) {
                                for (promo in PieceType.PROMOTION_OPTIONS) {
                                    out.add(Move(from, target, promotion = promo))
                                }
                            } else {
                                out.add(Move(from, target))
                            }
                        } else if (state.enPassantTarget != null && state.enPassantTarget == target) {
                            out.add(Move(from, target, isEnPassant = true))
                        }
                    }
                }
            }

            PieceType.KNIGHT -> {
                for ((dr, df) in KNIGHT_OFFSETS) {
                    val nr = from.rank + dr
                    val nf = from.file + df
                    if (nr in 0..7 && nf in 0..7) {
                        val occ = board[nr * 8 + nf]
                        if (occ == null || occ.color != piece.color) {
                            out.add(Move(from, Square(nr, nf)))
                        }
                    }
                }
            }

            PieceType.BISHOP -> addSlidingMoves(board, from, piece.color, BISHOP_DIRS, out)
            PieceType.ROOK -> addSlidingMoves(board, from, piece.color, ROOK_DIRS, out)
            PieceType.QUEEN -> addSlidingMoves(board, from, piece.color, QUEEN_DIRS, out)

            PieceType.KING -> {
                for ((dr, df) in KING_OFFSETS) {
                    val nr = from.rank + dr
                    val nf = from.file + df
                    if (nr in 0..7 && nf in 0..7) {
                        val occ = board[nr * 8 + nf]
                        if (occ == null || occ.color != piece.color) {
                            out.add(Move(from, Square(nr, nf)))
                        }
                    }
                }
                addCastlingMoves(state, from, piece.color, out)
            }
        }
    }

    private fun addSlidingMoves(
        board: List<Piece?>,
        from: Square,
        color: PieceColor,
        dirs: Array<Pair<Int, Int>>,
        out: MutableList<Move>
    ) {
        for ((dr, df) in dirs) {
            var nr = from.rank + dr
            var nf = from.file + df
            while (nr in 0..7 && nf in 0..7) {
                val occ = board[nr * 8 + nf]
                if (occ == null) {
                    out.add(Move(from, Square(nr, nf)))
                } else {
                    if (occ.color != color) {
                        out.add(Move(from, Square(nr, nf)))
                    }
                    break
                }
                nr += dr
                nf += df
            }
        }
    }

    private fun addCastlingMoves(
        state: BoardState,
        kingSq: Square,
        color: PieceColor,
        out: MutableList<Move>
    ) {
        val board = state.board
        val enemy = color.opposite()
        val homeRank = if (color == PieceColor.WHITE) 0 else 7
        if (kingSq.rank != homeRank || kingSq.file != 4) return
        if (isSquareAttacked(board, kingSq, enemy)) return

        val canKingSide = if (color == PieceColor.WHITE) {
            state.castlingRights.whiteKingside
        } else {
            state.castlingRights.blackKingside
        }
        val canQueenSide = if (color == PieceColor.WHITE) {
            state.castlingRights.whiteQueenside
        } else {
            state.castlingRights.blackQueenside
        }

        if (canKingSide) {
            val rook = board[homeRank * 8 + 7]
            if (rook?.type == PieceType.ROOK && rook.color == color &&
                board[homeRank * 8 + 5] == null &&
                board[homeRank * 8 + 6] == null &&
                !isSquareAttacked(board, Square(homeRank, 5), enemy) &&
                !isSquareAttacked(board, Square(homeRank, 6), enemy)
            ) {
                out.add(Move(kingSq, Square(homeRank, 6), isCastleKingside = true))
            }
        }

        if (canQueenSide) {
            val rook = board[homeRank * 8 + 0]
            if (rook?.type == PieceType.ROOK && rook.color == color &&
                board[homeRank * 8 + 1] == null &&
                board[homeRank * 8 + 2] == null &&
                board[homeRank * 8 + 3] == null &&
                !isSquareAttacked(board, Square(homeRank, 3), enemy) &&
                !isSquareAttacked(board, Square(homeRank, 2), enemy)
            ) {
                out.add(Move(kingSq, Square(homeRank, 2), isCastleQueenside = true))
            }
        }
    }

    internal fun applyMoveToBoard(board: List<Piece?>, move: Move): List<Piece?> {
        val next = board.toMutableList()
        val moving = next[move.from.index] ?: return board
        next[move.from.index] = null

        if (move.isEnPassant) {
            val capturedPawnRank = move.from.rank
            next[capturedPawnRank * 8 + move.to.file] = null
        }

        val placedPiece = if (move.promotion != null) {
            Piece(move.promotion, moving.color)
        } else {
            moving
        }
        next[move.to.index] = placedPiece

        if (move.isCastleKingside) {
            val r = move.from.rank
            val rook = next[r * 8 + 7]
            next[r * 8 + 7] = null
            next[r * 8 + 5] = rook
        } else if (move.isCastleQueenside) {
            val r = move.from.rank
            val rook = next[r * 8 + 0]
            next[r * 8 + 0] = null
            next[r * 8 + 3] = rook
        }
        return next
    }

    fun makeMove(state: BoardState, move: Move): BoardState? {
        if (state.status.isOver) return null
        val legalMoves = generateLegalMoves(state)
        val matched = legalMoves.firstOrNull {
            it.from == move.from && it.to == move.to && it.promotion == move.promotion
        } ?: return null

        val fenBefore = toFen(state)
        val movingPiece = state.pieceAt(matched.from) ?: return null
        val capturedPiece = if (matched.isEnPassant) {
            Piece(PieceType.PAWN, movingPiece.color.opposite())
        } else {
            state.pieceAt(matched.to)
        }

        val nextBoard = applyMoveToBoard(state.board, matched)
        val nextTurn = state.turn.opposite()
        val nextCastling = updateCastlingRights(state.castlingRights, matched, movingPiece)

        val nextEnPassant = if (
            movingPiece.type == PieceType.PAWN &&
            abs(matched.to.rank - matched.from.rank) == 2
        ) {
            Square((matched.from.rank + matched.to.rank) / 2, matched.from.file)
        } else {
            null
        }

        val nextHalfMove = if (movingPiece.type == PieceType.PAWN || capturedPiece != null) {
            0
        } else {
            state.halfMoveClock + 1
        }
        val nextFullMove = if (state.turn == PieceColor.BLACK) {
            state.fullMoveNumber + 1
        } else {
            state.fullMoveNumber
        }

        val tentative = BoardState(
            board = nextBoard,
            turn = nextTurn,
            castlingRights = nextCastling,
            enPassantTarget = nextEnPassant,
            halfMoveClock = nextHalfMove,
            fullMoveNumber = nextFullMove,
            positionCounts = state.positionCounts,
            moveHistory = state.moveHistory,
            status = GameStatus.IN_PROGRESS,
            initialFen = state.initialFen
        )

        val key = positionKey(tentative)
        val updatedCounts = state.positionCounts.toMutableMap()
        updatedCounts[key] = (updatedCounts[key] ?: 0) + 1

        val withCounts = tentative.copy(positionCounts = updatedCounts)
        val nextStatus = evaluateStatus(withCounts)
        val check = isInCheck(nextBoard, nextTurn)
        val checkmate = nextStatus == GameStatus.CHECKMATE_WHITE_WINS ||
            nextStatus == GameStatus.CHECKMATE_BLACK_WINS

        val san = buildSan(
            state = state,
            move = matched,
            movingPiece = movingPiece,
            isCapture = capturedPiece != null,
            legalMoves = legalMoves,
            isCheck = check,
            isCheckmate = checkmate
        )

        val finalWithoutHistory = withCounts.copy(status = nextStatus)
        val fenAfter = toFen(finalWithoutHistory)
        val record = MoveRecord(
            move = matched,
            san = san,
            fenBefore = fenBefore,
            fenAfter = fenAfter,
            movedPiece = movingPiece,
            capturedPiece = capturedPiece,
            isCheck = check,
            isCheckmate = checkmate
        )

        return finalWithoutHistory.copy(moveHistory = state.moveHistory + record)
    }

    fun makeMoveByUci(state: BoardState, uci: String): BoardState? {
        val clean = uci.trim().lowercase()
        if (clean.length < 4) return null
        val from = Square.fromAlgebraic(clean.substring(0, 2)) ?: return null
        val to = Square.fromAlgebraic(clean.substring(2, 4)) ?: return null
        val promo = if (clean.length >= 5) PieceType.fromFenChar(clean[4]) else null
        return makeMove(state, Move(from, to, promotion = promo))
    }

    private fun updateCastlingRights(
        rights: CastlingRights,
        move: Move,
        movingPiece: Piece
    ): CastlingRights {
        var wk = rights.whiteKingside
        var wq = rights.whiteQueenside
        var bk = rights.blackKingside
        var bq = rights.blackQueenside

        if (movingPiece.type == PieceType.KING) {
            if (movingPiece.color == PieceColor.WHITE) {
                wk = false
                wq = false
            } else {
                bk = false
                bq = false
            }
        }
        val touched = listOf(move.from, move.to)
        for (sq in touched) {
            when (sq) {
                Square(0, 0) -> wq = false
                Square(0, 7) -> wk = false
                Square(7, 0) -> bq = false
                Square(7, 7) -> bk = false
            }
        }
        return CastlingRights(wk, wq, bk, bq)
    }

    private fun evaluateStatus(state: BoardState): GameStatus {
        val pseudo = generatePseudoLegalMoves(state)
        val hasLegal = pseudo.any { move ->
            !isInCheck(applyMoveToBoard(state.board, move), state.turn)
        }
        if (!hasLegal) {
            return if (isInCheck(state.board, state.turn)) {
                if (state.turn == PieceColor.WHITE) {
                    GameStatus.CHECKMATE_BLACK_WINS
                } else {
                    GameStatus.CHECKMATE_WHITE_WINS
                }
            } else {
                GameStatus.STALEMATE
            }
        }
        if (isInsufficientMaterial(state.board)) {
            return GameStatus.DRAW_INSUFFICIENT_MATERIAL
        }
        val key = positionKey(state)
        if ((state.positionCounts[key] ?: 0) >= 3) {
            return GameStatus.DRAW_THREEFOLD_REPETITION
        }
        if (state.halfMoveClock >= 100) {
            return GameStatus.DRAW_FIFTY_MOVE_RULE
        }
        return GameStatus.IN_PROGRESS
    }

    fun isInsufficientMaterial(board: List<Piece?>): Boolean {
        val minors = mutableListOf<Pair<Piece, Square>>()
        for (idx in 0 until 64) {
            val p = board[idx] ?: continue
            when (p.type) {
                PieceType.PAWN, PieceType.ROOK, PieceType.QUEEN -> return false
                PieceType.BISHOP, PieceType.KNIGHT -> minors.add(p to Square.fromIndex(idx))
                PieceType.KING -> Unit
            }
        }
        if (minors.isEmpty()) return true // K vs K
        if (minors.size == 1) return true // K+B vs K or K+N vs K
        if (minors.size == 2) {
            val (p1, s1) = minors[0]
            val (p2, s2) = minors[1]
            // K+B vs K+B on same-colored squares
            if (p1.type == PieceType.BISHOP && p2.type == PieceType.BISHOP &&
                p1.color != p2.color && s1.isLightSquare == s2.isLightSquare
            ) {
                return true
            }
        }
        return false
    }

    fun buildSan(
        state: BoardState,
        move: Move,
        movingPiece: Piece,
        isCapture: Boolean,
        legalMoves: List<Move>,
        isCheck: Boolean,
        isCheckmate: Boolean
    ): String {
        val sb = StringBuilder()
        if (move.isCastleKingside) {
            sb.append("O-O")
        } else if (move.isCastleQueenside) {
            sb.append("O-O-O")
        } else {
            if (movingPiece.type == PieceType.PAWN) {
                if (isCapture) {
                    sb.append('a' + move.from.file)
                    sb.append('x')
                }
                sb.append(move.to.algebraic)
                if (move.promotion != null) {
                    sb.append('=')
                    sb.append(move.promotion.sanLetter)
                }
            } else {
                sb.append(movingPiece.type.sanLetter)
                val ambiguous = legalMoves.filter { other ->
                    other.from != move.from &&
                        other.to == move.to &&
                        state.pieceAt(other.from) == movingPiece
                }
                if (ambiguous.isNotEmpty()) {
                    val sameFile = ambiguous.any { it.from.file == move.from.file }
                    val sameRank = ambiguous.any { it.from.rank == move.from.rank }
                    if (!sameFile) {
                        sb.append('a' + move.from.file)
                    } else if (!sameRank) {
                        sb.append(move.from.rank + 1)
                    } else {
                        sb.append(move.from.algebraic)
                    }
                }
                if (isCapture) sb.append('x')
                sb.append(move.to.algebraic)
            }
        }
        if (isCheckmate) {
            sb.append('#')
        } else if (isCheck) {
            sb.append('+')
        }
        return sb.toString()
    }

    fun calculateMaterialSummary(board: List<Piece?>): MaterialSummary {
        val startCounts = mapOf(
            PieceType.QUEEN to 1,
            PieceType.ROOK to 2,
            PieceType.BISHOP to 2,
            PieceType.KNIGHT to 2,
            PieceType.PAWN to 8
        )
        val whitePresent = mutableMapOf<PieceType, Int>()
        val blackPresent = mutableMapOf<PieceType, Int>()
        var whiteScore = 0
        var blackScore = 0

        for (p in board) {
            if (p == null) continue
            if (p.color == PieceColor.WHITE) {
                whitePresent[p.type] = (whitePresent[p.type] ?: 0) + 1
                whiteScore += p.type.materialValue
            } else {
                blackPresent[p.type] = (blackPresent[p.type] ?: 0) + 1
                blackScore += p.type.materialValue
            }
        }

        // Pieces White has captured = Black's missing pieces
        val whiteCaptured = mutableListOf<PieceType>()
        val blackCaptured = mutableListOf<PieceType>()
        val displayOrder = listOf(
            PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT, PieceType.PAWN
        )
        for (type in displayOrder) {
            val expected = startCounts[type] ?: 0
            val missingBlack = (expected - (blackPresent[type] ?: 0)).coerceAtLeast(0)
            repeat(missingBlack) { whiteCaptured.add(type) }

            val missingWhite = (expected - (whitePresent[type] ?: 0)).coerceAtLeast(0)
            repeat(missingWhite) { blackCaptured.add(type) }
        }
        return MaterialSummary(
            whiteCaptured = whiteCaptured,
            blackCaptured = blackCaptured,
            whiteAdvantage = whiteScore - blackScore
        )
    }

    fun toPgn(
        state: BoardState,
        whiteName: String = "White",
        blackName: String = "Black",
        event: String = "Grand Master Chess Offline"
    ): String {
        val sb = StringBuilder()
        sb.appendLine("[Event \"$event\"]")
        sb.appendLine("[Site \"Grand Master Chess\"]")
        sb.appendLine("[White \"$whiteName\"]")
        sb.appendLine("[Black \"$blackName\"]")
        sb.appendLine("[Result \"${state.status.pgnResult()}\"]")
        if (state.initialFen != BoardState.INITIAL_FEN) {
            sb.appendLine("[SetUp \"1\"]")
            sb.appendLine("[FEN \"${state.initialFen}\"]")
        }
        sb.appendLine()

        var moveNum = 1
        val history = state.moveHistory
        for (i in history.indices) {
            val rec = history[i]
            if (rec.movedPiece.color == PieceColor.WHITE) {
                sb.append("$moveNum. ${rec.san} ")
            } else {
                if (i == 0) {
                    sb.append("$moveNum... ${rec.san} ")
                } else {
                    sb.append("${rec.san} ")
                }
                moveNum++
            }
        }
        sb.append(state.status.pgnResult())
        return sb.toString().trim()
    }

    fun fromPgn(pgn: String): BoardState? {
        val fenHeaderRegex = Regex("""\[FEN\s+"([^"]+)"\]""")
        val customFen = fenHeaderRegex.find(pgn)?.groupValues?.getOrNull(1)
        var state = if (customFen != null) {
            fromFen(customFen) ?: return null
        } else {
            initialState()
        }

        // Remove header tags, brace comments, semicolon comments, and parenthetical variations
        var body = pgn.replace(Regex("""\[[^\]]*\]"""), " ")
        body = body.replace(Regex("""\{[^}]*\}"""), " ")
        body = body.replace(Regex(""";[^\n]*"""), " ")
        // Strip nested simple parentheses
        repeat(3) {
            body = body.replace(Regex("""\([^()]*\)"""), " ")
        }

        val tokens = body.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        for (rawToken in tokens) {
            val token = rawToken.trim()
            if (token in setOf("1-0", "0-1", "1/2-1/2", "*")) break
            if (token.startsWith("$")) continue
            // Strip move numbers like "1." or "12..." or "1.e4"
            val cleaned = token.replace(Regex("""^\d+\.+"""), "")
            if (cleaned.isEmpty()) continue
            val normalized = cleaned
                .replace("0-0-0", "O-O-O")
                .replace("0-0", "O-O")
                .trimEnd('!', '?')

            val matched = findMoveMatchingSan(state, normalized) ?: return null
            state = makeMove(state, matched) ?: return null
        }
        return state
    }

    private fun findMoveMatchingSan(state: BoardState, targetSan: String): Move? {
        val legal = generateLegalMoves(state)
        val strippedTarget = targetSan.trimEnd('+', '#')
        for (move in legal) {
            val movingPiece = state.pieceAt(move.from) ?: continue
            val isCapture = move.isEnPassant || state.pieceAt(move.to) != null
            val candidate = buildSan(
                state = state,
                move = move,
                movingPiece = movingPiece,
                isCapture = isCapture,
                legalMoves = legal,
                isCheck = false,
                isCheckmate = false
            )
            if (candidate == strippedTarget) {
                return move
            }
        }
        return null
    }
}
