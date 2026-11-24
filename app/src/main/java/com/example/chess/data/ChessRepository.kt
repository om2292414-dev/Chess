package com.example.chess.data

import com.example.chess.model.BoardTheme
import com.example.chess.model.PieceStyle
import com.example.chess.model.PuzzleRushVariant
import kotlinx.coroutines.flow.Flow

class ChessRepository(
    private val dao: ChessDao,
    private val settingsStore: SettingsStore
) {
    val allGames: Flow<List<SavedGameEntity>> = dao.observeAllGames()
    val recentGames: Flow<List<SavedGameEntity>> = dao.observeRecentGames(5)
    val defeatedBotIds: Flow<List<String>> = dao.observeDefeatedBotIds()
    val solvedPuzzles: Flow<List<SolvedPuzzleEntity>> = dao.observeSolvedPuzzles()
    val settings: Flow<UserSettings> = settingsStore.settingsFlow

    suspend fun saveGame(entity: SavedGameEntity): Long = dao.insertGame(entity)

    suspend fun updateGameAccuracy(gameId: Long, whiteAcc: Double, blackAcc: Double) =
        dao.updateGameAccuracy(gameId, whiteAcc, blackAcc)

    suspend fun deleteGame(gameId: Long) = dao.deleteGame(gameId)

    suspend fun markPuzzleSolved(puzzleId: String, attempts: Int) {
        dao.markPuzzleSolved(SolvedPuzzleEntity(puzzleId = puzzleId, attempts = attempts))
    }

    suspend fun setBoardTheme(theme: BoardTheme) = settingsStore.setBoardTheme(theme)
    suspend fun setPieceStyle(style: PieceStyle) = settingsStore.setPieceStyle(style)
    suspend fun setSoundEnabled(enabled: Boolean) = settingsStore.setSoundEnabled(enabled)
    suspend fun setShowCoordinates(show: Boolean) = settingsStore.setShowCoordinates(show)
    suspend fun setShowLegalMoves(show: Boolean) = settingsStore.setShowLegalMoves(show)
    suspend fun setShowLiveEvalBar(show: Boolean) = settingsStore.setShowLiveEvalBar(show)
    suspend fun updatePuzzleRating(newRating: Int, currentStreak: Int) =
        settingsStore.updatePuzzleRating(newRating, currentStreak)
    suspend fun recordPuzzleRushScore(variant: PuzzleRushVariant, score: Int) =
        settingsStore.recordPuzzleRushScore(variant, score)
}
