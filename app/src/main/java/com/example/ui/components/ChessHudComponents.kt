package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.chess.model.EngineEvaluation
import com.example.chess.model.MoveClassification
import com.example.chess.model.MoveRecord
import com.example.chess.model.Piece
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceStyle
import com.example.chess.model.PieceType
import com.example.ui.theme.ChessBgDeep
import com.example.ui.theme.ChessBgSecondary
import com.example.ui.theme.ChessCheckRed
import com.example.ui.theme.ChessCream
import com.example.ui.theme.ChessGreenPrimary
import com.example.ui.theme.ChessSurfaceBorder
import com.example.ui.theme.ChessSurfaceElevated
import com.example.ui.theme.ChessTextMuted
import com.example.ui.theme.ChessTextPrimary
import com.example.ui.theme.ChessTextSecondary
import com.example.ui.theme.MonoFontFamily

/**
 * Chess.com-style animated Evaluation Bar (horizontal compact strip for mobile portrait
 * so the board retains full edge-to-edge width!).
 */
@Composable
fun EvaluationBar(
    evaluation: EngineEvaluation,
    flipped: Boolean = false,
    isComputing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val rawWhiteRatio = evaluation.whiteFillRatio()
    val targetLeftRatio = if (flipped) 1f - rawWhiteRatio else rawWhiteRatio
    val animatedRatio by animateFloatAsState(
        targetValue = targetLeftRatio.coerceIn(0.03f, 0.97f),
        animationSpec = tween(durationMillis = 360),
        label = "eval_bar_ratio"
    )
    val scoreText = evaluation.formattedScore()
    val whiteAhead = (evaluation.mateIn != null && evaluation.mateIn > 0) ||
        (evaluation.mateIn == null && evaluation.scoreCp >= 0)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1E1C1A))
            .border(1.dp, ChessSurfaceBorder, RoundedCornerShape(6.dp))
            .testTag("evaluation_bar"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val leftColor = if (flipped) Color(0xFF2B2926) else Color(0xFFEEEED2)
        val rightColor = if (flipped) Color(0xFFEEEED2) else Color(0xFF2B2926)

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(animatedRatio)
                .background(leftColor),
            contentAlignment = Alignment.CenterStart
        ) {
            if ((!flipped && whiteAhead) || (flipped && !whiteAhead)) {
                Text(
                    text = if (isComputing) "..." else scoreText,
                    fontFamily = MonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (flipped) ChessTextPrimary else ChessBgDeep,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f - animatedRatio)
                .background(rightColor),
            contentAlignment = Alignment.CenterEnd
        ) {
            if ((!flipped && !whiteAhead) || (flipped && whiteAhead)) {
                Text(
                    text = if (isComputing) "..." else scoreText,
                    fontFamily = MonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (flipped) ChessBgDeep else ChessTextPrimary,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
        }
    }
}

/**
 * Player nameplate showing avatar, name, ELO/title badge, captured pieces,
 * real-time material advantage (+3), and digital countdown clock.
 */
@Composable
fun PlayerInfoBar(
    name: String,
    subtitle: String?,
    avatarPiece: Piece,
    avatarAccent: Color,
    pieceStyle: PieceStyle,
    capturedPieces: List<PieceType>,
    capturedPieceColor: PieceColor,
    materialAdvantage: Int,
    clockMillis: Long?,
    isActiveTurn: Boolean,
    isThinking: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isActiveTurn) ChessSurfaceElevated else ChessBgSecondary)
            .border(
                width = 1.dp,
                color = if (isActiveTurn) ChessGreenPrimary.copy(alpha = 0.65f) else ChessSurfaceBorder.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar badge
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(avatarAccent.copy(alpha = 0.2f))
                .border(1.5.dp, avatarAccent.copy(alpha = 0.75f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            ChessPieceIcon(
                piece = avatarPiece,
                style = pieceStyle,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Name + ELO + Captured pieces row
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    color = ChessTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Surface(
                        color = ChessBgDeep,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = subtitle,
                            fontFamily = MonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChessTextSecondary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
                if (isThinking) {
                    Text(
                        text = "thinking...",
                        style = MaterialTheme.typography.labelSmall,
                        color = ChessGreenPrimary
                    )
                }
            }

            // Captured pieces & material diff
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(18.dp)
            ) {
                val visibleCaps = capturedPieces.take(10)
                visibleCaps.forEach { pieceType ->
                    ChessPieceIcon(
                        piece = Piece(pieceType, capturedPieceColor),
                        style = pieceStyle,
                        modifier = Modifier.size(14.dp)
                    )
                }
                if (materialAdvantage > 0) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "+$materialAdvantage",
                        fontFamily = MonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChessGreenPrimary
                    )
                }
            }
        }

        // Digital Clock Pill
        if (clockMillis != null) {
            val isLowTime = clockMillis in 1L..15_000L
            val clockBg = when {
                isLowTime && isActiveTurn -> ChessCheckRed
                isActiveTurn -> ChessCream
                else -> ChessBgDeep
            }
            val clockFg = when {
                isLowTime && isActiveTurn -> Color.White
                isActiveTurn -> ChessBgDeep
                else -> ChessTextSecondary
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(clockBg)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formatClock(clockMillis),
                    fontFamily = MonoFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = clockFg
                )
            }
        } else if (isActiveTurn) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(ChessGreenPrimary)
            )
        }
    }
}

fun formatClock(millis: Long): String {
    val clamped = millis.coerceAtLeast(0L)
    if (clamped < 10_000L) {
        val secs = clamped / 1000L
        val tenths = (clamped % 1000L) / 100L
        return "0:0$secs.$tenths"
    }
    val totalSeconds = (clamped + 999L) / 1000L
    val mins = totalSeconds / 60L
    val secs = totalSeconds % 60L
    return "%d:%02d".format(mins, secs)
}

/**
 * Compact horizontal SAN move ticker with active ply highlighting and playback controls.
 */
@Composable
fun MoveHistoryStrip(
    moves: List<MoveRecord>,
    viewingPly: Int, // 0 = initial position, 1..moves.size = after move i-1
    classifications: Map<Int, MoveClassification> = emptyMap(),
    onSelectPly: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    LaunchedEffect(viewingPly, moves.size) {
        if (viewingPly > 0 && moves.isNotEmpty()) {
            listState.animateScrollToItem((viewingPly - 1).coerceIn(0, moves.lastIndex))
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ChessBgSecondary)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (moves.isEmpty()) {
            Text(
                text = "Make a move to begin",
                style = MaterialTheme.typography.bodySmall,
                color = ChessTextMuted,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
        } else {
            LazyRow(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(moves) { idx, record ->
                    val ply = idx + 1
                    val isSelected = ply == viewingPly
                    val cls = classifications[idx]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) ChessGreenPrimary.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable { onSelectPly(ply) }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        if (idx % 2 == 0) {
                            Text(
                                text = "${idx / 2 + 1}.",
                                fontFamily = MonoFontFamily,
                                fontSize = 12.sp,
                                color = ChessTextMuted,
                                modifier = Modifier.padding(end = 3.dp)
                            )
                        }
                        Text(
                            text = record.san,
                            fontFamily = MonoFontFamily,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) ChessCream else ChessTextPrimary
                        )
                        if (cls != null) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = cls.symbol,
                                fontFamily = MonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(cls.colorHex)
                            )
                        }
                    }
                }
            }
        }

        // Compact First / Prev / Next / Last playback controls
        IconButton(
            onClick = { onSelectPly(0) },
            enabled = viewingPly > 0,
            modifier = Modifier
                .size(34.dp)
                .testTag("btn_first_move")
        ) {
            Icon(
                imageVector = Icons.Default.FirstPage,
                contentDescription = stringResource(R.string.cd_first_move),
                tint = if (viewingPly > 0) ChessTextPrimary else ChessTextMuted
            )
        }
        IconButton(
            onClick = { onSelectPly((viewingPly - 1).coerceAtLeast(0)) },
            enabled = viewingPly > 0,
            modifier = Modifier
                .size(34.dp)
                .testTag("btn_prev_move")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.cd_prev_move),
                tint = if (viewingPly > 0) ChessTextPrimary else ChessTextMuted
            )
        }
        IconButton(
            onClick = { onSelectPly((viewingPly + 1).coerceAtMost(moves.size)) },
            enabled = viewingPly < moves.size,
            modifier = Modifier
                .size(34.dp)
                .testTag("btn_next_move")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.cd_next_move),
                tint = if (viewingPly < moves.size) ChessTextPrimary else ChessTextMuted
            )
        }
        IconButton(
            onClick = { onSelectPly(moves.size) },
            enabled = viewingPly < moves.size,
            modifier = Modifier
                .size(34.dp)
                .testTag("btn_last_move")
        ) {
            Icon(
                imageVector = Icons.Default.LastPage,
                contentDescription = stringResource(R.string.cd_last_move),
                tint = if (viewingPly < moves.size) ChessTextPrimary else ChessTextMuted
            )
        }
    }
}
