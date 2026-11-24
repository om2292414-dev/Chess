package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.chess.engine.ChessEngine
import com.example.chess.model.BoardState
import com.example.chess.model.BoardTheme
import com.example.chess.model.Move
import com.example.chess.model.Piece
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceStyle
import com.example.chess.model.PieceType
import com.example.chess.model.Square
import com.example.ui.theme.ChessBgSecondary
import com.example.ui.theme.ChessCheckRed
import com.example.ui.theme.ChessSurfaceBorder
import com.example.ui.theme.ChessSurfaceElevated
import com.example.ui.theme.MonoFontFamily
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

data class BoardArrow(
    val from: Square,
    val to: Square,
    val color: Color
)

/**
 * Interactive Jetpack Compose chessboard supporting both tap-to-move and drag-and-drop,
 * legal move dots/rings, last-move highlight, king-in-check radial alert, coordinates,
 * best-move/hint arrows, and pawn promotion picker dialog.
 */
@Composable
fun ChessBoardView(
    boardState: BoardState,
    boardTheme: BoardTheme,
    pieceStyle: PieceStyle,
    flipped: Boolean,
    interactive: Boolean,
    showCoordinates: Boolean = true,
    showLegalMoves: Boolean = true,
    lastMove: Move? = boardState.moveHistory.lastOrNull()?.move,
    arrows: List<BoardArrow> = emptyList(),
    onMoveSelected: (Move) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSquare by remember(boardState.board, boardState.turn) { mutableStateOf<Square?>(null) }
    var draggingFrom by remember(boardState.board, boardState.turn) { mutableStateOf<Square?>(null) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    var pendingPromotionMoves by remember { mutableStateOf<List<Move>>(emptyList()) }
    var boardPixelSize by remember { mutableStateOf(IntSize.Zero) }

    val legalMovesForSelected = remember(boardState, selectedSquare) {
        val sq = selectedSquare ?: return@remember emptyList()
        if (!interactive) emptyList() else ChessEngine.generateLegalMovesForSquare(boardState, sq)
    }

    val checkedKingSquare = remember(boardState.board, boardState.turn) {
        if (ChessEngine.isInCheck(boardState.board, boardState.turn)) {
            ChessEngine.findKingSquare(boardState.board, boardState.turn)
        } else null
    }

    val currentBoardState by rememberUpdatedState(boardState)
    val currentFlipped by rememberUpdatedState(flipped)
    val currentInteractive by rememberUpdatedState(interactive)
    val currentSelected by rememberUpdatedState(selectedSquare)
    val currentLegalForSelected by rememberUpdatedState(legalMovesForSelected)
    val currentOnMove by rememberUpdatedState(onMoveSelected)

    fun commitTarget(from: Square, to: Square): Boolean {
        val moves = ChessEngine.generateLegalMovesForSquare(currentBoardState, from)
            .filter { it.to == to }
        if (moves.isEmpty()) return false
        if (moves.size > 1 && moves.any { it.promotion != null }) {
            pendingPromotionMoves = moves
            selectedSquare = null
            return true
        }
        selectedSquare = null
        currentOnMove(moves.first())
        return true
    }

    fun offsetToSquare(offset: Offset, size: IntSize, isFlipped: Boolean): Square? {
        if (size.width <= 0 || size.height <= 0) return null
        val sqSize = size.width / 8f
        val col = (offset.x / sqSize).toInt().coerceIn(0, 7)
        val row = (offset.y / sqSize).toInt().coerceIn(0, 7)
        val file = if (isFlipped) 7 - col else col
        val rank = if (isFlipped) row else 7 - row
        return Square(rank, file)
    }

    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .onSizeChanged { boardPixelSize = it }
            .testTag("chess_board")
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (!currentInteractive) return@awaitEachGesture

                    val startPos = down.position
                    val startSq = offsetToSquare(startPos, boardPixelSize, currentFlipped)
                        ?: return@awaitEachGesture
                    val startPiece = currentBoardState.pieceAt(startSq)
                    val canDragStart = startPiece != null && startPiece.color == currentBoardState.turn

                    var isDragging = false
                    var lastPos = startPos
                    val slopPx = viewConfiguration.touchSlop

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            lastPos = change.position
                            break
                        }
                        lastPos = change.position
                        if (!isDragging && canDragStart &&
                            hypot(lastPos.x - startPos.x, lastPos.y - startPos.y) > slopPx
                        ) {
                            isDragging = true
                            draggingFrom = startSq
                            selectedSquare = startSq
                            dragPosition = lastPos
                        }
                        if (isDragging) {
                            dragPosition = lastPos
                            change.consume()
                        }
                    }

                    if (isDragging) {
                        val dropFrom = draggingFrom
                        draggingFrom = null
                        val dropSq = offsetToSquare(lastPos, boardPixelSize, currentFlipped)
                        if (dropFrom != null && dropSq != null && dropFrom != dropSq) {
                            commitTarget(dropFrom, dropSq)
                        }
                    } else {
                        // Tap-to-move handling
                        val prevSelected = currentSelected
                        if (prevSelected != null) {
                            if (prevSelected == startSq) {
                                selectedSquare = null
                            } else {
                                val moved = commitTarget(prevSelected, startSq)
                                if (!moved) {
                                    // Switch selection if tapping another own piece
                                    selectedSquare = if (canDragStart) startSq else null
                                }
                            }
                        } else if (canDragStart) {
                            selectedSquare = startSq
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sqSize = size.width / 8f
            val lightColor = Color(boardTheme.lightSquareHex)
            val darkColor = Color(boardTheme.darkSquareHex)
            val hlLight = Color(boardTheme.highlightLightHex)
            val hlDark = Color(boardTheme.highlightDarkHex)

            val legalDestMap = if (showLegalMoves) {
                legalMovesForSelected.associateBy { it.to }
            } else emptyMap()

            // 1. Draw squares & highlights
            for (rank in 0..7) {
                for (file in 0..7) {
                    val sq = Square(rank, file)
                    val col = if (flipped) 7 - file else file
                    val row = if (flipped) rank else 7 - rank
                    val topLeft = Offset(col * sqSize, row * sqSize)

                    val isLight = sq.isLightSquare
                    val isLastMoveSq = lastMove != null && (lastMove.from == sq || lastMove.to == sq)
                    val isSelectedSq = selectedSquare == sq

                    val baseColor = when {
                        isSelectedSq || isLastMoveSq -> if (isLight) hlLight else hlDark
                        else -> if (isLight) lightColor else darkColor
                    }

                    drawRect(
                        color = baseColor,
                        topLeft = topLeft,
                        size = Size(sqSize, sqSize)
                    )

                    // King in check radial glow
                    if (checkedKingSquare == sq) {
                        val center = Offset(topLeft.x + sqSize / 2f, topLeft.y + sqSize / 2f)
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    ChessCheckRed.copy(alpha = 0.92f),
                                    ChessCheckRed.copy(alpha = 0.45f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = sqSize * 0.68f
                            ),
                            topLeft = topLeft,
                            size = Size(sqSize, sqSize)
                        )
                    }

                    // Drag hover ring
                    if (draggingFrom != null) {
                        val hoverSq = offsetToSquare(dragPosition, boardPixelSize, flipped)
                        if (hoverSq == sq) {
                            drawRect(
                                color = Color.White.copy(alpha = 0.55f),
                                topLeft = topLeft,
                                size = Size(sqSize, sqSize),
                                style = Stroke(width = sqSize * 0.08f)
                            )
                        }
                    }

                    // Coordinates (rank on left column, file on bottom row)
                    if (showCoordinates) {
                        val coordColor = if (isLight) darkColor else lightColor
                        val coordStyle = TextStyle(
                            color = coordColor,
                            fontSize = (sqSize * 0.20f).coerceIn(8f, 13f).sp,
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                        if (col == 0) {
                            val rankText = textMeasurer.measure("${rank + 1}", style = coordStyle)
                            drawText(
                                textLayoutResult = rankText,
                                topLeft = Offset(topLeft.x + sqSize * 0.06f, topLeft.y + sqSize * 0.04f)
                            )
                        }
                        if (row == 7) {
                            val fileText = textMeasurer.measure("${'a' + file}", style = coordStyle)
                            drawText(
                                textLayoutResult = fileText,
                                topLeft = Offset(
                                    topLeft.x + sqSize - fileText.size.width - sqSize * 0.06f,
                                    topLeft.y + sqSize - fileText.size.height - sqSize * 0.03f
                                )
                            )
                        }
                    }
                }
            }

            // 2. Draw legal move indicators
            if (showLegalMoves && legalDestMap.isNotEmpty()) {
                for ((destSq, move) in legalDestMap) {
                    val col = if (flipped) 7 - destSq.file else destSq.file
                    val row = if (flipped) destSq.rank else 7 - destSq.rank
                    val center = Offset(col * sqSize + sqSize / 2f, row * sqSize + sqSize / 2f)
                    val isCapture = move.isEnPassant || boardState.pieceAt(destSq) != null
                    if (isCapture) {
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.24f),
                            radius = sqSize * 0.43f,
                            center = center,
                            style = Stroke(width = sqSize * 0.095f)
                        )
                    } else {
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.22f),
                            radius = sqSize * 0.165f,
                            center = center
                        )
                    }
                }
            }

            // 3. Draw pieces (excluding actively dragged piece)
            for (rank in 0..7) {
                for (file in 0..7) {
                    val sq = Square(rank, file)
                    if (sq == draggingFrom) continue
                    val piece = boardState.pieceAt(sq) ?: continue
                    val col = if (flipped) 7 - file else file
                    val row = if (flipped) rank else 7 - rank
                    drawChessPiece(
                        piece = piece,
                        style = pieceStyle,
                        topLeft = Offset(col * sqSize, row * sqSize),
                        squareSize = sqSize
                    )
                }
            }

            // 4. Draw hint / best-move arrows
            for (arrow in arrows) {
                val fCol = if (flipped) 7 - arrow.from.file else arrow.from.file
                val fRow = if (flipped) arrow.from.rank else 7 - arrow.from.rank
                val tCol = if (flipped) 7 - arrow.to.file else arrow.to.file
                val tRow = if (flipped) arrow.to.rank else 7 - arrow.to.rank
                val start = Offset(fCol * sqSize + sqSize / 2f, fRow * sqSize + sqSize / 2f)
                val end = Offset(tCol * sqSize + sqSize / 2f, tRow * sqSize + sqSize / 2f)
                drawMoveArrow(start, end, sqSize, arrow.color)
            }

            // 5. Draw dragged piece lifted above the finger
            val activeDragSq = draggingFrom
            if (activeDragSq != null) {
                val dragPiece = boardState.pieceAt(activeDragSq)
                if (dragPiece != null) {
                    val liftedSize = sqSize * 1.18f
                    drawChessPiece(
                        piece = dragPiece,
                        style = pieceStyle,
                        topLeft = Offset(
                            dragPosition.x - liftedSize / 2f,
                            dragPosition.y - liftedSize * 0.72f
                        ),
                        squareSize = liftedSize
                    )
                }
            }
        }
    }

    // Pawn Promotion Dialog
    if (pendingPromotionMoves.isNotEmpty()) {
        val color = boardState.turn
        PromotionPickerDialog(
            color = color,
            pieceStyle = pieceStyle,
            onDismiss = { pendingPromotionMoves = emptyList() },
            onChoose = { chosenType ->
                val move = pendingPromotionMoves.firstOrNull { it.promotion == chosenType }
                pendingPromotionMoves = emptyList()
                if (move != null) {
                    onMoveSelected(move)
                }
            }
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMoveArrow(
    start: Offset,
    end: Offset,
    sqSize: Float,
    color: Color
) {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val dist = hypot(dx, dy)
    if (dist < 1f) return
    val angle = atan2(dy, dx)
    val headLen = sqSize * 0.36f
    val headHalfWidth = sqSize * 0.24f
    val shaftEnd = Offset(
        end.x - cos(angle) * headLen * 0.85f,
        end.y - sin(angle) * headLen * 0.85f
    )
    drawLine(
        color = color,
        start = start,
        end = shaftEnd,
        strokeWidth = sqSize * 0.16f,
        cap = StrokeCap.Round
    )
    val leftWing = Offset(
        end.x - cos(angle) * headLen + sin(angle) * headHalfWidth,
        end.y - sin(angle) * headLen - cos(angle) * headHalfWidth
    )
    val rightWing = Offset(
        end.x - cos(angle) * headLen - sin(angle) * headHalfWidth,
        end.y - sin(angle) * headLen + cos(angle) * headHalfWidth
    )
    val headPath = Path().apply {
        moveTo(end.x, end.y)
        lineTo(leftWing.x, leftWing.y)
        lineTo(rightWing.x, rightWing.y)
        close()
    }
    drawPath(headPath, color = color)
}

@Composable
private fun PromotionPickerDialog(
    color: PieceColor,
    pieceStyle: PieceStyle,
    onDismiss: () -> Unit,
    onChoose: (PieceType) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ChessBgSecondary,
            tonalElevation = 8.dp,
            modifier = Modifier.border(1.dp, ChessSurfaceBorder, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Promote Pawn",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PieceType.PROMOTION_OPTIONS.forEach { type ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(ChessSurfaceElevated)
                                .clickable { onChoose(type) }
                                .padding(10.dp)
                                .testTag("promote_${type.name.lowercase()}")
                        ) {
                            ChessPieceIcon(
                                piece = Piece(type, color),
                                style = pieceStyle,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = type.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
