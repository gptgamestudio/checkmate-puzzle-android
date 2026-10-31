package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.UserSession

class ChessStorageRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("chess_puzzle_arena_storage", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SOLVED_PUZZLE_IDS = "solved_puzzle_ids"
        private const val KEY_CURRENT_PUZZLE_INDEX = "current_puzzle_index"
        private const val KEY_USER_LOGGED_IN = "user_logged_in"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_USERNAME = "user_username"
        private const val KEY_USER_RATING = "user_rating"
        private const val KEY_USER_COUNTRY = "user_country"
        private const val KEY_USER_FLAG = "user_country_flag"
        private const val KEY_COMPLETED_STRATEGIES = "completed_strategies"
        private const val KEY_TOTAL_WINS = "total_wins"
        private const val KEY_TOTAL_LOSSES = "total_losses"
    }

    // Auto-save & load solved puzzles
    fun saveSolvedPuzzle(puzzleId: String) {
        val currentSet = loadSolvedPuzzles().toMutableSet()
        currentSet.add(puzzleId)
        prefs.edit().putStringSet(KEY_SOLVED_PUZZLE_IDS, currentSet).apply()
    }

    fun loadSolvedPuzzles(): Set<String> {
        return prefs.getStringSet(KEY_SOLVED_PUZZLE_IDS, emptySet()) ?: emptySet()
    }

    fun saveCurrentPuzzleIndex(index: Int) {
        prefs.edit().putInt(KEY_CURRENT_PUZZLE_INDEX, index).apply()
    }

    fun loadCurrentPuzzleIndex(): Int {
        return prefs.getInt(KEY_CURRENT_PUZZLE_INDEX, 0)
    }

    // Auto-save & load User Session
    fun saveUserSession(session: UserSession) {
        prefs.edit()
            .putBoolean(KEY_USER_LOGGED_IN, session.isLoggedIn)
            .putString(KEY_USER_EMAIL, session.email)
            .putString(KEY_USER_USERNAME, session.username)
            .putInt(KEY_USER_RATING, session.rating)
            .putString(KEY_USER_COUNTRY, session.country)
            .putString(KEY_USER_FLAG, session.countryFlag)
            .putInt(KEY_TOTAL_WINS, session.totalWins)
            .putInt(KEY_TOTAL_LOSSES, session.totalLosses)
            .apply()
    }

    fun loadUserSession(): UserSession {
        val isLoggedIn = prefs.getBoolean(KEY_USER_LOGGED_IN, false)
        val email = prefs.getString(KEY_USER_EMAIL, "analynlandingin77@gmail.com") ?: "analynlandingin77@gmail.com"
        val username = prefs.getString(KEY_USER_USERNAME, "GM Player") ?: "GM Player"
        val rating = prefs.getInt(KEY_USER_RATING, 2150)
        val country = prefs.getString(KEY_USER_COUNTRY, "Philippines") ?: "Philippines"
        val flag = prefs.getString(KEY_USER_FLAG, "🇵🇭") ?: "🇵🇭"
        val wins = prefs.getInt(KEY_TOTAL_WINS, 42)
        val losses = prefs.getInt(KEY_TOTAL_LOSSES, 8)

        return UserSession(
            username = username,
            email = email,
            rating = rating,
            country = country,
            countryFlag = flag,
            isLoggedIn = isLoggedIn,
            totalWins = wins,
            totalLosses = losses
        )
    }

    fun recordMatchOutcome(won: Boolean, ratingChange: Int = 15) {
        val session = loadUserSession()
        val newWins = if (won) session.totalWins + 1 else session.totalWins
        val newLosses = if (!won) session.totalLosses + 1 else session.totalLosses
        val newRating = (session.rating + if (won) ratingChange else -ratingChange).coerceAtLeast(100)
        saveUserSession(session.copy(rating = newRating, totalWins = newWins, totalLosses = newLosses))
    }

    // Strategy lessons auto-save
    fun saveCompletedStrategy(strategyId: String) {
        val currentSet = loadCompletedStrategies().toMutableSet()
        currentSet.add(strategyId)
        prefs.edit().putStringSet(KEY_COMPLETED_STRATEGIES, currentSet).apply()
    }

    fun loadCompletedStrategies(): Set<String> {
        return prefs.getStringSet(KEY_COMPLETED_STRATEGIES, emptySet()) ?: emptySet()
    }
}
