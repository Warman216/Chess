package com.example.chess.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.engine.GameState
import com.example.chess.model.GameOutcome
import com.example.chess.model.PieceColor
import com.example.chess.model.WinReason

@Composable
fun AiCommentaryCard(
    commentary: String,
    coachHint: String?,
    isCoachLoading: Boolean,
    onDismissHint: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Coach Hint Banner (if requested)
        AnimatedVisibility(
            visible = coachHint != null || isCoachLoading,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "Coach Hint",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (isCoachLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Gemini Coach is analyzing tactical motifs...",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else if (coachHint != null) {
                        Text(
                            text = coachHint,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onDismissHint,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        // Live Grandmaster Insight / Commentary
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Grandmaster Analysis",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = commentary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun GameControlsBar(
    canUndo: Boolean,
    isAiThinking: Boolean,
    onUndo: () -> Unit,
    onHint: () -> Unit,
    onFlip: () -> Unit,
    onResign: () -> Unit,
    onDraw: () -> Unit,
    onNewGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Coach Hint
        FilledTonalIconButton(
            onClick = onHint,
            enabled = !isAiThinking,
            modifier = Modifier.testTag("hint_button")
        ) {
            Icon(Icons.Default.Lightbulb, contentDescription = "Ask Coach Hint")
        }

        // Undo Move
        FilledTonalIconButton(
            onClick = onUndo,
            enabled = canUndo && !isAiThinking,
            modifier = Modifier.testTag("undo_button")
        ) {
            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo Move")
        }

        // Flip Board
        FilledTonalIconButton(
            onClick = onFlip,
            modifier = Modifier.testTag("flip_board_button")
        ) {
            Icon(Icons.Default.ScreenRotation, contentDescription = "Flip Board")
        }

        // Offer Draw
        FilledTonalIconButton(
            onClick = onDraw,
            enabled = !isAiThinking,
            modifier = Modifier.testTag("draw_button")
        ) {
            Icon(Icons.Default.Handshake, contentDescription = "Offer Draw")
        }

        // Resign
        FilledTonalIconButton(
            onClick = onResign,
            enabled = !isAiThinking,
            modifier = Modifier.testTag("resign_button")
        ) {
            Icon(
                Icons.Default.Flag,
                contentDescription = "Resign",
                tint = MaterialTheme.colorScheme.error
            )
        }

        // New Game
        Button(
            onClick = onNewGame,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("new_game_button")
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("New", fontSize = 13.sp)
        }
    }
}

@Composable
fun GameOverDialog(
    state: GameState,
    playerColor: PieceColor,
    onPlayAgain: () -> Unit,
    onViewStats: () -> Unit,
    onDismiss: () -> Unit
) {
    val isWin = (state.outcome == GameOutcome.WHITE_WINS && playerColor == PieceColor.WHITE) ||
            (state.outcome == GameOutcome.BLACK_WINS && playerColor == PieceColor.BLACK)
    val isLoss = (state.outcome == GameOutcome.WHITE_WINS && playerColor == PieceColor.BLACK) ||
            (state.outcome == GameOutcome.BLACK_WINS && playerColor == PieceColor.WHITE)
    val isDraw = state.outcome == GameOutcome.DRAW

    val title = when {
        isWin -> "🎉 Victory!"
        isLoss -> "Checkmate! Defeat"
        else -> "🤝 Draw Game"
    }

    val subtitle = when (state.winReason) {
        WinReason.CHECKMATE -> if (isWin) "You checkmated the AI opponent!" else "The AI delivered checkmate."
        WinReason.RESIGNATION -> if (isWin) "AI resigned the match." else "You resigned the match."
        WinReason.STALEMATE -> "Stalemate! No legal moves remain."
        WinReason.FIFTY_MOVES -> "Draw declared via 50-move rule."
        WinReason.INSUFFICIENT_MATERIAL -> "Draw by insufficient material."
        WinReason.AGREEMENT -> "Match ended by mutual draw agreement."
        else -> "Match finished."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Moves: ${state.moveHistory.size}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = if (isWin) "+15 Rating" else if (isLoss) "-12 Rating" else "+1 Rating",
                            color = if (isWin) Color(0xFF10B981) else if (isLoss) Color(0xFFEF4444) else Color(0xFFF59E0B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onPlayAgain,
                modifier = Modifier.testTag("game_over_play_again")
            ) {
                Text("Play Again")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onViewStats,
                modifier = Modifier.testTag("game_over_view_stats")
            ) {
                Text("View Stats")
            }
        }
    )
}
