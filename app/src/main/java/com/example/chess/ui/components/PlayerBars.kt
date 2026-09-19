package com.example.chess.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.ai.AiDifficulty
import com.example.chess.auth.GoogleUser
import com.example.chess.model.Piece
import com.example.chess.model.PieceColor

@Composable
fun OpponentInfoBar(
    difficulty: AiDifficulty,
    isAiTurn: Boolean,
    isAiThinking: Boolean,
    capturedPieces: List<Piece>,
    materialAdvantage: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ai_thinking_anim")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ai_pulse"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = if (isAiTurn) androidx.compose.foundation.BorderStroke(
            1.5.dp,
            MaterialTheme.colorScheme.primary
        ) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // AI Avatar
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF6366F1), Color(0xFFA855F7))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (difficulty == AiDifficulty.GRANDMASTER) Icons.Default.AutoAwesome else Icons.Default.Psychology,
                        contentDescription = "AI Opponent",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = difficulty.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (difficulty == AiDifficulty.GRANDMASTER) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "✨ AI",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (isAiThinking) {
                        Text(
                            text = "Thinking deeply...",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.alpha(alphaAnim)
                        )
                    } else {
                        Text(
                            text = "Rating ~${difficulty.ratingEstimate}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Captured pieces & material
            Row(verticalAlignment = Alignment.CenterVertically) {
                CapturedPieceRow(capturedPieces)
                if (materialAdvantage < 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "+${-materialAdvantage}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerInfoBar(
    user: GoogleUser,
    playerColor: PieceColor,
    isPlayerTurn: Boolean,
    capturedPieces: List<Piece>,
    materialAdvantage: Int,
    durationSeconds: Long,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = if (isPlayerTurn) androidx.compose.foundation.BorderStroke(
            1.5.dp,
            MaterialTheme.colorScheme.primary
        ) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // User Avatar
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0EA5E9), Color(0xFF10B981))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.initials,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (user.isSignedIn) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "✓ Sync",
                                fontSize = 10.sp,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    val mins = durationSeconds / 60
                    val secs = durationSeconds % 60
                    Text(
                        text = "${playerColor.name.lowercase().replaceFirstChar { it.uppercase() }} • %02d:%02d".format(mins, secs),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            // Captured pieces & material
            Row(verticalAlignment = Alignment.CenterVertically) {
                CapturedPieceRow(capturedPieces)
                if (materialAdvantage > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "+$materialAdvantage",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                }
            }
        }
    }
}

@Composable
private fun CapturedPieceRow(pieces: List<Piece>) {
    val symbolMap = pieces.groupBy { it.type }
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        symbolMap.forEach { (type, list) ->
            val sym = when (type) {
                com.example.chess.model.PieceType.PAWN -> "♙"
                com.example.chess.model.PieceType.KNIGHT -> "♘"
                com.example.chess.model.PieceType.BISHOP -> "♗"
                com.example.chess.model.PieceType.ROOK -> "♖"
                com.example.chess.model.PieceType.QUEEN -> "♕"
                com.example.chess.model.PieceType.KING -> "♔"
            }
            Text(
                text = "$sym${if (list.size > 1) list.size else ""}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
