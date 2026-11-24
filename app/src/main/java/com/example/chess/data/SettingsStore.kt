package com.example.chess.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.chess.model.BoardTheme
import com.example.chess.model.PieceStyle
import com.example.chess.model.PuzzleRushVariant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.chessDataStore: DataStore<Preferences> by preferencesDataStore(name = "chess_prefs")

data class UserSettings(
    val boardTheme: BoardTheme = BoardTheme.CLASSIC_GREEN,
    val pieceStyle: PieceStyle = PieceStyle.NEO,
    val soundEnabled: Boolean = true,
    val showCoordinates: Boolean = true,
    val showLegalMoves: Boolean = true,
    val showLiveEvalBar: Boolean = true,
    val puzzleRating: Int = 800,
    val puzzleStreakBest: Int = 0,
    val rushBest3Min: Int = 0,
    val rushBest5Min: Int = 0,
    val rushBestSurvival: Int = 0
)

class SettingsStore(private val context: Context) {

    private object Keys {
        val BOARD_THEME = stringPreferencesKey("board_theme")
        val PIECE_STYLE = stringPreferencesKey("piece_style")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val SHOW_COORDINATES = booleanPreferencesKey("show_coordinates")
        val SHOW_LEGAL_MOVES = booleanPreferencesKey("show_legal_moves")
        val SHOW_LIVE_EVAL = booleanPreferencesKey("show_live_eval")
        val PUZZLE_RATING = intPreferencesKey("puzzle_rating")
        val PUZZLE_STREAK_BEST = intPreferencesKey("puzzle_streak_best")
        val RUSH_BEST_3MIN = intPreferencesKey("rush_best_3min")
        val RUSH_BEST_5MIN = intPreferencesKey("rush_best_5min")
        val RUSH_BEST_SURVIVAL = intPreferencesKey("rush_best_survival")
    }

    val settingsFlow: Flow<UserSettings> = context.chessDataStore.data.map { prefs ->
        UserSettings(
            boardTheme = prefs[Keys.BOARD_THEME]?.let { name ->
                BoardTheme.entries.firstOrNull { it.name == name }
            } ?: BoardTheme.CLASSIC_GREEN,
            pieceStyle = prefs[Keys.PIECE_STYLE]?.let { name ->
                PieceStyle.entries.firstOrNull { it.name == name }
            } ?: PieceStyle.NEO,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            showCoordinates = prefs[Keys.SHOW_COORDINATES] ?: true,
            showLegalMoves = prefs[Keys.SHOW_LEGAL_MOVES] ?: true,
            showLiveEvalBar = prefs[Keys.SHOW_LIVE_EVAL] ?: true,
            puzzleRating = prefs[Keys.PUZZLE_RATING] ?: 800,
            puzzleStreakBest = prefs[Keys.PUZZLE_STREAK_BEST] ?: 0,
            rushBest3Min = prefs[Keys.RUSH_BEST_3MIN] ?: 0,
            rushBest5Min = prefs[Keys.RUSH_BEST_5MIN] ?: 0,
            rushBestSurvival = prefs[Keys.RUSH_BEST_SURVIVAL] ?: 0
        )
    }

    suspend fun setBoardTheme(theme: BoardTheme) {
        context.chessDataStore.edit { it[Keys.BOARD_THEME] = theme.name }
    }

    suspend fun setPieceStyle(style: PieceStyle) {
        context.chessDataStore.edit { it[Keys.PIECE_STYLE] = style.name }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.chessDataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setShowCoordinates(show: Boolean) {
        context.chessDataStore.edit { it[Keys.SHOW_COORDINATES] = show }
    }

    suspend fun setShowLegalMoves(show: Boolean) {
        context.chessDataStore.edit { it[Keys.SHOW_LEGAL_MOVES] = show }
    }

    suspend fun setShowLiveEvalBar(show: Boolean) {
        context.chessDataStore.edit { it[Keys.SHOW_LIVE_EVAL] = show }
    }

    suspend fun updatePuzzleRating(newRating: Int, currentStreak: Int) {
        context.chessDataStore.edit { prefs ->
            prefs[Keys.PUZZLE_RATING] = newRating.coerceIn(400, 3000)
            val prevBest = prefs[Keys.PUZZLE_STREAK_BEST] ?: 0
            if (currentStreak > prevBest) {
                prefs[Keys.PUZZLE_STREAK_BEST] = currentStreak
            }
        }
    }

    suspend fun recordPuzzleRushScore(variant: PuzzleRushVariant, score: Int) {
        context.chessDataStore.edit { prefs ->
            val key = when (variant) {
                PuzzleRushVariant.THREE_MIN -> Keys.RUSH_BEST_3MIN
                PuzzleRushVariant.FIVE_MIN -> Keys.RUSH_BEST_5MIN
                PuzzleRushVariant.SURVIVAL -> Keys.RUSH_BEST_SURVIVAL
            }
            val current = prefs[key] ?: 0
            if (score > current) {
                prefs[key] = score
            }
        }
    }
}
