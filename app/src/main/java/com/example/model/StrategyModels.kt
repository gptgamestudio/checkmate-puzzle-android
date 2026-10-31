package com.example.model

enum class DashboardState {
    AUTH_DASHBOARD,   // 1st Dashboard: Login / Sign Up with password eye toggle & Google Sign In
    LOBBY_DASHBOARD,  // 2nd Dashboard: Username, Live Members (14,289), Equal Buttons Grid, 3m/5m/10m
    GAME_DASHBOARD    // 3rd Dashboard: Single-line top HUD, Tournament Green/White board, Online/AI/Puzzles
}

enum class GamePlayMode {
    PUZZLES_2000,
    ONLINE_MULTIPLAYER,
    PLAY_WITH_AI,
    STRATEGY_STUDY
}

enum class TimeControl(val minutes: Int, val totalSeconds: Int, val label: String) {
    MIN_3(3, 180, "3 Min Blitz"),
    MIN_5(5, 300, "5 Min Rapid"),
    MIN_10(10, 600, "10 Min Classical")
}

enum class AiLevel(val title: String, val rating: Int, val description: String) {
    HARD("Hard Engine", 2000, "Advanced club master tactics"),
    MASTER("Master Tier", 2250, "FIDE Master positional depth"),
    GRANDMASTER_IM("Grandmaster IM", 2550, "International Master tactical perfection")
}

data class OnlineChallenger(
    val username: String,
    val country: String,
    val flag: String,
    val rating: Int,
    val title: String = ""
)

data class StrategyLesson(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val keyPrinciple: String,
    val exampleFen: String,
    val keyMove: String
)

data class UserSession(
    val username: String = "GM Player",
    val email: String = "analynlandingin77@gmail.com",
    val rating: Int = 2150,
    val country: String = "Philippines",
    val countryFlag: String = "🇵🇭",
    val isLoggedIn: Boolean = true,
    val totalWins: Int = 42,
    val totalLosses: Int = 8
)
