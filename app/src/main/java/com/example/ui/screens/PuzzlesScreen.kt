package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.chess.data.PuzzleRepository
import com.example.chess.data.UserSettings
import com.example.chess.model.Move
import com.example.chess.model.PieceColor
import com.example.chess.model.PuzzleRushVariant
import com.example.ui.components.BoardArrow
import com.example.ui.components.ChessBoardView
import com.example.ui.theme.ChessArrowHint
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
import com.example.viewmodel.PuzzleStatus
import com.example.viewmodel.PuzzleTabMode
import com.example.viewmodel.PuzzleUiState

@Composable
fun PuzzlesScreen(
    uiState: PuzzleUiState,
    settings: UserSettings,
    solvedPuzzleIds: Set<String>,
    onSelectTabMode: (PuzzleTabMode) -> Unit,
    onPuzzleMove: (Move) -> Unit,
    onHint: () -> Unit,
    onRetry: () -> Unit,
    onNextPuzzle: () -> Unit,
    onSelectRushVariant: (PuzzleRushVariant) -> Unit,
    onStartRush: (PuzzleRushVariant) -> Unit,
    onStopRush: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ChessBgPrimary),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 580.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Puzzles",
                        style = MaterialTheme.typography.headlineMedium,
                        color = ChessCream
                    )
                    Surface(
                        color = ChessBgSecondary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "★ ${settings.puzzleRating}",
                            fontFamily = MonoFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChessGold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.cd_settings),
                        tint = ChessTextSecondary
                    )
                }
            }

            // Rated vs Puzzle Rush mode selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ChessBgSecondary)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PuzzleModeTab(
                    label = "Rated Tactics",
                    selected = uiState.tabMode == PuzzleTabMode.RATED,
                    onClick = { onSelectTabMode(PuzzleTabMode.RATED) },
                    tag = "tab_rated_puzzles",
                    modifier = Modifier.weight(1f)
                )
                PuzzleModeTab(
                    label = "Puzzle Rush",
                    selected = uiState.tabMode == PuzzleTabMode.RUSH,
                    onClick = { onSelectTabMode(PuzzleTabMode.RUSH) },
                    tag = "tab_puzzle_rush",
                    modifier = Modifier.weight(1f)
                )
            }

            if (uiState.tabMode == PuzzleTabMode.RUSH && !uiState.rushActive) {
                PuzzleRushLobbyAndSummary(
                    uiState = uiState,
                    settings = settings,
                    onSelectVariant = onSelectRushVariant,
                    onStartRush = { onStartRush(uiState.rushVariant) }
                )
            } else {
                // Active Board (either Rated Puzzle or active Puzzle Rush)
                if (uiState.tabMode == PuzzleTabMode.RUSH) {
                    RushHudHeader(
                        secondsLeft = uiState.rushSecondsLeft,
                        isTimed = uiState.rushVariant.durationSeconds > 0,
                        score = uiState.rushScore,
                        strikes = uiState.rushStrikes,
                        onStop = onStopRush
                    )
                } else {
                    RatedPuzzleHeader(
                        puzzleTitle = uiState.currentPuzzle.title,
                        theme = uiState.currentPuzzle.theme,
                        rating = uiState.currentPuzzle.rating,
                        streak = uiState.currentStreak,
                        index = uiState.currentPuzzleIndex + 1,
                        total = PuzzleRepository.ALL_PUZZLES.size,
                        alreadySolved = uiState.currentPuzzle.id in solvedPuzzleIds
                    )
                }

                // Side-to-move pill
                val turnColor = uiState.boardState.turn
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(if (turnColor == PieceColor.WHITE) ChessCream else Color(0xFF1A1918))
                                .border(1.dp, ChessSurfaceBorder, CircleShape)
                        )
                        Text(
                            text = if (turnColor == PieceColor.WHITE) "White to Move" else "Black to Move",
                            style = MaterialTheme.typography.titleSmall,
                            color = ChessCream
                        )
                    }
                    if (uiState.ratingDelta != null && uiState.tabMode == PuzzleTabMode.RATED) {
                        val delta = uiState.ratingDelta
                        Text(
                            text = if (delta >= 0) "+$delta" else "$delta",
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = if (delta >= 0) ChessGreenPrimary else ChessBlunderRed
                        )
                    }
                }

                val arrows = remember(uiState.hintArrow) {
                    val hint = uiState.hintArrow
                    if (hint != null) listOf(BoardArrow(hint.first, hint.second, ChessArrowHint)) else emptyList()
                }

                ChessBoardView(
                    boardState = uiState.boardState,
                    boardTheme = settings.boardTheme,
                    pieceStyle = settings.pieceStyle,
                    flipped = uiState.boardFlipped,
                    interactive = uiState.status != PuzzleStatus.SOLVED,
                    showCoordinates = settings.showCoordinates,
                    showLegalMoves = settings.showLegalMoves,
                    arrows = arrows,
                    onMoveSelected = onPuzzleMove,
                    modifier = Modifier.fillMaxWidth()
                )

                // Feedback Callout
                val feedbackColor = when (uiState.status) {
                    PuzzleStatus.SOLVED -> ChessGreenPrimary
                    PuzzleStatus.WRONG_MOVE -> ChessBlunderRed
                    PuzzleStatus.SOLVING -> ChessSurfaceBorder
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(feedbackColor.copy(alpha = 0.18f))
                        .border(1.dp, feedbackColor.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = uiState.feedbackMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ChessCream
                    )
                }

                // Controls for Rated Mode
                if (uiState.tabMode == PuzzleTabMode.RATED) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onHint,
                            enabled = uiState.status != PuzzleStatus.SOLVED,
                            modifier = Modifier.weight(1f).height(48.dp).testTag("btn_puzzle_hint"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = ChessGold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hint", color = ChessCream)
                        }
                        OutlinedButton(
                            onClick = onRetry,
                            modifier = Modifier.weight(1f).height(48.dp).testTag("btn_puzzle_retry"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = ChessTextPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Retry", color = ChessCream)
                        }
                        Button(
                            onClick = onNextPuzzle,
                            modifier = Modifier.weight(1.2f).height(48.dp).testTag("btn_puzzle_next"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ChessGreenPrimary)
                        ) {
                            Text("Next", color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PuzzleModeTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) ChessGreenPrimary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else ChessTextSecondary
        )
    }
}

@Composable
private fun RatedPuzzleHeader(
    puzzleTitle: String,
    theme: String,
    rating: Int,
    streak: Int,
    index: Int,
    total: Int,
    alreadySolved: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ChessBgSecondary)
            .border(1.dp, ChessSurfaceBorder.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "#$index/$total • $puzzleTitle",
                    style = MaterialTheme.typography.titleSmall,
                    color = ChessCream
                )
                if (alreadySolved) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Solved",
                        tint = ChessGreenPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = "$theme • Difficulty $rating",
                style = MaterialTheme.typography.bodySmall,
                color = ChessTextSecondary
            )
        }
        if (streak > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(ChessGold.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = ChessGold, modifier = Modifier.size(16.dp))
                Text(
                    text = "$streak",
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = ChessGold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun RushHudHeader(
    secondsLeft: Int,
    isTimed: Boolean,
    score: Int,
    strikes: Int,
    onStop: () -> Unit
) {
    val timeText = if (isTimed) {
        val m = secondsLeft / 60
        val s = secondsLeft % 60
        "%d:%02d".format(m, s)
    } else "∞"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ChessBgSecondary)
            .border(1.dp, ChessGold.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Bolt, contentDescription = null, tint = ChessGold)
            Text(
                text = timeText,
                fontFamily = MonoFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (isTimed && secondsLeft <= 15) ChessBlunderRed else ChessCream
            )
        }

        Text(
            text = "Score: $score",
            fontFamily = MonoFontFamily,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = ChessGreenPrimary
        )

        // 3 Strikes indicator
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(3) { i ->
                val struck = i < strikes
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (struck) ChessBlunderRed else ChessBgDeep),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = if (struck) Color.White else ChessTextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            IconButton(onClick = onStop, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Stop, contentDescription = "Stop Rush", tint = ChessTextSecondary)
            }
        }
    }
}

@Composable
private fun PuzzleRushLobbyAndSummary(
    uiState: PuzzleUiState,
    settings: UserSettings,
    onSelectVariant: (PuzzleRushVariant) -> Unit,
    onStartRush: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ChessBgSecondary)
            .border(1.dp, ChessSurfaceBorder, RoundedCornerShape(16.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.Bolt, contentDescription = null, tint = ChessGold, modifier = Modifier.size(30.dp))
            Column {
                Text("Puzzle Rush", style = MaterialTheme.typography.headlineMedium, color = ChessCream)
                Text(
                    text = "Solve as many tactics as possible before 3 strikes or time runs out!",
                    style = MaterialTheme.typography.bodySmall,
                    color = ChessTextSecondary
                )
            }
        }

        // Variant Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PuzzleRushVariant.entries.forEach { variant ->
                val selected = variant == uiState.rushVariant
                val best = when (variant) {
                    PuzzleRushVariant.THREE_MIN -> settings.rushBest3Min
                    PuzzleRushVariant.FIVE_MIN -> settings.rushBest5Min
                    PuzzleRushVariant.SURVIVAL -> settings.rushBestSurvival
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) ChessGold.copy(alpha = 0.2f) else ChessSurfaceElevated)
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) ChessGold else ChessSurfaceBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectVariant(variant) }
                        .padding(vertical = 12.dp, horizontal = 6.dp)
                        .testTag("rush_variant_${variant.name.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = variant.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (selected) ChessGold else ChessCream
                    )
                    Text(
                        text = "Best: $best",
                        fontFamily = MonoFontFamily,
                        fontSize = 11.sp,
                        color = ChessTextSecondary
                    )
                }
            }
        }

        Button(
            onClick = onStartRush,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("btn_start_rush"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ChessGreenPrimary)
        ) {
            Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Start ${uiState.rushVariant.label} Rush",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        if (uiState.rushFinished) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ChessBgDeep)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Last Run Result: ${uiState.rushScore} Solved",
                    style = MaterialTheme.typography.titleMedium,
                    color = ChessGold
                )
                uiState.rushHistory.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${item.puzzle.title} (${item.puzzle.rating})",
                            style = MaterialTheme.typography.bodySmall,
                            color = ChessTextPrimary
                        )
                        Icon(
                            imageVector = if (item.solved) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (item.solved) ChessGreenPrimary else ChessBlunderRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
