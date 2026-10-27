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
import androidx.compose.ui.unit.dp
import com.example.engine.ChessGameState
import com.example.model.Move
import com.example.model.Position

@Composable
fun ChessBoardView(
    gameState: ChessGameState,
    selectedPos: Position?,
    legalMoves: List<Move>,
    lastMove: Move?,
    isCheck: Boolean,
    onSquareTapped: (Position) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "check_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Palette matching screenshot: Slate-Charcoal Dark squares & Pearlescent Silver Light squares
    val darkSquareColor = Color(0xFF555B62)       // Rich Slate Charcoal
    val lightSquareColor = Color(0xFFD6DBDF)      // Pearlescent Pale Silver White
    val outerFrameColor = Color(0xFFE8ECEF)       // 3D White/Silver Beveled Frame
    val outerBorderStroke = Color(0xFF94A3B8)
    val selectedHighlight = Color(0xFFFACC15).copy(alpha = 0.42f)
    val lastMoveHighlight = Color(0xFF38BDF8).copy(alpha = 0.32f)
    val checkColor = Color(0xFFDC2626)

    val kingInCheckPos = if (isCheck) gameState.findKing(gameState.activeColor) else null

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .testTag("chess_board_container"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
                .testTag("chess_board_canvas")
                .pointerInput(gameState, selectedPos, legalMoves) {
                    detectTapGestures { offset ->
                        val boardSize = size.width
                        val squareSize = boardSize / 8f
                        val col = (offset.x / squareSize).toInt().coerceIn(0, 7)
                        val row = (offset.y / squareSize).toInt().coerceIn(0, 7)
                        onSquareTapped(Position(col, row))
                    }
                }
        ) {
            val boardW = size.width
            val squareSize = boardW / 8f

            // 1. Draw 3D Beveled Outer Board Frame
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFFFFF), outerFrameColor, Color(0xFFCBD5E1))
                ),
                topLeft = Offset(0f, 0f),
                size = Size(boardW, boardW),
                cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
            )

            // Inner Board Shadow/Bevel inset
            drawRoundRect(
                color = outerBorderStroke,
                topLeft = Offset(1f, 1f),
                size = Size(boardW - 2f, boardW - 2f),
                cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )

            // 2. Draw 8x8 Chess Squares
            for (r in 0..7) {
                for (c in 0..7) {
                    val isLight = (r + c) % 2 == 0
                    val sqColor = if (isLight) lightSquareColor else darkSquareColor
                    val x = c * squareSize
                    val y = r * squareSize

                    drawRect(
                        color = sqColor,
                        topLeft = Offset(x, y),
                        size = Size(squareSize, squareSize)
                    )

                    // Subtle 3D Top Inner Highlight on light squares
                    if (isLight) {
                        drawRect(
                            color = Color.White.copy(alpha = 0.25f),
                            topLeft = Offset(x, y),
                            size = Size(squareSize, 2.5f)
                        )
                    }
                }
            }

            // 3. Last Move Highlights (From & To)
            if (lastMove != null) {
                listOf(lastMove.from, lastMove.to).forEach { pos ->
                    drawRect(
                        color = lastMoveHighlight,
                        topLeft = Offset(pos.col * squareSize, pos.row * squareSize),
                        size = Size(squareSize, squareSize)
                    )
                }
            }

            // 4. Selected Square Highlight
            if (selectedPos != null) {
                val sx = selectedPos.col * squareSize
                val sy = selectedPos.row * squareSize
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

            // 5. King in Check Red Pulsating Halo
            if (kingInCheckPos != null) {
                val kx = kingInCheckPos.col * squareSize
                val ky = kingInCheckPos.row * squareSize
                drawCircle(
                    color = checkColor.copy(alpha = pulseAlpha),
                    radius = squareSize * 0.48f,
                    center = Offset(kx + squareSize / 2f, ky + squareSize / 2f)
                )
            }

            // 6. Draw 3D Glossy Chess Pieces
            for (r in 0..7) {
                for (c in 0..7) {
                    val pos = Position(c, r)
                    val piece = gameState.getPiece(pos)
                    if (piece != null) {
                        ChessPieceRenderer.drawPiece(
                            scope = this,
                            piece = piece,
                            squareSize = squareSize,
                            offset = Offset(c * squareSize, r * squareSize)
                        )
                    }
                }
            }

            // 7. Legal Move Indicators
            for (move in legalMoves) {
                val mx = move.to.col * squareSize
                val my = move.to.row * squareSize
                val center = Offset(mx + squareSize / 2f, my + squareSize / 2f)
                val targetPiece = gameState.getPiece(move.to)

                if (targetPiece != null || move.isEnPassant) {
                    // Capture Ring (Red-orange targeting ring with glow)
                    drawCircle(
                        color = Color(0xFFEF4444).copy(alpha = 0.90f),
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
                    // Move Dot (Green/yellow vibrant beacon matching mobile look)
                    drawCircle(
                        color = Color(0xFF22C55E).copy(alpha = 0.85f),
                        radius = squareSize * 0.16f,
                        center = center
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.70f),
                        radius = squareSize * 0.07f,
                        center = center
                    )
                }
            }
        }
    }
}
