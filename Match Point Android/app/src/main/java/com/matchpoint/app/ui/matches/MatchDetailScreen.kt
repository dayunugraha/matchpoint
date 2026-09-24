package com.matchpoint.app.ui.matches

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matchpoint.app.domain.MatchType
import com.matchpoint.app.domain.Side
import com.matchpoint.app.repository.SessionRepository
import com.matchpoint.app.ui.theme.GlassCard
import com.matchpoint.app.ui.theme.GoogleSans
import com.matchpoint.app.ui.theme.StatusPill
import com.matchpoint.app.ui.theme.Theme
import com.matchpoint.app.ui.theme.themeScore
import com.matchpoint.app.ui.theme.themeTitle
import java.text.DateFormat
import java.util.Date
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailScreen(
    repository: SessionRepository,
    matchId: UUID,
    onEdit: (UUID) -> Unit,
    onBack: () -> Unit
) {
    val viewModel: MatchDetailViewModel = viewModel(factory = MatchDetailViewModel.Factory(repository, matchId))
    val state by viewModel.uiState.collectAsState()
    val match = state.match ?: return

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Match Detail", style = themeTitle(18.sp), color = Theme.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Theme.textPrimary) }
                },
                actions = {
                    if (state.editable) {
                        IconButton(onClick = { onEdit(match.sessionId) }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Theme.textPrimary) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                StatusPill(text = if (match.type == MatchType.DOUBLES) "DOUBLES" else "SINGLES")
                match.finishedAt?.let {
                    Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it)), fontFamily = GoogleSans, fontSize = 12.sp, color = Theme.textSecondary)
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    TeamBlock(state.sideANames, isWinner = match.winnerSide == Side.A)
                    Text("vs", fontFamily = GoogleSans, fontSize = 13.sp, color = Theme.textSecondary, modifier = Modifier.padding(vertical = 6.dp))
                    TeamBlock(state.sideBNames, isWinner = match.winnerSide == Side.B)
                    Text("${match.finalScoreA} — ${match.finalScoreB}", style = themeScore(40.sp), color = Theme.accent, modifier = Modifier.padding(top = 8.dp))
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Column {
                    DetailRow("Duration", duration(match.startedAt, match.finishedAt))
                    HorizontalDivider(color = Theme.cardBorder)
                    DetailRow("Started serve", state.serverName ?: "—")
                    HorizontalDivider(color = Theme.cardBorder)
                    DetailRow("Deuce & Advantage", if (match.deuceAdvantageEnabled) "On" else "Off")
                }
            }
        }
    }
}

@Composable
private fun TeamBlock(names: List<String>, isWinner: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(names.joinToString(" & "), style = themeTitle(18.sp), color = Theme.textPrimary)
        if (isWinner) Text(" 🏆", fontFamily = GoogleSans, fontSize = 14.sp)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontFamily = GoogleSans, fontSize = 13.sp, color = Theme.textSecondary)
        Text(value, fontFamily = GoogleSans, fontSize = 13.sp, color = Theme.textPrimary)
    }
}

private fun duration(startedAt: Long?, finishedAt: Long?): String {
    if (startedAt == null || finishedAt == null) return "—"
    val minutes = maxOf(1, (finishedAt - startedAt) / 60000)
    return "$minutes min"
}
