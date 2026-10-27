package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PieceColor
import com.example.viewmodel.ChessPuzzleViewModel

@Composable
fun LandscapeChessScreen(
    viewModel: ChessPuzzleViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.showLevelSelectMenu) {
        LevelSelectScreen(
            puzzles = uiState.puzzles,
            currentLevelIndex = uiState.currentPuzzleIndex,
            solvedIds = uiState.solvedPuzzleIds,
            onSelectLevel = { idx ->
                viewModel.loadPuzzle(idx)
            },
            onBackToBoard = {
                viewModel.toggleLevelSelectMenu(false)
            }
        )
        return
    }

    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0D1017), Color(0xFF141A24), Color(0xFF0A0D12))
    )

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("landscape_chess_screen"),
        color = Color(0xFF0F1015)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header / Timer Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Level title & theme
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "LEVEL ${uiState.currentPuzzleIndex + 1}: ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFACC15)
                        )
                        Text(
                            text = uiState.currentPuzzle.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // ⏱ 01:51 Stopwatch / Timer Pill (Matching screenshot)
                    Box(
                        modifier = Modifier
                            .shadow(4.dp, RoundedCornerShape(8.dp))
                            .background(Color(0xFF222836), RoundedCornerShape(8.dp))
                            .border(1.5.dp, Color(0xFF475569), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Timer",
                                tint = Color(0xFFFACC15),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "01:51",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                    }

                    // Level Picker Button
                    OutlinedButton(
                        onClick = { viewModel.toggleLevelSelectMenu(true) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE2E8F0)),
                        modifier = Modifier.testTag("open_level_select_button")
                    ) {
                        Icon(imageVector = Icons.Default.GridOn, contentDescription = "Levels", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Levels (${uiState.currentPuzzleIndex + 1}/${uiState.puzzles.size})", fontSize = 12.sp)
                    }
                }

                // Main Game Content (16:9 Landscape)
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    val isLandscape = maxWidth > maxHeight

                    if (isLandscape) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Pane: 8x8 Chess Board with Overlaid Winner & Loser Avatar Cards
                            Box(
                                modifier = Modifier
                                    .weight(1.05f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                ChessBoardView(
                                    gameState = uiState.gameState,
                                    selectedPos = uiState.selectedPosition,
                                    legalMoves = uiState.legalMovesForSelected,
                                    lastMove = uiState.lastMove,
                                    isCheck = (uiState.gameState.activeColor == PieceColor.WHITE &&
                                            com.example.engine.ChessEngine().isKingInCheck(uiState.gameState, PieceColor.WHITE)) ||
                                            (uiState.gameState.activeColor == PieceColor.BLACK &&
                                                    com.example.engine.ChessEngine().isKingInCheck(uiState.gameState, PieceColor.BLACK)),
                                    onSquareTapped = { pos -> viewModel.onSquareTapped(pos) },
                                    modifier = Modifier.fillMaxHeight(0.98f)
                                )

                                // Opponent Avatar Box (Top-Left of Board - Black Side)
                                PlayerAvatarCard(
                                    isPlayer = false,
                                    name = "Grandmaster",
                                    rating = "2150",
                                    isWinner = uiState.isGameOverDefeat,
                                    isLoser = uiState.isSolved,
                                    currentEmote = uiState.opponentEmote,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(start = 6.dp, top = 6.dp)
                                )

                                // Player Avatar Box (Bottom-Right of Board - White Side)
                                PlayerAvatarCard(
                                    isPlayer = true,
                                    name = "You",
                                    rating = "1850",
                                    isWinner = uiState.isSolved,
                                    isLoser = uiState.isGameOverDefeat,
                                    currentEmote = uiState.playerEmote,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(end = 6.dp, bottom = 6.dp)
                                )
                            }

                            // Right Pane: Status, Controls, Emotes & Godot Code
                            Column(
                                modifier = Modifier
                                    .weight(0.95f)
                                    .fillMaxHeight()
                                    .verticalScroll(rememberScrollState())
                                    .padding(vertical = 2.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Tactical Badge & Status
                                Column {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFEAB308).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                .border(1.dp, Color(0xFFEAB308), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = uiState.currentPuzzle.theme,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFDE047)
                                            )
                                        }

                                        val diffCol = when (uiState.currentPuzzle.difficulty) {
                                            "Beginner", "Easy" -> Color(0xFF22C55E)
                                            "Intermediate", "Medium" -> Color(0xFFF59E0B)
                                            else -> Color(0xFFEC4899)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .background(diffCol.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                .border(1.dp, diffCol, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = uiState.currentPuzzle.difficulty,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = diffCol
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Dynamic Status Card
                                    val bannerBg = when {
                                        uiState.isSolved -> Color(0xFF15803D)
                                        uiState.isIncorrectMove -> Color(0xFF991B1B)
                                        else -> Color(0xFF1E2230)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(bannerBg, RoundedCornerShape(8.dp))
                                            .border(1.dp, if (uiState.isSolved) Color(0xFF22C55E) else Color(0xFF334155), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                            .testTag("status_banner")
                                    ) {
                                        Text(
                                            text = uiState.statusMessage,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Description
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF171922)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF272A38)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = uiState.currentPuzzle.description,
                                            color = Color(0xFFCBD5E1),
                                            fontSize = 11.5.sp,
                                            lineHeight = 15.sp,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Quick Reaction Emotes (Matching screenshot: WOW!, GG!, Good Luck!, OOPSIE)
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "QUICK EMOTES",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf("WOW! 😲", "GG! ✨", "Good Luck! 🍀", "OOPSIE 😭").forEach { emote ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(Color(0xFF1E2536), RoundedCornerShape(6.dp))
                                                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp))
                                                    .clickable { viewModel.sendEmote(emote) }
                                                    .padding(vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = emote,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Control Navigation Buttons (Back Arrow, Hint, Reset, Next Level)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = { viewModel.toggleLevelSelectMenu(true) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.testTag("back_arrow_button")
                                        ) {
                                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Levels", tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Levels", fontSize = 11.5.sp, color = Color.White)
                                        }

                                        Button(
                                            onClick = { viewModel.requestHint() },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).testTag("hint_button")
                                        ) {
                                            Icon(imageVector = Icons.Default.Lightbulb, contentDescription = "Hint", tint = Color(0xFFFDE047), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Hint", color = Color.White, fontSize = 11.sp)
                                        }

                                        Button(
                                            onClick = { viewModel.restartPuzzle() },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).testTag("reset_button")
                                        ) {
                                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset", tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Reset", color = Color.White, fontSize = 11.sp)
                                        }

                                        val nextBtnBg = if (uiState.isSolved) Color(0xFF16A34A) else Color(0xFF2563EB)
                                        Button(
                                            onClick = { viewModel.nextPuzzle() },
                                            colors = ButtonDefaults.buttonColors(containerColor = nextBtnBg),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1.2f).testTag("next_level_arrow_button")
                                        ) {
                                            Text(
                                                text = if (uiState.isSolved) "Next Level" else "Next",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 11.5.sp
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Next Level", tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    // View Godot GDScript Engine Code
                                    Button(
                                        onClick = { viewModel.toggleGodotDialog(true) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("godot_code_export_button")
                                    ) {
                                        Icon(imageVector = Icons.Default.Code, contentDescription = "Godot Code", tint = Color(0xFFFDE047), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = "View Godot GDScript Engine Code", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        // Portrait Fallback
                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ChessBoardView(
                                gameState = uiState.gameState,
                                selectedPos = uiState.selectedPosition,
                                legalMoves = uiState.legalMovesForSelected,
                                lastMove = uiState.lastMove,
                                isCheck = false,
                                onSquareTapped = { pos -> viewModel.onSquareTapped(pos) },
                                modifier = Modifier.fillMaxWidth(0.95f).padding(vertical = 4.dp)
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { viewModel.toggleLevelSelectMenu(true) }) { Text("Levels") }
                                Button(onClick = { viewModel.restartPuzzle() }) { Text("Reset") }
                                Button(onClick = { viewModel.nextPuzzle() }) { Text("Next Level") }
                            }
                        }
                    }
                }

                // Dedicated Bottom Banner Ad Space (320x50)
                AdMobBannerView(
                    bannerId = uiState.adMobBannerId,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Big 3D "YOU WIN" or "TRY AGAIN / YOU LOSE" Popup Overlay
            WinnerLoserOverlay(
                isWin = uiState.isSolved,
                isLose = uiState.isGameOverDefeat,
                onNextLevel = { viewModel.nextPuzzle() },
                onRetry = { viewModel.restartPuzzle() },
                onHint = { viewModel.requestHint() },
                modifier = Modifier.fillMaxSize()
            )

            // Interstitial Ad Dialog Overlay (triggered after winning checkmate)
            if (uiState.showInterstitialAd) {
                AdMobInterstitialOverlay(
                    adUnitId = uiState.adMobInterstitialId,
                    appId = uiState.adMobAppId,
                    onDismiss = { advanceToNext ->
                        viewModel.dismissInterstitialAd(advanceToNextLevel = advanceToNext)
                    }
                )
            }

            // Godot Engine GDScript Export Dialog
            if (uiState.showGodotExportDialog) {
                GodotExportDialog(onDismiss = { viewModel.toggleGodotDialog(false) })
            }
        }
    }
}
