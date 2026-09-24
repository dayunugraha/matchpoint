package com.matchpoint.app.ui.matches

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import com.matchpoint.app.ui.theme.SmoothCorner
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.matchpoint.app.ui.theme.themeTitle
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordMatchScreen(
    repository: SessionRepository,
    sessionId: UUID,
    existingMatchId: UUID?,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val viewModel: RecordMatchViewModel = viewModel(factory = RecordMatchViewModel.Factory(repository, sessionId, existingMatchId))
    val state by viewModel.ui.collectAsState()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit Match" else "Add Match", style = themeTitle(18.sp), color = Theme.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Theme.textPrimary) }
                },
                actions = {
                    TextButton(onClick = { viewModel.save(onSaved) }, enabled = state.canSave) {
                        Text("Save", color = if (state.canSave) Theme.accent else Theme.textSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Text("Mode", fontFamily = GoogleSans, fontSize = 12.sp, color = Theme.textSecondary, modifier = Modifier.padding(bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 20.dp)) {
                MatchType.entries.forEach { type ->
                    val selected = state.matchType == type
                    Text(
                        type.name.lowercase().replaceFirstChar { it.uppercase() },
                        fontFamily = GoogleSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = if (selected) Theme.accentText else Theme.textPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .clip(SmoothCorner(14.dp))
                            .background(if (selected) Theme.accent else Theme.cardFill)
                            .clickable(enabled = !state.isEditing) { viewModel.setMatchType(type) }
                            .padding(vertical = 14.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Text(
                "Tap a player to cycle: unassigned → Side A → Side B",
                fontFamily = GoogleSans, fontSize = 12.sp, color = Theme.textSecondary
            )
            Text(
                "Side A: ${state.players.count { it.side == Side.A }}/${state.requiredPerSide} · Side B: ${state.players.count { it.side == Side.B }}/${state.requiredPerSide}",
                fontFamily = GoogleSans, fontSize = 13.sp, color = Theme.textPrimary, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )

            if (state.players.isEmpty()) {
                Text("No players in this mabar.", fontFamily = GoogleSans, fontSize = 13.sp, color = Theme.textSecondary)
            } else {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        state.players.forEachIndexed { index, p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.togglePlayer(p.id) }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(p.name, fontFamily = GoogleSans, fontSize = 14.sp, color = Theme.textPrimary)
                                StatusPill(
                                    text = when (p.side) {
                                        Side.A -> "Side A"
                                        Side.B -> "Side B"
                                        null -> "Tap to assign"
                                    },
                                    filled = p.side != null
                                )
                            }
                            if (index < state.players.size - 1) HorizontalDivider(color = Theme.cardBorder)
                        }
                    }
                }
            }

            Text("Final Score", fontFamily = GoogleSans, fontSize = 12.sp, color = Theme.textSecondary, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    ScoreField("Side A", state.scoreA, viewModel::setScoreA)
                    Text("–", style = themeTitle(20.sp), color = Theme.textSecondary)
                    ScoreField("Side B", state.scoreB, viewModel::setScoreB)
                }
            }
        }
    }
}

@Composable
private fun ScoreField(title: String, value: String, onValueChange: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontFamily = GoogleSans, fontSize = 11.sp, color = Theme.textSecondary)
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .width(64.dp)
                .clip(SmoothCorner(10.dp))
                .background(Theme.fieldFill)
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = value,
                onValueChange = { onValueChange(it.filter(Char::isDigit)) },
                singleLine = true,
                textStyle = TextStyle(fontFamily = GoogleSans, fontSize = 16.sp, color = Theme.fieldText, textAlign = TextAlign.Center),
                cursorBrush = SolidColor(Theme.fieldText),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
