package com.example.chess.engine

import com.example.chess.model.CastlingRights
import com.example.chess.model.GameOutcome
import com.example.chess.model.Move
import com.example.chess.model.Piece
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import com.example.chess.model.Square
import com.example.chess.model.WinReason

data class GameState(
    val board: Array<Array<Piece?>>,
    val turn: PieceColor,
    val castlingRights: CastlingRights,
    val enPassantTarget: Square?,
    val halfmoveClock: Int,
    val fullmoveNumber: Int,
    val moveHistory: List<MoveRecord> = emptyList(),
    val outcome: GameOutcome = GameOutcome.ONGOING,
    val winReason: WinReason? = null,
    val isCheck: Boolean = false
) {
    fun getPiece(sq: Square): Piece? = if (sq.isValid) board[sq.rank][sq.file] else null

    fun copyBoard(): Array<Array<Piece?>> {
        return Array(8) { r -> Array(8) { f -> board[r][f] } }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GameState) return false
        if (turn != other.turn) return false
        if (castlingRights != other.castlingRights) return false
        if (enPassantTarget != other.enPassantTarget) return false
        if (halfmoveClock != other.halfmoveClock) return false
        if (fullmoveNumber != other.fullmoveNumber) return false
        if (outcome != other.outcome) return false
        if (isCheck != other.isCheck) return false
        if (!board.contentDeepEquals(other.board)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = board.contentDeepHashCode()
        result = 31 * result + turn.hashCode()
        result = 31 * result + castlingRights.hashCode()
        result = 31 * result + (enPassantTarget?.hashCode() ?: 0)
        result = 31 * result + halfmoveClock
        result = 31 * result + fullmoveNumber
        return result
    }
}

data class MoveRecord(
    val move: Move,
    val san: String,
    val capturedPiece: Piece?,
    val fenBefore: String,
    val fenAfter: String
)

class ChessEngine {

    companion object {
        const val STARTING_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

        fun createInitialState(): GameState {
            return fromFen(STARTING_FEN)
        }

        fun fromFen(fen: String): GameState {
            val parts = fen.trim().split(" ")
            val board = Array(8) { Array<Piece?>(8) { null } }

            val rows = parts[0].split("/")
            for (r in 0..7) {
                val rank = 7 - r
                var file = 0
                for (c in rows[r]) {
                    if (c.isDigit()) {
                        file += c.digitToInt()
                    } else {
                        board[rank][file] = Piece.fromFenChar(c)
                        file++
                    }
                }
            }

            val turn = if (parts.getOrNull(1) == "b") PieceColor.BLACK else PieceColor.WHITE
            val castling = CastlingRights.fromFen(parts.getOrNull(2) ?: "KQkq")
            val epStr = parts.getOrNull(3) ?: "-"
            val epTarget = if (epStr != "-") Square.fromAlgebraic(epStr) else null
            val halfmove = parts.getOrNull(4)?.toIntOrNull() ?: 0
            val fullmove = parts.getOrNull(5)?.toIntOrNull() ?: 1

            val stateWithoutCheck = GameState(
                board = board,
                turn = turn,
                castlingRights = castling,
                enPassantTarget = epTarget,
                halfmoveClock = halfmove,
                fullmoveNumber = fullmove
            )

            val inCheck = isKingInCheck(stateWithoutCheck, turn)
            val legalMoves = getLegalMoves(stateWithoutCheck)
            val outcome: GameOutcome
            val winReason: WinReason?

            if (legalMoves.isEmpty()) {
                if (inCheck) {
                    outcome = if (turn == PieceColor.WHITE) GameOutcome.BLACK_WINS else GameOutcome.WHITE_WINS
                    winReason = WinReason.CHECKMATE
                } else {
                    outcome = GameOutcome.DRAW
                    winReason = WinReason.STALEMATE
                }
            } else if (isInsufficientMaterial(board)) {
                outcome = GameOutcome.DRAW
                winReason = WinReason.INSUFFICIENT_MATERIAL
            } else if (halfmove >= 100) {
                outcome = GameOutcome.DRAW
                winReason = WinReason.FIFTY_MOVES
            } else {
                outcome = GameOutcome.ONGOING
                winReason = null
            }

            return stateWithoutCheck.copy(
                outcome = outcome,
                winReason = winReason,
                isCheck = inCheck
            )
        }

        fun toFen(state: GameState): String {
            val sb = StringBuilder()
            for (r in 7 downTo 0) {
                var emptyCount = 0
                for (f in 0..7) {
                    val piece = state.board[r][f]
                    if (piece == null) {
                        emptyCount++
                    } else {
                        if (emptyCount > 0) {
                            sb.append(emptyCount)
                            emptyCount = 0
                        }
                        sb.append(piece.fenChar)
                    }
                }
                if (emptyCount > 0) {
                    sb.append(emptyCount)
                }
                if (r > 0) sb.append('/')
            }

            sb.append(if (state.turn == PieceColor.WHITE) " w " else " b ")
            sb.append(state.castlingRights.toFen()).append(" ")
            sb.append(state.enPassantTarget?.algebraic ?: "-").append(" ")
            sb.append(state.halfmoveClock).append(" ")
            sb.append(state.fullmoveNumber)

            return sb.toString()
        }

        fun getLegalMoves(state: GameState): List<Move> {
            val pseudoMoves = generatePseudoMoves(state, state.turn)
            return pseudoMoves.filter { move ->
                val simulated = applyMoveInternal(state, move)
                !isKingInCheck(simulated, state.turn)
            }
        }

        fun getLegalMovesForSquare(state: GameState, square: Square): List<Move> {
            val piece = state.getPiece(square) ?: return emptyList()
            if (piece.color != state.turn) return emptyList()
            return getLegalMoves(state).filter { it.from == square }
        }

        fun applyMove(state: GameState, move: Move): GameState {
            val fenBefore = toFen(state)
            val movingPiece = state.getPiece(move.from) ?: return state
            val capturedPiece = if (move.isEnPassant) {
                val capturedRank = if (state.turn == PieceColor.WHITE) move.to.rank - 1 else move.to.rank + 1
                state.board[capturedRank][move.to.file]
            } else {
                state.getPiece(move.to)
            }

            val san = computeSan(state, move)
            val newState = applyMoveInternal(state, move)
            val nextTurn = state.turn.opposite()
            val inCheck = isKingInCheck(newState, nextTurn)
            val nextLegalMoves = getLegalMoves(newState.copy(isCheck = inCheck))

            val outcome: GameOutcome
            val winReason: WinReason?

            if (nextLegalMoves.isEmpty()) {
                if (inCheck) {
                    outcome = if (nextTurn == PieceColor.WHITE) GameOutcome.BLACK_WINS else GameOutcome.WHITE_WINS
                    winReason = WinReason.CHECKMATE
                } else {
                    outcome = GameOutcome.DRAW
                    winReason = WinReason.STALEMATE
                }
            } else if (isInsufficientMaterial(newState.board)) {
                outcome = GameOutcome.DRAW
                winReason = WinReason.INSUFFICIENT_MATERIAL
            } else if (newState.halfmoveClock >= 100) {
                outcome = GameOutcome.DRAW
                winReason = WinReason.FIFTY_MOVES
            } else {
                outcome = GameOutcome.ONGOING
                winReason = null
            }

            val fenAfter = toFen(newState)
            val moveRecord = MoveRecord(
                move = move,
                san = if (outcome != GameOutcome.ONGOING && winReason == WinReason.CHECKMATE) {
                    san.replace("+", "#")
                } else if (inCheck && !san.endsWith("+")) {
                    "$san+"
                } else {
                    san
                },
                capturedPiece = capturedPiece,
                fenBefore = fenBefore,
                fenAfter = fenAfter
            )

            return newState.copy(
                outcome = outcome,
                winReason = winReason,
                isCheck = inCheck,
                moveHistory = state.moveHistory + moveRecord
            )
        }

        private fun applyMoveInternal(state: GameState, move: Move): GameState {
            val newBoard = state.copyBoard()
            val piece = newBoard[move.from.rank][move.from.file] ?: return state
            newBoard[move.from.rank][move.from.file] = null

            var newEpTarget: Square? = null
            var resetHalfmove = piece.type == PieceType.PAWN

            if (move.isEnPassant) {
                val capRank = if (piece.color == PieceColor.WHITE) move.to.rank - 1 else move.to.rank + 1
                newBoard[capRank][move.to.file] = null
                resetHalfmove = true
            } else if (move.isCastling) {
                // Move rook
                val rank = move.from.rank
                if (move.to.file == 6) { // Kingside
                    val rook = newBoard[rank][7]
                    newBoard[rank][7] = null
                    newBoard[rank][5] = rook
                } else if (move.to.file == 2) { // Queenside
                    val rook = newBoard[rank][0]
                    newBoard[rank][0] = null
                    newBoard[rank][3] = rook
                }
            } else if (piece.type == PieceType.PAWN) {
                if (Math.abs(move.to.rank - move.from.rank) == 2) {
                    val epRank = (move.from.rank + move.to.rank) / 2
                    newEpTarget = Square(move.from.file, epRank)
                }
            }

            if (newBoard[move.to.rank][move.to.file] != null) {
                resetHalfmove = true
            }

            val finalPiece = if (move.promotion != null) {
                Piece(move.promotion, piece.color)
            } else {
                piece
            }
            newBoard[move.to.rank][move.to.file] = finalPiece

            // Update Castling rights
            var wK = state.castlingRights.whiteKingside
            var wQ = state.castlingRights.whiteQueenside
            var bK = state.castlingRights.blackKingside
            var bQ = state.castlingRights.blackQueenside

            if (piece.type == PieceType.KING) {
                if (piece.color == PieceColor.WHITE) {
                    wK = false; wQ = false
                } else {
                    bK = false; bQ = false
                }
            }
            if (piece.type == PieceType.ROOK) {
                if (move.from == Square(0, 0)) wQ = false
                if (move.from == Square(7, 0)) wK = false
                if (move.from == Square(0, 7)) bQ = false
                if (move.from == Square(7, 7)) bK = false
            }
            // If rook was captured on original square
            if (move.to == Square(0, 0)) wQ = false
            if (move.to == Square(7, 0)) wK = false
            if (move.to == Square(0, 7)) bQ = false
            if (move.to == Square(7, 7)) bK = false

            val updatedCastling = CastlingRights(wK, wQ, bK, bQ)
            val updatedHalfmove = if (resetHalfmove) 0 else state.halfmoveClock + 1
            val updatedFullmove = if (state.turn == PieceColor.BLACK) state.fullmoveNumber + 1 else state.fullmoveNumber

            return GameState(
                board = newBoard,
                turn = state.turn.opposite(),
                castlingRights = updatedCastling,
                enPassantTarget = newEpTarget,
                halfmoveClock = updatedHalfmove,
                fullmoveNumber = updatedFullmove,
                moveHistory = state.moveHistory
            )
        }

        private fun generatePseudoMoves(state: GameState, color: PieceColor): List<Move> {
            val moves = mutableListOf<Move>()
            val board = state.board

            for (r in 0..7) {
                for (f in 0..7) {
                    val piece = board[r][f] ?: continue
                    if (piece.color != color) continue
                    val sq = Square(f, r)

                    when (piece.type) {
                        PieceType.PAWN -> generatePawnMoves(state, sq, piece, moves)
                        PieceType.KNIGHT -> generateKnightMoves(board, sq, color, moves)
                        PieceType.BISHOP -> generateSlidingMoves(board, sq, color, BISHOP_DIRS, moves)
                        PieceType.ROOK -> generateSlidingMoves(board, sq, color, ROOK_DIRS, moves)
                        PieceType.QUEEN -> generateSlidingMoves(board, sq, color, QUEEN_DIRS, moves)
                        PieceType.KING -> generateKingMoves(state, sq, color, moves)
                    }
                }
            }

            return moves
        }

        private fun generatePawnMoves(state: GameState, sq: Square, piece: Piece, moves: MutableList<Move>) {
            val dir = if (piece.color == PieceColor.WHITE) 1 else -1
            val startRank = if (piece.color == PieceColor.WHITE) 1 else 6
            val promoRank = if (piece.color == PieceColor.WHITE) 7 else 0

            // 1 square forward
            val forward1 = Square(sq.file, sq.rank + dir)
            if (forward1.isValid && state.board[forward1.rank][forward1.file] == null) {
                if (forward1.rank == promoRank) {
                    addPromotions(sq, forward1, moves)
                } else {
                    moves.add(Move(sq, forward1))
                    // 2 squares forward from starting rank
                    if (sq.rank == startRank) {
                        val forward2 = Square(sq.file, sq.rank + 2 * dir)
                        if (state.board[forward2.rank][forward2.file] == null) {
                            moves.add(Move(sq, forward2))
                        }
                    }
                }
            }

            // Captures (diagonal left & right)
            for (df in listOf(-1, 1)) {
                val capSq = Square(sq.file + df, sq.rank + dir)
                if (capSq.isValid) {
                    val targetPiece = state.board[capSq.rank][capSq.file]
                    if (targetPiece != null && targetPiece.color != piece.color) {
                        if (capSq.rank == promoRank) {
                            addPromotions(sq, capSq, moves)
                        } else {
                            moves.add(Move(sq, capSq))
                        }
                    } else if (state.enPassantTarget == capSq) {
                        // En Passant
                        moves.add(Move(sq, capSq, isEnPassant = true))
                    }
                }
            }
        }

        private fun addPromotions(from: Square, to: Square, moves: MutableList<Move>) {
            moves.add(Move(from, to, promotion = PieceType.QUEEN))
            moves.add(Move(from, to, promotion = PieceType.ROOK))
            moves.add(Move(from, to, promotion = PieceType.BISHOP))
            moves.add(Move(from, to, promotion = PieceType.KNIGHT))
        }

        private fun generateKnightMoves(board: Array<Array<Piece?>>, sq: Square, color: PieceColor, moves: MutableList<Move>) {
            val knightOffsets = listOf(
                Pair(-2, -1), Pair(-2, 1), Pair(-1, -2), Pair(-1, 2),
                Pair(1, -2), Pair(1, 2), Pair(2, -1), Pair(2, 1)
            )
            for ((df, dr) in knightOffsets) {
                val target = Square(sq.file + df, sq.rank + dr)
                if (target.isValid) {
                    val destPiece = board[target.rank][target.file]
                    if (destPiece == null || destPiece.color != color) {
                        moves.add(Move(sq, target))
                    }
                }
            }
        }

        private fun generateSlidingMoves(
            board: Array<Array<Piece?>>,
            sq: Square,
            color: PieceColor,
            directions: List<Pair<Int, Int>>,
            moves: MutableList<Move>
        ) {
            for ((df, dr) in directions) {
                var f = sq.file + df
                var r = sq.rank + dr
                while (f in 0..7 && r in 0..7) {
                    val destPiece = board[r][f]
                    val target = Square(f, r)
                    if (destPiece == null) {
                        moves.add(Move(sq, target))
                    } else {
                        if (destPiece.color != color) {
                            moves.add(Move(sq, target))
                        }
                        break
                    }
                    f += df
                    r += dr
                }
            }
        }

        private fun generateKingMoves(state: GameState, sq: Square, color: PieceColor, moves: MutableList<Move>) {
            val board = state.board
            for (df in -1..1) {
                for (dr in -1..1) {
                    if (df == 0 && dr == 0) continue
                    val target = Square(sq.file + df, sq.rank + dr)
                    if (target.isValid) {
                        val destPiece = board[target.rank][target.file]
                        if (destPiece == null || destPiece.color != color) {
                            moves.add(Move(sq, target))
                        }
                    }
                }
            }

            // Castling
            if (!isKingInCheck(state, color)) {
                val rank = if (color == PieceColor.WHITE) 0 else 7
                if (sq == Square(4, rank)) {
                    val cr = state.castlingRights
                    val canKingside = if (color == PieceColor.WHITE) cr.whiteKingside else cr.blackKingside
                    val canQueenside = if (color == PieceColor.WHITE) cr.whiteQueenside else cr.blackQueenside

                    // Kingside castling (squares 5 and 6 must be empty and not attacked)
                    if (canKingside &&
                        board[rank][5] == null && board[rank][6] == null &&
                        board[rank][7]?.type == PieceType.ROOK && board[rank][7]?.color == color &&
                        !isSquareAttacked(state, Square(5, rank), color.opposite()) &&
                        !isSquareAttacked(state, Square(6, rank), color.opposite())
                    ) {
                        moves.add(Move(sq, Square(6, rank), isCastling = true))
                    }

                    // Queenside castling (squares 1, 2, 3 must be empty, 2 and 3 not attacked)
                    if (canQueenside &&
                        board[rank][1] == null && board[rank][2] == null && board[rank][3] == null &&
                        board[rank][0]?.type == PieceType.ROOK && board[rank][0]?.color == color &&
                        !isSquareAttacked(state, Square(2, rank), color.opposite()) &&
                        !isSquareAttacked(state, Square(3, rank), color.opposite())
                    ) {
                        moves.add(Move(sq, Square(2, rank), isCastling = true))
                    }
                }
            }
        }

        fun isKingInCheck(state: GameState, color: PieceColor): Boolean {
            val kingSq = findKing(state.board, color) ?: return false
            return isSquareAttacked(state, kingSq, color.opposite())
        }

        private fun findKing(board: Array<Array<Piece?>>, color: PieceColor): Square? {
            for (r in 0..7) {
                for (f in 0..7) {
                    val p = board[r][f]
                    if (p?.type == PieceType.KING && p.color == color) {
                        return Square(f, r)
                    }
                }
            }
            return null
        }

        private fun isSquareAttacked(state: GameState, sq: Square, byColor: PieceColor): Boolean {
            val board = state.board

            // Pawns
            val pawnPushes = if (byColor == PieceColor.WHITE) -1 else 1
            for (df in listOf(-1, 1)) {
                val pSq = Square(sq.file + df, sq.rank + pawnPushes)
                if (pSq.isValid) {
                    val p = board[pSq.rank][pSq.file]
                    if (p?.type == PieceType.PAWN && p.color == byColor) return true
                }
            }

            // Knights
            val knightOffsets = listOf(
                Pair(-2, -1), Pair(-2, 1), Pair(-1, -2), Pair(-1, 2),
                Pair(1, -2), Pair(1, 2), Pair(2, -1), Pair(2, 1)
            )
            for ((df, dr) in knightOffsets) {
                val t = Square(sq.file + df, sq.rank + dr)
                if (t.isValid) {
                    val p = board[t.rank][t.file]
                    if (p?.type == PieceType.KNIGHT && p.color == byColor) return true
                }
            }

            // Kings
            for (df in -1..1) {
                for (dr in -1..1) {
                    if (df == 0 && dr == 0) continue
                    val t = Square(sq.file + df, sq.rank + dr)
                    if (t.isValid) {
                        val p = board[t.rank][t.file]
                        if (p?.type == PieceType.KING && p.color == byColor) return true
                    }
                }
            }

            // Diagonals (Bishops / Queens)
            for ((df, dr) in BISHOP_DIRS) {
                var f = sq.file + df
                var r = sq.rank + dr
                while (f in 0..7 && r in 0..7) {
                    val p = board[r][f]
                    if (p != null) {
                        if (p.color == byColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) {
                            return true
                        }
                        break
                    }
                    f += df
                    r += dr
                }
            }

            // Straights (Rooks / Queens)
            for ((df, dr) in ROOK_DIRS) {
                var f = sq.file + df
                var r = sq.rank + dr
                while (f in 0..7 && r in 0..7) {
                    val p = board[r][f]
                    if (p != null) {
                        if (p.color == byColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) {
                            return true
                        }
                        break
                    }
                    f += df
                    r += dr
                }
            }

            return false
        }

        private fun isInsufficientMaterial(board: Array<Array<Piece?>>): Boolean {
            val pieces = mutableListOf<Piece>()
            for (r in 0..7) {
                for (f in 0..7) {
                    val p = board[r][f]
                    if (p != null) pieces.add(p)
                }
            }

            if (pieces.size <= 2) return true // K vs K
            if (pieces.size == 3) {
                // K + N vs K or K + B vs K
                val nonKings = pieces.filter { it.type != PieceType.KING }
                if (nonKings.size == 1 && (nonKings[0].type == PieceType.KNIGHT || nonKings[0].type == PieceType.BISHOP)) {
                    return true
                }
            }
            return false
        }

        private fun computeSan(state: GameState, move: Move): String {
            val piece = state.getPiece(move.from) ?: return move.uci
            if (move.isCastling) {
                return if (move.to.file == 6) "O-O" else "O-O-O"
            }

            val sb = StringBuilder()
            val isCapture = state.getPiece(move.to) != null || move.isEnPassant

            if (piece.type == PieceType.PAWN) {
                if (isCapture) {
                    sb.append(('a' + move.from.file))
                    sb.append('x')
                }
                sb.append(move.to.algebraic)
                if (move.promotion != null) {
                    sb.append('=').append(move.promotion.symbol)
                }
            } else {
                sb.append(piece.type.symbol)
                // Disambiguation
                val legalMoves = getLegalMoves(state)
                val conflicts = legalMoves.filter {
                    it.from != move.from && it.to == move.to && state.getPiece(it.from)?.type == piece.type
                }
                if (conflicts.isNotEmpty()) {
                    val sameFile = conflicts.any { it.from.file == move.from.file }
                    val sameRank = conflicts.any { it.from.rank == move.from.rank }
                    if (!sameFile) {
                        sb.append(('a' + move.from.file))
                    } else if (!sameRank) {
                        sb.append(move.from.rank + 1)
                    } else {
                        sb.append(move.from.algebraic)
                    }
                }

                if (isCapture) {
                    sb.append('x')
                }
                sb.append(move.to.algebraic)
            }

            return sb.toString()
        }

        private val BISHOP_DIRS = listOf(Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1))
        private val ROOK_DIRS = listOf(Pair(0, 1), Pair(0, -1), Pair(1, 0), Pair(-1, 0))
        private val QUEEN_DIRS = BISHOP_DIRS + ROOK_DIRS
    }
}
