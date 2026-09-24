package com.matchpoint.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.matchpoint.app.R
import com.matchpoint.app.backup.BackupService
import com.matchpoint.app.data.AppPreferences
import com.matchpoint.app.repository.SessionRepository
import com.matchpoint.app.ui.home.HomeScreen
import com.matchpoint.app.ui.home.MatchSettingsScreen
import com.matchpoint.app.ui.home.SessionRosterScreen
import com.matchpoint.app.ui.match.LiveMatchScreen
import com.matchpoint.app.ui.match.MatchCompleteScreen
import com.matchpoint.app.ui.match.SelectPlayersScreen
import com.matchpoint.app.ui.matches.MatchDetailScreen
import com.matchpoint.app.ui.matches.MatchesListScreen
import com.matchpoint.app.ui.matches.RecordMatchScreen
import com.matchpoint.app.ui.matches.SessionHistoryDetailScreen
import com.matchpoint.app.ui.players.PlayerProfileScreen
import com.matchpoint.app.ui.players.PlayersListScreen
import com.matchpoint.app.ui.settings.SettingsScreen
import com.matchpoint.app.ui.theme.LocalCourtTheme
import com.matchpoint.app.wear.WearEngineManager
import java.util.UUID

private fun tabs(theme: com.matchpoint.app.ui.theme.CourtTheme) = listOf(
    BottomNavItem(Routes.HOME, "Home", theme.tabHomeActive, theme.tabHomeInactive),
    BottomNavItem(Routes.MATCHES, "Matches", theme.tabMatchesActive, theme.tabMatchesInactive),
    BottomNavItem(Routes.PLAYERS, "Players", theme.tabPlayersActive, theme.tabPlayersInactive),
    BottomNavItem(Routes.SETTINGS, "Settings", theme.tabSettingsActive, theme.tabSettingsInactive)
)

@Composable
fun RootNavGraph(
    repository: SessionRepository,
    preferences: AppPreferences,
    backupService: BackupService,
    wearEngineManager: WearEngineManager
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in Routes.bottomTabs
    val tabs = tabs(LocalCourtTheme.current)

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    items = tabs,
                    selectedRoute = currentRoute,
                    onSelect = { route ->
                        navController.navigate(route) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(bottom = if (showBottomBar) padding.calculateBottomPadding() else 0.dp)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    repository = repository,
                    preferences = preferences,
                    onOpenLive = { navController.navigate(Routes.liveMatch(it)) },
                    onOpenSelectPlayers = { matchId, side -> navController.navigate(Routes.selectPlayers(matchId, side)) },
                    onOpenMatchDetail = { navController.navigate(Routes.matchDetail(it)) },
                    onOpenMatchSettings = { navController.navigate(Routes.matchSettings(it)) },
                    onOpenSessionRoster = { navController.navigate(Routes.sessionRoster(it)) }
                )
            }
            composable(
                route = Routes.MATCH_SETTINGS,
                arguments = listOf(navArgument("matchId") { type = NavType.StringType })
            ) { entry ->
                val matchId = UUID.fromString(entry.arguments?.getString("matchId"))
                MatchSettingsScreen(repository = repository, matchId = matchId, onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.SESSION_ROSTER,
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
            ) { entry ->
                val sessionId = UUID.fromString(entry.arguments?.getString("sessionId"))
                SessionRosterScreen(repository = repository, sessionId = sessionId, onBack = { navController.popBackStack() })
            }
            composable(Routes.MATCHES) {
                MatchesListScreen(
                    repository = repository,
                    onOpenSessionHistory = { navController.navigate(Routes.sessionHistory(it)) }
                )
            }
            composable(Routes.PLAYERS) {
                PlayersListScreen(
                    repository = repository,
                    onOpenProfile = { navController.navigate(Routes.playerProfile(it)) }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(preferences = preferences, backupService = backupService, wearEngineManager = wearEngineManager)
            }
            composable(
                route = Routes.LIVE_MATCH,
                arguments = listOf(navArgument("matchId") { type = NavType.StringType })
            ) { entry ->
                val matchId = UUID.fromString(entry.arguments?.getString("matchId"))
                LiveMatchScreen(
                    repository = repository,
                    matchId = matchId,
                    wearEngineManager = wearEngineManager,
                    onBack = { navController.popBackStack() },
                    onFinished = {
                        navController.navigate(Routes.matchComplete(matchId)) {
                            popUpTo(Routes.HOME)
                        }
                    },
                    onAbandoned = {
                        navController.popBackStack(Routes.HOME, inclusive = false)
                    }
                )
            }
            composable(
                route = Routes.MATCH_COMPLETE,
                arguments = listOf(navArgument("matchId") { type = NavType.StringType })
            ) { entry ->
                val matchId = UUID.fromString(entry.arguments?.getString("matchId"))
                MatchCompleteScreen(
                    repository = repository,
                    matchId = matchId,
                    onContinue = { navController.popBackStack(Routes.HOME, inclusive = false) }
                )
            }
            composable(
                route = Routes.SELECT_PLAYERS,
                arguments = listOf(
                    navArgument("matchId") { type = NavType.StringType },
                    navArgument("side") { type = NavType.StringType }
                )
            ) { entry ->
                val matchId = UUID.fromString(entry.arguments?.getString("matchId"))
                val side = entry.arguments?.getString("side") ?: "ANY"
                SelectPlayersScreen(
                    repository = repository,
                    matchId = matchId,
                    initialSide = side,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Routes.SESSION_HISTORY,
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
            ) { entry ->
                val sessionId = UUID.fromString(entry.arguments?.getString("sessionId"))
                SessionHistoryDetailScreen(
                    repository = repository,
                    sessionId = sessionId,
                    onOpenMatchDetail = { navController.navigate(Routes.matchDetail(it)) },
                    onAddMatch = { navController.navigate(Routes.recordMatch(sessionId)) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Routes.MATCH_DETAIL,
                arguments = listOf(navArgument("matchId") { type = NavType.StringType })
            ) { entry ->
                val matchId = UUID.fromString(entry.arguments?.getString("matchId"))
                MatchDetailScreen(
                    repository = repository,
                    matchId = matchId,
                    onEdit = { sessionId -> navController.navigate(Routes.recordMatch(sessionId, matchId)) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Routes.RECORD_MATCH,
                arguments = listOf(
                    navArgument("sessionId") { type = NavType.StringType },
                    navArgument("matchId") { type = NavType.StringType }
                )
            ) { entry ->
                val sessionId = UUID.fromString(entry.arguments?.getString("sessionId"))
                val matchIdArg = entry.arguments?.getString("matchId")
                val existingMatchId = if (matchIdArg == Routes.RECORD_MATCH_NEW) null else UUID.fromString(matchIdArg)
                RecordMatchScreen(
                    repository = repository,
                    sessionId = sessionId,
                    existingMatchId = existingMatchId,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Routes.PLAYER_PROFILE,
                arguments = listOf(navArgument("playerId") { type = NavType.StringType })
            ) { entry ->
                val playerId = UUID.fromString(entry.arguments?.getString("playerId"))
                PlayerProfileScreen(
                    repository = repository,
                    playerId = playerId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
