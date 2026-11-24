package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val GrandMasterDarkColorScheme = darkColorScheme(
    primary = ChessGreenPrimary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF3E5A23),
    onPrimaryContainer = ChessCream,
    secondary = ChessCream,
    onSecondary = ChessBgDeep,
    secondaryContainer = ChessSurfaceElevated,
    onSecondaryContainer = ChessTextPrimary,
    tertiary = ChessGold,
    onTertiary = ChessBgDeep,
    background = ChessBgPrimary,
    onBackground = ChessTextPrimary,
    surface = ChessBgSecondary,
    onSurface = ChessTextPrimary,
    surfaceVariant = ChessSurfaceElevated,
    onSurfaceVariant = ChessTextSecondary,
    outline = ChessSurfaceBorder,
    outlineVariant = Color(0xFF3A3632),
    error = ChessCheckRed,
    onError = Color.White
)

private val GrandMasterShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(22.dp)
)

@Composable
fun GrandMasterChessTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GrandMasterDarkColorScheme,
        typography = GrandMasterTypography,
        shapes = GrandMasterShapes,
        content = content
    )
}
