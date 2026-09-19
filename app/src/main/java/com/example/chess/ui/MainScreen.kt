package com.example.chess.ui

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chess.ui.account.AccountScreen
import com.example.chess.ui.game.GameScreen
import com.example.chess.ui.history.HistoryScreen
import com.example.chess.ui.stats.StatsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ChessViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    var currentTab by rememberSaveable { mutableIntStateOf(0) }

    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val playerColor by viewModel.playerColor.collectAsStateWithLifecycle()
    val difficulty by viewModel.difficulty.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val selectedSquare by viewModel.selectedSquare.collectAsStateWithLifecycle()
    val legalMoves by viewModel.legalMovesForSelected.collectAsStateWithLifecycle()
    val lastMove by viewModel.lastMove.collectAsStateWithLifecycle()
    val isAiThinking by viewModel.isAiThinking.collectAsStateWithLifecycle()
    val aiCommentary by viewModel.aiCommentary.collectAsStateWithLifecycle()
    val coachHint by viewModel.coachHint.collectAsStateWithLifecycle()
    val isCoachLoading by viewModel.isCoachLoading.collectAsStateWithLifecycle()
    val boardFlipped by viewModel.boardFlipped.collectAsStateWithLifecycle()
    val boardTheme by viewModel.boardTheme.collectAsStateWithLifecycle()
    val whiteCaptured by viewModel.whiteCaptured.collectAsStateWithLifecycle()
    val blackCaptured by viewModel.blackCaptured.collectAsStateWithLifecycle()
    val materialScore by viewModel.materialScore.collectAsStateWithLifecycle()
    val gameDurationSeconds by viewModel.gameDurationSeconds.collectAsStateWithLifecycle()

    val detailedStats by viewModel.detailedStats.collectAsStateWithLifecycle()
    val userGames by viewModel.userGames.collectAsStateWithLifecycle()

    val selectedGameForReview by viewModel.selectedGameForReview.collectAsStateWithLifecycle()
    val reviewGameState by viewModel.reviewGameState.collectAsStateWithLifecycle()
    val reviewMoveIndex by viewModel.reviewMoveIndex.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Chess AI",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${detailedStats.rating}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    // Google Account Profile button
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF4285F4), Color(0xFF34A853))
                                )
                            )
                            .clickable { currentTab = 3 }
                            .testTag("top_bar_account_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.initials,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.testTag("bottom_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.SportsEsports, contentDescription = "Play Chess") },
                    label = { Text("Play", fontSize = 11.sp) },
                    modifier = Modifier.testTag("tab_play")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Win-Loss Stats") },
                    label = { Text("Stats", fontSize = 11.sp) },
                    modifier = Modifier.testTag("tab_stats")
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.History, contentDescription = "Match History") },
                    label = { Text("History", fontSize = 11.sp) },
                    modifier = Modifier.testTag("tab_history")
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Google Account") },
                    label = { Text("Account", fontSize = 11.sp) },
                    modifier = Modifier.testTag("tab_account")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab_transition"
            ) { tab ->
                when (tab) {
                    0 -> GameScreen(
                        gameState = gameState,
                        playerColor = playerColor,
                        difficulty = difficulty,
                        user = user,
                        selectedSquare = selectedSquare,
                        legalMoves = legalMoves,
                        lastMove = lastMove,
                        isAiThinking = isAiThinking,
                        aiCommentary = aiCommentary,
                        coachHint = coachHint,
                        isCoachLoading = isCoachLoading,
                        boardFlipped = boardFlipped,
                        boardTheme = boardTheme,
                        whiteCaptured = whiteCaptured,
                        blackCaptured = blackCaptured,
                        materialScore = materialScore,
                        gameDurationSeconds = gameDurationSeconds,
                        onSquareClick = viewModel::onSquareClicked,
                        onUndo = viewModel::undoMove,
                        onHint = viewModel::requestCoachHint,
                        onDismissHint = viewModel::dismissCoachHint,
                        onFlip = viewModel::toggleBoardFlip,
                        onResign = viewModel::resign,
                        onDraw = viewModel::offerDraw,
                        onNewGame = { col, diff -> viewModel.startNewGame(col, diff) },
                        onNavigateToStats = { currentTab = 1 }
                    )
                    1 -> StatsScreen(
                        stats = detailedStats,
                        user = user
                    )
                    2 -> HistoryScreen(
                        games = userGames,
                        onSelectGameForReview = viewModel::openGameReview,
                        selectedGameForReview = selectedGameForReview,
                        reviewGameState = reviewGameState,
                        reviewMoveIndex = reviewMoveIndex,
                        onStepReviewMove = viewModel::stepReviewMove,
                        onCloseReview = viewModel::closeGameReview,
                        boardTheme = boardTheme
                    )
                    3 -> AccountScreen(
                        user = user,
                        difficulty = difficulty,
                        boardTheme = boardTheme,
                        playerColor = playerColor,
                        onSetDifficulty = viewModel::setDifficulty,
                        onSetBoardTheme = viewModel::setBoardTheme,
                        onSetPlayerColor = { col -> viewModel.startNewGame(col, difficulty) },
                        onSignInCustom = viewModel::signInWithCustomAccount,
                        onSignInGoogle = { act -> viewModel.signInWithGoogle(act) },
                        onSignOut = viewModel::signOut,
                        onClearHistory = viewModel::clearHistory
                    )
                }
            }
        }
    }
}
