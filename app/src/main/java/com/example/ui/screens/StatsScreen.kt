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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.chess.data.PuzzleRepository
import com.example.chess.data.SavedGameEntity
import com.example.chess.data.UserSettings
import com.example.chess.model.BotProfile
import com.example.chess.model.Piece
import com.example.chess.model.PieceColor
import com.example.ui.components.ChessPieceIcon
import com.example.ui.theme.ChessBgDeep
import com.example.ui.theme.ChessBgPrimary
import com.example.ui.theme.ChessBgSecondary
import com.example.ui.theme.ChessBlunderRed
import com.example.ui.theme.ChessCream
import com.example.ui.theme.ChessGold
import com.example.ui.theme.ChessGreenPrimary
import com.example.ui.theme.ChessSurfaceBorder
import com.example.ui.theme.ChessSurfaceElevated
import com.example.ui.theme.ChessTextMuted
import com.example.ui.theme.ChessTextPrimary
import com.example.ui.theme.ChessTextSecondary
import com.example.ui.theme.MonoFontFamily

@Composable
fun StatsScreen(
    settings: UserSettings,
    allGames: List<SavedGameEntity>,
    defeatedBotIds: Set<String>,
    solvedPuzzlesCount: Int,
    onChallengeBot: (BotProfile) -> Unit,
    onOpenGameInAnalysis: (SavedGameEntity) -> Unit,
    onDeleteGame: (Long) -> Unit,
    onOpenSettings: () -> Unit
) {
    val botGames = allGames.filter { it.mode == "VS_BOT" }
    val wins = botGames.count { it.userOutcome == "WIN" }
    val draws = botGames.count { it.userOutcome == "DRAW" }
    val losses = botGames.count { it.userOutcome == "LOSS" }
    val totalBotGames = (wins + draws + losses).coerceAtLeast(1)

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
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Career Stats",
                        style = MaterialTheme.typography.headlineLarge,
                        color = ChessCream
                    )
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.cd_settings),
                            tint = ChessTextSecondary
                        )
                    }
                }
            }

            // Win / Loss / Draw Record Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ChessBgSecondary)
                        .border(1.dp, ChessSurfaceBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Bot Match Record", style = MaterialTheme.typography.titleMedium, color = ChessCream)
                        Text(
                            text = "${botGames.size} games",
                            fontFamily = MonoFontFamily,
                            fontSize = 12.sp,
                            color = ChessTextSecondary
                        )
                    }

                    // Proportional W/D/L Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(ChessBgDeep)
                    ) {
                        if (botGames.isNotEmpty()) {
                            if (wins > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(wins.toFloat() / totalBotGames)
                                        .background(ChessGreenPrimary)
                                )
                            }
                            if (draws > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(draws.toFloat() / totalBotGames)
                                        .background(ChessTextSecondary)
                                )
                            }
                            if (losses > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(losses.toFloat() / totalBotGames)
                                        .background(ChessBlunderRed)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        RecordMetric("Wins", wins, ChessGreenPrimary)
                        RecordMetric("Draws", draws, ChessTextSecondary)
                        RecordMetric("Losses", losses, ChessBlunderRed)
                    }
                }
            }

            // Puzzle High Scores Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ChessBgSecondary)
                        .border(1.dp, ChessSurfaceBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = ChessGold)
                        Text("Puzzle Mastery", style = MaterialTheme.typography.titleMedium, color = ChessCream)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatMiniCard("Rating", "${settings.puzzleRating}", ChessGold, Modifier.weight(1f))
                        StatMiniCard("Solved", "$solvedPuzzlesCount/${PuzzleRepository.ALL_PUZZLES.size}", ChessGreenPrimary, Modifier.weight(1f))
                        StatMiniCard("Best Streak", "${settings.puzzleStreakBest}", ChessCream, Modifier.weight(1f))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatMiniCard("Rush 3m", "${settings.rushBest3Min}", ChessGold, Modifier.weight(1f))
                        StatMiniCard("Rush 5m", "${settings.rushBest5Min}", ChessGold, Modifier.weight(1f))
                        StatMiniCard("Survival", "${settings.rushBestSurvival}", ChessGold, Modifier.weight(1f))
                    }
                }
            }

            // Bots Defeated Trophy Case
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ChessBgSecondary)
                        .border(1.dp, ChessSurfaceBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Bots Defeated", style = MaterialTheme.typography.titleMedium, color = ChessCream)
                        Text(
                            text = "${defeatedBotIds.size} / ${BotProfile.ROSTER.size}",
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = ChessGreenPrimary
                        )
                    }

                    BotProfile.ROSTER.chunked(3).forEach { rowBots ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowBots.forEach { bot ->
                                val beaten = bot.id in defeatedBotIds
                                val accent = Color(bot.accentHex)
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (beaten) ChessGreenPrimary.copy(alpha = 0.14f) else ChessBgDeep)
                                        .border(
                                            1.dp,
                                            if (beaten) ChessGreenPrimary.copy(alpha = 0.7f) else ChessSurfaceBorder.copy(alpha = 0.5f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { onChallengeBot(bot) }
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(contentAlignment = Alignment.TopEnd) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(accent.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            ChessPieceIcon(
                                                piece = Piece(bot.avatarPiece, PieceColor.WHITE),
                                                style = settings.pieceStyle,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        if (beaten) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Defeated",
                                                tint = ChessGreenPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = bot.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ChessTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${bot.elo}",
                                        fontFamily = MonoFontFamily,
                                        fontSize = 10.sp,
                                        color = if (beaten) ChessGreenPrimary else ChessTextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Saved Game Archive
            item {
                Text(
                    text = "Saved Game Archive (${allGames.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = ChessCream
                )
            }

            if (allGames.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ChessBgSecondary)
                            .padding(22.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Completed matches are automatically archived locally here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ChessTextMuted
                        )
                    }
                }
            } else {
                items(allGames, key = { it.id }) { game ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RecentGameRow(
                            game = game,
                            onClick = { onOpenGameInAnalysis(game) },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onDeleteGame(game.id) }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete saved game",
                                tint = ChessTextMuted
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun RecordMetric(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            fontFamily = MonoFontFamily,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ChessTextSecondary
        )
    }
}

@Composable
private fun StatMiniCard(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ChessSurfaceElevated)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontFamily = MonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = accent
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ChessTextSecondary,
            maxLines = 1
        )
    }
}
