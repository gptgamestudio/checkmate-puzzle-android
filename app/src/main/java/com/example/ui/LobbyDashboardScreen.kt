package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.AiLevel
import com.example.model.CompetitiveRank
import com.example.model.GamePlayMode
import com.example.model.LeaderboardPlayer
import com.example.model.MatchHistoryItem
import com.example.model.TimeControl
import com.example.model.UserSession

@Composable
fun LobbyDashboardScreen(
    userSession: UserSession,
    adBannerId: String,
    solvedCount: Int,
    availableOnlineCredits: Int,
    rewardedOnlineEntriesUsedToday: Int,
    nextCreditResetTimeText: String,
    competitiveRank: CompetitiveRank,
    isMatchmaking: Boolean,
    matchmakingStatus: String,
    matchmakingSearchRange: Int,
    matchHistory: List<MatchHistoryItem>,
    leaderboard: List<LeaderboardPlayer>,
    statusMessage: String,
    onWatchRewardedOnlineAd: () -> Unit,
    onStartMatchmaking: (TimeControl) -> Unit,
    onCancelMatchmaking: () -> Unit,
    onStartGame: (mode: GamePlayMode, timeControl: TimeControl, aiLevel: AiLevel) -> Unit,
    onOpenStrategy: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMode by remember { mutableStateOf(GamePlayMode.ONLINE_MULTIPLAYER) }
    var selectedTimeControl by remember { mutableStateOf(TimeControl.MIN_5) }
    var selectedAiLevel by remember { mutableStateOf(AiLevel.MASTER) }
    var showOwnerDialog by remember { mutableStateOf(false) }
    var selectedLobbyTab by remember { mutableStateOf(0) } // 0: Play Modes, 1: Leaderboard, 2: History

    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF030A1A), Color(0xFF06142E), Color(0xFF030A1A))
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgGradient)
            .testTag("lobby_dashboard_screen"),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Main scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // Header: User Profile with Competitive Rank Badge & Server Status
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1835)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF0070F3))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: User Info with Rank & Badge
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(
                                    Brush.radialGradient(listOf(Color(0xFFE5B94F), Color(0xFF997A2E))),
                                    CircleShape
                                )
                                .border(2.dp, Color(0xFFF3C763), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(userSession.countryFlag, fontSize = 22.sp)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = userSession.username,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.5.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                // Rank Badge (e.g. 👑 Grandmaster / 🏆 Master)
                                Box(
                                    modifier = Modifier
                                        .background(Color(competitiveRank.colorHex).copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                                        .border(1.dp, Color(competitiveRank.colorHex), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "${competitiveRank.badge} ${competitiveRank.title}",
                                        fontSize = 9.sp,
                                        color = Color(competitiveRank.colorHex),
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                            Text(
                                text = "Elo: ${userSession.rating} 🌐 • Rank: ${competitiveRank.title} • Record: ${userSession.totalWins}W / ${userSession.totalLosses}L",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    // Right: Server Status & Actions
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF071938), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF0070F3), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "⚡ SERVER ONLINE",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF38BDF8)
                                )
                                Text(
                                    text = "Matchmaking Active",
                                    fontSize = 8.5.sp,
                                    color = Color(0xFF22C55E)
                                )
                            }
                        }
                        IconButton(onClick = { showOwnerDialog = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Security, contentDescription = "Owner Settings", tint = Color(0xFFE5B94F), modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onLogout, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sub-navigation Tabs: [Game Modes] [Leaderboard] [Match History]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TabButton(
                    title = "🎮 Play Modes",
                    isSelected = selectedLobbyTab == 0,
                    onClick = { selectedLobbyTab = 0 },
                    modifier = Modifier.weight(1f)
                )
                TabButton(
                    title = "🏆 Leaderboard",
                    isSelected = selectedLobbyTab == 1,
                    onClick = { selectedLobbyTab = 1 },
                    modifier = Modifier.weight(1f)
                )
                TabButton(
                    title = "📜 Match History",
                    isSelected = selectedLobbyTab == 2,
                    onClick = { selectedLobbyTab = 2 },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // TAB 0: MAIN GAME MODES & ONLINE REWARDED ENTRY HUB
            if (selectedLobbyTab == 0) {
                // Section B: Free Online Game Entry System Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF071938)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF0070F3))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🎟️", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "FREE ONLINE REWARDED ENTRY",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "1 Rewarded Ad = 1 Online Session (Continuous on Win Streak)",
                                        fontSize = 9.5.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            // Credit Counter Badge (Remaining out of 10)
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF0A255C), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$availableOnlineCredits Available",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFFD700)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Progress row & reset countdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Daily Limit: $rewardedOnlineEntriesUsedToday / 10 used",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (rewardedOnlineEntriesUsedToday >= 10) Color(0xFFEF4444) else Color(0xFF38BDF8)
                            )
                            Text(
                                text = "⏳ $nextCreditResetTimeText",
                                fontSize = 10.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }

                        // Max limit notice if all 10 used
                        if (rewardedOnlineEntriesUsedToday >= 10) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D).copy(alpha = 0.6f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "⚠️ Your 10 free online entries have been used. Come back when your next 24-hour entry window becomes available. ($nextCreditResetTimeText)",
                                    fontSize = 10.sp,
                                    color = Color(0xFFFCA5A5),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onWatchRewardedOnlineAd,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066E0)),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .testTag("watch_rewarded_entry_button")
                            ) {
                                Icon(Icons.Default.Videocam, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Watch Ad to Unlock +1 Online Entry (${10 - rewardedOnlineEntriesUsedToday} left today)",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section Title: Game Modes
                Text(
                    text = "PUMILI NG LARO (GAME SELECTION)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Equal-sized Buttons Grid (2x2 layout)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EqualLobbyButton(
                        title = "PLAY NOW ONLINE",
                        subtitle = "Global Elo Ladder Match",
                        icon = Icons.Default.Bolt,
                        iconColor = Color(0xFF22C55E),
                        isSelected = selectedMode == GamePlayMode.ONLINE_MULTIPLAYER,
                        onClick = { selectedMode = GamePlayMode.ONLINE_MULTIPLAYER },
                        modifier = Modifier.weight(1f)
                    )

                    EqualLobbyButton(
                        title = "2,000 PUZZLES",
                        subtitle = "Level by level checkmate",
                        icon = Icons.Default.Extension,
                        iconColor = Color(0xFFF59E0B),
                        isSelected = selectedMode == GamePlayMode.PUZZLES_2000,
                        onClick = { selectedMode = GamePlayMode.PUZZLES_2000 },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EqualLobbyButton(
                        title = "PLAY WITH AI",
                        subtitle = "Master / Hard / Grand IM",
                        icon = Icons.Default.SmartToy,
                        iconColor = Color(0xFFA855F7),
                        isSelected = selectedMode == GamePlayMode.PLAY_WITH_AI,
                        onClick = { selectedMode = GamePlayMode.PLAY_WITH_AI },
                        modifier = Modifier.weight(1f)
                    )

                    EqualLobbyButton(
                        title = "STRATEGY GUIDE",
                        subtitle = "GM tactics & principles",
                        icon = Icons.Default.Book,
                        iconColor = Color(0xFF38BDF8),
                        isSelected = selectedMode == GamePlayMode.STRATEGY_STUDY,
                        onClick = {
                            selectedMode = GamePlayMode.STRATEGY_STUDY
                            onOpenStrategy()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // AI Level Selector (if Play with AI is selected)
                if (selectedMode == GamePlayMode.PLAY_WITH_AI) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "AI ENGINE DIFFICULTY:",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA855F7)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AiLevel.values().forEach { level ->
                            val isLevelSelected = selectedAiLevel == level
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isLevelSelected) Color(0xFFA855F7).copy(alpha = 0.3f) else Color(0xFF131927),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isLevelSelected) Color(0xFFA855F7) else Color(0xFF27324A),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedAiLevel = level }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${level.title} (${level.rating})",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLevelSelected) Color.White else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Time Control Selection (3 min, 5 min, 10 min)
                Text(
                    text = "PUMILI NG ORAS (SELECT MINUTES):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TimeControl.values().forEach { tc ->
                        val isTcSelected = selectedTimeControl == tc
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isTcSelected) Color(0xFF2563EB).copy(alpha = 0.3f) else Color(0xFF131927),
                                    RoundedCornerShape(10.dp)
                                )
                                .border(
                                    1.5.dp,
                                    if (isTcSelected) Color(0xFF38BDF8) else Color(0xFF27324A),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedTimeControl = tc }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = if (isTcSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${tc.minutes} MIN",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isTcSelected) Color.White else Color(0xFFCBD5E1)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Status notice
                if (statusMessage.isNotBlank()) {
                    Text(
                        text = statusMessage,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                }

                // Main START GAME / MATCHMAKING BUTTON
                Button(
                    onClick = {
                        if (selectedMode == GamePlayMode.ONLINE_MULTIPLAYER) {
                            onStartMatchmaking(selectedTimeControl)
                        } else {
                            onStartGame(selectedMode, selectedTimeControl, selectedAiLevel)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (selectedMode) {
                            GamePlayMode.ONLINE_MULTIPLAYER -> Color(0xFF16A34A)
                            GamePlayMode.PLAY_WITH_AI -> Color(0xFF9333EA)
                            else -> Color(0xFFD97706)
                        }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("start_game_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (selectedMode) {
                                GamePlayMode.ONLINE_MULTIPLAYER -> "⚔️ GLOBAL ONLINE MATCHMAKING (${selectedTimeControl.label})"
                                GamePlayMode.PLAY_WITH_AI -> "SIMULAN ANG LARO LABAN SA AI (${selectedAiLevel.title})"
                                GamePlayMode.STRATEGY_STUDY -> "BUKSAN ANG STRATEGY ARALIN"
                                else -> "SIMULAN ANG 2,000 PUZZLES (LEVEL BY LEVEL)"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }

            // TAB 1: GLOBAL COMPETITIVE LEADERBOARD (Section F & H)
            if (selectedLobbyTab == 1) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF071938)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF0070F3))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "👑", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "GLOBAL CHESS LADDER",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "Top Rated Players",
                                fontSize = 11.sp,
                                color = Color(0xFF38BDF8)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        leaderboard.take(15).forEach { player ->
                            val isMe = player.username == userSession.username
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isMe) Color(0xFF0052CC).copy(alpha = 0.35f) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        if (isMe) 1.dp else 0.dp,
                                        if (isMe) Color(0xFF38BDF8) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "#${player.rank}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (player.rank <= 3) Color(0xFFFFD700) else Color(0xFF94A3B8),
                                        modifier = Modifier.width(28.dp)
                                    )
                                    Text(text = player.flag, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = player.username,
                                                fontSize = 12.sp,
                                                fontWeight = if (isMe) FontWeight.Black else FontWeight.Bold,
                                                color = if (isMe) Color(0xFFFFD700) else Color.White
                                            )
                                            if (player.title.isNotBlank()) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(Color(0xFFDC2626), RoundedCornerShape(3.dp))
                                                        .padding(horizontal = 3.dp, vertical = 1.dp)
                                                ) {
                                                    Text(player.title, fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Black)
                                                }
                                            }
                                        }
                                        Text(
                                            text = "${player.competitiveRank.badge} ${player.competitiveRank.title}",
                                            fontSize = 9.5.sp,
                                            color = Color(player.competitiveRank.colorHex)
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${player.rating} Elo",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF38BDF8)
                                    )
                                    Text(
                                        text = "${player.wins}W / ${player.losses}L",
                                        fontSize = 9.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }

            // TAB 2: MATCH HISTORY (Section F & H)
            if (selectedLobbyTab == 2) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF071938)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF0070F3))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "📜", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "RECENT ONLINE MATCH HISTORY",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "Verified Elo Games",
                                fontSize = 11.sp,
                                color = Color(0xFF38BDF8)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (matchHistory.isEmpty()) {
                            Text(
                                text = "No matches played yet. Start a global match to record your history!",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            matchHistory.forEach { match ->
                                val isWin = match.result == "VICTORY"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0A1835), RoundedCornerShape(8.dp))
                                        .border(
                                            1.dp,
                                            if (isWin) Color(0xFF22C55E).copy(alpha = 0.5f) else Color(0xFFEF4444).copy(alpha = 0.5f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (isWin) Color(0xFF16A34A).copy(alpha = 0.25f) else Color(0xFFDC2626).copy(alpha = 0.25f),
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = if (isWin) "WIN" else "LOSS",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isWin) Color(0xFF4ADE80) else Color(0xFFF87171)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(match.opponentFlag, fontSize = 13.sp)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "vs ${match.opponentName}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                            Text(
                                                text = "Opponent Elo: ${match.opponentRating}",
                                                fontSize = 9.5.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (match.ratingDelta >= 0) "+${match.ratingDelta} Elo" else "${match.ratingDelta} Elo",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (match.ratingDelta >= 0) Color(0xFF4ADE80) else Color(0xFFF87171)
                                        )
                                        Text(
                                            text = "Rating: ${match.newRating}",
                                            fontSize = 9.sp,
                                            color = Color(0xFFCBD5E1)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Live APK Download & Share Card
            val context = androidx.compose.ui.platform.LocalContext.current
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        try {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://github.com/gptgamestudio/checkmate-puzzle-android/releases/latest/download/checkmate-puzzle.apk")
                            )
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    .testTag("download_live_apk_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF071938)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF20456E))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFF38BDF8).copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "LIVE APK DOWNLOAD (v1.0.0)",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "25 MB",
                                    fontSize = 9.sp,
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "gptgamestudio.github.io/checkmate-puzzle-android",
                                fontSize = 9.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                    Button(
                        onClick = {
                            try {
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://github.com/gptgamestudio/checkmate-puzzle-android/releases/latest/download/checkmate-puzzle.apk")
                                )
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            text = "GET APK 📥",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Section C: AUTOMATIC GLOBAL MATCHMAKING MODAL DIALOG
        if (isMatchmaking) {
            Dialog(onDismissRequest = onCancelMatchmaking) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(12.dp)
                        .testTag("matchmaking_modal_dialog"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF071938)),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF0070F3))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🌐 GLOBAL ONLINE MATCHMAKING",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        CircularProgressIndicator(
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.size(54.dp),
                            strokeWidth = 4.dp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = matchmakingStatus,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Matchmaking details
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF030D22), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Your Rating", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                Text("${userSession.rating} Elo", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF38BDF8))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Search Range", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                Text("±$matchmakingSearchRange Elo", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF22C55E))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Rank Tier", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                Text(competitiveRank.title, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(competitiveRank.colorHex))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onCancelMatchmaking,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .testTag("cancel_matchmaking_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cancel Search", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        if (showOwnerDialog) {
            OwnerDashboardDialog(
                initialEmail = userSession.email.ifBlank { "analynlandingin77@gmail.com" },
                onDismiss = { showOwnerDialog = false }
            )
        }

        // Small Locked AdMob Banner at bottom of Lobby Dashboard
        AdMobBannerView(
            bannerId = adBannerId,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun TabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .background(
                if (isSelected) Color(0xFF0066E0) else Color(0xFF0B172E),
                RoundedCornerShape(8.dp)
            )
            .border(
                1.dp,
                if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E293B),
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 11.5.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
            color = if (isSelected) Color.White else Color(0xFF94A3B8)
        )
    }
}

@Composable
fun EqualLobbyButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(78.dp)
            .clickable { onClick() }
            .testTag("lobby_button_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF071938) else Color(0xFF0A1835)
        ),
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) Color(0xFF0070F3) else Color(0xFF1E293B)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconColor.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                    .border(1.dp, iconColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    maxLines = 1
                )
            }
        }
    }
}
