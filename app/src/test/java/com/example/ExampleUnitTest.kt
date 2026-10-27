package com.example

import com.example.engine.ChessEngine
import com.example.engine.ChessGameState
import com.example.model.Move
import com.example.model.PieceColor
import com.example.model.PieceType
import com.example.model.Position
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    private val engine = ChessEngine()

    @Test
    fun testFenLoading() {
        // Back rank position: White Rook on a1, White King g1, Black King g8, Pawns f7, g7, h7
        val fen = "6k1/5ppp/8/8/8/8/5PPP/R5K1 w - - 0 1"
        val state = ChessGameState.fromFen(fen)

        assertEquals(PieceColor.WHITE, state.activeColor)
        val rook = state.getPiece(Position.fromAlgebraic("a1")!!)
        assertNotNull(rook)
        assertEquals(PieceType.ROOK, rook?.type)
        assertEquals(PieceColor.WHITE, rook?.color)

        val blackKing = state.getPiece(Position.fromAlgebraic("g8")!!)
        assertNotNull(blackKing)
        assertEquals(PieceType.KING, blackKing?.type)
        assertEquals(PieceColor.BLACK, blackKing?.color)
    }

    @Test
    fun testBackRankCheckmate() {
        val fen = "6k1/5ppp/8/8/8/8/5PPP/R5K1 w - - 0 1"
        val state = ChessGameState.fromFen(fen)

        val move = Move(Position.fromAlgebraic("a1")!!, Position.fromAlgebraic("a8")!!)
        val legalMoves = engine.getLegalMoves(state, move.from)
        assertTrue(legalMoves.any { it.to == move.to })

        val nextState = engine.makeMove(state, move)
        assertEquals(PieceColor.BLACK, nextState.activeColor)
        assertTrue("Black King must be in check", engine.isKingInCheck(nextState, PieceColor.BLACK))
        assertTrue("Black King must be in checkmate", engine.isCheckmate(nextState))
    }

    @Test
    fun testScholarCheckmate() {
        val fen = "r1bqkb1r/pppp1ppp/2n5/4p3/2B1n3/5Q2/PPPP1PPP/RNB1K1NR w KQkq - 0 1"
        val state = ChessGameState.fromFen(fen)

        val move = Move(Position.fromAlgebraic("f3")!!, Position.fromAlgebraic("f7")!!)
        val legalMoves = engine.getLegalMoves(state, move.from)
        assertTrue(legalMoves.any { it.to == move.to })

        val nextState = engine.makeMove(state, move)
        assertTrue("Black King must be in checkmate after Qxf7#", engine.isCheckmate(nextState))
    }

    @Test
    fun testSmotheredMate() {
        val fen = "6rk/6pp/3N4/8/8/8/8/4Q1K1 w - - 0 1"
        val state = ChessGameState.fromFen(fen)

        val move = Move(Position.fromAlgebraic("d6")!!, Position.fromAlgebraic("f7")!!)
        val legalMoves = engine.getLegalMoves(state, move.from)
        assertTrue(legalMoves.any { it.to == move.to })

        val nextState = engine.makeMove(state, move)
        assertTrue("Black King must be in checkmate after Nf7#", engine.isCheckmate(nextState))
    }
}
