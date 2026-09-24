package com.matchpoint.app.ui

import android.content.Context
import com.matchpoint.app.audio.SoundManager
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

    init {
        SoundManager.init(context, preferences)
        // Connect eagerly (not screen-scoped like VolumeKeyBridge) so the remote is already
        // live by the time the user walks on court, rather than only once they open Live
        // Scoring. RemoteCommandHandler's callbacks are still screen-scoped — see
        // LiveMatchScreen.
        wearEngineManager.connect()
    }
}
