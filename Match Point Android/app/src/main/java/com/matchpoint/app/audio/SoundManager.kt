package com.matchpoint.app.audio

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import com.matchpoint.app.data.AppPreferences

/**
 * Plays the bundled announcer clips (res/raw). Fire-and-forget from the caller's
 * perspective — scoring never waits on this, and a missing/failed clip is skipped
 * rather than surfaced. Clips play strictly one at a time; queuing a new sequence
 * drops whatever was queued-but-not-yet-started so the announcer never falls behind
 * the latest valid score. Ported from the iOS app's AudioManager — woman voice only,
 * there's no male-voice variant or toggle here.
 */
object SoundManager {
    private lateinit var appContext: Context
    private lateinit var preferences: AppPreferences
    private var player: MediaPlayer? = null
    private val pending: MutableList<String> = mutableListOf()
    private val handler = Handler(Looper.getMainLooper())

    fun init(context: Context, preferences: AppPreferences) {
        appContext = context.applicationContext
        this.preferences = preferences
    }

    /** Enqueues a clip sequence. If nothing is playing, starts immediately; otherwise the
     * current clip finishes and this sequence plays next, replacing any earlier backlog. */
    fun play(filenames: List<String>) {
        if (filenames.isEmpty() || !isReady() || !preferences.announcerEnabled.value) return
        pending.clear()
        pending.addAll(filenames)
        if (player == null) playNext()
    }

    /** Like [play], but cuts off whatever's currently playing instead of waiting for it to
     * finish — used by the manual Sound FX chips, where a new tap always wins immediately. */
    fun playImmediately(filenames: List<String>) {
        if (filenames.isEmpty() || !isReady() || !preferences.announcerEnabled.value) return
        releasePlayer()
        pending.clear()
        pending.addAll(filenames)
        playNext()
    }

    private fun isReady(): Boolean = this::appContext.isInitialized

    private fun releasePlayer() {
        player?.setOnCompletionListener(null)
        player?.release()
        player = null
    }

    /** "pause_5s" -> 5000ms, "pause_2.5s" -> 2500ms; a silent gap token, not a real clip. */
    private fun pauseMillis(name: String): Long? {
        if (!name.startsWith("pause_") || !name.endsWith("s")) return null
        return name.removePrefix("pause_").removeSuffix("s").toDoubleOrNull()?.let { (it * 1000).toLong() }
    }

    private fun playNext() {
        if (pending.isEmpty()) {
            player = null
            return
        }
        val name = pending.removeAt(0)

        pauseMillis(name)?.let { ms ->
            handler.postDelayed({ playNext() }, ms)
            return
        }

        val resId = appContext.resources.getIdentifier(name, "raw", appContext.packageName)
        val mp = if (resId != 0) MediaPlayer.create(appContext, resId) else null
        if (mp == null) {
            playNext()
            return
        }
        mp.setOnCompletionListener {
            it.release()
            if (player === it) player = null
            playNext()
        }
        player = mp
        mp.start()
    }
}
