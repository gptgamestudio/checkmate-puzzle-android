package com.example.model

enum class PieceType(val symbol: String, val notation: String) {
    PAWN("P", ""),
    KNIGHT("N", "N"),
    BISHOP("B", "B"),
    ROOK("R", "R"),
    QUEEN("Q", "Q"),
    KING("K", "K")
}

enum class PieceColor {
    WHITE,
    BLACK;

    fun opposite(): PieceColor = if (this == WHITE) BLACK else WHITE
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
        fun fromChar(c: Char): Piece? {
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

data class Position(
    val col: Int, // 0..7 (a..h)
    val row: Int  // 0..7 (8..1, so row 0 is rank 8, row 7 is rank 1)
) {
    val algebraic: String
        get() = "${('a' + col)}${8 - row}"

    val isValid: Boolean
        get() = col in 0..7 && row in 0..7

    companion object {
        fun fromAlgebraic(s: String): Position? {
            if (s.length < 2) return null
            val col = s[0].lowercaseChar() - 'a'
            val rank = s[1].digitToIntOrNull() ?: return null
            val row = 8 - rank
            if (col in 0..7 && row in 0..7) {
                return Position(col, row)
            }
            return null
        }
    }
}

data class Move(
    val from: Position,
    val to: Position,
    val promotion: PieceType? = null,
    val isCapture: Boolean = false,
    val isCastling: Boolean = false,
    val isEnPassant: Boolean = false
) {
    val notation: String
        get() {
            val prom = promotion?.notation?.let { "=$it" } ?: ""
            return "${from.algebraic}${to.algebraic}$prom"
        }

    companion object {
        fun fromUci(uci: String): Move? {
            if (uci.length < 4) return null
            val from = Position.fromAlgebraic(uci.substring(0, 2)) ?: return null
            val to = Position.fromAlgebraic(uci.substring(2, 4)) ?: return null
            val prom = if (uci.length >= 5) {
                when (uci[4].lowercaseChar()) {
                    'q' -> PieceType.QUEEN
                    'r' -> PieceType.ROOK
                    'b' -> PieceType.BISHOP
                    'n' -> PieceType.KNIGHT
                    else -> null
                }
            } else null
            return Move(from, to, promotion = prom)
        }
    }
}

data class Puzzle(
    val id: String,
    val title: String,
    val theme: String, // "Mate in 1", "Mate in 2", "Smothered Mate", etc.
    val difficulty: String, // "Beginner", "Intermediate", "Master"
    val turn: PieceColor,
    val fen: String,
    val solutionMoves: List<String>, // UCI notation, e.g. ["e2e4", "e7e5"]
    val description: String,
    val hints: List<String>
)
