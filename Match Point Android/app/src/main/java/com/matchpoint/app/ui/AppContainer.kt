package com.matchpoint.app.ui

import android.content.Context
import com.matchpoint.app.audio.SoundManager
import com.matchpoint.app.backend.InstallationTracker
import com.matchpoint.app.backend.RegistrationRepository
import com.matchpoint.app.backend.ReleaseRepository
import com.matchpoint.app.backend.SupabaseBackend
import com.matchpoint.app.backup.BackupService
import com.matchpoint.app.data.AppDatabase
import com.matchpoint.app.data.AppPreferences
import com.matchpoint.app.repository.SessionRepository
import com.matchpoint.app.wear.WearEngineManager

/** Minimal manual service locator — no DI framework needed for this app's size. */
class AppContainer(context: Context) {
    val repository: SessionRepository = SessionRepository(AppDatabase.get(context))
    val preferences: AppPreferences = AppPreferences(context)
    val backupService: BackupService = BackupService(AppDatabase.get(context))
    val wearEngineManager: WearEngineManager = WearEngineManager(context)

    // Backend/release service layer — independent of the local-first Room data above; only
    // ever writes to users/installations, never Room. Constructed eagerly (no network I/O
    // yet), same as everything else here — actual calls only happen from RegistrationRepository
    // and MainActivity's post-registration "touch" effect, never per-screen.
    val supabaseBackend: SupabaseBackend = SupabaseBackend()
    val installationTracker: InstallationTracker = InstallationTracker(context, supabaseBackend)
    val registrationRepository: RegistrationRepository = RegistrationRepository(context, supabaseBackend, installationTracker)
    val releaseRepository: ReleaseRepository = ReleaseRepository(supabaseBackend)

    init {
        SoundManager.init(context, preferences)
        // Connect eagerly (not screen-scoped like VolumeKeyBridge) so the remote is already
        // live by the time the user walks on court, rather than only once they open Live
        // Scoring. RemoteCommandHandler's callbacks are still screen-scoped — see
        // LiveMatchScreen.
        wearEngineManager.connect()
    }

    companion object {
        @Volatile private var instance: AppContainer? = null

        /** One container per process: an Activity recreate must not rebuild services (and
         * re-run Wear Engine connect) — only a real process restart does. */
        fun get(context: Context): AppContainer =
            instance ?: synchronized(this) {
                instance ?: AppContainer(context.applicationContext).also { instance = it }
            }
    }
}
