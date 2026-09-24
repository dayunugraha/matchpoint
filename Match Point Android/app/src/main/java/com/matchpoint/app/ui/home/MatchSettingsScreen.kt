package com.matchpoint.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import com.matchpoint.app.ui.theme.ThemedSwitch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.matchpoint.app.repository.SessionRepository
import com.matchpoint.app.ui.theme.GlassCard
import com.matchpoint.app.ui.theme.GoogleSans
import com.matchpoint.app.ui.theme.Theme
import com.matchpoint.app.ui.theme.themeTitle
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchSettingsScreen(
    repository: SessionRepository,
    matchId: UUID,
    onBack: () -> Unit
) {
    val viewModel: MatchSettingsViewModel = viewModel(factory = MatchSettingsViewModel.Factory(repository, matchId))
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Match Settings", style = themeTitle(18.sp), color = Theme.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Theme.textPrimary) }
                },
                actions = {
                    TextButton(onClick = { viewModel.save(onBack) }) { Text("Done", color = Theme.accent) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Text("STARTING SERVER", fontFamily = GoogleSans, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 11.sp, color = Theme.textSecondary, modifier = Modifier.padding(bottom = 8.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    state.participants.forEachIndexed { index, (id, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setStartingServer(id) }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(name, fontFamily = GoogleSans, fontSize = 14.sp, color = Theme.textPrimary)
                            if (state.selectedServerId == id) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = Theme.accent)
                            }
                        }
                        if (index < state.participants.size - 1) HorizontalDivider(color = Theme.cardBorder)
                    }
                    if (state.participants.isEmpty()) {
                        Text("Select players for this match first.", fontFamily = GoogleSans, fontSize = 13.sp, color = Theme.textSecondary)
                    }
                }
            }

            Text("SCORING AT 40–40", fontFamily = GoogleSans, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 11.sp, color = Theme.textSecondary, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Deuce & Advantage", fontFamily = GoogleSans, fontSize = 14.sp, color = Theme.textPrimary)
                        Text("Off = sudden point decides the game", fontFamily = GoogleSans, fontSize = 12.sp, color = Theme.textSecondary, modifier = Modifier.padding(top = 4.dp))
                    }
                    ThemedSwitch(
                        checked = state.deuceAdvantageEnabled,
                        onCheckedChange = { viewModel.setDeuceAdvantageEnabled(it) }
                    )
                }
            }
        }
    }
}
