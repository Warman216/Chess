package com.example

import com.example.chess.ai.AiDifficulty
import com.example.chess.ai.HeuristicChessAi
import com.example.chess.engine.ChessEngine
import com.example.chess.model.GameOutcome
import com.example.chess.model.Move
import com.example.chess.model.PieceColor
import com.example.chess.model.Square
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChessEngineUnitTest {

    @Test
    fun initialPosition_has20LegalMoves() {
        val initial = ChessEngine.createInitialState()
        val legalMoves = ChessEngine.getLegalMoves(initial)
        // 8 pawns * 2 moves each (1 or 2 squares) = 16 moves + 2 knights * 2 moves = 4 moves => 20 moves
        assertEquals(20, legalMoves.size)
        assertFalse(initial.isCheck)
        assertEquals(GameOutcome.ONGOING, initial.outcome)
    }

    @Test
    fun applyMove_updatesTurnAndHistory() {
        val initial = ChessEngine.createInitialState()
        val e2e4 = Move(Square(4, 1), Square(4, 3)) // e2 to e4
        val next = ChessEngine.applyMove(initial, e2e4)

        assertEquals(PieceColor.BLACK, next.turn)
        assertEquals(1, next.moveHistory.size)
        assertEquals("e4", next.moveHistory[0].san)
        assertEquals(Square(4, 2), next.enPassantTarget) // en passant square on e3
    }

    @Test
    fun aiSelectsLegalMove() = kotlinx.coroutines.runBlocking {
        val initial = ChessEngine.createInitialState()
        val ai = HeuristicChessAi()
        val result = ai.selectMove(initial, AiDifficulty.CLUB)

        assertNotNull(result.move)
        val legalMoves = ChessEngine.getLegalMoves(initial)
        assertTrue(legalMoves.any { it.from == result.move.from && it.to == result.move.to })
    }

    @Test
    fun fenRoundTrip_matchesCorrectly() {
        val initial = ChessEngine.createInitialState()
        val fen = ChessEngine.toFen(initial)
        val expected = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        assertEquals(expected, fen)

        val restored = ChessEngine.fromFen(fen)
        assertEquals(fen, ChessEngine.toFen(restored))
    }
}
