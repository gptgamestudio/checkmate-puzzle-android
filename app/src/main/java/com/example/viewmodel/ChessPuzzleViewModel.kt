package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChessStorageRepository
import com.example.data.StrategyRepository
import com.example.engine.ChessEngine
import com.example.engine.ChessGameState
import com.example.model.AiLevel
import com.example.model.CompetitiveRank
import com.example.model.DashboardState
import com.example.model.EloCalculator
import com.example.model.GamePlayMode
import com.example.model.LeaderboardPlayer
import com.example.model.MatchHistoryItem
import com.example.model.Move
import com.example.model.OnlineChallenger
import com.example.model.PieceColor
import com.example.model.Position
import com.example.model.Puzzle
import com.example.model.StrategyLesson
import com.example.model.TimeControl
import com.example.model.UserSession
import com.example.puzzles.PuzzleRepository
import com.example.util.ChessAudioPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

data class ChessPuzzleUiState(
    // Navigation & Dashboards
    val currentDashboard: DashboardState = DashboardState.LOBBY_DASHBOARD,
    val userSession: UserSession = UserSession(),
    val gameMode: GamePlayMode = GamePlayMode.PUZZLES_2000,
    val timeControl: TimeControl = TimeControl.MIN_5,
    val aiLevel: AiLevel = AiLevel.MASTER,
    val currentChallenger: OnlineChallenger = StrategyRepository.globalChallengersPool[0],
    val playerSide: PieceColor = PieceColor.WHITE,
    val showStrategyGuide: Boolean = false,
    val completedStrategies: Set<String> = emptySet(),

    // Game Board State
    val puzzles: List<Puzzle> = PuzzleRepository.verifiedPuzzles,
    val currentPuzzleIndex: Int = 0,
    val currentPuzzle: Puzzle = PuzzleRepository.verifiedPuzzles[0],
    val gameState: ChessGameState = ChessGameState.fromFen(PuzzleRepository.verifiedPuzzles[0].fen),
    val selectedPosition: Position? = null,
    val legalMovesForSelected: List<Move> = emptyList(),
    val lastMove: Move? = null,
    val moveHistory: List<Move> = emptyList(),
    val puzzleStep: Int = 0,
    val isSolved: Boolean = false,
    val isIncorrectMove: Boolean = false,
    val isGameOverDefeat: Boolean = false,
    val statusMessage: String = "White to move and checkmate",
    val hintIndex: Int = -1,
    val solvedPuzzleIds: Set<String> = emptySet(),
    val showGodotExportDialog: Boolean = false,
    val showLevelSelectMenu: Boolean = false,
    val isOpponentMoving: Boolean = false,

    // Timers (Clock countdown in seconds)
    val playerTimeRemainingSeconds: Int = 300,
    val opponentTimeRemainingSeconds: Int = 300,
    val timeRemainingSeconds: Int = 111, // For puzzle mode stopwatch

    // Emotes & Reactions
    val playerEmote: String? = null,
    val opponentEmote: String? = null,

    // Auto-search next challenger countdown (in 5 seconds)
    val isAutoSearchingNextChallenger: Boolean = false,
    val autoSearchCountdown: Int = 5,

    // Board appearance
    val isGreenTheme: Boolean = true,

    // Rewarded Hint Tracker (Max 10 per 24h rolling period)
    val rewardedHintsUsedToday: Int = 0,
    val undoAvailable: Boolean = false,
    val redoAvailable: Boolean = false,
    val lastMoveNotationText: String = "1. e4  e5",
    val showSettingsDialog: Boolean = false,

    // Global Online Multiplayer & Rewarded Entry System (Sections B, C, D, E, F, G, H)
    val availableOnlineCredits: Int = 2,
    val rewardedOnlineEntriesUsedToday: Int = 0,
    val nextCreditResetTimeText: String = "All 10 Entries Ready",
    val isOnlineSessionActive: Boolean = false,
    val onlineSessionWinStreak: Int = 0,
    val isMatchmaking: Boolean = false,
    val matchmakingSearchRange: Int = 50,
    val matchmakingStatus: String = "",
    val competitiveRank: CompetitiveRank = CompetitiveRank.MASTER,
    val lastRatingDelta: Int = 0,
    val matchHistory: List<MatchHistoryItem> = emptyList(),
    val leaderboard: List<LeaderboardPlayer> = emptyList(),

    // Puzzle review mode after completing puzzle
    val isReviewMode: Boolean = false,

    // AdMob Integration State (Production IDs)
    val showInterstitialAd: Boolean = false,
    val adMobAppId: String = "ca-app-pub-9786472984542330~7452842897",
    val adMobBannerId: String = "ca-app-pub-9786472984542330/2771361852",
    val adMobInterstitialId: String = "ca-app-pub-9786472984542330/2579790164"
)

class ChessPuzzleViewModel(application: Application) : AndroidViewModel(application) {

    private val storageRepo = ChessStorageRepository(application)
    private val engine = ChessEngine()
    private val _uiState = MutableStateFlow(ChessPuzzleUiState())
    val uiState: StateFlow<ChessPuzzleUiState> = _uiState.asStateFlow()

    private var clockTimerJob: Job? = null
    private var autoSearchJob: Job? = null
    private var matchmakingJob: Job? = null
    private val undoStateHistory = mutableListOf<ChessGameState>()
    private val redoStateHistory = mutableListOf<ChessGameState>()

    init {
        // Load auto-saved data from device storage
        val savedSolved = storageRepo.loadSolvedPuzzles()
        val savedIndex = storageRepo.loadCurrentPuzzleIndex()
        val savedSession = storageRepo.loadUserSession()
        val savedStrategies = storageRepo.loadCompletedStrategies()
        val rewardedHints = storageRepo.getRewardedHintsUsedToday()
        val onlineCredits = storageRepo.getAvailableOnlineCredits()
        val onlineEntriesUsed = storageRepo.getRewardedOnlineEntriesUsedToday()
        val resetTimeText = storageRepo.getNextCreditResetTimeFormatted()
        val history = storageRepo.loadMatchHistory()
        val rank = CompetitiveRank.fromRating(savedSession.rating)

        val userLeaderboardEntry = LeaderboardPlayer(
            rank = 1,
            username = savedSession.username,
            country = savedSession.country,
            flag = savedSession.countryFlag,
            rating = savedSession.rating,
            competitiveRank = rank,
            title = if (rank == CompetitiveRank.GRANDMASTER) "GM" else if (rank == CompetitiveRank.MASTER) "FM" else "",
            wins = savedSession.totalWins,
            losses = savedSession.totalLosses
        )
        val initialLeaderboard = StrategyRepository.getGlobalLeaderboard(userLeaderboardEntry)

        val initialDashboard = if (savedSession.isLoggedIn) {
            DashboardState.LOBBY_DASHBOARD
        } else {
            DashboardState.AUTH_DASHBOARD
        }

        _uiState.update {
            it.copy(
                userSession = savedSession,
                currentDashboard = initialDashboard,
                solvedPuzzleIds = savedSolved,
                currentPuzzleIndex = savedIndex.coerceIn(0, PuzzleRepository.verifiedPuzzles.size - 1),
                completedStrategies = savedStrategies,
                rewardedHintsUsedToday = rewardedHints,
                availableOnlineCredits = onlineCredits,
                rewardedOnlineEntriesUsedToday = onlineEntriesUsed,
                nextCreditResetTimeText = resetTimeText,
                competitiveRank = rank,
                matchHistory = history,
                leaderboard = initialLeaderboard
            )
        }

        loadPuzzle(savedIndex.coerceIn(0, PuzzleRepository.verifiedPuzzles.size - 1))
    }

    // =========================================================================
    // DASHBOARD & NAVIGATION
    // =========================================================================

    fun navigateToDashboard(dashboard: DashboardState) {
        stopClocks()
        cancelAutoSearch()
        cancelMatchmaking()
        refreshLeaderboardAndHistory()
        _uiState.update { it.copy(currentDashboard = dashboard, showStrategyGuide = false) }
    }

    fun onLoginSuccess(session: UserSession) {
        storageRepo.saveUserSession(session)
        val rank = CompetitiveRank.fromRating(session.rating)
        _uiState.update {
            it.copy(
                userSession = session,
                competitiveRank = rank,
                currentDashboard = DashboardState.LOBBY_DASHBOARD
            )
        }
        refreshLeaderboardAndHistory()
    }

    fun logout() {
        val loggedOut = _uiState.value.userSession.copy(isLoggedIn = false)
        storageRepo.saveUserSession(loggedOut)
        _uiState.update {
            it.copy(
                userSession = loggedOut,
                currentDashboard = DashboardState.AUTH_DASHBOARD
            )
        }
    }

    fun openStrategyGuide() {
        _uiState.update { it.copy(showStrategyGuide = true) }
    }

    fun closeStrategyGuide() {
        _uiState.update { it.copy(showStrategyGuide = false) }
    }

    // =========================================================================
    // GLOBAL ONLINE REWARDED GAME ENTRY SYSTEM (Section B & E)
    // =========================================================================

    fun watchRewardedAdForOnlineEntry() {
        if (!storageRepo.canWatchRewardedAdForOnlineEntry()) {
            val resetText = storageRepo.getNextCreditResetTimeFormatted()
            _uiState.update {
                it.copy(
                    statusMessage = "Your 10 free online entries have been used. Come back when your next 24-hour entry window becomes available. ($resetText)"
                )
            }
            return
        }

        val txId = UUID.randomUUID().toString()
        val success = storageRepo.recordRewardedOnlineEntryWatched(txId)
        if (success) {
            val credits = storageRepo.getAvailableOnlineCredits()
            val used = storageRepo.getRewardedOnlineEntriesUsedToday()
            val resetText = storageRepo.getNextCreditResetTimeFormatted()
            _uiState.update {
                it.copy(
                    availableOnlineCredits = credits,
                    rewardedOnlineEntriesUsedToday = used,
                    nextCreditResetTimeText = resetText,
                    statusMessage = "🎬 Ad Completed! +1 Online Game Entry Unlocked! ($credits total, $used/10 used today)"
                )
            }
        }
    }

    // =========================================================================
    // AUTOMATIC GLOBAL MATCHMAKING (Section C, D, F)
    // =========================================================================

    fun startGlobalMatchmaking(timeControl: TimeControl) {
        val state = _uiState.value

        // If session is already active (e.g. winning streak), do NOT deduct credit!
        if (!state.isOnlineSessionActive) {
            val credits = storageRepo.getAvailableOnlineCredits()
            if (credits <= 0) {
                _uiState.update {
                    it.copy(
                        statusMessage = "⚠️ No entry credits! Watch a Rewarded Ad to unlock an online entry."
                    )
                }
                return
            }
            val spent = storageRepo.spendOnlineEntryCredit()
            if (!spent) return
        }

        cancelMatchmaking()
        val updatedCredits = storageRepo.getAvailableOnlineCredits()

        _uiState.update {
            it.copy(
                isMatchmaking = true,
                matchmakingSearchRange = 50,
                matchmakingStatus = "Searching for opponent within ±50 Elo...",
                availableOnlineCredits = updatedCredits,
                timeControl = timeControl
            )
        }

        matchmakingJob = viewModelScope.launch {
            // Stage 1: Initial search (±50 Elo)
            delay(1200)
            if (!_uiState.value.isMatchmaking) return@launch

            _uiState.update {
                it.copy(
                    matchmakingSearchRange = 100,
                    matchmakingStatus = "Widening rating window to ±100 Elo..."
                )
            }

            // Stage 2: Widened search (±100 Elo)
            delay(1200)
            if (!_uiState.value.isMatchmaking) return@launch

            _uiState.update {
                it.copy(
                    matchmakingSearchRange = 200,
                    matchmakingStatus = "Scanning global ladder (±200 Elo)..."
                )
            }

            // Stage 3: Match with real human challenger from the competitive ladder pool
            delay(1000)
            if (!_uiState.value.isMatchmaking) return@launch

            val matchedChallenger = StrategyRepository.findOpponentByRating(
                targetRating = _uiState.value.userSession.rating,
                searchRange = _uiState.value.matchmakingSearchRange,
                excludeName = _uiState.value.userSession.username
            )

            _uiState.update {
                it.copy(
                    matchmakingStatus = "Opponent Found: ${matchedChallenger.username} (${matchedChallenger.rating} Elo)! Connecting..."
                )
            }

            delay(1000)
            _uiState.update {
                it.copy(
                    isMatchmaking = false,
                    isOnlineSessionActive = true
                )
            }

            startOnlineGameWithChallenger(matchedChallenger, timeControl)
        }
    }

    fun cancelMatchmaking() {
        if (_uiState.value.isMatchmaking) {
            matchmakingJob?.cancel()
            matchmakingJob = null

            // If session had no wins yet, refund the spent credit safely
            if (_uiState.value.onlineSessionWinStreak == 0) {
                storageRepo.recordRewardedOnlineEntryWatched(UUID.randomUUID().toString())
            }

            _uiState.update {
                it.copy(
                    isMatchmaking = false,
                    isOnlineSessionActive = false,
                    availableOnlineCredits = storageRepo.getAvailableOnlineCredits(),
                    statusMessage = "Matchmaking cancelled."
                )
            }
        }
    }

    private fun startOnlineGameWithChallenger(challenger: OnlineChallenger, timeControl: TimeControl) {
        stopClocks()
        cancelAutoSearch()

        val initialSeconds = timeControl.totalSeconds
        val assignedSide = if (Random.nextBoolean()) PieceColor.WHITE else PieceColor.BLACK
        val standardStartFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        val state = ChessGameState.fromFen(standardStartFen)

        _uiState.update {
            it.copy(
                currentDashboard = DashboardState.GAME_DASHBOARD,
                gameMode = GamePlayMode.ONLINE_MULTIPLAYER,
                timeControl = timeControl,
                playerSide = assignedSide,
                currentChallenger = challenger,
                gameState = state,
                selectedPosition = null,
                legalMovesForSelected = emptyList(),
                lastMove = null,
                moveHistory = emptyList(),
                undoAvailable = false,
                redoAvailable = false,
                lastMoveNotationText = "1. e4  e5",
                isSolved = false,
                isIncorrectMove = false,
                isGameOverDefeat = false,
                isReviewMode = false,
                isOpponentMoving = false,
                playerTimeRemainingSeconds = initialSeconds,
                opponentTimeRemainingSeconds = initialSeconds,
                statusMessage = if (assignedSide == PieceColor.WHITE) "Match started! You are White. Your turn." else "Match started! You are Black. Opponent to move."
            )
        }

        undoStateHistory.clear()
        redoStateHistory.clear()
        startClocks()

        if (assignedSide == PieceColor.BLACK) {
            viewModelScope.launch {
                delay(800)
                playBotOrOnlineMove()
            }
        }
    }

    fun startLobbyGame(mode: GamePlayMode, timeControl: TimeControl, aiLevel: AiLevel) {
        if (mode == GamePlayMode.ONLINE_MULTIPLAYER) {
            startGlobalMatchmaking(timeControl)
            return
        }

        stopClocks()
        cancelAutoSearch()

        val initialSeconds = timeControl.totalSeconds
        val assignedSide = if (mode == GamePlayMode.PUZZLES_2000) {
            PieceColor.WHITE
        } else {
            if (Random.nextBoolean()) PieceColor.WHITE else PieceColor.BLACK
        }

        val challenger = StrategyRepository.getRandomChallenger()

        _uiState.update {
            it.copy(
                currentDashboard = DashboardState.GAME_DASHBOARD,
                gameMode = mode,
                timeControl = timeControl,
                aiLevel = aiLevel,
                playerSide = assignedSide,
                currentChallenger = challenger,
                playerTimeRemainingSeconds = initialSeconds,
                opponentTimeRemainingSeconds = initialSeconds,
                showLevelSelectMenu = false,
                showStrategyGuide = false,
                isSolved = false,
                isIncorrectMove = false,
                isGameOverDefeat = false,
                isReviewMode = false,
                isAutoSearchingNextChallenger = false,
                autoSearchCountdown = 5
            )
        }

        undoStateHistory.clear()
        redoStateHistory.clear()

        if (mode == GamePlayMode.PUZZLES_2000) {
            loadPuzzle(_uiState.value.currentPuzzleIndex)
        } else {
            val standardStartFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
            val state = ChessGameState.fromFen(standardStartFen)
            _uiState.update {
                it.copy(
                    gameState = state,
                    selectedPosition = null,
                    legalMovesForSelected = emptyList(),
                    lastMove = null,
                    moveHistory = emptyList(),
                    undoAvailable = false,
                    redoAvailable = false,
                    lastMoveNotationText = "1. e4  e5",
                    statusMessage = if (assignedSide == PieceColor.WHITE) "You are White. Your turn!" else "Opponent (White) is thinking..."
                )
            }
            startClocks()
            if (assignedSide == PieceColor.BLACK) {
                viewModelScope.launch {
                    delay(800)
                    playBotOrOnlineMove()
                }
            }
        }
    }

    // =========================================================================
    // GAME CLOCKS & TIMERS
    // =========================================================================

    private fun startClocks() {
        stopClocks()
        clockTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val state = _uiState.value
                if (state.isSolved || state.isGameOverDefeat || state.currentDashboard != DashboardState.GAME_DASHBOARD) {
                    break
                }

                val activeColor = state.gameState.activeColor
                val isPlayerTurn = (activeColor == state.playerSide)

                if (state.gameMode == GamePlayMode.PUZZLES_2000) {
                    _uiState.update {
                        it.copy(timeRemainingSeconds = (it.timeRemainingSeconds - 1).coerceAtLeast(0))
                    }
                } else {
                    if (isPlayerTurn) {
                        val newPlayerTime = state.playerTimeRemainingSeconds - 1
                        if (newPlayerTime <= 0) {
                            handleTimeoutLoss()
                            break
                        }
                        _uiState.update { it.copy(playerTimeRemainingSeconds = newPlayerTime) }
                    } else {
                        val newOppTime = state.opponentTimeRemainingSeconds - 1
                        if (newOppTime <= 0) {
                            handleTimeoutWin()
                            break
                        }
                        _uiState.update { it.copy(opponentTimeRemainingSeconds = newOppTime) }
                    }
                }
            }
        }
    }

    private fun stopClocks() {
        clockTimerJob?.cancel()
        clockTimerJob = null
    }

    private fun handleTimeoutLoss() {
        stopClocks()
        handlePlayerDefeat("Time Out! You ran out of time.")
    }

    private fun handleTimeoutWin() {
        stopClocks()
        handlePlayerVictory("Opponent ran out of time! Victory by timeout! ⏱🏆")
    }

    // =========================================================================
    // PUZZLE MANAGEMENT
    // =========================================================================

    fun loadPuzzle(index: Int) {
        stopClocks()
        cancelAutoSearch()

        val puzzles = _uiState.value.puzzles
        val safeIndex = index.coerceIn(0, puzzles.size - 1)
        val puzzle = puzzles[safeIndex]
        val gameState = ChessGameState.fromFen(puzzle.fen)

        storageRepo.saveCurrentPuzzleIndex(safeIndex)
        undoStateHistory.clear()
        redoStateHistory.clear()

        _uiState.update {
            it.copy(
                currentPuzzleIndex = safeIndex,
                currentPuzzle = puzzle,
                gameState = gameState,
                selectedPosition = null,
                legalMovesForSelected = emptyList(),
                lastMove = null,
                moveHistory = emptyList(),
                undoAvailable = false,
                redoAvailable = false,
                puzzleStep = 0,
                isSolved = false,
                isIncorrectMove = false,
                isGameOverDefeat = false,
                isReviewMode = false,
                hintIndex = -1,
                timeRemainingSeconds = 120,
                playerSide = puzzle.turn,
                statusMessage = "${if (puzzle.turn == PieceColor.WHITE) "White" else "Black"} to move • Level ${safeIndex + 1}: ${puzzle.theme}"
            )
        }

        startClocks()
    }

    fun nextPuzzle() {
        val nextIdx = (_uiState.value.currentPuzzleIndex + 1) % _uiState.value.puzzles.size
        loadPuzzle(nextIdx)
    }

    fun previousPuzzle() {
        val count = _uiState.value.puzzles.size
        val prevIdx = (_uiState.value.currentPuzzleIndex - 1 + count) % count
        loadPuzzle(prevIdx)
    }

    fun restartPuzzle() {
        if (_uiState.value.gameMode == GamePlayMode.PUZZLES_2000) {
            loadPuzzle(_uiState.value.currentPuzzleIndex)
        } else {
            startLobbyGame(_uiState.value.gameMode, _uiState.value.timeControl, _uiState.value.aiLevel)
        }
    }

    fun enterReviewMode() {
        _uiState.update { it.copy(isReviewMode = true) }
    }

    fun sendEmote(emote: String) {
        _uiState.update { it.copy(playerEmote = emote) }
        viewModelScope.launch {
            delay(2500)
            _uiState.update { it.copy(playerEmote = null) }
        }
    }

    // =========================================================================
    // MOVE HANDLING & CHESS MECHANICS
    // =========================================================================

    fun onSquareTapped(pos: Position) = onSquareSelected(pos)

    fun onSquareSelected(pos: Position) {
        val state = _uiState.value
        if (state.isSolved || state.isGameOverDefeat || state.isOpponentMoving || state.isReviewMode) return

        if (state.gameState.activeColor != state.playerSide) return

        val selected = state.selectedPosition
        if (selected == null) {
            val piece = state.gameState.getPiece(pos)
            if (piece != null && piece.color == state.playerSide) {
                val legalMoves = engine.getLegalMoves(state.gameState, pos)
                _uiState.update {
                    it.copy(
                        selectedPosition = pos,
                        legalMovesForSelected = legalMoves
                    )
                }
            }
        } else {
            if (selected == pos) {
                _uiState.update {
                    it.copy(
                        selectedPosition = null,
                        legalMovesForSelected = emptyList()
                    )
                }
                return
            }

            val chosenMove = state.legalMovesForSelected.firstOrNull { it.to == pos }
            if (chosenMove != null) {
                makeUserMove(chosenMove)
            } else {
                val piece = state.gameState.getPiece(pos)
                if (piece != null && piece.color == state.playerSide) {
                    val legalMoves = engine.getLegalMoves(state.gameState, pos)
                    _uiState.update {
                        it.copy(
                            selectedPosition = pos,
                            legalMovesForSelected = legalMoves
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            selectedPosition = null,
                            legalMovesForSelected = emptyList()
                        )
                    }
                }
            }
        }
    }

    private fun makeUserMove(move: Move) {
        val currentState = _uiState.value

        if (currentState.gameMode == GamePlayMode.PUZZLES_2000) {
            executePuzzleMove(move)
        } else {
            executeFullGameMove(move)
        }
    }

    private fun executePuzzleMove(move: Move) {
        val currentState = _uiState.value
        val puzzle = currentState.currentPuzzle
        val currentStep = currentState.puzzleStep

        val expectedUci = puzzle.solutionMoves.getOrNull(currentStep)
        val userUci = move.from.algebraic + move.to.algebraic + (move.promotion?.let { "=${it.notation.lowercase()}" } ?: "")
        val isCorrect = (expectedUci != null && (userUci.startsWith(expectedUci) || expectedUci.startsWith(userUci.substring(0, 4))))

        val newState = engine.makeMove(currentState.gameState, move)
        val updatedHistory = currentState.moveHistory + move
        val isCapture = move.isCapture || currentState.gameState.getPiece(move.to) != null
        val isCheck = engine.isKingInCheck(newState, newState.activeColor)

        if (isCapture) {
            ChessAudioPlayer.playCaptureSound()
        } else if (isCheck) {
            ChessAudioPlayer.playCheckSound()
        } else {
            ChessAudioPlayer.playMoveSound()
        }

        if (isCorrect) {
            val nextStep = currentStep + 1
            if (nextStep >= puzzle.solutionMoves.size) {
                // Puzzle fully solved!
                ChessAudioPlayer.playWinSound()
                stopClocks()
                storageRepo.saveSolvedPuzzle(puzzle.id)
                storageRepo.recordMatchOutcome(won = true, ratingChange = 12)

                val solvedSet = storageRepo.loadSolvedPuzzles()
                val updatedSession = storageRepo.loadUserSession()

                _uiState.update {
                    it.copy(
                        gameState = newState,
                        selectedPosition = null,
                        legalMovesForSelected = emptyList(),
                        lastMove = move,
                        moveHistory = updatedHistory,
                        isSolved = true,
                        isIncorrectMove = false,
                        isGameOverDefeat = false,
                        solvedPuzzleIds = solvedSet,
                        userSession = updatedSession,
                        playerEmote = "GG! 🏆",
                        opponentEmote = "😱 WOW!",
                        statusMessage = "CHECKMATE! Level ${currentState.currentPuzzleIndex + 1} Solved!"
                    )
                }

                // Trigger controlled AdMob Interstitial Ad at completion transition
                viewModelScope.launch {
                    delay(1500)
                    _uiState.update { it.copy(showInterstitialAd = true) }
                }
            } else {
                // Player's move correct; now opponent replies automatically
                _uiState.update {
                    it.copy(
                        gameState = newState,
                        selectedPosition = null,
                        legalMovesForSelected = emptyList(),
                        lastMove = move,
                        moveHistory = updatedHistory,
                        puzzleStep = nextStep,
                        isOpponentMoving = true,
                        statusMessage = if (isCheck) "Check! Opponent responding..." else "Good move! Opponent responding..."
                    )
                }

                viewModelScope.launch {
                    delay(500)
                    playOpponentReply(nextStep, newState, updatedHistory)
                }
            }
        } else {
            // Incorrect move: Do NOT force an ad! Allow retry and offer optional Watch Ad for Help.
            ChessAudioPlayer.playDefeatSound()
            _uiState.update {
                it.copy(
                    gameState = newState,
                    selectedPosition = null,
                    legalMovesForSelected = emptyList(),
                    lastMove = move,
                    isIncorrectMove = true,
                    isGameOverDefeat = false, // Keep puzzle accessible
                    playerEmote = "OOPSIE! 😭",
                    opponentEmote = "HEHE! 😈",
                    statusMessage = "Incorrect move! Try again or tap Watch Ad for Help."
                )
            }

            viewModelScope.launch {
                delay(1800)
                _uiState.update {
                    it.copy(
                        gameState = currentState.gameState,
                        lastMove = currentState.lastMove,
                        selectedPosition = null,
                        legalMovesForSelected = emptyList(),
                        isIncorrectMove = false,
                        playerEmote = null,
                        opponentEmote = null,
                        statusMessage = "${if (puzzle.turn == PieceColor.WHITE) "White" else "Black"} to move • Level ${currentState.currentPuzzleIndex + 1}: ${puzzle.theme}"
                    )
                }
            }
        }
    }

    private fun executeFullGameMove(move: Move) {
        val currentState = _uiState.value
        undoStateHistory.add(currentState.gameState)
        redoStateHistory.clear()

        val newState = engine.makeMove(currentState.gameState, move)
        val updatedHistory = currentState.moveHistory + move
        val notationText = formatMoveHistoryPair(updatedHistory)

        val isCapture = move.isCapture || currentState.gameState.getPiece(move.to) != null
        val isCheck = engine.isKingInCheck(newState, newState.activeColor)
        val isCheckmate = engine.isCheckmate(newState)
        val isStalemate = engine.isStalemate(newState)

        if (isCheckmate) {
            handlePlayerVictory("CHECKMATE! Victory against ${currentState.currentChallenger.username}! 🏆")
            return
        }

        if (isCapture) {
            ChessAudioPlayer.playCaptureSound()
        } else if (isCheck) {
            ChessAudioPlayer.playCheckSound()
        } else {
            ChessAudioPlayer.playMoveSound()
        }

        if (isStalemate) {
            stopClocks()
            _uiState.update {
                it.copy(
                    gameState = newState,
                    lastMove = move,
                    moveHistory = updatedHistory,
                    selectedPosition = null,
                    legalMovesForSelected = emptyList(),
                    undoAvailable = true,
                    redoAvailable = false,
                    lastMoveNotationText = notationText,
                    statusMessage = "Draw by Stalemate! (Tabla)"
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                gameState = newState,
                selectedPosition = null,
                legalMovesForSelected = emptyList(),
                lastMove = move,
                moveHistory = updatedHistory,
                undoAvailable = true,
                redoAvailable = false,
                lastMoveNotationText = notationText,
                isOpponentMoving = true,
                statusMessage = "Good move! ${currentState.currentChallenger.username} is thinking..."
            )
        }

        viewModelScope.launch {
            delay(if (currentState.gameMode == GamePlayMode.ONLINE_MULTIPLAYER) 1100 else 700)
            playBotOrOnlineMove()
        }
    }

    private fun playBotOrOnlineMove() {
        val state = _uiState.value
        if (state.isSolved || state.isGameOverDefeat) return

        val diffLevel = when (state.aiLevel) {
            AiLevel.HARD -> 1
            AiLevel.MASTER -> 2
            AiLevel.GRANDMASTER_IM -> 3
        }

        val bestMove = engine.chooseBestMove(state.gameState, diffLevel)
        if (bestMove == null) {
            if (engine.isKingInCheck(state.gameState, state.gameState.activeColor)) {
                handlePlayerVictory("Opponent in checkmate! YOU WIN! 🏆")
            } else {
                _uiState.update { it.copy(statusMessage = "Game drawn by stalemate.") }
            }
            return
        }

        val nextState = engine.makeMove(state.gameState, bestMove)
        val history = state.moveHistory + bestMove
        val isCheckmate = engine.isCheckmate(nextState)

        if (isCheckmate) {
            handlePlayerDefeat("Checkmate! Talo ka laban kay ${state.currentChallenger.username}.")
            return
        }

        val isCapture = bestMove.isCapture || state.gameState.getPiece(bestMove.to) != null
        val isCheck = engine.isKingInCheck(nextState, nextState.activeColor)

        if (isCapture) {
            ChessAudioPlayer.playCaptureSound()
        } else if (isCheck) {
            ChessAudioPlayer.playCheckSound()
        } else {
            ChessAudioPlayer.playMoveSound()
        }

        _uiState.update {
            it.copy(
                gameState = nextState,
                lastMove = bestMove,
                moveHistory = history,
                isOpponentMoving = false,
                statusMessage = if (isCheck) "CHECK! Defend your King!" else "Your turn: choose your piece."
            )
        }
    }

    // =========================================================================
    // VICTORY, DEFEAT & CONTINUOUS MATCHMAKING (Section D & E)
    // =========================================================================

    private fun handlePlayerVictory(message: String) {
        ChessAudioPlayer.playWinSound()
        stopClocks()

        val state = _uiState.value
        val playerRating = state.userSession.rating
        val oppRating = state.currentChallenger.rating
        val delta = EloCalculator.calculateDelta(playerRating, oppRating, 1.0)

        val historyItem = storageRepo.recordMatchOutcomeWithElo(
            won = true,
            ratingDelta = delta,
            opponentName = state.currentChallenger.username,
            opponentRating = state.currentChallenger.rating,
            opponentFlag = state.currentChallenger.flag
        )

        val updatedSession = storageRepo.loadUserSession()
        val newRank = CompetitiveRank.fromRating(updatedSession.rating)
        val newStreak = state.onlineSessionWinStreak + 1

        _uiState.update {
            it.copy(
                isSolved = true,
                isGameOverDefeat = false,
                isOpponentMoving = false,
                playerEmote = "GG! 🏆",
                opponentEmote = "😱 WOW!",
                statusMessage = "$message (+$delta Elo)",
                userSession = updatedSession,
                competitiveRank = newRank,
                lastRatingDelta = delta,
                onlineSessionWinStreak = newStreak,
                isOnlineSessionActive = (state.gameMode == GamePlayMode.ONLINE_MULTIPLAYER)
            )
        }

        refreshLeaderboardAndHistory()

        // Trigger Interstitial Ad at game transition
        viewModelScope.launch {
            delay(1500)
            _uiState.update { it.copy(showInterstitialAd = true) }
        }

        // Section D: Winning Streak & Continuous Matchmaking
        // Automatically search for next opponent in 5s without returning to dashboard and without deducting entry!
        if (state.gameMode == GamePlayMode.ONLINE_MULTIPLAYER) {
            start5SecOnlineAutoSearch()
        }
    }

    private fun handlePlayerDefeat(message: String) {
        ChessAudioPlayer.playDefeatSound()
        stopClocks()

        val state = _uiState.value
        val playerRating = state.userSession.rating
        val oppRating = state.currentChallenger.rating
        val delta = EloCalculator.calculateDelta(playerRating, oppRating, 0.0)

        storageRepo.recordMatchOutcomeWithElo(
            won = false,
            ratingDelta = delta,
            opponentName = state.currentChallenger.username,
            opponentRating = state.currentChallenger.rating,
            opponentFlag = state.currentChallenger.flag
        )

        val updatedSession = storageRepo.loadUserSession()
        val newRank = CompetitiveRank.fromRating(updatedSession.rating)
        val remainingEntries = storageRepo.getAvailableOnlineCredits()

        _uiState.update {
            it.copy(
                isGameOverDefeat = true,
                isSolved = false,
                isOpponentMoving = false,
                playerEmote = "😭 Defeat",
                opponentEmote = "👑 Checkmate!",
                statusMessage = "$message ($delta Elo) • Session Ended",
                userSession = updatedSession,
                competitiveRank = newRank,
                lastRatingDelta = delta,
                onlineSessionWinStreak = 0,
                isOnlineSessionActive = false,
                availableOnlineCredits = remainingEntries
            )
        }

        refreshLeaderboardAndHistory()
    }

    private fun start5SecOnlineAutoSearch() {
        cancelAutoSearch()
        autoSearchJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAutoSearchingNextChallenger = true,
                    autoSearchCountdown = 5
                )
            }

            for (sec in 5 downTo 1) {
                _uiState.update { it.copy(autoSearchCountdown = sec) }
                delay(1000)
            }

            // 5s elapsed: find next suitable human opponent by rating
            val nextChallenger = StrategyRepository.findOpponentByRating(
                targetRating = _uiState.value.userSession.rating,
                searchRange = 100,
                excludeName = _uiState.value.currentChallenger.username
            )

            startOnlineGameWithChallenger(nextChallenger, _uiState.value.timeControl)
        }
    }

    fun cancelAutoSearch() {
        autoSearchJob?.cancel()
        autoSearchJob = null
        _uiState.update { it.copy(isAutoSearchingNextChallenger = false) }
    }

    private fun playOpponentReply(step: Int, stateBeforeReply: ChessGameState, history: List<Move>) {
        val puzzle = _uiState.value.currentPuzzle
        val opponentUci = puzzle.solutionMoves.getOrNull(step) ?: return
        val oppMove = Move.fromUci(opponentUci) ?: return

        val stateAfterReply = engine.makeMove(stateBeforeReply, oppMove)
        val isCheck = engine.isKingInCheck(stateAfterReply, stateAfterReply.activeColor)
        val isOppCapture = oppMove.isCapture || stateBeforeReply.getPiece(oppMove.to) != null

        if (isOppCapture) {
            ChessAudioPlayer.playCaptureSound()
        } else if (isCheck) {
            ChessAudioPlayer.playCheckSound()
        } else {
            ChessAudioPlayer.playMoveSound()
        }

        _uiState.update {
            it.copy(
                gameState = stateAfterReply,
                lastMove = oppMove,
                moveHistory = history + oppMove,
                puzzleStep = step + 1,
                isOpponentMoving = false,
                statusMessage = if (isCheck) "Check! Deliver the final blow!" else "Your turn: deliver checkmate!"
            )
        }
    }

    fun dismissInterstitialAd(advanceToNextLevel: Boolean = false) {
        _uiState.update { it.copy(showInterstitialAd = false) }
        if (advanceToNextLevel) {
            if (_uiState.value.gameMode == GamePlayMode.PUZZLES_2000) {
                nextPuzzle()
            } else if (_uiState.value.gameMode == GamePlayMode.ONLINE_MULTIPLAYER) {
                cancelAutoSearch()
                startGlobalMatchmaking(_uiState.value.timeControl)
            }
        }
    }

    fun requestHint() {
        val puzzle = _uiState.value.currentPuzzle
        val currentHintIdx = _uiState.value.hintIndex
        val nextHintIdx = (currentHintIdx + 1).coerceAtMost(puzzle.hints.size - 1)
        val hintText = puzzle.hints.getOrNull(nextHintIdx) ?: "Look for forcing checks!"

        _uiState.update {
            it.copy(
                hintIndex = nextHintIdx,
                statusMessage = "💡 Hint ${nextHintIdx + 1}/${puzzle.hints.size}: $hintText"
            )
        }
    }

    fun toggleGodotDialog(show: Boolean) {
        _uiState.update { it.copy(showGodotExportDialog = show) }
    }

    fun toggleLevelSelectMenu(show: Boolean) {
        _uiState.update { it.copy(showLevelSelectMenu = show) }
    }

    fun toggleBoardTheme() {
        _uiState.update { it.copy(isGreenTheme = !it.isGreenTheme) }
    }

    // =========================================================================
    // PRO MASTER REDESIGN CONTROLS (Undo, Redo, Resign, Rewarded Hint Ad)
    // =========================================================================

    fun watchRewardedAdForHint() {
        if (!storageRepo.canWatchRewardedAdForHint()) {
            _uiState.update {
                it.copy(
                    statusMessage = "⚠️ Max 10 rewarded hints reached today! (Resets every 24h)"
                )
            }
            return
        }

        val success = storageRepo.recordRewardedHintWatched()
        if (success) {
            val count = storageRepo.getRewardedHintsUsedToday()
            _uiState.update {
                it.copy(
                    rewardedHintsUsedToday = count,
                    statusMessage = "🎬 Ad Completed! +1 Hint Unlocked ($count/10 used today)"
                )
            }
            requestHint()
        }
    }

    fun undoMove() {
        val state = _uiState.value
        if (state.isSolved || state.isOpponentMoving) return

        if (state.gameMode == GamePlayMode.PUZZLES_2000) {
            restartPuzzle()
            _uiState.update { it.copy(statusMessage = "Move undone • Try again!") }
            return
        }

        if (undoStateHistory.isNotEmpty()) {
            redoStateHistory.add(state.gameState)
            val previousState = undoStateHistory.removeAt(undoStateHistory.size - 1)
            val newHistory = if (state.moveHistory.isNotEmpty()) state.moveHistory.dropLast(1) else emptyList()
            _uiState.update {
                it.copy(
                    gameState = previousState,
                    selectedPosition = null,
                    legalMovesForSelected = emptyList(),
                    lastMove = newHistory.lastOrNull(),
                    moveHistory = newHistory,
                    undoAvailable = undoStateHistory.isNotEmpty(),
                    redoAvailable = true,
                    lastMoveNotationText = formatMoveHistoryPair(newHistory),
                    statusMessage = "Move undone"
                )
            }
        }
    }

    fun redoMove() {
        val state = _uiState.value
        if (redoStateHistory.isNotEmpty()) {
            val nextState = redoStateHistory.removeAt(redoStateHistory.size - 1)
            undoStateHistory.add(state.gameState)
            _uiState.update {
                it.copy(
                    gameState = nextState,
                    selectedPosition = null,
                    legalMovesForSelected = emptyList(),
                    undoAvailable = true,
                    redoAvailable = redoStateHistory.isNotEmpty(),
                    statusMessage = "Move redone"
                )
            }
        }
    }

    fun resignGame() {
        val state = _uiState.value
        if (state.isSolved || state.isGameOverDefeat) return

        if (state.gameMode == GamePlayMode.PUZZLES_2000) {
            restartPuzzle()
            return
        }

        handlePlayerDefeat("You resigned • Match Over")
    }

    fun toggleSettingsDialog(show: Boolean) {
        _uiState.update { it.copy(showSettingsDialog = show) }
    }

    private fun refreshLeaderboardAndHistory() {
        val session = storageRepo.loadUserSession()
        val history = storageRepo.loadMatchHistory()
        val rank = CompetitiveRank.fromRating(session.rating)

        val userEntry = LeaderboardPlayer(
            rank = 1,
            username = session.username,
            country = session.country,
            flag = session.countryFlag,
            rating = session.rating,
            competitiveRank = rank,
            title = if (rank == CompetitiveRank.GRANDMASTER) "GM" else if (rank == CompetitiveRank.MASTER) "FM" else "",
            wins = session.totalWins,
            losses = session.totalLosses
        )
        val ladder = StrategyRepository.getGlobalLeaderboard(userEntry)

        _uiState.update {
            it.copy(
                userSession = session,
                competitiveRank = rank,
                matchHistory = history,
                leaderboard = ladder,
                availableOnlineCredits = storageRepo.getAvailableOnlineCredits(),
                rewardedOnlineEntriesUsedToday = storageRepo.getRewardedOnlineEntriesUsedToday(),
                nextCreditResetTimeText = storageRepo.getNextCreditResetTimeFormatted()
            )
        }
    }

    private fun formatMoveHistoryPair(history: List<Move>): String {
        if (history.isEmpty()) return "Ready"
        val last = history.last()
        val moveNumber = (history.size + 1) / 2
        return if (history.size % 2 != 0) {
            "$moveNumber. ${last.from.algebraic} ➔ ${last.to.algebraic}"
        } else {
            val prev = history[history.size - 2]
            "$moveNumber. ${prev.from.algebraic} ➔ ${prev.to.algebraic}  ${last.to.algebraic}"
        }
    }
}
