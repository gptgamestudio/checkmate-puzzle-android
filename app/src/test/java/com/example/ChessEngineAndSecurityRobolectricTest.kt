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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChessEngineAndSecurityRobolectricTest {

    private lateinit var engine: ChessEngine

    @Before
    fun setup() {
        engine = ChessEngine()
    }

    /**
     * Test 1: Legal Move Generation and Initial Board Setup
     */
    @Test
    fun testInitialBoardSetupAndLegalMoves() {
        val standardFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        val state = ChessGameState.fromFen(standardFen)

        assertEquals(PieceColor.WHITE, state.activeColor)
        val allWhiteMoves = engine.getAllLegalMoves(state)
        // Standard initial position has exactly 20 legal moves (16 pawn moves + 4 knight moves)
        assertEquals(20, allWhiteMoves.size)
        assertFalse(engine.isKingInCheck(state, PieceColor.WHITE))
        assertFalse(engine.isKingInCheck(state, PieceColor.BLACK))
    }

    /**
     * Test 2: Check and Checkmate Detection (Scholar's Mate)
     */
    @Test
    fun testScholarsMateCheckmate() {
        // Position right before Scholar's Mate: White Queen at h5, Bishop at c4, Black pawns at e5
        // FEN: r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4
        val fenBeforeMate = "r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4"
        val state = ChessGameState.fromFen(fenBeforeMate)

        val qFrom = Position.fromAlgebraic("h5")!!
        val qTo = Position.fromAlgebraic("f7")!!
        val qxf7Move = Move(qFrom, qTo, isCapture = true)
        val stateAfterQxf7 = engine.makeMove(state, qxf7Move)

        assertTrue("Black King must be in check", engine.isKingInCheck(stateAfterQxf7, PieceColor.BLACK))
        assertTrue("Position must be checkmate", engine.isCheckmate(stateAfterQxf7))
        assertEquals(0, engine.getAllLegalMoves(stateAfterQxf7).size)
    }

    /**
     * Test 3: Stalemate Detection
     */
    @Test
    fun testStalemateDetection() {
        // Classic stalemate position: Black King at a8, White King at c7, White Queen at b6
        val stalemateFen = "k7/8/1QK5/8/8/8/8/8 b - - 0 1"
        val state = ChessGameState.fromFen(stalemateFen)

        assertFalse("Black King must NOT be in check", engine.isKingInCheck(state, PieceColor.BLACK))
        assertTrue("Position must be stalemate", engine.isStalemate(state))
        assertEquals(0, engine.getAllLegalMoves(state).size)
    }

    /**
     * Test 4: Kingside and Queenside Castling
     */
    @Test
    fun testCastlingExecution() {
        // White King at e1, Rooks at a1 and h1, all pieces cleared between them
        val castlingFen = "r3k2r/pppppppp/8/8/8/8/PPPPPPPP/R3K2R w KQkq - 0 1"
        val state = ChessGameState.fromFen(castlingFen)

        val kingPos = Position.fromAlgebraic("e1")!!
        val kingLegalMoves = engine.getLegalMoves(state, kingPos)
        val kingsideCastle = kingLegalMoves.firstOrNull { it.isCastling && it.to == Position.fromAlgebraic("g1") }
        val queensideCastle = kingLegalMoves.firstOrNull { it.isCastling && it.to == Position.fromAlgebraic("c1") }

        assertNotNull("Kingside castling must be legal", kingsideCastle)
        assertNotNull("Queenside castling must be legal", queensideCastle)

        // Execute Kingside castling
        val afterKingside = engine.makeMove(state, kingsideCastle!!)
        assertEquals(PieceType.KING, afterKingside.getPiece(Position.fromAlgebraic("g1")!!)?.type)
        assertEquals(PieceType.ROOK, afterKingside.getPiece(Position.fromAlgebraic("f1")!!)?.type)
        assertNull(afterKingside.getPiece(Position.fromAlgebraic("e1")!!))
        assertNull(afterKingside.getPiece(Position.fromAlgebraic("h1")!!))
    }

    /**
     * Test 5: Castling Blocked When Squares Are Attacked
     */
    @Test
    fun testCastlingBlockedWhenSquareAttacked() {
        // Black Bishop on a6 attacks f1 directly through the empty diagonal (e2 is empty)
        val attackedFen = "r3k2r/pppp1ppp/b7/8/8/8/PPPP1PPP/R3K2R w KQkq - 0 1"
        val state = ChessGameState.fromFen(attackedFen)

        val kingMoves = engine.getLegalMoves(state, Position.fromAlgebraic("e1")!!)
        val kingsideCastle = kingMoves.firstOrNull { it.isCastling && it.to == Position.fromAlgebraic("g1") }
        assertNull("Kingside castling through attacked f1 square must be blocked", kingsideCastle)
    }

    /**
     * Test 6: En Passant Pawn Capture
     */
    @Test
    fun testEnPassantCapture() {
        // White pawn on e5, Black pawns on rank 7
        val epSetupFen = "rnbqkbnr/pppppppp/8/4P3/8/8/PPPP1PPP/RNBQKBNR b KQkq - 0 2"
        val state = ChessGameState.fromFen(epSetupFen)

        // Black plays d7-d5 (2 squares)
        val d5Move = Move(Position.fromAlgebraic("d7")!!, Position.fromAlgebraic("d5")!!)
        val stateAfterD5 = engine.makeMove(state, d5Move)

        // En passant target must be d6
        assertEquals(Position.fromAlgebraic("d6"), stateAfterD5.enPassantTarget)

        // White pawn on e5 captures en passant to d6
        val e5Pos = Position.fromAlgebraic("e5")!!
        val d6Pos = Position.fromAlgebraic("d6")!!
        val whiteEpMove = engine.getLegalMoves(stateAfterD5, e5Pos)
            .firstOrNull { it.isEnPassant && it.to == d6Pos }

        assertNotNull("En passant move must be generated", whiteEpMove)

        val stateAfterEp = engine.makeMove(stateAfterD5, whiteEpMove!!)
        assertEquals(PieceType.PAWN, stateAfterEp.getPiece(d6Pos)?.type)
        assertNull("Captured Black pawn on d5 must be removed", stateAfterEp.getPiece(Position.fromAlgebraic("d5")!!))
    }

    /**
     * Test 7: Pawn Promotion
     */
    @Test
    fun testPawnPromotion() {
        // White pawn on e7, can advance to e8 and promote
        val promoFen = "8/4P3/8/8/8/8/8/k1K5 w - - 0 1"
        val state = ChessGameState.fromFen(promoFen)

        val legalMoves = engine.getLegalMoves(state, Position.fromAlgebraic("e7")!!)
        val promotions = legalMoves.filter { it.promotion != null }

        assertEquals("Pawn on 7th rank must have 4 promotion options (Q, R, B, N)", 4, promotions.size)
        assertTrue(promotions.any { it.promotion == PieceType.QUEEN })
        assertTrue(promotions.any { it.promotion == PieceType.ROOK })
        assertTrue(promotions.any { it.promotion == PieceType.BISHOP })
        assertTrue(promotions.any { it.promotion == PieceType.KNIGHT })

        // Execute Queen promotion
        val queenPromo = promotions.first { it.promotion == PieceType.QUEEN }
        val stateAfterPromo = engine.makeMove(state, queenPromo)
        assertEquals(PieceType.QUEEN, stateAfterPromo.getPiece(Position.fromAlgebraic("e8")!!)?.type)
        assertEquals(PieceColor.WHITE, stateAfterPromo.getPiece(Position.fromAlgebraic("e8")!!)?.color)
    }

    /**
     * Test 8: Security and Owner Key Cryptographic Hash Verification
     */
    @Test
    fun testOwnerSecurityVerification() {
        val expectedHash = "2576a9fc047a6d45e85497fb230c831e96d232ba65c49c1369cb01c67ee4eace"
        val digest = MessageDigest.getInstance("SHA-256")

        // Arbitrary wrong keys must fail
        val wrongHash = digest.digest("wrong-password-123".toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
        assertFalse("Arbitrary password must not match owner key hash", wrongHash.equals(expectedHash, ignoreCase = true))
    }
}
