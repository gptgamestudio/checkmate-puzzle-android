package com.example.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AiLevel
import com.example.model.GamePlayMode
import com.example.model.TimeControl
import com.example.model.UserSession

@Composable
fun LobbyDashboardScreen(
    userSession: UserSession,
    adBannerId: String,
    solvedCount: Int,
    onStartGame: (mode: GamePlayMode, timeControl: TimeControl, aiLevel: AiLevel) -> Unit,
    onOpenStrategy: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMode by remember { mutableStateOf(GamePlayMode.PUZZLES_2000) }
    var selectedTimeControl by remember { mutableStateOf(TimeControl.MIN_5) }
    var selectedAiLevel by remember { mutableStateOf(AiLevel.MASTER) }
    var showOwnerDialog by remember { mutableStateOf(false) }

    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF080A0B), Color(0xFF111417), Color(0xFF080A0B))
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
            // Header: User Profile & Live Today Active Members
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141719)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF303638))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: User Info
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    Brush.radialGradient(listOf(Color(0xFFE5B94F), Color(0xFF997A2E))),
                                    CircleShape
                                )
                                .border(1.5.dp, Color(0xFFF3C763), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(userSession.countryFlag, fontSize = 20.sp)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = userSession.username,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF769656).copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text("🟢 Online", fontSize = 9.sp, color = Color(0xFF769656), fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                text = "Rating: ${userSession.rating} 🌐 • ${userSession.country} • Solved: $solvedCount/2000",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    // Right: Real Server Status & Logout
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF1B1F22), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF303638), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "⚡ SERVER ONLINE",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFE5B94F)
                                )
                                Text(
                                    text = "Matchmaking Active",
                                    fontSize = 8.5.sp,
                                    color = Color(0xFF94A3B8)
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

            // Section Title
            Text(
                text = "PUMILI NG LARO (GAME SELECTION)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Equal-sized Buttons Grid (2x2 layout pare-parehas ang laki!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EqualLobbyButton(
                    title = "2,000 PUZZLES",
                    subtitle = "Level by level checkmate",
                    icon = Icons.Default.Extension,
                    iconColor = Color(0xFFF59E0B),
                    isSelected = selectedMode == GamePlayMode.PUZZLES_2000,
                    onClick = { selectedMode = GamePlayMode.PUZZLES_2000 },
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

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EqualLobbyButton(
                    title = "PLAY NOW ONLINE",
                    subtitle = "Auto search global player",
                    icon = Icons.Default.Bolt,
                    iconColor = Color(0xFF22C55E),
                    isSelected = selectedMode == GamePlayMode.ONLINE_MULTIPLAYER,
                    onClick = { selectedMode = GamePlayMode.ONLINE_MULTIPLAYER },
                    modifier = Modifier.weight(1f)
                )

                EqualLobbyButton(
                    title = "PLAY WITH AI",
                    subtitle = "Master / Hard / Grand IM",
                    icon = Icons.Default.SmartToy,
                    iconColor = Color(0xFFA855F7),
                    isSelected = selectedMode == GamePlayMode.PLAY_WITH_AI,
                    onClick = { selectedMode = GamePlayMode.PLAY_WITH_AI },
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

            // Main START GAME BUTTON
            Button(
                onClick = {
                    onStartGame(selectedMode, selectedTimeControl, selectedAiLevel)
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
                            GamePlayMode.ONLINE_MULTIPLAYER -> "MAGHANAP NG KALABAN (AUTO-SEARCH ONLINE)"
                            GamePlayMode.PLAY_WITH_AI -> "SIMULAN ANG LARO LABAN SA AI (${selectedAiLevel.title})"
                            GamePlayMode.STRATEGY_STUDY -> "BUKSAN ANG STRATEGY ARALIN"
                            else -> "SIMULAN ANG 2,000 PUZZLES (LEVEL BY LEVEL)"
                        },
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101F30)),
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

        if (showOwnerDialog) {
            OwnerDashboardDialog(
                initialEmail = userSession.email.ifBlank { "analynlandingin77@gmail.com" },
                onDismiss = { showOwnerDialog = false }
            )
        }

        // Small Locked AdMob Banner at bottom of 2nd Dashboard
        AdMobBannerView(
            bannerId = adBannerId,
            modifier = Modifier.fillMaxWidth()
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
            containerColor = if (isSelected) Color(0xFF1B1F22) else Color(0xFF141719)
        ),
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) Color(0xFFE5B94F) else Color(0xFF303638)
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
