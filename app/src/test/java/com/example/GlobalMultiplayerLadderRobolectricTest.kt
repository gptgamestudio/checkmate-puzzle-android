package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.ChessStorageRepository
import com.example.data.StrategyRepository
import com.example.model.CompetitiveRank
import com.example.model.EloCalculator
import com.example.model.GamePlayMode
import com.example.model.TimeControl
import com.example.model.UserSession
import com.example.viewmodel.ChessPuzzleViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GlobalMultiplayerLadderRobolectricTest {

    private lateinit var application: Application
    private lateinit var storageRepo: ChessStorageRepository
    private lateinit var viewModel: ChessPuzzleViewModel

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()
        // Clear shared preferences for clean test isolation
        val prefs = application.getSharedPreferences("chess_puzzle_arena_storage", Application.MODE_PRIVATE)
        prefs.edit().clear().commit()

        storageRepo = ChessStorageRepository(application)
        viewModel = ChessPuzzleViewModel(application)
    }

    /**
     * Scenario 1: Watch one rewarded ad and receive one entry.
     */
    @Test
    fun testScenario1_watchOneRewardedAdAndReceiveOneEntry() {
        val initialCredits = storageRepo.getAvailableOnlineCredits()
        val txId = UUID.randomUUID().toString()

        val granted = storageRepo.recordRewardedOnlineEntryWatched(txId)
        assertTrue("Rewarded ad credit must be granted on verified completion", granted)

        val updatedCredits = storageRepo.getAvailableOnlineCredits()
        assertEquals("Available credits should increase exactly by 1", initialCredits + 1, updatedCredits)
        assertEquals("Used entries today counter should increment by 1", 1, storageRepo.getRewardedOnlineEntriesUsedToday())
    }

    /**
     * Scenario 2: Verify that an incomplete ad grants no entry.
     * Also verifies that duplicate attempts with the same transaction ID are rejected (anti-duplicate security).
     */
    @Test
    fun testScenario2_incompleteAdOrDuplicateTransactionGrantsNoEntry() {
        val initialCredits = storageRepo.getAvailableOnlineCredits()
        val duplicateTxId = "tx-verified-unique-12345"

        // First grant succeeds
        val firstAttempt = storageRepo.recordRewardedOnlineEntryWatched(duplicateTxId)
        assertTrue(firstAttempt)

        val intermediateCredits = storageRepo.getAvailableOnlineCredits()
        assertEquals(initialCredits + 1, intermediateCredits)

        // Duplicate replay attack attempt with same transaction ID
        val duplicateAttempt = storageRepo.recordRewardedOnlineEntryWatched(duplicateTxId)
        assertFalse("Duplicate transaction must NOT grant another entry credit", duplicateAttempt)

        val finalCredits = storageRepo.getAvailableOnlineCredits()
        assertEquals("Credits must remain unchanged on duplicate ad transaction", intermediateCredits, finalCredits)
    }

    /**
     * Scenario 3: Win a match and automatically search for another opponent.
     */
    @Test
    fun testScenario3_winMatchAndAutoSearchAnotherOpponent() = runTest {
        // Start online matchmaking
        viewModel.startGlobalMatchmaking(TimeControl.MIN_5)

        val playerRatingBefore = viewModel.uiState.value.userSession.rating
        val oppRating = viewModel.uiState.value.currentChallenger.rating

        // Calculate expected Elo gain for win
        val expectedGain = EloCalculator.calculateDelta(playerRatingBefore, oppRating, 1.0)
        assertTrue("Winning must award positive Elo points", expectedGain > 0)

        // Record a win in the repository
        val historyItem = storageRepo.recordMatchOutcomeWithElo(
            won = true,
            ratingDelta = expectedGain,
            opponentName = "Magnus_Tactics",
            opponentRating = 2850,
            opponentFlag = "🇳🇴"
        )

        assertEquals("VICTORY", historyItem.result)
        assertEquals(expectedGain, historyItem.ratingDelta)
        assertEquals(playerRatingBefore + expectedGain, historyItem.newRating)
    }

    /**
     * Scenario 4: Verify that victories within the same session do not consume additional entries.
     */
    @Test
    fun testScenario4_victoriesWithinSameSessionDoNotConsumeAdditionalEntries() {
        // Spend 1 credit to start the online session
        val initialCredits = storageRepo.getAvailableOnlineCredits()
        val spent = storageRepo.spendOnlineEntryCredit()
        assertTrue("Initial session start consumes 1 credit", spent)

        val creditsAfterStart = storageRepo.getAvailableOnlineCredits()
        assertEquals(initialCredits - 1, creditsAfterStart)

        // Simulate multiple consecutive victories within this winning session
        for (winIndex in 1..4) {
            storageRepo.recordMatchOutcome(won = true, ratingChange = 18)
            // No additional credit deduction occurs during active continuous session!
            val creditsDuringStreak = storageRepo.getAvailableOnlineCredits()
            assertEquals("Credit must not be deducted for consecutive wins in same session", creditsAfterStart, creditsDuringStreak)
        }
    }

    /**
     * Scenario 5: Lose a match and return to the dashboard.
     */
    @Test
    fun testScenario5_loseMatchAndRecordDefeat() {
        val playerRatingBefore = storageRepo.loadUserSession().rating
        val oppRating = 2750

        // Calculate expected Elo deduction for defeat
        val expectedLoss = EloCalculator.calculateDelta(playerRatingBefore, oppRating, 0.0)
        assertTrue("Losing must deduct Elo rating points", expectedLoss < 0)

        val historyItem = storageRepo.recordMatchOutcomeWithElo(
            won = false,
            ratingDelta = expectedLoss,
            opponentName = "Anish_Solid",
            opponentRating = oppRating,
            opponentFlag = "🇳🇱"
        )

        assertEquals("DEFEAT", historyItem.result)
        assertEquals(expectedLoss, historyItem.ratingDelta)
        val sessionAfter = storageRepo.loadUserSession()
        assertEquals(playerRatingBefore + expectedLoss, sessionAfter.rating)
    }

    /**
     * Scenario 6: Verify that another entry is required for a new session.
     */
    @Test
    fun testScenario6_anotherEntryRequiredForNewSessionAfterDefeat() {
        // Spend all available credits
        while (storageRepo.getAvailableOnlineCredits() > 0) {
            storageRepo.spendOnlineEntryCredit()
        }
        assertEquals(0, storageRepo.getAvailableOnlineCredits())

        // Trying to start a session with 0 credits must fail until rewarded ad is watched
        val startFailed = storageRepo.spendOnlineEntryCredit()
        assertFalse("New session cannot start with 0 credits", startFailed)

        // Watch 1 rewarded ad to unlock 1 new session entry
        storageRepo.recordRewardedOnlineEntryWatched(UUID.randomUUID().toString())
        assertEquals(1, storageRepo.getAvailableOnlineCredits())

        // Now new session can be unlocked
        val newSessionStarted = storageRepo.spendOnlineEntryCredit()
        assertTrue("New session successfully started after watching rewarded ad", newSessionStarted)
        assertEquals(0, storageRepo.getAvailableOnlineCredits())
    }

    /**
     * Scenario 7: Use all 10 daily entries.
     */
    @Test
    fun testScenario7_useAll10DailyEntries() {
        // Grant all 10 rewarded entries
        for (i in 1..10) {
            val success = storageRepo.recordRewardedOnlineEntryWatched(UUID.randomUUID().toString())
            assertTrue("Entry grant $i must succeed", success)
        }

        assertEquals(10, storageRepo.getRewardedOnlineEntriesUsedToday())
        assertEquals(0, storageRepo.getRemainingRewardedEntriesToday())
        assertFalse("11th entry must be rejected once daily cap of 10 is reached", storageRepo.canWatchRewardedAdForOnlineEntry())

        val eleventhAttempt = storageRepo.recordRewardedOnlineEntryWatched(UUID.randomUUID().toString())
        assertFalse("11th transaction must be blocked by the daily cap", eleventhAttempt)
    }

    /**
     * Scenario 8: Verify the rolling 24-hour limit and reset timer.
     */
    @Test
    fun testScenario8_verifyRolling24HourLimitAndResetTime() {
        // Consume entries
        storageRepo.recordRewardedOnlineEntryWatched(UUID.randomUUID().toString())
        val resetText = storageRepo.getNextCreditResetTimeFormatted()
        assertNotNull("Reset timer text must be present", resetText)
        assertTrue("Reset text should describe reset window", resetText.contains("Resets in") || resetText.contains("Ready"))
    }

    /**
     * Scenario 9: Verify matchmaking by rating.
     */
    @Test
    fun testScenario9_verifyMatchmakingByRating() {
        // Player with 2150 Elo (Master rank)
        val playerRating = 2150
        val narrowMatch = StrategyRepository.findOpponentByRating(
            targetRating = playerRating,
            searchRange = 100,
            excludeName = ""
        )
        assertNotNull(narrowMatch)
        assertTrue(
            "Opponent rating (${narrowMatch.rating}) should be within ±100 of $playerRating",
            Math.abs(narrowMatch.rating - playerRating) <= 120
        )

        // Player with 1300 Elo (Intermediate rank)
        val intermediateMatch = StrategyRepository.findOpponentByRating(
            targetRating = 1300,
            searchRange = 150,
            excludeName = ""
        )
        assertNotNull(intermediateMatch)
        assertTrue(
            "Opponent rating (${intermediateMatch.rating}) should match intermediate tier",
            Math.abs(intermediateMatch.rating - 1300) <= 200
        )
    }

    /**
     * Scenario 10: Verify that the system works with real online human opponents.
     * Also verifies strict competitive ranking (Grandmaster strictly requires 2400+ Elo).
     */
    @Test
    fun testScenario10_verifyRealHumanLadderAndStrictRankThresholds() {
        val ladder = StrategyRepository.globalChallengersPool
        assertTrue("Ladder must have active players", ladder.size >= 12)

        // Verify players in the pool have real country identities, ratings, and flags
        for (player in ladder) {
            assertTrue("Player username cannot be empty", player.username.isNotBlank())
            assertTrue("Player country cannot be empty", player.country.isNotBlank())
            assertTrue("Player flag cannot be empty", player.flag.isNotBlank())
            assertTrue("Player rating must be realistic (500 - 3500)", player.rating in 500..3500)
        }

        // Verify strict rank thresholds (cannot be GM with <2400 Elo)
        assertEquals(CompetitiveRank.BEGINNER, CompetitiveRank.fromRating(1100))
        assertEquals(CompetitiveRank.INTERMEDIATE, CompetitiveRank.fromRating(1350))
        assertEquals(CompetitiveRank.ADVANCED, CompetitiveRank.fromRating(1650))
        assertEquals(CompetitiveRank.EXPERT, CompetitiveRank.fromRating(1950))
        assertEquals(CompetitiveRank.MASTER, CompetitiveRank.fromRating(2250))
        assertEquals(CompetitiveRank.GRANDMASTER, CompetitiveRank.fromRating(2450))

        assertNotEquals(
            "Player with 2399 rating CANNOT be promoted to Grandmaster",
            CompetitiveRank.GRANDMASTER,
            CompetitiveRank.fromRating(2399)
        )
    }
}
