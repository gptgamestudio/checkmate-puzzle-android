package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.ChessEngine
import com.example.engine.ChessGameState
import com.example.model.Move
import com.example.model.PieceColor
import com.example.model.Position
import com.example.model.Puzzle
import com.example.puzzles.PuzzleRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChessPuzzleUiState(
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
    // Emotes & Reactions
    val playerEmote: String? = null,
    val opponentEmote: String? = null,
    val timeRemainingSeconds: Int = 111, // 01:51 matching screenshot
    // AdMob Integration State
    val showInterstitialAd: Boolean = false,
    val adMobAppId: String = "ca-app-pub-9786472984542330~7452842897",
    val adMobBannerId: String = "ca-app-pub-9786472984542330/2771361852",
    val adMobInterstitialId: String = "ca-app-pub-9786472984542330/2579790164"
)

class ChessPuzzleViewModel : ViewModel() {

    private val engine = ChessEngine()
    private val _uiState = MutableStateFlow(ChessPuzzleUiState())
    val uiState: StateFlow<ChessPuzzleUiState> = _uiState.asStateFlow()

    init {
        loadPuzzle(0)
    }

    fun loadPuzzle(index: Int) {
        val puzzles = _uiState.value.puzzles
        if (index !in puzzles.indices) return
        val puzzle = puzzles[index]
        val initialState = ChessGameState.fromFen(puzzle.fen)

        _uiState.update {
            it.copy(
                currentPuzzleIndex = index,
                currentPuzzle = puzzle,
                gameState = initialState,
                selectedPosition = null,
                legalMovesForSelected = emptyList(),
                lastMove = null,
                moveHistory = emptyList(),
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
                timeRemainingSeconds = 111
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
        loadPuzzle(_uiState.value.currentPuzzleIndex)
    }

    fun sendEmote(emote: String) {
        _uiState.update { it.copy(playerEmote = emote) }
        viewModelScope.launch {
            delay(2500)
            _uiState.update { it.copy(playerEmote = null) }
        }
    }

    fun onSquareTapped(pos: Position) {
        val state = _uiState.value
        if (state.isSolved || state.isOpponentMoving || state.showInterstitialAd) return

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
            executePlayerMove(matchingMove)
        } else {
            // Invalid destination tapped
            _uiState.update {
                it.copy(selectedPosition = null, legalMovesForSelected = emptyList())
            }
        }
    }

    private fun executePlayerMove(move: Move) {
        val currentState = _uiState.value
        val puzzle = currentState.currentPuzzle
        val currentStep = currentState.puzzleStep
        val expectedUci = puzzle.solutionMoves.getOrNull(currentStep)

        val moveUci = "${move.from.algebraic}${move.to.algebraic}"
        val isCorrect = expectedUci != null && (moveUci == expectedUci || move.notation.startsWith(expectedUci))

        // Execute move on engine
        val newState = engine.makeMove(currentState.gameState, move)
        val isCheck = engine.isKingInCheck(newState, newState.activeColor)
        val isCheckmate = engine.isCheckmate(newState)

        if (isCorrect) {
            val nextStep = currentStep + 1
            val updatedHistory = currentState.moveHistory + move

            if (nextStep >= puzzle.solutionMoves.size || isCheckmate) {
                // Puzzle Solved & Checkmate Delivered!
                // Player is WINNER (thumbs up avatar, YOU WIN banner)
                // Opponent is LOSER (shocked face)
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
                        solvedPuzzleIds = it.solvedPuzzleIds + puzzle.id
                    )
                }

                // Monetization Rule: Trigger Interstitial Ad upon puzzle solve
                viewModelScope.launch {
                    delay(2000)
                    _uiState.update { it.copy(showInterstitialAd = true) }
                }
            } else {
                // Multi-step puzzle: computer auto-plays response
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
            // Wrong move!
            // Player is LOSER (shocked face, TRY AGAIN banner)
            // Opponent is WINNER (smug laugh)
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

            // Roll back after brief visual display
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

    private fun playOpponentReply(step: Int, stateBeforeReply: ChessGameState, history: List<Move>) {
        val puzzle = _uiState.value.currentPuzzle
        val opponentUci = puzzle.solutionMoves.getOrNull(step) ?: return
        val oppMove = Move.fromUci(opponentUci) ?: return

        val stateAfterReply = engine.makeMove(stateBeforeReply, oppMove)
        val isCheck = engine.isKingInCheck(stateAfterReply, stateAfterReply.activeColor)

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
            nextPuzzle()
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
}
