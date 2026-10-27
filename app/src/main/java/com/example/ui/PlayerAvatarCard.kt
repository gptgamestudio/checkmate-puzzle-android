package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@Composable
fun PlayerAvatarCard(
    isPlayer: Boolean,
    name: String,
    rating: String,
    isWinner: Boolean,
    isLoser: Boolean,
    currentEmote: String?,
    modifier: Modifier = Modifier
) {
    // Determine which avatar face to display:
    // If winner -> avatar_winner (joyful thumbs up)
    // If loser -> avatar_loser (shocked wide-eyes gasping)
    val avatarRes = when {
        isWinner -> R.drawable.avatar_winner_1793081380198
        isLoser -> R.drawable.avatar_loser_1793081409645
        isPlayer -> R.drawable.avatar_winner_1793081380198
        else -> R.drawable.avatar_loser_1793081409645
    }

    val borderColor = when {
        isWinner -> Color(0xFFFACC15) // Golden
        isLoser -> Color(0xFFEF4444)  // Crimson
        else -> Color(0xFF3B82F6)   // Royal Blue
    }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .shadow(8.dp, RoundedCornerShape(12.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF222634), Color(0xFF13151D))
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(2.dp, borderColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar Image with royal frame
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .border(2.dp, borderColor, CircleShape)
            ) {
                Image(
                    painter = painterResource(id = avatarRes),
                    contentDescription = "$name Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                    if (isWinner) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "👑 WIN",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFACC15)
                        )
                    } else if (isLoser) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "💀 DEFEAT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFEF4444)
                        )
                    }
                }
                Text(
                    text = rating,
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        // Floating Speech Bubble Emote (e.g. "WOW!", "GG!", "OOPSIE")
        AnimatedVisibility(
            visible = !currentEmote.isNullOrEmpty(),
            enter = fadeIn() + scaleIn(animationSpec = tween(200)),
            exit = fadeOut() + scaleOut(animationSpec = tween(200)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(bottom = 30.dp, end = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .shadow(4.dp, RoundedCornerShape(8.dp))
                    .background(Color(0xFFFACC15), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFB45309), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = currentEmote ?: "",
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    color = Color(0xFF0F172A)
                )
            }
        }
    }
}
