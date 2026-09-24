package com.matchpoint.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.matchpoint.app.ui.theme.SmoothCorner
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PersonAddAlt
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matchpoint.app.R
import com.matchpoint.app.data.AppPreferences
import com.matchpoint.app.domain.MatchStatus
import com.matchpoint.app.repository.SessionRepository
import com.matchpoint.app.ui.theme.GhostButton
import com.matchpoint.app.ui.theme.GlassCard
import com.matchpoint.app.ui.theme.GoogleSans
import com.matchpoint.app.ui.theme.LocalCourtTheme
import com.matchpoint.app.ui.theme.PrimaryButton
import com.matchpoint.app.ui.theme.StatusPill
import com.matchpoint.app.ui.theme.Theme
import com.matchpoint.app.ui.theme.themeScore
import com.matchpoint.app.ui.theme.themeTitle
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.UUID

@Composable
fun HomeScreen(
    repository: SessionRepository,
    preferences: AppPreferences,
    onOpenLive: (UUID) -> Unit,
    onOpenSelectPlayers: (UUID, String) -> Unit,
    onOpenMatchDetail: (UUID) -> Unit,
    onOpenMatchSettings: (UUID) -> Unit,
    onOpenSessionRoster: (UUID) -> Unit
) {
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(repository, preferences))
    val state by viewModel.uiState.collectAsState()

    // After a match finishes there's no draft left; set up the next (empty) one right away
    // instead of asking the user to.
    val activeSessionId = state.activeSession?.id
    val needsNextMatch = activeSessionId != null && state.liveMatch == null && state.draftMatch == null
    LaunchedEffect(activeSessionId, needsNextMatch) {
        if (needsNextMatch) viewModel.ensureNextMatch()
    }

    var showStartSheet by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showEndDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    val session = state.activeSession
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.statusBars.only(WindowInsetsSides.Top),
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    containerColor = Theme.accent,
                    contentColor = Theme.accentText,
                    snackbarData = data
                )
            }
        }
    ) { snackbarPadding ->
    Column(modifier = Modifier.fillMaxSize().padding(snackbarPadding).padding(horizontal = 16.dp)) {
        if (session == null) {
            HomeHeader()
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    Image(
                        painter = painterResource(LocalCourtTheme.current.homeMabarIllustration),
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .fillMaxWidth()
                            .heightIn(max = 305.dp)
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.height(20.dp))
                    PrimaryButton(text = "Create new mabar", onClick = { showStartSheet = true })
                    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item { HomeHeader() }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(SmoothCorner(Theme.cornerRadius))
                            .background(Theme.cardFill)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            SpinningTennisBall()

                            Column(modifier = Modifier.weight(1f)) {
                                Text(session.name, style = themeTitle(20.sp), color = Theme.textPrimary)
                                Text(
                                    DateFormat.getDateInstance(DateFormat.FULL).format(Date(session.date)),
                                    fontFamily = GoogleSans,
                                    fontSize = 14.sp,
                                    color = Theme.textSecondary
                                )
                                if (session.startTime != null && session.endTime != null) {
                                    val timeFmt = remember { java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()) }
                                    Text(
                                        "${timeFmt.format(Date(session.startTime))} – ${timeFmt.format(Date(session.endTime))}",
                                        fontFamily = GoogleSans, fontSize = 13.sp, color = Theme.textSecondary
                                    )
                                }
                            }

                            Box {
                                IconButton(
                                    onClick = { menuExpanded = true },
                                    modifier = Modifier.size(36.dp).clip(CircleShape)
                                ) {
                                    Icon(Icons.Default.MoreHoriz, contentDescription = "Menu", tint = Theme.textSecondary, modifier = Modifier.size(18.dp))
                                }
                                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                    DropdownMenuItem(text = { Text("Edit Mabar Name") }, onClick = { menuExpanded = false; showRenameDialog = true })
                                    DropdownMenuItem(text = { Text("End Session") }, onClick = { menuExpanded = false; showEndDialog = true })
                                    DropdownMenuItem(text = { Text("Manage Players") }, onClick = { menuExpanded = false; onOpenSessionRoster(session.id) })
                                    val settingsTarget = state.liveMatch ?: state.draftMatch
                                    if (settingsTarget != null) {
                                        DropdownMenuItem(text = { Text("Match Settings") }, onClick = { menuExpanded = false; onOpenMatchSettings(settingsTarget.match.id) })
                                    }
                                }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatTile(icon = Icons.Default.Groups, label = "Mode", value = if (session.preferredMatchType == com.matchpoint.app.domain.MatchType.DOUBLES) "Doubles" else "Singles", modifier = Modifier.weight(1f))
                            StatTile(icon = Icons.Default.Flag, label = "Format", value = "First to ${session.preferredFirstToGames}", modifier = Modifier.weight(1f))
                            val defaultDeuce by preferences.defaultDeuceAdvantageEnabled.collectAsState()
                            StatTile(
                                icon = Icons.Default.SwapHoriz,
                                label = "Deuce",
                                value = if ((state.liveMatch ?: state.draftMatch)?.match?.deuceAdvantageEnabled ?: defaultDeuce) "On" else "Off",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        val matchNumber = { id: UUID -> (state.matches.indexOfFirst { it.match.id == id }.takeIf { it >= 0 } ?: state.matches.size) + 1 }
                        when {
                            state.liveMatch != null -> {
                                CurrentMatchSection(state.liveMatch!!, matchNumber(state.liveMatch!!.match.id), repository, onOpenLive)
                            }
                            state.draftMatch != null -> {
                                NextMatchSection(state.draftMatch!!, matchNumber(state.draftMatch!!.match.id), onOpenSelectPlayers, onOpenLive, viewModel)
                            }
                            else -> {}
                        }
                    }
                }

                item { ScoreboardSection(state, onOpenMatchDetail, onOpenLive) }
            }
        }
    }

    if (showStartSheet) {
        StartSessionSheet(
            viewModel = viewModel,
            onDismiss = { showStartSheet = false },
            onCreated = {
                scope.launch { snackbarHostState.showSnackbar("Mabar created — let's hit the court!") }
            }
        )
    }

    if (showRenameDialog && session != null) {
        com.matchpoint.app.ui.common.EditSessionDetailsDialog(
            initialName = session.name,
            initialDate = session.date,
            initialStartTime = session.startTime,
            initialEndTime = session.endTime,
            onDismiss = { showRenameDialog = false },
            onSave = { name, date, start, end ->
                viewModel.updateSessionDetails(name, date, start, end)
                showRenameDialog = false
            }
        )
    }

    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            title = { Text("End this mabar?") },
            text = { Text("Any proposed-but-not-started match will be discarded. History and leaderboard stay.") },
            confirmButton = { TextButton(onClick = { viewModel.endSession(); showEndDialog = false }) { Text("End") } },
            dismissButton = { TextButton(onClick = { showEndDialog = false }) { Text("Cancel") } }
        )
    }
    }
}

@Composable
private fun HomeHeader() {
    Column(modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)) {
        Image(
            painter = painterResource(LocalCourtTheme.current.logoImage),
            contentDescription = "Match Point",
            modifier = Modifier.heightIn(max = 46.dp).padding(bottom = 8.dp)
        )
        Text(greetingTitle(), style = themeTitle(30.sp), color = Theme.textPrimary)
        Text("Time to hit the court.", fontFamily = GoogleSans, fontSize = 14.sp, color = Theme.textSecondary, modifier = Modifier.padding(top = 2.dp))
    }
}

/** "Good morning/afternoon/evening, Dayu." — same hour thresholds as the iOS app's HomeView. */
private fun greetingTitle(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val salutation = when (hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
    return "$salutation, Dayu."
}

@Composable
private fun StatTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(SmoothCorner(12.dp))
            .background(Theme.cardFill)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Theme.accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Theme.accent, modifier = Modifier.size(15.dp))
        }
        Text(value, fontFamily = GoogleSans, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Theme.textPrimary, modifier = Modifier.padding(top = 6.dp), maxLines = 1)
        Text(label, fontFamily = GoogleSans, fontSize = 11.sp, color = Theme.textSecondary, maxLines = 1)
    }
}

/** Continuously spinning 3D tennis ball in an accent circle — matches the iOS session card header. */
@Composable
private fun SpinningTennisBall() {
    val transition = rememberInfiniteTransition(label = "ball")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(6000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "angle"
    )
    Box(
        modifier = Modifier.size(50.dp).clip(CircleShape).background(Theme.accent),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(LocalCourtTheme.current.spinningTennisBall),
            contentDescription = null,
            modifier = Modifier.size(32.dp).rotate(angle)
        )
    }
}

@Composable
private fun CurrentMatchSection(match: MatchUi, number: Int, repository: SessionRepository, onOpenLive: (UUID) -> Unit) {
    val score by androidx.compose.runtime.produceState<Pair<Int, Int>?>(initialValue = null, match.match.id) {
        repository.observeGamesScore(match.match).collect { value = it }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("MATCH #$number", fontFamily = GoogleSans, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Theme.textSecondary)
            StatusPill(text = "LIVE", filled = true)
        }
        MatchLineup(sideANames = match.sideANames, sideBNames = match.sideBNames, scoreA = score?.first, scoreB = score?.second)
        PrimaryButton(text = "Continue Scoring", onClick = { onOpenLive(match.match.id) })
    }
}

@Composable
private fun NextMatchSection(
    match: MatchUi,
    number: Int,
    onOpenSelectPlayers: (UUID, String) -> Unit,
    onOpenLive: (UUID) -> Unit,
    viewModel: HomeViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("MATCH #$number", fontFamily = GoogleSans, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Theme.textSecondary)
            if (!match.hasAnyPlayers) {
                StatusPill(text = "SET UP")
            } else {
                Text(
                    "Edit",
                    fontFamily = GoogleSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = Theme.accent,
                    modifier = Modifier.clickable { onOpenSelectPlayers(match.match.id, "ANY") }
                )
            }
        }
        MatchLineup(
            sideANames = match.sideANames,
            sideBNames = match.sideBNames,
            onSelectSideA = { onOpenSelectPlayers(match.match.id, "A") },
            onSelectSideB = { onOpenSelectPlayers(match.match.id, "B") },
            modifier = Modifier.padding(bottom = 4.dp)
        )
        PrimaryButton(
            text = "Start mabar now",
            enabled = match.isReadyToStart,
            onClick = { viewModel.startMatch(match.match, onStarted = onOpenLive) }
        )
    }
}

/** Side-by-side TEAM A / VS(or score) / TEAM B card — matches iOS's `matchLineup`. */
@Composable
private fun MatchLineup(
    sideANames: List<String>,
    sideBNames: List<String>,
    scoreA: Int? = null,
    scoreB: Int? = null,
    onSelectSideA: (() -> Unit)? = null,
    onSelectSideB: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(SmoothCorner(14.dp))
            .background(Theme.backgroundTop.copy(alpha = 0.45f))
            .border(1.dp, Theme.cardBorder, SmoothCorner(14.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TeamLineup(title = "TEAM A", names = sideANames, trailing = false, modifier = Modifier.weight(1f), onTap = onSelectSideA)
        Box {
            if (scoreA != null && scoreB != null) {
                Row(
                    modifier = Modifier
                        .clip(SmoothCorner(50))
                        .background(Theme.backgroundTop.copy(alpha = 0.6f))
                        .border(1.dp, Theme.cardBorder, SmoothCorner(50))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text("$scoreA", style = themeTitle(19.sp), color = if (scoreA >= scoreB) Theme.accent else Theme.textSecondary)
                    Text("–", style = themeTitle(19.sp), color = Theme.textSecondary)
                    Text("$scoreB", style = themeTitle(19.sp), color = if (scoreB >= scoreA) Theme.accent else Theme.textSecondary)
                }
            } else {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(Theme.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text("VS", fontFamily = GoogleSans, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Theme.accentText)
                }
            }
        }
        TeamLineup(title = "TEAM B", names = sideBNames, trailing = true, modifier = Modifier.weight(1f), onTap = onSelectSideB)
    }
}

@Composable
private fun TeamLineup(title: String, names: List<String>, trailing: Boolean, modifier: Modifier = Modifier, onTap: (() -> Unit)? = null) {
    val alignment = if (trailing) Alignment.End else Alignment.Start
    Column(
        modifier = modifier
            .let { if (onTap != null) it.clickable(onClick = onTap) else it },
        horizontalAlignment = alignment
    ) {
        Text(
            title, fontFamily = GoogleSans, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Theme.textSecondary,
            textAlign = if (trailing) TextAlign.End else TextAlign.Start
        )
        if (names.isNotEmpty()) {
            Text(
                names.joinToString("\n"),
                fontFamily = GoogleSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Theme.textPrimary,
                maxLines = 2,
                textAlign = if (trailing) TextAlign.End else TextAlign.Start,
                modifier = Modifier.padding(top = 4.dp)
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                if (!trailing) {
                    Icon(Icons.Default.PersonAddAlt, contentDescription = null, tint = if (onTap != null) Theme.accent else Theme.textSecondary, modifier = Modifier.size(14.dp))
                }
                Text(
                    "Select Player",
                    fontFamily = GoogleSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = if (onTap != null) Theme.accent else Theme.textSecondary
                )
                if (trailing) {
                    Icon(Icons.Default.PersonAddAlt, contentDescription = null, tint = if (onTap != null) Theme.accent else Theme.textSecondary, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
private fun ScoreboardSection(state: HomeUiState, onOpenMatchDetail: (UUID) -> Unit, onOpenLive: (UUID) -> Unit) {
    var tab by remember { mutableStateOf(0) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("SCOREBOARD", fontFamily = GoogleSans, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Theme.textSecondary)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(SmoothCorner(10.dp))
                .background(Theme.cardFill)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            listOf("Standings", "Matches").forEachIndexed { index, label ->
                val selected = tab == index
                Text(
                    label,
                    fontFamily = GoogleSans,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 13.sp,
                    color = if (selected) Theme.textPrimary else Theme.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(SmoothCorner(8.dp))
                        .background(if (selected) Theme.textPrimary.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable { tab = index }
                        .padding(vertical = 7.dp)
                )
            }
        }
        if (tab == 0) {
            if (state.standings.isEmpty()) {
                ScoreboardEmptyState("No standings yet in this mabar.")
            } else {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    state.standings.forEachIndexed { index, sp ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("#${index + 1} ${sp.name}", fontFamily = GoogleSans, fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp, color = Theme.textPrimary)
                            Text("${sp.sessionPlayer.wins}-${sp.sessionPlayer.losses} · ${sp.sessionPlayer.points}pt", fontFamily = GoogleSans, fontSize = 12.sp, color = if (index == 0) Theme.accent else Theme.textSecondary)
                        }
                    }
                }
            }
        } else {
            val ordered = state.matches.filter { it.match.status == MatchStatus.LIVE || it.match.status == MatchStatus.COMPLETED }
            if (ordered.isEmpty()) {
                ScoreboardEmptyState("No matches yet in this mabar.")
            } else {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    ordered.forEachIndexed { i, m ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (m.match.status == MatchStatus.LIVE) onOpenLive(m.match.id) else onOpenMatchDetail(m.match.id)
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "M${i + 1}  ${m.sideANames.joinToString("&")} vs ${m.sideBNames.joinToString("&")}",
                                fontFamily = GoogleSans,
                                fontSize = 13.sp,
                                color = Theme.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            if (m.match.status == MatchStatus.LIVE) {
                                StatusPill(text = "LIVE", filled = true)
                            } else {
                                Text("${m.match.finalScoreA}-${m.match.finalScoreB}", fontFamily = GoogleSans, fontSize = 13.sp, color = Theme.textSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreboardEmptyState(message: String) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(LocalCourtTheme.current.scoreboardEmptyIllustration),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp)
        )
        Text(
            message,
            fontFamily = GoogleSans,
            fontSize = 13.sp,
            color = Theme.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}
