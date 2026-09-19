package com.example.chess.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.engine.GameState
import com.example.chess.model.Move
import com.example.chess.model.Piece
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import com.example.chess.model.Square
import com.example.chess.ui.BoardTheme

@Composable
fun ChessBoardView(
    state: GameState,
    selectedSquare: Square?,
    legalMoves: List<Move>,
    lastMove: Move?,
    isFlipped: Boolean,
    boardTheme: BoardTheme,
    onSquareClick: (Square) -> Unit,
    modifier: Modifier = Modifier
) {
    val lightColor = Color(boardTheme.lightColor)
    val darkColor = Color(boardTheme.darkColor)

    // Outer wooden / themed border
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(12.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E232A))
            .padding(6.dp)
            .clip(RoundedCornerShape(8.dp))
            .testTag("chess_board")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            for (row in 0..7) {
                val rank = if (isFlipped) row else 7 - row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    for (col in 0..7) {
                        val file = if (isFlipped) 7 - col else col
                        val sq = Square(file, rank)
                        val isLight = (file + rank) % 2 != 0
                        val piece = state.getPiece(sq)

                        val isSelected = selectedSquare == sq
                        val isLegalMove = legalMoves.any { it.to == sq }
                        val isLastMoveSrc = lastMove?.from == sq
                        val isLastMoveDst = lastMove?.to == sq
                        val isKingInCheck = state.isCheck && piece?.type == PieceType.KING && piece.color == state.turn

                        val baseSquareColor = if (isLight) lightColor else darkColor
                        val squareColor by animateColorAsState(
                            targetValue = when {
                                isSelected -> Color(0xFFFBBF24).copy(alpha = 0.7f)
                                isKingInCheck -> Color(0xFFEF4444).copy(alpha = 0.8f)
                                isLastMoveDst || isLastMoveSrc -> Color(0xFF38BDF8).copy(alpha = 0.45f)
                                else -> baseSquareColor
                            },
                            label = "square_color_$file$rank"
                        )

                        ChessSquareCell(
                            square = sq,
                            piece = piece,
                            backgroundColor = squareColor,
                            isSelected = isSelected,
                            isLegalMove = isLegalMove,
                            hasPiece = piece != null,
                            showFileLabel = (isFlipped && rank == 7) || (!isFlipped && rank == 0),
                            showRankLabel = (isFlipped && file == 7) || (!isFlipped && file == 0),
                            fileLabel = ('a' + file).toString(),
                            rankLabel = (rank + 1).toString(),
                            labelColor = if (isLight) darkColor.copy(alpha = 0.8f) else lightColor.copy(alpha = 0.8f),
                            onClick = { onSquareClick(sq) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChessSquareCell(
    square: Square,
    piece: Piece?,
    backgroundColor: Color,
    isSelected: Boolean,
    isLegalMove: Boolean,
    hasPiece: Boolean,
    showFileLabel: Boolean,
    showRankLabel: Boolean,
    fileLabel: String,
    rankLabel: String,
    labelColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag("square_${square.file}_${square.rank}"),
        contentAlignment = Alignment.Center
    ) {
        // Coordinate labels
        if (showRankLabel) {
            Text(
                text = rankLabel,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = labelColor,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 2.dp, top = 1.dp)
            )
        }
        if (showFileLabel) {
            Text(
                text = fileLabel,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = labelColor,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 2.dp, bottom = 1.dp)
            )
        }

        // Selected square border
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, Color(0xFFD97706), RoundedCornerShape(2.dp))
            )
        }

        // Piece
        if (piece != null) {
            ChessPieceIcon(
                piece = piece,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Legal move indicator
        if (isLegalMove) {
            if (hasPiece) {
                // Capture target ring
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.85f)
                        .border(3.dp, Color(0xCCEF4444), CircleShape)
                )
            } else {
                // Small dot for empty square
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0x9910B981))
                )
            }
        }
    }
}
