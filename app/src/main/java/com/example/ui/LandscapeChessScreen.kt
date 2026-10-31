package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
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
                availableOnlineCredits = uiState.availableOnlineCredits,
                rewardedOnlineEntriesUsedToday = uiState.rewardedOnlineEntriesUsedToday,
                nextCreditResetTimeText = uiState.nextCreditResetTimeText,
                competitiveRank = uiState.competitiveRank,
                isMatchmaking = uiState.isMatchmaking,
                matchmakingStatus = uiState.matchmakingStatus,
                matchmakingSearchRange = uiState.matchmakingSearchRange,
                matchHistory = uiState.matchHistory,
                leaderboard = uiState.leaderboard,
                statusMessage = uiState.statusMessage,
                onWatchRewardedOnlineAd = { viewModel.watchRewardedAdForOnlineEntry() },
                onStartMatchmaking = { tc -> viewModel.startGlobalMatchmaking(tc) },
                onCancelMatchmaking = { viewModel.cancelMatchmaking() },
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

    // Level Picker Menu for 2,000 Puzzles
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

    // =========================================================================
    // 3RD DASHBOARD: SAKANA CHESS PRO MASTER 16:9 REDESIGN
    // (Unified Oversized Chessboard for 2,000 Puzzles, Online, and AI)
    // =========================================================================

    // Deep Navy & Electric Blue Gradient matching the reference design
    val mainBgGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF030A1A),
            Color(0xFF07142E),
            Color(0xFF030A1A)
        )
    )

    var isMuted by remember {
        mutableStateOf(com.example.util.ChessAudioPlayer.isMuted)
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("landscape_chess_screen"),
        color = Color(0xFF030A1A)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(mainBgGradient)
        ) {
            // Subtle glowing electric blue and gold decorative ambient graphics
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF0052CC).copy(alpha = 0.18f),
                                Color.Transparent,
                                Color(0xFF003882).copy(alpha = 0.15f)
                            )
                        )
                    )
            )

            Column(modifier = Modifier.fillMaxSize()) {

                // 1. SAKANA PRO MASTER TOP BAR (YOU vs OPPONENT with Glowing VS, Timers & Avatars)
                SakanaProMasterTopBar(
                    playerUsername = uiState.userSession.username,
                    playerRating = uiState.userSession.rating,
                    playerSeconds = if (uiState.gameMode == GamePlayMode.PUZZLES_2000) uiState.timeRemainingSeconds else uiState.playerTimeRemainingSeconds,
                    opponentUsername = when (uiState.gameMode) {
                        GamePlayMode.PUZZLES_2000 -> "GM Level ${uiState.currentPuzzleIndex + 1}"
                        GamePlayMode.ONLINE_MULTIPLAYER -> uiState.currentChallenger.username
                        GamePlayMode.PLAY_WITH_AI -> "AI ${uiState.aiLevel.title}"
                        else -> "Grandmaster"
                    },
                    opponentRating = when (uiState.gameMode) {
                        GamePlayMode.PUZZLES_2000 -> 2200
                        GamePlayMode.ONLINE_MULTIPLAYER -> uiState.currentChallenger.rating
                        GamePlayMode.PLAY_WITH_AI -> uiState.aiLevel.rating
                        else -> 2200
                    },
                    opponentSeconds = if (uiState.gameMode == GamePlayMode.PUZZLES_2000) 300 else uiState.opponentTimeRemainingSeconds,
                    onBackClick = { viewModel.navigateToDashboard(DashboardState.LOBBY_DASHBOARD) },
                    onMenuClick = { viewModel.toggleSettingsDialog(true) }
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
                            .background(Color(0xFF0070F3))
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

                // 2. MAIN 16:9 GAME AREA: OVERSIZED CHESSBOARD + COMPACT RIGHT DASHBOARD
                // Banner ad is REMOVED from gameplay screen as instructed to maximize board height!
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val availableW = maxWidth
                    val availableH = maxHeight

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left / Center Pane: OVERSIZED CHESSBOARD (The undisputed star of the screen)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Calculate dynamic board size to use maximum practical screen height (~94–96%)
                            val boardSize = availableH * 0.96f

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
                                modifier = Modifier.size(boardSize)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Right Pane: SAKANA RIGHT DASHBOARD (Exact Match to Reference Design)
                        SakanaRightDashboard(
                            gameMode = uiState.gameMode,
                            currentPuzzleIndex = uiState.currentPuzzleIndex,
                            puzzleTitle = uiState.currentPuzzle.title,
                            activeColor = uiState.gameState.activeColor,
                            playerSide = uiState.playerSide,
                            isSolved = uiState.isSolved,
                            lastMoveNotation = uiState.lastMoveNotationText,
                            rewardedHintsUsedToday = uiState.rewardedHintsUsedToday,
                            statusMessage = uiState.statusMessage,
                            onWatchAdHint = { viewModel.watchRewardedAdForHint() },
                            onUndo = { viewModel.undoMove() },
                            onRedo = { viewModel.redoMove() },
                            onHint = { viewModel.requestHint() },
                            onSettings = { viewModel.toggleSettingsDialog(true) },
                            onResign = {
                                if (uiState.gameMode == GamePlayMode.PUZZLES_2000) {
                                    if (uiState.isSolved) viewModel.nextPuzzle() else viewModel.restartPuzzle()
                                } else {
                                    viewModel.resignGame()
                                }
                            },
                            modifier = Modifier
                                .widthIn(min = 210.dp, max = 245.dp)
                                .fillMaxHeight()
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }

            // In-Game Settings Modal Dialog
            if (uiState.showSettingsDialog) {
                InGameSettingsDialog(
                    isMuted = isMuted,
                    isGreenTheme = uiState.isGreenTheme,
                    isPuzzleMode = uiState.gameMode == GamePlayMode.PUZZLES_2000,
                    onToggleMute = {
                        isMuted = com.example.util.ChessAudioPlayer.toggleMute()
                    },
                    onToggleTheme = { viewModel.toggleBoardTheme() },
                    onSendEmote = { emote -> viewModel.sendEmote(emote) },
                    onOpenLevelSelect = {
                        viewModel.toggleSettingsDialog(false)
                        viewModel.toggleLevelSelectMenu(true)
                    },
                    onExitToLobby = {
                        viewModel.toggleSettingsDialog(false)
                        viewModel.navigateToDashboard(DashboardState.LOBBY_DASHBOARD)
                    },
                    onDismiss = { viewModel.toggleSettingsDialog(false) }
                )
            }

            // Big 3D "YOU WIN" or "TRY AGAIN" Victory Overlay
            WinnerLoserOverlay(
                isWin = uiState.isSolved && !uiState.isReviewMode,
                isLose = uiState.isGameOverDefeat,
                isPuzzleMode = uiState.gameMode == GamePlayMode.PUZZLES_2000,
                isOnlineMode = uiState.gameMode == GamePlayMode.ONLINE_MULTIPLAYER,
                ratingDelta = uiState.lastRatingDelta,
                winStreak = uiState.onlineSessionWinStreak,
                isAutoSearching = uiState.isAutoSearchingNextChallenger,
                autoSearchCountdown = uiState.autoSearchCountdown,
                onNextLevel = {
                    if (uiState.gameMode == GamePlayMode.PUZZLES_2000) {
                        viewModel.nextPuzzle()
                    } else {
                        viewModel.cancelAutoSearch()
                        viewModel.startLobbyGame(uiState.gameMode, uiState.timeControl, uiState.aiLevel)
                    }
                },
                onRetry = {
                    if (uiState.gameMode == GamePlayMode.PUZZLES_2000) {
                        viewModel.restartPuzzle()
                    } else {
                        viewModel.startLobbyGame(uiState.gameMode, uiState.timeControl, uiState.aiLevel)
                    }
                },
                onReview = { viewModel.enterReviewMode() },
                onWatchAdForHelp = { viewModel.watchRewardedAdForHint() },
                onReturnToDashboard = { viewModel.navigateToDashboard(DashboardState.LOBBY_DASHBOARD) }
            )

            // Interstitial Ad Dialog Overlay (Controlled frequency after winning checkmate)
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
 * Screen-Accurate Top Bar Matching Reference Design:
 * Glowing electric blue neon back button, Left Player card, Radiant Golden VS, Right Player card, and Menu button.
 */
@Composable
fun SakanaProMasterTopBar(
    playerUsername: String,
    playerRating: Int,
    playerSeconds: Int,
    opponentUsername: String,
    opponentRating: Int,
    opponentSeconds: Int,
    onBackClick: () -> Unit,
    onMenuClick: () -> Unit
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
            .height(50.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF030D22), Color(0xFF051433), Color(0xFF020919))
                )
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Circular Electric Blue Back Button
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(Color(0xFF0A1835), CircleShape)
                .border(2.dp, Color(0xFF0070F3), CircleShape)
                .border(1.dp, Color(0xFFD4AF37), CircleShape)
                .clickable { onBackClick() }
                .testTag("topbar_back_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        // Left Player Info Card (Avatar + Name & Rating + Blue Timer Pill)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Royal Avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0052CC), CircleShape)
                    .border(1.5.dp, Color(0xFFD4AF37), CircleShape)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.avatar_winner_1793081380198),
                    contentDescription = "Player Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Name & Trophy Rating
            Column {
                Text(
                    text = playerUsername.ifBlank { "Player" },
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🏆", fontSize = 9.sp)
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "$playerRating",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B)
                    )
                }
            }

            // Electric Blue Timer Pill
            Box(
                modifier = Modifier
                    .background(Color(0xFF071B3D), RoundedCornerShape(10.dp))
                    .border(1.5.dp, Color(0xFF0070F3), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "⏱️", fontSize = 10.sp)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = playerTimeFmt,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }
            }
        }

        // Center: RADIANT 3D GOLDEN "VS"
        Box(
            contentAlignment = Alignment.Center
        ) {
            // Glowing background aura
            Text(
                text = "VS",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFF59E0B).copy(alpha = 0.35f),
                modifier = Modifier.padding(2.dp)
            )
            Text(
                text = "VS",
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFFD700)
            )
        }

        // Right Opponent Info Card (Blue Timer Pill + Name & Rating + Avatar with Online Dot)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Electric Blue Timer Pill
            Box(
                modifier = Modifier
                    .background(Color(0xFF071B3D), RoundedCornerShape(10.dp))
                    .border(1.5.dp, Color(0xFF0070F3), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "⏱️", fontSize = 10.sp)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = oppTimeFmt,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }
            }

            // Name & Trophy Rating
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = opponentUsername,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🏆", fontSize = 9.sp)
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "$opponentRating",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B)
                    )
                }
            }

            // Opponent Avatar with Online Status Dot
            Box(
                modifier = Modifier
                    .size(36.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color(0xFF003882), CircleShape)
                        .border(1.5.dp, Color(0xFFD4AF37), CircleShape)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.avatar_winner_1793081380198),
                        contentDescription = "Opponent Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                // Bright Green Online Dot
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(Color(0xFF22C55E), CircleShape)
                        .border(1.dp, Color.Black, CircleShape)
                        .align(Alignment.BottomEnd)
                )
            }
        }

        // Right: Circular Electric Blue Menu Button
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(Color(0xFF0A1835), CircleShape)
                .border(2.dp, Color(0xFF0070F3), CircleShape)
                .border(1.dp, Color(0xFFD4AF37), CircleShape)
                .clickable { onMenuClick() }
                .testTag("topbar_menu_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Screen-Accurate Right Dashboard Matching Reference Design:
 * - Top: Mode Title + Turn Status + Move Notation
 * - Middle: Watch Ad +1 Hint Card (Electric blue with gold clapperboard)
 * - 2x2 Grid: Undo, Redo, Hint, Settings
 * - Bottom: Full-Width Resign Button
 */
@Composable
fun SakanaRightDashboard(
    gameMode: GamePlayMode,
    currentPuzzleIndex: Int,
    puzzleTitle: String,
    activeColor: PieceColor,
    playerSide: PieceColor,
    isSolved: Boolean,
    lastMoveNotation: String,
    rewardedHintsUsedToday: Int,
    statusMessage: String,
    onWatchAdHint: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onHint: () -> Unit,
    onSettings: () -> Unit,
    onResign: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. MATCH MODE & TURN NOTATION CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1B3B)),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF0070F3))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = when (gameMode) {
                            GamePlayMode.PUZZLES_2000 -> Icons.Default.GridOn
                            GamePlayMode.PLAY_WITH_AI -> Icons.Default.SmartToy
                            else -> Icons.Default.Public
                        },
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = when (gameMode) {
                            GamePlayMode.PUZZLES_2000 -> "2,000 Puzzles"
                            GamePlayMode.PLAY_WITH_AI -> "AI Match"
                            else -> "Online Match"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Turn Indicator
                Text(
                    text = if (isSolved) {
                        "Checkmate! Victory! 🎉"
                    } else if (activeColor == playerSide) {
                        "Your Turn: Move ${if (playerSide == PieceColor.WHITE) "White" else "Black"}"
                    } else {
                        "Opponent's Turn..."
                    },
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSolved) Color(0xFF22C55E) else Color(0xFF93C5FD)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Move Notation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF040E22), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = lastMoveNotation.ifBlank { "1. e4  e5" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }
            }
        }

        // 2. WATCH AD +1 HINT REWARDED CARD (Matching Reference Image)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onWatchAdHint() }
                .testTag("watch_ad_hint_button"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0066E0)
            ),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Gold Clapperboard Emoji / Icon Box
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF072459), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🎬", fontSize = 20.sp)
                }

                Column {
                    Text(
                        text = "Watch Ad",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "+1 Hint",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD700)
                    )
                    Text(
                        text = "(${10 - rewardedHintsUsedToday}/10 per 24h)",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFBAE6FD)
                    )
                }
            }
        }

        // 3. 2x2 ACTION BUTTON GRID (Undo, Redo, Hint, Settings)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SakanaActionButton(
                label = "Undo",
                icon = "↩",
                onClick = onUndo,
                modifier = Modifier.weight(1f)
            )
            SakanaActionButton(
                label = "Redo",
                icon = "↪",
                onClick = onRedo,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SakanaActionButton(
                label = "Hint",
                icon = "💡",
                onClick = onHint,
                modifier = Modifier.weight(1f)
            )
            SakanaActionButton(
                label = "Settings",
                icon = "⚙️",
                onClick = onSettings,
                modifier = Modifier.weight(1f)
            )
        }

        // 4. FULL-WIDTH RESIGN / NEXT LEVEL BUTTON
        Button(
            onClick = onResign,
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .testTag("sakana_resign_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSolved) Color(0xFF16A34A) else Color(0xFF0066E0)
            ),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (isSolved) Color(0xFF86EFAC) else Color(0xFF38BDF8)
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isSolved) {
                    Text(text = "Next Puzzle ➔", fontSize = 12.5.sp, fontWeight = FontWeight.Black, color = Color.White)
                } else {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = "Resign",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (gameMode == GamePlayMode.PUZZLES_2000) "Reset Puzzle" else "Resign",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun SakanaActionButton(
    label: String,
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(52.dp)
            .clickable { onClick() }
            .testTag("action_button_${label.lowercase()}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF071938)),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF0070F3))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = icon, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * In-Game Settings Dialog (Audio, Theme, Quick Emotes, Level Picker, Exit)
 */
@Composable
fun InGameSettingsDialog(
    isMuted: Boolean,
    isGreenTheme: Boolean,
    isPuzzleMode: Boolean,
    onToggleMute: () -> Unit,
    onToggleTheme: () -> Unit,
    onSendEmote: (String) -> Unit,
    onOpenLevelSelect: () -> Unit,
    onExitToLobby: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B172E)),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF0070F3))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚙️ GAME SETTINGS",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Sound Effect Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF060E1E), RoundedCornerShape(8.dp))
                        .clickable { onToggleMute() }
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = if (isMuted) Color(0xFFEF4444) else Color(0xFF22C55E),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Game Sound Effects", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Text(text = if (isMuted) "MUTED" else "FULL VOL", fontSize = 11.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Board Theme Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF060E1E), RoundedCornerShape(8.dp))
                        .clickable { onToggleTheme() }
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Board Squares Theme", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Text(text = if (isGreenTheme) "Green/Cream" else "Slate", fontSize = 11.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Emotes
                Text(
                    text = "QUICK EMOTES:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("WOW! 🤩", "GG! 🤝", "Good Luck! ✨", "OOPSIE! 😅").forEach { emote ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFF071938), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF0070F3), RoundedCornerShape(6.dp))
                                .clickable {
                                    onSendEmote(emote)
                                    onDismiss()
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emote, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons: Level Select (if puzzle) & Exit to Lobby
                if (isPuzzleMode) {
                    Button(
                        onClick = onOpenLevelSelect,
                        modifier = Modifier.fillMaxWidth().height(38.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066E0)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("2,000 Puzzles Level Picker", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = onExitToLobby,
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Exit to Lobby", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
