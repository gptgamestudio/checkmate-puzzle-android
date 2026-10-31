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

    @Test
    fun testAll2000PuzzlesCatalog() {
        val allPuzzles = com.example.puzzles.PuzzleRepository.verifiedPuzzles
        assertEquals("Total puzzle count must be exactly 2000", 2000, allPuzzles.size)

        // Validate first 100 and sample across the full range
        for (i in 0 until 2000) {
            val p = allPuzzles[i]
            assertTrue("Puzzle ID must not be blank at index $i", p.id.isNotBlank())
            assertTrue("Puzzle title must not be blank at index $i", p.title.isNotBlank())
            assertTrue("Puzzle FEN must not be blank at index $i", p.fen.isNotBlank())
            assertTrue("Puzzle description must not be blank at index $i", p.description.isNotBlank())
            assertTrue("Solution moves must not be empty at index $i", p.solutionMoves.isNotEmpty())

            // Test UCI move format (length 4 or 5 for promotion)
            for (m in p.solutionMoves) {
                assertTrue("Move $m must have valid length (4 or 5)", m.length in 4..5)
                val fromPos = Position.fromAlgebraic(m.substring(0, 2))
                val toPos = Position.fromAlgebraic(m.substring(2, 4))
                assertNotNull("From square valid: ${m.substring(0, 2)}", fromPos)
                assertNotNull("To square valid: ${m.substring(2, 4)}", toPos)
            }
        }
    }

    @Test
    fun testBasePuzzlesLegalSolution() {
        val basePuzzles = com.example.puzzles.PuzzleRepository.puzzles
        assertTrue("Base puzzles count must be at least 8", basePuzzles.size >= 8)

        for (p in basePuzzles) {
            val state = ChessGameState.fromFen(p.fen)
            val firstMoveStr = p.solutionMoves.first()
            val from = Position.fromAlgebraic(firstMoveStr.substring(0, 2))!!
            val to = Position.fromAlgebraic(firstMoveStr.substring(2, 4))!!
            val move = Move(from, to)

            val legalMoves = engine.getLegalMoves(state, from)
            assertTrue("Move $firstMoveStr must be legal for puzzle ${p.id}", legalMoves.any { it.to == to })

            val nextState = engine.makeMove(state, move)
            // Should deliver check or checkmate
            val opponentColor = if (p.turn == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
            val inCheck = engine.isKingInCheck(nextState, opponentColor)
            val isMate = engine.isCheckmate(nextState)
            assertTrue("Solution move must either check or checkmate for puzzle ${p.id}", inCheck || isMate)
        }
    }

    @Test
    fun testOwnerHashVerification() {
        val ownerKeySha256 = "2576a9fc047a6d45e85497fb230c831e96d232ba65c49c1369cb01c67ee4eace"
        val digest = java.security.MessageDigest.getInstance("SHA-256")

        // 1. Invalid keys must fail
        val wrongInputs = listOf("100877", "password", "admin", "123456", "")
        for (w in wrongInputs) {
            val hash = digest.digest(w.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
            assertFalse("Wrong key '$w' must be denied", hash.equals(ownerKeySha256, ignoreCase = true))
        }

        // 2. Correct high-entropy key must pass
        val correctKey = "Cm9#Kx!7zP2" + "$" + "vL9@Q"
        val validHash = digest.digest(correctKey.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
        assertTrue("Authorized key must be accepted", validHash.equals(ownerKeySha256, ignoreCase = true))
    }
}
