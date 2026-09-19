package com.example.chess.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.ai.AiDifficulty
import com.example.chess.ai.GeminiChessAi
import com.example.chess.ai.HeuristicChessAi
import com.example.chess.auth.GoogleAuthManager
import com.example.chess.auth.GoogleUser
import com.example.chess.data.ChessDatabase
import com.example.chess.data.ChessRepository
import com.example.chess.data.DetailedStats
import com.example.chess.data.GameRecordEntity
import com.example.chess.engine.ChessEngine
import com.example.chess.engine.GameState
import com.example.chess.engine.MoveRecord
import com.example.chess.model.GameOutcome
import com.example.chess.model.Move
import com.example.chess.model.Piece
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import com.example.chess.model.Square
import com.example.chess.model.WinReason
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class BoardTheme(val displayName: String, val lightColor: Long, val darkColor: Long) {
    WOOD("Classic Wood", 0xFFEADECA, 0xFFB88B4A),
    NAVY("Midnight Slate", 0xFFDFE6ED, 0xFF4A6B82),
    FOREST("Emerald Pine", 0xFFE2EBD8, 0xFF698F5F)
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ChessViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ChessDatabase.getInstance(application)
    private val repository = ChessRepository(db.chessDao())
    val authManager = GoogleAuthManager(application)
    private val geminiAi = GeminiChessAi()

    val currentUser: StateFlow<GoogleUser> = authManager.currentUser

    // Game state
    private val _gameState = MutableStateFlow(ChessEngine.createInitialState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _playerColor = MutableStateFlow(PieceColor.WHITE)
    val playerColor: StateFlow<PieceColor> = _playerColor.asStateFlow()

    private val _difficulty = MutableStateFlow(AiDifficulty.CLUB)
    val difficulty: StateFlow<AiDifficulty> = _difficulty.asStateFlow()

    private val _selectedSquare = MutableStateFlow<Square?>(null)
    val selectedSquare: StateFlow<Square?> = _selectedSquare.asStateFlow()

    private val _legalMovesForSelected = MutableStateFlow<List<Move>>(emptyList())
    val legalMovesForSelected: StateFlow<List<Move>> = _legalMovesForSelected.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _aiCommentary = MutableStateFlow("Make your opening move to begin the game.")
    val aiCommentary: StateFlow<String> = _aiCommentary.asStateFlow()

    private val _coachHint = MutableStateFlow<String?>(null)
    val coachHint: StateFlow<String?> = _coachHint.asStateFlow()

    private val _isCoachLoading = MutableStateFlow(false)
    val isCoachLoading: StateFlow<Boolean> = _isCoachLoading.asStateFlow()

    private val _lastMove = MutableStateFlow<Move?>(null)
    val lastMove: StateFlow<Move?> = _lastMove.asStateFlow()

    private val _boardFlipped = MutableStateFlow(false)
    val boardFlipped: StateFlow<Boolean> = _boardFlipped.asStateFlow()

    private val _boardTheme = MutableStateFlow(BoardTheme.NAVY)
    val boardTheme: StateFlow<BoardTheme> = _boardTheme.asStateFlow()

    private val _gameDurationSeconds = MutableStateFlow(0L)
    val gameDurationSeconds: StateFlow<Long> = _gameDurationSeconds.asStateFlow()

    // Captured pieces calculation
    private val _whiteCaptured = MutableStateFlow<List<Piece>>(emptyList())
    val whiteCaptured: StateFlow<List<Piece>> = _whiteCaptured.asStateFlow()

    private val _blackCaptured = MutableStateFlow<List<Piece>>(emptyList())
    val blackCaptured: StateFlow<List<Piece>> = _blackCaptured.asStateFlow()

    private val _materialScore = MutableStateFlow(0)
    val materialScore: StateFlow<Int> = _materialScore.asStateFlow()

    // Review Mode
    private val _selectedGameForReview = MutableStateFlow<GameRecordEntity?>(null)
    val selectedGameForReview: StateFlow<GameRecordEntity?> = _selectedGameForReview.asStateFlow()

    private val _reviewMoveIndex = MutableStateFlow(0)
    val reviewMoveIndex: StateFlow<Int> = _reviewMoveIndex.asStateFlow()

    private val _reviewGameState = MutableStateFlow(ChessEngine.createInitialState())
    val reviewGameState: StateFlow<GameState> = _reviewGameState.asStateFlow()

    private var timerJob: Job? = null
    private var gameStartTime = System.currentTimeMillis()

    // Dynamic stats bound to currentUser
    val detailedStats: StateFlow<DetailedStats> = currentUser.flatMapLatest { user ->
        repository.getDetailedStatsForUser(user.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetailedStats())

    val userGames: StateFlow<List<GameRecordEntity>> = currentUser.flatMapLatest { user ->
        repository.getGamesForUser(user.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        gameStartTime = System.currentTimeMillis()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (_gameState.value.outcome == GameOutcome.ONGOING) {
                    _gameDurationSeconds.value = (System.currentTimeMillis() - gameStartTime) / 1000
                }
            }
        }
    }

    fun setDifficulty(newDifficulty: AiDifficulty) {
        _difficulty.value = newDifficulty
    }

    fun setBoardTheme(theme: BoardTheme) {
        _boardTheme.value = theme
    }

    fun toggleBoardFlip() {
        _boardFlipped.value = !_boardFlipped.value
    }

    fun onSquareClicked(square: Square) {
        if (_isAiThinking.value) return
        val current = _gameState.value
        if (current.outcome != GameOutcome.ONGOING) return

        val selected = _selectedSquare.value

        // Check if destination matches any legal move for currently selected square
        if (selected != null) {
            val matchingMove = _legalMovesForSelected.value.firstOrNull { it.to == square }
            if (matchingMove != null) {
                applyPlayerMove(matchingMove)
                _selectedSquare.value = null
                _legalMovesForSelected.value = emptyList()
                return
            }
        }

        // Otherwise select friendly piece
        val piece = current.getPiece(square)
        if (piece != null && piece.color == _playerColor.value && current.turn == _playerColor.value) {
            _selectedSquare.value = square
            _legalMovesForSelected.value = ChessEngine.getLegalMovesForSquare(current, square)
            vibrate(10)
        } else {
            _selectedSquare.value = null
            _legalMovesForSelected.value = emptyList()
        }
    }

    private fun applyPlayerMove(move: Move) {
        val currentState = _gameState.value
        val nextState = ChessEngine.applyMove(currentState, move)
        _gameState.value = nextState
        _lastMove.value = move
        _coachHint.value = null
        updateCapturedPieces(nextState)
        vibrate(20)

        checkGameCompletion(nextState)

        if (nextState.outcome == GameOutcome.ONGOING && nextState.turn != _playerColor.value) {
            triggerAiMove(nextState)
        }
    }

    private fun triggerAiMove(state: GameState) {
        viewModelScope.launch {
            _isAiThinking.value = true
            // Natural human-like pacing for AI thinking
            val thinkingDelay = when (_difficulty.value) {
                AiDifficulty.CASUAL -> 400L
                AiDifficulty.CLUB -> 700L
                AiDifficulty.GRANDMASTER -> 1000L
            }
            delay(thinkingDelay)

            try {
                val aiResult = geminiAi.getAiMove(state, _difficulty.value)
                val nextState = ChessEngine.applyMove(state, aiResult.move)
                _gameState.value = nextState
                _lastMove.value = aiResult.move
                _aiCommentary.value = aiResult.commentary
                updateCapturedPieces(nextState)
                vibrate(25)

                checkGameCompletion(nextState)
            } catch (e: Exception) {
                _aiCommentary.value = "AI calculation interrupted: ${e.message}"
            } finally {
                _isAiThinking.value = false
            }
        }
    }

    private fun checkGameCompletion(state: GameState) {
        if (state.outcome == GameOutcome.ONGOING) return

        val user = currentUser.value
        val playerColor = _playerColor.value
        val result = when (state.outcome) {
            GameOutcome.WHITE_WINS -> if (playerColor == PieceColor.WHITE) "WIN" else "LOSS"
            GameOutcome.BLACK_WINS -> if (playerColor == PieceColor.BLACK) "WIN" else "LOSS"
            GameOutcome.DRAW -> "DRAW"
            else -> return
        }

        val winReason = when (state.winReason) {
            WinReason.CHECKMATE -> "Checkmate"
            WinReason.RESIGNATION -> "Resignation"
            WinReason.STALEMATE -> "Stalemate"
            WinReason.FIFTY_MOVES -> "50-Move Rule"
            WinReason.INSUFFICIENT_MATERIAL -> "Insufficient Material"
            WinReason.TIMEOUT -> "Timeout"
            WinReason.AGREEMENT -> "Mutual Agreement"
            null -> "Game Ended"
        }

        val movesSan = state.moveHistory.map { it.san }

        viewModelScope.launch {
            repository.recordGame(
                userGoogleId = user.id,
                userEmail = user.email,
                playerColor = playerColor.name,
                difficulty = _difficulty.value,
                result = result,
                winReason = winReason,
                moveCount = state.moveHistory.size,
                durationSeconds = _gameDurationSeconds.value,
                movesSan = movesSan,
                fenFinal = ChessEngine.toFen(state)
            )
        }
    }

    fun resign() {
        val current = _gameState.value
        if (current.outcome != GameOutcome.ONGOING) return
        val outcome = if (_playerColor.value == PieceColor.WHITE) GameOutcome.BLACK_WINS else GameOutcome.WHITE_WINS
        val finalState = current.copy(outcome = outcome, winReason = WinReason.RESIGNATION)
        _gameState.value = finalState
        _aiCommentary.value = "You resigned the match. Review your games in Stats to spot tactical improvements!"
        checkGameCompletion(finalState)
    }

    fun offerDraw() {
        val current = _gameState.value
        if (current.outcome != GameOutcome.ONGOING) return

        // Grandmaster / AI decides whether to accept draw based on evaluation
        val eval = geminiAi.runCatching {
            // If roughly equal (-150..150), accept
            val score = HeuristicChessAi().evaluatePosition(current)
            kotlin.math.abs(score) < 150
        }.getOrDefault(true)

        if (eval) {
            val finalState = current.copy(outcome = GameOutcome.DRAW, winReason = WinReason.AGREEMENT)
            _gameState.value = finalState
            _aiCommentary.value = "AI accepted the draw offer. A balanced contest!"
            checkGameCompletion(finalState)
        } else {
            _aiCommentary.value = "Draw offer declined. The AI sees an advantageous position and chooses to play on!"
        }
    }

    fun undoMove() {
        if (_isAiThinking.value) return
        val current = _gameState.value
        if (current.moveHistory.size < 2) return

        // Rewind 2 moves (AI move + Player move)
        val targetMoveCount = current.moveHistory.size - 2
        var replay = ChessEngine.createInitialState()
        for (i in 0 until targetMoveCount) {
            replay = ChessEngine.applyMove(replay, current.moveHistory[i].move)
        }
        _gameState.value = replay
        _lastMove.value = replay.moveHistory.lastOrNull()?.move
        _selectedSquare.value = null
        _legalMovesForSelected.value = emptyList()
        _coachHint.value = null
        _aiCommentary.value = "Move undone. Position restored."
        updateCapturedPieces(replay)
    }

    fun startNewGame(color: PieceColor = _playerColor.value, difficulty: AiDifficulty = _difficulty.value) {
        _playerColor.value = color
        _difficulty.value = difficulty
        _boardFlipped.value = (color == PieceColor.BLACK)
        _selectedSquare.value = null
        _legalMovesForSelected.value = emptyList()
        _isAiThinking.value = false
        _coachHint.value = null
        _lastMove.value = null
        _aiCommentary.value = "New match started vs ${difficulty.displayName}. Good luck!"

        val initial = ChessEngine.createInitialState()
        _gameState.value = initial
        updateCapturedPieces(initial)
        startTimer()

        // If player chose BLACK, AI makes first move
        if (color == PieceColor.BLACK) {
            triggerAiMove(initial)
        }
    }

    fun requestCoachHint() {
        if (_isAiThinking.value || _isCoachLoading.value) return
        val current = _gameState.value
        if (current.outcome != GameOutcome.ONGOING) return

        viewModelScope.launch {
            _isCoachLoading.value = true
            _coachHint.value = null
            try {
                val hint = geminiAi.getCoachHint(current, _playerColor.value)
                _coachHint.value = hint
            } catch (e: Exception) {
                _coachHint.value = "Look for open lines and active tactical piece placement."
            } finally {
                _isCoachLoading.value = false
            }
        }
    }

    fun dismissCoachHint() {
        _coachHint.value = null
    }

    private fun updateCapturedPieces(state: GameState) {
        val initialCounts = mapOf(
            PieceType.PAWN to 8,
            PieceType.KNIGHT to 2,
            PieceType.BISHOP to 2,
            PieceType.ROOK to 2,
            PieceType.QUEEN to 1
        )

        val whiteCurrent = mutableMapOf<PieceType, Int>()
        val blackCurrent = mutableMapOf<PieceType, Int>()

        for (r in 0..7) {
            for (f in 0..7) {
                val p = state.board[r][f] ?: continue
                if (p.color == PieceColor.WHITE) {
                    whiteCurrent[p.type] = (whiteCurrent[p.type] ?: 0) + 1
                } else {
                    blackCurrent[p.type] = (blackCurrent[p.type] ?: 0) + 1
                }
            }
        }

        val capturedFromWhite = mutableListOf<Piece>()
        val capturedFromBlack = mutableListOf<Piece>()

        var whiteVal = 0
        var blackVal = 0

        initialCounts.forEach { (type, count) ->
            val wLost = count - (whiteCurrent[type] ?: 0)
            if (wLost > 0) {
                repeat(wLost) { capturedFromWhite.add(Piece(type, PieceColor.WHITE)) }
            }
            val bLost = count - (blackCurrent[type] ?: 0)
            if (bLost > 0) {
                repeat(bLost) { capturedFromBlack.add(Piece(type, PieceColor.BLACK)) }
            }

            whiteVal += (whiteCurrent[type] ?: 0) * type.baseValue
            blackVal += (blackCurrent[type] ?: 0) * type.baseValue
        }

        _whiteCaptured.value = capturedFromWhite
        _blackCaptured.value = capturedFromBlack
        _materialScore.value = (whiteVal - blackVal) / 100
    }

    fun signInWithCustomAccount(email: String, displayName: String) {
        authManager.signInWithCustomAccount(email, displayName)
    }

    fun signInWithGoogle(activity: Activity, onError: (String) -> Unit = {}) {
        authManager.signInWithGoogle(
            activity = activity,
            scope = viewModelScope,
            onSuccess = {},
            onError = onError
        )
    }

    fun signOut() {
        authManager.signOut()
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory(currentUser.value.id)
        }
    }

    // Review / Replay mode
    fun openGameReview(game: GameRecordEntity) {
        _selectedGameForReview.value = game
        _reviewMoveIndex.value = 0
        rebuildReviewBoard(game, 0)
    }

    fun closeGameReview() {
        _selectedGameForReview.value = null
    }

    fun stepReviewMove(delta: Int) {
        val game = _selectedGameForReview.value ?: return
        val moves = if (game.movesSanJson.isNotBlank()) game.movesSanJson.split(",") else emptyList()
        val newIndex = (_reviewMoveIndex.value + delta).coerceIn(0, moves.size)
        _reviewMoveIndex.value = newIndex
        rebuildReviewBoard(game, newIndex)
    }

    private fun rebuildReviewBoard(game: GameRecordEntity, moveCount: Int) {
        val moves = if (game.movesSanJson.isNotBlank()) game.movesSanJson.split(",") else emptyList()
        var st = ChessEngine.createInitialState()
        for (i in 0 until moveCount) {
            val san = moves[i]
            val legal = ChessEngine.getLegalMoves(st)
            val matched = legal.firstOrNull {
                val sim = ChessEngine.applyMove(st, it)
                sim.moveHistory.lastOrNull()?.san?.replace("+", "")?.replace("#", "") ==
                        san.replace("+", "").replace("#", "")
            } ?: legal.firstOrNull()
            if (matched != null) {
                st = ChessEngine.applyMove(st, matched)
            }
        }
        _reviewGameState.value = st
    }

    private fun vibrate(ms: Long) {
        try {
            val context = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(ms)
            }
        } catch (e: Exception) {
            // Ignore vibration errors
        }
    }
}
