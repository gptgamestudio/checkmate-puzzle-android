package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChessStorageRepository
import com.example.data.StrategyRepository
import com.example.engine.ChessEngine
import com.example.engine.ChessGameState
import com.example.model.AiLevel
import com.example.model.DashboardState
import com.example.model.GamePlayMode
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

    // Rewarded Hint Tracker & AdMob (Max 10 per 24h rolling period)
    val rewardedHintsUsedToday: Int = 0,
    val undoAvailable: Boolean = false,
    val redoAvailable: Boolean = false,
    val lastMoveNotationText: String = "1. e4  e5",
    val showSettingsDialog: Boolean = false,

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
    private val undoStateHistory = mutableListOf<ChessGameState>()
    private val redoStateHistory = mutableListOf<ChessGameState>()

    init {
        // Load auto-saved data from device storage
        val savedSolved = storageRepo.loadSolvedPuzzles()
        val savedIndex = storageRepo.loadCurrentPuzzleIndex()
        val savedSession = storageRepo.loadUserSession()
        val savedStrategies = storageRepo.loadCompletedStrategies()
        val rewardedHints = storageRepo.getRewardedHintsUsedToday()

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
                rewardedHintsUsedToday = rewardedHints
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
        _uiState.update { it.copy(currentDashboard = dashboard, showStrategyGuide = false) }
    }

    fun onLoginSuccess(session: UserSession) {
        storageRepo.saveUserSession(session)
        _uiState.update {
            it.copy(
                userSession = session,
                currentDashboard = DashboardState.LOBBY_DASHBOARD
            )
        }
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

    fun startLobbyGame(mode: GamePlayMode, timeControl: TimeControl, aiLevel: AiLevel) {
        stopClocks()
        cancelAutoSearch()

        val initialSeconds = timeControl.totalSeconds
        // Auto-select White or Black pieces for the player
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
                isAutoSearchingNextChallenger = false,
                autoSearchCountdown = 5
            )
        }

        undoStateHistory.clear()
        redoStateHistory.clear()

        if (mode == GamePlayMode.PUZZLES_2000) {
            loadPuzzle(_uiState.value.currentPuzzleIndex)
        } else {
            // Full chess standard initial board
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
                    puzzleStep = 0,
                    statusMessage = if (assignedSide == PieceColor.WHITE) "You play White • Your turn to move" else "You play Black • Opponent moving White..."
                )
            }
            startClocks()

            // If player got Black, opponent moves first
            if (assignedSide == PieceColor.BLACK) {
                viewModelScope.launch {
                    delay(800)
                    playBotOrOnlineMove()
                }
            }
        }
    }

    // =========================================================================
    // CHESS CLOCK & TIMERS
    // =========================================================================

    private fun startClocks() {
        stopClocks()
        clockTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val state = _uiState.value
                if (state.currentDashboard != DashboardState.GAME_DASHBOARD) break
                if (state.isSolved || state.isGameOverDefeat || state.showInterstitialAd) continue

                val isPlayerTurn = state.gameState.activeColor == state.playerSide
                if (isPlayerTurn) {
                    val newTime = (state.playerTimeRemainingSeconds - 1).coerceAtLeast(0)
                    _uiState.update { it.copy(playerTimeRemainingSeconds = newTime) }
                    if (newTime == 0) {
                        onTimeout(isPlayerTimeout = true)
                        break
                    }
                } else {
                    val newTime = (state.opponentTimeRemainingSeconds - 1).coerceAtLeast(0)
                    _uiState.update { it.copy(opponentTimeRemainingSeconds = newTime) }
                    if (newTime == 0) {
                        onTimeout(isPlayerTimeout = false)
                        break
                    }
                }
            }
        }
    }

    private fun stopClocks() {
        clockTimerJob?.cancel()
        clockTimerJob = null
    }

    private fun onTimeout(isPlayerTimeout: Boolean) {
        stopClocks()
        if (isPlayerTimeout) {
            _uiState.update {
                it.copy(
                    isGameOverDefeat = true,
                    statusMessage = "Time out! You lost on time."
                )
            }
            storageRepo.recordMatchOutcome(won = false)
        } else {
            handlePlayerVictory("Opponent timed out! YOU WIN! 🏆")
        }
    }

    // =========================================================================
    // PUZZLE MANAGEMENT & AUTO-SAVE
    // =========================================================================

    fun loadPuzzle(index: Int) {
        val puzzles = _uiState.value.puzzles
        if (index !in puzzles.indices) return
        val puzzle = puzzles[index]
        val initialState = ChessGameState.fromFen(puzzle.fen)

        storageRepo.saveCurrentPuzzleIndex(index)
        undoStateHistory.clear()
        redoStateHistory.clear()

        _uiState.update {
            it.copy(
                currentPuzzleIndex = index,
                currentPuzzle = puzzle,
                gameState = initialState,
                playerSide = puzzle.turn,
                selectedPosition = null,
                legalMovesForSelected = emptyList(),
                lastMove = null,
                moveHistory = emptyList(),
                undoAvailable = false,
                redoAvailable = false,
                lastMoveNotationText = "Level ${index + 1}: ${puzzle.title}",
                puzzleStep = 0,
                isSolved = false,
                isIncorrectMove = false,
                isGameOverDefeat = false,
                statusMessage = "${if (puzzle.turn == PieceColor.WHITE) "White" else "Black"} to move • Level ${index + 1}: ${puzzle.theme}",
                hintIndex = -1,
                showLevelSelectMenu = false,
                showInterstitialAd = false,
                isOpponentMoving = false,
                playerEmote = null,
                opponentEmote = null,
                timeRemainingSeconds = 111,
                isAutoSearchingNextChallenger = false
            )
        }
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

    fun onSquareTapped(pos: Position) {
        val state = _uiState.value
        if (state.isSolved || state.isOpponentMoving || state.showInterstitialAd || state.isGameOverDefeat) return

        // In Online / AI mode, only allow moving own color pieces
        if (state.gameMode != GamePlayMode.PUZZLES_2000 && state.gameState.activeColor != state.playerSide) {
            return
        }

        val selected = state.selectedPosition
        val pieceAtPos = state.gameState.getPiece(pos)

        // Case 1: No piece selected yet
        if (selected == null) {
            if (pieceAtPos != null && pieceAtPos.color == state.gameState.activeColor) {
                val legalMoves = engine.getLegalMoves(state.gameState, pos)
                _uiState.update {
                    it.copy(
                        selectedPosition = pos,
                        legalMovesForSelected = legalMoves,
                        isIncorrectMove = false
                    )
                }
            }
            return
        }

        // Case 2: Tapping the same square deselects
        if (selected == pos) {
            _uiState.update {
                it.copy(selectedPosition = null, legalMovesForSelected = emptyList())
            }
            return
        }

        // Case 3: Tapping another own piece switches selection
        if (pieceAtPos != null && pieceAtPos.color == state.gameState.activeColor) {
            val legalMoves = engine.getLegalMoves(state.gameState, pos)
            _uiState.update {
                it.copy(
                    selectedPosition = pos,
                    legalMovesForSelected = legalMoves,
                    isIncorrectMove = false
                )
            }
            return
        }

        // Case 4: Attempting a move
        val matchingMove = state.legalMovesForSelected.firstOrNull { it.to == pos }
        if (matchingMove != null) {
            executeMove(matchingMove)
        } else {
            _uiState.update {
                it.copy(selectedPosition = null, legalMovesForSelected = emptyList())
            }
        }
    }

    private fun executeMove(move: Move) {
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

        val moveUci = "${move.from.algebraic}${move.to.algebraic}"
        val isCorrect = expectedUci != null && (moveUci == expectedUci || move.notation.startsWith(expectedUci))

        val newState = engine.makeMove(currentState.gameState, move)
        val isCheck = engine.isKingInCheck(newState, newState.activeColor)
        val isCheckmate = engine.isCheckmate(newState)

        val isCapture = move.isCapture || currentState.gameState.getPiece(move.to) != null
        if (isCorrect) {
            val nextStep = currentStep + 1
            val updatedHistory = currentState.moveHistory + move

            if (nextStep >= puzzle.solutionMoves.size || isCheckmate) {
                // Puzzle Solved! Full victory audio fanfare!
                ChessAudioPlayer.playWinSound()
                storageRepo.saveSolvedPuzzle(puzzle.id)
                val newSolvedSet = currentState.solvedPuzzleIds + puzzle.id

                _uiState.update {
                    it.copy(
                        gameState = newState,
                        selectedPosition = null,
                        legalMovesForSelected = emptyList(),
                        lastMove = move,
                        moveHistory = updatedHistory,
                        puzzleStep = nextStep,
                        isSolved = true,
                        isIncorrectMove = false,
                        isGameOverDefeat = false,
                        playerEmote = "GG! 🏆",
                        opponentEmote = "NOOO! 😱",
                        statusMessage = "CHECKMATE! Level ${it.currentPuzzleIndex + 1} Solved 🎉",
                        solvedPuzzleIds = newSolvedSet
                    )
                }

                viewModelScope.launch {
                    delay(2000)
                    _uiState.update { it.copy(showInterstitialAd = true) }
                }
            } else {
                if (isCapture) {
                    ChessAudioPlayer.playCaptureSound()
                } else if (isCheck) {
                    ChessAudioPlayer.playCheckSound()
                } else {
                    ChessAudioPlayer.playMoveSound()
                }

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
            ChessAudioPlayer.playDefeatSound()
            _uiState.update {
                it.copy(
                    gameState = newState,
                    selectedPosition = null,
                    legalMovesForSelected = emptyList(),
                    lastMove = move,
                    isIncorrectMove = true,
                    isGameOverDefeat = true,
                    playerEmote = "OOPSIE! 😭",
                    opponentEmote = "HEHE! 😈",
                    statusMessage = "Incorrect move! Try again."
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
                        isGameOverDefeat = false,
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
            ChessAudioPlayer.playWinSound()
            handlePlayerVictory("CHECKMATE! Panalo ka laban kay ${currentState.currentChallenger.username}! 🏆")
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
                statusMessage = "Good move! Turn ni ${currentState.currentChallenger.username}..."
            )
        }

        // Trigger bot / online response
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
            // No legal moves
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
            ChessAudioPlayer.playDefeatSound()
            stopClocks()
            storageRepo.recordMatchOutcome(won = false)
            _uiState.update {
                it.copy(
                    gameState = nextState,
                    lastMove = bestMove,
                    moveHistory = history,
                    isOpponentMoving = false,
                    isGameOverDefeat = true,
                    playerEmote = "😭 Defeat",
                    opponentEmote = "👑 Checkmate!",
                    statusMessage = "Checkmate! Talo ka sa labang ito. Try again!"
                )
            }
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
                statusMessage = if (isCheck) "CHECK! Depensahan ang King!" else "Your turn: Pumili ng pyesa na ititira."
            )
        }
    }

    // =========================================================================
    // VICTORY & AUTO-SEARCH (5s COUNTDOWN RULE)
    // =========================================================================

    private fun handlePlayerVictory(message: String) {
        ChessAudioPlayer.playWinSound()
        stopClocks()
        storageRepo.recordMatchOutcome(won = true, ratingChange = 18)

        val updatedSession = storageRepo.loadUserSession()

        _uiState.update {
            it.copy(
                isSolved = true,
                isGameOverDefeat = false,
                isOpponentMoving = false,
                playerEmote = "GG! 🏆",
                opponentEmote = "😱 WOW!",
                statusMessage = message,
                userSession = updatedSession
            )
        }

        // Trigger Interstitial Ad
        viewModelScope.launch {
            delay(1800)
            _uiState.update { it.copy(showInterstitialAd = true) }
        }

        // If in ONLINE match: Auto-search in 5 seconds countdown!
        if (_uiState.value.gameMode == GamePlayMode.ONLINE_MULTIPLAYER) {
            start5SecOnlineAutoSearch()
        }
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

            // 5s elapsed: Pick new challenger & auto-start next game!
            val nextChallenger = StrategyRepository.getRandomChallenger(_uiState.value.currentChallenger.username)
            val newSide = if (Random.nextBoolean()) PieceColor.WHITE else PieceColor.BLACK
            val standardStartFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
            val freshState = ChessGameState.fromFen(standardStartFen)

            _uiState.update {
                it.copy(
                    gameState = freshState,
                    selectedPosition = null,
                    legalMovesForSelected = emptyList(),
                    lastMove = null,
                    moveHistory = emptyList(),
                    isSolved = false,
                    isGameOverDefeat = false,
                    isAutoSearchingNextChallenger = false,
                    currentChallenger = nextChallenger,
                    playerSide = newSide,
                    playerTimeRemainingSeconds = it.timeControl.totalSeconds,
                    opponentTimeRemainingSeconds = it.timeControl.totalSeconds,
                    statusMessage = if (newSide == PieceColor.WHITE) "Bagong Challenger: ${nextChallenger.username} • Turn mo (White)!" else "Bagong Challenger: ${nextChallenger.username} • Turn ng kalaban (White)..."
                )
            }

            startClocks()

            if (newSide == PieceColor.BLACK) {
                delay(900)
                playBotOrOnlineMove()
            }
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
                // Instantly advance to next challenger
                cancelAutoSearch()
                startLobbyGame(GamePlayMode.ONLINE_MULTIPLAYER, _uiState.value.timeControl, _uiState.value.aiLevel)
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

        stopClocks()
        storageRepo.recordMatchOutcome(won = false, ratingChange = 12)
        ChessAudioPlayer.playDefeatSound()
        _uiState.update {
            it.copy(
                isGameOverDefeat = true,
                statusMessage = "🏳️ You resigned • Match Over",
                playerEmote = "OOPSIE! 😭",
                opponentEmote = "GG! 🏆",
                userSession = storageRepo.loadUserSession()
            )
        }
    }

    fun toggleSettingsDialog(show: Boolean) {
        _uiState.update { it.copy(showSettingsDialog = show) }
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
