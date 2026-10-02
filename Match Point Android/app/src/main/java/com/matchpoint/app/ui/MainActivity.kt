package com.matchpoint.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matchpoint.app.backend.UpdateState
import com.matchpoint.app.ui.match.MixioRemoteInput
import com.matchpoint.app.ui.match.VolumeKeyBridge
import com.matchpoint.app.ui.navigation.RootNavGraph
import com.matchpoint.app.ui.registration.RegistrationScreen
import com.matchpoint.app.ui.registration.RegistrationViewModel
import com.matchpoint.app.ui.splash.SplashScreen
import com.matchpoint.app.ui.theme.MatchPointTheme
import com.matchpoint.app.ui.theme.ThemedScreenBackground
import com.matchpoint.app.ui.update.UpdateDialog
import kotlinx.coroutines.delay
import androidx.compose.animation.core.animateFloatAsState

class MainActivity : ComponentActivity() {

    lateinit var container: AppContainer
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container = AppContainer.get(applicationContext)
        container.wearEngineManager.connect() // no-op if already connecting/connected; revives after a finish()
        enableEdgeToEdge()

        setContent {
            MatchPointTheme(preferences = container.preferences) {
                val isRegistered by container.registrationRepository.isRegistered.collectAsState()
                val updateState by container.releaseRepository.updateState.collectAsState()

                // Registered-user startup check only — never re-runs per screen. Confirms the
                // session is still valid, refreshes this installation's last_seen_at/app version,
                // and checks for a newer release once per app start. The release check is
                // non-critical: a failure just leaves updateState at Error and never blocks the
                // app below from opening.
                LaunchedEffect(isRegistered) {
                    if (isRegistered) {
                        container.registrationRepository.ensureSessionAndTouchInstallation()
                        container.releaseRepository.checkForUpdateOnce()
                    }
                }

                ThemedScreenBackground {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (isRegistered) {
                            RootNavGraph(
                                repository = container.repository,
                                preferences = container.preferences,
                                backupService = container.backupService,
                                wearEngineManager = container.wearEngineManager
                            )
                        } else {
                            val registrationViewModel: RegistrationViewModel =
                                viewModel(factory = RegistrationViewModel.Factory(container.registrationRepository))
                            RegistrationScreen(viewModel = registrationViewModel)
                        }
                        SplashOverlay()
                    }
                }

                (updateState as? UpdateState.UpdateAvailable)?.let { available ->
                    var dismissed by remember(available) { mutableStateOf(false) }
                    if (!dismissed) {
                        UpdateDialog(
                            state = available,
                            // No in-app download/install: hand the APK link to the system
                            // browser/download manager. https only, and only on a user tap.
                            onUpdate = { openApkUrl(available.apkUrl) },
                            onLater = { dismissed = true }
                        )
                    }
                }
            }
        }
    }

    // Intercepted directly so a Bluetooth "camera shutter" remote's volume keys can
    // drive live scoring; only consumed while the live match screen is registered.
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null && MixioRemoteInput.handleKey(event)) return true
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> VolumeKeyBridge.onVolumeUp?.let { it(); return true }
            KeyEvent.KEYCODE_VOLUME_DOWN -> VolumeKeyBridge.onVolumeDown?.let { it(); return true }
        }
        return super.onKeyDown(keyCode, event)
    }


    // The MIXIO remote's taps/swipes are consumed here (never reach normal UI) and decoded
    // for the live match screen — see MixioRemoteInput.
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean =
        MixioRemoteInput.handle(ev) || super.dispatchTouchEvent(ev)

    override fun dispatchGenericMotionEvent(ev: MotionEvent): Boolean =
        MixioRemoteInput.handle(ev) || super.dispatchGenericMotionEvent(ev)

    private fun openApkUrl(url: String) {
        val uri = Uri.parse(url)
        if (uri.scheme != "https") return
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
            .onFailure { Log.w("MainActivity", "No app can open the update link") }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) container.wearEngineManager.disconnect()
    }
}

/** Overlays [SplashScreen] for a fixed 3s, then fades it out over 0.6s — matching iOS's
 * ZStack + .task { sleep(3s) } + .animation(.easeOut(duration: 0.6)) splash handoff. */
@Composable
private fun SplashOverlay() {
    var isShowingSplash by rememberSaveable { mutableStateOf(true) }
    val alpha by animateFloatAsState(
        targetValue = if (isShowingSplash) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "splashAlpha"
    )

    LaunchedEffect(Unit) {
        delay(3000)
        isShowingSplash = false
    }

    if (alpha > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha)
                .clickable(enabled = isShowingSplash, indication = null, interactionSource = remember { MutableInteractionSource() }) {}
        ) {
            SplashScreen()
        }
    }
}
