package com.matchpoint.app.ui.match

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import com.matchpoint.app.ui.theme.SmoothCorner
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.matchpoint.app.domain.Side
import com.matchpoint.app.repository.SessionRepository
import com.matchpoint.app.ui.theme.GoogleSans
import com.matchpoint.app.ui.theme.Theme
import com.matchpoint.app.ui.theme.themeTitle
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SelectPlayersScreen(
    repository: SessionRepository,
    matchId: UUID,
    initialSide: String,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val viewModel: SelectPlayersViewModel = viewModel(factory = SelectPlayersViewModel.Factory(repository, matchId, initialSide))
    val state by viewModel.uiState.collectAsState()
    var newName by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Player Selection", style = themeTitle(18.sp), color = Theme.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Theme.textPrimary)
                    }
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
            Text(
                "Side A: ${state.sideACount}/${state.requiredPerSide} · Side B: ${state.sideBCount}/${state.requiredPerSide}",
                fontFamily = GoogleSans,
                fontSize = 13.sp,
                color = Theme.textPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                ThemedField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = "Add new player",
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { viewModel.addNewPlayer(newName); newName = "" },
                    enabled = newName.isNotBlank(),
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Theme.accent)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = Theme.accentText)
                }
            }

            if (state.players.isEmpty()) {
                Text("No players in the roster yet.", fontFamily = GoogleSans, fontSize = 13.sp, color = Theme.textSecondary, modifier = Modifier.padding(vertical = 12.dp))
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.players.forEach { player ->
                        PlayerPill(name = player.name, side = player.side, onClick = { viewModel.togglePlayer(player.id) })
                    }
                }
            }
        }
    }
}

/** Light "themed text field" fill — matches iOS's ThemedTextField (readable against the dark clay background). */
@Composable
private fun ThemedField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
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

@Composable
private fun PlayerPill(name: String, side: Side?, onClick: () -> Unit) {
    val assigned = side != null
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(CircleShape)
            .background(if (assigned) Theme.accent.copy(alpha = 0.16f) else Theme.cardFill)
            .border(1.dp, if (assigned) Theme.accent else Theme.cardBorder, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        if (assigned) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Theme.accent.copy(alpha = 0.16f))
                    .border(1.dp, Theme.accent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(side!!.name, fontFamily = GoogleSans, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Theme.accent)
            }
        }
        Text(
            name,
            fontFamily = GoogleSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = if (assigned) Theme.accent else Theme.textPrimary
        )
    }
}
