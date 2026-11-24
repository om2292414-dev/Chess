package com.example

import com.example.chess.data.PuzzleRepository
import com.example.chess.engine.ChessAi
import com.example.chess.engine.ChessEngine
import com.example.chess.model.GameStatus
import com.example.chess.model.Move
import com.example.chess.model.PieceType
import com.example.chess.model.Square
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChessEngineTest {

    @Test
    fun initialPositionHas20LegalMoves() {
        val state = ChessEngine.initialState()
        assertEquals(20, ChessEngine.generateLegalMoves(state).size)
    }

    @Test
    fun foolsMateProducesCheckmate() {
        var state = ChessEngine.initialState()
        state = ChessEngine.makeMoveByUci(state, "f2f3")!!
        state = ChessEngine.makeMoveByUci(state, "e7e5")!!
        state = ChessEngine.makeMoveByUci(state, "g2g4")!!
        state = ChessEngine.makeMoveByUci(state, "d8h4")!!
        assertEquals(GameStatus.CHECKMATE_BLACK_WINS, state.status)
        assertEquals("Qh4#", state.moveHistory.last().san)
    }

    @Test
    fun enPassantCaptureRemovesEnemyPawn() {
        var state = ChessEngine.fromFen("rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 3")!!
        state = ChessEngine.makeMoveByUci(state, "e5d6")!!
        assertEquals(null, state.pieceAt(Square.fromAlgebraic("d5")!!))
        assertEquals(PieceType.PAWN, state.pieceAt(Square.fromAlgebraic("d6")!!)?.type)
    }

    @Test
    fun castlingMovesKingAndRook() {
        var state = ChessEngine.fromFen("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1")!!
        state = ChessEngine.makeMoveByUci(state, "e1g1")!!
        assertEquals(PieceType.KING, state.pieceAt(Square.fromAlgebraic("g1")!!)?.type)
        assertEquals(PieceType.ROOK, state.pieceAt(Square.fromAlgebraic("f1")!!)?.type)
        assertEquals("O-O", state.moveHistory.last().san)
    }

    @Test
    fun stalemateAndInsufficientMaterialDetected() {
        val stale = ChessEngine.fromFen("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1")!!
        assertEquals(GameStatus.STALEMATE, stale.status)

        val insuff = ChessEngine.fromFen("4k3/8/8/8/8/8/8/4K2B w - - 0 1")!!
        assertEquals(GameStatus.DRAW_INSUFFICIENT_MATERIAL, insuff.status)
    }

    @Test
    fun pgnRoundTripPreservesMoves() {
        val samplePgn = "1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 4. Ba4 Nf6 5. O-O *"
        val parsed = ChessEngine.fromPgn(samplePgn)
        assertNotNull(parsed)
        assertEquals(9, parsed!!.moveHistory.size)
        val exported = ChessEngine.toPgn(parsed)
        assertTrue(exported.contains("5. O-O"))
    }

    @Test
    fun allOfflinePuzzlesAreValidAndLegal() {
        assertTrue(PuzzleRepository.ALL_PUZZLES.size >= 20)
        for (puzzle in PuzzleRepository.ALL_PUZZLES) {
            var state = ChessEngine.fromFen(puzzle.fen)
            assertNotNull("Invalid FEN in puzzle ${puzzle.id}", state)
            for (uci in puzzle.solutionUci) {
                state = ChessEngine.makeMoveByUci(state!!, uci)
                assertNotNull("Illegal move $uci in puzzle ${puzzle.id}", state)
            }
        }
    }

    @Test
    fun chessAiFindsMateInOne() = runBlocking {
        val state = ChessEngine.fromFen("6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1")!!
        val eval = ChessAi.evaluatePosition(state, depth = 2, timeBudgetMs = 800L)
        assertEquals("d1d8", eval.bestMove?.uci())
        assertNotNull(eval.mateIn)
    }
}
