package com.example.chess.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.model.Piece
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType

@Composable
fun ChessPieceIcon(
    piece: Piece,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp
) {
    val symbol = when (piece.type) {
        PieceType.KING -> if (piece.color == PieceColor.WHITE) "♔" else "♚"
        PieceType.QUEEN -> if (piece.color == PieceColor.WHITE) "♕" else "♛"
        PieceType.ROOK -> if (piece.color == PieceColor.WHITE) "♖" else "♜"
        PieceType.BISHOP -> if (piece.color == PieceColor.WHITE) "♗" else "♝"
        PieceType.KNIGHT -> if (piece.color == PieceColor.WHITE) "♘" else "♞"
        PieceType.PAWN -> if (piece.color == PieceColor.WHITE) "♙" else "♟"
    }

    val textColor = if (piece.color == PieceColor.WHITE) {
        Color(0xFFFFFDF8)
    } else {
        Color(0xFF1E232A)
    }

    val shadowColor = if (piece.color == PieceColor.WHITE) {
        Color(0xBB2A241C)
    } else {
        Color(0x88FFFFFF)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            fontSize = 36.sp,
            fontWeight = FontWeight.Normal,
            style = TextStyle(
                color = textColor,
                shadow = Shadow(
                    color = shadowColor,
                    offset = Offset(2f, 2f),
                    blurRadius = 4f
                )
            )
        )
    }
}
