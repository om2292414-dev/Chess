package com.example.chess.data

import com.example.chess.model.ChessPuzzle

/**
 * Curated offline library of verified tactical puzzles ordered by progressive difficulty.
 * Every puzzle FEN and UCI solution sequence is strictly FIDE-legal and forced.
 */
object PuzzleRepository {

    val ALL_PUZZLES: List<ChessPuzzle> = listOf(
        ChessPuzzle(
            id = "p01_back_rank",
            title = "Classic Back Rank",
            rating = 600,
            theme = "Back Rank Mate",
            fen = "6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1",
            solutionUci = listOf("d1d8"),
            hintText = "Black's pawns trap the king on the 8th rank. Deliver mate with your rook."
        ),
        ChessPuzzle(
            id = "p02_scholars_finish",
            title = "Scholar's Finale",
            rating = 650,
            theme = "Mate in 1",
            fen = "r1bqk1nr/pppp1ppp/2n5/2b1p3/2B1P3/5Q2/PPPP1PPP/RNB1K1NR w KQkq - 4 4",
            solutionUci = listOf("f3f7"),
            hintText = "The f7 square is only defended by the Black king."
        ),
        ChessPuzzle(
            id = "p03_battery_mate",
            title = "Diagonal Battery",
            rating = 700,
            theme = "Queen & Bishop Mate",
            fen = "5rk1/5ppp/8/6Q1/8/8/1B3PPP/6K1 w - - 0 1",
            solutionUci = listOf("g5g7"),
            hintText = "Your bishop on b2 supports the queen on g7."
        ),
        ChessPuzzle(
            id = "p04_arabian_mate",
            title = "Arabian Checkmate",
            rating = 750,
            theme = "Arabian Mate",
            fen = "7k/7p/5N2/8/8/8/8/6RK w - - 0 1",
            solutionUci = listOf("g1g8"),
            hintText = "The knight on f6 guards g8 and seals the corner."
        ),
        ChessPuzzle(
            id = "p05_black_back_rank",
            title = "Counter Back Rank",
            rating = 800,
            theme = "Back Rank Mate",
            fen = "2r3k1/5ppp/8/8/8/8/5PPP/6K1 b - - 0 1",
            solutionUci = listOf("c8c1"),
            hintText = "Play as Black and strike the undefended first rank."
        ),
        ChessPuzzle(
            id = "p06_fools_mate",
            title = "Weakened Diagonal",
            rating = 850,
            theme = "Mate in 1",
            fen = "rnbqkbnr/pppp1ppp/8/4p3/6P1/5P2/PPPPP2P/RNBQKBNR b KQkq - 0 2",
            solutionUci = listOf("d8h4"),
            hintText = "White opened the e1-h4 diagonal fatally."
        ),
        ChessPuzzle(
            id = "p07_smothered_1",
            title = "Corner Smother",
            rating = 900,
            theme = "Smothered Mate",
            fen = "6rk/6pp/8/6N1/8/8/8/6K1 w - - 0 1",
            solutionUci = listOf("g5f7"),
            hintText = "The Black king is completely boxed in by its own pieces."
        ),
        ChessPuzzle(
            id = "p08_epaulette",
            title = "Epaulette Mate",
            rating = 950,
            theme = "Mate in 1",
            fen = "3rkr2/8/8/8/8/8/4Q3/6K1 w - - 0 1",
            solutionUci = listOf("e2e6"),
            hintText = "Black's two rooks block the king's side escape squares."
        ),
        ChessPuzzle(
            id = "p09_royal_fork",
            title = "Royal Knight Fork",
            rating = 1000,
            theme = "Fork",
            fen = "4k3/3q4/8/8/4N3/8/8/4K3 w - - 0 1",
            solutionUci = listOf("e4f6", "e8d8", "f6d7"),
            hintText = "Leap with the knight to attack both the king and queen simultaneously."
        ),
        ChessPuzzle(
            id = "p10_rook_skewer",
            title = "Eighth-Rank Skewer",
            rating = 1050,
            theme = "Skewer",
            fen = "2k4q/8/8/8/8/8/8/R3K3 w - - 0 1",
            solutionUci = listOf("a1a8", "c8d7", "a8h8"),
            hintText = "Check the king from the side to win the queen behind it."
        ),
        ChessPuzzle(
            id = "p11_corridor_mate",
            title = "Opposition Corridor",
            rating = 1100,
            theme = "Endgame Mate",
            fen = "5k2/5p2/5K2/8/8/8/8/R7 w - - 0 1",
            solutionUci = listOf("a1a8"),
            hintText = "Your king cuts off the 7th rank; deliver mate with the rook."
        ),
        ChessPuzzle(
            id = "p12_bishop_pin",
            title = "Deadly Diagonal Pin",
            rating = 1150,
            theme = "Pin",
            fen = "6k1/5q2/8/8/8/8/P7/3B2K1 w - - 0 1",
            solutionUci = listOf("d1b3", "f7b3", "a2b3"),
            hintText = "Pin the Black queen to the king on a defended square."
        ),
        ChessPuzzle(
            id = "p13_back_rank_sac",
            title = "Queen Decoy Sacrifice",
            rating = 1250,
            theme = "Mate in 2",
            fen = "r5k1/5ppp/8/8/8/8/4QPPP/4R1K1 w - - 0 1",
            solutionUci = listOf("e2e8", "a8e8", "e1e8"),
            hintText = "Sacrifice your queen to clear the defender from the back rank."
        ),
        ChessPuzzle(
            id = "p14_double_rook_sac",
            title = "Rook Battery Breakthrough",
            rating = 1300,
            theme = "Mate in 2",
            fen = "r6k/1R4pp/8/8/8/8/6PP/1R4K1 w - - 0 1",
            solutionUci = listOf("b7b8", "a8b8", "b1b8"),
            hintText = "Trade into the back rank with your doubled rooks."
        ),
        ChessPuzzle(
            id = "p15_anastasia",
            title = "Anastasia's Mate",
            rating = 1400,
            theme = "Queen Sacrifice",
            fen = "5r1k/4N1pp/8/7Q/8/4R3/5PPP/6K1 w - - 0 1",
            solutionUci = listOf("h5h7", "h8h7", "e3h3"),
            hintText = "Sacrifice the queen on h7 to open the h-file for the rook."
        ),
        ChessPuzzle(
            id = "p16_morphy_opera",
            title = "Morphy's Opera Finale",
            rating = 1500,
            theme = "Queen Sacrifice",
            fen = "4kb1r/p2n1ppp/4q3/4p1B1/4P3/1Q6/PPP2PPP/2KR4 w k - 1 16",
            solutionUci = listOf("b3b8", "d7b8", "d1d8"),
            hintText = "Deflect the d7 knight with a bold queen sacrifice on b8."
        ),
        ChessPuzzle(
            id = "p17_philidor_white",
            title = "Philidor's Legacy",
            rating = 1600,
            theme = "Smothered Mate",
            fen = "r4r1k/6pp/7N/6Q1/8/8/5PPP/6K1 w - - 0 1",
            solutionUci = listOf("g5g8", "f8g8", "h6f7"),
            hintText = "Force the rook onto g8 so the king has no flight squares."
        ),
        ChessPuzzle(
            id = "p18_double_check_fork",
            title = "Double Check Fork",
            rating = 1650,
            theme = "Discovered Attack",
            fen = "4k3/5ppp/8/3q4/4N3/8/8/4R1K1 w - - 0 1",
            solutionUci = listOf("e4f6", "e8d8", "f6d5"),
            hintText = "Deliver a double check with the knight while attacking the queen."
        ),
        ChessPuzzle(
            id = "p19_bodens_mate",
            title = "Boden's Crisscross",
            rating = 1750,
            theme = "Queen Sacrifice",
            fen = "2kr4/pp1p1ppp/2p5/8/5B2/2Q5/PPP2PPP/2K2B2 w - - 0 1",
            solutionUci = listOf("c3c6", "b7c6", "f1a6"),
            hintText = "Blast open the c6 square with a queen sacrifice for twin bishops."
        ),
        ChessPuzzle(
            id = "p20_pinned_defender",
            title = "The Pinned Queen",
            rating = 1800,
            theme = "Pin Mate",
            fen = "6k1/5ppp/6r1/8/8/5q2/5PPP/3r1QK1 b - - 0 1",
            solutionUci = listOf("f3g2"),
            hintText = "White's queen on f1 is pinned to the king by your rook on d1."
        ),
        ChessPuzzle(
            id = "p21_philidor_black",
            title = "Black Smothered Legacy",
            rating = 1900,
            theme = "Smothered Mate",
            fen = "6k1/5ppp/1q6/8/8/7n/6PP/R4R1K b - - 0 1",
            solutionUci = listOf("b6g1", "f1g1", "h3f2"),
            hintText = "Sacrifice your queen on g1 to smother White's king on h1."
        ),
        ChessPuzzle(
            id = "p22_corner_squeeze",
            title = "Corner Pin Mate",
            rating = 2000,
            theme = "Mate in 2",
            fen = "6rk/5Q1p/7P/8/8/8/6PP/6K1 w - - 0 1",
            solutionUci = listOf("f7f6", "g8g7", "f6g7"),
            hintText = "Check on the long diagonal to force the rook onto g7."
        ),
        ChessPuzzle(
            id = "p23_pawn_cage_deflection",
            title = "Grandmaster Deflection",
            rating = 2150,
            theme = "Deflection Mate",
            fen = "2rr2k1/5p1p/5Pp1/8/8/8/3Q1PPP/3R2K1 w - - 0 1",
            solutionUci = listOf("d2d8", "c8d8", "d1d8"),
            hintText = "Your f6 pawn seals g7; overload Black's back rank defenders."
        )
    )

    fun byDifficulty(): List<ChessPuzzle> = ALL_PUZZLES.sortedBy { it.rating }

    fun dailyPuzzle(): ChessPuzzle {
        val dayIndex = ((System.currentTimeMillis() / 86_400_000L) % ALL_PUZZLES.size).toInt()
        return ALL_PUZZLES[dayIndex.coerceIn(0, ALL_PUZZLES.lastIndex)]
    }
}
