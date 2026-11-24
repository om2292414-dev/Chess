package com.example.chess.model

enum class PieceColor {
    WHITE,
    BLACK;

    fun opposite(): PieceColor = if (this == WHITE) BLACK else WHITE
}

enum class PieceType(
    val materialValue: Int,
    val centipawnValue: Int,
    val sanLetter: String,
    val fenLower: Char
) {
    PAWN(1, 100, "", 'p'),
    KNIGHT(3, 320, "N", 'n'),
    BISHOP(3, 330, "B", 'b'),
    ROOK(5, 500, "R", 'r'),
    QUEEN(9, 900, "Q", 'q'),
    KING(0, 20000, "K", 'k');

    companion object {
        val PROMOTION_OPTIONS = listOf(QUEEN, ROOK, BISHOP, KNIGHT)

        fun fromFenChar(c: Char): PieceType? = when (c.lowercaseChar()) {
            'p' -> PAWN
            'n' -> KNIGHT
            'b' -> BISHOP
            'r' -> ROOK
            'q' -> QUEEN
            'k' -> KING
            else -> null
        }
    }
}

data class Piece(
    val type: PieceType,
    val color: PieceColor
) {
    val fenChar: Char
        get() = if (color == PieceColor.WHITE) type.fenLower.uppercaseChar() else type.fenLower
}

data class Square(
    val rank: Int,
    val file: Int
) {
    val index: Int get() = rank * 8 + file

    val algebraic: String
        get() = "${'a' + file}${rank + 1}"

    val isLightSquare: Boolean
        get() = (rank + file) % 2 != 0

    companion object {
        fun fromIndex(index: Int): Square = Square(index / 8, index % 8)

        fun fromAlgebraic(text: String): Square? {
            if (text.length != 2) return null
            val f = text[0].lowercaseChar() - 'a'
            val r = text[1] - '1'
            if (f !in 0..7 || r !in 0..7) return null
            return Square(r, f)
        }

        fun isValid(rank: Int, file: Int): Boolean = rank in 0..7 && file in 0..7
    }
}

data class Move(
    val from: Square,
    val to: Square,
    val promotion: PieceType? = null,
    val isEnPassant: Boolean = false,
    val isCastleKingside: Boolean = false,
    val isCastleQueenside: Boolean = false
) {
    fun uci(): String {
        val promo = promotion?.fenLower?.toString() ?: ""
        return "${from.algebraic}${to.algebraic}$promo"
    }
}

data class CastlingRights(
    val whiteKingside: Boolean = true,
    val whiteQueenside: Boolean = true,
    val blackKingside: Boolean = true,
    val blackQueenside: Boolean = true
) {
    fun toFenPart(): String {
        val sb = StringBuilder()
        if (whiteKingside) sb.append('K')
        if (whiteQueenside) sb.append('Q')
        if (blackKingside) sb.append('k')
        if (blackQueenside) sb.append('q')
        return if (sb.isEmpty()) "-" else sb.toString()
    }
}

enum class GameStatus(val isOver: Boolean) {
    IN_PROGRESS(false),
    CHECKMATE_WHITE_WINS(true),
    CHECKMATE_BLACK_WINS(true),
    STALEMATE(true),
    DRAW_INSUFFICIENT_MATERIAL(true),
    DRAW_THREEFOLD_REPETITION(true),
    DRAW_FIFTY_MOVE_RULE(true),
    DRAW_AGREEMENT(true),
    RESIGNED_WHITE(true),
    RESIGNED_BLACK(true),
    TIMEOUT_WHITE_WINS(true),
    TIMEOUT_BLACK_WINS(true);

    fun pgnResult(): String = when (this) {
        CHECKMATE_WHITE_WINS, RESIGNED_BLACK, TIMEOUT_WHITE_WINS -> "1-0"
        CHECKMATE_BLACK_WINS, RESIGNED_WHITE, TIMEOUT_BLACK_WINS -> "0-1"
        STALEMATE, DRAW_INSUFFICIENT_MATERIAL, DRAW_THREEFOLD_REPETITION,
        DRAW_FIFTY_MOVE_RULE, DRAW_AGREEMENT -> "1/2-1/2"
        IN_PROGRESS -> "*"
    }

    fun headline(): String = when (this) {
        IN_PROGRESS -> "In Progress"
        CHECKMATE_WHITE_WINS -> "White Wins"
        CHECKMATE_BLACK_WINS -> "Black Wins"
        STALEMATE -> "Draw"
        DRAW_INSUFFICIENT_MATERIAL -> "Draw"
        DRAW_THREEFOLD_REPETITION -> "Draw"
        DRAW_FIFTY_MOVE_RULE -> "Draw"
        DRAW_AGREEMENT -> "Draw"
        RESIGNED_WHITE -> "Black Wins"
        RESIGNED_BLACK -> "White Wins"
        TIMEOUT_WHITE_WINS -> "White Wins"
        TIMEOUT_BLACK_WINS -> "Black Wins"
    }

    fun terminationReason(): String = when (this) {
        IN_PROGRESS -> "Active"
        CHECKMATE_WHITE_WINS, CHECKMATE_BLACK_WINS -> "By Checkmate"
        STALEMATE -> "By Stalemate"
        DRAW_INSUFFICIENT_MATERIAL -> "Insufficient Material"
        DRAW_THREEFOLD_REPETITION -> "Threefold Repetition"
        DRAW_FIFTY_MOVE_RULE -> "50-Move Rule"
        DRAW_AGREEMENT -> "By Agreement"
        RESIGNED_WHITE -> "White Resigned"
        RESIGNED_BLACK -> "Black Resigned"
        TIMEOUT_WHITE_WINS -> "Black Flagged on Time"
        TIMEOUT_BLACK_WINS -> "White Flagged on Time"
    }
}

data class MoveRecord(
    val move: Move,
    val san: String,
    val fenBefore: String,
    val fenAfter: String,
    val movedPiece: Piece,
    val capturedPiece: Piece?,
    val isCheck: Boolean,
    val isCheckmate: Boolean
)

data class BoardState(
    val board: List<Piece?>,
    val turn: PieceColor = PieceColor.WHITE,
    val castlingRights: CastlingRights = CastlingRights(),
    val enPassantTarget: Square? = null,
    val halfMoveClock: Int = 0,
    val fullMoveNumber: Int = 1,
    val positionCounts: Map<String, Int> = emptyMap(),
    val moveHistory: List<MoveRecord> = emptyList(),
    val status: GameStatus = GameStatus.IN_PROGRESS,
    val initialFen: String = INITIAL_FEN
) {
    fun pieceAt(square: Square): Piece? = board[square.index]
    fun pieceAt(rank: Int, file: Int): Piece? = board[rank * 8 + file]

    companion object {
        const val INITIAL_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    }
}

data class MaterialSummary(
    val whiteCaptured: List<PieceType>,
    val blackCaptured: List<PieceType>,
    val whiteAdvantage: Int
)

enum class TimeControlCategory(val label: String) {
    BULLET("Bullet"),
    BLITZ("Blitz"),
    RAPID("Rapid"),
    NONE("No Timer"),
    CUSTOM("Custom")
}

data class TimeControl(
    val id: String,
    val label: String,
    val category: TimeControlCategory,
    val initialMillis: Long,
    val incrementMillis: Long
) {
    val isTimed: Boolean get() = initialMillis > 0L

    companion object {
        val PRESETS = listOf(
            TimeControl("bullet_1_0", "1 min", TimeControlCategory.BULLET, 60_000L, 0L),
            TimeControl("bullet_1_1", "1 | 1", TimeControlCategory.BULLET, 60_000L, 1_000L),
            TimeControl("bullet_2_1", "2 | 1", TimeControlCategory.BULLET, 120_000L, 1_000L),
            TimeControl("blitz_3_0", "3 min", TimeControlCategory.BLITZ, 180_000L, 0L),
            TimeControl("blitz_3_2", "3 | 2", TimeControlCategory.BLITZ, 180_000L, 2_000L),
            TimeControl("blitz_5_0", "5 min", TimeControlCategory.BLITZ, 300_000L, 0L),
            TimeControl("rapid_10_0", "10 min", TimeControlCategory.RAPID, 600_000L, 0L),
            TimeControl("rapid_15_10", "15 | 10", TimeControlCategory.RAPID, 900_000L, 10_000L),
            TimeControl("rapid_30_0", "30 min", TimeControlCategory.RAPID, 1_800_000L, 0L),
            TimeControl("no_timer", "No Timer", TimeControlCategory.NONE, 0L, 0L)
        )

        fun custom(minutes: Int, incrementSeconds: Int): TimeControl {
            val mins = minutes.coerceIn(1, 180)
            val inc = incrementSeconds.coerceIn(0, 60)
            val label = if (inc > 0) "$mins | $inc" else "$mins min"
            return TimeControl(
                id = "custom_${mins}_${inc}",
                label = label,
                category = TimeControlCategory.CUSTOM,
                initialMillis = mins * 60_000L,
                incrementMillis = inc * 1_000L
            )
        }
    }
}

enum class GameMode {
    VS_BOT,
    PASS_AND_PLAY
}

enum class SidePreference(val label: String) {
    WHITE("White"),
    RANDOM("Random"),
    BLACK("Black")
}

data class BotProfile(
    val id: String,
    val name: String,
    val elo: Int,
    val tierBadge: String,
    val bio: String,
    val searchDepth: Int,
    val blunderRate: Double,
    val evalNoiseCp: Int,
    val accentHex: Long,
    val avatarPiece: PieceType
) {
    companion object {
        val ROSTER = listOf(
            BotProfile(
                id = "bot_martin",
                name = "Martin",
                elo = 250,
                tierBadge = "Beginner",
                bio = "Loves pushing pawns and learning the ropes.",
                searchDepth = 1,
                blunderRate = 0.55,
                evalNoiseCp = 260,
                accentHex = 0xFF66BB6A,
                avatarPiece = PieceType.PAWN
            ),
            BotProfile(
                id = "bot_maya",
                name = "Maya",
                elo = 500,
                tierBadge = "Novice",
                bio = "Spots simple captures and enjoys open games.",
                searchDepth = 1,
                blunderRate = 0.35,
                evalNoiseCp = 170,
                accentHex = 0xFF42A5F5,
                avatarPiece = PieceType.PAWN
            ),
            BotProfile(
                id = "bot_leo",
                name = "Leo",
                elo = 800,
                tierBadge = "Casual",
                bio = "Develops knights early and hunts forks.",
                searchDepth = 2,
                blunderRate = 0.24,
                evalNoiseCp = 120,
                accentHex = 0xFF26C6DA,
                avatarPiece = PieceType.KNIGHT
            ),
            BotProfile(
                id = "bot_clara",
                name = "Clara",
                elo = 1100,
                tierBadge = "Club",
                bio = "Plays principled openings and punishes hanging pieces.",
                searchDepth = 2,
                blunderRate = 0.14,
                evalNoiseCp = 80,
                accentHex = 0xFFAB47BC,
                avatarPiece = PieceType.BISHOP
            ),
            BotProfile(
                id = "bot_viktor",
                name = "Viktor",
                elo = 1400,
                tierBadge = "Intermediate",
                bio = "Sharp tactician who thrives in middlegame battles.",
                searchDepth = 2,
                blunderRate = 0.06,
                evalNoiseCp = 45,
                accentHex = 0xFFFFA726,
                avatarPiece = PieceType.ROOK
            ),
            BotProfile(
                id = "bot_priya",
                name = "Priya",
                elo = 1700,
                tierBadge = "Advanced",
                bio = "Solid positional control with clean endgame technique.",
                searchDepth = 3,
                blunderRate = 0.03,
                evalNoiseCp = 24,
                accentHex = 0xFFEC407A,
                avatarPiece = PieceType.BISHOP
            ),
            BotProfile(
                id = "bot_henrik",
                name = "Henrik",
                elo = 2000,
                tierBadge = "Master",
                bio = "Candidate Master strength with relentless pressure.",
                searchDepth = 3,
                blunderRate = 0.01,
                evalNoiseCp = 12,
                accentHex = 0xFFEF5350,
                avatarPiece = PieceType.QUEEN
            ),
            BotProfile(
                id = "bot_sofia",
                name = "GM Sofia",
                elo = 2250,
                tierBadge = "Grandmaster",
                bio = "Deep calculation and surgical tactical precision.",
                searchDepth = 3,
                blunderRate = 0.0,
                evalNoiseCp = 4,
                accentHex = 0xFFFFCA28,
                avatarPiece = PieceType.QUEEN
            ),
            BotProfile(
                id = "bot_apex",
                name = "Grand Master AI",
                elo = 2450,
                tierBadge = "Super GM",
                bio = "Maximum pure-Kotlin search depth with quiescence.",
                searchDepth = 4,
                blunderRate = 0.0,
                evalNoiseCp = 0,
                accentHex = 0xFF81B64C,
                avatarPiece = PieceType.KING
            )
        )

        fun byId(id: String?): BotProfile = ROSTER.firstOrNull { it.id == id } ?: ROSTER[2]
    }
}

enum class BoardTheme(
    val displayName: String,
    val lightSquareHex: Long,
    val darkSquareHex: Long,
    val highlightLightHex: Long,
    val highlightDarkHex: Long
) {
    CLASSIC_GREEN(
        displayName = "Classic Green",
        lightSquareHex = 0xFFEEEED2,
        darkSquareHex = 0xFF769656,
        highlightLightHex = 0xFFF6F669,
        highlightDarkHex = 0xFFBACA2B
    ),
    WOOD(
        displayName = "Tournament Wood",
        lightSquareHex = 0xFFF0D9B5,
        darkSquareHex = 0xFFB58863,
        highlightLightHex = 0xFFF7EC74,
        highlightDarkHex = 0xFFDAC34B
    ),
    BLUE(
        displayName = "Ocean Blue",
        lightSquareHex = 0xFFDEE3E6,
        darkSquareHex = 0xFF8CA2AD,
        highlightLightHex = 0xFF9BC7E3,
        highlightDarkHex = 0xFF699BC2
    ),
    WALNUT(
        displayName = "Dark Walnut",
        lightSquareHex = 0xFFD9C3A3,
        darkSquareHex = 0xFF7A5638,
        highlightLightHex = 0xFFE8D568,
        highlightDarkHex = 0xFFB79935
    )
}

enum class PieceStyle(val displayName: String, val subtitle: String) {
    CLASSIC("Classic", "Traditional Staunton contours"),
    NEO("Neo", "Clean Chess.com modern silhouettes"),
    WOODBLOCK("Wood", "Warm carved tournament feel"),
    MINIMAL("Minimal", "Crisp high-contrast geometric")
}

data class EngineEvaluation(
    val scoreCp: Int,
    val mateIn: Int? = null,
    val bestMove: Move? = null
) {
    fun formattedScore(): String {
        if (mateIn != null) {
            return if (mateIn > 0) "M$mateIn" else "-M${-mateIn}"
        }
        val pawns = scoreCp / 100.0
        val sign = if (scoreCp > 0) "+" else ""
        return "$sign${String.format(java.util.Locale.US, "%.1f", pawns)}"
    }

    /** Normalized 0f (Black winning) to 1f (White winning) for Eval Bar display. */
    fun whiteFillRatio(): Float {
        if (mateIn != null) {
            return if (mateIn > 0) 1f else 0f
        }
        val clamped = scoreCp.coerceIn(-1200, 1200)
        // Smooth sigmoid-like mapping centered at 0.5
        val ratio = 0.5 + (clamped / 1200.0) * 0.46
        return ratio.toFloat().coerceIn(0.04f, 0.96f)
    }
}

enum class MoveClassification(val label: String, val symbol: String, val colorHex: Long) {
    BEST("Best", "★", 0xFF81B64C),
    GOOD("Good", "✓", 0xFF96BC4B),
    INACCURACY("Inaccuracy", "?!", 0xFFF7C631),
    MISTAKE("Mistake", "?", 0xFFE68A2E),
    BLUNDER("Blunder", "??", 0xFFCA3431)
}

data class AnalyzedMove(
    val plyIndex: Int,
    val san: String,
    val playedMove: Move,
    val bestMove: Move?,
    val bestMoveSan: String?,
    val evalBeforeCp: Int,
    val evalAfterCp: Int,
    val mateAfter: Int?,
    val centipawnLoss: Int,
    val classification: MoveClassification,
    val moverColor: PieceColor
)

data class GameAnalysisResult(
    val moves: List<AnalyzedMove>,
    val whiteAccuracy: Double,
    val blackAccuracy: Double,
    val whiteCounts: Map<MoveClassification, Int>,
    val blackCounts: Map<MoveClassification, Int>
)

data class ChessPuzzle(
    val id: String,
    val title: String,
    val rating: Int,
    val theme: String,
    val fen: String,
    val solutionUci: List<String>,
    val hintText: String
)

enum class PuzzleRushVariant(val label: String, val durationSeconds: Int) {
    THREE_MIN("3 Min", 180),
    FIVE_MIN("5 Min", 300),
    SURVIVAL("Survival", 0)
}
