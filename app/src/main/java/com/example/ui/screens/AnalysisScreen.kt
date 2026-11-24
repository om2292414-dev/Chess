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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.chess.data.UserSettings
import com.example.chess.model.GameAnalysisResult
import com.example.chess.model.Move
import com.example.chess.model.MoveClassification
import com.example.ui.components.BoardArrow
import com.example.ui.components.ChessBoardView
import com.example.ui.components.EvaluationBar
import com.example.ui.components.MoveHistoryStrip
import com.example.ui.theme.ChessArrowBest
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
import com.example.viewmodel.AnalysisUiState

@Composable
fun AnalysisScreen(
    uiState: AnalysisUiState,
    settings: UserSettings,
    onPlayMove: (Move) -> Unit,
    onSelectViewingPly: (Int) -> Unit,
    onFlipBoard: () -> Unit,
    onToggleArrow: () -> Unit,
    onResetBoard: () -> Unit,
    onRunReview: () -> Unit,
    onImportFen: (String) -> Boolean,
    onImportPgn: (String) -> Boolean,
    onExportFen: () -> String,
    onExportPgn: () -> String,
    onOpenSettings: () -> Unit
) {
    var showImportExportDialog by remember { mutableStateOf(false) }

    // Determine best-move arrow: during post-game review of an inaccuracy/mistake/blunder,
    // show the best alternative from the position BEFORE that move if available, else show engine best move for current position!
    val currentAnalyzed = uiState.currentAnalyzedMove
    val boardArrows = remember(uiState.showBestMoveArrow, uiState.currentEval, currentAnalyzed) {
        if (!uiState.showBestMoveArrow) {
            emptyList()
        } else {
            val list = mutableListOf<BoardArrow>()
            val engineBest = uiState.currentEval.bestMove
            if (engineBest != null) {
                list.add(BoardArrow(engineBest.from, engineBest.to, ChessArrowBest))
            }
            if (currentAnalyzed != null &&
                currentAnalyzed.classification != MoveClassification.BEST &&
                currentAnalyzed.bestMove != null
            ) {
                // Show gold alternative arrow for what should have been played on that move
                list.add(BoardArrow(currentAnalyzed.bestMove.from, currentAnalyzed.bestMove.to, ChessArrowHint))
            }
            list
        }
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
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Analysis & Review",
                        style = MaterialTheme.typography.headlineMedium,
                        color = ChessCream
                    )
                    Text(
                        text = "${uiState.whiteLabel} vs ${uiState.blackLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ChessTextSecondary
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.cd_settings),
                        tint = ChessTextSecondary
                    )
                }
            }

            // Live Evaluation Bar
            EvaluationBar(
                evaluation = uiState.currentEval,
                flipped = uiState.boardFlipped,
                isComputing = uiState.isEvaluating
            )

            // Interactive Analysis Board
            ChessBoardView(
                boardState = uiState.displayedState,
                boardTheme = settings.boardTheme,
                pieceStyle = settings.pieceStyle,
                flipped = uiState.boardFlipped,
                interactive = true,
                showCoordinates = settings.showCoordinates,
                showLegalMoves = settings.showLegalMoves,
                lastMove = uiState.displayedState.moveHistory.lastOrNull()?.move,
                arrows = boardArrows,
                onMoveSelected = onPlayMove,
                modifier = Modifier.fillMaxWidth()
            )

            // Move History Strip + Playback Controls
            MoveHistoryStrip(
                moves = uiState.fullGameState.moveHistory,
                viewingPly = uiState.viewingPly,
                classifications = uiState.classificationMap,
                onSelectPly = onSelectViewingPly
            )

            // Inspected Move Review Callout
            if (currentAnalyzed != null) {
                val cls = currentAnalyzed.classification
                val clsColor = Color(cls.colorHex)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(clsColor.copy(alpha = 0.16f))
                        .border(1.dp, clsColor.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = clsColor,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = cls.symbol,
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${currentAnalyzed.san} is ${cls.label}",
                            style = MaterialTheme.typography.titleSmall,
                            color = ChessCream
                        )
                        if (cls != MoveClassification.BEST && currentAnalyzed.bestMoveSan != null) {
                            Text(
                                text = "Best alternative: ${currentAnalyzed.bestMoveSan} (shown in gold arrow)",
                                style = MaterialTheme.typography.bodySmall,
                                color = ChessGold
                            )
                        }
                    }
                }
            } else if (!uiState.statusBanner.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ChessBgSecondary)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = uiState.statusBanner,
                        style = MaterialTheme.typography.bodySmall,
                        color = ChessTextSecondary
                    )
                }
            }

            // Game Review Runner Button / Progress
            if (uiState.isRunningReview) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ChessBgSecondary)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Analyzing moves... ${(uiState.reviewProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleSmall,
                        color = ChessCream
                    )
                    LinearProgressIndicator(
                        progress = { uiState.reviewProgress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = ChessGreenPrimary,
                        trackColor = ChessBgDeep
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onRunReview,
                        enabled = uiState.fullGameState.moveHistory.isNotEmpty(),
                        modifier = Modifier.weight(1.3f).height(48.dp).testTag("btn_run_game_review"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ChessGreenPrimary)
                    ) {
                        Icon(Icons.Default.Analytics, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Game Review", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { showImportExportDialog = true },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("btn_pgn_fen_tools"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, tint = ChessCream, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PGN / FEN", color = ChessCream)
                    }
                }
            }

            // Quick Utility Strip (Flip, Best Arrow, Reset)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ChessBgSecondary)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AnalysisToolChip(
                    icon = Icons.Default.SwapVert,
                    label = "Flip",
                    onClick = onFlipBoard
                )
                AnalysisToolChip(
                    icon = if (uiState.showBestMoveArrow) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    label = if (uiState.showBestMoveArrow) "Arrows On" else "Arrows Off",
                    onClick = onToggleArrow
                )
                AnalysisToolChip(
                    icon = Icons.Default.RestartAlt,
                    label = "Reset",
                    onClick = onResetBoard
                )
            }

            // Full Game Review Accuracy & Classification Summary Card
            if (uiState.analysisResult != null) {
                GameReviewSummaryCard(
                    result = uiState.analysisResult,
                    whiteLabel = uiState.whiteLabel,
                    blackLabel = uiState.blackLabel
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showImportExportDialog) {
        PgnFenDialog(
            currentFen = onExportFen(),
            currentPgn = onExportPgn(),
            onImportFen = { fen ->
                val ok = onImportFen(fen)
                if (ok) showImportExportDialog = false
            },
            onImportPgn = { pgn ->
                val ok = onImportPgn(pgn)
                if (ok) showImportExportDialog = false
            },
            onDismiss = { showImportExportDialog = false }
        )
    }
}

@Composable
private fun AnalysisToolChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = ChessTextPrimary, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = ChessTextSecondary)
    }
}

@Composable
private fun GameReviewSummaryCard(
    result: GameAnalysisResult,
    whiteLabel: String,
    blackLabel: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ChessBgSecondary)
            .border(1.dp, ChessSurfaceBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Accuracy Report", style = MaterialTheme.typography.titleMedium, color = ChessCream)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AccuracyGaugeBox(
                playerName = whiteLabel,
                accuracy = result.whiteAccuracy,
                isWhite = true,
                modifier = Modifier.weight(1f)
            )
            AccuracyGaugeBox(
                playerName = blackLabel,
                accuracy = result.blackAccuracy,
                isWhite = false,
                modifier = Modifier.weight(1f)
            )
        }

        // Breakdown table
        MoveClassification.entries.forEach { cls ->
            val wCount = result.whiteCounts[cls] ?: 0
            val bCount = result.blackCounts[cls] ?: 0
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$wCount",
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = ChessCream,
                    modifier = Modifier.width(36.dp)
                )
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = cls.symbol,
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color(cls.colorHex)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = cls.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = ChessTextSecondary
                    )
                }
                Text(
                    text = "$bCount",
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = ChessCream,
                    modifier = Modifier.width(36.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun AccuracyGaugeBox(
    playerName: String,
    accuracy: Double,
    isWhite: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isWhite) ChessCream.copy(alpha = 0.12f) else ChessBgDeep)
            .border(1.dp, ChessSurfaceBorder, RoundedCornerShape(10.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = playerName,
            style = MaterialTheme.typography.labelSmall,
            color = ChessTextSecondary,
            maxLines = 1
        )
        Text(
            text = "${String.format(java.util.Locale.US, "%.1f", accuracy)}%",
            fontFamily = MonoFontFamily,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = ChessGreenPrimary
        )
    }
}

@Composable
private fun PgnFenDialog(
    currentFen: String,
    currentPgn: String,
    onImportFen: (String) -> Unit,
    onImportPgn: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var inputText by remember { mutableStateOf("") }
    var copiedNotice by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ChessBgSecondary,
        title = { Text("PGN & FEN Import / Export", color = ChessCream) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Copy current FEN / PGN
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(currentFen))
                            copiedNotice = "FEN copied to clipboard!"
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = ChessCream)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy FEN", color = ChessCream, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(currentPgn))
                            copiedNotice = "PGN copied to clipboard!"
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = ChessGreenPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy PGN", color = ChessCream, fontSize = 12.sp)
                    }
                }

                if (copiedNotice != null) {
                    Text(copiedNotice!!, style = MaterialTheme.typography.labelSmall, color = ChessGreenPrimary)
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("Paste FEN or PGN here", color = ChessTextMuted) },
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ChessGreenPrimary,
                        unfocusedBorderColor = ChessSurfaceBorder,
                        focusedTextColor = ChessCream,
                        unfocusedTextColor = ChessTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("pgn_fen_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onImportFen(inputText) },
                        enabled = inputText.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ChessSurfaceElevated)
                    ) {
                        Text("Load FEN", color = ChessCream)
                    }
                    Button(
                        onClick = { onImportPgn(inputText) },
                        enabled = inputText.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ChessGreenPrimary)
                    ) {
                        Text("Load PGN", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = ChessTextSecondary)
            }
        }
    )
}
