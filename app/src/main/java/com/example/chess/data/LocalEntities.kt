package com.example.chess.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_games")
data class SavedGameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playedAtMillis: Long = System.currentTimeMillis(),
    val mode: String, // "VS_BOT" or "PASS_AND_PLAY"
    val whiteName: String,
    val blackName: String,
    val botId: String? = null,
    val botElo: Int? = null,
    val userColor: String? = null, // "WHITE", "BLACK", or null for Pass & Play
    val result: String, // "1-0", "0-1", "1/2-1/2"
    val userOutcome: String, // "WIN", "LOSS", "DRAW", "LOCAL"
    val termination: String,
    val timeControlLabel: String,
    val pgn: String,
    val finalFen: String,
    val moveCount: Int,
    val whiteAccuracy: Double? = null,
    val blackAccuracy: Double? = null
)

@Entity(tableName = "solved_puzzles")
data class SolvedPuzzleEntity(
    @PrimaryKey val puzzleId: String,
    val solvedAtMillis: Long = System.currentTimeMillis(),
    val attempts: Int = 1
)
