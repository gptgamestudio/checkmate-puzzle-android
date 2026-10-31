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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DashboardState
import com.example.model.GamePlayMode
import com.example.model.PieceColor
import com.example.viewmodel.ChessPuzzleViewModel

@Composable
fun LandscapeChessScreen(
    viewModel: ChessPuzzleViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // 1ST DASHBOARD: Auth (Login / Sign Up)
    if (uiState.currentDashboard == DashboardState.AUTH_DASHBOARD) {
        AuthDashboardScreen(
            currentSession = uiState.userSession,
            adBannerId = uiState.adMobBannerId,
            onLoginSuccess = { session -> viewModel.onLoginSuccess(session) },
            modifier = modifier
        )
        return
    }

    // 2ND DASHBOARD: Lobby / Hub Screen
    if (uiState.currentDashboard == DashboardState.LOBBY_DASHBOARD) {
        if (uiState.showStrategyGuide) {
            StrategyGuideScreen(
                completedStrategyIds = uiState.completedStrategies,
                onSelectStrategy = { /* Strategy lesson selected */ },
                onBack = { viewModel.closeStrategyGuide() },
                modifier = modifier
            )
        } else {
            LobbyDashboardScreen(
                userSession = uiState.userSession,
                adBannerId = uiState.adMobBannerId,
                solvedCount = uiState.solvedPuzzleIds.size,
                onStartGame = { mode, timeControl, aiLevel ->
                    viewModel.startLobbyGame(mode, timeControl, aiLevel)
                },
                onOpenStrategy = { viewModel.openStrategyGuide() },
                onLogout = { viewModel.logout() },
                modifier = modifier
            )
        }
        return
    }

    // 3RD DASHBOARD: Game Arena (Landscape 16:9)
    if (uiState.showLevelSelectMenu) {
        LevelSelectScreen(
            puzzles = uiState.puzzles,
            currentLevelIndex = uiState.currentPuzzleIndex,
            solvedIds = uiState.solvedPuzzleIds,
            onSelectLevel = { idx -> viewModel.loadPuzzle(idx) },
            onBackToBoard = { viewModel.toggleLevelSelectMenu(false) }
        )
        return
    }

    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF080A0B), Color(0xFF111417), Color(0xFF080A0B))
    )

    var isMuted by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(com.example.util.ChessAudioPlayer.isMuted)
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("landscape_chess_screen"),
        color = Color(0xFF080A0B)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // 1. RESPONSIVE TOP BAR (YOU vs OPPONENT)
                SingleLineTopBar(
                    playerUsername = uiState.userSession.username,
                    playerColor = uiState.playerSide,
                    playerFlag = uiState.userSession.countryFlag,
                    playerRating = uiState.userSession.rating,
                    playerSeconds = if (uiState.gameMode == GamePlayMode.PUZZLES_2000) uiState.timeRemainingSeconds else uiState.playerTimeRemainingSeconds,
                    opponentUsername = if (uiState.gameMode == GamePlayMode.PUZZLES_2000) "Level ${uiState.currentPuzzleIndex + 1}" else uiState.currentChallenger.username,
                    opponentFlag = if (uiState.gameMode == GamePlayMode.PUZZLES_2000) "👑" else uiState.currentChallenger.flag,
                    opponentRating = if (uiState.gameMode == GamePlayMode.PUZZLES_2000) 2200 else uiState.currentChallenger.rating,
                    opponentSeconds = if (uiState.gameMode == GamePlayMode.PUZZLES_2000) 111 else uiState.opponentTimeRemainingSeconds,
                    isPuzzleMode = uiState.gameMode == GamePlayMode.PUZZLES_2000,
                    isMuted = isMuted,
                    onToggleMute = {
                        isMuted = com.example.util.ChessAudioPlayer.toggleMute()
                    },
                    onBackToLobby = { viewModel.navigateToDashboard(DashboardState.LOBBY_DASHBOARD) },
                    onToggleTheme = { viewModel.toggleBoardTheme() }
                )

                // Online Auto-Search Next Challenger Banner (5s Countdown)
                AnimatedVisibility(
                    visible = uiState.isAutoSearchingNextChallenger,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF769656))
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⚡ CHECKMATE! Auto-searching next challenger in ${uiState.autoSearchCountdown}s... 🌍",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.5.sp
                        )
                    }
                }

                // 2. MAIN 16:9 DASHBOARD / GAME AREA (Ligtas sa Ads at Top Bar)
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val isLandscape = maxWidth > maxHeight

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF141719), RoundedCornerShape(12.dp))
                            .border(1.5.dp, Color(0xFF303638), RoundedCornerShape(12.dp))
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLandscape) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left Pane: 8x8 Chess Board (Tournament Green & White standard, maximized)
                                Box(
                                    modifier = Modifier
                                        .weight(1.15f)
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
                                        isGreenTheme = uiState.isGreenTheme,
                                        flipBoard = uiState.playerSide == PieceColor.BLACK,
                                        modifier = Modifier.fillMaxHeight(0.99f)
                                    )

                                    // Opponent Avatar Box (Top-Left of Board)
                                    PlayerAvatarCard(
                                        isPlayer = false,
                                        name = if (uiState.gameMode == GamePlayMode.PUZZLES_2000) "Grandmaster" else uiState.currentChallenger.username,
                                        rating = if (uiState.gameMode == GamePlayMode.PUZZLES_2000) "2200" else "${uiState.currentChallenger.rating}",
                                        isWinner = uiState.isGameOverDefeat,
                                        isLoser = uiState.isSolved,
                                        currentEmote = uiState.opponentEmote,
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(6.dp)
                                    )

                                    // Player Avatar Box (Bottom-Right of Board)
                                    PlayerAvatarCard(
                                        isPlayer = true,
                                        name = uiState.userSession.username,
                                        rating = "${uiState.userSession.rating}",
                                        isWinner = uiState.isSolved,
                                        isLoser = uiState.isGameOverDefeat,
                                        currentEmote = uiState.playerEmote,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(6.dp)
                                    )
                                }

                                // Right Pane: Game Controls & Match Status
                                Column(
                                    modifier = Modifier
                                        .weight(0.85f)
                                        .fillMaxHeight()
                                        .padding(vertical = 4.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        // Match Mode / Title
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = when (uiState.gameMode) {
                                                    GamePlayMode.PUZZLES_2000 -> "Level ${uiState.currentPuzzleIndex + 1}: ${uiState.currentPuzzle.title}"
                                                    GamePlayMode.ONLINE_MULTIPLAYER -> "Online: vs ${uiState.currentChallenger.username}"
                                                    GamePlayMode.PLAY_WITH_AI -> "AI Match: ${uiState.aiLevel.title}"
                                                    else -> "Strategy Study"
                                                },
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFF22C55E).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (uiState.playerSide == PieceColor.WHITE) "You: White ⚪" else "You: Black ⚫",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFFDE68A)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Dynamic Status Banner
                                        val bannerBg = when {
                                            uiState.isSolved -> Color(0xFF15803D)
                                            uiState.isIncorrectMove || uiState.isGameOverDefeat -> Color(0xFF991B1B)
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
                                                fontSize = 12.sp,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Quick Emotes
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf("WOW! 🤩", "GG! 🤝", "Good Luck! ✨", "OOPSIE! 😅").forEach { emote ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(Color(0xFF1E2433), RoundedCornerShape(6.dp))
                                                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp))
                                                    .clickable { viewModel.sendEmote(emote) }
                                                    .padding(vertical = 5.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = emote,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFE2E8F0)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Action Buttons (Back, Reset, Next Level)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Button(
                                            onClick = { viewModel.navigateToDashboard(DashboardState.LOBBY_DASHBOARD) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(38.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowBack, contentDescription = "Lobby", modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Lobby", fontSize = 11.sp)
                                        }

                                        Button(
                                            onClick = { viewModel.restartPuzzle() },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(38.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Reset", fontSize = 11.sp)
                                        }

                                        if (uiState.gameMode == GamePlayMode.PUZZLES_2000) {
                                            Button(
                                                onClick = { viewModel.toggleLevelSelectMenu(true) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1.2f).height(38.dp)
                                            ) {
                                                Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("2000 Puzzles", fontSize = 10.5.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Vertical Portrait
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
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
                                        isGreenTheme = uiState.isGreenTheme,
                                        flipBoard = uiState.playerSide == PieceColor.BLACK,
                                        modifier = Modifier.fillMaxSize(0.98f)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.navigateToDashboard(DashboardState.LOBBY_DASHBOARD) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).height(36.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = "Lobby", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Lobby", fontSize = 11.sp)
                                    }
                                    Button(
                                        onClick = { viewModel.restartPuzzle() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).height(36.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Reset", fontSize = 11.sp)
                                    }
                                    if (uiState.gameMode == GamePlayMode.PUZZLES_2000) {
                                        Button(
                                            onClick = { viewModel.requestHint() },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(36.dp)
                                        ) {
                                            Icon(Icons.Default.Lightbulb, contentDescription = "Hint", modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Hint", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. FIXED BANNER AD SA ILALIM (50dp standard)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AdMobBannerView(
                        bannerId = uiState.adMobBannerId,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Big 3D "YOU WIN" or "TRY AGAIN" Overlay
            WinnerLoserOverlay(
                isWin = uiState.isSolved,
                isLose = uiState.isGameOverDefeat,
                onNextLevel = {
                    if (uiState.gameMode == GamePlayMode.PUZZLES_2000) {
                        viewModel.nextPuzzle()
                    } else {
                        viewModel.startLobbyGame(uiState.gameMode, uiState.timeControl, uiState.aiLevel)
                    }
                },
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

/**
 * 3rd Dashboard Top Bar: Exactly 1 single line containing username, countdown timer, country & rank globe!
 */
@Composable
fun SingleLineTopBar(
    playerUsername: String,
    playerColor: PieceColor,
    playerFlag: String,
    playerRating: Int,
    playerSeconds: Int,
    opponentUsername: String,
    opponentFlag: String,
    opponentRating: Int,
    opponentSeconds: Int,
    isPuzzleMode: Boolean,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onBackToLobby: () -> Unit,
    onToggleTheme: () -> Unit
) {
    val mPlayer = playerSeconds / 60
    val sPlayer = playerSeconds % 60
    val playerTimeFmt = String.format("%02d:%02d", mPlayer, sPlayer)

    val mOpp = opponentSeconds / 60
    val sOpp = opponentSeconds % 60
    val oppTimeFmt = String.format("%02d:%02d", mOpp, sOpp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(Color(0xFF141719))
            .border(1.dp, Color(0xFF303638))
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Back Arrow + YOU Info (YOU: Color, Rating, Timer)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackToLobby, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFFE5B94F), modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "YOU: ${if (playerColor == PieceColor.WHITE) "White" else "Black"}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$playerRating",
                fontSize = 10.5.sp,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .background(Color(0xFF1B1F22), RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFF303638), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "⏱️ $playerTimeFmt",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFE5B94F)
                )
            }
        }

        // Center: VS
        Text(
            text = "VS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFE5B94F)
        )

        // Right: OPPONENT Info (Opponent Name, Rating, Timer) + Sound + Palette
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .background(Color(0xFF1B1F22), RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFF303638), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "⏱️ $oppTimeFmt",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF38BDF8)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "OPPONENT: $opponentUsername",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$opponentRating",
                fontSize = 10.5.sp,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onToggleMute, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = if (isMuted) "Unmute Audio" else "Mute Audio",
                    tint = if (isMuted) Color(0xFFEF4444) else Color(0xFFE5B94F),
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(onClick = onToggleTheme, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Palette, contentDescription = "Theme", tint = Color(0xFF769656), modifier = Modifier.size(16.dp))
            }
        }
    }
}
