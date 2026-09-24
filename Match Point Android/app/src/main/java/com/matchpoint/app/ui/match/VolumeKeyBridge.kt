package com.matchpoint.app.ui.match

/**
 * Lets a screen intercept the hardware volume buttons directly (Activity.onKeyDown),
 * which is what makes cheap Bluetooth "camera shutter" remotes work for live scoring
 * out of the box — no HID-keyboard-specific hardware required.
 *
 * Only the live match screen sets these; MainActivity falls back to normal volume
 * behavior whenever they're null.
 */
object VolumeKeyBridge {
    var onVolumeUp: (() -> Unit)? = null
    var onVolumeDown: (() -> Unit)? = null

    fun clear() {
        onVolumeUp = null
        onVolumeDown = null
    }
}
