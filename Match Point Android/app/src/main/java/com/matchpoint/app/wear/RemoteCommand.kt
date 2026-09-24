package com.matchpoint.app.wear

/**
 * Every command the Huawei Watch is allowed to send. The watch has no notion of games, sets,
 * or serve; it only ever sends one of these words over Wear Engine (see the product spec,
 * section 20).
 */
enum class RemoteCommand(val wireValue: String) {
    POINT_A("POINT_A"),
    POINT_B("POINT_B"),
    PREVIOUS("PREVIOUS"),
    OK("OK"),
    SOUND_FX_PREVIOUS("SOUND_FX_PREVIOUS"),
    SOUND_FX_NEXT("SOUND_FX_NEXT"),
    SOUND_FX_CONFIRM("SOUND_FX_CONFIRM"),
    FINISH_MATCH("FINISH_MATCH"),

    /** Not in the original spec list: the watch asks for the full current state when it opens.
     * Android answers with MATCH_STATUS, SCORE_UPDATE, and SOUND_FX_UPDATE. */
    SYNC("SYNC"),

    /** Debug only, not in the product spec: connectivity test. Answered with PONG by
     * [WearEngineManager]. Remove before release. */
    PING("PING");

    companion object {
        fun fromWireValue(value: String): RemoteCommand? =
            entries.firstOrNull { it.wireValue.equals(value, ignoreCase = true) }
    }
}
