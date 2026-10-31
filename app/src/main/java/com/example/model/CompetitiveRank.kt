package com.example.model

import kotlin.math.pow
import kotlin.math.roundToInt

enum class CompetitiveRank(
    val title: String,
    val minRating: Int,
    val maxRating: Int,
    val badge: String,
    val colorHex: Long
) {
    BEGINNER("Beginner", 0, 1199, "🥉", 0xFF94A3B8),
    INTERMEDIATE("Intermediate", 1200, 1499, "🥈", 0xFF38BDF8),
    ADVANCED("Advanced", 1500, 1799, "🥇", 0xFF22C55E),
    EXPERT("Expert", 1800, 2099, "💎", 0xFFA855F7),
    MASTER("Master", 2100, 2399, "🏆", 0xFFF59E0B),
    GRANDMASTER("Grandmaster", 2400, 4000, "👑", 0xFFEF4444);

    companion object {
        /**
         * Strictly calculates rank from actual rating points.
         * A player CANNOT be promoted to Grandmaster merely by winning a fixed number of games;
         * they MUST achieve 2400+ rating points.
         */
        fun fromRating(rating: Int): CompetitiveRank {
            return when {
                rating >= 2400 -> GRANDMASTER
                rating >= 2100 -> MASTER
                rating >= 1800 -> EXPERT
                rating >= 1500 -> ADVANCED
                rating >= 1200 -> INTERMEDIATE
                else -> BEGINNER
            }
        }
    }
}

object EloCalculator {
    const val DEFAULT_K_FACTOR = 32

    /**
     * Standard Elo rating formula:
     * E = 1 / (1 + 10^((R_opp - R_player) / 400))
     * Delta = K * (ActualScore - ExpectedScore)
     *
     * @param playerRating The player's current Elo
     * @param opponentRating The opponent's current Elo
     * @param outcome 1.0 for win, 0.0 for loss, 0.5 for draw
     * @param kFactor Default is 32 (FIDE standard for non-veteran competitive tiers)
     * @return Positive delta for win, negative delta for loss
     */
    fun calculateDelta(
        playerRating: Int,
        opponentRating: Int,
        outcome: Double,
        kFactor: Int = DEFAULT_K_FACTOR
    ): Int {
        val exponent = (opponentRating - playerRating) / 400.0
        val expectedScore = 1.0 / (1.0 + 10.0.pow(exponent))
        val rawDelta = (kFactor * (outcome - expectedScore)).roundToInt()

        return when {
            outcome == 1.0 -> rawDelta.coerceAtLeast(1)
            outcome == 0.0 -> rawDelta.coerceAtMost(-1)
            else -> rawDelta
        }
    }
}

data class MatchHistoryItem(
    val id: String,
    val opponentName: String,
    val opponentRating: Int,
    val opponentFlag: String = "🌐",
    val result: String, // "VICTORY", "DEFEAT", "DRAW"
    val ratingDelta: Int,
    val newRating: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val moveCount: Int = 1
)

data class LeaderboardPlayer(
    val rank: Int,
    val username: String,
    val country: String,
    val flag: String,
    val rating: Int,
    val competitiveRank: CompetitiveRank,
    val title: String = "",
    val wins: Int = 0,
    val losses: Int = 0
)
