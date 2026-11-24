package com.example.chess.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChessDao {
    @Query("SELECT * FROM saved_games ORDER BY playedAtMillis DESC")
    fun observeAllGames(): Flow<List<SavedGameEntity>>

    @Query("SELECT * FROM saved_games ORDER BY playedAtMillis DESC LIMIT :limit")
    fun observeRecentGames(limit: Int): Flow<List<SavedGameEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: SavedGameEntity): Long

    @Query("UPDATE saved_games SET whiteAccuracy = :whiteAcc, blackAccuracy = :blackAcc WHERE id = :gameId")
    suspend fun updateGameAccuracy(gameId: Long, whiteAcc: Double, blackAcc: Double)

    @Query("DELETE FROM saved_games WHERE id = :gameId")
    suspend fun deleteGame(gameId: Long)

    @Query("SELECT DISTINCT botId FROM saved_games WHERE botId IS NOT NULL AND userOutcome = 'WIN'")
    fun observeDefeatedBotIds(): Flow<List<String>>

    @Query("SELECT * FROM solved_puzzles ORDER BY solvedAtMillis DESC")
    fun observeSolvedPuzzles(): Flow<List<SolvedPuzzleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markPuzzleSolved(puzzle: SolvedPuzzleEntity)
}
