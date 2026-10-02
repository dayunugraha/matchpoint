package com.matchpoint.app.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchpoint.app.backend.UpdateState
import com.matchpoint.app.ui.theme.GoogleSans
import com.matchpoint.app.ui.theme.Theme

/**
 * Shown when [ReleaseRepository][com.matchpoint.app.backend.ReleaseRepository] reports an
 * [UpdateState.UpdateAvailable]. Does not download or install anything — [onUpdate] only hands
 * the APK URL to the caller for a future download layer. A mandatory release drops the "Later"
 * action and can't be dismissed by tapping outside.
 */
@Composable
fun UpdateDialog(state: UpdateState.UpdateAvailable, onUpdate: () -> Unit, onLater: () -> Unit) {
    AlertDialog(
        onDismissRequest = { if (!state.isMandatory) onLater() },
        title = { Text(if (state.isMandatory) "Update required" else "Update available") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Match Point ${state.versionName}", fontFamily = GoogleSans, fontSize = 14.sp, color = Theme.textPrimary)
                if (state.isMandatory) {
                    Text(
                        "This update is required to keep using Match Point.",
                        fontFamily = GoogleSans,
                        fontSize = 13.sp,
                        color = Theme.textSecondary
                    )
                }
                state.releaseNotes?.takeIf { it.isNotBlank() }?.let {
                    Text(it, fontFamily = GoogleSans, fontSize = 13.sp, color = Theme.textSecondary)
                }
            }
        },
        confirmButton = { TextButton(onClick = onUpdate) { Text("Update") } },
        dismissButton = if (state.isMandatory) null else {
            { TextButton(onClick = onLater) { Text("Later") } }
        }
    )
}
