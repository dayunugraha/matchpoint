package com.matchpoint.app.ui.matches

import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.drawToBitmap
import com.matchpoint.app.data.SessionEntity
import com.matchpoint.app.ui.theme.LocalCourtTheme

/**
 * Renders [ShareCardContent] off-screen (in a fully transparent, non-dimming dialog window so it
 * never actually appears to the user) at a fixed 1080px width, captures it to a bitmap once its
 * layout settles, fits it onto the story canvas via [ShareCardRenderer], and launches the share
 * sheet. Mirrors iOS's synchronous `ImageRenderer`-based export — an Android `ComposeView` needs
 * a real (if invisible) window to measure/layout/draw into, hence the dialog.
 */
@Composable
fun ShareCardCapture(session: SessionEntity, standings: List<HistoryPlayerUi>, completedMatches: List<HistoryMatchUi>, onDone: () -> Unit) {
    val context = LocalContext.current
    val courtTheme = LocalCourtTheme.current

    Dialog(
        onDismissRequest = onDone,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        val view = LocalView.current
        val dialogWindowProvider = view.parent as? DialogWindowProvider
        SideEffect {
            dialogWindowProvider?.window?.apply {
                setDimAmount(0f)
                setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)
                decorView.alpha = 0f
            }
        }

        CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 1f)) {
            ShareCardContent(session = session, standings = standings, completedMatches = completedMatches)
        }

        LaunchedEffect(session.id) {
            // Two frames guarantees layout has settled before capture (measure -> layout -> draw).
            withFrameNanos {}
            withFrameNanos {}
            val bitmap = view.drawToBitmap()
            val storyImage = ShareCardRenderer.composeToStoryCanvas(bitmap, courtTheme)
            val uri = ShareCardRenderer.saveToCache(context, storyImage, session.name)
            context.startActivity(ShareCardRenderer.shareIntent(uri))
            onDone()
        }
    }
}
