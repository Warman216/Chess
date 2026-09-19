package com.example.chess.ai

import com.example.chess.engine.ChessEngine
import com.example.chess.engine.GameState
import com.example.chess.model.GameOutcome
import com.example.chess.model.Move
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

enum class AiDifficulty(val displayName: String, val description: String, val ratingEstimate: Int) {
    CASUAL("Casual", "Relaxed play, great for learning tactics", 1100),
    CLUB("Club Player", "Solid positional and tactical play", 1650),
    GRANDMASTER("Grandmaster Gemini", "Deep calculation powered by Gemini AI", 2200)
}

data class AiMoveResult(
    val move: Move,
    val commentary: String,
    val evaluationScore: Int
)

class HeuristicChessAi {

    suspend fun selectMove(state: GameState, difficulty: AiDifficulty): AiMoveResult = withContext(Dispatchers.Default) {
        val legalMoves = ChessEngine.getLegalMoves(state)
        if (legalMoves.isEmpty()) {
            throw IllegalStateException("No legal moves available")
        }

        val aiColor = state.turn
        val depth = when (difficulty) {
            AiDifficulty.CASUAL -> 2
            AiDifficulty.CLUB -> 3
            AiDifficulty.GRANDMASTER -> 4
        }

        // Move ordering: sort captures and promotions first
        val sortedMoves = orderMoves(state, legalMoves)

        if (difficulty == AiDifficulty.CASUAL && Random.nextFloat() < 0.25f && sortedMoves.size > 1) {
            // Pick from top 3 moves to simulate human casual play
            val candidates = sortedMoves.take(minOf(3, sortedMoves.size))
            val chosen = candidates[Random.nextInt(candidates.size)]
            val commentary = generateCommentary(state, chosen, 0)
            return@withContext AiMoveResult(chosen, commentary, 0)
        }

        var bestMove = sortedMoves[0]
        var bestScore = if (aiColor == PieceColor.WHITE) Int.MIN_VALUE + 100 else Int.MAX_VALUE - 100
        var alpha = Int.MIN_VALUE + 100
        var beta = Int.MAX_VALUE - 100

        for (move in sortedMoves) {
            val nextState = ChessEngine.applyMove(state, move)
            val score = minimax(nextState, depth - 1, alpha, beta, aiColor == PieceColor.BLACK)

            if (aiColor == PieceColor.WHITE) {
                if (score > bestScore) {
                    bestScore = score
                    bestMove = move
                }
                alpha = maxOf(alpha, bestScore)
            } else {
                if (score < bestScore) {
                    bestScore = score
                    bestMove = move
                }
                beta = minOf(beta, bestScore)
            }

            if (beta <= alpha) break
        }

        val commentary = generateCommentary(state, bestMove, bestScore)
        AiMoveResult(bestMove, commentary, bestScore)
    }

    private fun minimax(state: GameState, depth: Int, alpha: Int, beta: Int, isMaximizing: Boolean): Int {
        if (state.outcome != GameOutcome.ONGOING) {
            return when (state.outcome) {
                GameOutcome.WHITE_WINS -> 100000 + depth
                GameOutcome.BLACK_WINS -> -100000 - depth
                GameOutcome.DRAW -> 0
                else -> 0
            }
        }

        if (depth <= 0) {
            return evaluatePosition(state)
        }

        val legalMoves = ChessEngine.getLegalMoves(state)
        if (legalMoves.isEmpty()) {
            return if (state.isCheck) {
                if (isMaximizing) -100000 - depth else 100000 + depth
            } else 0
        }

        val sortedMoves = orderMoves(state, legalMoves)
        var curAlpha = alpha
        var curBeta = beta

        if (isMaximizing) {
            var maxEval = Int.MIN_VALUE + 100
            for (move in sortedMoves) {
                val nextState = ChessEngine.applyMove(state, move)
                val eval = minimax(nextState, depth - 1, curAlpha, curBeta, false)
                maxEval = maxOf(maxEval, eval)
                curAlpha = maxOf(curAlpha, eval)
                if (curBeta <= curAlpha) break
            }
            return maxEval
        } else {
            var minEval = Int.MAX_VALUE - 100
            for (move in sortedMoves) {
                val nextState = ChessEngine.applyMove(state, move)
                val eval = minimax(nextState, depth - 1, curAlpha, curBeta, true)
                minEval = minOf(minEval, eval)
                curBeta = minOf(curBeta, eval)
                if (curBeta <= curAlpha) break
            }
            return minEval
        }
    }

    private fun orderMoves(state: GameState, moves: List<Move>): List<Move> {
        return moves.sortedByDescending { move ->
            var score = 0
            val destPiece = state.getPiece(move.to)
            val srcPiece = state.getPiece(move.from)

            if (destPiece != null && srcPiece != null) {
                // MVV-LVA: Most Valuable Victim, Least Valuable Attacker
                score += destPiece.type.baseValue * 10 - srcPiece.type.baseValue
            }
            if (move.promotion != null) {
                score += 800
            }
            if (move.isCastling) {
                score += 150
            }
            score
        }
    }

    fun evaluatePosition(state: GameState): Int {
        var score = 0
        val board = state.board

        var totalPieces = 0
        for (r in 0..7) {
            for (f in 0..7) {
                val piece = board[r][f] ?: continue
                totalPieces++
                val base = piece.type.baseValue
                val pst = getPstValue(piece.type, piece.color, f, r)
                val pieceVal = base + pst

                if (piece.color == PieceColor.WHITE) {
                    score += pieceVal
                } else {
                    score -= pieceVal
                }
            }
        }

        // Center control bonus
        val center = listOf(Pair(3, 3), Pair(3, 4), Pair(4, 3), Pair(4, 4))
        for ((f, r) in center) {
            val p = board[r][f] ?: continue
            if (p.color == PieceColor.WHITE) score += 20 else score -= 20
        }

        return score
    }

    private fun getPstValue(type: PieceType, color: PieceColor, file: Int, rank: Int): Int {
        val r = if (color == PieceColor.WHITE) rank else 7 - rank
        val f = file

        return when (type) {
            PieceType.PAWN -> PAWN_PST[r][f]
            PieceType.KNIGHT -> KNIGHT_PST[r][f]
            PieceType.BISHOP -> BISHOP_PST[r][f]
            PieceType.ROOK -> ROOK_PST[r][f]
            PieceType.QUEEN -> QUEEN_PST[r][f]
            PieceType.KING -> KING_PST[r][f]
        }
    }

    fun generateCommentary(state: GameState, move: Move, score: Int): String {
        val piece = state.getPiece(move.from) ?: return "Calculated position carefully."
        val captured = state.getPiece(move.to)

        if (move.isCastling) {
            return "Castled to tuck the king into safety and activate the rook on the open file."
        }
        if (captured != null) {
            return "Captured ${captured.color.name.lowercase()} ${captured.type.name.lowercase()} on ${move.to.algebraic} to gain tactical initiative."
        }
        if (piece.type == PieceType.KNIGHT) {
            return "Repositioned knight to ${move.to.algebraic} to exert control over critical outpost squares."
        }
        if (piece.type == PieceType.BISHOP) {
            return "Placed bishop along the diagonal towards ${move.to.algebraic} to increase piece mobility."
        }
        if (piece.type == PieceType.PAWN) {
            return if (move.to.file in 3..4 && move.to.rank in 3..4) {
                "Pushed pawn to ${move.to.algebraic} to establish a firm claim on the center."
            } else {
                "Advanced pawn to ${move.to.algebraic} to challenge your pawn structure."
            }
        }
        if (piece.type == PieceType.ROOK) {
            return "Shifted rook to ${move.to.algebraic} to command this vertical corridor."
        }
        return "Advanced ${piece.type.name.lowercase()} to ${move.to.algebraic} to coordinate tactical pressure."
    }

    companion object {
        private val PAWN_PST = arrayOf(
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(50, 50, 50, 50, 50, 50, 50, 50),
            intArrayOf(10, 10, 20, 30, 30, 20, 10, 10),
            intArrayOf(5, 5, 10, 25, 25, 10, 5, 5),
            intArrayOf(0, 0, 0, 20, 20, 0, 0, 0),
            intArrayOf(5, -5, -10, 0, 0, -10, -5, 5),
            intArrayOf(5, 10, 10, -20, -20, 10, 10, 5),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
        )

        private val KNIGHT_PST = arrayOf(
            intArrayOf(-50, -40, -30, -30, -30, -30, -40, -50),
            intArrayOf(-40, -20, 0, 0, 0, 0, -20, -40),
            intArrayOf(-30, 0, 10, 15, 15, 10, 0, -30),
            intArrayOf(-30, 5, 15, 20, 20, 15, 5, -30),
            intArrayOf(-30, 0, 15, 20, 20, 15, 0, -30),
            intArrayOf(-30, 5, 10, 15, 15, 10, 5, -30),
            intArrayOf(-40, -20, 0, 5, 5, 0, -20, -40),
            intArrayOf(-50, -40, -30, -30, -30, -30, -40, -50)
        )

        private val BISHOP_PST = arrayOf(
            intArrayOf(-20, -10, -10, -10, -10, -10, -10, -20),
            intArrayOf(-10, 0, 0, 0, 0, 0, 0, -10),
            intArrayOf(-10, 0, 5, 10, 10, 5, 0, -10),
            intArrayOf(-10, 5, 5, 10, 10, 5, 5, -10),
            intArrayOf(-10, 0, 10, 10, 10, 10, 0, -10),
            intArrayOf(-10, 10, 10, 10, 10, 10, 10, -10),
            intArrayOf(-10, 5, 0, 0, 0, 0, 5, -10),
            intArrayOf(-20, -10, -10, -10, -10, -10, -10, -20)
        )

        private val ROOK_PST = arrayOf(
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(5, 10, 10, 10, 10, 10, 10, 5),
            intArrayOf(-5, 0, 0, 0, 0, 0, 0, -5),
            intArrayOf(-5, 0, 0, 0, 0, 0, 0, -5),
            intArrayOf(-5, 0, 0, 0, 0, 0, 0, -5),
            intArrayOf(-5, 0, 0, 0, 0, 0, 0, -5),
            intArrayOf(-5, 0, 0, 0, 0, 0, 0, -5),
            intArrayOf(0, 0, 0, 5, 5, 0, 0, 0)
        )

        private val QUEEN_PST = arrayOf(
            intArrayOf(-20, -10, -10, -5, -5, -10, -10, -20),
            intArrayOf(-10, 0, 0, 0, 0, 0, 0, -10),
            intArrayOf(-10, 0, 5, 5, 5, 5, 0, -10),
            intArrayOf(-5, 0, 5, 5, 5, 5, 0, -5),
            intArrayOf(0, 0, 5, 5, 5, 5, 0, -5),
            intArrayOf(-10, 5, 5, 5, 5, 5, 0, -10),
            intArrayOf(-10, 0, 5, 0, 0, 0, 0, -10),
            intArrayOf(-20, -10, -10, -5, -5, -10, -10, -20)
        )

        private val KING_PST = arrayOf(
            intArrayOf(-30, -40, -40, -50, -50, -40, -40, -30),
            intArrayOf(-30, -40, -40, -50, -50, -40, -40, -30),
            intArrayOf(-30, -40, -40, -50, -50, -40, -40, -30),
            intArrayOf(-30, -40, -40, -50, -50, -40, -40, -30),
            intArrayOf(-20, -30, -30, -40, -40, -30, -30, -20),
            intArrayOf(-10, -20, -20, -20, -20, -20, -20, -10),
            intArrayOf(20, 20, 0, 0, 0, 0, 20, 20),
            intArrayOf(20, 30, 10, 0, 0, 10, 30, 20)
        )
    }
}
