package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.chess.data.PuzzleRepository
import com.example.chess.data.SavedGameEntity
import com.example.chess.data.UserSettings
import com.example.chess.engine.ChessEngine
import com.example.chess.model.BotProfile
import com.example.ui.components.ChessBoardView
import com.example.ui.components.GrandMasterEmblem
import com.example.ui.theme.ChessBgDeep
import com.example.ui.theme.ChessBgPrimary
import com.example.ui.theme.ChessBgSecondary
import com.example.ui.theme.ChessBlunderRed
import com.example.ui.theme.ChessCream
import com.example.ui.theme.ChessGold
import com.example.ui.theme.ChessGreenPrimary
import com.example.ui.theme.ChessGreenShadow
import com.example.ui.theme.ChessSurfaceBorder
import com.example.ui.theme.ChessSurfaceElevated
import com.example.ui.theme.ChessTextMuted
import com.example.ui.theme.ChessTextPrimary
import com.example.ui.theme.ChessTextSecondary
import com.example.ui.theme.MonoFontFamily

@Composable
fun HomeScreen(
    settings: UserSettings,
    recentGames: List<SavedGameEntity>,
    defeatedBotsCount: Int,
    solvedPuzzlesCount: Int,
    onPlayVsBots: () -> Unit,
    onPlayPassAndPlay: () -> Unit,
    onOpenDailyPuzzle: () -> Unit,
    onOpenPuzzleRush: () -> Unit,
    onOpenAnalysis: () -> Unit,
    onOpenGameInAnalysis: (SavedGameEntity) -> Unit,
    onOpenSettings: () -> Unit
) {
    val dailyPuzzle = remember { PuzzleRepository.dailyPuzzle() }
    val dailyBoardState = remember(dailyPuzzle.fen) {
        ChessEngine.fromFen(dailyPuzzle.fen) ?: ChessEngine.initialState()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ChessBgPrimary),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Brand Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GrandMasterEmblem(modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "GRAND MASTER CHESS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp,
                                color = ChessCream
                            )
                            Text(
                                text = "100% Offline • Pure Kotlin Engine",
                                style = MaterialTheme.typography.labelSmall,
                                color = ChessTextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("home_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.cd_settings),
                            tint = ChessTextPrimary
                        )
                    }
                }
            }

            // Quick Stats Strip
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HomeStatChip(
                        label = "Puzzle Rating",
                        value = "${settings.puzzleRating}",
                        accent = ChessGold,
                        modifier = Modifier.weight(1f)
                    )
                    HomeStatChip(
                        label = "Bots Beaten",
                        value = "$defeatedBotsCount/${BotProfile.ROSTER.size}",
                        accent = ChessGreenPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    HomeStatChip(
                        label = "Puzzles",
                        value = "$solvedPuzzlesCount/${PuzzleRepository.ALL_PUZZLES.size}",
                        accent = ChessCream,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Hero Play Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(ChessBgSecondary, ChessSurfaceElevated)
                            )
                        )
                        .border(1.dp, ChessSurfaceBorder, RoundedCornerShape(16.dp))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Ready to Play?",
                            style = MaterialTheme.typography.headlineMedium,
                            color = ChessCream
                        )
                        Text(
                            text = "Challenge 9 offline AI personalities from 250 to 2450 ELO, or play locally with a friend.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ChessTextSecondary
                        )

                        Button(
                            onClick = onPlayVsBots,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("home_play_bots_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ChessGreenPrimary,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Icon(Icons.Default.SmartToy, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Play vs Bots",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onPlayPassAndPlay,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("home_pass_play_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ChessSurfaceBorder)
                            ) {
                                Icon(Icons.Default.Group, contentDescription = null, tint = ChessCream, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pass & Play", color = ChessCream, maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = onOpenAnalysis,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("home_analysis_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ChessSurfaceBorder)
                            ) {
                                Icon(Icons.Default.Insights, contentDescription = null, tint = ChessGreenPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Analysis", color = ChessCream, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // Daily Puzzle & Puzzle Rush Cards
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ChessBgSecondary)
                        .border(1.dp, ChessSurfaceBorder, RoundedCornerShape(16.dp))
                        .clickable { onOpenDailyPuzzle() }
                        .padding(14.dp)
                        .testTag("home_daily_puzzle_card"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(96.dp)) {
                        ChessBoardView(
                            boardState = dailyBoardState,
                            boardTheme = settings.boardTheme,
                            pieceStyle = settings.pieceStyle,
                            flipped = dailyBoardState.turn == com.example.chess.model.PieceColor.BLACK,
                            interactive = false,
                            showCoordinates = false,
                            showLegalMoves = false,
                            onMoveSelected = {}
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Extension,
                                contentDescription = null,
                                tint = ChessGreenPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "DAILY TACTIC",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ChessGreenPrimary,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = dailyPuzzle.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = ChessTextPrimary
                        )
                        Text(
                            text = "${dailyPuzzle.theme} • ${dailyPuzzle.rating} ELO",
                            style = MaterialTheme.typography.bodySmall,
                            color = ChessTextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Solve Puzzle →",
                            style = MaterialTheme.typography.labelLarge,
                            color = ChessGold
                        )
                    }
                }
            }

            // Puzzle Rush Banner
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF3A2B1D), ChessBgSecondary)
                            )
                        )
                        .border(1.dp, ChessGold.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .clickable { onOpenPuzzleRush() }
                        .padding(14.dp)
                        .testTag("home_puzzle_rush_card"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ChessGold.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = ChessGold,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Puzzle Rush",
                            style = MaterialTheme.typography.titleMedium,
                            color = ChessCream
                        )
                        Text(
                            text = "3-min sprint or Survival • Best 3m: ${settings.rushBest3Min}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ChessTextSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = ChessGold
                    )
                }
            }

            // Recent Games Section
            item {
                Text(
                    text = "Recent Games",
                    style = MaterialTheme.typography.titleLarge,
                    color = ChessCream,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (recentGames.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ChessBgSecondary)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No completed games yet. Finish a match to save and review it here!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ChessTextMuted
                        )
                    }
                }
            } else {
                items(recentGames, key = { it.id }) { game ->
                    RecentGameRow(
                        game = game,
                        onClick = { onOpenGameInAnalysis(game) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun HomeStatChip(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ChessBgSecondary)
            .border(1.dp, ChessSurfaceBorder.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontFamily = MonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = accent
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ChessTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun RecentGameRow(
    game: SavedGameEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val badgeColor = when (game.userOutcome) {
        "WIN" -> ChessGreenPrimary
        "LOSS" -> ChessBlunderRed
        "DRAW" -> ChessTextSecondary
        else -> ChessGold
    }
    val badgeText = when (game.userOutcome) {
        "WIN" -> "W"
        "LOSS" -> "L"
        "DRAW" -> "D"
        else -> "2P"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ChessBgSecondary)
            .border(1.dp, ChessSurfaceBorder.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(badgeColor.copy(alpha = 0.22f))
                .border(1.dp, badgeColor.copy(alpha = 0.7f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badgeText,
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = badgeColor
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${game.whiteName} vs ${game.blackName}",
                style = MaterialTheme.typography.titleSmall,
                color = ChessTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${game.result} • ${game.termination} • ${game.timeControlLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = ChessTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (game.whiteAccuracy != null) {
            Surface(
                color = ChessBgDeep,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "${game.whiteAccuracy.toInt()}%",
                    fontFamily = MonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChessGreenShadow.copy(alpha = 1f),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = ChessTextMuted
        )
    }
}
