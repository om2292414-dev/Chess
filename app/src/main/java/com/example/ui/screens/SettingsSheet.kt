package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.chess.data.UserSettings
import com.example.chess.model.BoardTheme
import com.example.chess.model.Piece
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceStyle
import com.example.chess.model.PieceType
import com.example.ui.components.ChessPieceIcon
import com.example.ui.theme.ChessBgSecondary
import com.example.ui.theme.ChessGreenPrimary
import com.example.ui.theme.ChessSurfaceBorder
import com.example.ui.theme.ChessSurfaceElevated
import com.example.ui.theme.ChessTextPrimary
import com.example.ui.theme.ChessTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    settings: UserSettings,
    onDismiss: () -> Unit,
    onBoardThemeSelected: (BoardTheme) -> Unit,
    onPieceStyleSelected: (PieceStyle) -> Unit,
    onSoundToggled: (Boolean) -> Unit,
    onCoordinatesToggled: (Boolean) -> Unit,
    onLegalMovesToggled: (Boolean) -> Unit,
    onLiveEvalToggled: (Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ChessBgSecondary,
        contentColor = ChessTextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "Board & App Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = ChessTextPrimary
            )

            // Board Theme Picker
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Board Theme",
                    style = MaterialTheme.typography.titleSmall,
                    color = ChessTextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BoardTheme.entries.forEach { theme ->
                        val selected = theme == settings.boardTheme
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) ChessGreenPrimary.copy(alpha = 0.18f) else ChessSurfaceElevated)
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) ChessGreenPrimary else ChessSurfaceBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { onBoardThemeSelected(theme) }
                                .padding(8.dp)
                                .testTag("theme_${theme.name.lowercase()}")
                        ) {
                            // 2x2 Mini Board Swatch
                            Column(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            ) {
                                Row(modifier = Modifier.weight(1f)) {
                                    Box(Modifier.weight(1f).height(20.dp).background(Color(theme.lightSquareHex)))
                                    Box(Modifier.weight(1f).height(20.dp).background(Color(theme.darkSquareHex)))
                                }
                                Row(modifier = Modifier.weight(1f)) {
                                    Box(Modifier.weight(1f).height(20.dp).background(Color(theme.darkSquareHex)))
                                    Box(Modifier.weight(1f).height(20.dp).background(Color(theme.lightSquareHex)))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = theme.displayName.substringBefore(" "),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) ChessGreenPrimary else ChessTextPrimary
                            )
                        }
                    }
                }
            }

            // Piece Style Picker
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Piece Style",
                    style = MaterialTheme.typography.titleSmall,
                    color = ChessTextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PieceStyle.entries.forEach { style ->
                        val selected = style == settings.pieceStyle
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) ChessGreenPrimary.copy(alpha = 0.18f) else ChessSurfaceElevated)
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) ChessGreenPrimary else ChessSurfaceBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { onPieceStyleSelected(style) }
                                .padding(8.dp)
                                .testTag("piece_style_${style.name.lowercase()}")
                        ) {
                            ChessPieceIcon(
                                piece = Piece(PieceType.KNIGHT, PieceColor.WHITE),
                                style = style,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = style.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) ChessGreenPrimary else ChessTextPrimary
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = ChessSurfaceBorder)

            // Toggles
            SettingToggleRow(
                title = "Sound Effects",
                subtitle = "Synthesized board clicks, captures, checks & fanfares",
                checked = settings.soundEnabled,
                onCheckedChange = onSoundToggled,
                tag = "toggle_sound"
            )
            SettingToggleRow(
                title = "Board Coordinates",
                subtitle = "Show a–h and 1–8 file and rank labels",
                checked = settings.showCoordinates,
                onCheckedChange = onCoordinatesToggled,
                tag = "toggle_coordinates"
            )
            SettingToggleRow(
                title = "Legal Move Highlights",
                subtitle = "Show dots and capture rings for valid moves",
                checked = settings.showLegalMoves,
                onCheckedChange = onLegalMovesToggled,
                tag = "toggle_legal_moves"
            )
            SettingToggleRow(
                title = "Live Evaluation Bar",
                subtitle = "Show real-time engine advantage bar while playing",
                checked = settings.showLiveEvalBar,
                onCheckedChange = onLiveEvalToggled,
                tag = "toggle_live_eval"
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = ChessTextPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = ChessTextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ChessGreenPrimary,
                uncheckedThumbColor = ChessTextSecondary,
                uncheckedTrackColor = ChessSurfaceElevated
            ),
            modifier = Modifier.testTag(tag)
        )
    }
}
