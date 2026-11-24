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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.chess.data.UserSettings
import com.example.chess.engine.ChessEngine
import com.example.chess.model.BotProfile
import com.example.chess.model.GameMode
import com.example.chess.model.Move
import com.example.chess.model.Piece
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import com.example.chess.model.SidePreference
import com.example.chess.model.TimeControl
import com.example.chess.model.TimeControlCategory
import com.example.ui.components.BoardArrow
import com.example.ui.components.ChessBoardView
import com.example.ui.components.ChessPieceIcon
import com.example.ui.components.EvaluationBar
import com.example.ui.components.MoveHistoryStrip
import com.example.ui.components.PlayerInfoBar
import com.example.ui.theme.ChessArrowHint
import com.example.ui.theme.ChessBgDeep
import com.example.ui.theme.ChessBgPrimary
import com.example.ui.theme.ChessBgSecondary
import com.example.ui.theme.ChessCream
import com.example.ui.theme.ChessGold
import com.example.ui.theme.ChessGreenPrimary
import com.example.ui.theme.ChessSurfaceBorder
import com.example.ui.theme.ChessSurfaceElevated
import com.example.ui.theme.ChessTextMuted
import com.example.ui.theme.ChessTextPrimary
import com.example.ui.theme.ChessTextSecondary
import com.example.ui.theme.MonoFontFamily
import com.example.viewmodel.GameUiState

@Composable
fun PlayScreen(
    uiState: GameUiState,
    settings: UserSettings,
    defeatedBotIds: Set<String>,
    onSelectMode: (GameMode) -> Unit,
    onSelectBot: (BotProfile) -> Unit,
    onSelectSide: (SidePreference) -> Unit,
    onSelectTimeControl: (TimeControl) -> Unit,
    onStartGame: () -> Unit,
    onUserMove: (Move) -> Unit,
    onFlipBoard: () -> Unit,
    onHint: () -> Unit,
    onTakeback: () -> Unit,
    onDraw: () -> Unit,
    onResign: () -> Unit,
    onNewGameLobby: () -> Unit,
    onSelectViewingPly: (Int) -> Unit,
    onDismissGameOver: () -> Unit,
    onReviewCurrentGame: () -> Unit,
    onOpenSettings: () -> Unit
) {
    if (!uiState.inActiveMatch) {
        GameSetupLobby(
            uiState = uiState,
            settings = settings,
            defeatedBotIds = defeatedBotIds,
            onSelectMode = onSelectMode,
            onSelectBot = onSelectBot,
            onSelectSide = onSelectSide,
            onSelectTimeControl = onSelectTimeControl,
            onStartGame = onStartGame,
            onOpenSettings = onOpenSettings
        )
    } else {
        ActiveMatchView(
            uiState = uiState,
            settings = settings,
            onUserMove = onUserMove,
            onFlipBoard = onFlipBoard,
            onHint = onHint,
            onTakeback = onTakeback,
            onDraw = onDraw,
            onResign = onResign,
            onNewGameLobby = onNewGameLobby,
            onRematch = onStartGame,
            onSelectViewingPly = onSelectViewingPly,
            onDismissGameOver = onDismissGameOver,
            onReviewCurrentGame = onReviewCurrentGame,
            onOpenSettings = onOpenSettings
        )
    }
}

@Composable
private fun GameSetupLobby(
    uiState: GameUiState,
    settings: UserSettings,
    defeatedBotIds: Set<String>,
    onSelectMode: (GameMode) -> Unit,
    onSelectBot: (BotProfile) -> Unit,
    onSelectSide: (SidePreference) -> Unit,
    onSelectTimeControl: (TimeControl) -> Unit,
    onStartGame: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var showCustomTimerDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ChessBgPrimary),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "New Game",
                    style = MaterialTheme.typography.headlineLarge,
                    color = ChessCream
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.cd_settings),
                        tint = ChessTextPrimary
                    )
                }
            }

            // Mode Segmented Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ChessBgSecondary)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ModePill(
                    title = "Play vs Bots",
                    selected = uiState.selectedMode == GameMode.VS_BOT,
                    onClick = { onSelectMode(GameMode.VS_BOT) },
                    tag = "mode_vs_bot",
                    modifier = Modifier.weight(1f)
                )
                ModePill(
                    title = "Pass & Play",
                    selected = uiState.selectedMode == GameMode.PASS_AND_PLAY,
                    onClick = { onSelectMode(GameMode.PASS_AND_PLAY) },
                    tag = "mode_pass_play",
                    modifier = Modifier.weight(1f)
                )
            }

            // Bot Selector (VS_BOT)
            if (uiState.selectedMode == GameMode.VS_BOT) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Choose Bot Opponent",
                            style = MaterialTheme.typography.titleMedium,
                            color = ChessCream
                        )
                        Text(
                            text = "${uiState.selectedBot.name} • ${uiState.selectedBot.elo}",
                            fontFamily = MonoFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChessGreenPrimary
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(BotProfile.ROSTER, key = { it.id }) { bot ->
                            val selected = bot.id == uiState.selectedBot.id
                            val beaten = bot.id in defeatedBotIds
                            val accent = Color(bot.accentHex)
                            Column(
                                modifier = Modifier
                                    .width(122.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (selected) ChessSurfaceElevated else ChessBgSecondary)
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) ChessGreenPrimary else ChessSurfaceBorder,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onSelectBot(bot) }
                                    .padding(10.dp)
                                    .testTag("bot_card_${bot.id}"),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(contentAlignment = Alignment.TopEnd) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(accent.copy(alpha = 0.22f))
                                            .border(1.5.dp, accent, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        ChessPieceIcon(
                                            piece = Piece(bot.avatarPiece, PieceColor.WHITE),
                                            style = settings.pieceStyle,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                    if (beaten) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Defeated",
                                            tint = ChessGreenPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = bot.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ChessTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Surface(
                                    color = accent.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${bot.elo} ELO",
                                        fontFamily = MonoFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accent,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = bot.tierBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ChessTextSecondary
                                )
                            }
                        }
                    }

                    // Selected Bot Bio Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ChessBgSecondary)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "\"${uiState.selectedBot.bio}\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = ChessTextSecondary
                        )
                    }

                    // Side Picker
                    Text(
                        text = "Play As",
                        style = MaterialTheme.typography.titleSmall,
                        color = ChessTextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SidePreference.entries.forEach { side ->
                            val selected = side == uiState.selectedSide
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) ChessGreenPrimary.copy(alpha = 0.22f) else ChessBgSecondary)
                                    .border(
                                        width = if (selected) 1.5.dp else 1.dp,
                                        color = if (selected) ChessGreenPrimary else ChessSurfaceBorder,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onSelectSide(side) }
                                    .padding(vertical = 12.dp)
                                    .testTag("side_${side.name.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = side.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) ChessCream else ChessTextSecondary
                                )
                            }
                        }
                    }
                }
            } else {
                // Pass & Play Info Note
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ChessBgSecondary)
                        .border(1.dp, ChessSurfaceBorder, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Pass & Play lets two players share one device. Board orientation stays fixed after each move — use the manual Flip button whenever desired.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ChessTextSecondary
                    )
                }
            }

            // Time Control Selector
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = ChessGreenPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Time Control",
                            style = MaterialTheme.typography.titleMedium,
                            color = ChessCream
                        )
                    }
                    TextButton(
                        onClick = { showCustomTimerDialog = true },
                        modifier = Modifier.testTag("btn_custom_timer")
                    ) {
                        Text("Custom...", color = ChessGold)
                    }
                }

                val groups = listOf(
                    TimeControlCategory.BULLET,
                    TimeControlCategory.BLITZ,
                    TimeControlCategory.RAPID
                )
                groups.forEach { cat ->
                    val presets = TimeControl.PRESETS.filter { it.category == cat }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = cat.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = ChessTextMuted,
                            modifier = Modifier.width(46.dp)
                        )
                        presets.forEach { tc ->
                            val selected = uiState.selectedTimeControl.id == tc.id
                            TimeControlChip(
                                label = tc.label,
                                selected = selected,
                                onClick = { onSelectTimeControl(tc) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // No Timer + Active Custom Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val noTimer = TimeControl.PRESETS.first { it.category == TimeControlCategory.NONE }
                    TimeControlChip(
                        label = "No Timer",
                        selected = uiState.selectedTimeControl.id == noTimer.id,
                        onClick = { onSelectTimeControl(noTimer) },
                        modifier = Modifier.weight(1f)
                    )
                    val isCustom = uiState.selectedTimeControl.category == TimeControlCategory.CUSTOM
                    TimeControlChip(
                        label = if (isCustom) "Custom: ${uiState.selectedTimeControl.label}" else "Custom Time",
                        selected = isCustom,
                        onClick = { showCustomTimerDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Primary Start Game Button
            Button(
                onClick = onStartGame,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("start_game_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ChessGreenPrimary,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (uiState.selectedMode == GameMode.VS_BOT) {
                        "Play vs ${uiState.selectedBot.name}"
                    } else {
                        "Start Pass & Play"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showCustomTimerDialog) {
        CustomTimeControlDialog(
            onDismiss = { showCustomTimerDialog = false },
            onConfirm = { mins, inc ->
                onSelectTimeControl(TimeControl.custom(mins, inc))
                showCustomTimerDialog = false
            }
        )
    }
}

@Composable
private fun ModePill(
    title: String,
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
            .padding(vertical = 11.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else ChessTextSecondary
        )
    }
}

@Composable
private fun TimeControlChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) ChessGreenPrimary.copy(alpha = 0.24f) else ChessBgSecondary)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) ChessGreenPrimary else ChessSurfaceBorder,
                shape = RoundedCornerShape(9.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = MonoFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) ChessCream else ChessTextSecondary,
            maxLines = 1
        )
    }
}

@Composable
private fun CustomTimeControlDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var minutes by remember { mutableFloatStateOf(10f) }
    var increment by remember { mutableFloatStateOf(5f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ChessBgSecondary,
        title = { Text("Custom Time Control", color = ChessCream) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Minutes per side: ${minutes.toInt()} min",
                    fontFamily = MonoFontFamily,
                    color = ChessTextPrimary
                )
                Slider(
                    value = minutes,
                    onValueChange = { minutes = it },
                    valueRange = 1f..60f,
                    colors = SliderDefaults.colors(thumbColor = ChessGreenPrimary, activeTrackColor = ChessGreenPrimary)
                )
                Text(
                    text = "Increment per move: +${increment.toInt()} sec",
                    fontFamily = MonoFontFamily,
                    color = ChessTextPrimary
                )
                Slider(
                    value = increment,
                    onValueChange = { increment = it },
                    valueRange = 0f..30f,
                    colors = SliderDefaults.colors(thumbColor = ChessGold, activeTrackColor = ChessGold)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(minutes.toInt(), increment.toInt()) },
                colors = ButtonDefaults.buttonColors(containerColor = ChessGreenPrimary)
            ) {
                Text("Apply", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = ChessTextSecondary)
            }
        }
    )
}

@Composable
private fun ActiveMatchView(
    uiState: GameUiState,
    settings: UserSettings,
    onUserMove: (Move) -> Unit,
    onFlipBoard: () -> Unit,
    onHint: () -> Unit,
    onTakeback: () -> Unit,
    onDraw: () -> Unit,
    onResign: () -> Unit,
    onNewGameLobby: () -> Unit,
    onRematch: () -> Unit,
    onSelectViewingPly: (Int) -> Unit,
    onDismissGameOver: () -> Unit,
    onReviewCurrentGame: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val displayed = uiState.displayedState
    val material = remember(displayed.board) {
        ChessEngine.calculateMaterialSummary(displayed.board)
    }

    val topColor = if (uiState.boardFlipped) PieceColor.WHITE else PieceColor.BLACK
    val bottomColor = topColor.opposite()

    fun nameForColor(color: PieceColor): Triple<String, String?, Pair<Piece, Color>> {
        return if (uiState.selectedMode == GameMode.PASS_AND_PLAY) {
            if (color == PieceColor.WHITE) {
                Triple("White", "Local", Piece(PieceType.KING, PieceColor.WHITE) to ChessCream)
            } else {
                Triple("Black", "Local", Piece(PieceType.KING, PieceColor.BLACK) to ChessGold)
            }
        } else {
            if (color == uiState.playerColor) {
                Triple("You", uiState.selectedTimeControl.label, Piece(PieceType.KING, color) to ChessGreenPrimary)
            } else {
                Triple(
                    uiState.selectedBot.name,
                    "${uiState.selectedBot.elo}",
                    Piece(uiState.selectedBot.avatarPiece, color) to Color(uiState.selectedBot.accentHex)
                )
            }
        }
    }

    val (topName, topSub, topAvatar) = nameForColor(topColor)
    val (botName, botSub, botAvatar) = nameForColor(bottomColor)

    val arrows = remember(uiState.hintMove) {
        val hint = uiState.hintMove
        if (hint != null) listOf(BoardArrow(hint.from, hint.to, ChessArrowHint)) else emptyList()
    }

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
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Compact Top Strip (Match Status + Eval Bar + Settings)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (settings.showLiveEvalBar) {
                    EvaluationBar(
                        evaluation = uiState.liveEvaluation,
                        flipped = uiState.boardFlipped,
                        isComputing = uiState.isEvaluating,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Text(
                        text = if (uiState.selectedMode == GameMode.VS_BOT) {
                            "vs ${uiState.selectedBot.name} (${uiState.selectedBot.elo})"
                        } else {
                            "Pass & Play • ${uiState.selectedTimeControl.label}"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = ChessCream,
                        modifier = Modifier.weight(1f)
                    )
                }
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.cd_settings),
                        tint = ChessTextSecondary
                    )
                }
            }

            // Top Player Bar
            PlayerInfoBar(
                name = topName,
                subtitle = topSub,
                avatarPiece = topAvatar.first,
                avatarAccent = topAvatar.second,
                pieceStyle = settings.pieceStyle,
                capturedPieces = if (topColor == PieceColor.WHITE) material.whiteCaptured else material.blackCaptured,
                capturedPieceColor = topColor.opposite(),
                materialAdvantage = if (topColor == PieceColor.WHITE) material.whiteAdvantage else -material.whiteAdvantage,
                clockMillis = if (topColor == PieceColor.WHITE) uiState.whiteClockMs else uiState.blackClockMs,
                isActiveTurn = !uiState.boardState.status.isOver && uiState.boardState.turn == topColor,
                isThinking = uiState.isBotThinking && uiState.selectedMode == GameMode.VS_BOT && topColor != uiState.playerColor
            )

            // Interactive Chessboard
            ChessBoardView(
                boardState = displayed,
                boardTheme = settings.boardTheme,
                pieceStyle = settings.pieceStyle,
                flipped = uiState.boardFlipped,
                interactive = uiState.canUserInteract,
                showCoordinates = settings.showCoordinates,
                showLegalMoves = settings.showLegalMoves,
                lastMove = displayed.moveHistory.lastOrNull()?.move,
                arrows = arrows,
                onMoveSelected = onUserMove,
                modifier = Modifier.fillMaxWidth()
            )

            // Bottom Player Bar
            PlayerInfoBar(
                name = botName,
                subtitle = botSub,
                avatarPiece = botAvatar.first,
                avatarAccent = botAvatar.second,
                pieceStyle = settings.pieceStyle,
                capturedPieces = if (bottomColor == PieceColor.WHITE) material.whiteCaptured else material.blackCaptured,
                capturedPieceColor = bottomColor.opposite(),
                materialAdvantage = if (bottomColor == PieceColor.WHITE) material.whiteAdvantage else -material.whiteAdvantage,
                clockMillis = if (bottomColor == PieceColor.WHITE) uiState.whiteClockMs else uiState.blackClockMs,
                isActiveTurn = !uiState.boardState.status.isOver && uiState.boardState.turn == bottomColor,
                isThinking = uiState.isBotThinking && uiState.selectedMode == GameMode.VS_BOT && bottomColor != uiState.playerColor
            )

            // Move History Strip & Playback
            MoveHistoryStrip(
                moves = uiState.boardState.moveHistory,
                viewingPly = uiState.viewingPly,
                onSelectPly = onSelectViewingPly
            )

            // Coach Feedback / Status Callout
            if (!uiState.coachFeedback.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ChessBgSecondary)
                        .border(1.dp, ChessSurfaceBorder.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = uiState.coachFeedback,
                        style = MaterialTheme.typography.bodySmall,
                        color = ChessCream
                    )
                }
            }

            // Action Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ChessBgSecondary)
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MatchActionIcon(
                    icon = Icons.Default.SwapVert,
                    label = "Flip",
                    onClick = onFlipBoard,
                    tag = "action_flip"
                )
                if (uiState.selectedMode == GameMode.VS_BOT) {
                    MatchActionIcon(
                        icon = Icons.Default.Lightbulb,
                        label = "Hint",
                        enabled = uiState.canUserInteract,
                        onClick = onHint,
                        tag = "action_hint"
                    )
                }
                MatchActionIcon(
                    icon = Icons.AutoMirrored.Filled.Undo,
                    label = "Undo",
                    enabled = uiState.boardState.moveHistory.isNotEmpty(),
                    onClick = onTakeback,
                    tag = "action_undo"
                )
                if (!uiState.boardState.status.isOver) {
                    MatchActionIcon(
                        icon = Icons.Default.Handshake,
                        label = "Draw",
                        onClick = onDraw,
                        tag = "action_draw"
                    )
                    MatchActionIcon(
                        icon = Icons.Default.Flag,
                        label = "Resign",
                        onClick = onResign,
                        tag = "action_resign"
                    )
                } else {
                    MatchActionIcon(
                        icon = Icons.Default.AddCircleOutline,
                        label = "New",
                        onClick = onNewGameLobby,
                        tag = "action_new"
                    )
                }
            }

            if (uiState.boardState.status.isOver) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onReviewCurrentGame,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ChessGreenPrimary)
                    ) {
                        Text("Game Review", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onRematch,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Rematch", color = ChessCream)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }

    if (uiState.showGameOverDialog) {
        AlertDialog(
            onDismissRequest = onDismissGameOver,
            containerColor = ChessBgSecondary,
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = uiState.boardState.status.headline(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = ChessCream
                    )
                    Text(
                        text = uiState.boardState.status.terminationReason(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ChessGreenPrimary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Result: ${uiState.boardState.status.pgnResult()} • ${uiState.boardState.moveHistory.size} plies",
                        fontFamily = MonoFontFamily,
                        color = ChessTextSecondary
                    )
                    Button(
                        onClick = {
                            onDismissGameOver()
                            onReviewCurrentGame()
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("dialog_review_game"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ChessGreenPrimary)
                    ) {
                        Text("Game Review & Accuracy", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRematch,
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Text("Rematch", color = ChessCream)
                        }
                        OutlinedButton(
                            onClick = onNewGameLobby,
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Text("New Setup", color = ChessTextSecondary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissGameOver) {
                    Text("Inspect Board", color = ChessTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun MatchActionIcon(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    tag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(tag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) ChessTextPrimary else ChessTextMuted,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) ChessTextSecondary else ChessTextMuted
        )
    }
}
