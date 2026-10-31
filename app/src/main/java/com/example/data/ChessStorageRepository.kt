package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.MatchHistoryItem
import com.example.model.UserSession
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

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

        // Rewarded Ad Hint Tracker (Max 10 per rolling 24-hour period)
        private const val KEY_REWARDED_AD_HINTS_COUNT = "rewarded_ad_hints_count"
        private const val KEY_REWARDED_AD_LAST_TIMESTAMP = "rewarded_ad_last_timestamp"
        const val MAX_REWARDED_HINTS_PER_24H = 10

        // Global Online Rewarded Entry System (Max 10 per rolling 24-hour period)
        private const val KEY_ONLINE_AVAILABLE_CREDITS = "online_available_credits"
        private const val KEY_ONLINE_ENTRIES_COUNT_24H = "online_entries_count_24h"
        private const val KEY_ONLINE_FIRST_ENTRY_TIMESTAMP = "online_first_entry_timestamp"
        private const val KEY_CLAIMED_AD_TRANSACTIONS = "claimed_ad_transactions"
        private const val KEY_MATCH_HISTORY_JSON = "match_history_json"
        const val MAX_REWARDED_ONLINE_ENTRIES_PER_24H = 10
        private const val ROLLING_24H_MILLIS = 24 * 60 * 60 * 1000L
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

    fun recordMatchOutcomeWithElo(
        won: Boolean,
        ratingDelta: Int,
        opponentName: String,
        opponentRating: Int,
        opponentFlag: String = "🌐"
    ): MatchHistoryItem {
        val session = loadUserSession()
        val newWins = if (won) session.totalWins + 1 else session.totalWins
        val newLosses = if (!won) session.totalLosses + 1 else session.totalLosses
        val newRating = (session.rating + ratingDelta).coerceAtLeast(100)
        saveUserSession(session.copy(rating = newRating, totalWins = newWins, totalLosses = newLosses))

        val item = MatchHistoryItem(
            id = UUID.randomUUID().toString(),
            opponentName = opponentName,
            opponentRating = opponentRating,
            opponentFlag = opponentFlag,
            result = if (won) "VICTORY" else "DEFEAT",
            ratingDelta = ratingDelta,
            newRating = newRating,
            timestamp = System.currentTimeMillis()
        )
        saveMatchHistoryItem(item)
        return item
    }

    // Match History Persistence
    fun saveMatchHistoryItem(item: MatchHistoryItem) {
        val history = loadMatchHistory().toMutableList()
        history.add(0, item) // Prepend newest
        val trimmed = history.take(30) // Keep latest 30 games

        val jsonArray = JSONArray()
        for (m in trimmed) {
            val obj = JSONObject().apply {
                put("id", m.id)
                put("opponentName", m.opponentName)
                put("opponentRating", m.opponentRating)
                put("opponentFlag", m.opponentFlag)
                put("result", m.result)
                put("ratingDelta", m.ratingDelta)
                put("newRating", m.newRating)
                put("timestamp", m.timestamp)
                put("moveCount", m.moveCount)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_MATCH_HISTORY_JSON, jsonArray.toString()).apply()
    }

    fun loadMatchHistory(): List<MatchHistoryItem> {
        val raw = prefs.getString(KEY_MATCH_HISTORY_JSON, null) ?: return defaultMatchHistory()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<MatchHistoryItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    MatchHistoryItem(
                        id = obj.getString("id"),
                        opponentName = obj.getString("opponentName"),
                        opponentRating = obj.getInt("opponentRating"),
                        opponentFlag = obj.optString("opponentFlag", "🌐"),
                        result = obj.getString("result"),
                        ratingDelta = obj.getInt("ratingDelta"),
                        newRating = obj.getInt("newRating"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        moveCount = obj.optInt("moveCount", 1)
                    )
                )
            }
            list
        } catch (_: Exception) {
            defaultMatchHistory()
        }
    }

    private fun defaultMatchHistory(): List<MatchHistoryItem> {
        val now = System.currentTimeMillis()
        return listOf(
            MatchHistoryItem(
                id = "init_1",
                opponentName = "Magnus_Tactics",
                opponentRating = 2850,
                opponentFlag = "🇳🇴",
                result = "VICTORY",
                ratingDelta = 22,
                newRating = 2150,
                timestamp = now - 3600000L
            ),
            MatchHistoryItem(
                id = "init_2",
                opponentName = "Hikaru_Speed",
                opponentRating = 2820,
                opponentFlag = "🇺🇸",
                result = "VICTORY",
                ratingDelta = 20,
                newRating = 2128,
                timestamp = now - 7200000L
            ),
            MatchHistoryItem(
                id = "init_3",
                opponentName = "Wesley_So_Pinoy",
                opponentRating = 2780,
                opponentFlag = "🇵🇭",
                result = "DEFEAT",
                ratingDelta = -14,
                newRating = 2108,
                timestamp = now - 14400000L
            )
        )
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

    // Rewarded Ad Hint Tracker (Max 10 per rolling 24-hour period)
    fun getRewardedHintsUsedToday(): Int {
        val lastTimestamp = prefs.getLong(KEY_REWARDED_AD_LAST_TIMESTAMP, 0L)
        val now = System.currentTimeMillis()
        if (now - lastTimestamp >= ROLLING_24H_MILLIS) {
            prefs.edit()
                .putInt(KEY_REWARDED_AD_HINTS_COUNT, 0)
                .putLong(KEY_REWARDED_AD_LAST_TIMESTAMP, now)
                .apply()
            return 0
        }
        return prefs.getInt(KEY_REWARDED_AD_HINTS_COUNT, 0)
    }

    fun canWatchRewardedAdForHint(): Boolean {
        return getRewardedHintsUsedToday() < MAX_REWARDED_HINTS_PER_24H
    }

    fun recordRewardedHintWatched(): Boolean {
        if (!canWatchRewardedAdForHint()) return false
        val current = getRewardedHintsUsedToday()
        val now = System.currentTimeMillis()
        val existingFirstTimestamp = prefs.getLong(KEY_REWARDED_AD_LAST_TIMESTAMP, 0L)
        val baseTimestamp = if (current == 0 || existingFirstTimestamp == 0L) now else existingFirstTimestamp
        prefs.edit()
            .putInt(KEY_REWARDED_AD_HINTS_COUNT, current + 1)
            .putLong(KEY_REWARDED_AD_LAST_TIMESTAMP, baseTimestamp)
            .apply()
        return true
    }

    // =========================================================================
    // GLOBAL ONLINE REWARDED GAME ENTRY SYSTEM (Section B & E)
    // =========================================================================

    /**
     * Checks and resets the rolling 24-hour window for rewarded online entries.
     */
    fun checkAndResetRollingOnlineWindow(): Int {
        val firstTimestamp = prefs.getLong(KEY_ONLINE_FIRST_ENTRY_TIMESTAMP, 0L)
        val now = System.currentTimeMillis()
        if (firstTimestamp > 0L && now - firstTimestamp >= ROLLING_24H_MILLIS) {
            // Rolling 24 hours elapsed: Reset daily used count and reset timestamp
            prefs.edit()
                .putInt(KEY_ONLINE_ENTRIES_COUNT_24H, 0)
                .putLong(KEY_ONLINE_FIRST_ENTRY_TIMESTAMP, 0L)
                .apply()
            return 0
        }
        return prefs.getInt(KEY_ONLINE_ENTRIES_COUNT_24H, 0)
    }

    fun getRewardedOnlineEntriesUsedToday(): Int {
        return checkAndResetRollingOnlineWindow()
    }

    fun getRemainingRewardedEntriesToday(): Int {
        val used = getRewardedOnlineEntriesUsedToday()
        return (MAX_REWARDED_ONLINE_ENTRIES_PER_24H - used).coerceAtLeast(0)
    }

    fun canWatchRewardedAdForOnlineEntry(): Boolean {
        return getRemainingRewardedEntriesToday() > 0
    }

    /**
     * Available entry credits the player currently has stored to start an online session.
     * Default is 2 starter credits on fresh install.
     */
    fun getAvailableOnlineCredits(): Int {
        if (!prefs.contains(KEY_ONLINE_AVAILABLE_CREDITS)) {
            prefs.edit().putInt(KEY_ONLINE_AVAILABLE_CREDITS, 2).apply()
            return 2
        }
        return prefs.getInt(KEY_ONLINE_AVAILABLE_CREDITS, 0)
    }

    /**
     * Grants 1 entry credit upon verified completion of a rewarded ad.
     * Anti-duplicate validation checks transactionId to prevent replay attacks.
     */
    fun recordRewardedOnlineEntryWatched(transactionId: String): Boolean {
        val claimedSet = prefs.getStringSet(KEY_CLAIMED_AD_TRANSACTIONS, emptySet()) ?: emptySet()
        if (claimedSet.contains(transactionId)) {
            // Prevent duplicate grant from same transaction
            return false
        }

        if (!canWatchRewardedAdForOnlineEntry()) {
            return false
        }

        val usedCount = getRewardedOnlineEntriesUsedToday()
        val now = System.currentTimeMillis()
        val firstTimestamp = prefs.getLong(KEY_ONLINE_FIRST_ENTRY_TIMESTAMP, 0L)
        val newFirstTimestamp = if (usedCount == 0 || firstTimestamp == 0L) now else firstTimestamp

        val currentCredits = getAvailableOnlineCredits()
        val newClaimedSet = claimedSet.toMutableSet().apply { add(transactionId) }

        prefs.edit()
            .putInt(KEY_ONLINE_ENTRIES_COUNT_24H, usedCount + 1)
            .putLong(KEY_ONLINE_FIRST_ENTRY_TIMESTAMP, newFirstTimestamp)
            .putInt(KEY_ONLINE_AVAILABLE_CREDITS, currentCredits + 1)
            .putStringSet(KEY_CLAIMED_AD_TRANSACTIONS, newClaimedSet)
            .apply()

        return true
    }

    /**
     * Deducts 1 entry credit to begin a new online session.
     * Returns true if credit was successfully consumed.
     */
    fun spendOnlineEntryCredit(): Boolean {
        val currentCredits = getAvailableOnlineCredits()
        if (currentCredits <= 0) return false
        prefs.edit().putInt(KEY_ONLINE_AVAILABLE_CREDITS, currentCredits - 1).apply()
        return true
    }

    /**
     * Human-readable next reset time for rolling 24-hour limit.
     */
    fun getNextCreditResetTimeFormatted(): String {
        val firstTimestamp = prefs.getLong(KEY_ONLINE_FIRST_ENTRY_TIMESTAMP, 0L)
        if (firstTimestamp <= 0L || getRewardedOnlineEntriesUsedToday() == 0) {
            return "All 10 Entries Ready"
        }
        val now = System.currentTimeMillis()
        val resetAt = firstTimestamp + ROLLING_24H_MILLIS
        val diffMillis = resetAt - now
        if (diffMillis <= 0) return "Ready to Reset"

        val hours = diffMillis / (1000 * 60 * 60)
        val minutes = (diffMillis % (1000 * 60 * 60)) / (1000 * 60)
        return "Resets in ${hours}h ${minutes}m"
    }
}
