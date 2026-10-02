package com.matchpoint.app.wear

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ReceivedCommand(
    val command: RemoteCommand,
    val timestamp: Long,
    val sequenceNumber: Long
)

/**
 * Routes a [RemoteCommand] decoded from the watch to whichever screen currently cares about
 * it. This is intentionally shaped like [com.matchpoint.app.ui.match.VolumeKeyBridge]: the
 * live match screen registers callbacks while it's on screen and clears them on dispose, so
 * a command that arrives with no screen listening is simply dropped.
 *
 * This object never touches UI state itself — it only invokes whatever callback the
 * currently active screen registered, which in turn calls into the existing
 * [com.matchpoint.app.ui.match.LiveMatchViewModel] / scoring layer. That indirection is what
 * keeps the watch a pure remote: no scoring logic lives here.
 */
object RemoteCommandHandler {
    var onPointA: (() -> Unit)? = null
    var onPointB: (() -> Unit)? = null
    var onPrevious: (() -> Unit)? = null
    var onFinishMatch: (() -> Unit)? = null
    var onAbandonMatch: (() -> Unit)? = null
    var onSoundFxPlay: ((Int) -> Unit)? = null
    var onSync: (() -> Unit)? = null

    private var sequenceCounter = 0L

    private val _lastReceived = MutableStateFlow<ReceivedCommand?>(null)
    val lastReceived: StateFlow<ReceivedCommand?> = _lastReceived.asStateFlow()

    /** Called by [WearEngineManager] for every command decoded off the wire. Must be called
     * on the main thread — the callbacks it invokes touch ViewModel/Compose state. */
    fun handle(command: RemoteCommand, argument: String? = null) {
        sequenceCounter += 1
        _lastReceived.value = ReceivedCommand(command, System.currentTimeMillis(), sequenceCounter)

        when (command) {
            RemoteCommand.POINT_A -> onPointA?.invoke()
            RemoteCommand.POINT_B -> onPointB?.invoke()
            RemoteCommand.PREVIOUS -> onPrevious?.invoke()
            RemoteCommand.FINISH_MATCH -> onFinishMatch?.invoke()
            RemoteCommand.ABANDON_MATCH -> onAbandonMatch?.invoke()
            RemoteCommand.SOUND_FX_PLAY -> argument?.toIntOrNull()?.let { onSoundFxPlay?.invoke(it) }
            RemoteCommand.SYNC -> onSync?.invoke()
            // Heartbeat: recorded above, answered by WearEngineManager.
            RemoteCommand.PING -> Unit
        }
    }

    fun clear() {
        onPointA = null
        onPointB = null
        onPrevious = null
        onFinishMatch = null
        onAbandonMatch = null
        onSoundFxPlay = null
        onSync = null
    }
}
