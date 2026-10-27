package com.example.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.model.Piece
import com.example.model.PieceColor
import com.example.model.PieceType

object ChessPieceRenderer {

    // 3D Glossy Ceramic White (Matching screenshot: Pearl-white with soft metallic reflection)
    private val WhiteBodyBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFFFFFFFF), Color(0xFFEFF3F8), Color(0xFFCBD3DC), Color(0xFFA6AFBC))
    )
    private val WhiteOutlineColor = Color(0xFF333842)
    private val WhiteSpecularColor = Color(0xEEFFFFFF)

    // 3D Glossy Jet Black (Matching screenshot: Rich Obsidian with top rim shine)
    private val BlackBodyBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF3B3E45), Color(0xFF25272C), Color(0xFF16171A), Color(0xFF0D0E10))
    )
    private val BlackOutlineColor = Color(0xFF0A0B0D)
    private val BlackSpecularColor = Color(0x99FFFFFF)

    // Soft drop shadow under piece
    private val ShadowBrush = Brush.radialGradient(
        colors = listOf(Color(0x66000000), Color(0x00000000))
    )

    fun drawPiece(
        scope: DrawScope,
        piece: Piece,
        squareSize: Float,
        offset: Offset
    ) {
        val isWhite = piece.color == PieceColor.WHITE
        val fillBrush = if (isWhite) WhiteBodyBrush else BlackBodyBrush
        val outlineColor = if (isWhite) WhiteOutlineColor else BlackOutlineColor
        val specColor = if (isWhite) WhiteSpecularColor else BlackSpecularColor

        val padding = squareSize * 0.10f
        val w = squareSize - 2 * padding
        val h = squareSize - 2 * padding
        val left = offset.x + padding
        val top = offset.y + padding

        // 1. Draw 3D Ground Contact Shadow
        scope.drawOval(
            brush = ShadowBrush,
            topLeft = Offset(left + w * 0.15f, top + h * 0.82f),
            size = Size(w * 0.70f, h * 0.22f)
        )

        // 2. Draw 3D Piece Geometry
        when (piece.type) {
            PieceType.PAWN -> drawPawn(scope, left, top, w, h, fillBrush, outlineColor, specColor, isWhite)
            PieceType.KNIGHT -> drawKnight(scope, left, top, w, h, fillBrush, outlineColor, specColor, isWhite)
            PieceType.BISHOP -> drawBishop(scope, left, top, w, h, fillBrush, outlineColor, specColor, isWhite)
            PieceType.ROOK -> drawRook(scope, left, top, w, h, fillBrush, outlineColor, specColor, isWhite)
            PieceType.QUEEN -> drawQueen(scope, left, top, w, h, fillBrush, outlineColor, specColor, isWhite)
            PieceType.KING -> drawKing(scope, left, top, w, h, fillBrush, outlineColor, specColor, isWhite)
        }
    }

    private fun drawPawn(
        scope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        fill: Brush, outline: Color, spec: Color, isWhite: Boolean
    ) {
        val strokeW = w * 0.052f

        // Base
        val basePath = Path().apply {
            moveTo(x + w * 0.18f, y + h * 0.90f)
            lineTo(x + w * 0.82f, y + h * 0.90f)
            quadraticTo(x + w * 0.84f, y + h * 0.78f, x + w * 0.72f, y + h * 0.74f)
            lineTo(x + w * 0.28f, y + h * 0.74f)
            quadraticTo(x + w * 0.16f, y + h * 0.78f, x + w * 0.18f, y + h * 0.90f)
            close()
        }
        scope.drawPath(basePath, fill)
        scope.drawPath(basePath, outline, style = Stroke(strokeW, join = StrokeJoin.Round))

        // Stem Body
        val stemPath = Path().apply {
            moveTo(x + w * 0.32f, y + h * 0.74f)
            quadraticTo(x + w * 0.44f, y + h * 0.52f, x + w * 0.38f, y + h * 0.40f)
            lineTo(x + w * 0.62f, y + h * 0.40f)
            quadraticTo(x + w * 0.56f, y + h * 0.52f, x + w * 0.68f, y + h * 0.74f)
            close()
        }
        scope.drawPath(stemPath, fill)
        scope.drawPath(stemPath, outline, style = Stroke(strokeW, join = StrokeJoin.Round))

        // Collar Ring
        val collarPath = Path().apply {
            moveTo(x + w * 0.30f, y + h * 0.40f)
            quadraticTo(x + w * 0.50f, y + h * 0.44f, x + w * 0.70f, y + h * 0.40f)
            quadraticTo(x + w * 0.50f, y + h * 0.36f, x + w * 0.30f, y + h * 0.40f)
            close()
        }
        scope.drawPath(collarPath, fill)
        scope.drawPath(collarPath, outline, style = Stroke(strokeW * 0.8f))

        // Head Sphere (3D glossy ball)
        val headR = w * 0.22f
        val headCenter = Offset(x + w * 0.50f, y + h * 0.24f)
        scope.drawCircle(fill, radius = headR, center = headCenter)
        scope.drawCircle(outline, radius = headR, center = headCenter, style = Stroke(strokeW))

        // Specular 3D Highlight
        val specCenter = Offset(headCenter.x - headR * 0.32f, headCenter.y - headR * 0.32f)
        scope.drawCircle(spec, radius = headR * 0.36f, center = specCenter)
        scope.drawCircle(Color.White.copy(alpha = if (isWhite) 0.95f else 0.45f), radius = headR * 0.16f, center = specCenter)
    }

    private fun drawKnight(
        scope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        fill: Brush, outline: Color, spec: Color, isWhite: Boolean
    ) {
        val strokeW = w * 0.052f

        // Base
        val basePath = Path().apply {
            moveTo(x + w * 0.16f, y + h * 0.90f)
            lineTo(x + w * 0.84f, y + h * 0.90f)
            lineTo(x + w * 0.76f, y + h * 0.78f)
            lineTo(x + w * 0.24f, y + h * 0.78f)
            close()
        }
        scope.drawPath(basePath, fill)
        scope.drawPath(basePath, outline, style = Stroke(strokeW, join = StrokeJoin.Round))

        // Horse Silhouette
        val horsePath = Path().apply {
            moveTo(x + w * 0.26f, y + h * 0.78f)
            // Back & Mane
            quadraticTo(x + w * 0.20f, y + h * 0.46f, x + w * 0.36f, y + h * 0.18f)
            // Ear
            lineTo(x + w * 0.46f, y + h * 0.10f)
            lineTo(x + w * 0.50f, y + h * 0.20f)
            lineTo(x + w * 0.58f, y + h * 0.12f)
            lineTo(x + w * 0.58f, y + h * 0.24f)
            // Forehead & Snout
            quadraticTo(x + w * 0.74f, y + h * 0.26f, x + w * 0.84f, y + h * 0.38f)
            lineTo(x + w * 0.84f, y + h * 0.48f)
            lineTo(x + w * 0.70f, y + h * 0.50f)
            // Jaw & Throat
            quadraticTo(x + w * 0.64f, y + h * 0.58f, x + w * 0.54f, y + h * 0.60f)
            quadraticTo(x + w * 0.68f, y + h * 0.70f, x + w * 0.74f, y + h * 0.78f)
            close()
        }
        scope.drawPath(horsePath, fill)
        scope.drawPath(horsePath, outline, style = Stroke(strokeW, join = StrokeJoin.Round))

        // Eye (Shiny circle)
        val eyePos = Offset(x + w * 0.60f, y + h * 0.34f)
        scope.drawCircle(outline, radius = w * 0.045f, center = eyePos)
        scope.drawCircle(Color.White, radius = w * 0.02f, center = Offset(eyePos.x - 1.5f, eyePos.y - 1.5f))

        // Curved mane highlight
        val maneSpec = Path().apply {
            moveTo(x + w * 0.30f, y + h * 0.32f)
            quadraticTo(x + w * 0.28f, y + h * 0.55f, x + w * 0.32f, y + h * 0.70f)
        }
        scope.drawPath(maneSpec, spec, style = Stroke(strokeW * 0.9f, cap = StrokeCap.Round))
    }

    private fun drawBishop(
        scope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        fill: Brush, outline: Color, spec: Color, isWhite: Boolean
    ) {
        val strokeW = w * 0.052f

        // Base
        val basePath = Path().apply {
            moveTo(x + w * 0.18f, y + h * 0.90f)
            lineTo(x + w * 0.82f, y + h * 0.90f)
            lineTo(x + w * 0.74f, y + h * 0.78f)
            lineTo(x + w * 0.26f, y + h * 0.78f)
            close()
        }
        scope.drawPath(basePath, fill)
        scope.drawPath(basePath, outline, style = Stroke(strokeW, join = StrokeJoin.Round))

        // Mitre Body
        val bodyPath = Path().apply {
            moveTo(x + w * 0.30f, y + h * 0.78f)
            lineTo(x + w * 0.34f, y + h * 0.60f)
            quadraticTo(x + w * 0.22f, y + h * 0.44f, x + w * 0.36f, y + h * 0.24f)
            lineTo(x + w * 0.50f, y + h * 0.15f)
            lineTo(x + w * 0.64f, y + h * 0.24f)
            quadraticTo(x + w * 0.78f, y + h * 0.44f, x + w * 0.66f, y + h * 0.60f)
            lineTo(x + w * 0.70f, y + h * 0.78f)
            close()
        }
        scope.drawPath(bodyPath, fill)
        scope.drawPath(bodyPath, outline, style = Stroke(strokeW, join = StrokeJoin.Round))

        // Diagonal Cleft
        scope.drawLine(
            color = outline,
            start = Offset(x + w * 0.58f, y + h * 0.26f),
            end = Offset(x + w * 0.42f, y + h * 0.42f),
            strokeWidth = strokeW * 1.1f,
            cap = StrokeCap.Round
        )

        // Top Finial Sphere
        val finialPos = Offset(x + w * 0.50f, y + h * 0.12f)
        scope.drawCircle(fill, radius = w * 0.07f, center = finialPos)
        scope.drawCircle(outline, radius = w * 0.07f, center = finialPos, style = Stroke(strokeW * 0.8f))
        scope.drawCircle(spec, radius = w * 0.03f, center = Offset(finialPos.x - 2f, finialPos.y - 2f))
    }

    private fun drawRook(
        scope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        fill: Brush, outline: Color, spec: Color, isWhite: Boolean
    ) {
        val strokeW = w * 0.052f

        // Base
        val basePath = Path().apply {
            moveTo(x + w * 0.16f, y + h * 0.90f)
            lineTo(x + w * 0.84f, y + h * 0.90f)
            lineTo(x + w * 0.76f, y + h * 0.78f)
            lineTo(x + w * 0.24f, y + h * 0.78f)
            close()
        }
        scope.drawPath(basePath, fill)
        scope.drawPath(basePath, outline, style = Stroke(strokeW, join = StrokeJoin.Round))

        // Tower Body & Crenellations
        val towerPath = Path().apply {
            moveTo(x + w * 0.28f, y + h * 0.78f)
            lineTo(x + w * 0.32f, y + h * 0.42f)
            lineTo(x + w * 0.22f, y + h * 0.38f)
            lineTo(x + w * 0.22f, y + h * 0.18f)
            // Crenellations
            lineTo(x + w * 0.35f, y + h * 0.18f)
            lineTo(x + w * 0.35f, y + h * 0.26f)
            lineTo(x + w * 0.44f, y + h * 0.26f)
            lineTo(x + w * 0.44f, y + h * 0.18f)
            lineTo(x + w * 0.56f, y + h * 0.18f)
            lineTo(x + w * 0.56f, y + h * 0.26f)
            lineTo(x + w * 0.65f, y + h * 0.26f)
            lineTo(x + w * 0.65f, y + h * 0.18f)
            lineTo(x + w * 0.78f, y + h * 0.18f)
            lineTo(x + w * 0.78f, y + h * 0.38f)
            lineTo(x + w * 0.68f, y + h * 0.42f)
            lineTo(x + w * 0.72f, y + h * 0.78f)
            close()
        }
        scope.drawPath(towerPath, fill)
        scope.drawPath(towerPath, outline, style = Stroke(strokeW, join = StrokeJoin.Round))

        // Center Embrasure Arch
        scope.drawRoundRect(
            color = outline,
            topLeft = Offset(x + w * 0.44f, y + h * 0.48f),
            size = Size(w * 0.12f, h * 0.18f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.06f, w * 0.06f)
        )

        // Gloss Specular Line
        scope.drawLine(
            color = spec,
            start = Offset(x + w * 0.28f, y + h * 0.24f),
            end = Offset(x + w * 0.72f, y + h * 0.24f),
            strokeWidth = strokeW * 0.8f,
            cap = StrokeCap.Round
        )
    }

    private fun drawQueen(
        scope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        fill: Brush, outline: Color, spec: Color, isWhite: Boolean
    ) {
        val strokeW = w * 0.052f

        // Base
        val basePath = Path().apply {
            moveTo(x + w * 0.14f, y + h * 0.90f)
            lineTo(x + w * 0.86f, y + h * 0.90f)
            lineTo(x + w * 0.76f, y + h * 0.80f)
            lineTo(x + w * 0.24f, y + h * 0.80f)
            close()
        }
        scope.drawPath(basePath, fill)
        scope.drawPath(basePath, outline, style = Stroke(strokeW, join = StrokeJoin.Round))

        // Coronet Body
        val queenPath = Path().apply {
            moveTo(x + w * 0.28f, y + h * 0.80f)
            quadraticTo(x + w * 0.34f, y + h * 0.58f, x + w * 0.18f, y + h * 0.30f)
            lineTo(x + w * 0.32f, y + h * 0.40f)
            lineTo(x + w * 0.40f, y + h * 0.24f)
            lineTo(x + w * 0.50f, y + h * 0.36f)
            lineTo(x + w * 0.60f, y + h * 0.24f)
            lineTo(x + w * 0.68f, y + h * 0.40f)
            lineTo(x + w * 0.82f, y + h * 0.30f)
            quadraticTo(x + w * 0.66f, y + h * 0.58f, x + w * 0.72f, y + h * 0.80f)
            close()
        }
        scope.drawPath(queenPath, fill)
        scope.drawPath(queenPath, outline, style = Stroke(strokeW, join = StrokeJoin.Round))

        // Pearls on Tiara Peaks
        val pearlR = w * 0.048f
        listOf(
            Offset(x + w * 0.18f, y + h * 0.28f),
            Offset(x + w * 0.40f, y + h * 0.22f),
            Offset(x + w * 0.60f, y + h * 0.22f),
            Offset(x + w * 0.82f, y + h * 0.28f)
        ).forEach { pos ->
            scope.drawCircle(fill, radius = pearlR, center = pos)
            scope.drawCircle(outline, radius = pearlR, center = pos, style = Stroke(strokeW * 0.7f))
            scope.drawCircle(spec, radius = pearlR * 0.5f, center = Offset(pos.x - 1f, pos.y - 1f))
        }
    }

    private fun drawKing(
        scope: DrawScope,
        x: Float, y: Float, w: Float, h: Float,
        fill: Brush, outline: Color, spec: Color, isWhite: Boolean
    ) {
        val strokeW = w * 0.052f

        // Base
        val basePath = Path().apply {
            moveTo(x + w * 0.14f, y + h * 0.90f)
            lineTo(x + w * 0.86f, y + h * 0.90f)
            lineTo(x + w * 0.76f, y + h * 0.80f)
            lineTo(x + w * 0.24f, y + h * 0.80f)
            close()
        }
        scope.drawPath(basePath, fill)
        scope.drawPath(basePath, outline, style = Stroke(strokeW, join = StrokeJoin.Round))

        // Robe & Crown Body
        val kingBody = Path().apply {
            moveTo(x + w * 0.26f, y + h * 0.80f)
            quadraticTo(x + w * 0.32f, y + h * 0.56f, x + w * 0.22f, y + h * 0.36f)
            lineTo(x + w * 0.40f, y + h * 0.40f)
            lineTo(x + w * 0.50f, y + h * 0.28f)
            lineTo(x + w * 0.60f, y + h * 0.40f)
            lineTo(x + w * 0.78f, y + h * 0.36f)
            quadraticTo(x + w * 0.68f, y + h * 0.56f, x + w * 0.74f, y + h * 0.80f)
            close()
        }
        scope.drawPath(kingBody, fill)
        scope.drawPath(kingBody, outline, style = Stroke(strokeW, join = StrokeJoin.Round))

        // Crown Cross
        val crossCenter = Offset(x + w * 0.50f, y + h * 0.15f)
        val crossH = h * 0.15f
        val crossW = w * 0.14f
        scope.drawLine(
            color = if (isWhite) Color(0xFFFACC15) else Color(0xFFE11D48),
            start = Offset(crossCenter.x, crossCenter.y - crossH * 0.5f),
            end = Offset(crossCenter.x, crossCenter.y + crossH * 0.5f),
            strokeWidth = strokeW * 1.3f,
            cap = StrokeCap.Round
        )
        scope.drawLine(
            color = if (isWhite) Color(0xFFFACC15) else Color(0xFFE11D48),
            start = Offset(crossCenter.x - crossW * 0.5f, crossCenter.y - crossH * 0.12f),
            end = Offset(crossCenter.x + crossW * 0.5f, crossCenter.y - crossH * 0.12f),
            strokeWidth = strokeW * 1.3f,
            cap = StrokeCap.Round
        )

        // Specular Curve
        scope.drawCircle(spec, radius = w * 0.08f, center = Offset(x + w * 0.38f, y + h * 0.44f))
    }
}
