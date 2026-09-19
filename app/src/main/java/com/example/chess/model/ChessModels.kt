package com.example.chess.model

enum class PieceColor {
    WHITE, BLACK;

    fun opposite(): PieceColor = if (this == WHITE) BLACK else WHITE
}

enum class PieceType(val symbol: String, val baseValue: Int) {
    PAWN("P", 100),
    KNIGHT("N", 320),
    BISHOP("B", 330),
    ROOK("R", 500),
    QUEEN("Q", 900),
    KING("K", 20000)
}

data class Piece(
    val type: PieceType,
    val color: PieceColor
) {
    val fenChar: Char
        get() {
            val c = when (type) {
                PieceType.PAWN -> 'p'
                PieceType.KNIGHT -> 'n'
                PieceType.BISHOP -> 'b'
                PieceType.ROOK -> 'r'
                PieceType.QUEEN -> 'q'
                PieceType.KING -> 'k'
            }
            return if (color == PieceColor.WHITE) c.uppercaseChar() else c
        }

    companion object {
        fun fromFenChar(c: Char): Piece? {
            val color = if (c.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
            val type = when (c.lowercaseChar()) {
                'p' -> PieceType.PAWN
                'n' -> PieceType.KNIGHT
                'b' -> PieceType.BISHOP
                'r' -> PieceType.ROOK
                'q' -> PieceType.QUEEN
                'k' -> PieceType.KING
                else -> return null
            }
            return Piece(type, color)
        }
    }
}

data class Square(val file: Int, val rank: Int) {
    val isValid: Boolean get() = file in 0..7 && rank in 0..7

    val algebraic: String
        get() = "${('a' + file)}${rank + 1}"

    companion object {
        fun fromAlgebraic(s: String): Square? {
            if (s.length != 2) return null
            val file = s[0] - 'a'
            val rank = s[1] - '1'
            val sq = Square(file, rank)
            return if (sq.isValid) sq else null
        }
    }
}

data class Move(
    val from: Square,
    val to: Square,
    val promotion: PieceType? = null,
    val isEnPassant: Boolean = false,
    val isCastling: Boolean = false
) {
    val uci: String
        get() = "${from.algebraic}${to.algebraic}${promotion?.symbol?.lowercase() ?: ""}"
}

data class CastlingRights(
    val whiteKingside: Boolean = true,
    val whiteQueenside: Boolean = true,
    val blackKingside: Boolean = true,
    val blackQueenside: Boolean = true
) {
    fun toFen(): String {
        val sb = StringBuilder()
        if (whiteKingside) sb.append('K')
        if (whiteQueenside) sb.append('Q')
        if (blackKingside) sb.append('k')
        if (blackQueenside) sb.append('q')
        return if (sb.isEmpty()) "-" else sb.toString()
    }

    companion object {
        fun fromFen(fen: String): CastlingRights {
            return CastlingRights(
                whiteKingside = fen.contains('K'),
                whiteQueenside = fen.contains('Q'),
                blackKingside = fen.contains('k'),
                blackQueenside = fen.contains('q')
            )
        }
    }
}

enum class GameOutcome {
    ONGOING,
    WHITE_WINS,
    BLACK_WINS,
    DRAW
}

enum class WinReason {
    CHECKMATE,
    RESIGNATION,
    STALEMATE,
    FIFTY_MOVES,
    INSUFFICIENT_MATERIAL,
    TIMEOUT,
    AGREEMENT
}
