package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val PremiumDarkColorScheme =
  darkColorScheme(
    primary = PremiumGold,
    onPrimary = Color(0xFF141719),
    primaryContainer = Color(0xFF2E2611),
    onPrimaryContainer = PremiumGold,
    secondary = TournamentGreenDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1B2818),
    onSecondaryContainer = TournamentGreenDark,
    tertiary = CyanAccent,
    background = PremiumBlackBg,
    onBackground = TextPrimary,
    surface = CharcoalCard,
    onSurface = TextPrimary,
    surfaceVariant = CharcoalCardElevated,
    onSurfaceVariant = TextSecondary,
    outline = SubtleGraphiteBorder,
    outlineVariant = Color(0xFF222628)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = PremiumDarkColorScheme,
    typography = Typography,
    content = content
  )
}
