package com.matchpoint.app.ui.players

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import com.matchpoint.app.ui.theme.SmoothCorner
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matchpoint.app.R
import com.matchpoint.app.repository.SessionRepository
import com.matchpoint.app.ui.theme.GoogleSans
import com.matchpoint.app.ui.theme.LocalCourtTheme
import com.matchpoint.app.ui.theme.PrimaryButton
import com.matchpoint.app.ui.theme.Theme
import com.matchpoint.app.ui.theme.themeTitle
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayersListScreen(
    repository: SessionRepository,
    onOpenProfile: (UUID) -> Unit
) {
    val viewModel: PlayersListViewModel = viewModel(factory = PlayersListViewModel.Factory(repository))
    val players by viewModel.players.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val filteredPlayers = if (query.isBlank()) players else players.filter { it.player.name.contains(query, ignoreCase = true) }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.statusBars.only(WindowInsetsSides.Top),
        topBar = {
            TopAppBar(
                title = { Text("Players", style = themeTitle(20.sp), color = Theme.textPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            if (players.isNotEmpty()) {
                FloatingActionButton(onClick = { showAddDialog = true }, containerColor = Theme.accent, contentColor = Theme.accentText) {
                    Icon(Icons.Default.Add, contentDescription = "Add Player")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            if (players.isNotEmpty()) {
                SearchField(query = query, onQueryChange = { query = it })
            }
            if (players.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize().padding(top = 24.dp)) {
                    Image(
                        painter = painterResource(LocalCourtTheme.current.playersEmptyIllustration),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 305.dp)
                    )
                    Text(
                        "No players in the roster yet. Start a session from Home to add or register players for today.",
                        fontFamily = GoogleSans,
                        fontSize = 13.sp,
                        color = Theme.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp)
                    )
                    PrimaryButton(text = "+ Add Player", onClick = { showAddDialog = true })
                }
            } else if (filteredPlayers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        "No players match \"$query\".",
                        fontFamily = GoogleSans,
                        fontSize = 13.sp,
                        color = Theme.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredPlayers, key = { it.player.id }) { card ->
                        PlayerCard(card, viewModel, onClick = { if (card.totalMatches > 0) onOpenProfile(card.player.id) })
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Player") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, placeholder = { Text("Name") }) },
            confirmButton = { TextButton(onClick = { viewModel.addPlayer(name); showAddDialog = false }) { Text("Add") } },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )
    }
}

/** Light "themed text field" fill — matches iOS's ThemedTextField (dark-mode-legible input style). */
@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 12.dp)
            .clip(SmoothCorner(10.dp))
            .background(Theme.fieldFill)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = Theme.fieldText.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
        androidx.compose.foundation.layout.Spacer(Modifier.padding(start = 4.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            if (query.isEmpty()) {
                Text("Search players", fontFamily = GoogleSans, fontSize = 14.sp, color = Theme.fieldText.copy(alpha = 0.45f))
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(fontFamily = GoogleSans, fontSize = 14.sp, color = Theme.fieldText),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Theme.fieldText),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PlayerCard(card: PlayerCardUi, viewModel: PlayersListViewModel, onClick: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SmoothCorner(16.dp))
            .background(Theme.cardFill)
            .border(1.dp, Theme.cardBorder, SmoothCorner(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(Theme.accent),
                contentAlignment = Alignment.Center
            ) {
                Text(card.player.name.take(1).uppercase(), fontFamily = GoogleSans, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Theme.accentText)
            }
            Text(
                card.player.name,
                fontFamily = GoogleSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Theme.textPrimary,
                maxLines = 1,
                modifier = Modifier.padding(top = 10.dp)
            )
            Text(
                if (card.totalMatches > 0) "${card.totalMatches} matches · ${card.totalPoints} pts" else "No matches yet",
                fontFamily = GoogleSans, fontSize = 11.sp, color = Theme.textSecondary, maxLines = 1
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(Theme.backgroundTop.copy(alpha = 0.5f))
        ) {
            IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Default.MoreHoriz, contentDescription = "Menu", tint = Theme.textSecondary, modifier = Modifier.size(15.dp))
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(text = { Text("Edit") }, onClick = { menuExpanded = false; showRename = true })
                DropdownMenuItem(text = { Text("Delete") }, onClick = { menuExpanded = false; showDelete = true })
            }
        }
    }

    if (showRename) {
        var name by remember { mutableStateOf(card.player.name) }
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("Edit Player") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
            confirmButton = { TextButton(onClick = { viewModel.renamePlayer(card.player, name); showRename = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { showRename = false }) { Text("Cancel") } }
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete ${card.player.name}?") },
            text = {
                Column {
                    Text("They'll disappear from every session's roster and leaderboard, including past ones.")
                    if (deleteError) Text("Can't delete — currently in a live match.", color = Theme.danger, modifier = Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePlayer(card.player) { success ->
                        if (success) showDelete = false else deleteError = true
                    }
                }) { Text("Delete", color = Theme.danger) }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } }
        )
    }
}
