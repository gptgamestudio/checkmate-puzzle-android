package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ChessGameState
import com.example.model.Move
import com.example.model.Position

/**
 * Sakana Chess Pro Master Oversized 8x8 Chessboard Component.
 * Shared across all 3 modes: 2,000 Puzzles, Play Now Online, and Play with AI.
 * Matches reference image with rich green/cream squares, rank/file coordinates, and gold beveled frame.
 */
@Composable
fun ChessBoardView(
    gameState: ChessGameState,
    selectedPos: Position?,
    legalMoves: List<Move>,
    lastMove: Move?,
    isCheck: Boolean,
    onSquareTapped: (Position) -> Unit,
    modifier: Modifier = Modifier,
    isGreenTheme: Boolean = true,
    flipBoard: Boolean = false
) {
    val textMeasurer = rememberTextMeasurer()

    val infiniteTransition = rememberInfiniteTransition(label = "check_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Exact color palette matching reference design
    val darkSquareColor = if (isGreenTheme) Color(0xFF5F9E43) else Color(0xFF475569) // Tournament Rich Green
    val lightSquareColor = if (isGreenTheme) Color(0xFFEEEDE2) else Color(0xFFE2E8F0) // Warm Cream/Ivory
    val selectedHighlight = Color(0xFFE5B94F).copy(alpha = 0.55f)
    val lastMoveHighlight = Color(0xFFF59E0B).copy(alpha = 0.32f)
    val checkColor = Color(0xFFDC2626)

    val kingInCheckPos = if (isCheck) gameState.findKing(gameState.activeColor) else null

    val rankLabels = if (flipBoard) listOf("1", "2", "3", "4", "5", "6", "7", "8") else listOf("8", "7", "6", "5", "4", "3", "2", "1")
    val fileLabels = if (flipBoard) listOf("H", "G", "F", "E", "D", "C", "B", "A") else listOf("A", "B", "C", "D", "E", "F", "G", "H")

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .testTag("chess_board_container"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
                .testTag("chess_board_canvas")
                .pointerInput(gameState, selectedPos, legalMoves, flipBoard) {
                    detectTapGestures { offset ->
                        val boardSize = size.width
                        val gutterLeft = boardSize * 0.052f
                        val gutterBottom = boardSize * 0.052f
                        val squareSize = (boardSize - gutterLeft) / 8f

                        val x = offset.x
                        val y = offset.y

                        if (x >= gutterLeft && x <= gutterLeft + 8 * squareSize && y >= 0f && y <= 8 * squareSize) {
                            val visualCol = ((x - gutterLeft) / squareSize).toInt().coerceIn(0, 7)
                            val visualRow = (y / squareSize).toInt().coerceIn(0, 7)
                            val boardCol = if (flipBoard) 7 - visualCol else visualCol
                            val boardRow = if (flipBoard) 7 - visualRow else visualRow
                            onSquareTapped(Position(boardCol, boardRow))
                        }
                    }
                }
        ) {
            val boardW = size.width
            val boardH = size.height
            val boardSize = minOf(boardW, boardH)

            val gutterLeft = boardSize * 0.052f
            val gutterBottom = boardSize * 0.052f
            val squareSize = (boardSize - gutterLeft) / 8f

            // 1. Draw Premium Beveled Outer Board Frame
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF222B38), Color(0xFF141A22), Color(0xFF090D12))
                ),
                topLeft = Offset(0f, 0f),
                size = Size(boardSize, boardSize),
                cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
            )

            // Outer Metallic Gold Border Stroke (Screen-Accurate)
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFFE082), Color(0xFFD4AF37), Color(0xFFB78A12), Color(0xFFFFE082))
                ),
                topLeft = Offset(1.5f, 1.5f),
                size = Size(boardSize - 3f, boardSize - 3f),
                cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                style = Stroke(width = 2.5.dp.toPx())
            )

            // Inner subtle border separation line
            drawRoundRect(
                color = Color(0xFF24303F),
                topLeft = Offset(gutterLeft - 1f, 0f),
                size = Size(8 * squareSize + 2f, 8 * squareSize + 2f),
                style = Stroke(width = 1.dp.toPx())
            )

            // 2. Draw 8x8 Chess Squares
            for (r in 0..7) {
                for (c in 0..7) {
                    val boardR = if (flipBoard) 7 - r else r
                    val boardC = if (flipBoard) 7 - c else c
                    val isLight = (boardR + boardC) % 2 == 0
                    val sqColor = if (isLight) lightSquareColor else darkSquareColor
                    val x = gutterLeft + c * squareSize
                    val y = r * squareSize

                    drawRect(
                        color = sqColor,
                        topLeft = Offset(x, y),
                        size = Size(squareSize, squareSize)
                    )

                    // Subtle 3D Top Inner Highlight on light squares
                    if (isLight) {
                        drawRect(
                            color = Color.White.copy(alpha = 0.35f),
                            topLeft = Offset(x, y),
                            size = Size(squareSize, 2.5f)
                        )
                    }
                }
            }

            // 3. Draw Rank numbers (8 to 1) along Left Gutter
            val rankStyle = androidx.compose.ui.text.TextStyle(
                color = Color.White,
                fontSize = (squareSize * 0.28f).sp,
                fontWeight = FontWeight.Black
            )
            for (r in 0..7) {
                val label = rankLabels[r]
                val measured = textMeasurer.measure(label, rankStyle)
                val textX = (gutterLeft - measured.size.width) / 2f
                val textY = r * squareSize + (squareSize - measured.size.height) / 2f
                drawText(
                    textMeasurer = textMeasurer,
                    text = label,
                    topLeft = Offset(textX, textY),
                    style = rankStyle
                )
            }

            // 4. Draw File letters (A to H) along Bottom Gutter
            val fileStyle = androidx.compose.ui.text.TextStyle(
                color = Color.White,
                fontSize = (squareSize * 0.28f).sp,
                fontWeight = FontWeight.Black
            )
            for (c in 0..7) {
                val label = fileLabels[c]
                val measured = textMeasurer.measure(label, fileStyle)
                val textX = gutterLeft + c * squareSize + (squareSize - measured.size.width) / 2f
                val textY = 8 * squareSize + (gutterBottom - measured.size.height) / 2f
                drawText(
                    textMeasurer = textMeasurer,
                    text = label,
                    topLeft = Offset(textX, textY),
                    style = fileStyle
                )
            }

            // 5. Last Move Highlights (From & To)
            if (lastMove != null) {
                listOf(lastMove.from, lastMove.to).forEach { pos ->
                    val vc = if (flipBoard) 7 - pos.col else pos.col
                    val vr = if (flipBoard) 7 - pos.row else pos.row
                    drawRect(
                        color = lastMoveHighlight,
                        topLeft = Offset(gutterLeft + vc * squareSize, vr * squareSize),
                        size = Size(squareSize, squareSize)
                    )
                }
            }

            // 6. Selected Square Highlight
            if (selectedPos != null) {
                val vc = if (flipBoard) 7 - selectedPos.col else selectedPos.col
                val vr = if (flipBoard) 7 - selectedPos.row else selectedPos.row
                val sx = gutterLeft + vc * squareSize
                val sy = vr * squareSize
                drawRect(
                    color = selectedHighlight,
                    topLeft = Offset(sx, sy),
                    size = Size(squareSize, squareSize)
                )
                drawRoundRect(
                    color = Color(0xFFFACC15),
                    topLeft = Offset(sx + 2f, sy + 2f),
                    size = Size(squareSize - 4f, squareSize - 4f),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 7. King in Check Red Pulsating Halo
            if (kingInCheckPos != null) {
                val vc = if (flipBoard) 7 - kingInCheckPos.col else kingInCheckPos.col
                val vr = if (flipBoard) 7 - kingInCheckPos.row else kingInCheckPos.row
                val kx = gutterLeft + vc * squareSize
                val ky = vr * squareSize
                drawCircle(
                    color = checkColor.copy(alpha = pulseAlpha),
                    radius = squareSize * 0.48f,
                    center = Offset(kx + squareSize / 2f, ky + squareSize / 2f)
                )
            }

            // 8. Draw 3D Glossy Chess Pieces
            for (r in 0..7) {
                for (c in 0..7) {
                    val boardR = if (flipBoard) 7 - r else r
                    val boardC = if (flipBoard) 7 - c else c
                    val pos = Position(boardC, boardR)
                    val piece = gameState.getPiece(pos)
                    if (piece != null) {
                        ChessPieceRenderer.drawPiece(
                            scope = this,
                            piece = piece,
                            squareSize = squareSize,
                            offset = Offset(gutterLeft + c * squareSize, r * squareSize)
                        )
                    }
                }
            }

            // 9. Legal Move Indicators
            for (move in legalMoves) {
                val vc = if (flipBoard) 7 - move.to.col else move.to.col
                val vr = if (flipBoard) 7 - move.to.row else move.to.row
                val mx = gutterLeft + vc * squareSize
                val my = vr * squareSize
                val center = Offset(mx + squareSize / 2f, my + squareSize / 2f)
                val targetPiece = gameState.getPiece(move.to)

                if (targetPiece != null || move.isEnPassant) {
                    // Capture Ring (Glowing Crimson with center point)
                    drawCircle(
                        color = Color(0xFFEF4444).copy(alpha = 0.92f),
                        radius = squareSize * 0.44f,
                        center = center,
                        style = Stroke(width = 4.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFFFACC15),
                        radius = squareSize * 0.12f,
                        center = center
                    )
                } else {
                    // Move Dot (Emerald beacon with specular center)
                    drawCircle(
                        color = Color(0xFF22C55E).copy(alpha = 0.88f),
                        radius = squareSize * 0.16f,
                        center = center
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.85f),
                        radius = squareSize * 0.07f,
                        center = center
                    )
                }
            }
        }
    }
}
