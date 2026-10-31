package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@Composable
fun WinnerLoserOverlay(
    isWin: Boolean,
    isLose: Boolean,
    isPuzzleMode: Boolean,
    isOnlineMode: Boolean,
    ratingDelta: Int,
    winStreak: Int,
    isAutoSearching: Boolean,
    autoSearchCountdown: Int,
    onNextLevel: () -> Unit,
    onRetry: () -> Unit,
    onReview: () -> Unit,
    onWatchAdForHelp: () -> Unit,
    onReturnToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isWin && !isLose) return

    val bannerRes = if (isWin) R.drawable.you_win_banner_1793081437626 else R.drawable.you_lose_banner_1793081465834

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(enabled = false) {}
            .testTag("winner_loser_overlay"),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
            exit = fadeOut() + scaleOut()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                // Big 3D Arched Ribbon Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(150.dp)
                        .clip(RoundedCornerShape(18.dp))
                ) {
                    Image(
                        painter = painterResource(id = bannerRes),
                        contentDescription = if (isWin) "You Win Banner" else "You Lose Banner",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Competitive Rating and Streak Card (Online & AI matches)
                if (isOnlineMode) {
                    Card(
                        modifier = Modifier
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF071938)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0070F3))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (isWin) "📈 Rating: +$ratingDelta Elo" else "📉 Rating: $ratingDelta Elo",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isWin) Color(0xFF4ADE80) else Color(0xFFF87171)
                            )
                            if (isWin && winStreak > 1) {
                                Text(
                                    text = "🔥 Win Streak: $winStreak",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFACC15)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action Buttons under banner
                if (isWin) {
                    if (isOnlineMode) {
                        // Section D: Continuous matchmaking after victory
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (isAutoSearching) {
                                Card(
                                    modifier = Modifier.padding(bottom = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF003D99)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                                ) {
                                    Text(
                                        text = "⚡ Matching next challenger in ${autoSearchCountdown}s... (No entry deducted)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = onNextLevel,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .shadow(8.dp, RoundedCornerShape(12.dp))
                                        .border(2.dp, Color(0xFFFACC15), RoundedCornerShape(12.dp))
                                        .testTag("overlay_next_challenger_button")
                                ) {
                                    Text(
                                        text = "NEXT CHALLENGER >",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, tint = Color.White)
                                }

                                Button(
                                    onClick = onReturnToDashboard,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("overlay_exit_dashboard_button")
                                ) {
                                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Lobby", fontSize = 13.sp, color = Color.White)
                                }
                            }
                        }
                    } else {
                        // Section A: Puzzle win - Back to Review & Next Level
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = onReview,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF64748B)),
                                modifier = Modifier.testTag("overlay_review_button")
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Back to Review", fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Button(
                                onClick = onNextLevel,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .shadow(8.dp, RoundedCornerShape(12.dp))
                                    .border(2.dp, Color(0xFFFACC15), RoundedCornerShape(12.dp))
                                    .testTag("overlay_next_button")
                            ) {
                                Text(
                                    text = "NEXT LEVEL >",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                } else {
                    // Section E: Defeat
                    if (isOnlineMode) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Session Ended. Return to online dashboard to queue again.",
                                fontSize = 11.5.sp,
                                color = Color(0xFFCBD5E1)
                            )
                            Button(
                                onClick = onReturnToDashboard,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066E0)),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8)),
                                modifier = Modifier.testTag("overlay_return_online_dashboard")
                            ) {
                                Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Return to Online Dashboard",
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        // Section A: Puzzle retry / watch ad for help
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = onRetry,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("overlay_retry_button")
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Try Again", fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Button(
                                onClick = onWatchAdForHelp,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066E0)),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFACC15)),
                                modifier = Modifier.testTag("overlay_watch_ad_help_button")
                            ) {
                                Text(text = "🎬", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Watch Ad for Help", fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
                            }
                        }
                    }
                }
            }
        }
    }
}
