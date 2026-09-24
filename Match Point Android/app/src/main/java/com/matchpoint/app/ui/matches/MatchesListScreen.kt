package com.matchpoint.app.ui.matches

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matchpoint.app.R
import com.matchpoint.app.repository.SessionRepository
import com.matchpoint.app.ui.theme.GlassCard
import com.matchpoint.app.ui.theme.GoogleSans
import com.matchpoint.app.ui.theme.LocalCourtTheme
import com.matchpoint.app.ui.theme.Theme
import com.matchpoint.app.ui.theme.themeTitle
import java.text.DateFormat
import java.util.Date
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchesListScreen(
    repository: SessionRepository,
    onOpenSessionHistory: (UUID) -> Unit
) {
    val viewModel: MatchesListViewModel = viewModel(factory = MatchesListViewModel.Factory(repository))
    val sessions by viewModel.pastSessions.collectAsState()

    var selectionMode by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<UUID>()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentWindowInsets = WindowInsets.statusBars.only(WindowInsetsSides.Top),
        topBar = {
            TopAppBar(
                title = { Text("Matches", style = themeTitle(20.sp), color = Theme.textPrimary) },
                actions = {
                    if (selectionMode) {
                        TextButton(onClick = { selectionMode = false; selected = emptySet() }) { Text("Cancel") }
                        IconButton(onClick = { if (selected.isNotEmpty()) showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete (${selected.size})", tint = Theme.danger)
                        }
                    } else if (sessions.isNotEmpty()) {
                        TextButton(onClick = { selectionMode = true }) { Text("Delete Mabar", color = Theme.textPrimary) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
            )
        }
    ) { padding ->
        if (sessions.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp, vertical = 24.dp)) {
                Image(
                    painter = painterResource(LocalCourtTheme.current.matchesEmptyIllustration),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 305.dp)
                )
                Text(
                    "No past mabar yet — today's scoreboard is on Home.",
                    fontFamily = GoogleSans,
                    fontSize = 13.sp,
                    color = Theme.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        "PAST MABAR",
                        fontFamily = GoogleSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Theme.textSecondary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                items(sessions, key = { it.session.id }) { row ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (selectionMode) {
                                selected = if (row.session.id in selected) selected - row.session.id else selected + row.session.id
                            } else {
                                onOpenSessionHistory(row.session.id)
                            }
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Image(
                                    painter = painterResource(LocalCourtTheme.current.pastMabarBadge),
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp)
                                )
                                Column(modifier = Modifier.padding(start = 12.dp)) {
                                    Text(row.session.name, fontFamily = GoogleSans, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Theme.textPrimary)
                                    Text(DateFormat.getDateInstance().format(Date(row.session.date)), fontFamily = GoogleSans, fontSize = 12.sp, color = Theme.textSecondary)
                                    Text("${row.completedCount} matches", fontFamily = GoogleSans, fontSize = 12.sp, color = Theme.textSecondary)
                                    row.topScorerText?.let {
                                        Text("Top: $it", fontFamily = GoogleSans, fontSize = 12.sp, color = Theme.accent)
                                    }
                                }
                            }
                            if (selectionMode) {
                                Checkbox(checked = row.session.id in selected, onCheckedChange = { checked ->
                                    selected = if (checked) selected + row.session.id else selected - row.session.id
                                })
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${selected.size} mabar?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSessions(sessions.filter { it.session.id in selected }.map { it.session })
                    selected = emptySet()
                    selectionMode = false
                    showDeleteConfirm = false
                }) { Text("Delete", color = Theme.danger) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
}
