package com.matchpoint.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.matchpoint.app.audio.SoundManager
import com.matchpoint.app.backup.BackupService
import com.matchpoint.app.ui.theme.CourtTheme
import com.matchpoint.app.ui.theme.SmoothCorner
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import com.matchpoint.app.ui.theme.GhostButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchpoint.app.data.AppPreferences
import com.matchpoint.app.domain.MatchType
import com.matchpoint.app.ui.theme.GlassCard
import com.matchpoint.app.ui.theme.GoogleSans
import com.matchpoint.app.ui.theme.SegmentedTabPicker
import com.matchpoint.app.ui.theme.Theme
import com.matchpoint.app.ui.theme.themeTitle
import com.matchpoint.app.wear.WearConnectionState
import com.matchpoint.app.wear.WearEngineManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(preferences: AppPreferences, backupService: BackupService, wearEngineManager: WearEngineManager) {
    val defaultType by preferences.defaultMatchType.collectAsState()
    val defaultDeuce by preferences.defaultDeuceAdvantageEnabled.collectAsState()
    val announcerEnabled by preferences.announcerEnabled.collectAsState()
    val courtTheme by preferences.courtTheme.collectAsState()
    val wearConnectionState by wearEngineManager.connectionState.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pendingImportJson by remember { mutableStateOf<String?>(null) }
    var showRestoreConfirmation by remember { mutableStateOf(false) }
    var backupErrorMessage by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val json = backupService.exportJson()
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            } catch (e: Exception) {
                backupErrorMessage = e.message ?: "Couldn't export backup."
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                    ?: throw IllegalStateException("Couldn't read the selected file.")
                pendingImportJson = json
                showRestoreConfirmation = true
            } catch (e: Exception) {
                backupErrorMessage = e.message ?: "Couldn't read backup file."
            }
        }
    }

    if (showRestoreConfirmation) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmation = false; pendingImportJson = null },
            title = { Text("Replace all data with this backup?") },
            text = { Text("Every current mabar, player, and match will be deleted and replaced with the backup's contents. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreConfirmation = false
                    val json = pendingImportJson
                    pendingImportJson = null
                    if (json != null) {
                        scope.launch {
                            try {
                                backupService.restore(json)
                            } catch (e: Exception) {
                                backupErrorMessage = e.message ?: "Couldn't restore backup."
                            }
                        }
                    }
                }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmation = false; pendingImportJson = null }) { Text("Cancel") }
            }
        )
    }

    if (backupErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { backupErrorMessage = null },
            title = { Text("Backup Error") },
            text = { Text(backupErrorMessage ?: "") },
            confirmButton = {
                TextButton(onClick = { backupErrorMessage = null }) { Text("OK") }
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.statusBars.only(WindowInsetsSides.Top),
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = themeTitle(20.sp), color = Theme.textPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("APPEARANCE", fontFamily = GoogleSans, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Theme.textSecondary, modifier = Modifier.padding(bottom = 8.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Court Theme", fontFamily = GoogleSans, fontSize = 14.sp, color = Theme.textPrimary, modifier = Modifier.padding(bottom = 8.dp))
                SegmentedTabPicker(
                    options = CourtTheme.entries,
                    selected = courtTheme,
                    label = { it.displayName },
                    onSelect = { preferences.setCourtTheme(it) }
                )
            }
            Text(
                "${courtTheme.displayName} — ${courtTheme.subtitle}.",
                fontFamily = GoogleSans,
                fontSize = 12.sp,
                color = Theme.textSecondary,
                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
            )

            Text("AUDIO & ANNOUNCER", fontFamily = GoogleSans, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Theme.textSecondary, modifier = Modifier.padding(bottom = 8.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Announcer", fontFamily = GoogleSans, fontSize = 14.sp, color = Theme.textPrimary)
                        Text("Voice call-outs for scores and match events", fontFamily = GoogleSans, fontSize = 12.sp, color = Theme.textSecondary, modifier = Modifier.padding(top = 4.dp))
                    }
                    ThemedSwitch(
                        checked = announcerEnabled,
                        onCheckedChange = { preferences.setAnnouncerEnabled(it) }
                    )
                }
                HorizontalDivider(color = Theme.cardBorder, modifier = Modifier.padding(vertical = 14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { context.startActivity(Intent(AndroidSettings.ACTION_BLUETOOTH_SETTINGS)) },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Bluetooth, contentDescription = null, tint = Theme.accent, modifier = Modifier.size(20.dp))
                        Column(modifier = Modifier.padding(start = 10.dp)) {
                            Text("Connect Bluetooth Speaker", fontFamily = GoogleSans, fontSize = 14.sp, color = Theme.textPrimary)
                            Text("Opens system Bluetooth settings", fontFamily = GoogleSans, fontSize = 12.sp, color = Theme.textSecondary, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Theme.textSecondary)
                }
                HorizontalDivider(color = Theme.cardBorder, modifier = Modifier.padding(vertical = 14.dp))
                GhostButton(
                    text = "Test Announcer",
                    enabled = announcerEnabled,
                    onClick = { SoundManager.play(listOf("game")) }
                )
            }

            Text("MATCH DEFAULTS", fontFamily = GoogleSans, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Theme.textSecondary, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Default Match Type", fontFamily = GoogleSans, fontSize = 14.sp, color = Theme.textPrimary, modifier = Modifier.padding(bottom = 8.dp))
                SegmentedTabPicker(
                    options = listOf(MatchType.DOUBLES, MatchType.SINGLES),
                    selected = defaultType,
                    label = { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                    onSelect = { preferences.setDefaultMatchType(it) }
                )
                HorizontalDivider(color = Theme.cardBorder, modifier = Modifier.padding(top = 16.dp, bottom = 14.dp))
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
                        checked = defaultDeuce,
                        onCheckedChange = { preferences.setDefaultDeuceAdvantageEnabled(it) }
                    )
                }
            }
            Text(
                "Defaults only pre-fill new matches — they don't lock the configuration.",
                fontFamily = GoogleSans,
                fontSize = 12.sp,
                color = Theme.textSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text("BACKUP & RESTORE", fontFamily = GoogleSans, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Theme.textSecondary, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Export Backup",
                    fontFamily = GoogleSans,
                    fontSize = 14.sp,
                    color = Theme.textPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { exportLauncher.launch(BackupService.suggestedFilename()) }
                        .padding(vertical = 4.dp)
                )
                HorizontalDivider(color = Theme.cardBorder, modifier = Modifier.padding(vertical = 14.dp))
                Text(
                    "Restore from Backup",
                    fontFamily = GoogleSans,
                    fontSize = 14.sp,
                    color = Theme.textPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { importLauncher.launch(arrayOf("application/json")) }
                        .padding(vertical = 4.dp)
                )
            }
            Text(
                "This app doesn't sync to the cloud — export a backup file periodically, especially before reinstalling the app or resetting this device. Restoring replaces all current mabars, players, and history with the backup's contents.",
                fontFamily = GoogleSans,
                fontSize = 12.sp,
                color = Theme.textSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )

            Text("WEARABLE REMOTE", fontFamily = GoogleSans, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Theme.textSecondary, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Wearable", fontFamily = GoogleSans, fontSize = 14.sp, color = Theme.textPrimary)
                    Text(
                        wearConnectionState.debugLabel(),
                        fontFamily = GoogleSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = if (wearConnectionState == WearConnectionState.CONNECTED) Theme.accent else Theme.textSecondary
                    )
                }
                HorizontalDivider(color = Theme.cardBorder, modifier = Modifier.padding(vertical = 14.dp))
                GhostButton(
                    text = "Retry Connection",
                    enabled = wearConnectionState != WearConnectionState.CONNECTING,
                    onClick = { wearEngineManager.connect() }
                )
            }
            Text(
                "Shows whether your HUAWEI WATCH GT 4 is connected to Match Point.",
                fontFamily = GoogleSans,
                fontSize = 12.sp,
                color = Theme.textSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

private fun WearConnectionState.debugLabel(): String = when (this) {
    WearConnectionState.DISCONNECTED -> "Disconnected"
    WearConnectionState.CONNECTING -> "Connecting…"
    WearConnectionState.NO_DEVICE -> "No paired watch found"
    WearConnectionState.PERMISSION_REQUIRED -> "Permission required"
    WearConnectionState.WATCH_APP_NOT_INSTALLED -> "Watch app not installed"
    WearConnectionState.CONNECTED -> "Connected"
    WearConnectionState.ERROR -> "Connection error"
}
