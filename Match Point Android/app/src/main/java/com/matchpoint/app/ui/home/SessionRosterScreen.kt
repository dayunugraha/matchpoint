package com.matchpoint.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import com.matchpoint.app.ui.theme.SmoothCorner
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matchpoint.app.domain.SessionPlayerState
import com.matchpoint.app.repository.SessionRepository
import com.matchpoint.app.ui.theme.GhostButton
import com.matchpoint.app.ui.theme.GlassCard
import com.matchpoint.app.ui.theme.GoogleSans
import com.matchpoint.app.ui.theme.Theme
import com.matchpoint.app.ui.theme.themeTitle
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionRosterScreen(
    repository: SessionRepository,
    sessionId: UUID,
    onBack: () -> Unit
) {
    val viewModel: SessionRosterViewModel = viewModel(factory = SessionRosterViewModel.Factory(repository, sessionId))
    val state by viewModel.uiState.collectAsState()
    var newName by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Add Players", style = themeTitle(18.sp), color = Theme.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Theme.textPrimary) }
                },
                actions = {
                    TextButton(onClick = onBack) { Text("Done", color = Theme.accent) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp).verticalScroll(rememberScrollState())) {
            Text("FROM ROSTER", fontFamily = GoogleSans, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Theme.textSecondary, modifier = Modifier.padding(bottom = 8.dp))
            if (state.unregisteredRosterPlayers.isEmpty()) {
                Text("Everyone in the roster is already registered today.", fontFamily = GoogleSans, fontSize = 13.sp, color = Theme.textSecondary)
            } else {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        state.unregisteredRosterPlayers.forEachIndexed { index, player ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.registerExisting(player.id) }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(player.name, fontFamily = GoogleSans, fontSize = 14.sp, color = Theme.textPrimary)
                                Icon(Icons.Default.AddCircle, contentDescription = "Add", tint = Theme.accent)
                            }
                            if (index < state.unregisteredRosterPlayers.size - 1) HorizontalDivider(color = Theme.cardBorder)
                        }
                    }
                }
            }

            Text("NEW PLAYER", fontFamily = GoogleSans, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Theme.textSecondary, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ThemedField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = "Player name",
                    modifier = Modifier.weight(1f)
                )
                GhostButton(
                    text = "Add",
                    modifier = Modifier.padding(start = 8.dp).weight(0.5f),
                    enabled = newName.isNotBlank(),
                    onClick = { viewModel.addNewPlayer(newName); newName = "" }
                )
            }

            if (state.roster.isNotEmpty()) {
                Text("TODAY'S ROSTER", fontFamily = GoogleSans, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Theme.textSecondary, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        state.roster.forEachIndexed { index, rp ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(rp.name, fontFamily = GoogleSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Theme.textPrimary)
                                    Text(stateLabel(rp.sessionPlayer.state), fontFamily = GoogleSans, fontSize = 12.sp, color = Theme.textSecondary)
                                }
                                if (rp.sessionPlayer.state == SessionPlayerState.PLAYING) {
                                    Text("Playing", fontFamily = GoogleSans, fontSize = 12.sp, color = Theme.accent)
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        ThemedSwitch(
                                            checked = rp.sessionPlayer.state == SessionPlayerState.AVAILABLE,
                                            onCheckedChange = { viewModel.toggleAvailable(rp.sessionPlayer) }
                                        )
                                        TextButton(onClick = { viewModel.unregister(rp.sessionPlayer) }) {
                                            Text("Remove", color = Theme.danger, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                            if (index < state.roster.size - 1) HorizontalDivider(color = Theme.cardBorder)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemedField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(SmoothCorner(10.dp))
            .background(Theme.fieldFill)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        if (value.isEmpty()) {
            Text(placeholder, fontFamily = GoogleSans, fontSize = 14.sp, color = Theme.fieldText.copy(alpha = 0.45f))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(fontFamily = GoogleSans, fontSize = 14.sp, color = Theme.fieldText),
            cursorBrush = SolidColor(Theme.fieldText),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun stateLabel(state: SessionPlayerState): String = when (state) {
    SessionPlayerState.REGISTERED -> "Registered"
    SessionPlayerState.AVAILABLE -> "Available"
    SessionPlayerState.PLAYING -> "Playing"
    SessionPlayerState.WAITING -> "Waiting"
}
