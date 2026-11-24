package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.GrandMasterEmblem
import com.example.ui.theme.ChessBgDeep
import com.example.ui.theme.ChessBgPrimary
import com.example.ui.theme.ChessBgSecondary
import com.example.ui.theme.ChessCream
import com.example.ui.theme.ChessGreenPrimary
import com.example.ui.theme.ChessTextMuted
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.MonoFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Sleek Chess.com-inspired opening splash screen with dark chessboard pattern background,
 * bold vector King/Knight emblem, animated "GRAND MASTER CHESS" & "BY CHIKU THE DEVELOPER"
 * typography, auto-advancing after 2.5 seconds or immediately on tap.
 */
@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    val emblemScale = remember { Animatable(0.65f) }
    val emblemAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleSlide = remember { Animatable(24f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val accentBarWidth = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            emblemAlpha.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
        }
        launch {
            emblemScale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
        }
        delay(220L)
        launch {
            titleAlpha.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
        }
        launch {
            titleSlide.animateTo(0f, tween(480, easing = FastOutSlowInEasing))
        }
        delay(260L)
        launch {
            accentBarWidth.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
        }
        launch {
            subtitleAlpha.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
        }
        delay(2020L) // Total ~2.5s before smooth transition
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ChessBgPrimary)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onFinished
            )
            .testTag("splash_screen")
    ) {
        // Subtle dark chessboard tile background (#312e2b & #262421)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cols = 8
            val tile = size.width / cols
            val rows = (size.height / tile).toInt() + 2
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val isDark = (r + c) % 2 == 0
                    drawRect(
                        color = if (isDark) ChessBgSecondary else ChessBgPrimary,
                        topLeft = Offset(c * tile, r * tile),
                        size = Size(tile, tile)
                    )
                }
            }
            // Dramatic spotlight radial vignette
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ChessGreenPrimary.copy(alpha = 0.08f),
                        ChessBgDeep.copy(alpha = 0.62f),
                        ChessBgDeep.copy(alpha = 0.92f)
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.44f),
                    radius = maxOf(size.width, size.height) * 0.72f
                ),
                size = size
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Bold vector King Crown + Knight Emblem
            GrandMasterEmblem(
                modifier = Modifier
                    .size(164.dp)
                    .scale(emblemScale.value)
                    .alpha(emblemAlpha.value)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Animated Title
            Text(
                text = stringResource(R.string.splash_title),
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 30.sp,
                lineHeight = 36.sp,
                letterSpacing = 2.4.sp,
                textAlign = TextAlign.Center,
                color = ChessCream,
                modifier = Modifier
                    .alpha(titleAlpha.value)
                    .padding(top = titleSlide.value.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Expanding Green Accent Bar
            Box(
                modifier = Modifier
                    .width((120f * accentBarWidth.value).dp)
                    .height(3.5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(ChessGreenPrimary)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Animated Developer Credit Subtitle
            Text(
                text = stringResource(R.string.splash_subtitle),
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 2.6.sp,
                textAlign = TextAlign.Center,
                color = ChessGreenPrimary,
                modifier = Modifier.alpha(subtitleAlpha.value)
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = stringResource(R.string.splash_tap_hint),
                style = MaterialTheme.typography.labelSmall,
                color = ChessTextMuted,
                modifier = Modifier.alpha(subtitleAlpha.value * 0.8f)
            )
        }
    }
}
