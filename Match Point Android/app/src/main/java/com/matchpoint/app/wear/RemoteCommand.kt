package com.matchpoint.app.wear

/**
 * Every command the Huawei Watch is allowed to send. The watch has no notion of games, sets,
 * or serve; it only ever sends one of these words over Wear Engine, optionally followed by
 * `|argument` (only SOUND_FX_PLAY takes one).
 */
enum class RemoteCommand(val wireValue: String) {
    POINT_A("POINT_A"),
    POINT_B("POINT_B"),
    PREVIOUS("PREVIOUS"),
    FINISH_MATCH("FINISH_MATCH"),
    ABANDON_MATCH("ABANDON_MATCH"),

    /** `SOUND_FX_PLAY|<index>`: play that entry of the Sound FX list sent in SOUND_FX_LIST. */
    SOUND_FX_PLAY("SOUND_FX_PLAY"),

    /** The watch asks for the full current state when it opens. */
    SYNC("SYNC"),

    /** The watch's connection heartbeat. Answered with PONG by [WearEngineManager]. */
    PING("PING");

    companion object {
        fun fromWireValue(value: String): RemoteCommand? =
            entries.firstOrNull { it.wireValue.equals(value, ignoreCase = true) }
    }
}
