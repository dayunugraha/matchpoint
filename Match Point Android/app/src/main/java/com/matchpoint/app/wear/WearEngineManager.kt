package com.matchpoint.app.wear

import android.content.Context
import android.util.Log
import com.huawei.wearengine.HiWear
import com.huawei.wearengine.WearEngineException
import com.huawei.wearengine.auth.AuthClient
import com.huawei.wearengine.auth.Permission
import com.huawei.wearengine.device.Device
import com.huawei.wearengine.device.DeviceClient
import com.huawei.wearengine.p2p.Message
import com.huawei.wearengine.p2p.P2pClient
import com.huawei.wearengine.p2p.Receiver
import com.huawei.wearengine.p2p.SendCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class WearConnectionState {
    DISCONNECTED,
    CONNECTING,
    NO_DEVICE,
    PERMISSION_REQUIRED,
    WATCH_APP_NOT_INSTALLED,
    CONNECTED,
    ERROR
}

/**
 * Owns every Huawei Wear Engine call the Android app makes: permission, bonded-device
 * discovery, and the P2P channel to the watch. This is the ONLY class in the app that talks
 * to the `com.huawei.wearengine` SDK — everything else sees [connectionState], [connect],
 * and [disconnect], plus decoded [RemoteCommand]s handed to [RemoteCommandHandler]. No
 * tennis scoring logic lives here or is reachable from here.
 *
 * Setup this class needs before it can reach a real watch (see wear/README.md at the repo
 * root for the full walkthrough):
 *  - An AppGallery Connect project with Wear Engine enabled, and its app ID placed in
 *    AndroidManifest.xml's `com.huawei.hms.client.appid` meta-data.
 *  - The Lite Wearable watch app's package name + signing fingerprint filled into
 *    [WATCH_APP_PACKAGE_NAME] / [WATCH_APP_FINGERPRINT] below.
 */
class WearEngineManager(context: Context) {

    private val appContext = context.applicationContext

    private val authClient: AuthClient = HiWear.getAuthClient(appContext)
    private val deviceClient: DeviceClient = HiWear.getDeviceClient(appContext)
    private val p2pClient: P2pClient = HiWear.getP2pClient(appContext).apply {
        setPeerPkgName(WATCH_APP_PACKAGE_NAME)
        setPeerFingerPrint(WATCH_APP_FINGERPRINT)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var connectJob: Job? = null
    private var retryJob: Job? = null
    private var healthJob: Job? = null
    private var stopped = false
    private var connectedDevice: Device? = null

    private val _connectionState = MutableStateFlow(WearConnectionState.DISCONNECTED)
    val connectionState: StateFlow<WearConnectionState> = _connectionState.asStateFlow()

    private val receiver = object : Receiver {
        override fun onReceiveMessage(message: Message) {
            // Wear Engine may deliver this off the main thread; RemoteCommandHandler's
            // callbacks reach ViewModel/Compose state, so hop back onto it.
            scope.launch { handleIncoming(message) }
        }
    }

    /** Detects, authorizes, and binds to the paired watch. Safe to call repeatedly (e.g. a
     * "Retry" button in the debug screen) — a connection already in flight is left alone.
     * When the watch is missing, the watch app is missing, or the attempt fails, it tries again
     * by itself every [RETRY_DELAY_MS]; a missing permission is not retried automatically. */
    fun connect() {
        if (connectJob?.isActive == true) return
        stopped = false
        retryJob?.cancel()
        connectJob = scope.launch {
            _connectionState.value = WearConnectionState.CONNECTING
            try {
                // DEVICE_MANAGER must be authorized before ANY device query, including
                // hasAvailableDevices() — Wear Engine rejects it otherwise.
                val granted = authClient.checkPermissions(REQUIRED_PERMISSIONS).await()
                if (granted == null || granted.any { !it }) {
                    val requestSucceeded = runCatching {
                        authClient.requestPermissionSuspend(REQUIRED_PERMISSIONS)
                    }.isSuccess
                    if (!requestSucceeded) {
                        _connectionState.value = WearConnectionState.PERMISSION_REQUIRED
                        return@launch
                    }
                }

                if (deviceClient.hasAvailableDevices().await() != true) {
                    _connectionState.value = WearConnectionState.NO_DEVICE
                    return@launch
                }

                val device = deviceClient.bondedDevices.await()?.firstOrNull { it.isConnected }
                if (device == null) {
                    _connectionState.value = WearConnectionState.NO_DEVICE
                    return@launch
                }

                if (p2pClient.isAppInstalled(device, WATCH_APP_PACKAGE_NAME).await() != true) {
                    _connectionState.value = WearConnectionState.WATCH_APP_NOT_INSTALLED
                    return@launch
                }

                p2pClient.registerReceiver(device, receiver).await()
                connectedDevice = device
                _connectionState.value = WearConnectionState.CONNECTED
                startHealthCheck(device)
            } catch (e: Exception) {
                val errorCode = (e as? WearEngineException)?.errorCode
                Log.w(TAG, "Failed to connect to Huawei watch: errorCode=${errorCode ?: "n/a"}", e)
                // Error 8 ("Scope unauthorized") means Huawei has not approved this app for
                // Wear Engine yet. Retrying does not help, so it is not retried.
                _connectionState.value =
                    if (errorCode == ERROR_CODE_UNAUTHORIZED) WearConnectionState.PERMISSION_REQUIRED
                    else WearConnectionState.ERROR
            }
            if (_connectionState.value in RETRY_STATES) scheduleRetry()
        }
    }

    private fun scheduleRetry() {
        if (stopped) return
        retryJob?.cancel()
        retryJob = scope.launch {
            delay(RETRY_DELAY_MS)
            connect()
        }
    }

    /** Wear Engine has no connection-lost callback here, so while connected the bonded device
     * list is checked every [HEALTH_INTERVAL_MS]. */
    private fun startHealthCheck(device: Device) {
        healthJob?.cancel()
        healthJob = scope.launch {
            while (true) {
                delay(HEALTH_INTERVAL_MS)
                if (!isStillConnected(device)) {
                    handleLost()
                    return@launch
                }
            }
        }
    }

    private suspend fun isStillConnected(device: Device): Boolean =
        runCatching {
            deviceClient.bondedDevices.await()?.any { it.uuid == device.uuid && it.isConnected } == true
        }.getOrDefault(false)

    /** The watch went away while connected: drop the receiver and start connecting again. */
    private fun handleLost() {
        Log.w(TAG, "Watch connection lost")
        healthJob?.cancel()
        connectedDevice?.let {
            runCatching { p2pClient.unregisterReceiver(receiver) }
        }
        connectedDevice = null
        _connectionState.value = WearConnectionState.DISCONNECTED
        connect()
    }

    /** Unregisters the P2P receiver and drops the current connection. Safe to call whether
     * or not a connection is currently active. */
    fun disconnect() {
        stopped = true
        connectJob?.cancel()
        retryJob?.cancel()
        healthJob?.cancel()
        val device = connectedDevice
        connectedDevice = null
        _connectionState.value = WearConnectionState.DISCONNECTED
        if (device != null) {
            runCatching { p2pClient.unregisterReceiver(receiver) }
                .onFailure { Log.w(TAG, "Failed to unregister Wear Engine receiver", it) }
        }
    }

    /** Sends a short text payload to the connected watch. No-ops if nothing is connected.
     * Not used for the five inbound commands (phone never needs to push those), but the
     * transport for a future per-command acknowledgement (see [RemoteCommandHandler]). */
    fun sendToWatch(text: String) {
        val device = connectedDevice ?: return
        val message = Message.Builder().setPayload(text.toByteArray(Charsets.UTF_8)).build()
        p2pClient.send(device, message, object : SendCallback {
            override fun onSendResult(resultCode: Int) {
                // 207 = success in Wear Engine (the Lite Wearable SDK uses the same code); log every
                // result while the two-way link is being verified.
                if (resultCode == 207) Log.d(TAG, "Sent to watch: $text")
                else Log.w(TAG, "Send to watch failed, resultCode=$resultCode")
            }

            override fun onSendProgress(progress: Long) = Unit
        }).addOnFailureListener {
            // A failed send may mean the watch is gone: check now instead of waiting for the
            // next health check.
            Log.w(TAG, "Send to watch failed", it)
            scope.launch { if (!isStillConnected(device)) handleLost() }
        }
    }

    private fun handleIncoming(message: Message) {
        // MVP wire format is a bare command string (see class doc on RemoteCommand) — no
        // JSON envelope yet. RemoteCommandHandler already carries a sequence number and
        // timestamp locally so upgrading the wire format later (command id + ack) won't
        // require touching any of the call sites that consume ReceivedCommand.
        val raw = String(message.data, Charsets.UTF_8).trim()
        Log.d(TAG, "Received from watch: \"$raw\"")
        // Wire format: COMMAND or COMMAND|argument (only SOUND_FX_PLAY has an argument).
        val parts = raw.split("|", limit = 2)
        val command = RemoteCommand.fromWireValue(parts[0])
        if (command == null) {
            Log.w(TAG, "Ignoring unrecognized remote command payload: \"$raw\"")
            return
        }
        RemoteCommandHandler.handle(command, parts.getOrNull(1))
        if (command == RemoteCommand.PING) sendToWatch(WatchEvent.Pong.encode())
        // No live match screen is open, so nothing answered the sync: tell the watch there is
        // no match to show.
        if (command == RemoteCommand.SYNC && RemoteCommandHandler.onSync == null) {
            sendToWatch(WatchEvent.MatchStatus(live = false).encode())
        }
    }

    companion object {
        private const val TAG = "WearEngineManager"
        private val REQUIRED_PERMISSIONS = arrayOf(Permission.DEVICE_MANAGER)

        // WearEngineException.errorCode seen in the log as "Scope unauthorized".
        private const val ERROR_CODE_UNAUTHORIZED = 8

        private const val RETRY_DELAY_MS = 15_000L
        private const val HEALTH_INTERVAL_MS = 10_000L
        private val RETRY_STATES = setOf(
            WearConnectionState.NO_DEVICE,
            WearConnectionState.WATCH_APP_NOT_INSTALLED,
            WearConnectionState.ERROR
        )

        // Watch app fingerprint: <bundleName>_<base64 raw EC public key of the watch's signing
        // certificate> (from "Match Point Watch Debug.cer"). A release watch cert needs a new value.
        private const val WATCH_APP_PACKAGE_NAME = "com.matchpoint.watch"
        private const val WATCH_APP_FINGERPRINT = "com.matchpoint.watch_BEI9/vflFOv3czAlgRCumzgAYIXWVnL32/pPIHun4/WAijCljdn7E/3wE+2yXfYamzFsOBKFQHE8OwojtU/ralg="
    }
}
