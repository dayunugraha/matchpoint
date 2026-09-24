package com.matchpoint.app.ui.players

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matchpoint.app.repository.SessionRepository
import com.matchpoint.app.repository.gameDifference
import com.matchpoint.app.repository.winRate
import com.matchpoint.app.ui.theme.GlassCard
import com.matchpoint.app.ui.theme.GoogleSans
import com.matchpoint.app.ui.theme.StatusPill
import com.matchpoint.app.ui.theme.Theme
import com.matchpoint.app.ui.theme.themeTitle
import java.text.DateFormat
import java.util.Date
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerProfileScreen(
    repository: SessionRepository,
    playerId: UUID,
    onBack: () -> Unit
) {
    val viewModel: PlayerProfileViewModel = viewModel(factory = PlayerProfileViewModel.Factory(repository, playerId))
    val state by viewModel.uiState.collectAsState()
    val player = state.player ?: return
    val totals = state.totals

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Profile", style = themeTitle(18.sp), color = Theme.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Theme.textPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(64.dp).padding(top = 16.dp).clip(CircleShape).background(Theme.accent),
                contentAlignment = Alignment.Center
            ) {
                Text(player.name.take(1).uppercase(), style = themeTitle(24.sp), color = Theme.accentText)
            }
            Text(player.name, style = themeTitle(20.sp), color = Theme.textPrimary, modifier = Modifier.padding(top = 16.dp, bottom = 16.dp))

            if (totals != null) {
                val stats = listOf(
                    "Matches" to "${totals.matchesPlayed}",
                    "W–L" to "${totals.wins}–${totals.losses}",
                    "Points" to "${totals.points}",
                    "Win Rate" to "${(totals.winRate * 100).toInt()}%",
                    "Games Won" to "${totals.gamesWon}",
                    "Game Diff" to (if (totals.gameDifference >= 0) "+${totals.gameDifference}" else "${totals.gameDifference}")
                )
                for (row in stats.chunked(2)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                        row.forEach { (label, value) ->
                            GlassCard(modifier = Modifier.weight(1f)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    Text(value, style = themeTitle(20.sp), color = Theme.accent)
                                    Text(label, fontFamily = GoogleSans, fontSize = 11.sp, color = Theme.textSecondary)
                                }
                            }
                        }
                    }
                }
            } else {
                Text("No matches played yet.", fontFamily = GoogleSans, fontSize = 13.sp, color = Theme.textSecondary, modifier = Modifier.padding(bottom = 10.dp))
            }

            Text(
                "MATCH HISTORY",
                fontFamily = GoogleSans,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Theme.textSecondary,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 8.dp)
            )

            if (state.history.isEmpty()) {
                Text(
                    "No matches played yet.",
                    fontFamily = GoogleSans, fontSize = 13.sp, color = Theme.textSecondary,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        state.history.forEachIndexed { index, row ->
                            HistoryRow(row)
                            if (index < state.history.size - 1) HorizontalDivider(color = Theme.cardBorder)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(row: MatchHistoryRow) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            if (row.sessionName.isNotBlank()) {
                Text(row.sessionName.uppercase(), fontFamily = GoogleSans, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Theme.textSecondary)
            }
            Text(row.matchupText, fontFamily = GoogleSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Theme.textPrimary, modifier = Modifier.padding(top = 2.dp))
            row.finishedAt?.let {
                Text(DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it)), fontFamily = GoogleSans, fontSize = 11.sp, color = Theme.textSecondary, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${row.myScore}–${row.opponentScore}", fontFamily = GoogleSans, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Theme.textPrimary, modifier = Modifier.padding(end = 8.dp))
            StatusPill(text = if (row.won) "W" else "L", filled = row.won)
        }
    }
}
