package com.matchpoint.app.ui.match

import android.content.res.Resources
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Window
import kotlin.math.abs
import kotlin.math.max

/**
 * Callbacks the live match screen registers while it's on screen, mirroring
 * [VolumeKeyBridge].
 */
object MixioRemoteBridge {
    var onPhoto1: (() -> Unit)? = null
    var onPhoto2: (() -> Unit)? = null
    var onHeart: (() -> Unit)? = null
    var onUp: (() -> Unit)? = null
    var onDown: (() -> Unit)? = null
    var onLeft: (() -> Unit)? = null
    var onRight: (() -> Unit)? = null

    fun clear() {
        onPhoto1 = null
        onPhoto2 = null
        onHeart = null
        onUp = null
        onDown = null
        onLeft = null
        onRight = null
    }
}

/**
 * Decodes the MIXIO Bluetooth remote. Besides one real key (Photo 1), the remote presents a
 * stylus digitizer: Photo 2 and Heart arrive as 30 ms taps and the arrow keys as swipes, so
 * they can't be caught with onKeyDown. Measured on-device (see MixioProbe log):
 *  - Photo 2 tap lands at ~85% of screen height, Heart at ~44% (the X coordinate is stale
 *    on taps, so only Y is used to tell them apart);
 *  - Arrow up = swipe with Y increasing, down = Y decreasing, left = X increasing,
 *    right = X decreasing.
 *
 * Every event from the remote is consumed, on every screen, so its phantom taps never press
 * whatever UI happens to sit at those coordinates.
 */
object MixioRemoteInput {
    private const val DEVICE_NAME_PREFIX = "MIXIO"
    private const val SWIPE_THRESHOLD = 0.08f
    private const val PHOTO2_MIN_Y = 0.65f
    private const val HEART_MIN_Y = 0.25f

    private var startX = 0f
    private var startY = 0f
    private var tracking = false

    /** Photo 1 alternates between KEYCODE_VOLUME_UP and KEYCODE_VOLUME_DOWN on successive
     * presses (measured with getevent), so both must mean "Photo 1" for this device — unlike
     * generic shutter remotes and the phone's own buttons, where up/down are two sides.
     * Returns true if [event] is a remote key press (consumed either way). */
    fun handleKey(event: KeyEvent): Boolean {
        if (event.device?.name?.startsWith(DEVICE_NAME_PREFIX) != true) return false
        val isVolumeKey = event.keyCode == KeyEvent.KEYCODE_VOLUME_UP || event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        if (isVolumeKey && event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            MixioRemoteBridge.onPhoto1?.invoke()
        }
        return isVolumeKey
    }

    /** Returns true if [event] came from the remote (and so must not reach normal UI). */
    fun handle(event: MotionEvent): Boolean {
        if (InputDevice.getDevice(event.deviceId)?.name?.startsWith(DEVICE_NAME_PREFIX) != true) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.rawX
                startY = event.rawY
                tracking = true
            }
            MotionEvent.ACTION_UP -> {
                if (tracking) classify(event.rawX, event.rawY)
                tracking = false
            }
            MotionEvent.ACTION_CANCEL -> tracking = false
        }
        return true
    }

    private fun classify(endX: Float, endY: Float) {
        val metrics = Resources.getSystem().displayMetrics
        val width = metrics.widthPixels.toFloat()
        val height = metrics.heightPixels.toFloat()
        val dx = (endX - startX) / width
        val dy = (endY - startY) / height

        when {
            max(abs(dx), abs(dy)) < SWIPE_THRESHOLD -> {
                val y = endY / height
                when {
                    y >= PHOTO2_MIN_Y -> MixioRemoteBridge.onPhoto2?.invoke()
                    y >= HEART_MIN_Y -> MixioRemoteBridge.onHeart?.invoke()
                }
            }
            abs(dy) > abs(dx) -> if (dy > 0) MixioRemoteBridge.onUp?.invoke() else MixioRemoteBridge.onDown?.invoke()
            else -> if (dx > 0) MixioRemoteBridge.onLeft?.invoke() else MixioRemoteBridge.onRight?.invoke()
        }
    }
}

private class MixioWindowCallback(private val delegate: Window.Callback) : Window.Callback by delegate {
    override fun dispatchTouchEvent(event: MotionEvent): Boolean =
        MixioRemoteInput.handle(event) || delegate.dispatchTouchEvent(event)

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean =
        MixioRemoteInput.handle(event) || delegate.dispatchGenericMotionEvent(event)
}

/** Dialogs are separate windows, so the remote's taps would land on them directly instead of
 * going through the Activity. Wrapping their window callback keeps the remote working (and
 * harmless) while a dialog is up. */
fun Window.interceptMixioRemote() {
    if (callback !is MixioWindowCallback) callback = MixioWindowCallback(callback)
}
