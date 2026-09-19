package com.example.chess.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chess.ai.AiDifficulty
import com.example.chess.auth.GoogleUser
import com.example.chess.engine.GameState
import com.example.chess.model.GameOutcome
import com.example.chess.model.Move
import com.example.chess.model.Piece
import com.example.chess.model.PieceColor
import com.example.chess.model.Square
import com.example.chess.ui.BoardTheme
import com.example.chess.ui.components.AiCommentaryCard
import com.example.chess.ui.components.ChessBoardView
import com.example.chess.ui.components.GameControlsBar
import com.example.chess.ui.components.GameOverDialog
import com.example.chess.ui.components.OpponentInfoBar
import com.example.chess.ui.components.PlayerInfoBar

@Composable
fun GameScreen(
    gameState: GameState,
    playerColor: PieceColor,
    difficulty: AiDifficulty,
    user: GoogleUser,
    selectedSquare: Square?,
    legalMoves: List<Move>,
    lastMove: Move?,
    isAiThinking: Boolean,
    aiCommentary: String,
    coachHint: String?,
    isCoachLoading: Boolean,
    boardFlipped: Boolean,
    boardTheme: BoardTheme,
    whiteCaptured: List<Piece>,
    blackCaptured: List<Piece>,
    materialScore: Int,
    gameDurationSeconds: Long,
    onSquareClick: (Square) -> Unit,
    onUndo: () -> Unit,
    onHint: () -> Unit,
    onDismissHint: () -> Unit,
    onFlip: () -> Unit,
    onResign: () -> Unit,
    onDraw: () -> Unit,
    onNewGame: (PieceColor, AiDifficulty) -> Unit,
    onNavigateToStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showNewGameDialog by remember { mutableStateOf(false) }
    var showGameOverDialog by remember { mutableStateOf(true) }

    val isGameOver = gameState.outcome != GameOutcome.ONGOING

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("game_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Opponent Bar
        OpponentInfoBar(
            difficulty = difficulty,
            isAiTurn = gameState.turn != playerColor && !isGameOver,
            isAiThinking = isAiThinking,
            capturedPieces = if (playerColor == PieceColor.WHITE) whiteCaptured else blackCaptured,
            materialAdvantage = if (playerColor == PieceColor.WHITE) materialScore else -materialScore
        )

        // Chess Board
        ChessBoardView(
            state = gameState,
            selectedSquare = selectedSquare,
            legalMoves = legalMoves,
            lastMove = lastMove,
            isFlipped = boardFlipped,
            boardTheme = boardTheme,
            onSquareClick = onSquareClick
        )

        // Player Bar
        PlayerInfoBar(
            user = user,
            playerColor = playerColor,
            isPlayerTurn = gameState.turn == playerColor && !isGameOver,
            capturedPieces = if (playerColor == PieceColor.WHITE) blackCaptured else whiteCaptured,
            materialAdvantage = if (playerColor == PieceColor.WHITE) materialScore else -materialScore,
            durationSeconds = gameDurationSeconds
        )

        // AI Commentary & Coach Hint Card
        AiCommentaryCard(
            commentary = aiCommentary,
            coachHint = coachHint,
            isCoachLoading = isCoachLoading,
            onDismissHint = onDismissHint
        )

        // Controls
        GameControlsBar(
            canUndo = gameState.moveHistory.size >= 2,
            isAiThinking = isAiThinking,
            onUndo = onUndo,
            onHint = onHint,
            onFlip = onFlip,
            onResign = onResign,
            onDraw = onDraw,
            onNewGame = { showNewGameDialog = true }
        )

        Spacer(modifier = Modifier.height(12.dp))
    }

    // Game Over Dialog
    if (isGameOver && showGameOverDialog) {
        GameOverDialog(
            state = gameState,
            playerColor = playerColor,
            onPlayAgain = {
                showGameOverDialog = false
                showNewGameDialog = true
            },
            onViewStats = {
                showGameOverDialog = false
                onNavigateToStats()
            },
            onDismiss = { showGameOverDialog = false }
        )
    }

    // New Game Setup Dialog
    if (showNewGameDialog) {
        var selectedColor by remember { mutableStateOf(playerColor) }
        var selectedDiff by remember { mutableStateOf(difficulty) }

        AlertDialog(
            onDismissRequest = { showNewGameDialog = false },
            title = { Text("Start New Chess Match") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Choose your side:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = selectedColor == PieceColor.WHITE,
                            onClick = { selectedColor = PieceColor.WHITE },
                            label = { Text("Play as White ♔") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedColor == PieceColor.BLACK,
                            onClick = { selectedColor = PieceColor.BLACK },
                            label = { Text("Play as Black ♚") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = "Select AI Difficulty:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AiDifficulty.values().forEach { d ->
                            FilterChip(
                                selected = selectedDiff == d,
                                onClick = { selectedDiff = d },
                                label = {
                                    Text(
                                        text = "${d.displayName} (~${d.ratingEstimate}) ${if (d == AiDifficulty.GRANDMASTER) "✨" else ""}",
                                        fontWeight = if (selectedDiff == d) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onNewGame(selectedColor, selectedDiff)
                        showNewGameDialog = false
                        showGameOverDialog = true
                    }
                ) {
                    Text("Start Game")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewGameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
