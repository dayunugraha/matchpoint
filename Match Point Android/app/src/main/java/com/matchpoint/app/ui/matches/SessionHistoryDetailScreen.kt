package com.matchpoint.app.ui.matches

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import com.matchpoint.app.ui.theme.SmoothCorner
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matchpoint.app.R
import com.matchpoint.app.repository.SessionRepository
import com.matchpoint.app.ui.theme.GhostButton
import com.matchpoint.app.ui.theme.GlassCard
import com.matchpoint.app.ui.theme.GoogleSans
import com.matchpoint.app.ui.theme.LocalCourtTheme
import com.matchpoint.app.ui.theme.SegmentedTabPicker
import com.matchpoint.app.ui.theme.Theme
import com.matchpoint.app.ui.theme.themeTitle
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHistoryDetailScreen(
    repository: SessionRepository,
    sessionId: UUID,
    onOpenMatchDetail: (UUID) -> Unit,
    onAddMatch: (UUID) -> Unit,
    onBack: () -> Unit
) {
    val viewModel: SessionHistoryDetailViewModel = viewModel(factory = SessionHistoryDetailViewModel.Factory(repository, sessionId))
    val state by viewModel.uiState.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showShare by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Session Detail", style = themeTitle(18.sp), color = Theme.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Theme.textPrimary) }
                },
                actions = {
                    IconButton(onClick = { showShare = true }) { Icon(Icons.Default.Share, contentDescription = "Share Recap", tint = Theme.textPrimary) }
                    IconButton(onClick = { showRename = true }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Theme.textPrimary) }
                    IconButton(onClick = { showDelete = true }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Theme.danger) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp).verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 20.dp)) {
                Image(
                    painter = painterResource(LocalCourtTheme.current.pastMabarBadge),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp).clip(CircleShape)
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(state.session?.name ?: "Mabar", style = themeTitle(20.sp), color = Theme.textPrimary)
                    state.session?.let { s ->
                        Text(
                            DateFormat.getDateInstance(DateFormat.FULL).format(Date(s.date)),
                            fontFamily = GoogleSans, fontSize = 13.sp, color = Theme.textSecondary
                        )
                        if (s.startTime != null && s.endTime != null) {
                            val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
                            Text(
                                "${timeFmt.format(Date(s.startTime))} – ${timeFmt.format(Date(s.endTime))}",
                                fontFamily = GoogleSans, fontSize = 12.sp, color = Theme.textSecondary
                            )
                        }
                    }
                }
            }

            SegmentedTabPicker(
                options = listOf(0, 1),
                selected = tab,
                label = { if (it == 0) "Standings" else "Matches" },
                onSelect = { tab = it },
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (tab == 0) {
                if (state.standings.isEmpty()) {
                    Text("No players registered.", fontFamily = GoogleSans, fontSize = 13.sp, color = Theme.textSecondary)
                } else {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            state.standings.forEachIndexed { index, sp ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Text(
                                            "${index + 1}",
                                            fontFamily = GoogleSans, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                            color = Theme.textSecondary,
                                            modifier = Modifier.padding(end = 10.dp)
                                        )
                                        Text(
                                            sp.name,
                                            fontFamily = GoogleSans,
                                            fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 14.sp,
                                            color = Theme.textPrimary
                                        )
                                    }
                                    Text("${sp.sessionPlayer.wins}–${sp.sessionPlayer.losses}", fontFamily = GoogleSans, fontSize = 12.sp, color = Theme.textSecondary, modifier = Modifier.padding(end = 12.dp))
                                    Text(
                                        "${sp.sessionPlayer.points} pts",
                                        fontFamily = GoogleSans, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                        color = if (index == 0) Theme.accent else Theme.textSecondary
                                    )
                                }
                                if (index < state.standings.size - 1) HorizontalDivider(color = Theme.cardBorder)
                            }
                        }
                    }
                }
            } else {
                if (state.completedMatches.isEmpty()) {
                    Text("No completed matches.", fontFamily = GoogleSans, fontSize = 13.sp, color = Theme.textSecondary)
                } else {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            state.completedMatches.forEachIndexed { index, m ->
                                MatchRow(
                                    number = index + 1,
                                    match = m,
                                    canMoveUp = index > 0,
                                    canMoveDown = index < state.completedMatches.size - 1,
                                    onClick = { onOpenMatchDetail(m.match.id) },
                                    onMoveUp = { viewModel.moveMatch(m.match.id, up = true) },
                                    onMoveDown = { viewModel.moveMatch(m.match.id, up = false) }
                                )
                                if (index < state.completedMatches.size - 1) HorizontalDivider(color = Theme.cardBorder)
                            }
                        }
                    }
                }
                GhostButton(
                    text = "+ Add Match",
                    modifier = Modifier.padding(top = 16.dp),
                    onClick = { onAddMatch(sessionId) }
                )
            }
        }
    }

    if (showRename && state.session != null) {
        val s = state.session!!
        com.matchpoint.app.ui.common.EditSessionDetailsDialog(
            initialName = s.name,
            initialDate = s.date,
            initialStartTime = s.startTime,
            initialEndTime = s.endTime,
            onDismiss = { showRename = false },
            onSave = { name, date, start, end ->
                viewModel.updateSessionDetails(name, date, start, end)
                showRename = false
            }
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete this mabar?") },
            text = { Text("This permanently removes its matches and standings. This can't be undone.") },
            confirmButton = { TextButton(onClick = { showDelete = false; viewModel.deleteSession(onBack) }) { Text("Delete", color = Theme.danger) } },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } }
        )
    }

    if (showShare && state.session != null) {
        ShareCardCapture(
            session = state.session!!,
            standings = state.standings,
            completedMatches = state.completedMatches,
            onDone = { showShare = false }
        )
    }
}

@Composable
private fun MatchRow(
    number: Int,
    match: HistoryMatchUi,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp)
    ) {
        Text(
            "M$number",
            fontFamily = GoogleSans, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Theme.textSecondary,
            modifier = Modifier.padding(top = 4.dp, end = 10.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            TeamScoreRow(match.sideANames, match.match.finalScoreA, isWinner = match.match.winnerSide == com.matchpoint.app.domain.Side.A)
            TeamScoreRow(match.sideBNames, match.match.finalScoreB, isWinner = match.match.winnerSide == com.matchpoint.app.domain.Side.B)
        }
        Column {
            IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up", tint = if (canMoveUp) Theme.textSecondary else Theme.textSecondary.copy(alpha = 0.3f))
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down", tint = if (canMoveDown) Theme.textSecondary else Theme.textSecondary.copy(alpha = 0.3f))
            }
        }
    }
}

@Composable
private fun TeamScoreRow(names: List<String>, score: Int?, isWinner: Boolean) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            names.joinToString(" & "),
            fontFamily = GoogleSans,
            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp,
            color = Theme.textPrimary,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        score?.let {
            Text("$it", style = themeTitle(16.sp), color = if (isWinner) Theme.accent else Theme.textSecondary)
        }
    }
}
