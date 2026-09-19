package com.example.chess.ui.history

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.LastPage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.chess.data.GameRecordEntity
import com.example.chess.engine.GameState
import com.example.chess.ui.BoardTheme
import com.example.chess.ui.components.ChessBoardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    games: List<GameRecordEntity>,
    onSelectGameForReview: (GameRecordEntity) -> Unit,
    selectedGameForReview: GameRecordEntity?,
    reviewGameState: GameState,
    reviewMoveIndex: Int,
    onStepReviewMove: (Int) -> Unit,
    onCloseReview: () -> Unit,
    boardTheme: BoardTheme,
    modifier: Modifier = Modifier
) {
    if (games.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No games recorded yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Complete a match vs AI to see your match history and review moves!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("history_screen"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Match History & Game Replays",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${games.size} matches saved in your account profile",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            items(games, key = { it.id }) { game ->
                GameHistoryCard(
                    game = game,
                    onReviewClick = { onSelectGameForReview(game) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Review Dialog
    if (selectedGameForReview != null) {
        GameReviewDialog(
            game = selectedGameForReview,
            gameState = reviewGameState,
            moveIndex = reviewMoveIndex,
            onStepMove = onStepReviewMove,
            onClose = onCloseReview,
            boardTheme = boardTheme
        )
    }
}

@Composable
private fun GameHistoryCard(
    game: GameRecordEntity,
    onReviewClick: () -> Unit
) {
    val resultColor = when (game.result) {
        "WIN" -> Color(0xFF10B981)
        "LOSS" -> Color(0xFFEF4444)
        else -> Color(0xFFF59E0B)
    }

    val dateFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(game.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onReviewClick)
            .testTag("game_history_card_${game.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Result indicator pill
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(resultColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = game.result.take(1),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = resultColor
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${game.result} vs ${game.aiDifficulty}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        text = "${game.winReason} • ${game.playerColor.lowercase().replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$dateStr • ${game.moveCount} moves",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (game.ratingChange != 0) {
                    val sign = if (game.ratingChange > 0) "+" else ""
                    Text(
                        text = "$sign${game.ratingChange}",
                        fontWeight = FontWeight.Bold,
                        color = resultColor,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                FilledTonalIconButton(
                    onClick = onReviewClick,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Review Game",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GameReviewDialog(
    game: GameRecordEntity,
    gameState: GameState,
    moveIndex: Int,
    onStepMove: (Int) -> Unit,
    onClose: () -> Unit,
    boardTheme: BoardTheme
) {
    val movesList = if (game.movesSanJson.isNotBlank()) game.movesSanJson.split(",") else emptyList()
    val totalMoves = movesList.size
    val currentMoveSan = if (moveIndex > 0 && moveIndex <= totalMoves) {
        val moveNum = ((moveIndex - 1) / 2) + 1
        val isWhite = (moveIndex % 2 != 0)
        "Move $moveNum: ${if (isWhite) "White" else "Black"} plays ${movesList[moveIndex - 1]}"
    } else {
        "Initial Starting Position"
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Game Review: ${game.result}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "vs ${game.aiDifficulty} • ${game.winReason}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close Review")
                    }
                }

                // Board
                ChessBoardView(
                    state = gameState,
                    selectedSquare = null,
                    legalMoves = emptyList(),
                    lastMove = gameState.moveHistory.lastOrNull()?.move,
                    isFlipped = game.playerColor == "BLACK",
                    boardTheme = boardTheme,
                    onSquareClick = {}
                )

                // Current Move text & Stepper
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = currentMoveSan,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stepper controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onStepMove(-totalMoves) },
                            enabled = moveIndex > 0
                        ) {
                            Icon(Icons.Default.FirstPage, contentDescription = "Jump to Start")
                        }

                        IconButton(
                            onClick = { onStepMove(-1) },
                            enabled = moveIndex > 0
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Move")
                        }

                        Text(
                            text = "$moveIndex / $totalMoves",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )

                        IconButton(
                            onClick = { onStepMove(1) },
                            enabled = moveIndex < totalMoves
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Move")
                        }

                        IconButton(
                            onClick = { onStepMove(totalMoves) },
                            enabled = moveIndex < totalMoves
                        ) {
                            Icon(Icons.AutoMirrored.Filled.LastPage, contentDescription = "Jump to End")
                        }
                    }
                }
            }
        }
    }
}
