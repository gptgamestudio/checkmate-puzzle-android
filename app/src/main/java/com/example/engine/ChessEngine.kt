package com.example.engine

import com.example.model.Move
import com.example.model.Piece
import com.example.model.PieceColor
import com.example.model.PieceType
import com.example.model.Position
import kotlin.math.abs

class ChessGameState(
    val board: Array<Array<Piece?>> = Array(8) { Array(8) { null } },
    var activeColor: PieceColor = PieceColor.WHITE,
    var whiteCastlingKingSide: Boolean = true,
    var whiteCastlingQueenSide: Boolean = true,
    var blackCastlingKingSide: Boolean = true,
    var blackCastlingQueenSide: Boolean = true,
    var enPassantTarget: Position? = null,
    var halfmoveClock: Int = 0,
    var fullmoveNumber: Int = 1
) {
    fun copy(): ChessGameState {
        val newBoard = Array(8) { r ->
            Array(8) { c -> board[r][c] }
        }
        return ChessGameState(
            board = newBoard,
            activeColor = activeColor,
            whiteCastlingKingSide = whiteCastlingKingSide,
            whiteCastlingQueenSide = whiteCastlingQueenSide,
            blackCastlingKingSide = blackCastlingKingSide,
            blackCastlingQueenSide = blackCastlingQueenSide,
            enPassantTarget = enPassantTarget,
            halfmoveClock = halfmoveClock,
            fullmoveNumber = fullmoveNumber
        )
    }

    fun getPiece(pos: Position): Piece? {
        if (!pos.isValid) return null
        return board[pos.row][pos.col]
    }

    fun setPiece(pos: Position, piece: Piece?) {
        if (pos.isValid) {
            board[pos.row][pos.col] = piece
        }
    }

    fun findKing(color: PieceColor): Position? {
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board[r][c]
                if (p != null && p.type == PieceType.KING && p.color == color) {
                    return Position(c, r)
                }
            }
        }
        return null
    }

    companion object {
        fun fromFen(fen: String): ChessGameState {
            val parts = fen.trim().split("\\s+".toRegex())
            val state = ChessGameState()
            val piecePlacement = parts.getOrNull(0) ?: "8/8/8/8/8/8/8/8"
            val rows = piecePlacement.split('/')
            for (r in 0 until minOf(8, rows.size)) {
                val rowStr = rows[r]
                var c = 0
                for (ch in rowStr) {
                    if (ch.isDigit()) {
                        c += ch.digitToInt()
                    } else {
                        val piece = Piece.fromChar(ch)
                        if (c in 0..7) {
                            state.board[r][c] = piece
                        }
                        c++
                    }
                }
            }

            state.activeColor = if (parts.getOrNull(1) == "b") PieceColor.BLACK else PieceColor.WHITE

            val castling = parts.getOrNull(2) ?: "-"
            state.whiteCastlingKingSide = castling.contains('K')
            state.whiteCastlingQueenSide = castling.contains('Q')
            state.blackCastlingKingSide = castling.contains('k')
            state.blackCastlingQueenSide = castling.contains('q')

            val ep = parts.getOrNull(3)
            state.enPassantTarget = if (ep != null && ep != "-") Position.fromAlgebraic(ep) else null
            state.halfmoveClock = parts.getOrNull(4)?.toIntOrNull() ?: 0
            state.fullmoveNumber = parts.getOrNull(5)?.toIntOrNull() ?: 1

            return state
        }
    }
}

class ChessEngine {

    fun isSquareAttacked(state: ChessGameState, target: Position, byColor: PieceColor): Boolean {
        // Check Knight attacks
        val knightOffsets = listOf(
            Pair(1, 2), Pair(2, 1), Pair(-1, 2), Pair(-2, 1),
            Pair(1, -2), Pair(2, -1), Pair(-1, -2), Pair(-2, -1)
        )
        for ((dc, dr) in knightOffsets) {
            val p = Position(target.col + dc, target.row + dr)
            if (p.isValid) {
                val piece = state.getPiece(p)
                if (piece != null && piece.color == byColor && piece.type == PieceType.KNIGHT) {
                    return true
                }
            }
        }

        // Check Pawn attacks
        val pawnDir = if (byColor == PieceColor.WHITE) 1 else -1 // white moves up (row decreasing), attacks from row+1
        val pawnAttackingRow = target.row + pawnDir
        if (pawnAttackingRow in 0..7) {
            for (dc in listOf(-1, 1)) {
                val p = Position(target.col + dc, pawnAttackingRow)
                if (p.isValid) {
                    val piece = state.getPiece(p)
                    if (piece != null && piece.color == byColor && piece.type == PieceType.PAWN) {
                        return true
                    }
                }
            }
        }

        // Check King attacks (adjacent squares)
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val p = Position(target.col + dc, target.row + dr)
                if (p.isValid) {
                    val piece = state.getPiece(p)
                    if (piece != null && piece.color == byColor && piece.type == PieceType.KING) {
                        return true
                    }
                }
            }
        }

        // Check straight lines (Rook / Queen)
        val straightDirs = listOf(Pair(0, 1), Pair(0, -1), Pair(1, 0), Pair(-1, 0))
        for ((dc, dr) in straightDirs) {
            var step = 1
            while (true) {
                val p = Position(target.col + dc * step, target.row + dr * step)
                if (!p.isValid) break
                val piece = state.getPiece(p)
                if (piece != null) {
                    if (piece.color == byColor && (piece.type == PieceType.ROOK || piece.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                step++
            }
        }

        // Check diagonals (Bishop / Queen)
        val diagDirs = listOf(Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1))
        for ((dc, dr) in diagDirs) {
            var step = 1
            while (true) {
                val p = Position(target.col + dc * step, target.row + dr * step)
                if (!p.isValid) break
                val piece = state.getPiece(p)
                if (piece != null) {
                    if (piece.color == byColor && (piece.type == PieceType.BISHOP || piece.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                step++
            }
        }

        return false
    }

    fun isKingInCheck(state: ChessGameState, color: PieceColor): Boolean {
        val kingPos = state.findKing(color) ?: return false
        return isSquareAttacked(state, kingPos, color.opposite())
    }

    fun getPseudoLegalMoves(state: ChessGameState, from: Position): List<Move> {
        val piece = state.getPiece(from) ?: return emptyList()
        val moves = mutableListOf<Move>()
        val color = piece.color
        val oppColor = color.opposite()

        when (piece.type) {
            PieceType.PAWN -> {
                val forward = if (color == PieceColor.WHITE) -1 else 1
                val startRow = if (color == PieceColor.WHITE) 6 else 1
                val promoRow = if (color == PieceColor.WHITE) 0 else 7

                // 1 step forward
                val oneStep = Position(from.col, from.row + forward)
                if (oneStep.isValid && state.getPiece(oneStep) == null) {
                    if (oneStep.row == promoRow) {
                        listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT).forEach { promo ->
                            moves.add(Move(from, oneStep, promotion = promo))
                        }
                    } else {
                        moves.add(Move(from, oneStep))
                    }

                    // 2 steps forward
                    if (from.row == startRow) {
                        val twoStep = Position(from.col, from.row + 2 * forward)
                        if (twoStep.isValid && state.getPiece(twoStep) == null) {
                            moves.add(Move(from, twoStep))
                        }
                    }
                }

                // Captures
                for (dc in listOf(-1, 1)) {
                    val capPos = Position(from.col + dc, from.row + forward)
                    if (capPos.isValid) {
                        val targetPiece = state.getPiece(capPos)
                        if (targetPiece != null && targetPiece.color == oppColor) {
                            if (capPos.row == promoRow) {
                                listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT).forEach { promo ->
                                    moves.add(Move(from, capPos, promotion = promo, isCapture = true))
                                }
                            } else {
                                moves.add(Move(from, capPos, isCapture = true))
                            }
                        } else if (capPos == state.enPassantTarget) {
                            // En passant capture
                            moves.add(Move(from, capPos, isCapture = true, isEnPassant = true))
                        }
                    }
                }
            }

            PieceType.KNIGHT -> {
                val offsets = listOf(
                    Pair(1, 2), Pair(2, 1), Pair(-1, 2), Pair(-2, 1),
                    Pair(1, -2), Pair(2, -1), Pair(-1, -2), Pair(-2, -1)
                )
                for ((dc, dr) in offsets) {
                    val to = Position(from.col + dc, from.row + dr)
                    if (to.isValid) {
                        val targetPiece = state.getPiece(to)
                        if (targetPiece == null) {
                            moves.add(Move(from, to))
                        } else if (targetPiece.color == oppColor) {
                            moves.add(Move(from, to, isCapture = true))
                        }
                    }
                }
            }

            PieceType.BISHOP -> {
                val dirs = listOf(Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1))
                for ((dc, dr) in dirs) {
                    var step = 1
                    while (true) {
                        val to = Position(from.col + dc * step, from.row + dr * step)
                        if (!to.isValid) break
                        val targetPiece = state.getPiece(to)
                        if (targetPiece == null) {
                            moves.add(Move(from, to))
                        } else {
                            if (targetPiece.color == oppColor) {
                                moves.add(Move(from, to, isCapture = true))
                            }
                            break
                        }
                        step++
                    }
                }
            }

            PieceType.ROOK -> {
                val dirs = listOf(Pair(0, 1), Pair(0, -1), Pair(1, 0), Pair(-1, 0))
                for ((dc, dr) in dirs) {
                    var step = 1
                    while (true) {
                        val to = Position(from.col + dc * step, from.row + dr * step)
                        if (!to.isValid) break
                        val targetPiece = state.getPiece(to)
                        if (targetPiece == null) {
                            moves.add(Move(from, to))
                        } else {
                            if (targetPiece.color == oppColor) {
                                moves.add(Move(from, to, isCapture = true))
                            }
                            break
                        }
                        step++
                    }
                }
            }

            PieceType.QUEEN -> {
                val dirs = listOf(
                    Pair(0, 1), Pair(0, -1), Pair(1, 0), Pair(-1, 0),
                    Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1)
                )
                for ((dc, dr) in dirs) {
                    var step = 1
                    while (true) {
                        val to = Position(from.col + dc * step, from.row + dr * step)
                        if (!to.isValid) break
                        val targetPiece = state.getPiece(to)
                        if (targetPiece == null) {
                            moves.add(Move(from, to))
                        } else {
                            if (targetPiece.color == oppColor) {
                                moves.add(Move(from, to, isCapture = true))
                            }
                            break
                        }
                        step++
                    }
                }
            }

            PieceType.KING -> {
                for (dr in -1..1) {
                    for (dc in -1..1) {
                        if (dr == 0 && dc == 0) continue
                        val to = Position(from.col + dc, from.row + dr)
                        if (to.isValid) {
                            val targetPiece = state.getPiece(to)
                            if (targetPiece == null) {
                                moves.add(Move(from, to))
                            } else if (targetPiece.color == oppColor) {
                                moves.add(Move(from, to, isCapture = true))
                            }
                        }
                    }
                }

                // Castling
                if (color == PieceColor.WHITE && from == Position(4, 7)) {
                    if (state.whiteCastlingKingSide &&
                        state.getPiece(Position(5, 7)) == null &&
                        state.getPiece(Position(6, 7)) == null &&
                        !isSquareAttacked(state, Position(4, 7), oppColor) &&
                        !isSquareAttacked(state, Position(5, 7), oppColor) &&
                        !isSquareAttacked(state, Position(6, 7), oppColor)
                    ) {
                        moves.add(Move(from, Position(6, 7), isCastling = true))
                    }
                    if (state.whiteCastlingQueenSide &&
                        state.getPiece(Position(3, 7)) == null &&
                        state.getPiece(Position(2, 7)) == null &&
                        state.getPiece(Position(1, 7)) == null &&
                        !isSquareAttacked(state, Position(4, 7), oppColor) &&
                        !isSquareAttacked(state, Position(3, 7), oppColor) &&
                        !isSquareAttacked(state, Position(2, 7), oppColor)
                    ) {
                        moves.add(Move(from, Position(2, 7), isCastling = true))
                    }
                } else if (color == PieceColor.BLACK && from == Position(4, 0)) {
                    if (state.blackCastlingKingSide &&
                        state.getPiece(Position(5, 0)) == null &&
                        state.getPiece(Position(6, 0)) == null &&
                        !isSquareAttacked(state, Position(4, 0), oppColor) &&
                        !isSquareAttacked(state, Position(5, 0), oppColor) &&
                        !isSquareAttacked(state, Position(6, 0), oppColor)
                    ) {
                        moves.add(Move(from, Position(6, 0), isCastling = true))
                    }
                    if (state.blackCastlingQueenSide &&
                        state.getPiece(Position(3, 0)) == null &&
                        state.getPiece(Position(2, 0)) == null &&
                        state.getPiece(Position(1, 0)) == null &&
                        !isSquareAttacked(state, Position(4, 0), oppColor) &&
                        !isSquareAttacked(state, Position(3, 0), oppColor) &&
                        !isSquareAttacked(state, Position(2, 0), oppColor)
                    ) {
                        moves.add(Move(from, Position(2, 0), isCastling = true))
                    }
                }
            }
        }
        return moves
    }

    fun makeMove(state: ChessGameState, move: Move): ChessGameState {
        val next = state.copy()
        val piece = next.getPiece(move.from) ?: return next

        // Reset en passant target by default
        next.enPassantTarget = null

        // Handle Pawn double push for en passant target
        if (piece.type == PieceType.PAWN && abs(move.to.row - move.from.row) == 2) {
            val epRow = (move.from.row + move.to.row) / 2
            next.enPassantTarget = Position(move.from.col, epRow)
        }

        // Handle En Passant capture
        if (piece.type == PieceType.PAWN && move.isEnPassant) {
            val capturedPawnRow = move.from.row
            next.setPiece(Position(move.to.col, capturedPawnRow), null)
        }

        // Handle Castling rook movement
        if (piece.type == PieceType.KING && move.isCastling) {
            if (move.to.col == 6) { // Kingside
                val rook = next.getPiece(Position(7, move.from.row))
                next.setPiece(Position(7, move.from.row), null)
                next.setPiece(Position(5, move.from.row), rook)
            } else if (move.to.col == 2) { // Queenside
                val rook = next.getPiece(Position(0, move.from.row))
                next.setPiece(Position(0, move.from.row), null)
                next.setPiece(Position(3, move.from.row), rook)
            }
        }

        // Move king / rook destroys castling rights
        if (piece.type == PieceType.KING) {
            if (piece.color == PieceColor.WHITE) {
                next.whiteCastlingKingSide = false
                next.whiteCastlingQueenSide = false
            } else {
                next.blackCastlingKingSide = false
                next.blackCastlingQueenSide = false
            }
        }
        if (piece.type == PieceType.ROOK) {
            if (piece.color == PieceColor.WHITE) {
                if (move.from == Position(7, 7)) next.whiteCastlingKingSide = false
                if (move.from == Position(0, 7)) next.whiteCastlingQueenSide = false
            } else {
                if (move.from == Position(7, 0)) next.blackCastlingKingSide = false
                if (move.from == Position(0, 0)) next.blackCastlingQueenSide = false
            }
        }

        // Execute placement
        val finalPiece = if (move.promotion != null) {
            Piece(move.promotion, piece.color)
        } else {
            piece
        }

        next.setPiece(move.from, null)
        next.setPiece(move.to, finalPiece)

        // Switch turn
        next.activeColor = state.activeColor.opposite()
        if (state.activeColor == PieceColor.BLACK) {
            next.fullmoveNumber++
        }

        return next
    }

    fun getLegalMoves(state: ChessGameState, from: Position): List<Move> {
        val piece = state.getPiece(from) ?: return emptyList()
        if (piece.color != state.activeColor) return emptyList()

        val pseudo = getPseudoLegalMoves(state, from)
        return pseudo.filter { move ->
            val nextState = makeMove(state, move)
            // Ensure own king is not in check
            !isKingInCheck(nextState, piece.color)
        }
    }

    fun getAllLegalMoves(state: ChessGameState): List<Move> {
        val moves = mutableListOf<Move>()
        for (r in 0..7) {
            for (c in 0..7) {
                val pos = Position(c, r)
                val p = state.getPiece(pos)
                if (p != null && p.color == state.activeColor) {
                    moves.addAll(getLegalMoves(state, pos))
                }
            }
        }
        return moves
    }

    fun isCheckmate(state: ChessGameState): Boolean {
        return isKingInCheck(state, state.activeColor) && getAllLegalMoves(state).isEmpty()
    }

    fun isStalemate(state: ChessGameState): Boolean {
        return !isKingInCheck(state, state.activeColor) && getAllLegalMoves(state).isEmpty()
    }
}
