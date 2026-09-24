package com.matchpoint.app.ui.match

import androidx.compose.foundation.Image
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.matchpoint.app.ui.theme.SmoothCorner
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matchpoint.app.R
import com.matchpoint.app.audio.SoundEffects
import com.matchpoint.app.domain.MatchOutcome
import com.matchpoint.app.domain.PointDisplay
import com.matchpoint.app.domain.Side
import com.matchpoint.app.repository.SessionRepository
import com.matchpoint.app.ui.theme.DangerTextButton
import com.matchpoint.app.ui.theme.GoogleSans
import com.matchpoint.app.ui.theme.LocalCourtTheme
import com.matchpoint.app.ui.theme.PrimaryButton
import com.matchpoint.app.ui.theme.Theme
import com.matchpoint.app.ui.theme.themeScore
import com.matchpoint.app.ui.theme.themeTitle
import com.matchpoint.app.wear.RemoteCommandHandler
import com.matchpoint.app.wear.WatchEvent
import com.matchpoint.app.wear.WearConnectionState
import com.matchpoint.app.wear.WearEngineManager
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LiveMatchScreen(
    repository: SessionRepository,
    matchId: UUID,
    wearEngineManager: WearEngineManager,
    onBack: () -> Unit,
    onFinished: () -> Unit,
    onAbandoned: () -> Unit
) {
    val viewModel: LiveMatchViewModel = viewModel(factory = LiveMatchViewModel.Factory(repository, matchId))
    val state by viewModel.uiState.collectAsState()
    var serverMenuOpen by remember { mutableStateOf(false) }
    var showAbandonDialog by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }
    // Only shown once the MIXIO remote's left/right/heart has been used, so touch-only users
    // don't see an always-highlighted first chip.
    var showSfxSelection by remember { mutableStateOf(false) }

    // Keep the phone screen on while scoring. The phone is usually in a bag during play, and a
    // sleeping phone stops receiving commands from the Huawei Watch remote.
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    // Bind hardware volume keys (incl. Bluetooth "camera shutter" remotes) to scoring
    // only while this screen is on screen; MainActivity falls back to normal volume otherwise.
    DisposableEffect(matchId) {
        VolumeKeyBridge.onVolumeUp = { if (!showFinishDialog && !showAbandonDialog) viewModel.recordPoint(Side.A) }
        VolumeKeyBridge.onVolumeDown = { if (!showFinishDialog && !showAbandonDialog) viewModel.recordPoint(Side.B) }
        onDispose { VolumeKeyBridge.clear() }
    }

    // MIXIO Bluetooth remote (its Photo 1 arrives as a volume key, handled here rather than
    // by the generic volume mapping above). Same lifecycle as the volume keys; every action calls into the existing ViewModel. While a confirmation
    // dialog is up, Heart confirms it, Up cancels it, and scoring buttons are ignored so a
    // stray press can't change the score behind it.
    val currentOnFinishedForRemote by rememberUpdatedState(onFinished)
    val currentOnAbandonedForRemote by rememberUpdatedState(onAbandoned)
    DisposableEffect(matchId) {
        val dialogOpen = { showFinishDialog || showAbandonDialog }
        MixioRemoteBridge.onPhoto1 = { if (!dialogOpen()) viewModel.recordPoint(Side.A) }
        MixioRemoteBridge.onPhoto2 = { if (!dialogOpen()) viewModel.recordPoint(Side.B) }
        MixioRemoteBridge.onUp = {
            when {
                showFinishDialog -> showFinishDialog = false
                showAbandonDialog -> showAbandonDialog = false
                else -> viewModel.undoLastPoint()
            }
        }
        MixioRemoteBridge.onDown = { if (!dialogOpen()) showAbandonDialog = true }
        MixioRemoteBridge.onLeft = {
            if (!dialogOpen()) {
                showSfxSelection = true
                viewModel.selectPreviousSoundFx()
            }
        }
        MixioRemoteBridge.onRight = {
            if (!dialogOpen()) {
                showSfxSelection = true
                viewModel.selectNextSoundFx()
            }
        }
        MixioRemoteBridge.onHeart = {
            when {
                showFinishDialog -> {
                    showFinishDialog = false
                    viewModel.finish(currentOnFinishedForRemote)
                }
                showAbandonDialog -> {
                    showAbandonDialog = false
                    viewModel.abandon(currentOnAbandonedForRemote)
                }
                // Once the score is final Heart stops playing sound FX and instead opens the
                // Finish dialog (press Heart again to confirm) — the remote can't long-press.
                viewModel.uiState.value?.score?.outcome is MatchOutcome.Completed -> showFinishDialog = true
                else -> {
                    showSfxSelection = true
                    viewModel.playSelectedSoundFx()
                }
            }
        }
        onDispose { MixioRemoteBridge.clear() }
    }

    // Same lifecycle as VolumeKeyBridge above: only this screen acts on watch remote
    // commands, and only while it's on screen. Every callback calls into the existing
    // ViewModel; the watch only ever receives state back, it never computes any.
    // OK isn't bound here — the watch sends SOUND_FX_CONFIRM on this screen.
    val connection by wearEngineManager.connectionState.collectAsState()
    val soundFxIndex by viewModel.selectedSoundFxIndex.collectAsState()
    val currentOnFinished by rememberUpdatedState(onFinished)
    var finishRequested by remember { mutableStateOf(false) }

    DisposableEffect(matchId) {
        var completionSent = false
        wearEngineManager.connect()
        RemoteCommandHandler.onPointA = { viewModel.recordPoint(Side.A) }
        RemoteCommandHandler.onPointB = { viewModel.recordPoint(Side.B) }
        RemoteCommandHandler.onPrevious = { viewModel.undoLastPoint() }
        RemoteCommandHandler.onSoundFxPrevious = { viewModel.selectPreviousSoundFx() }
        RemoteCommandHandler.onSoundFxNext = { viewModel.selectNextSoundFx() }
        RemoteCommandHandler.onSoundFxConfirm = { viewModel.playSelectedSoundFx() }
        // The watch just opened and wants everything: resend the full current state.
        RemoteCommandHandler.onSync = {
            wearEngineManager.sendToWatch(WatchEvent.MatchStatus(live = true).encode())
            viewModel.uiState.value?.score?.let { score ->
                wearEngineManager.sendToWatch(
                    WatchEvent.ScoreUpdate(
                        score.gamesA, score.gamesB, score.gameDisplay.pointsA, score.gameDisplay.pointsB
                    ).encode()
                )
            }
            val fx = viewModel.selectedSoundFxIndex.value
            wearEngineManager.sendToWatch(WatchEvent.SoundFxUpdate(fx, SoundEffects[fx].title).encode())
        }
        // Android decides whether the match may finish (same rule as the on-screen button);
        // the watch's long press is only a request.
        RemoteCommandHandler.onFinishMatch = {
            val ui = viewModel.uiState.value
            val outcome = ui?.score?.outcome
            if (ui != null && outcome is MatchOutcome.Completed) {
                finishRequested = true
                viewModel.finish {
                    completionSent = true
                    wearEngineManager.sendToWatch(
                        WatchEvent.MatchComplete(ui.score.gamesA, ui.score.gamesB, outcome.winner).encode()
                    )
                    currentOnFinished()
                }
            } else {
                wearEngineManager.sendToWatch(WatchEvent.Error("FINISH_NOT_ALLOWED").encode())
            }
        }
        onDispose {
            RemoteCommandHandler.clear()
            if (!completionSent) wearEngineManager.sendToWatch(WatchEvent.MatchStatus(live = false).encode())
        }
    }

    // Push phone state to the watch. Keyed on `connection` so everything is re-sent when the
    // watch (re)connects. Skipped once finish was requested: completion may reset the
    // derived score, and the watch should keep showing the final result.
    LaunchedEffect(connection) {
        if (connection == WearConnectionState.CONNECTED) {
            wearEngineManager.sendToWatch(WatchEvent.MatchStatus(live = true).encode())
        }
    }
    LaunchedEffect(connection, state?.score) {
        val score = state?.score
        if (connection == WearConnectionState.CONNECTED && score != null && !finishRequested) {
            wearEngineManager.sendToWatch(
                WatchEvent.ScoreUpdate(
                    score.gamesA, score.gamesB, score.gameDisplay.pointsA, score.gameDisplay.pointsB
                ).encode()
            )
        }
    }
    LaunchedEffect(connection, soundFxIndex) {
        if (connection == WearConnectionState.CONNECTED) {
            wearEngineManager.sendToWatch(
                WatchEvent.SoundFxUpdate(soundFxIndex, SoundEffects[soundFxIndex].title).encode()
            )
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Live Scoring", style = themeTitle(18.sp), color = Theme.textPrimary) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Theme.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        val ui = state
        if (ui == null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.Center) {
                Text("Loading...", color = Theme.textSecondary, fontFamily = GoogleSans, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
            return@Scaffold
        }

        val isFinishable = ui.score.outcome is MatchOutcome.Completed
        val serverName = ui.allParticipants.firstOrNull { it.first == ui.score.currentServerPlayerId }?.second ?: "—"

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Box(modifier = Modifier.padding(vertical = 12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(SmoothCorner(8.dp))
                            .border(1.dp, Theme.cardBorder, SmoothCorner(8.dp))
                            .clickable { serverMenuOpen = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Image(
                            painter = painterResource(LocalCourtTheme.current.spinningTennisBall),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "Serving: $serverName",
                            fontFamily = GoogleSans,
                            fontSize = 13.sp,
                            color = Theme.textPrimary,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Theme.textPrimary, modifier = Modifier.height(16.dp))
                    }
                    DropdownMenu(expanded = serverMenuOpen, onDismissRequest = { serverMenuOpen = false }) {
                        ui.allParticipants.forEach { (id, name) ->
                            DropdownMenuItem(text = { Text(name) }, onClick = { viewModel.setServer(id); serverMenuOpen = false })
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().height(340.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ScoreHalf(
                        names = ui.sideANames,
                        games = ui.score.gamesA,
                        point = ui.score.gameDisplay.pointsA,
                        isLit = ui.lastScoredSide == Side.A,
                        tapEnabled = !isFinishable,
                        // Undo always removes the single last event regardless of which
                        // side's button is tapped — both stay clickable from either side.
                        undoEnabled = ui.hasHistory,
                        onTap = { viewModel.recordPoint(Side.A) },
                        onUndo = { viewModel.undoLastPoint() },
                        modifier = Modifier.weight(1f)
                    )
                    ScoreHalf(
                        names = ui.sideBNames,
                        games = ui.score.gamesB,
                        point = ui.score.gameDisplay.pointsB,
                        isLit = ui.lastScoredSide == Side.B,
                        tapEnabled = !isFinishable,
                        undoEnabled = ui.hasHistory,
                        onTap = { viewModel.recordPoint(Side.B) },
                        onUndo = { viewModel.undoLastPoint() },
                        modifier = Modifier.weight(1f)
                    )
                }

                SoundEffectRow(
                    selectedIndex = if (showSfxSelection) soundFxIndex else null,
                    onPlay = { viewModel.playSoundEffect(it) }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Theme.backgroundBottom)
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp, bottom = 8.dp)
            ) {
                PrimaryButton(text = "Finish mabar", enabled = isFinishable, onClick = { showFinishDialog = true })
                DangerTextButton(text = "Abandon", onClick = { showAbandonDialog = true })
            }
        }
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("Finish mabar?") },
            text = {
                MixioDialogInterceptor()
                Text("Confirm the final score and winner before completing this match.")
            },
            confirmButton = { TextButton(onClick = { showFinishDialog = false; viewModel.finish(onFinished) }) { Text("Finish mabar") } },
            dismissButton = { TextButton(onClick = { showFinishDialog = false }) { Text("Cancel") } }
        )
    }

    if (showAbandonDialog) {
        AlertDialog(
            onDismissRequest = { showAbandonDialog = false },
            title = { Text("Abandon this match?") },
            text = {
                MixioDialogInterceptor()
                Text("This match won't count toward the leaderboard, wins, or losses. You'll be able to pick players and set it up again from Home.")
            },
            confirmButton = { TextButton(onClick = { showAbandonDialog = false; viewModel.abandon(onAbandoned) }) { Text("Abandon Match", color = Theme.danger) } },
            dismissButton = { TextButton(onClick = { showAbandonDialog = false }) { Text("Keep Playing") } }
        )
    }
}

@Composable
private fun ScoreHalf(
    names: List<String>,
    games: Int,
    point: PointDisplay,
    isLit: Boolean,
    tapEnabled: Boolean,
    undoEnabled: Boolean,
    onTap: () -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(SmoothCorner(Theme.cornerRadius))
            .background(if (isLit) Theme.accent.copy(alpha = 0.1f) else Theme.cardFill)
            .border(if (isLit) 2.dp else 1.dp, if (isLit) Theme.accent else Theme.cardBorder, SmoothCorner(Theme.cornerRadius))
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clickable(enabled = tapEnabled, onClick = onTap),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(names.joinToString("\n"), style = themeTitle(16.sp), color = Theme.textPrimary, textAlign = TextAlign.Center)
                Text("$games", style = themeTitle(22.sp), color = Theme.textSecondary, modifier = Modifier.padding(top = 8.dp))
                Text(pointLabel(point), style = themeScore(56.sp), color = Theme.accent, modifier = Modifier.padding(top = 8.dp))
            }
        }
        HorizontalDivider(color = Theme.cardBorder)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = undoEnabled, onClick = onUndo)
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Undo,
                contentDescription = "Undo",
                tint = if (undoEnabled) Theme.textSecondary else Theme.textSecondary.copy(alpha = 0.35f)
            )
        }
    }
}

/** How many chip rows are visible before "Show all" is needed — keeps the score court, not
 * this list, the dominant element on screen, matching iOS's `collapsedSoundEffectLines`. */
private const val CollapsedSoundEffectCount = 6

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SoundEffectRow(selectedIndex: Int?, onPlay: (String) -> Unit) {
    var showAll by remember { mutableStateOf(false) }
    val visible = if (showAll) SoundEffects else SoundEffects.take(CollapsedSoundEffectCount)

    // The remote can step the selection past the collapsed rows; reveal it so it's visible.
    LaunchedEffect(selectedIndex) {
        if (selectedIndex != null && selectedIndex >= CollapsedSoundEffectCount) showAll = true
    }

    Column(modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)) {
        Text(
            "SOUND FX",
            fontFamily = GoogleSans,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = Theme.textSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            visible.forEach { effect ->
                SoundReactionChip(
                    title = effect.title,
                    selected = selectedIndex != null && SoundEffects[selectedIndex] == effect,
                    onClick = { onPlay(effect.filename) }
                )
            }
        }
        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .clickable { showAll = !showAll },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(if (showAll) "Hide" else "Show all", fontFamily = GoogleSans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Theme.accent)
            Icon(
                if (showAll) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Theme.accent,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SoundReactionChip(title: String, selected: Boolean, onClick: () -> Unit) {
    var flashed by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    Text(
        title,
        fontFamily = GoogleSans,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        fontSize = 13.sp,
        color = if (flashed) Theme.accentText else Theme.textPrimary,
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(if (flashed) Theme.accent else Theme.cardFill)
            .border(
                if (selected) 2.dp else 1.dp,
                if (flashed || selected) Theme.accent else Theme.cardBorder,
                androidx.compose.foundation.shape.CircleShape
            )
            .clickable {
                onClick()
                flashed = true
                scope.launch {
                    kotlinx.coroutines.delay(200)
                    flashed = false
                }
            }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    )
}

private fun pointLabel(point: PointDisplay): String = when (point) {
    PointDisplay.LOVE -> "0"
    PointDisplay.FIFTEEN -> "15"
    PointDisplay.THIRTY -> "30"
    PointDisplay.FORTY -> "40"
    PointDisplay.ADVANTAGE -> "Ad"
}

@Composable
private fun MixioDialogInterceptor() {
    val view = LocalView.current
    SideEffect { (view.parent as? DialogWindowProvider)?.window?.interceptMixioRemote() }
}
