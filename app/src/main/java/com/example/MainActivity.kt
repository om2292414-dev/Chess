package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chess.audio.ChessSoundEngine
import com.example.chess.data.ChessDatabase
import com.example.chess.data.ChessRepository
import com.example.chess.data.SettingsStore
import com.example.chess.data.UserSettings
import com.example.chess.model.GameMode
import com.example.chess.model.PieceColor
import com.example.ui.screens.AnalysisScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PlayScreen
import com.example.ui.screens.PuzzlesScreen
import com.example.ui.screens.SettingsBottomSheet
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.theme.ChessBgDeep
import com.example.ui.theme.ChessBgPrimary
import com.example.ui.theme.ChessGreenPrimary
import com.example.ui.theme.ChessSurfaceElevated
import com.example.ui.theme.ChessTextMuted
import com.example.ui.theme.ChessTextPrimary
import com.example.ui.theme.GrandMasterChessTheme
import com.example.viewmodel.AnalysisViewModel
import com.example.viewmodel.GameViewModel
import com.example.viewmodel.PuzzleTabMode
import com.example.viewmodel.PuzzleViewModel
import kotlinx.coroutines.launch

enum class MainTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    PLAY("Play", Icons.Filled.PlayArrow, Icons.Outlined.PlayArrow),
    PUZZLES("Puzzles", Icons.Filled.Extension, Icons.Outlined.Extension),
    ANALYSIS("Analysis", Icons.Filled.Insights, Icons.Outlined.Insights),
    STATS("Stats", Icons.Filled.BarChart, Icons.Outlined.BarChart)
}

class MainActivity : ComponentActivity() {

    private lateinit var repository: ChessRepository
    private lateinit var soundEngine: ChessSoundEngine
    private lateinit var gameViewModel: GameViewModel
    private lateinit var puzzleViewModel: PuzzleViewModel
    private lateinit var analysisViewModel: AnalysisViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )

        val db = ChessDatabase.getInstance(applicationContext)
        val settingsStore = SettingsStore(applicationContext)
        repository = ChessRepository(db.chessDao(), settingsStore)
        soundEngine = ChessSoundEngine()

        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
                GameViewModel::class.java -> GameViewModel(repository, soundEngine) as T
                PuzzleViewModel::class.java -> PuzzleViewModel(repository, soundEngine) as T
                AnalysisViewModel::class.java -> AnalysisViewModel(repository, soundEngine) as T
                else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
            }
        }
        gameViewModel = ViewModelProvider(this, factory)[GameViewModel::class.java]
        puzzleViewModel = ViewModelProvider(this, factory)[PuzzleViewModel::class.java]
        analysisViewModel = ViewModelProvider(this, factory)[AnalysisViewModel::class.java]

        setContent {
            GrandMasterChessTheme {
                GrandMasterChessApp(
                    repository = repository,
                    soundEngine = soundEngine,
                    gameViewModel = gameViewModel,
                    puzzleViewModel = puzzleViewModel,
                    analysisViewModel = analysisViewModel
                )
            }
        }
    }
}

@Composable
private fun GrandMasterChessApp(
    repository: ChessRepository,
    soundEngine: ChessSoundEngine,
    gameViewModel: GameViewModel,
    puzzleViewModel: PuzzleViewModel,
    analysisViewModel: AnalysisViewModel
) {
    var showSplash by rememberSaveable { mutableStateOf(true) }
    var currentTab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val settings by repository.settings.collectAsStateWithLifecycle(initialValue = UserSettings())
    val allGames by repository.allGames.collectAsStateWithLifecycle(initialValue = emptyList())
    val recentGames by repository.recentGames.collectAsStateWithLifecycle(initialValue = emptyList())
    val defeatedBotsList by repository.defeatedBotIds.collectAsStateWithLifecycle(initialValue = emptyList())
    val solvedPuzzles by repository.solvedPuzzles.collectAsStateWithLifecycle(initialValue = emptyList())

    val gameUiState by gameViewModel.uiState.collectAsStateWithLifecycle()
    val puzzleUiState by puzzleViewModel.uiState.collectAsStateWithLifecycle()
    val analysisUiState by analysisViewModel.uiState.collectAsStateWithLifecycle()

    val defeatedBotIds = remember(defeatedBotsList) { defeatedBotsList.toSet() }
    val solvedPuzzleIds = remember(solvedPuzzles) { solvedPuzzles.map { it.puzzleId }.toSet() }

    LaunchedEffect(settings.soundEnabled) {
        soundEngine.enabled = settings.soundEnabled
    }

    // Back navigation handling for secondary tabs & active match
    BackHandler(enabled = !showSplash && (currentTab != MainTab.HOME || gameUiState.inActiveMatch)) {
        if (currentTab == MainTab.PLAY && gameUiState.inActiveMatch) {
            gameViewModel.openNewGameLobby()
        } else if (currentTab != MainTab.HOME) {
            currentTab = MainTab.HOME
        }
    }

    Crossfade(
        targetState = showSplash,
        animationSpec = tween(durationMillis = 380),
        label = "splash_transition"
    ) { isSplashing ->
        if (isSplashing) {
            SplashScreen(onFinished = { showSplash = false })
        } else {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ChessBgPrimary)
            ) {
                val useNavigationRail = maxWidth >= 680.dp

                val mainContent: @Composable (Modifier) -> Unit = { contentModifier ->
                    Box(modifier = contentModifier) {
                        when (currentTab) {
                            MainTab.HOME -> HomeScreen(
                                settings = settings,
                                recentGames = recentGames,
                                defeatedBotsCount = defeatedBotIds.size,
                                solvedPuzzlesCount = solvedPuzzleIds.size,
                                onPlayVsBots = {
                                    gameViewModel.selectMode(GameMode.VS_BOT)
                                    gameViewModel.openNewGameLobby()
                                    currentTab = MainTab.PLAY
                                },
                                onPlayPassAndPlay = {
                                    gameViewModel.selectMode(GameMode.PASS_AND_PLAY)
                                    gameViewModel.openNewGameLobby()
                                    currentTab = MainTab.PLAY
                                },
                                onOpenDailyPuzzle = {
                                    puzzleViewModel.openDailyPuzzle()
                                    currentTab = MainTab.PUZZLES
                                },
                                onOpenPuzzleRush = {
                                    puzzleViewModel.selectTabMode(PuzzleTabMode.RUSH)
                                    currentTab = MainTab.PUZZLES
                                },
                                onOpenAnalysis = { currentTab = MainTab.ANALYSIS },
                                onOpenGameInAnalysis = { saved ->
                                    analysisViewModel.loadSavedGame(saved, autoRunReview = false)
                                    currentTab = MainTab.ANALYSIS
                                },
                                onOpenSettings = { showSettingsSheet = true }
                            )

                            MainTab.PLAY -> PlayScreen(
                                uiState = gameUiState,
                                settings = settings,
                                defeatedBotIds = defeatedBotIds,
                                onSelectMode = gameViewModel::selectMode,
                                onSelectBot = gameViewModel::selectBot,
                                onSelectSide = gameViewModel::selectSide,
                                onSelectTimeControl = gameViewModel::selectTimeControl,
                                onStartGame = gameViewModel::startGame,
                                onUserMove = gameViewModel::onUserMove,
                                onFlipBoard = gameViewModel::flipBoard,
                                onHint = gameViewModel::requestHint,
                                onTakeback = gameViewModel::takebackMove,
                                onDraw = gameViewModel::agreeDraw,
                                onResign = gameViewModel::resignGame,
                                onNewGameLobby = gameViewModel::openNewGameLobby,
                                onSelectViewingPly = gameViewModel::selectViewingPly,
                                onDismissGameOver = gameViewModel::dismissGameOverDialog,
                                onReviewCurrentGame = {
                                    val whiteName = if (gameUiState.selectedMode == GameMode.PASS_AND_PLAY) {
                                        "White"
                                    } else if (gameUiState.playerColor == PieceColor.WHITE) {
                                        "You"
                                    } else {
                                        gameUiState.selectedBot.name
                                    }
                                    val blackName = if (gameUiState.selectedMode == GameMode.PASS_AND_PLAY) {
                                        "Black"
                                    } else if (gameUiState.playerColor == PieceColor.BLACK) {
                                        "You"
                                    } else {
                                        gameUiState.selectedBot.name
                                    }
                                    analysisViewModel.loadFromBoardState(
                                        state = gameUiState.boardState,
                                        whiteName = whiteName,
                                        blackName = blackName,
                                        savedGameId = gameUiState.lastSavedGameId,
                                        autoRunReview = true
                                    )
                                    currentTab = MainTab.ANALYSIS
                                },
                                onOpenSettings = { showSettingsSheet = true }
                            )

                            MainTab.PUZZLES -> PuzzlesScreen(
                                uiState = puzzleUiState,
                                settings = settings,
                                solvedPuzzleIds = solvedPuzzleIds,
                                onSelectTabMode = puzzleViewModel::selectTabMode,
                                onPuzzleMove = puzzleViewModel::onPuzzleMove,
                                onHint = puzzleViewModel::requestHint,
                                onRetry = puzzleViewModel::retryCurrentPuzzle,
                                onNextPuzzle = puzzleViewModel::nextRatedPuzzle,
                                onSelectRushVariant = puzzleViewModel::selectRushVariant,
                                onStartRush = puzzleViewModel::startPuzzleRush,
                                onStopRush = { puzzleViewModel.stopRush(saveScore = true) },
                                onOpenSettings = { showSettingsSheet = true }
                            )

                            MainTab.ANALYSIS -> AnalysisScreen(
                                uiState = analysisUiState,
                                settings = settings,
                                onPlayMove = analysisViewModel::onPlayMoveOnBoard,
                                onSelectViewingPly = analysisViewModel::selectViewingPly,
                                onFlipBoard = analysisViewModel::flipBoard,
                                onToggleArrow = analysisViewModel::toggleBestMoveArrow,
                                onResetBoard = analysisViewModel::resetBoard,
                                onRunReview = analysisViewModel::runFullGameReview,
                                onImportFen = analysisViewModel::importFen,
                                onImportPgn = analysisViewModel::importPgn,
                                onExportFen = analysisViewModel::exportCurrentFen,
                                onExportPgn = analysisViewModel::exportCurrentPgn,
                                onOpenSettings = { showSettingsSheet = true }
                            )

                            MainTab.STATS -> StatsScreen(
                                settings = settings,
                                allGames = allGames,
                                defeatedBotIds = defeatedBotIds,
                                solvedPuzzlesCount = solvedPuzzleIds.size,
                                onChallengeBot = { bot ->
                                    gameViewModel.startQuickBotGame(bot)
                                    currentTab = MainTab.PLAY
                                },
                                onOpenGameInAnalysis = { saved ->
                                    analysisViewModel.loadSavedGame(saved, autoRunReview = false)
                                    currentTab = MainTab.ANALYSIS
                                },
                                onDeleteGame = { id ->
                                    scope.launch { repository.deleteGame(id) }
                                },
                                onOpenSettings = { showSettingsSheet = true }
                            )
                        }
                    }
                }

                if (useNavigationRail) {
                    Scaffold(
                        containerColor = ChessBgPrimary,
                        contentWindowInsets = WindowInsets.safeDrawing
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            NavigationRail(
                                containerColor = ChessBgDeep,
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                MainTab.entries.forEach { tab ->
                                    val selected = tab == currentTab
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { currentTab = tab },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label
                                            )
                                        },
                                        label = { Text(tab.label) },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = ChessGreenPrimary,
                                            selectedTextColor = ChessGreenPrimary,
                                            indicatorColor = ChessSurfaceElevated,
                                            unselectedIconColor = ChessTextMuted,
                                            unselectedTextColor = ChessTextMuted
                                        ),
                                        modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                                    )
                                }
                            }
                            mainContent(Modifier.weight(1f).fillMaxHeight())
                        }
                    }
                } else {
                    Scaffold(
                        containerColor = ChessBgPrimary,
                        contentWindowInsets = WindowInsets.safeDrawing,
                        bottomBar = {
                            NavigationBar(
                                containerColor = ChessBgDeep,
                                tonalElevation = 0.dp
                            ) {
                                MainTab.entries.forEach { tab ->
                                    val selected = tab == currentTab
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { currentTab = tab },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label
                                            )
                                        },
                                        label = { Text(tab.label, maxLines = 1) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = ChessGreenPrimary,
                                            selectedTextColor = ChessGreenPrimary,
                                            indicatorColor = ChessSurfaceElevated,
                                            unselectedIconColor = ChessTextMuted,
                                            unselectedTextColor = ChessTextMuted
                                        ),
                                        modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        mainContent(Modifier.fillMaxSize().padding(innerPadding))
                    }
                }
            }

            if (showSettingsSheet) {
                SettingsBottomSheet(
                    settings = settings,
                    onDismiss = { showSettingsSheet = false },
                    onBoardThemeSelected = { theme -> scope.launch { repository.setBoardTheme(theme) } },
                    onPieceStyleSelected = { style -> scope.launch { repository.setPieceStyle(style) } },
                    onSoundToggled = { enabled -> scope.launch { repository.setSoundEnabled(enabled) } },
                    onCoordinatesToggled = { show -> scope.launch { repository.setShowCoordinates(show) } },
                    onLegalMovesToggled = { show -> scope.launch { repository.setShowLegalMoves(show) } },
                    onLiveEvalToggled = { show -> scope.launch { repository.setShowLiveEvalBar(show) } }
                )
            }
        }
    }
}
