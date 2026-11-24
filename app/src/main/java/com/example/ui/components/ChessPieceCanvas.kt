package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.chess.model.Piece
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceStyle
import com.example.chess.model.PieceType
import com.example.ui.theme.ChessCream
import com.example.ui.theme.ChessGreenPrimary
import com.example.ui.theme.ChessGreenShadow

/**
 * Renders crisp, sculptural vector chess pieces using Compose Canvas paths.
 * Supports 4 visual styles (Neo, Classic, Woodblock, Minimal) offline with zero external assets.
 */
@Composable
fun ChessPieceIcon(
    piece: Piece,
    style: PieceStyle = PieceStyle.NEO,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawChessPiece(
            piece = piece,
            style = style,
            topLeft = Offset.Zero,
            squareSize = minOf(size.width, size.height)
        )
    }
}

fun DrawScope.drawChessPiece(
    piece: Piece,
    style: PieceStyle,
    topLeft: Offset,
    squareSize: Float
) {
    val isWhite = piece.color == PieceColor.WHITE
    val pad = squareSize * 0.08f
    val s = squareSize - pad * 2f
    val ox = topLeft.x + pad
    val oy = topLeft.y + pad

    val fillBrush: Brush
    val strokeColor: Color
    val detailColor: Color
    val strokeWidth: Float

    when (style) {
        PieceStyle.NEO -> {
            fillBrush = if (isWhite) {
                Brush.verticalGradient(
                    listOf(Color(0xFFFFFFFF), Color(0xFFE9E9DF)),
                    startY = oy,
                    endY = oy + s
                )
            } else {
                Brush.verticalGradient(
                    listOf(Color(0xFF4B4845), Color(0xFF1F1D1B)),
                    startY = oy,
                    endY = oy + s
                )
            }
            strokeColor = if (isWhite) Color(0xFF2B2825) else Color(0xFF11100E)
            detailColor = if (isWhite) Color(0xFF3E3A36) else Color(0xFFCFCBC2)
            strokeWidth = (s * 0.042f).coerceAtLeast(1.2f)
        }

        PieceStyle.CLASSIC -> {
            fillBrush = if (isWhite) {
                Brush.linearGradient(
                    listOf(Color(0xFFFFFDF4), Color(0xFFDCD5BE)),
                    start = Offset(ox, oy),
                    end = Offset(ox + s, oy + s)
                )
            } else {
                Brush.linearGradient(
                    listOf(Color(0xFF3B3A3A), Color(0xFF141414)),
                    start = Offset(ox, oy),
                    end = Offset(ox + s, oy + s)
                )
            }
            strokeColor = Color(0xFF161514)
            detailColor = if (isWhite) Color(0xFF2B2926) else Color(0xFFE0DDD5)
            strokeWidth = (s * 0.048f).coerceAtLeast(1.4f)
        }

        PieceStyle.WOODBLOCK -> {
            fillBrush = if (isWhite) {
                Brush.verticalGradient(
                    listOf(Color(0xFFFBE6C2), Color(0xFFD5A96E)),
                    startY = oy,
                    endY = oy + s
                )
            } else {
                Brush.verticalGradient(
                    listOf(Color(0xFF6B3E26), Color(0xFF361D10)),
                    startY = oy,
                    endY = oy + s
                )
            }
            strokeColor = if (isWhite) Color(0xFF5D3A1A) else Color(0xFF1D0D06)
            detailColor = if (isWhite) Color(0xFF6C4321) else Color(0xFFE4BE8F)
            strokeWidth = (s * 0.045f).coerceAtLeast(1.3f)
        }

        PieceStyle.MINIMAL -> {
            fillBrush = if (isWhite) {
                Brush.verticalGradient(listOf(Color(0xFFFDFDFB), Color(0xFFF2F2EA)))
            } else {
                Brush.verticalGradient(listOf(Color(0xFF282725), Color(0xFF1A1918)))
            }
            strokeColor = if (isWhite) Color(0xFF22201E) else Color(0xFFD8D5CC)
            detailColor = if (isWhite) Color(0xFF22201E) else Color(0xFFD8D5CC)
            strokeWidth = (s * 0.036f).coerceAtLeast(1.1f)
        }
    }

    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val detailStroke = Stroke(width = (strokeWidth * 0.72f).coerceAtLeast(1f), cap = StrokeCap.Round)

    // Subtle grounded contact shadow
    drawOval(
        color = Color.Black.copy(alpha = 0.24f),
        topLeft = Offset(ox + s * 0.14f, oy + s * 0.85f),
        size = Size(s * 0.72f, s * 0.11f)
    )

    when (piece.type) {
        PieceType.PAWN -> drawPawn(ox, oy, s, fillBrush, strokeColor, detailColor, stroke, detailStroke)
        PieceType.ROOK -> drawRook(ox, oy, s, fillBrush, strokeColor, detailColor, stroke, detailStroke)
        PieceType.KNIGHT -> drawKnight(ox, oy, s, fillBrush, strokeColor, detailColor, stroke, detailStroke)
        PieceType.BISHOP -> drawBishop(ox, oy, s, fillBrush, strokeColor, detailColor, stroke, detailStroke)
        PieceType.QUEEN -> drawQueen(ox, oy, s, fillBrush, strokeColor, detailColor, stroke, detailStroke)
        PieceType.KING -> drawKing(ox, oy, s, fillBrush, strokeColor, detailColor, stroke, detailStroke)
    }
}

private fun DrawScope.drawPawn(
    ox: Float, oy: Float, s: Float,
    fill: Brush, strokeColor: Color, detailColor: Color,
    stroke: Stroke, detailStroke: Stroke
) {
    val body = Path().apply {
        moveTo(ox + s * 0.20f, oy + s * 0.88f)
        lineTo(ox + s * 0.80f, oy + s * 0.88f)
        cubicTo(ox + s * 0.78f, oy + s * 0.75f, ox + s * 0.68f, oy + s * 0.67f, ox + s * 0.62f, oy + s * 0.60f)
        lineTo(ox + s * 0.59f, oy + s * 0.44f)
        lineTo(ox + s * 0.41f, oy + s * 0.44f)
        lineTo(ox + s * 0.38f, oy + s * 0.60f)
        cubicTo(ox + s * 0.32f, oy + s * 0.67f, ox + s * 0.22f, oy + s * 0.75f, ox + s * 0.20f, oy + s * 0.88f)
        close()
    }
    drawPath(body, brush = fill)
    drawPath(body, color = strokeColor, style = stroke)

    // Collar ring
    drawRoundRect(
        brush = fill,
        topLeft = Offset(ox + s * 0.32f, oy + s * 0.40f),
        size = Size(s * 0.36f, s * 0.08f),
        cornerRadius = CornerRadius(s * 0.04f, s * 0.04f)
    )
    drawRoundRect(
        color = strokeColor,
        topLeft = Offset(ox + s * 0.32f, oy + s * 0.40f),
        size = Size(s * 0.36f, s * 0.08f),
        cornerRadius = CornerRadius(s * 0.04f, s * 0.04f),
        style = stroke
    )

    // Head sphere
    val headCenter = Offset(ox + s * 0.50f, oy + s * 0.27f)
    val headRadius = s * 0.155f
    drawCircle(brush = fill, radius = headRadius, center = headCenter)
    drawCircle(color = strokeColor, radius = headRadius, center = headCenter, style = stroke)

    // Base line accent
    drawLine(
        color = detailColor.copy(alpha = 0.55f),
        start = Offset(ox + s * 0.28f, oy + s * 0.80f),
        end = Offset(ox + s * 0.72f, oy + s * 0.80f),
        strokeWidth = detailStroke.width
    )
}

private fun DrawScope.drawRook(
    ox: Float, oy: Float, s: Float,
    fill: Brush, strokeColor: Color, detailColor: Color,
    stroke: Stroke, detailStroke: Stroke
) {
    val rook = Path().apply {
        // Base
        moveTo(ox + s * 0.18f, oy + s * 0.88f)
        lineTo(ox + s * 0.82f, oy + s * 0.88f)
        lineTo(ox + s * 0.79f, oy + s * 0.77f)
        lineTo(ox + s * 0.71f, oy + s * 0.72f)
        lineTo(ox + s * 0.68f, oy + s * 0.39f)
        lineTo(ox + s * 0.77f, oy + s * 0.32f)
        // Battlements
        lineTo(ox + s * 0.77f, oy + s * 0.14f)
        lineTo(ox + s * 0.64f, oy + s * 0.14f)
        lineTo(ox + s * 0.64f, oy + s * 0.23f)
        lineTo(ox + s * 0.56f, oy + s * 0.23f)
        lineTo(ox + s * 0.56f, oy + s * 0.14f)
        lineTo(ox + s * 0.44f, oy + s * 0.14f)
        lineTo(ox + s * 0.44f, oy + s * 0.23f)
        lineTo(ox + s * 0.36f, oy + s * 0.23f)
        lineTo(ox + s * 0.36f, oy + s * 0.14f)
        lineTo(ox + s * 0.23f, oy + s * 0.14f)
        lineTo(ox + s * 0.23f, oy + s * 0.32f)
        lineTo(ox + s * 0.32f, oy + s * 0.39f)
        lineTo(ox + s * 0.29f, oy + s * 0.72f)
        lineTo(ox + s * 0.21f, oy + s * 0.77f)
        close()
    }
    drawPath(rook, brush = fill)
    drawPath(rook, color = strokeColor, style = stroke)

    // Masonry bands
    drawLine(detailColor.copy(alpha = 0.6f), Offset(ox + s * 0.32f, oy + s * 0.39f), Offset(ox + s * 0.68f, oy + s * 0.39f), detailStroke.width)
    drawLine(detailColor.copy(alpha = 0.6f), Offset(ox + s * 0.29f, oy + s * 0.72f), Offset(ox + s * 0.71f, oy + s * 0.72f), detailStroke.width)
    drawLine(detailColor.copy(alpha = 0.6f), Offset(ox + s * 0.23f, oy + s * 0.80f), Offset(ox + s * 0.77f, oy + s * 0.80f), detailStroke.width)
}

private fun DrawScope.drawKnight(
    ox: Float, oy: Float, s: Float,
    fill: Brush, strokeColor: Color, detailColor: Color,
    stroke: Stroke, detailStroke: Stroke
) {
    val knight = Path().apply {
        moveTo(ox + s * 0.18f, oy + s * 0.88f)
        lineTo(ox + s * 0.82f, oy + s * 0.88f)
        lineTo(ox + s * 0.79f, oy + s * 0.76f)
        // Curved back mane
        cubicTo(ox + s * 0.80f, oy + s * 0.48f, ox + s * 0.74f, oy + s * 0.24f, ox + s * 0.54f, oy + s * 0.16f)
        // Ear
        lineTo(ox + s * 0.49f, oy + s * 0.08f)
        lineTo(ox + s * 0.42f, oy + s * 0.17f)
        // Forehead to muzzle
        cubicTo(ox + s * 0.30f, oy + s * 0.24f, ox + s * 0.22f, oy + s * 0.38f, ox + s * 0.18f, oy + s * 0.48f)
        cubicTo(ox + s * 0.16f, oy + s * 0.54f, ox + s * 0.22f, oy + s * 0.59f, ox + s * 0.29f, oy + s * 0.57f)
        // Jawline under snout
        lineTo(ox + s * 0.44f, oy + s * 0.48f)
        // Chest sweep to base
        cubicTo(ox + s * 0.44f, oy + s * 0.60f, ox + s * 0.28f, oy + s * 0.67f, ox + s * 0.22f, oy + s * 0.76f)
        close()
    }
    drawPath(knight, brush = fill)
    drawPath(knight, color = strokeColor, style = stroke)

    // Eye & nostril
    drawCircle(color = detailColor, radius = s * 0.032f, center = Offset(ox + s * 0.39f, oy + s * 0.31f))
    drawCircle(color = detailColor, radius = s * 0.02f, center = Offset(ox + s * 0.24f, oy + s * 0.49f))

    // Mane contour accent
    val mane = Path().apply {
        moveTo(ox + s * 0.56f, oy + s * 0.24f)
        cubicTo(ox + s * 0.68f, oy + s * 0.36f, ox + s * 0.70f, oy + s * 0.55f, ox + s * 0.68f, oy + s * 0.74f)
    }
    drawPath(mane, color = detailColor.copy(alpha = 0.55f), style = detailStroke)
    drawLine(detailColor.copy(alpha = 0.6f), Offset(ox + s * 0.23f, oy + s * 0.78f), Offset(ox + s * 0.77f, oy + s * 0.78f), detailStroke.width)
}

private fun DrawScope.drawBishop(
    ox: Float, oy: Float, s: Float,
    fill: Brush, strokeColor: Color, detailColor: Color,
    stroke: Stroke, detailStroke: Stroke
) {
    val mitre = Path().apply {
        moveTo(ox + s * 0.18f, oy + s * 0.88f)
        lineTo(ox + s * 0.82f, oy + s * 0.88f)
        cubicTo(ox + s * 0.76f, oy + s * 0.76f, ox + s * 0.68f, oy + s * 0.72f, ox + s * 0.62f, oy + s * 0.67f)
        cubicTo(ox + s * 0.73f, oy + s * 0.50f, ox + s * 0.68f, oy + s * 0.28f, ox + s * 0.50f, oy + s * 0.18f)
        cubicTo(ox + s * 0.32f, oy + s * 0.28f, ox + s * 0.27f, oy + s * 0.50f, ox + s * 0.38f, oy + s * 0.67f)
        cubicTo(ox + s * 0.32f, oy + s * 0.72f, ox + s * 0.24f, oy + s * 0.76f, ox + s * 0.18f, oy + s * 0.88f)
        close()
    }
    drawPath(mitre, brush = fill)
    drawPath(mitre, color = strokeColor, style = stroke)

    // Top finial orb
    val finial = Offset(ox + s * 0.50f, oy + s * 0.13f)
    drawCircle(brush = fill, radius = s * 0.06f, center = finial)
    drawCircle(color = strokeColor, radius = s * 0.06f, center = finial, style = stroke)

    // Diagonal mitre sash & cross
    drawLine(detailColor, Offset(ox + s * 0.48f, oy + s * 0.34f), Offset(ox + s * 0.63f, oy + s * 0.48f), strokeWidth = detailStroke.width * 1.1f)
    drawLine(detailColor.copy(alpha = 0.7f), Offset(ox + s * 0.36f, oy + s * 0.67f), Offset(ox + s * 0.64f, oy + s * 0.67f), detailStroke.width)
    drawLine(detailColor.copy(alpha = 0.6f), Offset(ox + s * 0.25f, oy + s * 0.80f), Offset(ox + s * 0.75f, oy + s * 0.80f), detailStroke.width)
}

private fun DrawScope.drawQueen(
    ox: Float, oy: Float, s: Float,
    fill: Brush, strokeColor: Color, detailColor: Color,
    stroke: Stroke, detailStroke: Stroke
) {
    val crown = Path().apply {
        moveTo(ox + s * 0.20f, oy + s * 0.88f)
        lineTo(ox + s * 0.80f, oy + s * 0.88f)
        lineTo(ox + s * 0.76f, oy + s * 0.73f)
        lineTo(ox + s * 0.86f, oy + s * 0.30f)
        lineTo(ox + s * 0.67f, oy + s * 0.54f)
        lineTo(ox + s * 0.68f, oy + s * 0.20f)
        lineTo(ox + s * 0.54f, oy + s * 0.50f)
        lineTo(ox + s * 0.50f, oy + s * 0.17f)
        lineTo(ox + s * 0.46f, oy + s * 0.50f)
        lineTo(ox + s * 0.32f, oy + s * 0.20f)
        lineTo(ox + s * 0.33f, oy + s * 0.54f)
        lineTo(ox + s * 0.14f, oy + s * 0.30f)
        lineTo(ox + s * 0.24f, oy + s * 0.73f)
        close()
    }
    drawPath(crown, brush = fill)
    drawPath(crown, color = strokeColor, style = stroke)

    // Crown pearls
    val tips = listOf(
        Offset(ox + s * 0.14f, oy + s * 0.27f),
        Offset(ox + s * 0.32f, oy + s * 0.17f),
        Offset(ox + s * 0.50f, oy + s * 0.13f),
        Offset(ox + s * 0.68f, oy + s * 0.17f),
        Offset(ox + s * 0.86f, oy + s * 0.27f)
    )
    for (tip in tips) {
        drawCircle(brush = fill, radius = s * 0.045f, center = tip)
        drawCircle(color = strokeColor, radius = s * 0.045f, center = tip, style = stroke)
    }

    drawLine(detailColor.copy(alpha = 0.65f), Offset(ox + s * 0.24f, oy + s * 0.73f), Offset(ox + s * 0.76f, oy + s * 0.73f), detailStroke.width)
    drawLine(detailColor.copy(alpha = 0.65f), Offset(ox + s * 0.22f, oy + s * 0.81f), Offset(ox + s * 0.78f, oy + s * 0.81f), detailStroke.width)
}

private fun DrawScope.drawKing(
    ox: Float, oy: Float, s: Float,
    fill: Brush, strokeColor: Color, detailColor: Color,
    stroke: Stroke, detailStroke: Stroke
) {
    // Royal Cross on top
    val crossWidth = s * 0.065f
    drawLine(
        color = strokeColor,
        start = Offset(ox + s * 0.50f, oy + s * 0.07f),
        end = Offset(ox + s * 0.50f, oy + s * 0.27f),
        strokeWidth = crossWidth,
        cap = StrokeCap.Round
    )
    drawLine(
        color = strokeColor,
        start = Offset(ox + s * 0.40f, oy + s * 0.15f),
        end = Offset(ox + s * 0.60f, oy + s * 0.15f),
        strokeWidth = crossWidth,
        cap = StrokeCap.Round
    )
    // Inner cross highlight
    drawLine(
        brush = fill,
        start = Offset(ox + s * 0.50f, oy + s * 0.09f),
        end = Offset(ox + s * 0.50f, oy + s * 0.25f),
        strokeWidth = crossWidth * 0.45f,
        cap = StrokeCap.Round
    )
    drawLine(
        brush = fill,
        start = Offset(ox + s * 0.42f, oy + s * 0.15f),
        end = Offset(ox + s * 0.58f, oy + s * 0.15f),
        strokeWidth = crossWidth * 0.45f,
        cap = StrokeCap.Round
    )

    // Crown lobes & body
    val kingBody = Path().apply {
        moveTo(ox + s * 0.20f, oy + s * 0.88f)
        lineTo(ox + s * 0.80f, oy + s * 0.88f)
        lineTo(ox + s * 0.77f, oy + s * 0.72f)
        cubicTo(ox + s * 0.90f, oy + s * 0.52f, ox + s * 0.86f, oy + s * 0.27f, ox + s * 0.66f, oy + s * 0.29f)
        cubicTo(ox + s * 0.58f, oy + s * 0.30f, ox + s * 0.53f, oy + s * 0.35f, ox + s * 0.50f, oy + s * 0.25f)
        cubicTo(ox + s * 0.47f, oy + s * 0.35f, ox + s * 0.42f, oy + s * 0.30f, ox + s * 0.34f, oy + s * 0.29f)
        cubicTo(ox + s * 0.14f, oy + s * 0.27f, ox + s * 0.10f, oy + s * 0.52f, ox + s * 0.23f, oy + s * 0.72f)
        close()
    }
    drawPath(kingBody, brush = fill)
    drawPath(kingBody, color = strokeColor, style = stroke)

    // Center ridge + base bands
    drawLine(detailColor.copy(alpha = 0.65f), Offset(ox + s * 0.50f, oy + s * 0.30f), Offset(ox + s * 0.50f, oy + s * 0.68f), detailStroke.width)
    drawLine(detailColor.copy(alpha = 0.65f), Offset(ox + s * 0.24f, oy + s * 0.72f), Offset(ox + s * 0.76f, oy + s * 0.72f), detailStroke.width)
    drawLine(detailColor.copy(alpha = 0.65f), Offset(ox + s * 0.22f, oy + s * 0.80f), Offset(ox + s * 0.78f, oy + s * 0.80f), detailStroke.width)
}

/**
 * Bold vector King Crown + Knight emblem in #81b64c (green) and #eeeed2 (cream)
 * for the Grand Master Chess opening splash screen and home branding.
 */
@Composable
fun GrandMasterEmblem(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val s = minOf(size.width, size.height)
        val ox = (size.width - s) / 2f
        val oy = (size.height - s) / 2f

        // Outer shield glow ring
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(ChessGreenPrimary.copy(alpha = 0.24f), Color.Transparent),
                center = Offset(ox + s * 0.5f, oy + s * 0.52f),
                radius = s * 0.52f
            ),
            radius = s * 0.52f,
            center = Offset(ox + s * 0.5f, oy + s * 0.52f)
        )

        // Royal Crown behind/above the Knight in Cream & Green
        val crownPath = Path().apply {
            moveTo(ox + s * 0.27f, oy + s * 0.39f)
            lineTo(ox + s * 0.20f, oy + s * 0.19f)
            lineTo(ox + s * 0.37f, oy + s * 0.29f)
            lineTo(ox + s * 0.50f, oy + s * 0.11f)
            lineTo(ox + s * 0.63f, oy + s * 0.29f)
            lineTo(ox + s * 0.80f, oy + s * 0.19f)
            lineTo(ox + s * 0.73f, oy + s * 0.39f)
            close()
        }
        drawPath(crownPath, color = ChessCream)
        drawPath(
            crownPath,
            color = ChessGreenPrimary,
            style = Stroke(width = s * 0.025f, join = StrokeJoin.Round)
        )
        // Cross atop Crown
        drawLine(
            color = ChessCream,
            start = Offset(ox + s * 0.50f, oy + s * 0.03f),
            end = Offset(ox + s * 0.50f, oy + s * 0.14f),
            strokeWidth = s * 0.042f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = ChessCream,
            start = Offset(ox + s * 0.44f, oy + s * 0.075f),
            end = Offset(ox + s * 0.56f, oy + s * 0.075f),
            strokeWidth = s * 0.042f,
            cap = StrokeCap.Round
        )

        // Bold Geometric Knight Silhouette in Chess Green (#81b64c)
        val knightPath = Path().apply {
            moveTo(ox + s * 0.24f, oy + s * 0.90f)
            lineTo(ox + s * 0.76f, oy + s * 0.90f)
            lineTo(ox + s * 0.73f, oy + s * 0.79f)
            cubicTo(ox + s * 0.76f, oy + s * 0.58f, ox + s * 0.72f, oy + s * 0.40f, ox + s * 0.56f, oy + s * 0.35f)
            lineTo(ox + s * 0.51f, oy + s * 0.28f)
            lineTo(ox + s * 0.45f, oy + s * 0.36f)
            cubicTo(ox + s * 0.34f, oy + s * 0.41f, ox + s * 0.25f, oy + s * 0.52f, ox + s * 0.21f, oy + s * 0.61f)
            cubicTo(ox + s * 0.19f, oy + s * 0.66f, ox + s * 0.25f, oy + s * 0.70f, ox + s * 0.32f, oy + s * 0.67f)
            lineTo(ox + s * 0.46f, oy + s * 0.59f)
            cubicTo(ox + s * 0.45f, oy + s * 0.70f, ox + s * 0.32f, oy + s * 0.73f, ox + s * 0.27f, oy + s * 0.79f)
            close()
        }
        drawPath(
            knightPath,
            brush = Brush.verticalGradient(
                listOf(ChessGreenPrimary, ChessGreenShadow),
                startY = oy + s * 0.28f,
                endY = oy + s * 0.90f
            )
        )
        drawPath(
            knightPath,
            color = ChessCream,
            style = Stroke(width = s * 0.026f, join = StrokeJoin.Round)
        )

        // Cream Knight eye & mane highlight
        drawCircle(color = ChessCream, radius = s * 0.028f, center = Offset(ox + s * 0.40f, oy + s * 0.47f))
        val maneHighlight = Path().apply {
            moveTo(ox + s * 0.58f, oy + s * 0.42f)
            cubicTo(ox + s * 0.67f, oy + s * 0.52f, ox + s * 0.67f, oy + s * 0.67f, ox + s * 0.65f, oy + s * 0.78f)
        }
        drawPath(
            maneHighlight,
            color = ChessCream.copy(alpha = 0.85f),
            style = Stroke(width = s * 0.024f, cap = StrokeCap.Round)
        )
        // Base plinth bar
        drawRoundRect(
            color = ChessCream,
            topLeft = Offset(ox + s * 0.24f, oy + s * 0.83f),
            size = Size(s * 0.52f, s * 0.045f),
            cornerRadius = CornerRadius(s * 0.02f, s * 0.02f)
        )
    }
}
